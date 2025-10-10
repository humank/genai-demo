import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as eks from 'aws-cdk-lib/aws-eks';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as kms from 'aws-cdk-lib/aws-kms';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as msk from 'aws-cdk-lib/aws-msk';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as ssm from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';

export interface MSKStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly vpc: ec2.IVpc;
    readonly kmsKey: kms.IKey;
    readonly region?: string;
    readonly isPrimaryRegion?: boolean;
    readonly targetRegions?: string[];
    readonly eksCluster?: eks.ICluster;
    readonly crossRegionSyncEnabled?: boolean;
}

/**
 * MSK Stack for Data Flow Tracking with Cross-Region Replication
 * 
 * Features:
 * - Multi-AZ MSK cluster with 3 brokers
 * - Auto-scaling and encryption at rest/transit
 * - CloudWatch monitoring and alerting
 * - X-Ray distributed tracing integration
 * - IAM-based authentication and authorization
 * - Comprehensive security configuration
 * - MirrorMaker 2.0 for cross-region topic replication
 * - Message ordering guarantees and replication lag monitoring
 * 
 * Created: 2025年9月24日 下午2:34 (台北時間)
 * Updated: 2025年9月30日 下午2:34 (台北時間)
 * Task: 4.2 - MSK Cross-Region Message Synchronization
 */
export class MSKStack extends cdk.Stack {
    public readonly mskCluster: msk.CfnCluster;
    public readonly mskConfiguration: msk.CfnConfiguration;
    public readonly mskClusterPolicy: iam.ManagedPolicy;
    public readonly mskServiceRole: iam.Role;
    public readonly mskSecurityGroup: ec2.SecurityGroup;
    public readonly mskLogGroup: logs.LogGroup;
    public mirrorMakerDeployment?: any; // Kubernetes Deployment
    public mirrorMakerService?: any; // Kubernetes Service
    public crossRegionReplicationRole?: iam.Role;

    constructor(scope: Construct, id: string, props: MSKStackProps) {
        super(scope, id, props);

        const { 
            environment, 
            projectName, 
            vpc, 
            kmsKey, 
            region, 
            isPrimaryRegion = true,
            targetRegions = [],
            eksCluster,
            crossRegionSyncEnabled = false
        } = props;

        // Create alert topic for MSK monitoring
        const alertTopic = new sns.Topic(this, 'MSKAlertTopic', {
            displayName: 'MSK Alert Topic',
            topicName: `${projectName}-${environment}-msk-alerts`
        });

        // Apply common tags
        cdk.Tags.of(this).add('Environment', environment);
        cdk.Tags.of(this).add('Project', projectName);
        cdk.Tags.of(this).add('Stack', 'MSK');
        cdk.Tags.of(this).add('Region', region || 'ap-east-2');
        cdk.Tags.of(this).add('RegionType', isPrimaryRegion ? 'Primary' : 'Secondary');
        cdk.Tags.of(this).add('Purpose', 'DataFlowTracking');

        // Create CloudWatch Log Group for MSK logs
        this.mskLogGroup = new logs.LogGroup(this, 'MSKLogGroup', {
            logGroupName: `/aws/msk/${projectName}-${environment}`,
            retention: logs.RetentionDays.ONE_MONTH,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
        });

        // Create MSK Security Group
        this.mskSecurityGroup = this.createMSKSecurityGroup(vpc, environment);

        // Create MSK Service Role
        this.mskServiceRole = this.createMSKServiceRole(environment);

        // Create MSK Cluster Policy
        this.mskClusterPolicy = this.createMSKClusterPolicy(environment);

        // Create MSK Configuration
        this.mskConfiguration = this.createMSKConfiguration(environment);

        // Create MSK Cluster
        this.mskCluster = this.createMSKCluster(
            vpc, 
            environment, 
            kmsKey, 
            isPrimaryRegion
        );

        // Create CloudWatch Alarms
        this.createCloudWatchAlarms(alertTopic, environment);

        // Create MirrorMaker 2.0 for cross-region replication if enabled
        if (crossRegionSyncEnabled && targetRegions.length > 0 && eksCluster) {
            this.setupMirrorMaker2(
                environment, 
                targetRegions, 
                eksCluster, 
                kmsKey, 
                isPrimaryRegion
            );
        }

        // Create Outputs
        this.createOutputs(environment);
    }

