import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as cloudwatchActions from 'aws-cdk-lib/aws-cloudwatch-actions';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ecsPatterns from 'aws-cdk-lib/aws-ecs-patterns';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as msk from 'aws-cdk-lib/aws-msk';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as ssm from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';

export interface MSKCrossRegionStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly vpc: ec2.IVpc;
    readonly primaryMskCluster: msk.CfnCluster;
    readonly secondaryMskCluster?: msk.CfnCluster;
    readonly primaryRegion: string;
    readonly secondaryRegion: string;
    readonly regionType: 'primary' | 'secondary';
    readonly alertingTopic: sns.ITopic;
}

export class MSKCrossRegionStack extends cdk.Stack {
    public readonly mirrorMakerCluster: ecs.Cluster;
    public readonly mirrorMakerService: ecsPatterns.ApplicationLoadBalancedFargateService;
    public readonly mirrorMakerTaskRole: iam.Role;
    public readonly mirrorMakerLogGroup: logs.LogGroup;
    public readonly replicationMonitoring: cloudwatch.Dashboard;

    constructor(scope: Construct, id: string, props: MSKCrossRegionStackProps) {
        super(scope, id, props);

        const {
            environment,
            projectName,
            vpc,
            primaryMskCluster,
            secondaryMskCluster,
            primaryRegion,
            secondaryRegion,
            regionType,
            alertingTopic
        } = props;

        // Apply common tags
        const commonTags = {
            Project: projectName,
            Environment: environment,
            ManagedBy: 'AWS-CDK',
            Component: 'MSK-CrossRegion',
            Service: 'MirrorMaker',
            RegionType: regionType
        };

        Object.entries(commonTags).forEach(([key, value]) => {
            cdk.Tags.of(this).add(key, value);
        });

        // Get multi-region configuration
        const multiRegionConfig = this.node.tryGetContext('genai-demo:multi-region') || {};

        // Only deploy MirrorMaker if cross-region replication is enabled
        if (multiRegionConfig['enable-cross-region-replication'] && environment === 'production') {
            // Create ECS cluster for MirrorMaker
            this.mirrorMakerCluster = this.createECSCluster(projectName, environment, vpc);

            // Create IAM role for MirrorMaker task
            this.mirrorMakerTaskRole = this.createMirrorMakerTaskRole(projectName, environment, primaryRegion, secondaryRegion);

            // Create log group for MirrorMaker
            this.mirrorMakerLogGroup = this.createMirrorMakerLogGroup(projectName, environment);

            // Deploy MirrorMaker 2.0 service
            this.mirrorMakerService = this.createMirrorMakerService(
                projectName,
                environment,
                vpc,
                this.mirrorMakerCluster,
                primaryMskCluster,
                secondaryMskCluster,
                primaryRegion,
                secondaryRegion,
                regionType
            );

            // Create monitoring dashboard for replication
            this.replicationMonitoring = this.createReplicationMonitoring(
                projectName,
                environment,
                regionType,
                primaryRegion,
                secondaryRegion
            );

            // Set up alerting for replication issues
            this.createReplicationAlerting(projectName, environment, alertingTopic);

            // Store replication configuration in Parameter Store
            this.storeReplicationConfiguration(projectName, environment, primaryRegion, secondaryRegion, regionType);

            // Create outputs
            this.createOutputs(projectName, environment, regionType);
        }
    }

    private createECSCluster(projectName: string, environment: string, vpc: ec2.IVpc): ecs.Cluster {
        const cluster = new ecs.Cluster(this, 'MirrorMakerCluster', {
            clusterName: `${projectName}-${environment}-mirrormaker`,
            vpc: vpc,
            containerInsights: true,
            enableFargateCapacityProviders: true
        });

        cdk.Tags.of(cluster).add('Name', `${projectName}-${environment}-mirrormaker-cluster`);
        cdk.Tags.of(cluster).add('Service', 'MirrorMaker');

        return cluster;
    }

