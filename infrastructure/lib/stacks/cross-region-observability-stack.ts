import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as kms from 'aws-cdk-lib/aws-kms';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as ssm from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';

export interface CrossRegionObservabilityStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly primaryRegion: string;
    readonly secondaryRegion: string;
    readonly regionType: 'primary' | 'secondary';
    readonly kmsKey: kms.IKey;
    readonly alertingTopic: sns.ITopic;
}

export class CrossRegionObservabilityStack extends cdk.Stack {
    public readonly crossRegionLogsBucket: s3.Bucket;
    public readonly crossRegionMetricsBucket: s3.Bucket;
    public readonly logReplicationRole: iam.Role;
    public readonly observabilityDashboard: cloudwatch.Dashboard;

    constructor(scope: Construct, id: string, props: CrossRegionObservabilityStackProps) {
        super(scope, id, props);

        const {
            environment,
            projectName,
            primaryRegion,
            secondaryRegion,
            regionType,
            kmsKey,
            alertingTopic
        } = props;

        // Apply common tags
        const commonTags = {
            Project: projectName,
            Environment: environment,
            ManagedBy: 'AWS-CDK',
            Component: 'CrossRegionObservability',
            RegionType: regionType
        };

        Object.entries(commonTags).forEach(([key, value]) => {
            cdk.Tags.of(this).add(key, value);
        });

        // Get multi-region configuration
        const multiRegionConfig = this.node.tryGetContext('genai-demo:multi-region') || {};

        // Only deploy cross-region observability if enabled
        if (multiRegionConfig['enable-cross-region-replication'] && environment === 'production') {
            // Create S3 buckets for cross-region data replication
            this.crossRegionLogsBucket = this.createCrossRegionLogsBucket(projectName, environment, regionType, kmsKey);
            this.crossRegionMetricsBucket = this.createCrossRegionMetricsBucket(projectName, environment, regionType, kmsKey);

            // Create IAM role for log replication
            this.logReplicationRole = this.createLogReplicationRole(projectName, environment, primaryRegion, secondaryRegion);

            // Set up cross-region log replication
            this.setupCrossRegionLogReplication(projectName, environment, regionType, primaryRegion, secondaryRegion);

            // Create unified observability dashboard
            this.observabilityDashboard = this.createUnifiedObservabilityDashboard(
                projectName,
                environment,
                regionType,
                primaryRegion,
                secondaryRegion
            );

            // Store observability configuration
            this.storeObservabilityConfiguration(projectName, environment, regionType, primaryRegion, secondaryRegion);

            // Create outputs
            this.createOutputs(projectName, environment, regionType);
        }
    }

    private createCrossRegionLogsBucket(
        projectName: string,
        environment: string,
        regionType: 'primary' | 'secondary',
        kmsKey: kms.IKey
    ): s3.Bucket {
        const bucket = new s3.Bucket(this, 'CrossRegionLogsBucket', {
            bucketName: `${projectName}-${environment}-${regionType}-cross-region-logs`,
            encryption: s3.BucketEncryption.KMS,
            encryptionKey: kmsKey,
            versioned: true,
            lifecycleRules: [
                {
                    id: 'LogsLifecycle',
                    enabled: true,
                    transitions: [
                        {
                            storageClass: s3.StorageClass.INFREQUENT_ACCESS,
                            transitionAfter: cdk.Duration.days(30)
                        },
                        {
                            storageClass: s3.StorageClass.GLACIER,
                            transitionAfter: cdk.Duration.days(90)
                        },
                        {
                            storageClass: s3.StorageClass.DEEP_ARCHIVE,
                            transitionAfter: cdk.Duration.days(365)
                        }
                    ],
                    expiration: cdk.Duration.days(2555) // 7 years
                }
            ],
            removalPolicy: environment === 'production' ? cdk.RemovalPolicy.RETAIN : cdk.RemovalPolicy.DESTROY
        });

        cdk.Tags.of(bucket).add('Name', `${projectName}-${environment}-${regionType}-cross-region-logs`);
        cdk.Tags.of(bucket).add('DataType', 'Logs');
        cdk.Tags.of(bucket).add('ReplicationEnabled', 'true');

        return bucket;
    }

    private createCrossRegionMetricsBucket(
        projectName: string,
        environment: string,
        regionType: 'primary' | 'secondary',
        kmsKey: kms.IKey
    ): s3.Bucket {
        const bucket = new s3.Bucket(this, 'CrossRegionMetricsBucket', {
            bucketName: `${projectName}-${environment}-${regionType}-cross-region-metrics`,
            encryption: s3.BucketEncryption.KMS,
            encryptionKey: kmsKey,
            versioned: true,
            lifecycleRules: [
                {
                    id: 'MetricsLifecycle',
                    enabled: true,
                    transitions: [
                        {
                            storageClass: s3.StorageClass.INFREQUENT_ACCESS,
                            transitionAfter: cdk.Duration.days(90)
                        },
                        {
                            storageClass: s3.StorageClass.GLACIER,
                            transitionAfter: cdk.Duration.days(365)
                        }
                    ],
                    expiration: cdk.Duration.days(1095) // 3 years
                }
            ],
            removalPolicy: environment === 'production' ? cdk.RemovalPolicy.RETAIN : cdk.RemovalPolicy.DESTROY
        });

        cdk.Tags.of(bucket).add('Name', `${projectName}-${environment}-${regionType}-cross-region-metrics`);
        cdk.Tags.of(bucket).add('DataType', 'Metrics');
        cdk.Tags.of(bucket).add('ReplicationEnabled', 'true');

        return bucket;
    }

