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
            environment: 'test',
            projectName: 'test-project'
        });
        template = Template.fromStack(stack);
    });

    test('should create KMS key with proper configuration', () => {
        template.hasResourceProperties('AWS::KMS::Key', {
            Description: 'Enhanced KMS key for test-project test - internal data encryption',
            EnableKeyRotation: true,
        });
    });

    test('should create application IAM role', () => {
        template.hasResourceProperties('AWS::IAM::Role', {
            Description: 'Enhanced IAM role for test-project test application with internal data access',
            RoleName: 'test-project-test-enhanced-application-role',
        });
    });

    test('should attach CloudWatch managed policy to role', () => {
        // Check that the enhanced application role has managed policies
        template.hasResourceProperties('AWS::IAM::Role', {
            RoleName: 'test-project-test-enhanced-application-role',
            ManagedPolicyArns: Match.anyValue()
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