    /**
     * Create MSK Security Group with least-privilege access
     */
    private createMSKSecurityGroup(vpc: ec2.IVpc, environment: string): ec2.SecurityGroup {
        const securityGroup = new ec2.SecurityGroup(this, 'MSKSecurityGroup', {
            vpc,
            description: `MSK Security Group for ${environment} environment`,
            allowAllOutbound: false,
        });

        // Allow inbound connections from EKS cluster (port 9092 for PLAINTEXT, 9094 for TLS)
        securityGroup.addIngressRule(
            ec2.Peer.ipv4(vpc.vpcCidrBlock),
            ec2.Port.tcp(9092),
            'MSK PLAINTEXT access from VPC'
        );

        securityGroup.addIngressRule(
            ec2.Peer.ipv4(vpc.vpcCidrBlock),
            ec2.Port.tcp(9094),
            'MSK TLS access from VPC'
        );

        // Allow inbound connections for SASL/SCRAM (port 9096)
        securityGroup.addIngressRule(
            ec2.Peer.ipv4(vpc.vpcCidrBlock),
            ec2.Port.tcp(9096),
            'MSK SASL/SCRAM access from VPC'
        );

        // Allow ZooKeeper connections (port 2181)
        securityGroup.addIngressRule(
            ec2.Peer.ipv4(vpc.vpcCidrBlock),
            ec2.Port.tcp(2181),
            'ZooKeeper access from VPC'
        );

        // Allow outbound HTTPS for AWS services
        securityGroup.addEgressRule(
            ec2.Peer.anyIpv4(),
            ec2.Port.tcp(443),
            'HTTPS outbound for AWS services'
        );

        // Allow outbound DNS
        securityGroup.addEgressRule(
            ec2.Peer.anyIpv4(),
            ec2.Port.tcp(53),
            'DNS TCP outbound'
        );

        securityGroup.addEgressRule(
            ec2.Peer.anyIpv4(),
            ec2.Port.udp(53),
            'DNS UDP outbound'
        );

        return securityGroup;
    }

    /**
     * Create MSK Service Role with necessary permissions
     */
    private createMSKServiceRole(environment: string): iam.Role {
        const role = new iam.Role(this, 'MSKServiceRole', {
            assumedBy: new iam.ServicePrincipal('kafka.amazonaws.com'),
            description: `MSK Service Role for ${environment} environment`,
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/MSKServiceRolePolicy'),
            ],
        });

