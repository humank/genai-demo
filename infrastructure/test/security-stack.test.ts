import * as cdk from 'aws-cdk-lib';
import { Template, Match } from 'aws-cdk-lib/assertions';
import { SecurityStack } from '../src/stacks/security-stack';

describe('SecurityStack', () => {
    let app: cdk.App;
    let stack: SecurityStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App();
        stack = new SecurityStack(app, 'TestSecurityStack', {
            environment: 'test',
            projectName: 'test-project',
            region: 'us-east-1',
            primaryRegion: 'us-east-1',
            secondaryRegions: ['us-west-2', 'eu-west-1'],
            crossRegionEnabled: true,
            complianceStandards: ['SOC2', 'ISO27001', 'GDPR'],
            dataClassification: 'confidential'
        });
        template = Template.fromStack(stack);
    });

    test('should create enhanced KMS key with proper configuration', () => {
        template.hasResourceProperties('AWS::KMS::Key', {
            Description: Match.stringLikeRegexp('Enhanced KMS key for test-project test - confidential data encryption'),
            EnableKeyRotation: true,
        });
    });

    test('should create cross-region KMS key when enabled', () => {
        template.hasResourceProperties('AWS::KMS::Key', {
            Description: Match.stringLikeRegexp('Cross-region KMS key for test-project test - Multi-region confidential data encryption'),
            EnableKeyRotation: true,
        });
    });

    test('should create enhanced application IAM role with multiple service principals', () => {
        template.hasResourceProperties('AWS::IAM::Role', {
            RoleName: 'test-project-test-enhanced-application-role',
            Description: 'Enhanced IAM role for test-project test application with confidential data access',
            ManagedPolicyArns: Match.arrayWith([
                Match.objectLike({
                    'Fn::Join': Match.arrayWith([
                        Match.arrayWith([
                            Match.stringLikeRegexp(':iam::aws:policy/CloudWatchAgentServerPolicy')
                        ])
                    ])
                }),
                Match.objectLike({
                    'Fn::Join': Match.arrayWith([
                        Match.arrayWith([
                            Match.stringLikeRegexp(':iam::aws:policy/AWSXRayDaemonWriteAccess')
                        ])
                    ])
                })
            ])
        });
    });

    test('should create compliance monitoring role', () => {
        template.hasResourceProperties('AWS::IAM::Role', {
            RoleName: 'test-project-test-compliance-role',
            Description: 'Compliance monitoring role for SOC2, ISO27001, GDPR standards',
        });
    });

    test('should create data classification bucket with encryption', () => {
        template.hasResourceProperties('AWS::S3::Bucket', {
            BucketEncryption: {
                ServerSideEncryptionConfiguration: [
                    {
                        ServerSideEncryptionByDefault: {
                            SSEAlgorithm: 'aws:kms',
                        },
                    },
                ],
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

    test('should create compliance monitoring Lambda function', () => {
        template.hasResourceProperties('AWS::Lambda::Function', {
            FunctionName: 'test-project-test-compliance-monitoring',
            Runtime: 'python3.11',
            Handler: 'index.lambda_handler',
            Timeout: 300,
            MemorySize: 256,
            Environment: {
                Variables: {
                    PROJECT_NAME: 'test-project',
                    ENVIRONMENT: 'test',
                    DATA_CLASSIFICATION: 'confidential',
                    COMPLIANCE_STANDARDS: 'SOC2,ISO27001,GDPR',
                },
            },
        });
    });

    test('should create data sovereignty monitoring function', () => {
        template.hasResourceProperties('AWS::Lambda::Function', {
            FunctionName: 'test-project-test-data-sovereignty',
            Runtime: 'python3.11',
            Handler: 'index.lambda_handler',
            Timeout: 180,
            MemorySize: 128,
            Environment: {
                Variables: {
                    PROJECT_NAME: 'test-project',
                    ENVIRONMENT: 'test',
                    PRIMARY_REGION: 'us-east-1',
                },
            },
        });
    });

    test('should create AWS Config rule for compliance monitoring', () => {
        template.hasResourceProperties('AWS::Config::ConfigRule', {
            ConfigRuleName: 'test-project-test-security-compliance',
            Description: 'Security compliance rule for SOC2, ISO27001, GDPR standards',
            Source: {
                Owner: 'AWS_LAMBDA',
            },
        });
    });

    test('should create EventBridge rules for scheduled monitoring', () => {
        // Compliance monitoring schedule
        template.hasResourceProperties('AWS::Events::Rule', {
            Description: 'Daily compliance monitoring schedule',
            ScheduleExpression: 'cron(0 2 * * ? *)',
        });

        // Data sovereignty monitoring schedule
        template.hasResourceProperties('AWS::Events::Rule', {
            Description: 'Daily data sovereignty monitoring',
            ScheduleExpression: 'cron(0 6 * * ? *)',
        });
    });

    test('should create GDPR data processing log group', () => {
        template.hasResourceProperties('AWS::Logs::LogGroup', {
            LogGroupName: '/aws/test-project/test/gdpr-data-processing',
            RetentionInDays: 2192, // 6 years for GDPR compliance
        });
    });

    test('should output enhanced KMS key information', () => {
        template.hasOutput('KMSKeyId', {
            Description: 'Enhanced KMS Key ID for application encryption',
            Export: {
                Name: 'TestSecurityStack-KMSKeyId',
            },
        });

        template.hasOutput('KMSKeyArn', {
            Description: 'Enhanced KMS Key ARN for application encryption',
            Export: {
                Name: 'TestSecurityStack-KMSKeyArn',
            },
        });
    });

    test('should output cross-region KMS key information when enabled', () => {
        template.hasOutput('CrossRegionKMSKeyId', {
            Description: 'Cross-region KMS Key ID for multi-region encryption',
            Export: {
                Name: 'TestSecurityStack-CrossRegionKMSKeyId',
            },
        });

        template.hasOutput('CrossRegionKMSKeyArn', {
            Description: 'Cross-region KMS Key ARN for multi-region encryption',
            Export: {
                Name: 'TestSecurityStack-CrossRegionKMSKeyArn',
            },
        });
    });

    test('should output role ARNs', () => {
        template.hasOutput('ApplicationRoleArn', {
            Description: 'Enhanced application role ARN with security policies',
            Export: {
                Name: 'TestSecurityStack-ApplicationRoleArn',
            },
        });

        template.hasOutput('ComplianceRoleArn', {
            Description: 'Compliance monitoring role ARN',
            Export: {
                Name: 'TestSecurityStack-ComplianceRoleArn',
            },
        });
    });

    test('should output data classification bucket information', () => {
        template.hasOutput('DataClassificationBucketName', {
            Description: 'Data classification bucket name with encryption',
            Export: {
                Name: 'TestSecurityStack-DataClassificationBucketName',
            },
        });

        template.hasOutput('DataClassificationBucketArn', {
            Description: 'Data classification bucket ARN',
            Export: {
                Name: 'TestSecurityStack-DataClassificationBucketArn',
            },
        });
    });

    test('should output compliance monitoring function ARN', () => {
        template.hasOutput('ComplianceMonitoringFunctionArn', {
            Description: 'Compliance monitoring function ARN',
            Export: {
                Name: 'TestSecurityStack-ComplianceMonitoringFunctionArn',
            },
        });
    });

    test('should output security configuration summary', () => {
        template.hasOutput('SecurityStackSummary', {
            Description: 'Security stack configuration summary',
            Export: {
                Name: 'TestSecurityStack-SecuritySummary',
            },
        });
    });

    test('should have correct resource counts for enhanced security', () => {
        template.resourceCountIs('AWS::KMS::Key', 2); // Main key + cross-region key
        template.resourceCountIs('AWS::KMS::Alias', 2); // Aliases for both keys
        template.resourceCountIs('AWS::IAM::Role', 3); // Application role + compliance role + log retention role
        template.resourceCountIs('AWS::S3::Bucket', 1); // Data classification bucket
        template.resourceCountIs('AWS::Lambda::Function', 3); // Compliance + sovereignty + log retention functions
        template.resourceCountIs('AWS::Config::ConfigRule', 1); // Security compliance rule
        template.resourceCountIs('AWS::Events::Rule', 2); // Compliance + sovereignty schedules
        template.resourceCountIs('AWS::Logs::LogGroup', 1); // GDPR data processing logs
    });

    test('should apply proper tags to resources', () => {
        // Check that stack-level tags are applied
        const stackTags = {
            Environment: 'test',
            Project: 'test-project',
            Stack: 'Security',
            DataClassification: 'confidential',
            ComplianceStandards: 'SOC2,ISO27001,GDPR',
        };

        // Verify tags are applied to key resources
        template.hasResourceProperties('AWS::KMS::Key', {
            Tags: Match.arrayWith([
                { Key: 'Environment', Value: 'test' },
                { Key: 'Project', Value: 'test-project' },
                { Key: 'Stack', Value: 'Security' },
            ]),
        });
    });

    describe('Cross-region disabled configuration', () => {
        test('should not create cross-region KMS key when disabled', () => {
            const singleRegionApp = new cdk.App();
            const singleRegionStack = new SecurityStack(singleRegionApp, 'TestSingleRegionSecurityStack', {
                environment: 'test',
                projectName: 'test-project',
                region: 'us-east-1',
                crossRegionEnabled: false,
                complianceStandards: ['SOC2'],
                dataClassification: 'internal'
            });
            const singleRegionTemplate = Template.fromStack(singleRegionStack);
            
            singleRegionTemplate.resourceCountIs('AWS::KMS::Key', 1); // Only main key
            singleRegionTemplate.resourceCountIs('AWS::Lambda::Function', 2); // Compliance + log retention functions
        });

        test('should not output cross-region KMS key information when disabled', () => {
            const singleRegionApp = new cdk.App();
            const singleRegionStack = new SecurityStack(singleRegionApp, 'TestSingleRegionSecurityStack', {
                environment: 'test',
                projectName: 'test-project',
                region: 'us-east-1',
                crossRegionEnabled: false,
                complianceStandards: ['SOC2'],
                dataClassification: 'internal'
            });
            const singleRegionTemplate = Template.fromStack(singleRegionStack);
            
            const outputs = singleRegionTemplate.toJSON().Outputs;
            expect(outputs).not.toHaveProperty('CrossRegionKMSKeyId');
            expect(outputs).not.toHaveProperty('CrossRegionKMSKeyArn');
        });
    });

    describe('GDPR compliance configuration', () => {
        test('should create GDPR-specific bucket policies', () => {
            template.hasResourceProperties('AWS::S3::BucketPolicy', {
                PolicyDocument: {
                    Statement: Match.arrayWith([
                        Match.objectLike({
                            Sid: 'GDPRDataProtection',
                            Effect: 'Deny',
                            Action: ['s3:GetObject', 's3:PutObject'],
                        }),
                    ]),
                },
            });
        });
    });
});