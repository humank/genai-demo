import * as cdk from 'aws-cdk-lib';
import { Match, Template } from 'aws-cdk-lib/assertions';
import { SecurityStack } from '../../src/stacks/security-stack';

describe('SecurityStack', () => {
    let app: cdk.App;
    let stack: SecurityStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App();
        stack = new SecurityStack(app, 'TestSecurityStack', {
            env: { region: 'us-east-1', account: '123456789012' },
        });
        template = Template.fromStack(stack);
    });

    test('should create KMS key with proper configuration', () => {
        template.hasResourceProperties('AWS::KMS::Key', {
            Description: 'KMS key for GenAI Demo application encryption',
            EnableKeyRotation: true,
        });
    });

    test('should create application IAM role', () => {
        template.hasResourceProperties('AWS::IAM::Role', {
            AssumeRolePolicyDocument: {
                Statement: [
                    {
                        Action: 'sts:AssumeRole',
                        Effect: 'Allow',
                        Principal: {
                            Service: 'ec2.amazonaws.com',
                        },
                    },
                ],
            },
            Description: 'IAM role for GenAI Demo application',
        });
    });

    test('should attach CloudWatch managed policy to role', () => {
        template.hasResourceProperties('AWS::IAM::Role', {
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

    test('should create KMS key policy allowing role access', () => {
        // Check that KMS key has a policy with root account access
        template.hasResourceProperties('AWS::KMS::Key', {
            KeyPolicy: {
                Statement: Match.arrayWith([
                    Match.objectLike({
                        Action: 'kms:*',
                        Effect: 'Allow',
                        Principal: {
                            AWS: Match.anyValue()
                        },
                        Resource: '*',
                    })
                ])
            },
        });
    });

    test('should export KMS key ID', () => {
        template.hasOutput('KMSKeyId', {});
    });

    test('should export application role ARN', () => {
        template.hasOutput('ApplicationRoleArn', {});
    });
});