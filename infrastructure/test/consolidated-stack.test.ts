import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { AlertingStack } from '../src/stacks/alerting-stack';
import { AnalyticsStack } from '../src/stacks/analytics-stack';
import { CoreInfrastructureStack } from '../src/stacks/core-infrastructure-stack';
import { NetworkStack } from '../src/stacks/network-stack';
import { ObservabilityStack } from '../src/stacks/observability-stack';
import { SecurityStack } from '../src/stacks/security-stack';

describe('Consolidated Infrastructure Tests', () => {
    let app: cdk.App;

    beforeEach(() => {
        app = new cdk.App();
    });

    describe('NetworkStack', () => {
        let networkStack: NetworkStack;
        let template: Template;

        beforeEach(() => {
            networkStack = new NetworkStack(app, 'TestNetworkStack', {});
            template = Template.fromStack(networkStack);
        });

        test('should create VPC with correct CIDR', () => {
            template.hasResourceProperties('AWS::EC2::VPC', {
                CidrBlock: '10.0.0.0/16',
            });
        });

        test('should create security groups', () => {
            template.resourceCountIs('AWS::EC2::SecurityGroup', 3);
        });

        test('should create subnets', () => {
            template.resourceCountIs('AWS::EC2::Subnet', 6);
        });

        test('should create outputs', () => {
            template.hasOutput('VpcId', {});
            template.hasOutput('ALBSecurityGroupId', {});
        });
    });

    describe('SecurityStack', () => {
        let securityStack: SecurityStack;
        let template: Template;

        beforeEach(() => {
            securityStack = new SecurityStack(app, 'TestSecurityStack', {});
            template = Template.fromStack(securityStack);
        });

        test('should create KMS key', () => {
            template.hasResourceProperties('AWS::KMS::Key', {
                Description: 'KMS key for GenAI Demo application encryption',
                EnableKeyRotation: true,
            });
        });

        test('should create IAM role', () => {
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
            });
        });

        test('should create outputs', () => {
            template.hasOutput('KMSKeyId', {});
            template.hasOutput('ApplicationRoleArn', {});
        });
    });

    describe('CoreInfrastructureStack', () => {
        let coreStack: CoreInfrastructureStack;
        let networkStack: NetworkStack;
        let securityStack: SecurityStack;
        let template: Template;

        beforeEach(() => {
            networkStack = new NetworkStack(app, 'TestNetworkStack', {});
            securityStack = new SecurityStack(app, 'TestSecurityStack', {});

            coreStack = new CoreInfrastructureStack(app, 'TestCoreStack', {
                vpc: networkStack.vpc,
                securityGroups: networkStack.securityGroups,
                kmsKey: securityStack.kmsKey,
            });

            template = Template.fromStack(coreStack);
        });

        test('should create Application Load Balancer', () => {
            template.hasResourceProperties('AWS::ElasticLoadBalancingV2::LoadBalancer', {
                Type: 'application',
                Scheme: 'internet-facing',
            });
        });

        test('should create target group', () => {
            template.hasResourceProperties('AWS::ElasticLoadBalancingV2::TargetGroup', {
                Port: 8080,
                Protocol: 'HTTP',
                TargetType: 'ip',
            });
        });

        test('should create HTTP listener', () => {
            template.hasResourceProperties('AWS::ElasticLoadBalancingV2::Listener', {
                Port: 80,
                Protocol: 'HTTP',
            });
        });
    });

    describe('AlertingStack', () => {
        let alertingStack: AlertingStack;
        let networkStack: NetworkStack;
        let template: Template;

        beforeEach(() => {
            networkStack = new NetworkStack(app, 'TestNetworkStack', {});

            alertingStack = new AlertingStack(app, 'TestAlertingStack', {
                environment: 'test',
                region: 'us-east-1',
                applicationName: 'genai-demo',
                alertingConfig: {
                    criticalAlerts: {
                        emailAddresses: ['test@example.com'],
                    },
                    warningAlerts: {
                        emailAddresses: ['test@example.com'],
                    },
                    infoAlerts: {
                        emailAddresses: ['test@example.com'],
                    },
                },
            });

            template = Template.fromStack(alertingStack);
        });

        test('should create SNS topics', () => {
            template.resourceCountIs('AWS::SNS::Topic', 3);
        });

        test('should create SNS subscriptions', () => {
            template.resourceCountIs('AWS::SNS::Subscription', 3);
        });
    });

    describe('ObservabilityStack', () => {
        let observabilityStack: ObservabilityStack;
        let networkStack: NetworkStack;
        let securityStack: SecurityStack;
        let template: Template;

        beforeEach(() => {
            networkStack = new NetworkStack(app, 'TestNetworkStack', {});
            securityStack = new SecurityStack(app, 'TestSecurityStack', {});

            observabilityStack = new ObservabilityStack(app, 'TestObservabilityStack', {
                vpc: networkStack.vpc,
                kmsKey: securityStack.kmsKey,
            });

            template = Template.fromStack(observabilityStack);
        });

        test('should create CloudWatch log groups', () => {
            template.resourceCountIs('AWS::Logs::LogGroup', 1);
        });

        test('should create CloudWatch dashboard', () => {
            template.resourceCountIs('AWS::CloudWatch::Dashboard', 1);
        });
    });

    describe('AnalyticsStack', () => {
        let analyticsStack: AnalyticsStack;
        let networkStack: NetworkStack;
        let securityStack: SecurityStack;
        let alertingStack: AlertingStack;
        let template: Template;

        beforeEach(() => {
            networkStack = new NetworkStack(app, 'TestNetworkStack', {});
            securityStack = new SecurityStack(app, 'TestSecurityStack', {});
            alertingStack = new AlertingStack(app, 'TestAlertingStack', {
                environment: 'test',
                region: 'us-east-1',
                applicationName: 'genai-demo',
                alertingConfig: {
                    criticalAlerts: {
                        emailAddresses: ['test@example.com'],
                    },
                    warningAlerts: {
                        emailAddresses: ['test@example.com'],
                    },
                    infoAlerts: {
                        emailAddresses: ['test@example.com'],
                    },
                },
            });

            // Create a mock MSK cluster for testing
            const mockMskCluster = {
                ref: 'mock-msk-cluster',
                attrArn: 'arn:aws:kafka:us-east-1:123456789012:cluster/mock-cluster/*'
            } as any;

            analyticsStack = new AnalyticsStack(app, 'TestAnalyticsStack', {
                environment: 'test',
                projectName: 'genai-demo',
                vpc: networkStack.vpc,
                kmsKey: securityStack.kmsKey,
                mskCluster: mockMskCluster,
                alertingTopic: alertingStack.criticalAlertsTopic,
                region: 'us-east-1',
            });

            template = Template.fromStack(analyticsStack);
        });

        test('should create S3 bucket for analytics', () => {
            template.resourceCountIs('AWS::S3::Bucket', 1);
        });

        test('should create Kinesis Data Firehose', () => {
            template.resourceCountIs('AWS::KinesisFirehose::DeliveryStream', 1);
        });

        test('should create Lambda function', () => {
            template.resourceCountIs('AWS::Lambda::Function', 1);
        });
    });

    describe('Stack Integration', () => {
        test('should create all stacks without errors', () => {
            const networkStack = new NetworkStack(app, 'NetworkStack', {});
            const securityStack = new SecurityStack(app, 'SecurityStack', {});

            const alertingStack = new AlertingStack(app, 'AlertingStack', {
                environment: 'test',
                region: 'us-east-1',
                applicationName: 'genai-demo',
                alertingConfig: {
                    criticalAlerts: {
                        emailAddresses: ['test@example.com'],
                    },
                    warningAlerts: {
                        emailAddresses: ['test@example.com'],
                    },
                    infoAlerts: {
                        emailAddresses: ['test@example.com'],
                    },
                },
            });

            const coreStack = new CoreInfrastructureStack(app, 'CoreStack', {
                vpc: networkStack.vpc,
                securityGroups: networkStack.securityGroups,
                kmsKey: securityStack.kmsKey,
            });

            const observabilityStack = new ObservabilityStack(app, 'ObservabilityStack', {
                vpc: networkStack.vpc,
                kmsKey: securityStack.kmsKey,
            });

            // Set up dependencies
            coreStack.addDependency(networkStack);
            coreStack.addDependency(securityStack);
            observabilityStack.addDependency(networkStack);
            observabilityStack.addDependency(securityStack);
            alertingStack.addDependency(networkStack);

            // Should not throw any errors
            expect(() => app.synth()).not.toThrow();
        });
    });
});