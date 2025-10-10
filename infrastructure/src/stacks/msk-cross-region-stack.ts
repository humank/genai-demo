import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as cloudwatchActions from 'aws-cdk-lib/aws-cloudwatch-actions';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as eks from 'aws-cdk-lib/aws-eks';
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
    readonly eksCluster: eks.ICluster;
    readonly primaryMskCluster: msk.CfnCluster;
    readonly secondaryMskCluster?: msk.CfnCluster;
    readonly primaryRegion: string;
    readonly secondaryRegion: string;
    readonly regionType: 'primary' | 'secondary';
    readonly alertingTopic: sns.ITopic;
}

export class MSKCrossRegionStack extends cdk.Stack {
    public readonly mirrorMakerDeployment: eks.KubernetesManifest;
    public readonly mirrorMakerService: eks.KubernetesManifest;
    public readonly mirrorMakerServiceAccount: eks.ServiceAccount;
    public readonly mirrorMakerLogGroup: logs.LogGroup;
    public readonly replicationMonitoring: cloudwatch.Dashboard;

    constructor(scope: Construct, id: string, props: MSKCrossRegionStackProps) {
        super(scope, id, props);

        const {
            environment,
            projectName,
            eksCluster,
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
            // Create Service Account for MirrorMaker on EKS
            this.mirrorMakerServiceAccount = this.createMirrorMakerServiceAccount(projectName, environment, eksCluster);

            // Create log group for MirrorMaker
            this.mirrorMakerLogGroup = this.createMirrorMakerLogGroup(projectName, environment);

            // Deploy MirrorMaker 2.0 deployment and service on EKS
            this.mirrorMakerDeployment = this.createMirrorMakerDeployment(
                projectName,
                environment,
                eksCluster,
                primaryMskCluster,
                secondaryMskCluster,
                primaryRegion,
                secondaryRegion,
                regionType
            );

            this.mirrorMakerService = this.createMirrorMakerKubernetesService(
                projectName,
                environment,
                eksCluster
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

    private createMirrorMakerServiceAccount(projectName: string, environment: string, eksCluster: eks.ICluster): eks.ServiceAccount {
        const serviceAccount = eksCluster.addServiceAccount('MirrorMakerServiceAccount', {
            name: `mirrormaker-${environment}`,
            namespace: 'default',
        });

        // Add necessary IAM permissions for cross-region MSK access
        serviceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'kafka-cluster:Connect',
                'kafka-cluster:AlterCluster',
                'kafka-cluster:DescribeCluster',
                'kafka-cluster:*Topic*',
                'kafka-cluster:WriteData',
                'kafka-cluster:ReadData',
                'kafka-cluster:AlterGroup',
                'kafka-cluster:DescribeGroup',
            ],
            resources: ['*'], // Will be restricted to specific MSK clusters
        }));

        cdk.Tags.of(serviceAccount).add('Name', `${projectName}-${environment}-mirrormaker-sa`);
        cdk.Tags.of(serviceAccount).add('Service', 'MirrorMaker');

