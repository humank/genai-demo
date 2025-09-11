import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { DisasterRecoveryStack } from '../lib/stacks/disaster-recovery-stack';

describe('DisasterRecoveryStack', () => {
    let app: cdk.App;
    let stack: DisasterRecoveryStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App({
            context: {
                'genai-demo:environments': {
                    'production': {
                        'vpc-cidr': '10.2.0.0/16',
                        'nat-gateways': 3,
                        'eks-node-type': 'm6g.large',
                        'eks-min-nodes': 2,
                        'eks-max-nodes': 10,
                        'rds-instance-type': 'db.r6g.large',
                        'rds-storage': 100,
                        'rds-multi-az': true,
                        'msk-instance-type': 'kafka.m5.large',
                        'msk-brokers': 3
                    },
                    'production-dr': {
                        'vpc-cidr': '10.3.0.0/16',
                        'nat-gateways': 3,
                        'eks-node-type': 'm6g.large',
                        'eks-min-nodes': 1,
                        'eks-max-nodes': 8,
                        'rds-instance-type': 'db.r6g.large',
                        'rds-storage': 100,
                        'rds-multi-az': true,
                        'msk-instance-type': 'kafka.m5.large',
                        'msk-brokers': 3
                    }
                },
                'genai-demo:regions': {
                    'primary': 'ap-east-2',
                    'secondary': 'ap-northeast-1',
                    'regions': {
                        'ap-east-2': {
                            'name': 'Taiwan',
                            'type': 'primary',
                            'backup-retention': {
                                'rds': 30,
                                'logs': 90
                            }
                        },
                        'ap-northeast-1': {
                            'name': 'Tokyo',
                            'type': 'secondary',
                            'backup-retention': {
                                'rds': 30,
                                'logs': 90
                            }
                        }
                    }
                },
                'genai-demo:multi-region': {
                    'enable-dr': true,
                    'enable-cross-region-peering': true,
                    'enable-cross-region-replication': true,
                    'failover-rto-minutes': 1,
                    'failover-rpo-minutes': 0
                },
                'genai-demo:networking': {
                    'availability-zones': 3,
                    'enable-vpc-flow-logs': true,
                    'enable-dns-hostnames': true,
                    'enable-dns-support': true
                }
            }
        });
    });

    describe('when DR is enabled for production environment', () => {
        beforeEach(() => {
            stack = new DisasterRecoveryStack(app, 'TestDRStack', {
                environment: 'production',
                projectName: 'genai-demo',
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                domain: 'dr.kimkao.io',
                env: {
                    account: '123456789012',
                    region: 'ap-northeast-1'
                }
            });

            template = Template.fromStack(stack);
        });

        test('should create VPC with DR-specific CIDR', () => {
            template.hasResourceProperties('AWS::EC2::VPC', {
                CidrBlock: '10.3.0.0/16'
            });
        });

        test('should create Application Load Balancer for DR', () => {
            template.hasResourceProperties('AWS::ElasticLoadBalancingV2::LoadBalancer', {
                Scheme: 'internet-facing',
                Type: 'application'
            });
        });

        test('should create ACM certificate for DR domain', () => {
            template.hasResourceProperties('AWS::CertificateManager::Certificate', {
                DomainName: 'dr.kimkao.io',
                ValidationMethod: 'DNS'
            });
        });

        test('should create CloudWatch dashboard for DR monitoring', () => {
            template.hasResourceProperties('AWS::CloudWatch::Dashboard', {
                DashboardName: 'genai-demo-production-dr-monitoring'
            });
        });

        test('should create Systems Manager parameters for DR configuration', () => {
            template.hasResourceProperties('AWS::SSM::Parameter', {
                Name: '/genai-demo/production/dr/configuration',
                Type: 'String'
            });
        });

        test('should create SNS topic for cross-region replication notifications', () => {
            template.hasResourceProperties('AWS::SNS::Topic', {
                TopicName: 'genai-demo-production-dr-replication-notifications'
            });
        });

        test('should have proper DR tags applied', () => {
            template.hasResourceProperties('AWS::EC2::VPC', {
                Tags: [
                    {
                        Key: 'Environment',
                        Value: 'production-dr'
                    },
                    {
                        Key: 'Project',
                        Value: 'genai-demo'
                    },
                    {
                        Key: 'RegionType',
                        Value: 'secondary'
                    }
                ]
            });
        });

        test('should create outputs for DR stack references', () => {
            const outputs = template.findOutputs('*');
            expect(outputs).toHaveProperty('DRRegion');
            expect(outputs).toHaveProperty('DREnvironment');
            expect(outputs).toHaveProperty('DRDomain');
            expect(outputs).toHaveProperty('DRDeploymentTimestamp');
        });

        test('should configure region-specific resource sizing', () => {
            // Verify that DR configuration adjusts resource sizing
            // This would be validated through the Systems Manager parameter content
            template.hasResourceProperties('AWS::SSM::Parameter', {
                Name: '/genai-demo/production/dr/config/eks-min-nodes'
            });
        });
    });

    describe('when DR is disabled', () => {
        beforeEach(() => {
            const appWithDisabledDR = new cdk.App({
                context: {
                    'genai-demo:multi-region': {
                        'enable-dr': false
                    }
                }
            });

            stack = new DisasterRecoveryStack(appWithDisabledDR, 'TestDRStackDisabled', {
                environment: 'production',
                projectName: 'genai-demo',
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                env: {
                    account: '123456789012',
                    region: 'ap-northeast-1'
                }
            });

            template = Template.fromStack(stack);
        });

        test('should not create any DR resources when disabled', () => {
            template.resourceCountIs('AWS::EC2::VPC', 0);
            template.resourceCountIs('AWS::ElasticLoadBalancingV2::LoadBalancer', 0);
            template.resourceCountIs('AWS::CertificateManager::Certificate', 0);
            template.resourceCountIs('AWS::CloudWatch::Dashboard', 0);
        });
    });

    describe('when environment is not production', () => {
        beforeEach(() => {
            stack = new DisasterRecoveryStack(app, 'TestDRStackDev', {
                environment: 'development',
                projectName: 'genai-demo',
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                env: {
                    account: '123456789012',
                    region: 'ap-northeast-1'
                }
            });

            template = Template.fromStack(stack);
        });

        test('should not create DR resources for non-production environment', () => {
            template.resourceCountIs('AWS::EC2::VPC', 0);
            template.resourceCountIs('AWS::ElasticLoadBalancingV2::LoadBalancer', 0);
            template.resourceCountIs('AWS::CertificateManager::Certificate', 0);
        });
    });
});