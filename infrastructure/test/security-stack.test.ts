import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { SecurityStack } from '../src/stacks/security-stack';

describe('SecurityStack', () => {
    let app: cdk.App;
    let stack: SecurityStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App();
        stack = new SecurityStack(app, 'TestSecurityStack', {});
        template = Template.fromStack(stack);
    });

    test('should create KMS key with proper configuration', () => {
        template.hasResourceProperties('AWS::KMS::Key', {
            Description: 'KMS key for GenAI Demo application encryption',
            EnableKeyRotation: true,
        });
    });

    test('should create application IAM role with EC2 service principal', () => {
        template.hasResourceProperties('AWS::IAM::Role', {
            AssumeRolePolicyDocument: {
                Statement: [
                    {
                        Effect: 'Allow',
                        Principal: {
                            Service: 'ec2.amazonaws.com',
                        },
                        Action: 'sts:AssumeRole',
                    },
                ],
            },
            Description: 'IAM role for GenAI Demo application',
            ManagedPolicyArns: [
                {
                    'Fn::Join': [
                        '',
                        [
                            'arn:',
                            { Ref: 'AWS::Partition' },
                            ':iam::aws:policy/CloudWatchAgentServerPolicy',
                        ],
                    ],
                },
            ],
        });
    });

    test('should grant KMS key access to application role', () => {
        // Check that the application role has a policy for KMS access
        template.hasResourceProperties('AWS::IAM::Policy', {
            PolicyDocument: {
                Statement: [
                    {
                        Effect: 'Allow',
                        Action: [
                            'kms:Decrypt',
                            'kms:Encrypt',
                            'kms:ReEncrypt*',
                            'kms:GenerateDataKey*',
                        ],
                        Resource: {
                            'Fn::GetAtt': ['ApplicationKeyA04DA845', 'Arn'],
                        },
                    },
                ],
            },
        });
    });

    test('should output KMS key ID', () => {
        template.hasOutput('KMSKeyId', {
            Value: {
                Ref: 'ApplicationKeyA04DA845',
            },
            Export: {
                Name: 'TestSecurityStack-KMSKeyId',
            },
        });
    });

    test('should output application role ARN', () => {
        template.hasOutput('ApplicationRoleArn', {
            Value: {
                'Fn::GetAtt': ['ApplicationRole90C00724', 'Arn'],
            },
            Export: {
                Name: 'TestSecurityStack-ApplicationRoleArn',
            },
        });
    });

    test('should have exactly one KMS key', () => {
        template.resourceCountIs('AWS::KMS::Key', 1);
    });

    test('should have exactly one IAM role', () => {
        template.resourceCountIs('AWS::IAM::Role', 1);
    });

    test('should have exactly one IAM policy for KMS access', () => {
        template.resourceCountIs('AWS::IAM::Policy', 1);
    });
});