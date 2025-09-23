import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as cloudwatchActions from 'aws-cdk-lib/aws-cloudwatch-actions';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as glue from 'aws-cdk-lib/aws-glue';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as kinesisFirehose from 'aws-cdk-lib/aws-kinesisfirehose';
import * as kms from 'aws-cdk-lib/aws-kms';
import * as msk from 'aws-cdk-lib/aws-msk';
import * as quicksight from 'aws-cdk-lib/aws-quicksight';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as sns from 'aws-cdk-lib/aws-sns';
import { Construct } from 'constructs';

export interface AnalyticsStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly vpc: ec2.IVpc;
    readonly kmsKey: kms.IKey;
    readonly mskCluster: msk.CfnCluster;
    readonly alertingTopic: sns.ITopic;
    readonly region?: string;
}

export class AnalyticsStack extends cdk.Stack {
    public readonly dataLakeBucket: s3.Bucket;
    public readonly firehoseDeliveryStream: kinesisFirehose.CfnDeliveryStream;
    public readonly glueDatabase: glue.CfnDatabase;
    public readonly glueCrawler: glue.CfnCrawler;
    public readonly quicksightDataSource?: quicksight.CfnDataSource;

    constructor(scope: Construct, id: string, props: AnalyticsStackProps) {
        super(scope, id, props);

        const { environment, projectName, vpc, kmsKey, mskCluster, alertingTopic, region } = props;

        // Apply common tags
        const commonTags = {
            Project: projectName,
            Environment: environment,
            ManagedBy: 'AWS-CDK',
            Component: 'Analytics',
            Service: 'BusinessIntelligence'
        };

        Object.entries(commonTags).forEach(([key, value]) => {
            cdk.Tags.of(this).add(key, value);
        });

        // Create S3 Data Lake
        this.dataLakeBucket = this.createDataLakeBucket(projectName, environment, kmsKey);

        // Create Kinesis Data Firehose
        this.firehoseDeliveryStream = this.createFirehoseDeliveryStream(
            projectName,
            environment,
            this.dataLakeBucket,
            kmsKey
        );

        // Create AWS Glue resources
        const glueResources = this.createGlueResources(projectName, environment, this.dataLakeBucket);
        this.glueDatabase = glueResources.database;
        this.glueCrawler = glueResources.crawler;

        // Create QuickSight data source (optional)
        const enableQuickSight = this.node.tryGetContext('enableQuickSight') === 'true';
        if (enableQuickSight) {
            this.quicksightDataSource = this.createQuickSightDataSource(projectName, environment);
        }

        // Set up monitoring and alerting
        this.setupMonitoringAndAlerting(projectName, environment, alertingTopic);

        // Create outputs
        this.createOutputs(projectName, environment);
    }
    private createDataLakeBucket(
        projectName: string,
        environment: string,
        kmsKey: kms.IKey
    ): s3.Bucket {
        const bucket = new s3.Bucket(this, 'DataLakeBucket', {
            bucketName: `${projectName}-${environment}-data-lake-${this.account}`,
            versioned: environment === 'production',
            encryption: s3.BucketEncryption.KMS,
            encryptionKey: kmsKey,
            blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
            removalPolicy: environment === 'production' ? cdk.RemovalPolicy.RETAIN : cdk.RemovalPolicy.DESTROY,
            autoDeleteObjects: environment !== 'production',

            // Lifecycle configuration for cost optimization
            lifecycleRules: [
                {
                    id: 'TransitionToIA',
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
                    ]
                }
            ]
        });

        cdk.Tags.of(bucket).add('Name', `${projectName}-${environment}-data-lake`);
        cdk.Tags.of(bucket).add('DataClassification', 'BusinessData');

