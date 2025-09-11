import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { SecurityStack } from '../lib/stacks/security-stack';

describe('SecurityStack', () => {
    let app: cdk.App;
    let stack: SecurityStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App();
        stack = new SecurityStack(app, 'TestSecurityStack', {
            environment: 'test',
            region: 'us-east-1',
        });
        template = Template.fromStack(stack);
    });

    test('should create KMS key with proper configuration', () => {
        template.hasResourceProperties('AWS::KMS::Key', {
            Description: 'KMS key for GenAI Demo test environment',
            EnableKeyRotation: true,
            KeyRotationStatus: 'Enabled',
        });

        template.hasResourceProperties('AWS::KMS::Alias', {
            AliasName: 'alias/genai-demo-test-key',
        });
    });

    test('should create CloudTrail with proper configuration', () => {
        template.hasResourceProperties('AWS::CloudTrail::Trail', {
            TrailName: 'genai-demo-test-trail',
            IncludeGlobalServiceEvents: true,
            IsMultiRegionTrail: true,
            EnableLogFileValidation: true,
        });
    });

    test('should create S3 bucket for CloudTrail logs', () => {
        template.hasResourceProperties('AWS::S3::Bucket', {
            BucketName: {
                'Fn::Sub': 'genai-demo-cloudtrail-test-${AWS::AccountId}',
            },
            PublicAccessBlockConfiguration: {
                BlockPublicAcls: true,
                BlockPublicPolicy: true,
                IgnorePublicAcls: true,
                RestrictPublicBuckets: true,
            },
            VersioningConfiguration: {
                Status: 'Enabled',
            },
        });
    });

    test('should create application IAM role with least privilege', () => {
        template.hasResourceProperties('AWS::IAM::Role', {
            RoleName: 'genai-demo-application-test-role',
            AssumeRolePolicyDocument: {
                Statement: [
                    {
                        Effect: 'Allow',
                        Principal: {
                            Service: 'ecs-tasks.amazonaws.com',
                        },
                        Action: 'sts:AssumeRole',
                    },
                ],
            },
        });

        // Check for CloudWatch Logs permissions
        template.hasResourceProperties('AWS::IAM::Policy', {
            PolicyDocument: {
                Statement: [
                    {
                        Sid: 'CloudWatchLogsAccess',
                        Effect: 'Allow',
                        Action: [
                            'logs:CreateLogGroup',
                            'logs:CreateLogStream',
                            'logs:PutLogEvents',
                            'logs:DescribeLogGroups',
                            'logs:DescribeLogStreams',
                        ],
                    },
                ],
            },
        });
    });

    test('should create observability IAM role', () => {
        template.hasResourceProperties('AWS::IAM::Role', {
            RoleName: 'genai-demo-observability-test-role',
            AssumeRolePolicyDocument: {
                Statement: [
                    {
                        Effect: 'Allow',
                        Principal: {
                            Service: ['ecs-tasks.amazonaws.com', 'lambda.amazonaws.com'],
                        },
                        Action: 'sts:AssumeRole',
                    },
                ],
            },
        });
    });

    test('should create secrets for secure configuration', () => {
        template.hasResourceProperties('AWS::SecretsManager::Secret', {
            Name: 'genai-demo/test/database',
            Description: 'Database credentials for GenAI Demo application',
        });

        template.hasResourceProperties('AWS::SecretsManager::Secret', {
            Name: 'genai-demo/test/kafka',
            Description: 'Kafka credentials and configuration',
        });

        template.hasResourceProperties('AWS::SecretsManager::Secret', {
            Name: 'genai-demo/test/application',
            Description: 'Application-specific secrets',
        });
    });

    test('should create CloudWatch Log Group for CloudTrail', () => {
        template.hasResourceProperties('AWS::Logs::LogGroup', {
            LogGroupName: '/aws/cloudtrail/genai-demo-test',
            RetentionInDays: 365,
        });
    });

    test('should output important ARNs', () => {
        template.hasOutput('ApplicationRoleArn', {
            Description: 'ARN of the application IAM role',
            Export: {
                Name: 'genai-demo-application-role-arn',
            },
        });

        template.hasOutput('ObservabilityRoleArn', {
            Description: 'ARN of the observability IAM role',
            Export: {
                Name: 'genai-demo-observability-role-arn',
            },
        });

        template.hasOutput('KmsKeyArn', {
            Description: 'ARN of the KMS key for encryption',
            Export: {
                Name: 'genai-demo-kms-key-arn',
            },
        });
    });

    test('should have proper resource tagging', () => {
        const resources = template.findResources('AWS::KMS::Key');
        const keyLogicalId = Object.keys(resources)[0];
        const keyResource = resources[keyLogicalId];

        expect(keyResource.Properties.Tags).toContainEqual({
            Key: 'Environment',
            Value: 'test',
        });

        expect(keyResource.Properties.Tags).toContainEqual({
            Key: 'Purpose',
            Value: 'Encryption',
        });
    });

    test('should configure KMS key policy for CloudWatch Logs', () => {
        template.hasResourceProperties('AWS::KMS::Key', {
            KeyPolicy: {
                Statement: [
                    {
                        Sid: 'EnableRootAccess',
                        Effect: 'Allow',
                        Principal: {
                            AWS: {
                                'Fn::Sub': 'arn:aws:iam::${AWS::AccountId}:root',
                            },
                        },
                        Action: 'kms:*',
                        Resource: '*',
                    },
                    {
                        Sid: 'AllowCloudWatchLogs',
                        Effect: 'Allow',
                        Principal: {
                            Service: 'logs.amazonaws.com',
                        },
                        Action: [
                            'kms:Encrypt',
                            'kms:Decrypt',
                            'kms:ReEncrypt*',
                            'kms:GenerateDataKey*',
                            'kms:DescribeKey',
                        ],
                        Resource: '*',
                    },
                ],
            },
        });
    });

    test('should configure S3 lifecycle policies for cost optimization', () => {
        template.hasResourceProperties('AWS::S3::Bucket', {
            LifecycleConfiguration: {
                Rules: [
                    {
                        Id: 'CloudTrailLogRetention',
                        Status: 'Enabled',
                        Transitions: [
                            {
                                StorageClass: 'STANDARD_IA',
                                TransitionInDays: 30,
                            },
                            {
                                StorageClass: 'GLACIER',
                                TransitionInDays: 90,
                            },
                        ],
                        ExpirationInDays: 2555, // 7 years
                    },
                ],
            },
        });
    });
});