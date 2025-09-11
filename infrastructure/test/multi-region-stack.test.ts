import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as route53 from 'aws-cdk-lib/aws-route53';
import { MultiRegionStack } from '../lib/stacks/multi-region-stack';

describe('MultiRegionStack', () => {
    let app: cdk.App;
    let stack: MultiRegionStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App({
            context: {
                'genai-demo:multi-region': {
                    'enable-dr': true,
                    'enable-cross-region-peering': true,
                    'enable-cross-region-replication': true,
                    'failover-rto-minutes': 1,
                    'failover-rpo-minutes': 0,
                    'health-check-interval': 30,
                    'health-check-failure-threshold': 3
                },
                'genai-demo:regions': {
                    'primary': 'ap-east-2',
                    'secondary': 'ap-northeast-1'
                }
            }
        });
    });

    describe('when multi-region is enabled for production', () => {
        beforeEach(() => {
            // Create mock VPC for testing
            const mockVpc = ec2.Vpc.fromVpcAttributes(stack, 'MockVpc', {
                vpcId: 'vpc-12345',
                availabilityZones: ['ap-east-2a', 'ap-east-2b'],
                publicSubnetIds: ['subnet-12345', 'subnet-67890'],
                privateSubnetIds: ['subnet-abcde', 'subnet-fghij']
            });

            // Create mock hosted zone
            const mockHostedZone = route53.HostedZone.fromHostedZoneAttributes(stack, 'MockHostedZone', {
                hostedZoneId: 'Z123456789',
                zoneName: 'kimkao.io'
            });

            stack = new MultiRegionStack(app, 'TestMultiRegionStack', {
                environment: 'production',
                projectName: 'genai-demo',
                regionType: 'primary',
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                domain: 'kimkao.io',
                primaryVpc: mockVpc,
                hostedZone: mockHostedZone,
                env: {
                    account: '123456789012',
                    region: 'ap-east-2'
                }
            });

            template = Template.fromStack(stack);
        });

        test('should create Route 53 health check for primary region', () => {
            template.hasResourceProperties('AWS::Route53::HealthCheck', {
                Type: 'HTTPS',
                ResourcePath: '/actuator/health',
                FullyQualifiedDomainName: 'api.kimkao.io',
                Port: 443,
                RequestInterval: 30,
                FailureThreshold: 3
            });
        });

        test('should create CloudWatch alarms for health check monitoring', () => {
            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                ComparisonOperator: 'LessThanThreshold',
                EvaluationPeriods: 3,
                Threshold: 1,
                TreatMissingData: 'breaching'
            });
        });

        test('should create SNS topic for multi-region alerts', () => {
            template.hasResourceProperties('AWS::SNS::Topic', {
                TopicName: 'genai-demo-production-primary-multi-region-alerts'
            });
        });

        test('should have proper tags applied', () => {
            template.hasResourceProperties('AWS::Route53::HealthCheck', {
                Tags: [
                    {
                        Key: 'Environment',
                        Value: 'production'
                    },
                    {
                        Key: 'Project',
                        Value: 'genai-demo'
                    },
                    {
                        Key: 'RegionType',
                        Value: 'primary'
                    }
                ]
            });
        });

        test('should create outputs for cross-stack references', () => {
            const outputs = template.findOutputs('*');
            expect(outputs).toHaveProperty('RegionType');
            expect(outputs).toHaveProperty('MultiRegionEnabled');
        });
    });

    describe('when multi-region is disabled', () => {
        beforeEach(() => {
            const appWithDisabledMR = new cdk.App({
                context: {
                    'genai-demo:multi-region': {
                        'enable-dr': false
                    }
                }
            });

            stack = new MultiRegionStack(appWithDisabledMR, 'TestMultiRegionStackDisabled', {
                environment: 'production',
                projectName: 'genai-demo',
                regionType: 'primary',
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                env: {
                    account: '123456789012',
                    region: 'ap-east-2'
                }
            });

            template = Template.fromStack(stack);
        });

        test('should not create any multi-region resources', () => {
            template.resourceCountIs('AWS::Route53::HealthCheck', 0);
            template.resourceCountIs('AWS::EC2::VPCPeeringConnection', 0);
            template.resourceCountIs('AWS::CertificateManager::Certificate', 0);
        });
    });

    describe('when environment is not production', () => {
        beforeEach(() => {
            stack = new MultiRegionStack(app, 'TestMultiRegionStackDev', {
                environment: 'development',
                projectName: 'genai-demo',
                regionType: 'primary',
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                env: {
                    account: '123456789012',
                    region: 'ap-east-2'
                }
            });

            template = Template.fromStack(stack);
        });

        test('should not create any multi-region resources for non-production environment', () => {
            template.resourceCountIs('AWS::Route53::HealthCheck', 0);
            template.resourceCountIs('AWS::EC2::VPCPeeringConnection', 0);
            template.resourceCountIs('AWS::CertificateManager::Certificate', 0);
        });
    });
});