        return bucket;
    }

    private createFirehoseDeliveryStream(
        projectName: string,
        environment: string,
        dataLakeBucket: s3.Bucket,
        kmsKey: kms.IKey
    ): kinesisFirehose.CfnDeliveryStream {
        // Create IAM role for Firehose
        const firehoseRole = new iam.Role(this, 'FirehoseDeliveryRole', {
            roleName: `${projectName}-${environment}-firehose-delivery-role`,
            assumedBy: new iam.ServicePrincipal('firehose.amazonaws.com'),
            description: 'IAM role for Kinesis Data Firehose delivery to S3'
        });

        // Add S3 permissions
        firehoseRole.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                's3:AbortMultipartUpload',
                's3:GetBucketLocation',
                's3:GetObject',
                's3:ListBucket',
                's3:ListBucketMultipartUploads',
                's3:PutObject'
            ],
            resources: [
                dataLakeBucket.bucketArn,
                `${dataLakeBucket.bucketArn}/*`
            ]
        }));

        // Add KMS permissions
        firehoseRole.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'kms:Decrypt',
                'kms:GenerateDataKey'
            ],
            resources: [kmsKey.keyArn]
        }));

        // Create Kinesis Data Firehose delivery stream
        const deliveryStream = new kinesisFirehose.CfnDeliveryStream(this, 'DomainEventsFirehose', {
            deliveryStreamName: `${projectName}-${environment}-domain-events-firehose`,
            deliveryStreamType: 'DirectPut',

            extendedS3DestinationConfiguration: {
                bucketArn: dataLakeBucket.bucketArn,
                roleArn: firehoseRole.roleArn,

                // Partitioning configuration for efficient querying
                prefix: 'domain-events/year=!{timestamp:yyyy}/month=!{timestamp:MM}/day=!{timestamp:dd}/hour=!{timestamp:HH}/',
                errorOutputPrefix: 'errors/domain-events/',

                // Buffering configuration
                bufferingHints: {
                    sizeInMBs: 5,
                    intervalInSeconds: 300
                },

                // Compression
                compressionFormat: 'GZIP',

                // Encryption
                encryptionConfiguration: {
                    kmsEncryptionConfig: {
                        awskmsKeyArn: kmsKey.keyArn
                    }
                }
            }
        });

        cdk.Tags.of(deliveryStream).add('Name', `${projectName}-${environment}-domain-events-firehose`);
        cdk.Tags.of(deliveryStream).add('DataType', 'DomainEvents');

        return deliveryStream;
    }

    private createGlueResources(
        projectName: string,
        environment: string,
        dataLakeBucket: s3.Bucket
    ): { database: glue.CfnDatabase; crawler: glue.CfnCrawler } {
        // Create Glue database
        const database = new glue.CfnDatabase(this, 'GlueDatabase', {
            catalogId: this.account,
            databaseInput: {
                name: `${projectName}_${environment}_data_lake`,
                description: `Data lake database for ${projectName} ${environment} environment`
            }
        });

        // Create IAM role for Glue crawler
        const crawlerRole = new iam.Role(this, 'GlueCrawlerRole', {
            roleName: `${projectName}-${environment}-glue-crawler-role`,
            assumedBy: new iam.ServicePrincipal('glue.amazonaws.com'),
            description: 'IAM role for AWS Glue crawler'
        });

        // Add Glue service role policy
        crawlerRole.addManagedPolicy(
            iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSGlueServiceRole')
        );

        // Add S3 permissions for crawler
        crawlerRole.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                's3:GetObject',
                's3:ListBucket'
            ],
            resources: [
                dataLakeBucket.bucketArn,
                `${dataLakeBucket.bucketArn}/*`
            ]
        }));

        // Create Glue crawler
        const crawler = new glue.CfnCrawler(this, 'GlueCrawler', {
            name: `${projectName}-${environment}-domain-events-crawler`,
            role: crawlerRole.roleArn,
            databaseName: database.ref,
            description: 'Crawler for domain events data in S3 data lake',

            targets: {
                s3Targets: [
                    {
                        path: `s3://${dataLakeBucket.bucketName}/domain-events/`,
                        exclusions: ['backup/**', 'errors/**']
                    }
                ]
            },

            // Schedule crawler to run daily
            schedule: {
                scheduleExpression: 'cron(0 2 * * ? *)'
            },

            // Schema change policy
            schemaChangePolicy: {
                updateBehavior: 'UPDATE_IN_DATABASE',
                deleteBehavior: 'LOG'
            }
        });

        cdk.Tags.of(database).add('Name', `${projectName}-${environment}-data-lake-db`);
        cdk.Tags.of(crawler).add('Name', `${projectName}-${environment}-domain-events-crawler`);

        return { database, crawler };
    }

    private createQuickSightDataSource(
        projectName: string,
        environment: string
    ): quicksight.CfnDataSource {
        // Get QuickSight user ARN from context or use default
        const quicksightUserArn = this.node.tryGetContext('quicksight-user-arn') ||
            `arn:aws:quicksight:${this.region}:${this.account}:user/default/${projectName}-admin`;

        // Create QuickSight data source for Athena/Glue
        const dataSource = new quicksight.CfnDataSource(this, 'QuickSightDataSource', {
            awsAccountId: this.account,
            dataSourceId: `${projectName}-${environment}-athena-datasource`,
            name: `${projectName} ${environment} Athena Data Source`,
            type: 'ATHENA',

            dataSourceParameters: {
                athenaParameters: {
                    workGroup: 'primary'
                }
            },

            permissions: [
                {
                    principal: quicksightUserArn,
                    actions: [
                        'quicksight:DescribeDataSource',
                        'quicksight:DescribeDataSourcePermissions',
                        'quicksight:PassDataSource',
                        'quicksight:UpdateDataSource',
                        'quicksight:DeleteDataSource',
                        'quicksight:UpdateDataSourcePermissions'
                    ]
                }
            ]
        });

        cdk.Tags.of(dataSource).add('Name', `${projectName}-${environment}-athena-datasource`);
        cdk.Tags.of(dataSource).add('DataSourceType', 'Athena');

        return dataSource;
    }

    private setupMonitoringAndAlerting(
        projectName: string,
        environment: string,
        alertingTopic: sns.ITopic
    ): void {
        // Create CloudWatch alarms for analytics pipeline monitoring
        const alarms = [
            {
                name: 'FirehoseDeliveryFailures',
                metric: new cloudwatch.Metric({
                    namespace: 'AWS/KinesisFirehose',
                    metricName: 'DeliveryToS3.Records',
                    dimensionsMap: {
                        'DeliveryStreamName': this.firehoseDeliveryStream.deliveryStreamName!
                    },
                    statistic: 'Sum',
                    period: cdk.Duration.minutes(5)
                }),
                threshold: 1,
                comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
                description: 'Firehose delivery to S3 has failed'
            },
            {
                name: 'GlueCrawlerFailures',
                metric: new cloudwatch.Metric({
                    namespace: 'AWS/Glue',
                    metricName: 'glue.driver.aggregate.numFailedTasks',
                    dimensionsMap: {
                        'JobName': this.glueCrawler.name!
                    },
                    statistic: 'Sum',
                    period: cdk.Duration.minutes(15)
                }),
                threshold: 0,
                comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
                description: 'Glue crawler has failed tasks'
            },
            {
                name: 'DataLakeBucketSize',
                metric: new cloudwatch.Metric({
                    namespace: 'AWS/S3',
                    metricName: 'BucketSizeBytes',
                    dimensionsMap: {
                        'BucketName': this.dataLakeBucket.bucketName,
                        'StorageType': 'StandardStorage'
                    },
                    statistic: 'Average',
                    period: cdk.Duration.hours(24)
                }),
                threshold: 100 * 1024 * 1024 * 1024, // 100GB
                comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
                description: 'Data lake bucket size is growing rapidly'
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

            cdk.Tags.of(alarm).add('Name', `${projectName}-${environment}-${alarmConfig.name}`);
            cdk.Tags.of(alarm).add('AlarmType', 'Analytics');
        });
    }

    private createOutputs(projectName: string, environment: string): void {
        // Data Lake Outputs
        new cdk.CfnOutput(this, 'DataLakeBucketName', {
            value: this.dataLakeBucket.bucketName,
            description: 'S3 data lake bucket name',
            exportName: `${projectName}-${environment}-data-lake-bucket-name`
        });

        new cdk.CfnOutput(this, 'DataLakeBucketArn', {
            value: this.dataLakeBucket.bucketArn,
            description: 'S3 data lake bucket ARN',
            exportName: `${projectName}-${environment}-data-lake-bucket-arn`
        });

        // Kinesis Data Firehose Outputs
        new cdk.CfnOutput(this, 'FirehoseDeliveryStreamName', {
            value: this.firehoseDeliveryStream.deliveryStreamName!,
            description: 'Kinesis Data Firehose delivery stream name',
            exportName: `${projectName}-${environment}-firehose-stream-name`
        });

        new cdk.CfnOutput(this, 'FirehoseDeliveryStreamArn', {
            value: this.firehoseDeliveryStream.attrArn,
            description: 'Kinesis Data Firehose delivery stream ARN',
            exportName: `${projectName}-${environment}-firehose-stream-arn`
        });

        // AWS Glue Outputs
        new cdk.CfnOutput(this, 'GlueDatabaseName', {
            value: this.glueDatabase.ref,
            description: 'AWS Glue database name',
            exportName: `${projectName}-${environment}-glue-database-name`
        });

        new cdk.CfnOutput(this, 'GlueCrawlerName', {
            value: this.glueCrawler.name!,
            description: 'AWS Glue crawler name',
            exportName: `${projectName}-${environment}-glue-crawler-name`
        });

        // QuickSight Outputs (optional)
        if (this.quicksightDataSource) {
            new cdk.CfnOutput(this, 'QuickSightDataSourceId', {
                value: this.quicksightDataSource.dataSourceId!,
                description: 'QuickSight data source ID',
                exportName: `${projectName}-${environment}-quicksight-datasource-id`
            });

            new cdk.CfnOutput(this, 'QuickSightDataSourceArn', {
                value: this.quicksightDataSource.attrArn,
                description: 'QuickSight data source ARN',
                exportName: `${projectName}-${environment}-quicksight-datasource-arn`
            });
        }

        // Analytics Configuration for Spring Boot
        new cdk.CfnOutput(this, 'AnalyticsConfiguration', {
            value: JSON.stringify({
                'analytics.firehose.stream-name': this.firehoseDeliveryStream.deliveryStreamName,
                'analytics.data-lake.bucket-name': this.dataLakeBucket.bucketName,
                'analytics.glue.database-name': this.glueDatabase.ref,
                'analytics.quicksight.data-source-id': this.quicksightDataSource?.dataSourceId || 'not-enabled',
                'analytics.enabled': true
            }),
            description: 'Analytics configuration for Spring Boot application',
            exportName: `${projectName}-${environment}-analytics-config`
        });
    }
}