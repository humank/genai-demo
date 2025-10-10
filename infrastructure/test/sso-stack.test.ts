import * as cdk from 'aws-cdk-lib';
import * as sns from 'aws-cdk-lib/aws-sns';
import { Template, Match } from 'aws-cdk-lib/assertions';
import { SSOStack } from '../src/stacks/sso-stack';

describe('SSOStack', () => {
    let app: cdk.App;
    let stack: SSOStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App();
    });

    describe('Basic SSO Stack', () => {
        beforeEach(() => {
            stack = new SSOStack(app, 'TestSSOStack', {
                environment: 'test',
                projectName: 'genai-demo',
                region: 'us-east-1',
                ssoInstanceArn: 'arn:aws:sso:::instance/ssoins-1234567890abcdef'
            });
            template = Template.fromStack(stack);
        });

        test('should create basic permission sets', () => {
            // Verify permission sets are created
            template.hasResourceProperties('AWS::SSO::PermissionSet', {
                Name: 'genai-demo-test-Developer'
            });

            template.hasResourceProperties('AWS::SSO::PermissionSet', {
                Name: 'genai-demo-test-Admin'
            });

            template.hasResourceProperties('AWS::SSO::PermissionSet', {
                Name: 'genai-demo-test-ReadOnly'
            });

            template.hasResourceProperties('AWS::SSO::PermissionSet', {
                Name: 'genai-demo-test-DataAnalyst'
            });
        });

        test('should create cross-region access role', () => {
            template.hasResourceProperties('AWS::IAM::Role', {
                RoleName: 'genai-demo-test-cross-region-role'
            });
        });

        test('should have proper tags', () => {
            template.hasResourceProperties('AWS::SSO::PermissionSet', {
                Tags: Match.arrayWith([
                    {
                        Key: 'Component',
                        Value: 'Identity'
                    },
                    {
                        Key: 'Environment',
                        Value: 'test'
                    }
                ])
            });
        });
    });

    describe('Cross-Region SSO Stack', () => {
        let securityMonitoringTopic: sns.Topic;

        beforeEach(() => {
            // Create a separate stack for the topic to avoid scope issues
            const topicStack = new cdk.Stack(app, 'TestTopicStack');
            securityMonitoringTopic = new sns.Topic(topicStack, 'TestSecurityTopic', {
                topicName: 'test-security-monitoring'
            });

            stack = new SSOStack(app, 'TestCrossRegionSSOStack', {
                environment: 'production',
                projectName: 'genai-demo',
                region: 'us-east-1',
                ssoInstanceArn: 'arn:aws:sso:::instance/ssoins-1234567890abcdef',
                primaryRegion: 'us-east-1',
                secondaryRegions: ['us-west-2', 'eu-west-1'],
                crossRegionEnabled: true,
                securityMonitoringTopic
            });
            template = Template.fromStack(stack);
        });

        test('should create cross-region admin permission set', () => {
            template.hasResourceProperties('AWS::SSO::PermissionSet', {
                Name: 'genai-demo-production-CrossRegionAdmin'
            });
        });

        test('should create cross-region audit bucket', () => {
            template.hasResourceProperties('AWS::S3::Bucket', {
                BucketName: {
                    'Fn::Join': Match.anyValue()
                }
            });
        });

        test('should create cross-region CloudTrail', () => {
            template.hasResourceProperties('AWS::CloudTrail::Trail', {
                TrailName: 'genai-demo-production-cross-region-audit',
                IsMultiRegionTrail: true,
                IncludeGlobalServiceEvents: true,
                EnableLogFileValidation: true
            });
        });

        test('should create security event correlation function', () => {
            template.hasResourceProperties('AWS::Lambda::Function', {
                FunctionName: 'genai-demo-production-security-event-correlation',
                Runtime: 'python3.11',
                Timeout: 600
            });
        });

        test('should create EventBridge rules for security monitoring', () => {
            template.hasResourceProperties('AWS::Events::Rule', {
                Name: 'genai-demo-production-sso-events',
                EventPattern: {
                    source: ['aws.sso'],
                    'detail-type': ['AWS API Call via CloudTrail']
                }
            });

            template.hasResourceProperties('AWS::Events::Rule', {
                Name: 'genai-demo-production-iam-events',
                EventPattern: {
                    source: ['aws.iam'],
                    'detail-type': ['AWS API Call via CloudTrail']
                }
            });
        });

        test('should create region-specific execution roles', () => {
            // Should create roles for each secondary region
            template.hasResourceProperties('AWS::IAM::Role', {
                RoleName: 'genai-demo-production-us-west-2-execution-role'
            });

            template.hasResourceProperties('AWS::IAM::Role', {
                RoleName: 'genai-demo-production-eu-west-1-execution-role'
            });
        });

        test('should have enhanced cross-region permissions', () => {
            // Verify cross-region admin permission set exists
            template.hasResourceProperties('AWS::SSO::PermissionSet', {
                Name: 'genai-demo-production-CrossRegionAdmin',
                InlinePolicy: {
                    Version: '2012-10-17',
                    Statement: Match.anyValue()
                }
            });
        });

        test('should create proper outputs for cross-region setup', () => {
            template.hasOutput('CrossRegionAdminPermissionSetArn', {});
            template.hasOutput('CrossRegionAuditBucketName', {});
            template.hasOutput('CrossRegionCloudTrailArn', {});
            template.hasOutput('SecurityEventCorrelationFunctionArn', {});
            template.hasOutput('MultiRegionSSOEnabled', {
                Value: 'true'
            });
        });

        test('should have proper resource tagging', () => {
            template.hasResourceProperties('AWS::S3::Bucket', {
                Tags: Match.arrayWith([
                    Match.objectLike({
                        Key: 'Environment',
                        Value: 'production'
                    })
                ])
            });
        });
    });

    describe('Security Features', () => {
        beforeEach(() => {
            // Create a separate stack for the SNS topic
            const topicStack = new cdk.Stack(app, 'TestTopicStack');
            const securityMonitoringTopic = new sns.Topic(topicStack, 'TestSecurityTopic', {
                topicName: 'test-security-monitoring'
            });

            stack = new SSOStack(app, 'TestSecuritySSOStack', {
                environment: 'production',
                projectName: 'genai-demo',
                region: 'us-east-1',
                ssoInstanceArn: 'arn:aws:sso:::instance/ssoins-1234567890abcdef',
                primaryRegion: 'us-east-1',
                secondaryRegions: ['us-west-2'],
                crossRegionEnabled: true,
                securityMonitoringTopic
            });
            template = Template.fromStack(stack);
        });

        test('should enforce MFA for sensitive operations', () => {
            // Verify that admin permission set exists with security policies
            template.hasResourceProperties('AWS::SSO::PermissionSet', {
                Name: 'genai-demo-production-Admin',
                InlinePolicy: {
                    Version: '2012-10-17',
                    Statement: Match.arrayWith([
                        Match.objectLike({
                            Effect: 'Deny'
                        })
                    ])
                }
            });
        });

        test('should create encrypted audit bucket', () => {
            template.hasResourceProperties('AWS::S3::Bucket', {
                BucketEncryption: {
                    ServerSideEncryptionConfiguration: [
                        {
                            ServerSideEncryptionByDefault: {
                                SSEAlgorithm: 'aws:kms'
                            }
                        }
                    ]
                }
            });
        });

        test('should create KMS key for audit encryption', () => {
            template.hasResourceProperties('AWS::KMS::Key', {
                Description: 'Cross-region audit log encryption key for genai-demo-production',
                EnableKeyRotation: true
            });
        });

        test('should configure CloudTrail with insights', () => {
            template.hasResourceProperties('AWS::CloudTrail::Trail', {
                InsightSelectors: [
                    {
                        InsightType: 'ApiCallRateInsight'
                    }
                ]
            });
        });
    });

    describe('Error Handling', () => {
        test('should throw error when SSO instance ARN is missing', () => {
            expect(() => {
                new SSOStack(app, 'TestSSOStackNoArn', {
                    environment: 'test',
                    projectName: 'genai-demo',
                    region: 'us-east-1'
                    // Missing ssoInstanceArn
                });
            }).toThrow('SSO Instance ARN must be provided via props or CDK context');
        });
    });
});