import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as cloudwatchActions from 'aws-cdk-lib/aws-cloudwatch-actions';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as kms from 'aws-cdk-lib/aws-kms';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as msk from 'aws-cdk-lib/aws-msk';
import * as sns from 'aws-cdk-lib/aws-sns';
import { Construct } from 'constructs';

export interface MSKStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly vpc: ec2.IVpc;
    readonly mskSecurityGroup: ec2.ISecurityGroup;
    readonly kmsKey: kms.IKey;
    readonly alertingTopic: sns.ITopic;
    readonly region?: string;
}

export class MSKStack extends cdk.Stack {
    public readonly mskCluster: msk.CfnCluster;
    public readonly mskConfiguration: msk.CfnConfiguration;
    public readonly mskClusterPolicy: msk.CfnClusterPolicy;
    public readonly mskConnectRole: iam.Role;
    public readonly mskConnectLogGroup: logs.LogGroup;

    constructor(scope: Construct, id: string, props: MSKStackProps) {
        super(scope, id, props);

        const { environment, projectName, vpc, mskSecurityGroup, kmsKey, alertingTopic, region } = props;

        // Apply common tags
        const commonTags = {
            Project: projectName,
            Environment: environment,
            ManagedBy: 'AWS-CDK',
            Component: 'MSK',
            Service: 'Messaging'
        };

        Object.entries(commonTags).forEach(([key, value]) => {
            cdk.Tags.of(this).add(key, value);
        });

        // Get environment-specific configuration
        const envConfig = this.node.tryGetContext('genai-demo:environments')?.[environment] || {};
        const mskConfig = envConfig['msk'] || {};

        // Create MSK Configuration
        this.mskConfiguration = this.createMSKConfiguration(projectName, environment, mskConfig);

        // Create MSK Cluster
        this.mskCluster = this.createMSKCluster(
            projectName,
            environment,
            vpc,
            mskSecurityGroup,
            kmsKey,
            this.mskConfiguration,
            mskConfig
        );

        // Create MSK Cluster Policy for security
        this.mskClusterPolicy = this.createMSKClusterPolicy(projectName, environment);

        // Create MSK Connect resources
        const mskConnectResources = this.createMSKConnectResources(projectName, environment, vpc);
        this.mskConnectRole = mskConnectResources.role;
        this.mskConnectLogGroup = mskConnectResources.logGroup;

        // Set up monitoring and alerting
        this.setupMonitoringAndAlerting(projectName, environment, alertingTopic);

        // Create outputs
        this.createOutputs(projectName, environment);
    }

    private createMSKConfiguration(
        projectName: string,
        environment: string,
        mskConfig: any
    ): msk.CfnConfiguration {
        // Kafka configuration optimized for domain events
        const kafkaConfig = [
            // Performance and reliability settings
            'auto.create.topics.enable=false',
            'default.replication.factor=3',
            'min.insync.replicas=2',
            'num.partitions=6',
            'offsets.topic.replication.factor=3',
            'transaction.state.log.replication.factor=3',
            'transaction.state.log.min.isr=2',

            // Log retention for domain events (7 days default, 30 days for production)
            `log.retention.hours=${environment === 'production' ? 720 : 168}`,
            'log.retention.bytes=1073741824', // 1GB per partition
            'log.segment.bytes=104857600', // 100MB segments

            // Compression for better throughput
            'compression.type=snappy',

            // Security settings
            'ssl.client.auth=none',
            'security.inter.broker.protocol=TLS',

            // Performance tuning
            'num.network.threads=8',
            'num.io.threads=16',
            'socket.send.buffer.bytes=102400',
            'socket.receive.buffer.bytes=102400',
            'socket.request.max.bytes=104857600',

            // JVM settings for better performance
            'jvm.performance.opts=-server -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35',

            // Monitoring and metrics
            'jmx.port=11001',
            'enable.metrics.push=true'
        ].join('\n');

        const configuration = new msk.CfnConfiguration(this, 'MSKConfiguration', {
            name: `${projectName}-${environment}-msk-config`,
            description: `MSK configuration for ${projectName} ${environment} environment - optimized for domain events`,
            kafkaVersionsList: ['2.8.1'],
            serverProperties: kafkaConfig
        });

        // Add tags
        cdk.Tags.of(configuration).add('Name', `${projectName}-${environment}-msk-config`);
        cdk.Tags.of(configuration).add('ConfigurationType', 'MSK');

        return configuration;
    }

