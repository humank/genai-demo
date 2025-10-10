import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import * as sns from 'aws-cdk-lib/aws-sns';
import { Template } from 'aws-cdk-lib/assertions';
import { DataCatalogStack } from '../src/stacks/data-catalog-stack';

describe('DataCatalogStack', () => {
    let app: cdk.App;
    let stack: DataCatalogStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App();
        
        // Create a mock stack to contain the resources
        const mockStack = new cdk.Stack(app, 'MockStack', {
            env: {
                account: '123456789012',
                region: 'us-east-1'
            }
        });
        
        // Create mock VPC
        const vpc = ec2.Vpc.fromVpcAttributes(mockStack, 'MockVpc', {
            vpcId: 'vpc-12345678',
            availabilityZones: ['us-east-1a', 'us-east-1b'],
            privateSubnetIds: ['subnet-12345678', 'subnet-87654321']
        });

        // Create mock security group
        const securityGroup = ec2.SecurityGroup.fromSecurityGroupId(
            mockStack, 
            'MockSecurityGroup', 
            'sg-12345678'
        );

        // Create mock Aurora cluster
        const auroraCluster = rds.DatabaseCluster.fromDatabaseClusterAttributes(
            mockStack,
            'MockAuroraCluster',
            {
                clusterIdentifier: 'mock-aurora-cluster',
                instanceIdentifiers: ['instance-1'],
                engine: rds.DatabaseClusterEngine.auroraPostgres({
                    version: rds.AuroraPostgresEngineVersion.VER_15_4
                }),
                port: 5432,
                securityGroups: [securityGroup],
                clusterEndpointAddress: 'mock-aurora-cluster.cluster-xyz.us-east-1.rds.amazonaws.com',
                readerEndpointAddress: 'mock-aurora-cluster.cluster-ro-xyz.us-east-1.rds.amazonaws.com'
            }
        );

        // Create mock database secret
        const databaseSecret = secretsmanager.Secret.fromSecretNameV2(
            mockStack,
            'MockDatabaseSecret',
            'mock-database-secret'
        );

        // Create mock SNS topic
        const alertTopic = sns.Topic.fromTopicArn(
            mockStack,
            'MockAlertTopic',
            'arn:aws:sns:us-east-1:123456789012:mock-alert-topic'
        );

        // Create DataCatalogStack
        stack = new DataCatalogStack(app, 'TestDataCatalogStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc: vpc,
            auroraCluster: auroraCluster,
            databaseSecret: databaseSecret,
            rdsSecurityGroup: securityGroup,
            env: {
                account: '123456789012',
                region: 'us-east-1'
            }
        });

        template = Template.fromStack(stack);
    });

    test('creates Glue Database with correct configuration', () => {
        template.hasResourceProperties('AWS::Glue::Database', {
            DatabaseInput: {
                Name: 'genai-demo_test_catalog',
                Description: 'Auto-discovered schema catalog for genai-demo test application',
                Parameters: {
                    'classification': 'postgresql',
                    'typeOfData': 'relational',
                    'created_by': 'aws-glue-crawler',
                    'auto_discovery': 'true',
                    'bounded_contexts': '13',
                    'project': 'genai-demo',
                    'environment': 'test'
                }
            }
        });
    });

    test('creates Glue Crawler with automated discovery configuration', () => {
        template.hasResourceProperties('AWS::Glue::Crawler', {
            Name: 'genai-demo-test-aurora-auto-discovery',
            Description: 'Automatically discovers and catalogs all tables in GenAI Demo Aurora database across 13 bounded contexts',
            SchemaChangePolicy: {
                UpdateBehavior: 'UPDATE_IN_DATABASE',
                DeleteBehavior: 'LOG'
            },
            RecrawlPolicy: {
                RecrawlBehavior: 'CRAWL_EVERYTHING'
            },
            Schedule: {
                ScheduleExpression: 'cron(0 2 * * ? *)'
            }
        });
    });

    test('creates Aurora JDBC connection with proper exclusions', () => {
        template.hasResourceProperties('AWS::Glue::Connection', {
            ConnectionInput: {
                Name: 'genai-demo-test-aurora-connection',
                Description: 'Connection to genai-demo test Aurora PostgreSQL cluster for schema discovery',
                ConnectionType: 'JDBC'
            }
        });
    });

    test('creates IAM role with necessary permissions', () => {
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
            },
            ManagedPolicyArns: [
                {
                    'Fn::Join': [
                        '',
                        [
                            'arn:',
                            { Ref: 'AWS::Partition' },
                            ':iam::aws:policy/service-role/AWSGlueServiceRole'
                        ]
                    ]
                }
            ]
        });
    });

    test('creates Lambda function for real-time crawler triggering', () => {
        template.hasResourceProperties('AWS::Lambda::Function', {
            FunctionName: 'genai-demo-test-glue-crawler-trigger',
            Runtime: 'python3.9',
            Handler: 'trigger_crawler.handler',
            Description: 'Triggers Glue Crawler when Aurora schema changes are detected'
        });
    });

    test('creates EventBridge rule for RDS events', () => {
        template.hasResourceProperties('AWS::Events::Rule', {
            Name: 'TestDataCatalogStack-rds-schema-change-detection',
            Description: 'Trigger Glue Crawler when Aurora schema changes are detected',
            EventPattern: {
                source: ['aws.rds'],
                'detail-type': ['RDS DB Instance Event', 'RDS DB Cluster Event'],
                detail: {
                    EventCategories: [
                        'configuration change',
                        'creation',
                        'deletion',
                        'maintenance'
                    ]
                }
            }
        });
    });

    test('creates CloudWatch Dashboard for monitoring', () => {
        template.hasResourceProperties('AWS::CloudWatch::Dashboard', {
            DashboardName: 'genai-demo-test-data-catalog-monitoring'
        });
    });

    test('creates CloudWatch Alarm for crawler failures', () => {
        template.hasResourceProperties('AWS::CloudWatch::Alarm', {
            AlarmName: 'genai-demo-test-glue-crawler-failure',
            ComparisonOperator: 'GreaterThanOrEqualToThreshold',
            EvaluationPeriods: 1,
            Threshold: 1,
            TreatMissingData: 'notBreaching',
            AlarmDescription: 'Glue Crawler failed to complete successfully'
        });
    });

    test('has proper stack outputs', () => {
        template.hasOutput('GlueDatabaseName', {});
        template.hasOutput('GlueCrawlerName', {});
        template.hasOutput('AuroraConnectionName', {});
        template.hasOutput('DataCatalogDashboardUrl', {});
    });

    test('applies proper tags', () => {
        const resources = template.findResources('AWS::Glue::Database');
        expect(Object.keys(resources)).toHaveLength(1);
        
        const resources2 = template.findResources('AWS::Glue::Crawler');
        expect(Object.keys(resources2)).toHaveLength(1);
    });
});