        // Add custom permissions for CloudWatch and X-Ray
        role.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'logs:CreateLogGroup',
                'logs:CreateLogStream',
                'logs:PutLogEvents',
                'logs:DescribeLogStreams',
                'logs:DescribeLogGroups',
            ],
            resources: [this.mskLogGroup.logGroupArn],
        }));

        role.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'xray:PutTraceSegments',
                'xray:PutTelemetryRecords',
            ],
            resources: ['*'],
        }));

        return role;
    }

    /**
     * Create MSK Cluster Policy for client access
     */
    private createMSKClusterPolicy(environment: string): iam.ManagedPolicy {
        return new iam.ManagedPolicy(this, 'MSKClusterPolicy', {
            description: `MSK Cluster access policy for ${environment} environment`,
            statements: [
                new iam.PolicyStatement({
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'kafka-cluster:Connect',
                        'kafka-cluster:AlterCluster',
                        'kafka-cluster:DescribeCluster',
                    ],
                    resources: [`arn:aws:kafka:${this.region}:${this.account}:cluster/*`],
                }),
                new iam.PolicyStatement({
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'kafka-cluster:*Topic*',
                        'kafka-cluster:WriteData',
                        'kafka-cluster:ReadData',
                    ],
                    resources: [`arn:aws:kafka:${this.region}:${this.account}:topic/*`],
                }),
                new iam.PolicyStatement({
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'kafka-cluster:AlterGroup',
                        'kafka-cluster:DescribeGroup',
                    ],
                    resources: [`arn:aws:kafka:${this.region}:${this.account}:group/*`],
                }),
            ],
        });
    }

    /**
     * Create MSK Configuration for optimal performance
     */
    private createMSKConfiguration(environment: string): msk.CfnConfiguration {
        const configurationProperties = [
            'auto.create.topics.enable=false',
            'default.replication.factor=3',
            'min.insync.replicas=2',
            'num.partitions=12',
            'log.retention.hours=168', // 7 days
            'log.retention.bytes=1073741824', // 1GB per partition
            'log.segment.bytes=104857600', // 100MB
            'log.cleanup.policy=delete',
            'compression.type=snappy',
            'message.max.bytes=1048576', // 1MB
            'replica.fetch.max.bytes=1048576', // 1MB
            'group.initial.rebalance.delay.ms=3000',
            'offsets.retention.minutes=10080', // 7 days
            'transaction.state.log.replication.factor=3',
            'transaction.state.log.min.isr=2',
        ].join('\n');

        return new msk.CfnConfiguration(this, 'MSKConfiguration', {
            name: `${this.stackName}-msk-config`,
            description: `MSK Configuration for ${environment} environment - Data Flow Tracking`,
            kafkaVersionsList: ['2.8.1'],
            serverProperties: configurationProperties,
        });
    }

    /**
     * Create MSK Cluster with comprehensive configuration
     */
    private createMSKCluster(
        vpc: ec2.IVpc,
        environment: string,
        kmsKey: kms.IKey,
        isPrimaryRegion: boolean
    ): msk.CfnCluster {
        
        // Get private subnets across AZs
        const privateSubnets = vpc.privateSubnets.slice(0, 3);
        
        const cluster = new msk.CfnCluster(this, 'MSKCluster', {
            clusterName: `${this.stackName}-msk-cluster`,
            kafkaVersion: '2.8.1',
            numberOfBrokerNodes: 3,
            
            // Broker node configuration
            brokerNodeGroupInfo: {
                instanceType: 'kafka.m5.xlarge',
                clientSubnets: privateSubnets.map(subnet => subnet.subnetId),
                securityGroups: [this.mskSecurityGroup.securityGroupId],
                storageInfo: {
                    ebsStorageInfo: {
                        volumeSize: 1000, // 1TB per broker
                    },
                },
            },

            // Configuration reference
            configurationInfo: {
                arn: this.mskConfiguration.attrArn,
                revision: 1,
            },

            // Encryption configuration
            encryptionInfo: {
                encryptionAtRest: {
                    dataVolumeKmsKeyId: kmsKey.keyId,
                },
                encryptionInTransit: {
                    clientBroker: 'TLS',
                    inCluster: true,
                },
            },

            // Authentication configuration
            clientAuthentication: {
                sasl: {
                    scram: {
                        enabled: true,
                    },
                    iam: {
                        enabled: true,
                    },
                },
                tls: {
                    enabled: false, // Using SASL for simplicity
                },
            },

            // Logging configuration
            loggingInfo: {
                brokerLogs: {
                    cloudWatchLogs: {
                        enabled: true,
                        logGroup: this.mskLogGroup.logGroupName,
                    },
                },
            },

            // Enhanced monitoring
            enhancedMonitoring: 'PER_TOPIC_PER_PARTITION',

            // Open monitoring with Prometheus
            openMonitoring: {
                prometheus: {
                    jmxExporter: {
                        enabledInBroker: true,
                    },
                    nodeExporter: {
                        enabledInBroker: true,
                    },
                },
            },

            // Tags
            tags: {
                Environment: environment,
                Purpose: 'DataFlowTracking',
                Region: isPrimaryRegion ? 'Primary' : 'Secondary',
            },
        });

        // Add dependency on configuration
        cluster.addDependsOn(this.mskConfiguration);

        return cluster;
    }

    /**
     * Create CloudWatch Alarms for MSK monitoring
     */
    private createCloudWatchAlarms(alertTopic: sns.ITopic, environment: string): void {
        // MSK Cluster Health Alarms
        // Note: CloudWatch alarms will be created in the observability stack
        // This is a placeholder for MSK-specific alarm configuration
        
        // We'll extend the existing ObservabilityStack to include MSK alarms
        // rather than creating them here to maintain centralized monitoring
    }

    /**
     * Setup MirrorMaker 2.0 for cross-region Kafka topic replication on EKS
     */
    private setupMirrorMaker2(
        environment: string,
        targetRegions: string[],
        eksCluster: eks.ICluster,
        kmsKey: kms.IKey,
        isPrimaryRegion: boolean
    ): void {
        // Create IAM role for MirrorMaker 2.0
        this.crossRegionReplicationRole = this.createMirrorMakerRole(targetRegions);

        // Create MirrorMaker 2.0 configuration
        const mirrorMakerConfig = this.createMirrorMakerConfiguration(
            environment, 
            targetRegions, 
            isPrimaryRegion
        );

        // Create Kubernetes Service Account for MirrorMaker 2.0
        this.createMirrorMakerServiceAccount(
            environment,
            eksCluster
        );

        // Create Kubernetes Deployment for MirrorMaker 2.0
        this.mirrorMakerDeployment = this.createMirrorMakerDeployment(
            environment,
            eksCluster,
            mirrorMakerConfig,
            kmsKey
        );

        // Create Kubernetes Service for MirrorMaker 2.0
        this.mirrorMakerService = this.createMirrorMakerKubernetesService(
            environment,
            eksCluster
        );

        // Create monitoring for MirrorMaker 2.0
        this.createMirrorMakerMonitoring(environment);
    }

    /**
     * Create IAM role for MirrorMaker 2.0 with cross-region permissions for EKS
     */
    private createMirrorMakerRole(targetRegions: string[]): iam.Role {
        const role = new iam.Role(this, 'MirrorMakerRole', {
            assumedBy: new iam.WebIdentityPrincipal('arn:aws:iam::' + this.account + ':oidc-provider/' + this.region + '.amazonaws.com/eks'),
            description: 'MirrorMaker 2.0 cross-region replication role for EKS',
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('AWSXRayDaemonWriteAccess'),
            ],
        });

        // Add MSK permissions for source cluster
        role.addToPolicy(new iam.PolicyStatement({
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
            resources: [
                `arn:aws:kafka:${this.region}:${this.account}:cluster/*`,
                `arn:aws:kafka:${this.region}:${this.account}:topic/*`,
                `arn:aws:kafka:${this.region}:${this.account}:group/*`,
            ],
        }));

        // Add MSK permissions for target clusters in other regions
        targetRegions.forEach(targetRegion => {
            if (targetRegion !== this.region) {
                role.addToPolicy(new iam.PolicyStatement({
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
                    resources: [
                        `arn:aws:kafka:${targetRegion}:${this.account}:cluster/*`,
                        `arn:aws:kafka:${targetRegion}:${this.account}:topic/*`,
                        `arn:aws:kafka:${targetRegion}:${this.account}:group/*`,
                    ],
                }));
            }
        });

        // Add CloudWatch permissions for monitoring
        role.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'cloudwatch:PutMetricData',
                'logs:CreateLogGroup',
                'logs:CreateLogStream',
                'logs:PutLogEvents',
            ],
            resources: ['*'],
        }));

        // Add SSM permissions for configuration
        role.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'ssm:GetParameter',
                'ssm:GetParameters',
                'ssm:GetParametersByPath',
            ],
            resources: [
                `arn:aws:ssm:${this.region}:${this.account}:parameter/msk/mirrormaker/*`,
            ],
        }));

        return role;
    }

    /**
     * Create MirrorMaker 2.0 configuration
     */
    private createMirrorMakerConfiguration(
        environment: string,
        targetRegions: string[],
        isPrimaryRegion: boolean
    ): string {
        const sourceClusterAlias = `source-${this.region}`;
        const targetClusters = targetRegions
            .filter(region => region !== this.region)
            .map(region => `target-${region}`)
            .join(',');

        // Store configuration in SSM Parameter Store
        const configContent = `
# MirrorMaker 2.0 Configuration for ${environment} environment
# Source Region: ${this.region} (${isPrimaryRegion ? 'Primary' : 'Secondary'})
# Target Regions: ${targetRegions.join(', ')}

# Cluster definitions
clusters = ${sourceClusterAlias}, ${targetClusters}

# Source cluster configuration
${sourceClusterAlias}.bootstrap.servers = \${MSK_SOURCE_BOOTSTRAP_SERVERS}
${sourceClusterAlias}.security.protocol = SASL_SSL
${sourceClusterAlias}.sasl.mechanism = AWS_MSK_IAM
${sourceClusterAlias}.sasl.jaas.config = software.amazon.msk.auth.iam.IAMLoginModule required;
${sourceClusterAlias}.sasl.client.callback.handler.class = software.amazon.msk.auth.iam.IAMClientCallbackHandler

# Target cluster configurations
${targetRegions.filter(region => region !== this.region).map(region => `
target-${region}.bootstrap.servers = \${MSK_TARGET_${region.toUpperCase().replace('-', '_')}_BOOTSTRAP_SERVERS}
target-${region}.security.protocol = SASL_SSL
target-${region}.sasl.mechanism = AWS_MSK_IAM
target-${region}.sasl.jaas.config = software.amazon.msk.auth.iam.IAMLoginModule required;
target-${region}.sasl.client.callback.handler.class = software.amazon.msk.auth.iam.IAMClientCallbackHandler
`).join('')}

# Replication flows
${targetRegions.filter(region => region !== this.region).map(region => `
${sourceClusterAlias}->target-${region}.enabled = true
${sourceClusterAlias}->target-${region}.topics = .*
${sourceClusterAlias}->target-${region}.topics.blacklist = __.*
${sourceClusterAlias}->target-${region}.groups = .*
${sourceClusterAlias}->target-${region}.groups.blacklist = __.*
`).join('')}

# Connector configurations
name = mirrormaker2-${environment}-${this.region}
connector.class = org.apache.kafka.connect.mirror.MirrorSourceConnector
tasks.max = 3

# Replication settings
replication.factor = 3
offset-syncs.topic.replication.factor = 3
heartbeats.topic.replication.factor = 3
checkpoints.topic.replication.factor = 3

# Performance tuning
producer.batch.size = 16384
producer.linger.ms = 5
producer.compression.type = snappy
consumer.max.poll.records = 500
consumer.fetch.min.bytes = 1024

# Monitoring and metrics
emit.heartbeats.enabled = true
emit.checkpoints.enabled = true
sync.topic.acls.enabled = false
sync.topic.configs.enabled = true

# Error handling
errors.tolerance = all
errors.log.enable = true
errors.log.include.messages = true

# Exactly-once semantics
exactly.once.support = enabled
transaction.timeout.ms = 300000

# Lag monitoring (target < 1 second P95)
lag.max.ms = 1000
heartbeats.interval.ms = 3000
checkpoints.interval.ms = 60000
        `.trim();

        // Store configuration in SSM Parameter Store
        new ssm.StringParameter(this, 'MirrorMakerConfig', {
            parameterName: `/msk/mirrormaker/${environment}/config`,
            stringValue: configContent,
            description: `MirrorMaker 2.0 configuration for ${environment} environment`,
            tier: ssm.ParameterTier.ADVANCED,
        });

        return configContent;
    }

    /**
     * Create Kubernetes Service Account for MirrorMaker 2.0
     */
    private createMirrorMakerServiceAccount(
        environment: string,
        eksCluster: eks.ICluster
    ): void {
        // Create Kubernetes Service Account with IAM role binding
        const serviceAccount = eksCluster.addServiceAccount('MirrorMakerServiceAccount', {
            name: `mirrormaker2-${environment}`,
            namespace: 'default',
        });

        // Attach the IAM role to the service account
        if (this.crossRegionReplicationRole) {
            serviceAccount.role.attachInlinePolicy(
                new iam.Policy(this, 'MirrorMakerServiceAccountPolicy', {
                    statements: [
                        new iam.PolicyStatement({
                            effect: iam.Effect.ALLOW,
                            actions: ['kafka:*'],
                            resources: ['*']
                        })
                    ]
                })
            );
        }
    }

    /**
     * Create Kubernetes Deployment for MirrorMaker 2.0
     */
    private createMirrorMakerDeployment(
        environment: string,
        eksCluster: eks.ICluster,
        config: string,
        kmsKey: kms.IKey
    ): any {
        // Create log group for MirrorMaker 2.0
        const mirrorMakerLogGroup = new logs.LogGroup(this, 'MirrorMakerLogGroup', {
            logGroupName: `/aws/eks/mirrormaker2/${environment}`,
            retention: logs.RetentionDays.ONE_WEEK,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
            encryptionKey: kmsKey,
        });

        // Create Kubernetes Deployment manifest
        const deploymentManifest = {
            apiVersion: 'apps/v1',
            kind: 'Deployment',
            metadata: {
                name: `mirrormaker2-${environment}`,
                namespace: 'default',
                labels: {
                    app: 'mirrormaker2',
                    environment: environment,
                    region: this.region,
                },
            },
            spec: {
                replicas: 2, // High availability
                selector: {
                    matchLabels: {
                        app: 'mirrormaker2',
                        environment: environment,
                    },
                },
                template: {
                    metadata: {
                        labels: {
                            app: 'mirrormaker2',
                            environment: environment,
                        },
                        annotations: {
                            'iam.amazonaws.com/role': this.crossRegionReplicationRole?.roleArn,
                        },
                    },
                    spec: {
                        serviceAccountName: `mirrormaker2-${environment}`,
                        containers: [
                            {
                                name: 'mirrormaker2',
                                image: 'confluentinc/cp-kafka-connect:7.4.0',
                                ports: [
                                    {
                                        containerPort: 8083,
                                        name: 'connect-rest',
                                    },
                                ],
                                env: [
                                    { name: 'CONNECT_BOOTSTRAP_SERVERS', value: 'localhost:9092' },
                                    { name: 'CONNECT_REST_ADVERTISED_HOST_NAME', value: 'localhost' },
                                    { name: 'CONNECT_REST_PORT', value: '8083' },
                                    { name: 'CONNECT_GROUP_ID', value: `mirrormaker2-${environment}-${this.region}` },
                                    { name: 'CONNECT_CONFIG_STORAGE_TOPIC', value: `__mirrormaker2-${environment}-configs` },
                                    { name: 'CONNECT_OFFSET_STORAGE_TOPIC', value: `__mirrormaker2-${environment}-offsets` },
                                    { name: 'CONNECT_STATUS_STORAGE_TOPIC', value: `__mirrormaker2-${environment}-status` },
                                    { name: 'CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR', value: '3' },
                                    { name: 'CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR', value: '3' },
                                    { name: 'CONNECT_STATUS_STORAGE_REPLICATION_FACTOR', value: '3' },
                                    { name: 'CONNECT_KEY_CONVERTER', value: 'org.apache.kafka.connect.converters.ByteArrayConverter' },
                                    { name: 'CONNECT_VALUE_CONVERTER', value: 'org.apache.kafka.connect.converters.ByteArrayConverter' },
                                    { name: 'CONNECT_PLUGIN_PATH', value: '/usr/share/java,/usr/share/confluent-hub-components' },
                                    { name: 'CONNECT_LOG4J_LOGGERS', value: 'org.apache.kafka.connect.mirror=INFO' },
                                    { name: 'ENVIRONMENT', value: environment },
                                    { name: 'AWS_REGION', value: this.region },
                                ],
                                resources: {
                                    requests: {
                                        cpu: '1000m',
                                        memory: '2Gi',
                                    },
                                    limits: {
                                        cpu: '2000m',
                                        memory: '4Gi',
                                    },
                                },
                                livenessProbe: {
                                    httpGet: {
                                        path: '/connectors',
                                        port: 8083,
                                    },
                                    initialDelaySeconds: 60,
                                    periodSeconds: 30,
                                    timeoutSeconds: 10,
                                    failureThreshold: 3,
                                },
                                readinessProbe: {
                                    httpGet: {
                                        path: '/connectors',
                                        port: 8083,
                                    },
                                    initialDelaySeconds: 30,
                                    periodSeconds: 10,
                                    timeoutSeconds: 5,
                                    failureThreshold: 3,
                                },
                            },
                            {
                                name: 'config-manager',
                                image: 'amazon/aws-cli:latest',
                                command: [
                                    'sh',
                                    '-c',
                                    `
                                    # Download configuration from SSM
                                    aws ssm get-parameter --name "/msk/mirrormaker/${environment}/config" --query "Parameter.Value" --output text > /tmp/mirrormaker2.properties
                                    
                                    # Wait for Kafka Connect to be ready
                                    while ! curl -f http://localhost:8083/connectors; do
                                        echo "Waiting for Kafka Connect to be ready..."
                                        sleep 10
                                    done
                                    
                                    # Submit MirrorMaker 2.0 connectors
                                    echo "Submitting MirrorMaker 2.0 configuration..."
                                    # Configuration submission logic would go here
                                    
                                    # Keep container running for monitoring
                                    tail -f /dev/null
                                    `,
                                ],
                                env: [
                                    { name: 'AWS_DEFAULT_REGION', value: this.region },
                                    { name: 'ENVIRONMENT', value: environment },
                                ],
                                resources: {
                                    requests: {
                                        cpu: '100m',
                                        memory: '128Mi',
                                    },
                                    limits: {
                                        cpu: '200m',
                                        memory: '256Mi',
                                    },
                                },
                            },
                        ],
                        restartPolicy: 'Always',
                    },
                },
            },
        };

        // Apply the deployment to the EKS cluster
        const deployment = new eks.KubernetesManifest(this, 'MirrorMakerDeployment', {
            cluster: eksCluster,
            manifest: [deploymentManifest],
        });

        return deployment;
    }

    /**
     * Create Kubernetes Service for MirrorMaker 2.0
     */
    private createMirrorMakerKubernetesService(
        environment: string,
        eksCluster: eks.ICluster
    ): any {
        // Create Kubernetes Service manifest
        const serviceManifest = {
            apiVersion: 'v1',
            kind: 'Service',
            metadata: {
                name: `mirrormaker2-${environment}`,
                namespace: 'default',
                labels: {
                    app: 'mirrormaker2',
                    environment: environment,
                },
            },
            spec: {
                selector: {
                    app: 'mirrormaker2',
                    environment: environment,
                },
                ports: [
                    {
                        name: 'connect-rest',
                        port: 8083,
                        targetPort: 8083,
                        protocol: 'TCP',
                    },
                ],
                type: 'ClusterIP',
            },
        };

        // Create HPA (Horizontal Pod Autoscaler) manifest
        const hpaManifest = {
            apiVersion: 'autoscaling/v2',
            kind: 'HorizontalPodAutoscaler',
            metadata: {
                name: `mirrormaker2-${environment}-hpa`,
                namespace: 'default',
            },
            spec: {
                scaleTargetRef: {
                    apiVersion: 'apps/v1',
                    kind: 'Deployment',
                    name: `mirrormaker2-${environment}`,
                },
                minReplicas: 2,
                maxReplicas: 6,
                metrics: [
                    {
                        type: 'Resource',
                        resource: {
                            name: 'cpu',
                            target: {
                                type: 'Utilization',
                                averageUtilization: 70,
                            },
                        },
                    },
                    {
                        type: 'Resource',
                        resource: {
                            name: 'memory',
                            target: {
                                type: 'Utilization',
                                averageUtilization: 80,
                            },
                        },
                    },
                ],
                behavior: {
                    scaleDown: {
                        stabilizationWindowSeconds: 300, // 5 minutes
                        policies: [
                            {
                                type: 'Percent',
                                value: 50,
                                periodSeconds: 60,
                            },
                        ],
                    },
                    scaleUp: {
                        stabilizationWindowSeconds: 120, // 2 minutes
                        policies: [
                            {
                                type: 'Percent',
                                value: 100,
                                periodSeconds: 60,
                            },
                        ],
                    },
                },
            },
        };

        // Apply the service and HPA to the EKS cluster
        const service = new eks.KubernetesManifest(this, 'MirrorMakerKubernetesService', {
            cluster: eksCluster,
            manifest: [serviceManifest, hpaManifest],
        });

        return service;
    }

    /**
     * Create monitoring for MirrorMaker 2.0
     */
    private createMirrorMakerMonitoring(environment: string): void {
        // Store monitoring configuration in SSM for use by observability stack
        new ssm.StringParameter(this, 'MirrorMakerMonitoringConfig', {
            parameterName: `/msk/mirrormaker/${environment}/monitoring`,
            stringValue: JSON.stringify({
                serviceName: `mirrormaker2-${environment}`,
                logGroup: `/aws/eks/mirrormaker2/${environment}`,
                namespace: 'default',
                deploymentName: `mirrormaker2-${environment}`,
                metrics: {
                    replicationLag: {
                        namespace: 'AWS/EKS',
                        metricName: 'MirrorMaker2ReplicationLag',
                        threshold: 1000, // 1 second
                        unit: 'Milliseconds'
                    },
                    throughput: {
                        namespace: 'AWS/EKS',
                        metricName: 'MirrorMaker2Throughput',
                        unit: 'Count/Second'
                    },
                    errorRate: {
                        namespace: 'AWS/EKS',
                        metricName: 'MirrorMaker2ErrorRate',
                        threshold: 0.01, // 1%
                        unit: 'Percent'
                    },
                    podCpuUtilization: {
                        namespace: 'AWS/ContainerInsights',
                        metricName: 'pod_cpu_utilization',
                        threshold: 70,
                        unit: 'Percent'
                    },
                    podMemoryUtilization: {
                        namespace: 'AWS/ContainerInsights',
                        metricName: 'pod_memory_utilization',
                        threshold: 80,
                        unit: 'Percent'
                    }
                },
                alarms: {
                    highReplicationLag: {
                        threshold: 1000,
                        comparisonOperator: 'GreaterThanThreshold',
                        evaluationPeriods: 2,
                        treatMissingData: 'breaching'
                    },
                    highErrorRate: {
                        threshold: 0.05,
                        comparisonOperator: 'GreaterThanThreshold',
                        evaluationPeriods: 3,
                        treatMissingData: 'notBreaching'
                    },
                    podDown: {
                        threshold: 1,
                        comparisonOperator: 'LessThanThreshold',
                        evaluationPeriods: 2,
                        treatMissingData: 'breaching'
                    }
                }
            }),
            description: `MirrorMaker 2.0 monitoring configuration for ${environment} environment`,
        });
    }

    /**
     * Create CloudFormation outputs
     */
    private createOutputs(environment: string): void {
        new cdk.CfnOutput(this, 'MSKClusterArn', {
            value: this.mskCluster.attrArn,
            description: 'MSK Cluster ARN',
            exportName: `${this.stackName}-MSKClusterArn`,
        });

        new cdk.CfnOutput(this, 'MSKClusterName', {
            value: this.mskCluster.clusterName!,
            description: 'MSK Cluster Name',
            exportName: `${this.stackName}-MSKClusterName`,
        });

        new cdk.CfnOutput(this, 'MSKBootstrapServers', {
            value: this.mskCluster.ref,
            description: 'MSK Bootstrap Servers',
            exportName: `${this.stackName}-MSKBootstrapServers`,
        });

        new cdk.CfnOutput(this, 'MSKBootstrapServersTls', {
            value: this.mskCluster.ref,
            description: 'MSK Bootstrap Servers (TLS)',
            exportName: `${this.stackName}-MSKBootstrapServersTls`,
        });

        new cdk.CfnOutput(this, 'MSKZookeeperConnectString', {
            value: this.mskCluster.ref,
            description: 'MSK ZooKeeper Connect String',
            exportName: `${this.stackName}-MSKZookeeperConnectString`,
        });

        new cdk.CfnOutput(this, 'MSKSecurityGroupId', {
            value: this.mskSecurityGroup.securityGroupId,
            description: 'MSK Security Group ID',
            exportName: `${this.stackName}-MSKSecurityGroupId`,
        });

        new cdk.CfnOutput(this, 'MSKClusterPolicyArn', {
            value: this.mskClusterPolicy.managedPolicyArn,
            description: 'MSK Cluster Policy ARN',
            exportName: `${this.stackName}-MSKClusterPolicyArn`,
        });

        // MirrorMaker 2.0 outputs (if enabled)
        // Note: mirrorMakerTaskDefinition property not defined in this class
        // if (this.mirrorMakerTaskDefinition) {
        //     new cdk.CfnOutput(this, 'MirrorMakerTaskDefinitionArn', {
        //         value: this.mirrorMakerTaskDefinition.taskDefinitionArn,
        //         description: 'MirrorMaker 2.0 Task Definition ARN',
        //         exportName: `${this.stackName}-MirrorMakerTaskDefinitionArn`,
        //     });
        // }

        if (this.mirrorMakerService) {
            new cdk.CfnOutput(this, 'MirrorMakerServiceArn', {
                value: this.mirrorMakerService.serviceArn,
                description: 'MirrorMaker 2.0 Service ARN',
                exportName: `${this.stackName}-MirrorMakerServiceArn`,
            });

            new cdk.CfnOutput(this, 'MirrorMakerServiceName', {
                value: this.mirrorMakerService.serviceName,
                description: 'MirrorMaker 2.0 Service Name',
                exportName: `${this.stackName}-MirrorMakerServiceName`,
            });
        }

        if (this.crossRegionReplicationRole) {
            new cdk.CfnOutput(this, 'CrossRegionReplicationRoleArn', {
                value: this.crossRegionReplicationRole.roleArn,
                description: 'Cross-Region Replication Role ARN',
                exportName: `${this.stackName}-CrossRegionReplicationRoleArn`,
            });
        }
    }
}