    private createMirrorMakerTaskRole(
        projectName: string,
        environment: string,
        primaryRegion: string,
        secondaryRegion: string
    ): iam.Role {
        const role = new iam.Role(this, 'MirrorMakerTaskRole', {
            roleName: `${projectName}-${environment}-mirrormaker-task-role`,
            assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
            description: 'IAM role for MirrorMaker 2.0 ECS task'
        });

        // Add MSK permissions for both regions
        const regions = [primaryRegion, secondaryRegion];
        regions.forEach(region => {
            role.addToPolicy(new iam.PolicyStatement({
                effect: iam.Effect.ALLOW,
                actions: [
                    'kafka-cluster:Connect',
                    'kafka-cluster:AlterCluster',
                    'kafka-cluster:DescribeCluster',
                    'kafka-cluster:WriteData',
                    'kafka-cluster:ReadData',
                    'kafka-cluster:CreateTopic',
                    'kafka-cluster:DescribeTopic',
                    'kafka-cluster:AlterTopic',
                    'kafka-cluster:DeleteTopic',
                    'kafka-cluster:DescribeGroup',
                    'kafka-cluster:AlterGroup'
                ],
                resources: [
                    `arn:aws:kafka:${region}:${this.account}:cluster/${projectName}-${environment}-msk/*`,
                    `arn:aws:kafka:${region}:${this.account}:topic/${projectName}-${environment}-msk/*`,
                    `arn:aws:kafka:${region}:${this.account}:group/${projectName}-${environment}-msk/*`
                ]
            }));
        });

        // Add CloudWatch Logs permissions
        role.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'logs:CreateLogGroup',
                'logs:CreateLogStream',
                'logs:PutLogEvents',
                'logs:DescribeLogGroups',
                'logs:DescribeLogStreams'
            ],
            resources: [
                `arn:aws:logs:${this.region}:${this.account}:log-group:/aws/ecs/mirrormaker/${projectName}-${environment}*`
            ]
        }));

        // Add CloudWatch Metrics permissions
        role.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'cloudwatch:PutMetricData'
            ],
            resources: ['*'],
            conditions: {
                StringEquals: {
                    'cloudwatch:namespace': `${projectName}/${environment}/MirrorMaker`
                }
            }
        }));

        // Add Systems Manager permissions for configuration
        role.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'ssm:GetParameter',
                'ssm:GetParameters',
                'ssm:GetParametersByPath'
            ],
            resources: [
                `arn:aws:ssm:${this.region}:${this.account}:parameter/${projectName}/${environment}/mirrormaker/*`
            ]
        }));

        cdk.Tags.of(role).add('Name', `${projectName}-${environment}-mirrormaker-task-role`);
        cdk.Tags.of(role).add('Service', 'MirrorMaker');

        return role;
    }

    private createMirrorMakerLogGroup(projectName: string, environment: string): logs.LogGroup {
        const logGroup = new logs.LogGroup(this, 'MirrorMakerLogGroup', {
            logGroupName: `/aws/ecs/mirrormaker/${projectName}-${environment}`,
            retention: environment === 'production' ? logs.RetentionDays.ONE_MONTH : logs.RetentionDays.ONE_WEEK,
            removalPolicy: environment === 'production' ? cdk.RemovalPolicy.RETAIN : cdk.RemovalPolicy.DESTROY
        });

        cdk.Tags.of(logGroup).add('Name', `${projectName}-${environment}-mirrormaker-logs`);
        cdk.Tags.of(logGroup).add('LogType', 'MirrorMaker');

        return logGroup;
    }

    private createMirrorMakerService(
        projectName: string,
        environment: string,
        vpc: ec2.IVpc,
        cluster: ecs.Cluster,
        primaryMskCluster: msk.CfnCluster,
        secondaryMskCluster: msk.CfnCluster | undefined,
        primaryRegion: string,
        secondaryRegion: string,
        regionType: 'primary' | 'secondary'
    ): ecsPatterns.ApplicationLoadBalancedFargateService {
        // MirrorMaker 2.0 configuration
        const mirrorMakerConfig = this.generateMirrorMakerConfig(
            projectName,
            environment,
            primaryMskCluster,
            secondaryMskCluster,
            primaryRegion,
            secondaryRegion,
            regionType
        );

        // Create Fargate service for MirrorMaker
        const service = new ecsPatterns.ApplicationLoadBalancedFargateService(this, 'MirrorMakerService', {
            serviceName: `${projectName}-${environment}-mirrormaker`,
            cluster: cluster,
            cpu: 1024, // 1 vCPU
            memoryLimitMiB: 2048, // 2 GB
            desiredCount: 1, // Single instance for simplicity

            // Task definition
            taskImageOptions: {
                image: ecs.ContainerImage.fromRegistry('confluentinc/cp-kafka-connect:7.4.0'),
                containerName: 'mirrormaker',
                containerPort: 8083,

                // Environment variables for MirrorMaker configuration
                environment: {
                    CONNECT_BOOTSTRAP_SERVERS: cdk.Fn.getAtt(primaryMskCluster.logicalId, 'BootstrapBrokerStringSaslIam').toString(),
                    CONNECT_REST_ADVERTISED_HOST_NAME: 'localhost',
                    CONNECT_REST_PORT: '8083',
                    CONNECT_GROUP_ID: `${projectName}-${environment}-mirrormaker-group`,
                    CONNECT_CONFIG_STORAGE_TOPIC: `${projectName}.${environment}.mirrormaker.configs`,
                    CONNECT_OFFSET_STORAGE_TOPIC: `${projectName}.${environment}.mirrormaker.offsets`,
                    CONNECT_STATUS_STORAGE_TOPIC: `${projectName}.${environment}.mirrormaker.status`,
                    CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR: '3',
                    CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR: '3',
                    CONNECT_STATUS_STORAGE_REPLICATION_FACTOR: '3',
                    CONNECT_KEY_CONVERTER: 'org.apache.kafka.connect.storage.StringConverter',
                    CONNECT_VALUE_CONVERTER: 'org.apache.kafka.connect.json.JsonConverter',
                    CONNECT_VALUE_CONVERTER_SCHEMAS_ENABLE: 'false',
                    CONNECT_INTERNAL_KEY_CONVERTER: 'org.apache.kafka.connect.json.JsonConverter',
                    CONNECT_INTERNAL_VALUE_CONVERTER: 'org.apache.kafka.connect.json.JsonConverter',
                    CONNECT_INTERNAL_KEY_CONVERTER_SCHEMAS_ENABLE: 'false',
                    CONNECT_INTERNAL_VALUE_CONVERTER_SCHEMAS_ENABLE: 'false',
                    CONNECT_LOG4J_ROOT_LOGLEVEL: 'INFO',
                    CONNECT_LOG4J_LOGGERS: 'org.apache.kafka.connect.runtime.rest=WARN,org.reflections=ERROR',
                    CONNECT_PLUGIN_PATH: '/usr/share/java,/usr/share/confluent-hub-components',

                    // Security configuration for MSK IAM
                    CONNECT_SECURITY_PROTOCOL: 'SASL_SSL',
                    CONNECT_SASL_MECHANISM: 'AWS_MSK_IAM',
                    CONNECT_SASL_JAAS_CONFIG: 'software.amazon.msk.auth.iam.IAMLoginModule required;',
                    CONNECT_SASL_CLIENT_CALLBACK_HANDLER_CLASS: 'software.amazon.msk.auth.iam.IAMClientCallbackHandler',

                    // Producer configuration
                    CONNECT_PRODUCER_SECURITY_PROTOCOL: 'SASL_SSL',
                    CONNECT_PRODUCER_SASL_MECHANISM: 'AWS_MSK_IAM',
                    CONNECT_PRODUCER_SASL_JAAS_CONFIG: 'software.amazon.msk.auth.iam.IAMLoginModule required;',
                    CONNECT_PRODUCER_SASL_CLIENT_CALLBACK_HANDLER_CLASS: 'software.amazon.msk.auth.iam.IAMClientCallbackHandler',

                    // Consumer configuration
                    CONNECT_CONSUMER_SECURITY_PROTOCOL: 'SASL_SSL',
                    CONNECT_CONSUMER_SASL_MECHANISM: 'AWS_MSK_IAM',
                    CONNECT_CONSUMER_SASL_JAAS_CONFIG: 'software.amazon.msk.auth.iam.IAMLoginModule required;',
                    CONNECT_CONSUMER_SASL_CLIENT_CALLBACK_HANDLER_CLASS: 'software.amazon.msk.auth.iam.IAMClientCallbackHandler',

                    // MirrorMaker 2.0 specific configuration
                    MIRRORMAKER_CONFIG: mirrorMakerConfig
                },

                taskRole: this.mirrorMakerTaskRole,

                // Logging configuration
                logDriver: ecs.LogDrivers.awsLogs({
                    logGroup: this.mirrorMakerLogGroup,
                    streamPrefix: 'mirrormaker'
                })
            },

            // Network configuration
            publicLoadBalancer: false,
            listenerPort: 8083,

            // Health check configuration
            healthCheckGracePeriod: cdk.Duration.seconds(300),

            // VPC configuration
            taskSubnets: {
                subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS
            }
        });

        // Configure health check
        service.targetGroup.configureHealthCheck({
            path: '/connectors',
            port: '8083',
            protocol: elbv2.Protocol.HTTP,
            healthyThresholdCount: 2,
            unhealthyThresholdCount: 3,
            timeout: cdk.Duration.seconds(30),
            interval: cdk.Duration.seconds(60)
        });

        cdk.Tags.of(service.service).add('Name', `${projectName}-${environment}-mirrormaker-service`);
        cdk.Tags.of(service.service).add('Service', 'MirrorMaker');

        return service;
    }

    private generateMirrorMakerConfig(
        projectName: string,
        environment: string,
        primaryMskCluster: msk.CfnCluster,
        secondaryMskCluster: msk.CfnCluster | undefined,
        primaryRegion: string,
        secondaryRegion: string,
        regionType: 'primary' | 'secondary'
    ): string {
        const primaryBootstrapServers = cdk.Fn.getAtt(primaryMskCluster.logicalId, 'BootstrapBrokerStringSaslIam').toString();
        const secondaryBootstrapServers = secondaryMskCluster
            ? cdk.Fn.getAtt(secondaryMskCluster.logicalId, 'BootstrapBrokerStringSaslIam').toString()
            : 'placeholder-secondary-servers';

        // MirrorMaker 2.0 configuration for bidirectional replication
        const config = {
            // Cluster aliases
            clusters: `${primaryRegion},${secondaryRegion}`,
            [`${primaryRegion}.bootstrap.servers`]: primaryBootstrapServers,
            [`${secondaryRegion}.bootstrap.servers`]: secondaryBootstrapServers,

            // Security configuration for both clusters
            [`${primaryRegion}.security.protocol`]: 'SASL_SSL',
            [`${primaryRegion}.sasl.mechanism`]: 'AWS_MSK_IAM',
            [`${primaryRegion}.sasl.jaas.config`]: 'software.amazon.msk.auth.iam.IAMLoginModule required;',
            [`${primaryRegion}.sasl.client.callback.handler.class`]: 'software.amazon.msk.auth.iam.IAMClientCallbackHandler',

            [`${secondaryRegion}.security.protocol`]: 'SASL_SSL',
            [`${secondaryRegion}.sasl.mechanism`]: 'AWS_MSK_IAM',
            [`${secondaryRegion}.sasl.jaas.config`]: 'software.amazon.msk.auth.iam.IAMLoginModule required;',
            [`${secondaryRegion}.sasl.client.callback.handler.class`]: 'software.amazon.msk.auth.iam.IAMClientCallbackHandler',

            // Replication flows (bidirectional)
            [`${primaryRegion}->${secondaryRegion}.enabled`]: 'true',
            [`${secondaryRegion}->${primaryRegion}.enabled`]: 'true',

            // Topic patterns to replicate (domain events only)
            [`${primaryRegion}->${secondaryRegion}.topics`]: `${projectName}\\.${environment}\\..*`,
            [`${secondaryRegion}->${primaryRegion}.topics`]: `${projectName}\\.${environment}\\..*`,

            // Replication policy
            'replication.policy.class': 'org.apache.kafka.connect.mirror.DefaultReplicationPolicy',
            'replication.policy.separator': '.',

            // Sync configuration
            'sync.topic.acls.enabled': 'false',
            'sync.topic.configs.enabled': 'true',
            'emit.heartbeats.enabled': 'true',
            'emit.checkpoints.enabled': 'true',

            // Performance tuning
            'tasks.max': '2',
            'replication.factor': '3',
            'refresh.topics.enabled': 'true',
            'refresh.topics.interval.seconds': '600', // 10 minutes
            'refresh.groups.enabled': 'true',
            'refresh.groups.interval.seconds': '600', // 10 minutes

            // Heartbeat configuration
            'heartbeats.topic.replication.factor': '3',
            'checkpoints.topic.replication.factor': '3',
            'offset.syncs.topic.replication.factor': '3',

            // Error handling
            'errors.tolerance': 'all',
            'errors.log.enable': 'true',
            'errors.log.include.messages': 'true'
        };

        return JSON.stringify(config);
    }

    private createReplicationMonitoring(
        projectName: string,
        environment: string,
        regionType: 'primary' | 'secondary',
        primaryRegion: string,
        secondaryRegion: string
    ): cloudwatch.Dashboard {
        const dashboard = new cloudwatch.Dashboard(this, 'ReplicationMonitoringDashboard', {
            dashboardName: `${projectName}-${environment}-${regionType}-msk-replication`,
            defaultInterval: cdk.Duration.minutes(5)
        });

        // Add header widget
        dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `# MSK Cross-Region Replication Monitoring\n\n**Environment:** ${environment}\n**Region Type:** ${regionType}\n**Primary Region:** ${primaryRegion}\n**Secondary Region:** ${secondaryRegion}\n**Last Updated:** ${new Date().toISOString()}`,
                width: 24,
                height: 4
            })
        );

        // Add MirrorMaker service metrics
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'MirrorMaker Service Health',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/ECS',
                        metricName: 'CPUUtilization',
                        dimensionsMap: {
                            ServiceName: `${projectName}-${environment}-mirrormaker`,
                            ClusterName: `${projectName}-${environment}-mirrormaker`
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5)
                    })
                ],
                right: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/ECS',
                        metricName: 'MemoryUtilization',
                        dimensionsMap: {
                            ServiceName: `${projectName}-${environment}-mirrormaker`,
                            ClusterName: `${projectName}-${environment}-mirrormaker`
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5)
                    })
                ],
                width: 12,
                height: 6
            })
        );

        // Add replication lag metrics
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Replication Lag',
                left: [
                    new cloudwatch.Metric({
                        namespace: `${projectName}/${environment}/MirrorMaker`,
                        metricName: 'ReplicationLag',
                        dimensionsMap: {
                            SourceCluster: primaryRegion,
                            TargetCluster: secondaryRegion
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5)
                    })
                ],
                width: 12,
                height: 6
            })
        );

        // Add throughput metrics
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Replication Throughput',
                left: [
                    new cloudwatch.Metric({
                        namespace: `${projectName}/${environment}/MirrorMaker`,
                        metricName: 'MessagesReplicated',
                        dimensionsMap: {
                            SourceCluster: primaryRegion,
                            TargetCluster: secondaryRegion
                        },
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(5)
                    })
                ],
                right: [
                    new cloudwatch.Metric({
                        namespace: `${projectName}/${environment}/MirrorMaker`,
                        metricName: 'BytesReplicated',
                        dimensionsMap: {
                            SourceCluster: primaryRegion,
                            TargetCluster: secondaryRegion
                        },
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(5)
                    })
                ],
                width: 24,
                height: 6
            })
        );

        return dashboard;
    }

    private createReplicationAlerting(
        projectName: string,
        environment: string,
        alertingTopic: sns.ITopic
    ): void {
        // MirrorMaker service health alarm
        const serviceHealthAlarm = new cloudwatch.Alarm(this, 'MirrorMakerServiceHealthAlarm', {
            alarmName: `${projectName}-${environment}-mirrormaker-service-unhealthy`,
            alarmDescription: 'MirrorMaker service is unhealthy',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/ECS',
                metricName: 'RunningTaskCount',
                dimensionsMap: {
                    ServiceName: `${projectName}-${environment}-mirrormaker`,
                    ClusterName: `${projectName}-${environment}-mirrormaker`
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(5)
            }),
            threshold: 1,
            comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
            evaluationPeriods: 2,
            treatMissingData: cloudwatch.TreatMissingData.BREACHING
        });

        serviceHealthAlarm.addAlarmAction(new cloudwatchActions.SnsAction(alertingTopic));

        // Replication lag alarm
        const replicationLagAlarm = new cloudwatch.Alarm(this, 'ReplicationLagAlarm', {
            alarmName: `${projectName}-${environment}-replication-lag-high`,
            alarmDescription: 'Cross-region replication lag is high',
            metric: new cloudwatch.Metric({
                namespace: `${projectName}/${environment}/MirrorMaker`,
                metricName: 'ReplicationLag',
                statistic: 'Average',
                period: cdk.Duration.minutes(5)
            }),
            threshold: 300000, // 5 minutes in milliseconds
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            evaluationPeriods: 3,
            treatMissingData: cloudwatch.TreatMissingData.BREACHING
        });

        replicationLagAlarm.addAlarmAction(new cloudwatchActions.SnsAction(alertingTopic));

        // Add tags
        cdk.Tags.of(serviceHealthAlarm).add('Name', `${projectName}-${environment}-mirrormaker-service-health`);
        cdk.Tags.of(serviceHealthAlarm).add('AlarmType', 'ServiceHealth');
        cdk.Tags.of(replicationLagAlarm).add('Name', `${projectName}-${environment}-replication-lag`);
        cdk.Tags.of(replicationLagAlarm).add('AlarmType', 'ReplicationLag');
    }

    private storeReplicationConfiguration(
        projectName: string,
        environment: string,
        primaryRegion: string,
        secondaryRegion: string,
        regionType: 'primary' | 'secondary'
    ): void {
        const parameterPrefix = `/genai-demo/${environment}/mirrormaker`;

        // Store replication configuration
        const replicationConfig = {
            enabled: true,
            primaryRegion: primaryRegion,
            secondaryRegion: secondaryRegion,
            regionType: regionType,
            bidirectional: true,
            topicPattern: `${projectName}\\.${environment}\\..*`,
            replicationFactor: 3,
            tasksMax: 2,
            refreshIntervalSeconds: 600
        };

        new ssm.StringParameter(this, 'ReplicationConfiguration', {
            parameterName: `${parameterPrefix}/configuration`,
            stringValue: JSON.stringify(replicationConfig),
            description: `MirrorMaker replication configuration for ${projectName} ${environment}`,
            tier: ssm.ParameterTier.STANDARD
        });

        // Store service endpoint
        new ssm.StringParameter(this, 'MirrorMakerEndpoint', {
            parameterName: `${parameterPrefix}/endpoint`,
            stringValue: this.mirrorMakerService.loadBalancer.loadBalancerDnsName,
            description: `MirrorMaker service endpoint for ${projectName} ${environment}`,
            tier: ssm.ParameterTier.STANDARD
        });
    }

    private createOutputs(projectName: string, environment: string, regionType: 'primary' | 'secondary'): void {
        // MirrorMaker service outputs
        new cdk.CfnOutput(this, 'MirrorMakerServiceName', {
            value: this.mirrorMakerService.service.serviceName,
            description: 'MirrorMaker ECS service name',
            exportName: `${projectName}-${environment}-${regionType}-mirrormaker-service-name`
        });

        new cdk.CfnOutput(this, 'MirrorMakerClusterName', {
            value: this.mirrorMakerCluster.clusterName,
            description: 'MirrorMaker ECS cluster name',
            exportName: `${projectName}-${environment}-${regionType}-mirrormaker-cluster-name`
        });

        new cdk.CfnOutput(this, 'MirrorMakerEndpoint', {
            value: this.mirrorMakerService.loadBalancer.loadBalancerDnsName,
            description: 'MirrorMaker service endpoint',
            exportName: `${projectName}-${environment}-${regionType}-mirrormaker-endpoint`
        });

        new cdk.CfnOutput(this, 'MirrorMakerTaskRoleArn', {
            value: this.mirrorMakerTaskRole.roleArn,
            description: 'MirrorMaker task role ARN',
            exportName: `${projectName}-${environment}-${regionType}-mirrormaker-task-role-arn`
        });

        new cdk.CfnOutput(this, 'ReplicationMonitoringDashboardUrl', {
            value: `https://${this.region}.console.aws.amazon.com/cloudwatch/home?region=${this.region}#dashboards:name=${this.replicationMonitoring.dashboardName}`,
            description: 'Replication monitoring dashboard URL',
            exportName: `${projectName}-${environment}-${regionType}-replication-monitoring-url`
        });

        // Configuration outputs
        new cdk.CfnOutput(this, 'CrossRegionReplicationEnabled', {
            value: 'true',
            description: 'Cross-region replication status',
            exportName: `${projectName}-${environment}-${regionType}-cross-region-replication-enabled`
        });

        new cdk.CfnOutput(this, 'ReplicationTopicPattern', {
            value: `${projectName}\\.${environment}\\..*`,
            description: 'Topic pattern for cross-region replication',
            exportName: `${projectName}-${environment}-${regionType}-replication-topic-pattern`
        });
    }
}