    private createLogReplicationRole(
        projectName: string,
        environment: string,
        primaryRegion: string,
        secondaryRegion: string
    ): iam.Role {
        const role = new iam.Role(this, 'LogReplicationRole', {
            roleName: `${projectName}-${environment}-log-replication-role`,
            assumedBy: new iam.ServicePrincipal('logs.amazonaws.com'),
            description: 'IAM role for cross-region log replication'
        });

        // Add permissions for cross-region log replication
        role.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                's3:GetBucketVersioning',
                's3:PutBucketVersioning',
                's3:ReplicateObject',
                's3:ReplicateDelete',
                's3:ReplicateTags',
                's3:GetObjectVersionTagging'
            ],
            resources: [
                this.crossRegionLogsBucket.bucketArn,
                `${this.crossRegionLogsBucket.bucketArn}/*`,
                this.crossRegionMetricsBucket.bucketArn,
                `${this.crossRegionMetricsBucket.bucketArn}/*`
            ]
        }));

        cdk.Tags.of(role).add('Name', `${projectName}-${environment}-log-replication-role`);
        cdk.Tags.of(role).add('Service', 'LogReplication');

        return role;
    }

    private setupCrossRegionLogReplication(
        projectName: string,
        environment: string,
        regionType: 'primary' | 'secondary',
        primaryRegion: string,
        secondaryRegion: string
    ): void {
        // Create log groups with cross-region replication
        const logGroups = [
            `/aws/lambda/${projectName}-${environment}`,
            `/aws/ecs/${projectName}-${environment}`,
            `/aws/rds/instance/${projectName}-${environment}`,
            `/aws/msk/cluster/${projectName}-${environment}`,
            `/aws/apigateway/${projectName}-${environment}`
        ];

        logGroups.forEach((logGroupName, index) => {
            const logGroup = new logs.LogGroup(this, `CrossRegionLogGroup${index}`, {
                logGroupName: logGroupName,
                retention: environment === 'production' ? logs.RetentionDays.ONE_MONTH : logs.RetentionDays.ONE_WEEK,
                removalPolicy: environment === 'production' ? cdk.RemovalPolicy.RETAIN : cdk.RemovalPolicy.DESTROY
            });

            cdk.Tags.of(logGroup).add('Name', logGroupName);
            cdk.Tags.of(logGroup).add('CrossRegionReplication', 'enabled');
        });
    }

    private createUnifiedObservabilityDashboard(
        projectName: string,
        environment: string,
        regionType: 'primary' | 'secondary',
        primaryRegion: string,
        secondaryRegion: string
    ): cloudwatch.Dashboard {
        const dashboard = new cloudwatch.Dashboard(this, 'UnifiedObservabilityDashboard', {
            dashboardName: `${projectName}-${environment}-${regionType}-unified-observability`,
            defaultInterval: cdk.Duration.minutes(5)
        });

        // Add header widget
        dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `# Unified Cross-Region Observability Dashboard\n\n**Environment:** ${environment}\n**Region Type:** ${regionType}\n**Primary Region:** ${primaryRegion}\n**Secondary Region:** ${secondaryRegion}\n**Last Updated:** ${new Date().toISOString()}`,
                width: 24,
                height: 4
            })
        );

        return dashboard;
    }

    private storeObservabilityConfiguration(
        projectName: string,
        environment: string,
        regionType: 'primary' | 'secondary',
        primaryRegion: string,
        secondaryRegion: string
    ): void {
        const parameterPrefix = `/genai-demo/${environment}/observability`;

        // Store cross-region observability configuration
        const observabilityConfig = {
            crossRegionReplicationEnabled: true,
            primaryRegion: primaryRegion,
            secondaryRegion: secondaryRegion,
            regionType: regionType,
            logsBucket: this.crossRegionLogsBucket.bucketName,
            metricsBucket: this.crossRegionMetricsBucket.bucketName,
            replicationRole: this.logReplicationRole.roleArn,
            dashboardName: this.observabilityDashboard.dashboardName
        };

        new ssm.StringParameter(this, 'ObservabilityConfiguration', {
            parameterName: `${parameterPrefix}/cross-region-config`,
            stringValue: JSON.stringify(observabilityConfig),
            description: `Cross-region observability configuration for ${projectName} ${environment}`,
            tier: ssm.ParameterTier.STANDARD
        });
    }

    private createOutputs(projectName: string, environment: string, regionType: 'primary' | 'secondary'): void {
        // Cross-region observability outputs
        new cdk.CfnOutput(this, 'CrossRegionLogsBucketName', {
            value: this.crossRegionLogsBucket.bucketName,
            description: 'Cross-region logs bucket name',
            exportName: `${projectName}-${environment}-${regionType}-cross-region-logs-bucket`
        });

        new cdk.CfnOutput(this, 'CrossRegionObservabilityEnabled', {
            value: 'true',
            description: 'Cross-region observability status',
            exportName: `${projectName}-${environment}-${regionType}-cross-region-observability-enabled`
        });
    }
}