        return serviceAccount;
    }

    private createMirrorMakerLogGroup(projectName: string, environment: string): logs.LogGroup {
        const logGroup = new logs.LogGroup(this, 'MirrorMakerLogGroup', {
            logGroupName: `/aws/eks/mirrormaker/${projectName}-${environment}`,
            retention: environment === 'production' ? logs.RetentionDays.ONE_MONTH : logs.RetentionDays.ONE_WEEK,
            removalPolicy: environment === 'production' ? cdk.RemovalPolicy.RETAIN : cdk.RemovalPolicy.DESTROY
        });

        cdk.Tags.of(logGroup).add('Name', `${projectName}-${environment}-mirrormaker-logs`);
        cdk.Tags.of(logGroup).add('LogType', 'MirrorMaker');

        return logGroup;
    }

    private createMirrorMakerDeployment(
        projectName: string,
        environment: string,
        eksCluster: eks.ICluster,
        primaryMskCluster: msk.CfnCluster,
        secondaryMskCluster: msk.CfnCluster | undefined,
        primaryRegion: string,
        secondaryRegion: string,
        regionType: 'primary' | 'secondary'
    ): eks.KubernetesManifest {
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

        // Create Kubernetes Deployment for MirrorMaker
        const deployment = new eks.KubernetesManifest(this, 'MirrorMakerDeployment', {
            cluster: eksCluster,
            manifest: [{
                apiVersion: 'apps/v1',
                kind: 'Deployment',
                metadata: {
                    name: `mirrormaker-${environment}`,
                    namespace: 'default',
                    labels: {
                        app: 'mirrormaker',
                        environment: environment,
                        region: regionType,
                        project: projectName
                    }
                },
                spec: {
                    replicas: 2, // High availability
                    selector: {
                        matchLabels: {
                            app: 'mirrormaker',
                            environment: environment
                        }
                    },
                    template: {
                        metadata: {
                            labels: {
                                app: 'mirrormaker',
                                environment: environment,
                                region: regionType
                            }
                        },
                        spec: {
                            serviceAccountName: `mirrormaker-${environment}`,
                            containers: [{
                                name: 'mirrormaker',
                                image: 'confluentinc/cp-kafka-connect:7.4.0',
                                ports: [{
                                    containerPort: 8083,
                                    name: 'connect-rest'
                                }],
                                env: [
                                    { name: 'CONNECT_BOOTSTRAP_SERVERS', value: cdk.Fn.getAtt(primaryMskCluster.logicalId, 'BootstrapBrokerStringSaslIam').toString() },
                                    { name: 'CONNECT_REST_ADVERTISED_HOST_NAME', value: 'localhost' },
                                    { name: 'CONNECT_REST_PORT', value: '8083' },
                                    { name: 'CONNECT_GROUP_ID', value: `${projectName}-${environment}-mirrormaker-group` },
                                    { name: 'CONNECT_CONFIG_STORAGE_TOPIC', value: `${projectName}.${environment}.mirrormaker.configs` },
                                    { name: 'CONNECT_OFFSET_STORAGE_TOPIC', value: `${projectName}.${environment}.mirrormaker.offsets` },
                                    { name: 'CONNECT_STATUS_STORAGE_TOPIC', value: `${projectName}.${environment}.mirrormaker.status` },
                                    { name: 'CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR', value: '3' },
                                    { name: 'CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR', value: '3' },
                                    { name: 'CONNECT_STATUS_STORAGE_REPLICATION_FACTOR', value: '3' },
                                    { name: 'CONNECT_KEY_CONVERTER', value: 'org.apache.kafka.connect.storage.StringConverter' },
                                    { name: 'CONNECT_VALUE_CONVERTER', value: 'org.apache.kafka.connect.json.JsonConverter' },
                                    { name: 'CONNECT_VALUE_CONVERTER_SCHEMAS_ENABLE', value: 'false' },
                                    { name: 'CONNECT_INTERNAL_KEY_CONVERTER', value: 'org.apache.kafka.connect.json.JsonConverter' },
                                    { name: 'CONNECT_INTERNAL_VALUE_CONVERTER', value: 'org.apache.kafka.connect.json.JsonConverter' },
                                    { name: 'CONNECT_INTERNAL_KEY_CONVERTER_SCHEMAS_ENABLE', value: 'false' },
                                    { name: 'CONNECT_INTERNAL_VALUE_CONVERTER_SCHEMAS_ENABLE', value: 'false' },
                                    { name: 'CONNECT_LOG4J_ROOT_LOGLEVEL', value: 'INFO' },
                                    { name: 'CONNECT_LOG4J_LOGGERS', value: 'org.apache.kafka.connect.runtime.rest=WARN,org.reflections=ERROR' },
                                    { name: 'CONNECT_PLUGIN_PATH', value: '/usr/share/java,/usr/share/confluent-hub-components' },
                                    { name: 'CONNECT_SECURITY_PROTOCOL', value: 'SASL_SSL' },
                                    { name: 'CONNECT_SASL_MECHANISM', value: 'AWS_MSK_IAM' },
                                    { name: 'CONNECT_SASL_JAAS_CONFIG', value: 'software.amazon.msk.auth.iam.IAMLoginModule required;' },
                                    { name: 'CONNECT_SASL_CLIENT_CALLBACK_HANDLER_CLASS', value: 'software.amazon.msk.auth.iam.IAMClientCallbackHandler' },
                                    { name: 'CONNECT_PRODUCER_SECURITY_PROTOCOL', value: 'SASL_SSL' },
                                    { name: 'CONNECT_PRODUCER_SASL_MECHANISM', value: 'AWS_MSK_IAM' },
                                    { name: 'CONNECT_PRODUCER_SASL_JAAS_CONFIG', value: 'software.amazon.msk.auth.iam.IAMLoginModule required;' },
                                    { name: 'CONNECT_PRODUCER_SASL_CLIENT_CALLBACK_HANDLER_CLASS', value: 'software.amazon.msk.auth.iam.IAMClientCallbackHandler' },
                                    { name: 'CONNECT_CONSUMER_SECURITY_PROTOCOL', value: 'SASL_SSL' },
                                    { name: 'CONNECT_CONSUMER_SASL_MECHANISM', value: 'AWS_MSK_IAM' },
                                    { name: 'CONNECT_CONSUMER_SASL_JAAS_CONFIG', value: 'software.amazon.msk.auth.iam.IAMLoginModule required;' },
                                    { name: 'CONNECT_CONSUMER_SASL_CLIENT_CALLBACK_HANDLER_CLASS', value: 'software.amazon.msk.auth.iam.IAMClientCallbackHandler' },
                                    { name: 'MIRRORMAKER_CONFIG', value: mirrorMakerConfig },
                                    { name: 'AWS_REGION', value: this.region },
                                    { name: 'ENVIRONMENT', value: environment }
                                ],
                                resources: {
                                    requests: {
                                        cpu: '1000m',
                                        memory: '2Gi'
                                    },
                                    limits: {
                                        cpu: '2000m',
                                        memory: '4Gi'
                                    }
                                },
                                livenessProbe: {
                                    httpGet: {
                                        path: '/connectors',
                                        port: 8083
                                    },
                                    initialDelaySeconds: 60,
                                    periodSeconds: 30,
                                    timeoutSeconds: 10,
                                    failureThreshold: 3
                                },
                                readinessProbe: {
                                    httpGet: {
                                        path: '/connectors',
                                        port: 8083
                                    },
                                    initialDelaySeconds: 30,
                                    periodSeconds: 10,
                                    timeoutSeconds: 5,
                                    failureThreshold: 3
                                }
                            }]
                        }
                    }
                }
            }]
        });

        return deployment;
    }

    private createMirrorMakerKubernetesService(
        projectName: string,
        environment: string,
        eksCluster: eks.ICluster
    ): eks.KubernetesManifest {
        const service = new eks.KubernetesManifest(this, 'MirrorMakerKubernetesService', {
            cluster: eksCluster,
            manifest: [{
                apiVersion: 'v1',
                kind: 'Service',
                metadata: {
                    name: `mirrormaker-${environment}`,
                    namespace: 'default',
                    labels: {
                        app: 'mirrormaker',
                        environment: environment,
                        project: projectName
                    }
                },
                spec: {
                    selector: {
                        app: 'mirrormaker',
                        environment: environment
                    },
                    ports: [{
                        port: 8083,
                        targetPort: 8083,
                        protocol: 'TCP',
                        name: 'connect-rest'
                    }],
                    type: 'ClusterIP'
                }
            }]
        });

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

        // Add MirrorMaker service metrics (EKS-based)
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'MirrorMaker Service Health',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/EKS',
                        metricName: 'cluster_node_count',
                        dimensionsMap: {
                            ClusterName: `${projectName}-${environment}-eks`
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5)
                    })
                ],
                right: [
                    new cloudwatch.Metric({
                        namespace: `${projectName}/${environment}/MirrorMaker`,
                        metricName: 'PodCount',
                        dimensionsMap: {
                            Deployment: `mirrormaker-${environment}`,
                            Namespace: 'default'
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
        // MirrorMaker service health alarm (EKS-based)
        const serviceHealthAlarm = new cloudwatch.Alarm(this, 'MirrorMakerServiceHealthAlarm', {
            alarmName: `${projectName}-${environment}-mirrormaker-service-unhealthy`,
            alarmDescription: 'MirrorMaker service is unhealthy',
            metric: new cloudwatch.Metric({
                namespace: `${projectName}/${environment}/MirrorMaker`,
                metricName: 'PodCount',
                dimensionsMap: {
                    Deployment: `mirrormaker-${environment}`,
                    Namespace: 'default'
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
            stringValue: `mirrormaker-${environment}.default.svc.cluster.local:8083`,
            description: `MirrorMaker service endpoint for ${projectName} ${environment}`,
            tier: ssm.ParameterTier.STANDARD
        });
    }

    private createOutputs(projectName: string, environment: string, regionType: 'primary' | 'secondary'): void {
        // MirrorMaker service outputs (EKS-based)
        new cdk.CfnOutput(this, 'MirrorMakerDeploymentName', {
            value: `mirrormaker-${environment}`,
            description: 'MirrorMaker EKS deployment name',
            exportName: `${projectName}-${environment}-${regionType}-mirrormaker-deployment-name`
        });

        new cdk.CfnOutput(this, 'MirrorMakerServiceName', {
            value: `mirrormaker-${environment}`,
            description: 'MirrorMaker Kubernetes service name',
            exportName: `${projectName}-${environment}-${regionType}-mirrormaker-service-name`
        });

        new cdk.CfnOutput(this, 'MirrorMakerEndpoint', {
            value: `mirrormaker-${environment}.default.svc.cluster.local:8083`,
            description: 'MirrorMaker service endpoint',
            exportName: `${projectName}-${environment}-${regionType}-mirrormaker-endpoint`
        });

        new cdk.CfnOutput(this, 'MirrorMakerServiceAccountArn', {
            value: this.mirrorMakerServiceAccount.serviceAccountName,
            description: 'MirrorMaker service account ARN',
            exportName: `${projectName}-${environment}-${regionType}-mirrormaker-service-account-arn`
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