    private createMSKCluster(
        projectName: string,
        environment: string,
        vpc: ec2.IVpc,
        mskSecurityGroup: ec2.ISecurityGroup,
        kmsKey: kms.IKey,
        configuration: msk.CfnConfiguration,
        mskConfig: any
    ): msk.CfnCluster {
        // Get broker instance type based on environment
        const brokerInstanceType = mskConfig['broker-instance-type'] ||
            (environment === 'production' ? 'kafka.m5.large' : 'kafka.t3.small');

        const numberOfBrokers = mskConfig['number-of-brokers'] ||
            (environment === 'production' ? 3 : 2);

        const storageSize = mskConfig['storage-size'] ||
            (environment === 'production' ? 100 : 20);

        // Create log group for MSK cluster logs
        const mskLogGroup = new logs.LogGroup(this, 'MSKClusterLogGroup', {
            logGroupName: `/aws/msk/cluster/${projectName}-${environment}`,
            retention: environment === 'production' ? logs.RetentionDays.ONE_MONTH : logs.RetentionDays.ONE_WEEK,
            removalPolicy: environment === 'production' ? cdk.RemovalPolicy.RETAIN : cdk.RemovalPolicy.DESTROY
        });

        // Get private subnets for MSK deployment
        const privateSubnets = vpc.privateSubnets.slice(0, Math.min(numberOfBrokers, vpc.privateSubnets.length));

        const cluster = new msk.CfnCluster(this, 'MSKCluster', {
            clusterName: `${projectName}-${environment}-msk`,
            kafkaVersion: '2.8.1',
            numberOfBrokerNodes: numberOfBrokers,

            // Broker node configuration
            brokerNodeGroupInfo: {
                instanceType: brokerInstanceType,
                clientSubnets: privateSubnets.map(subnet => subnet.subnetId),
                securityGroups: [mskSecurityGroup.securityGroupId],

                // Storage configuration
                storageInfo: {
                    ebsStorageInfo: {
                        volumeSize: storageSize,
                        provisionedThroughput: environment === 'production' ? {
                            enabled: true,
                            volumeThroughput: 250
                        } : undefined
                    }
                },

                // Connectivity configuration
                connectivityInfo: {
                    publicAccess: {
                        type: 'DISABLED'
                    }
                }
            },

            // Configuration reference
            configurationInfo: {
                arn: configuration.attrArn,
                revision: 1
            },

            // Encryption configuration
            encryptionInfo: {
                encryptionAtRest: {
                    dataVolumeKmsKeyId: kmsKey.keyId
                },
                encryptionInTransit: {
                    clientBroker: 'TLS',
                    inCluster: true
                }
            },

            // Enhanced monitoring
            enhancedMonitoring: environment === 'production' ? 'PER_TOPIC_PER_BROKER' : 'PER_BROKER',

            // Client authentication
            clientAuthentication: {
                sasl: {
                    iam: {
                        enabled: true
                    }
                },
                tls: {
                    enabled: false // We'll use IAM authentication
                }
            },

            // Logging configuration
            loggingInfo: {
                brokerLogs: {
                    cloudWatchLogs: {
                        enabled: true,
                        logGroup: mskLogGroup.logGroupName
                    },
                    firehose: {
                        enabled: false
                    },
                    s3: {
                        enabled: false
                    }
                }
            },

            // Open monitoring with Prometheus
            openMonitoring: {
                prometheus: {
                    jmxExporter: {
                        enabledInBroker: true
                    },
                    nodeExporter: {
                        enabledInBroker: true
                    }
                }
            }
        });

        // Add tags
        cdk.Tags.of(cluster).add('Name', `${projectName}-${environment}-msk`);
        cdk.Tags.of(cluster).add('BrokerInstanceType', brokerInstanceType);
        cdk.Tags.of(cluster).add('NumberOfBrokers', numberOfBrokers.toString());
        cdk.Tags.of(cluster).add('StorageSize', `${storageSize}GB`);

        return cluster;
    }

