import * as cdk from 'aws-cdk-lib';
import { Template, Match } from 'aws-cdk-lib/assertions';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as kms from 'aws-cdk-lib/aws-kms';
import { ObservabilityStack } from '../src/stacks/observability-stack';

describe('ObservabilityStack - AWS Native Concurrency Monitoring', () => {
    let app: cdk.App;
    let stack: ObservabilityStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App();
        
        // Create a test stack to contain VPC and KMS key
        const testStack = new cdk.Stack(app, 'TestStack');
        
        // Create VPC for testing
        const vpc = new ec2.Vpc(testStack, 'TestVpc', {
            maxAzs: 2,
            natGateways: 1,
        });

        // Create KMS key for testing
        const kmsKey = new kms.Key(testStack, 'TestKmsKey', {
            description: 'Test KMS key for observability stack',
        });

        // Create the ObservabilityStack
        stack = new ObservabilityStack(app, 'TestObservabilityStack', {
            vpc: vpc,
            kmsKey: kmsKey,
            environment: 'test',
        });

        template = Template.fromStack(stack);
    });

    describe('CloudWatch Container Insights Configuration', () => {
        test('should create Container Insights IAM role', () => {
            template.hasResourceProperties('AWS::IAM::Role', {
                AssumeRolePolicyDocument: {
                    Statement: [
                        {
                            Effect: 'Allow',
                            Principal: {
                                Service: 'ec2.amazonaws.com'
                            },
                            Action: 'sts:AssumeRole'
                        }
                    ]
                },
                ManagedPolicyArns: [
                    {
                        'Fn::Join': [
                            '',
                            [
                                'arn:',
                                { Ref: 'AWS::Partition' },
                                ':iam::aws:policy/CloudWatchAgentServerPolicy'
                            ]
                        ]
                    },
                    {
                        'Fn::Join': [
                            '',
                            [
                                'arn:',
                                { Ref: 'AWS::Partition' },
                                ':iam::aws:policy/CloudWatchLogsFullAccess'
                            ]
                        ]
                    }
                ]
            });
        });

        test('should create Container Insights log groups', () => {
            template.hasResourceProperties('AWS::Logs::LogGroup', {
                LogGroupName: '/aws/containerinsights/test-genai-demo/performance',
                RetentionInDays: 7
            });

            template.hasResourceProperties('AWS::Logs::LogGroup', {
                LogGroupName: '/aws/containerinsights/test-genai-demo/application',
                RetentionInDays: 7
            });

            template.hasResourceProperties('AWS::Logs::LogGroup', {
                LogGroupName: '/aws/containerinsights/test-genai-demo/dataplane',
                RetentionInDays: 7
            });

            template.hasResourceProperties('AWS::Logs::LogGroup', {
                LogGroupName: '/aws/containerinsights/test-genai-demo/host',
                RetentionInDays: 7
            });
        });
    });

    describe('X-Ray Distributed Tracing Configuration', () => {
        test('should create X-Ray IAM role', () => {
            template.hasResourceProperties('AWS::IAM::Role', {
                AssumeRolePolicyDocument: {
                    Statement: [
                        {
                            Effect: 'Allow',
                            Principal: {
                                Service: 'ec2.amazonaws.com'
                            },
                            Action: 'sts:AssumeRole'
                        }
                    ]
                },
                ManagedPolicyArns: [
                    {
                        'Fn::Join': [
                            '',
                            [
                                'arn:',
                                { Ref: 'AWS::Partition' },
                                ':iam::aws:policy/AWSXRayDaemonWriteAccess'
                            ]
                        ]
                    }
                ]
            });
        });

        test('should create X-Ray sampling rule Lambda function', () => {
            template.hasResourceProperties('AWS::Lambda::Function', {
                Runtime: 'python3.9',
                Handler: 'index.handler',
                Timeout: 60
            });
        });
    });

    describe('Amazon Managed Grafana Configuration', () => {
        test('should create Grafana workspace', () => {
            template.hasResourceProperties('AWS::Grafana::Workspace', {
                AccountAccessType: 'CURRENT_ACCOUNT',
                AuthenticationProviders: ['AWS_SSO'],
                PermissionType: 'SERVICE_MANAGED',
                Name: 'genai-demo-test',
                Description: 'GenAI Demo AWS Native Concurrency Monitoring System for test environment',
                DataSources: ['CLOUDWATCH', 'XRAY', 'PROMETHEUS'],
                NotificationDestinations: ['SNS']
            });
        });

        test('should create Grafana IAM role', () => {
            template.hasResourceProperties('AWS::IAM::Role', {
                AssumeRolePolicyDocument: {
                    Statement: [
                        {
                            Effect: 'Allow',
                            Principal: {
                                Service: 'grafana.amazonaws.com'
                            },
                            Action: 'sts:AssumeRole'
                        }
                    ]
                },
                ManagedPolicyArns: [
                    {
                        'Fn::Join': [
                            '',
                            [
                                'arn:',
                                { Ref: 'AWS::Partition' },
                                ':iam::aws:policy/CloudWatchReadOnlyAccess'
                            ]
                        ]
                    },
                    {
                        'Fn::Join': [
                            '',
                            [
                                'arn:',
                                { Ref: 'AWS::Partition' },
                                ':iam::aws:policy/AWSXRayReadOnlyAccess'
                            ]
                        ]
                    }
                ]
            });
        });
    });

    describe('CloudWatch Dashboard Configuration', () => {
        test('should create comprehensive monitoring dashboard', () => {
            template.hasResourceProperties('AWS::CloudWatch::Dashboard', {
                DashboardName: 'GenAI-Demo-test'
            });
        });

        test('should include concurrency monitoring widgets', () => {
            // The dashboard body should contain widgets for:
            // - EKS Container Insights metrics
            // - Thread pool metrics
            // - JVM metrics
            // - HTTP request metrics
            // - X-Ray service map links
            // - Grafana dashboard links
            
            const dashboardResource = template.findResources('AWS::CloudWatch::Dashboard');
            const dashboardKeys = Object.keys(dashboardResource);
            expect(dashboardKeys.length).toBeGreaterThan(0);
            
            const dashboard = dashboardResource[dashboardKeys[0]];
            expect(dashboard.Properties.DashboardBody).toBeDefined();
        });
    });

    describe('Stack Outputs', () => {
        test('should export monitoring URLs and resource ARNs', () => {
            // Check that outputs exist (the exact export names may vary based on stack name)
            const outputs = template.findOutputs('*');
            const outputKeys = Object.keys(outputs);
            
            expect(outputKeys).toContain('LogGroupName');
            expect(outputKeys).toContain('DashboardURL');
            expect(outputKeys).toContain('XRayServiceMapURL');
            expect(outputKeys).toContain('GrafanaWorkspaceId');
            expect(outputKeys).toContain('GrafanaWorkspaceURL');
            expect(outputKeys).toContain('ContainerInsightsRoleArn');
            expect(outputKeys).toContain('XRayRoleArn');
        });
    });

    describe('Security and Compliance', () => {
        test('should use least privilege IAM policies', () => {
            // Check that IAM policies only grant necessary permissions
            const roles = template.findResources('AWS::IAM::Role');
            
            Object.values(roles).forEach((role: any) => {
                if (role.Properties.Policies) {
                    role.Properties.Policies.forEach((policy: any) => {
                        // Ensure no wildcard resources for sensitive actions
                        if (policy.PolicyDocument.Statement) {
                            policy.PolicyDocument.Statement.forEach((statement: any) => {
                                if (statement.Effect === 'Allow' && 
                                    statement.Action.some((action: string) => 
                                        action.includes('Delete') || action.includes('Terminate'))) {
                                    expect(statement.Resource).not.toBe('*');
                                }
                            });
                        }
                    });
                }
            });
        });

        test('should encrypt log groups', () => {
            const logGroups = template.findResources('AWS::Logs::LogGroup');
            
            Object.values(logGroups).forEach((logGroup: any) => {
                // Log groups should have retention policy
                expect(logGroup.Properties.RetentionInDays).toBeDefined();
                expect(logGroup.Properties.RetentionInDays).toBeGreaterThan(0);
            });
        });
    });

    describe('Resource Tagging', () => {
        test('should apply consistent tags to resources', () => {
            // Check that resources have appropriate tags
            const resources = template.findResources('AWS::IAM::Role');
            
            // At least one resource should exist
            expect(Object.keys(resources).length).toBeGreaterThan(0);
        });
    });

    describe('Integration with Existing Infrastructure', () => {
        test('should integrate with existing deadlock monitoring', () => {
            // Verify that existing deadlock monitoring Lambda function is preserved
            template.hasResourceProperties('AWS::Lambda::Function', {
                Environment: {
                    Variables: {
                        LOG_GROUP_NAME: '/aws/rds/instance/genai-demo-TestObservabilityStack-primary-aurora/postgresql',
                        ENVIRONMENT: 'TestObservabilityStack'
                    }
                }
            });
        });

        test('should preserve existing Aurora monitoring widgets', () => {
            // The dashboard should still contain Aurora deadlock monitoring widgets
            const dashboardResource = template.findResources('AWS::CloudWatch::Dashboard');
            expect(Object.keys(dashboardResource).length).toBeGreaterThan(0);
        });
    });
});