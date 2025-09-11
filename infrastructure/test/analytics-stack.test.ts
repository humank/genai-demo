import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as kms from 'aws-cdk-lib/aws-kms';
import * as msk from 'aws-cdk-lib/aws-msk';
import * as sns from 'aws-cdk-lib/aws-sns';
import { AnalyticsStack } from '../lib/stacks/analytics-stack';

describe('AnalyticsStack', () => {
    let app: cdk.App;
    let vpc: ec2.Vpc;
    let kmsKey: kms.Key;
    let mskCluster: msk.CfnCluster;
    let alertingTopic: sns.Topic;

    beforeEach(() => {
        app = new cdk.App();

        // Create test VPC
        const vpcStack = new cdk.Stack(app, 'TestVpcStack');
        vpc = new ec2.Vpc(vpcStack, 'TestVpc', {
            maxAzs: 2,
            ipAddresses: ec2.IpAddresses.cidr('10.0.0.0/16')
        });

        // Create test KMS key
        kmsKey = new kms.Key(vpcStack, 'TestKmsKey', {
            description: 'Test KMS key for analytics stack'
        });

        // Create test SNS topic
        alertingTopic = new sns.Topic(vpcStack, 'TestAlertingTopic', {
            topicName: 'test-alerting-topic'
        });

        // Create mock MSK cluster
        mskCluster = new msk.CfnCluster(vpcStack, 'TestMskCluster', {
            clusterName: 'test-msk-cluster',
            kafkaVersion: '2.8.1',
            numberOfBrokerNodes: 2,
            brokerNodeGroupInfo: {
                instanceType: 'kafka.t3.small',
                clientSubnets: vpc.privateSubnets.map(subnet => subnet.subnetId),
                storageInfo: {
                    ebsStorageInfo: {
                        volumeSize: 20
                    }
                }
            }
        });
    });

    test('should create analytics stack with all required resources', () => {
        // Given
        const stack = new AnalyticsStack(app, 'TestAnalyticsStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc: vpc,
            kmsKey: kmsKey,
            mskCluster: mskCluster,
            alertingTopic: alertingTopic,
            region: 'ap-northeast-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then - Verify S3 Data Lake Bucket exists with correct properties
        template.resourceCountIs('AWS::S3::Bucket', 1);

        // Verify bucket has encryption and public access block
        const bucketResources = template.findResources('AWS::S3::Bucket');
        const bucketResource = Object.values(bucketResources)[0] as any;

        // Check encryption configuration
        expect(bucketResource.Properties.BucketEncryption).toBeDefined();
        expect(bucketResource.Properties.BucketEncryption.ServerSideEncryptionConfiguration).toEqual(
            expect.arrayContaining([
                expect.objectContaining({
                    ServerSideEncryptionByDefault: expect.objectContaining({
                        SSEAlgorithm: 'aws:kms'
                    })
                })
            ])
        );

        // Check public access block configuration
        expect(bucketResource.Properties.PublicAccessBlockConfiguration).toEqual(
            expect.objectContaining({
                BlockPublicAcls: true,
                BlockPublicPolicy: true,
                IgnorePublicAcls: true,
                RestrictPublicBuckets: true
            })
        );

        // Then - Verify Kinesis Data Firehose
        template.hasResourceProperties('AWS::KinesisFirehose::DeliveryStream', {
            DeliveryStreamName: 'genai-demo-test-domain-events-firehose',
            DeliveryStreamType: 'DirectPut'
        });

        // Then - Verify AWS Glue Database
        template.hasResourceProperties('AWS::Glue::Database', {
            DatabaseInput: {
                Name: 'genai-demo_test_data_lake',
                Description: 'Data lake database for genai-demo test environment'
            }
        });

        // Then - Verify AWS Glue Crawler
        template.hasResourceProperties('AWS::Glue::Crawler', {
            Name: 'genai-demo-test-domain-events-crawler',
            DatabaseName: {
                Ref: 'GlueDatabase'
            }
        });

        // Then - Verify QuickSight Data Source
        template.hasResourceProperties('AWS::QuickSight::DataSource', {
            DataSourceId: 'genai-demo-test-athena-datasource',
            Name: 'genai-demo test Athena Data Source',
            Type: 'ATHENA'
        });

        // Note: QuickSight Data Sets and Dashboards are created manually in the console
        // for better flexibility and easier maintenance
    });

    test('should create proper IAM roles and policies', () => {
        // Given
        const stack = new AnalyticsStack(app, 'TestAnalyticsStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc: vpc,
            kmsKey: kmsKey,
            mskCluster: mskCluster,
            alertingTopic: alertingTopic,
            region: 'ap-northeast-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then - Verify Firehose Delivery Role
        template.hasResourceProperties('AWS::IAM::Role', {
            RoleName: 'genai-demo-test-firehose-delivery-role',
            AssumeRolePolicyDocument: {
                Statement: [{
                    Effect: 'Allow',
                    Principal: {
                        Service: 'firehose.amazonaws.com'
                    },
                    Action: 'sts:AssumeRole'
                }]
            }
        });

        // Then - Verify Glue Crawler Role
        template.hasResourceProperties('AWS::IAM::Role', {
            RoleName: 'genai-demo-test-glue-crawler-role',
            AssumeRolePolicyDocument: {
                Statement: [{
                    Effect: 'Allow',
                    Principal: {
                        Service: 'glue.amazonaws.com'
                    },
                    Action: 'sts:AssumeRole'
                }]
            }
        });

        // Then - Verify IAM policies exist
        template.resourceCountIs('AWS::IAM::Policy', 2);

        // Verify policies have correct structure (without checking exact content due to CDK complexity)
        const policies = template.findResources('AWS::IAM::Policy');
        const policyNames = Object.keys(policies);

        expect(policyNames.some(name => name.includes('FirehoseDeliveryRole'))).toBe(true);
        expect(policyNames.some(name => name.includes('GlueCrawlerRole'))).toBe(true);
    });

    test('should create CloudWatch alarms for monitoring', () => {
        // Given
        const stack = new AnalyticsStack(app, 'TestAnalyticsStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc: vpc,
            kmsKey: kmsKey,
            mskCluster: mskCluster,
            alertingTopic: alertingTopic,
            region: 'ap-northeast-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then - Verify CloudWatch Alarms
        template.hasResourceProperties('AWS::CloudWatch::Alarm', {
            AlarmName: 'genai-demo-test-FirehoseDeliveryFailures',
            AlarmDescription: 'Firehose delivery to S3 has failed',
            ComparisonOperator: 'LessThanThreshold',
            Threshold: 1
        });

        template.hasResourceProperties('AWS::CloudWatch::Alarm', {
            AlarmName: 'genai-demo-test-GlueCrawlerFailures',
            AlarmDescription: 'Glue crawler has failed tasks',
            ComparisonOperator: 'GreaterThanThreshold',
            Threshold: 0
        });

        template.hasResourceProperties('AWS::CloudWatch::Alarm', {
            AlarmName: 'genai-demo-test-DataLakeBucketSize',
            AlarmDescription: 'Data lake bucket size is growing rapidly',
            ComparisonOperator: 'GreaterThanThreshold'
        });
    });

    test('should create proper outputs for cross-stack references', () => {
        // Given
        const stack = new AnalyticsStack(app, 'TestAnalyticsStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc: vpc,
            kmsKey: kmsKey,
            mskCluster: mskCluster,
            alertingTopic: alertingTopic,
            region: 'ap-northeast-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then - Verify Stack Outputs
        template.hasOutput('DataLakeBucketName', {
            Description: 'S3 data lake bucket name',
            Export: {
                Name: 'genai-demo-test-data-lake-bucket-name'
            }
        });

        template.hasOutput('FirehoseDeliveryStreamName', {
            Description: 'Kinesis Data Firehose delivery stream name',
            Export: {
                Name: 'genai-demo-test-firehose-stream-name'
            }
        });

        template.hasOutput('GlueDatabaseName', {
            Description: 'AWS Glue database name',
            Export: {
                Name: 'genai-demo-test-glue-database-name'
            }
        });

        template.hasOutput('QuickSightDataSourceId', {
            Description: 'QuickSight data source ID',
            Export: {
                Name: 'genai-demo-test-quicksight-datasource-id'
            }
        });

        template.hasOutput('AnalyticsConfiguration', {
            Description: 'Analytics configuration for Spring Boot application',
            Export: {
                Name: 'genai-demo-test-analytics-config'
            }
        });
    });

    test('should apply proper tags to all resources', () => {
        // Given
        const stack = new AnalyticsStack(app, 'TestAnalyticsStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc: vpc,
            kmsKey: kmsKey,
            mskCluster: mskCluster,
            alertingTopic: alertingTopic,
            region: 'ap-northeast-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then - Verify common tags are applied (removed unused variable)

        // Check that resources have proper tags
        const bucketResources = template.findResources('AWS::S3::Bucket');
        const bucketResource = Object.values(bucketResources)[0] as any;

        expect(bucketResource.Properties.Tags).toEqual(
            expect.arrayContaining([
                expect.objectContaining({ Key: 'Project', Value: 'genai-demo' }),
                expect.objectContaining({ Key: 'Environment', Value: 'test' }),
                expect.objectContaining({ Key: 'Component', Value: 'Analytics' })
            ])
        );
    });

    test('should configure lifecycle policies for cost optimization', () => {
        // Given
        const stack = new AnalyticsStack(app, 'TestAnalyticsStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc: vpc,
            kmsKey: kmsKey,
            mskCluster: mskCluster,
            alertingTopic: alertingTopic,
            region: 'ap-northeast-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then - Verify S3 lifecycle configuration
        const bucketResources = template.findResources('AWS::S3::Bucket');
        const bucketResource = Object.values(bucketResources)[0] as any;

        expect(bucketResource.Properties.LifecycleConfiguration).toBeDefined();
        expect(bucketResource.Properties.LifecycleConfiguration.Rules).toEqual(
            expect.arrayContaining([
                expect.objectContaining({
                    Id: 'TransitionToIA',
                    Status: 'Enabled',
                    Transitions: expect.arrayContaining([
                        expect.objectContaining({
                            StorageClass: 'STANDARD_IA',
                            TransitionInDays: 30
                        }),
                        expect.objectContaining({
                            StorageClass: 'GLACIER',
                            TransitionInDays: 90
                        }),
                        expect.objectContaining({
                            StorageClass: 'DEEP_ARCHIVE',
                            TransitionInDays: 365
                        })
                    ])
                })
            ])
        );
    });
});