    private createMSKClusterPolicy(
        projectName: string,
        environment: string
    ): msk.CfnClusterPolicy {
        // Create IAM policy for MSK cluster access
        const policyDocument = {
            Version: '2012-10-17',
            Statement: [
                {
                    Sid: 'AllowEKSAccess',
                    Effect: 'Allow',
                    Principal: {
                        AWS: `arn:aws:iam::${this.account}:root`
                    },
                    Action: [
                        'kafka-cluster:Connect',
                        'kafka-cluster:AlterCluster',
                        'kafka-cluster:DescribeCluster'
                    ],
                    Resource: `arn:aws:kafka:${this.region}:${this.account}:cluster/${projectName}-${environment}-msk/*`
                },
                {
                    Sid: 'AllowTopicAccess',
                    Effect: 'Allow',
                    Principal: {
                        AWS: `arn:aws:iam::${this.account}:root`
                    },
                    Action: [
                        'kafka-cluster:CreateTopic',
                        'kafka-cluster:DeleteTopic',
                        'kafka-cluster:AlterTopic',
                        'kafka-cluster:DescribeTopic'
                    ],
                    Resource: `arn:aws:kafka:${this.region}:${this.account}:topic/${projectName}-${environment}-msk/*`
                },
                {
                    Sid: 'AllowGroupAccess',
                    Effect: 'Allow',
                    Principal: {
                        AWS: `arn:aws:iam::${this.account}:root`
                    },
                    Action: [
                        'kafka-cluster:AlterGroup',
                        'kafka-cluster:DescribeGroup'
                    ],
                    Resource: `arn:aws:kafka:${this.region}:${this.account}:group/${projectName}-${environment}-msk/*`
                },
                {
                    Sid: 'AllowDataAccess',
                    Effect: 'Allow',
                    Principal: {
                        AWS: `arn:aws:iam::${this.account}:root`
                    },
                    Action: [
                        'kafka-cluster:ReadData',
                        'kafka-cluster:WriteData'
                    ],
                    Resource: [
                        `arn:aws:kafka:${this.region}:${this.account}:topic/${projectName}-${environment}-msk/*`,
                        `arn:aws:kafka:${this.region}:${this.account}:group/${projectName}-${environment}-msk/*`
                    ]
                }
            ]
        };

        const clusterPolicy = new msk.CfnClusterPolicy(this, 'MSKClusterPolicy', {
            clusterArn: cdk.Fn.getAtt(this.mskCluster.logicalId, 'Arn').toString(),
            policy: policyDocument
        });

        return clusterPolicy;
    }

    private createMSKConnectResources(
        projectName: string,
        environment: string,
        vpc: ec2.IVpc
    ): { role: iam.Role; logGroup: logs.LogGroup } {
        // Create IAM role for MSK Connect
        const mskConnectRole = new iam.Role(this, 'MSKConnectRole', {
            roleName: `${projectName}-${environment}-msk-connect-role`,
            assumedBy: new iam.ServicePrincipal('kafkaconnect.amazonaws.com'),
            description: 'IAM role for MSK Connect service'
        });

        // Add policies for MSK Connect
        mskConnectRole.addManagedPolicy(
            iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonMSKConnectReadOnlyAccess')
        );

        mskConnectRole.addToPolicy(new iam.PolicyStatement({
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
                `arn:aws:kafka:${this.region}:${this.account}:cluster/${projectName}-${environment}-msk/*`,
                `arn:aws:kafka:${this.region}:${this.account}:topic/${projectName}-${environment}-msk/*`,
                `arn:aws:kafka:${this.region}:${this.account}:group/${projectName}-${environment}-msk/*`
            ]
        }));

        // Add S3 permissions for data streaming
        mskConnectRole.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                's3:GetObject',
                's3:PutObject',
                's3:DeleteObject',
                's3:ListBucket'
            ],
            resources: [
                `arn:aws:s3:::${projectName}-${environment}-*`,
                `arn:aws:s3:::${projectName}-${environment}-*/*`
            ]
        }));

        // Add CloudWatch Logs permissions
        mskConnectRole.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'logs:CreateLogGroup',
                'logs:CreateLogStream',
                'logs:PutLogEvents',
                'logs:DescribeLogGroups',
                'logs:DescribeLogStreams'
            ],
            resources: [
                `arn:aws:logs:${this.region}:${this.account}:log-group:/aws/msk/connect/${projectName}-${environment}*`
            ]
        }));

        // Create log group for MSK Connect
        const mskConnectLogGroup = new logs.LogGroup(this, 'MSKConnectLogGroup', {
            logGroupName: `/aws/msk/connect/${projectName}-${environment}`,
            retention: environment === 'production' ? logs.RetentionDays.ONE_MONTH : logs.RetentionDays.ONE_WEEK,
            removalPolicy: environment === 'production' ? cdk.RemovalPolicy.RETAIN : cdk.RemovalPolicy.DESTROY
        });

        // Add tags
        cdk.Tags.of(mskConnectRole).add('Name', `${projectName}-${environment}-msk-connect-role`);
        cdk.Tags.of(mskConnectRole).add('Service', 'MSKConnect');
        cdk.Tags.of(mskConnectLogGroup).add('Name', `${projectName}-${environment}-msk-connect-logs`);
        cdk.Tags.of(mskConnectLogGroup).add('LogType', 'MSKConnect');

        return {
            role: mskConnectRole,
            logGroup: mskConnectLogGroup
        };
    }

    private setupMonitoringAndAlerting(
        projectName: string,
        environment: string,
        alertingTopic: sns.ITopic
    ): void {
        // Create CloudWatch alarms for MSK cluster monitoring
        const alarms = [
            {
                name: 'MSKClusterCPUUtilization',
                metric: new cloudwatch.Metric({
                    namespace: 'AWS/Kafka',
                    metricName: 'CpuUser',
                    dimensionsMap: {
                        'Cluster Name': `${projectName}-${environment}-msk`
                    },
                    statistic: 'Average',
                    period: cdk.Duration.minutes(5)
                }),
                threshold: 80,
                comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
                description: 'MSK cluster CPU utilization is high'
            },
            {
                name: 'MSKClusterMemoryUtilization',
                metric: new cloudwatch.Metric({
                    namespace: 'AWS/Kafka',
                    metricName: 'MemoryUsed',
                    dimensionsMap: {
                        'Cluster Name': `${projectName}-${environment}-msk`
                    },
                    statistic: 'Average',
                    period: cdk.Duration.minutes(5)
                }),
                threshold: 85,
                comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
                description: 'MSK cluster memory utilization is high'
            },
            {
                name: 'MSKClusterDiskUtilization',
                metric: new cloudwatch.Metric({
                    namespace: 'AWS/Kafka',
                    metricName: 'KafkaDataLogsDiskUsed',
                    dimensionsMap: {
                        'Cluster Name': `${projectName}-${environment}-msk`
                    },
                    statistic: 'Average',
                    period: cdk.Duration.minutes(5)
                }),
                threshold: 80,
                comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
                description: 'MSK cluster disk utilization is high'
            },
            {
                name: 'MSKProducerRequestRate',
                metric: new cloudwatch.Metric({
                    namespace: 'AWS/Kafka',
                    metricName: 'ProduceMessageConversionsPerSec',
                    dimensionsMap: {
                        'Cluster Name': `${projectName}-${environment}-msk`
                    },
                    statistic: 'Sum',
                    period: cdk.Duration.minutes(5)
                }),
                threshold: 1000,
                comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
                description: 'MSK producer request rate is high'
            },
            {
                name: 'MSKConsumerLag',
                metric: new cloudwatch.Metric({
                    namespace: 'AWS/Kafka',
                    metricName: 'EstimatedMaxTimeLag',
                    dimensionsMap: {
                        'Cluster Name': `${projectName}-${environment}-msk`
                    },
                    statistic: 'Maximum',
                    period: cdk.Duration.minutes(5)
                }),
                threshold: 60000, // 60 seconds
                comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
                description: 'MSK consumer lag is high'
            }
        ];

        alarms.forEach(alarmConfig => {
            const alarm = new cloudwatch.Alarm(this, alarmConfig.name, {
                alarmName: `${projectName}-${environment}-${alarmConfig.name}`,
                alarmDescription: alarmConfig.description,
                metric: alarmConfig.metric,
                threshold: alarmConfig.threshold,
                comparisonOperator: alarmConfig.comparisonOperator,
                evaluationPeriods: 2,
                treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
            });

            // Add SNS action for alerting
            alarm.addAlarmAction(new cloudwatchActions.SnsAction(alertingTopic));

            // Add tags
            cdk.Tags.of(alarm).add('Name', `${projectName}-${environment}-${alarmConfig.name}`);
            cdk.Tags.of(alarm).add('AlarmType', 'MSK');
            cdk.Tags.of(alarm).add('Severity', 'High');
        });

        // Create custom metric for domain events processing
        const domainEventsMetric = new cloudwatch.Metric({
            namespace: `${projectName}/${environment}/DomainEvents`,
            metricName: 'EventsProcessed',
            statistic: 'Sum',
            period: cdk.Duration.minutes(5)
        });

        const domainEventsAlarm = new cloudwatch.Alarm(this, 'DomainEventsProcessingFailure', {
            alarmName: `${projectName}-${environment}-DomainEventsProcessingFailure`,
            alarmDescription: 'Domain events processing has stopped or failed',
            metric: domainEventsMetric,
            threshold: 1,
            comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
            evaluationPeriods: 3,
            treatMissingData: cloudwatch.TreatMissingData.BREACHING
        });

        domainEventsAlarm.addAlarmAction(new cloudwatchActions.SnsAction(alertingTopic));

        // Add tags
        cdk.Tags.of(domainEventsAlarm).add('Name', `${projectName}-${environment}-DomainEventsProcessingFailure`);
        cdk.Tags.of(domainEventsAlarm).add('AlarmType', 'BusinessLogic');
        cdk.Tags.of(domainEventsAlarm).add('Severity', 'Critical');
    }

    private createOutputs(projectName: string, environment: string): void {
        // MSK Cluster Outputs
        new cdk.CfnOutput(this, 'MSKClusterArn', {
            value: cdk.Fn.getAtt(this.mskCluster.logicalId, 'Arn').toString(),
            description: 'MSK cluster ARN',
            exportName: `${projectName}-${environment}-msk-cluster-arn`
        });

        new cdk.CfnOutput(this, 'MSKClusterName', {
            value: this.mskCluster.clusterName!,
            description: 'MSK cluster name',
            exportName: `${projectName}-${environment}-msk-cluster-name`
        });

        new cdk.CfnOutput(this, 'MSKBootstrapServers', {
            value: cdk.Fn.getAtt(this.mskCluster.logicalId, 'BootstrapBrokerString').toString(),
            description: 'MSK bootstrap servers (plaintext)',
            exportName: `${projectName}-${environment}-msk-bootstrap-servers`
        });

        new cdk.CfnOutput(this, 'MSKBootstrapServersTLS', {
            value: cdk.Fn.getAtt(this.mskCluster.logicalId, 'BootstrapBrokerStringTls').toString(),
            description: 'MSK bootstrap servers (TLS)',
            exportName: `${projectName}-${environment}-msk-bootstrap-servers-tls`
        });

        new cdk.CfnOutput(this, 'MSKBootstrapServersIAM', {
            value: cdk.Fn.getAtt(this.mskCluster.logicalId, 'BootstrapBrokerStringSaslIam').toString(),
            description: 'MSK bootstrap servers (SASL/IAM)',
            exportName: `${projectName}-${environment}-msk-bootstrap-servers-iam`
        });

        new cdk.CfnOutput(this, 'MSKZookeeperConnectString', {
            value: cdk.Fn.getAtt(this.mskCluster.logicalId, 'ZookeeperConnectString').toString(),
            description: 'MSK Zookeeper connection string',
            exportName: `${projectName}-${environment}-msk-zookeeper-connect`
        });

        // MSK Configuration Outputs
        new cdk.CfnOutput(this, 'MSKConfigurationArn', {
            value: cdk.Fn.getAtt(this.mskConfiguration.logicalId, 'Arn').toString(),
            description: 'MSK configuration ARN',
            exportName: `${projectName}-${environment}-msk-config-arn`
        });

        // MSK Connect Outputs
        new cdk.CfnOutput(this, 'MSKConnectRoleArn', {
            value: this.mskConnectRole.roleArn,
            description: 'MSK Connect service role ARN',
            exportName: `${projectName}-${environment}-msk-connect-role-arn`
        });

        new cdk.CfnOutput(this, 'MSKConnectLogGroupName', {
            value: this.mskConnectLogGroup.logGroupName,
            description: 'MSK Connect log group name',
            exportName: `${projectName}-${environment}-msk-connect-log-group`
        });

        // Domain Events Topic Configuration
        new cdk.CfnOutput(this, 'DomainEventsTopicPrefix', {
            value: `${projectName}.${environment}`,
            description: 'Prefix for domain events topics',
            exportName: `${projectName}-${environment}-domain-events-topic-prefix`
        });

        // Kafka Topics for Domain Events
        const domainEventTopics = [
            // 現有業務領域事件
            'customer.created',
            'customer.updated',
            'order.created',
            'order.confirmed',
            'order.cancelled',
            'payment.processed',
            'payment.failed',
            'inventory.reserved',
            'inventory.released',
            'product.created',
            'product.updated',
            
            // 新增可觀測性事件
            'observability.user.behavior',
            'observability.performance.metrics',
            'observability.business.analytics'
        ];

        new cdk.CfnOutput(this, 'DomainEventsTopics', {
            value: domainEventTopics.map(topic => `${projectName}.${environment}.${topic}`).join(','),
            description: 'List of domain events topics',
            exportName: `${projectName}-${environment}-domain-events-topics`
        });

        // Dead Letter Queue Topics for Observability Events
        const observabilityDLQTopics = [
            'observability.user.behavior.dlq',
            'observability.performance.metrics.dlq',
            'observability.business.analytics.dlq'
        ];

        new cdk.CfnOutput(this, 'ObservabilityDLQTopics', {
            value: observabilityDLQTopics.map(topic => `${projectName}.${environment}.${topic}`).join(','),
            description: 'List of observability dead letter queue topics',
            exportName: `${projectName}-${environment}-observability-dlq-topics`
        });

        // Observability Topics Configuration
        new cdk.CfnOutput(this, 'ObservabilityTopicsConfig', {
            value: JSON.stringify({
                'user-behavior': `${projectName}.${environment}.observability.user.behavior`,
                'performance-metrics': `${projectName}.${environment}.observability.performance.metrics`,
                'business-analytics': `${projectName}.${environment}.observability.business.analytics`,
                'user-behavior-dlq': `${projectName}.${environment}.observability.user.behavior.dlq`,
                'performance-metrics-dlq': `${projectName}.${environment}.observability.performance.metrics.dlq`,
                'business-analytics-dlq': `${projectName}.${environment}.observability.business.analytics.dlq`
            }),
            description: 'Observability topics configuration mapping',
            exportName: `${projectName}-${environment}-observability-topics-config`
        });

        // Spring Boot Configuration Helper
        new cdk.CfnOutput(this, 'SpringBootKafkaConfig', {
            value: JSON.stringify({
                'spring.kafka.bootstrap-servers': cdk.Fn.getAtt(this.mskCluster.logicalId, 'BootstrapBrokerStringSaslIam').toString(),
                'spring.kafka.security.protocol': 'SASL_SSL',
                'spring.kafka.sasl.mechanism': 'AWS_MSK_IAM',
                'spring.kafka.sasl.jaas.config': 'software.amazon.msk.auth.iam.IAMLoginModule required;',
                'spring.kafka.sasl.client.callback.handler.class': 'software.amazon.msk.auth.iam.IAMClientCallbackHandler',
                'spring.kafka.producer.key-serializer': 'org.apache.kafka.common.serialization.StringSerializer',
                'spring.kafka.producer.value-serializer': 'org.springframework.kafka.support.serializer.JsonSerializer',
                'spring.kafka.consumer.key-deserializer': 'org.apache.kafka.common.serialization.StringDeserializer',
                'spring.kafka.consumer.value-deserializer': 'org.springframework.kafka.support.serializer.JsonDeserializer',
                'spring.kafka.consumer.group-id': `${projectName}-${environment}-consumer-group`,
                'spring.kafka.consumer.auto-offset-reset': 'earliest',
                'spring.kafka.consumer.properties.spring.json.trusted.packages': 'solid.humank.genaidemo.domain.*.events'
            }),
            description: 'Spring Boot Kafka configuration for MSK integration',
            exportName: `${projectName}-${environment}-spring-kafka-config`
        });

        // Spring Boot Observability Configuration Helper
        new cdk.CfnOutput(this, 'SpringBootObservabilityConfig', {
            value: JSON.stringify({
                'genai-demo.domain-events.topic.prefix': `${projectName}.${environment}`,
                'genai-demo.domain-events.topic.observability.user-behavior': `${projectName}.${environment}.observability.user.behavior`,
                'genai-demo.domain-events.topic.observability.performance-metrics': `${projectName}.${environment}.observability.performance.metrics`,
                'genai-demo.domain-events.topic.observability.business-analytics': `${projectName}.${environment}.observability.business.analytics`,
                'genai-demo.domain-events.publishing.dlq.enabled': 'true',
                'genai-demo.domain-events.publishing.dlq.topic-suffix': '.dlq',
                'genai-demo.events.publisher': 'kafka',
                'genai-demo.events.async': 'true'
            }),
            description: 'Spring Boot observability configuration for MSK integration',
            exportName: `${projectName}-${environment}-spring-observability-config`
        });
    }
}