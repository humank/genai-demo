import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as route53 from 'aws-cdk-lib/aws-route53';
import { Route53FailoverStack } from '../lib/stacks/route53-failover-stack';

describe('Route53FailoverStack', () => {
    let app: cdk.App;
    let stack: Route53FailoverStack;
    let template: Template;
    let mockVpc: ec2.IVpc;
    let mockPrimaryALB: elbv2.IApplicationLoadBalancer;
    let mockSecondaryALB: elbv2.IApplicationLoadBalancer;
    let mockHostedZone: route53.IHostedZone;

    beforeEach(() => {
        app = new cdk.App({
            context: {
                'genai-demo:multi-region': {
                    'enable-dr': true,
                    'health-check-interval': 30,
                    'health-check-failure-threshold': 3,
                    'failover-rto-minutes': 1,
                    'failover-rpo-minutes': 0
                }
            }
        });

        // Create mock resources
        const testStack = new cdk.Stack(app, 'TestStack');

        mockVpc = new ec2.Vpc(testStack, 'MockVpc', {
            maxAzs: 2,
            ipAddresses: ec2.IpAddresses.cidr('10.0.0.0/16')
        });

        mockPrimaryALB = new elbv2.ApplicationLoadBalancer(testStack, 'MockPrimaryALB', {
            vpc: mockVpc,
            internetFacing: true
        });

        mockSecondaryALB = new elbv2.ApplicationLoadBalancer(testStack, 'MockSecondaryALB', {
            vpc: mockVpc,
            internetFacing: true
        });

        mockHostedZone = new route53.HostedZone(testStack, 'MockHostedZone', {
            zoneName: 'kimkao.io'
        });
    });

    describe('when both primary and secondary regions are configured', () => {
        beforeEach(() => {
            stack = new Route53FailoverStack(app, 'TestRoute53FailoverStack', {
                environment: 'production',
                projectName: 'genai-demo',
                domain: 'kimkao.io',
                hostedZone: mockHostedZone,
                primaryLoadBalancer: mockPrimaryALB,
                secondaryLoadBalancer: mockSecondaryALB,
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                env: {
                    account: '123456789012',
                    region: 'ap-east-2'
                }
            });

            template = Template.fromStack(stack);
        });

        test('should create health checks for both regions', () => {
            template.resourceCountIs('AWS::Route53::HealthCheck', 2);

            template.hasResourceProperties('AWS::Route53::HealthCheck', {
                Type: 'HTTPS',
                ResourcePath: '/actuator/health',
                FullyQualifiedDomainName: 'api.kimkao.io',
                Port: 443,
                RequestInterval: 30,
                FailureThreshold: 3,
                MeasureLatency: true,
                EnableSNI: true
            });

            template.hasResourceProperties('AWS::Route53::HealthCheck', {
                Type: 'HTTPS',
                ResourcePath: '/actuator/health',
                FullyQualifiedDomainName: 'api-dr.kimkao.io',
                Port: 443,
                RequestInterval: 30,
                FailureThreshold: 3,
                MeasureLatency: true,
                EnableSNI: true
            });
        });

        test('should create failover DNS records', () => {
            template.hasResourceProperties('AWS::Route53::RecordSet', {
                Name: 'api.kimkao.io.',
                Type: 'A',
                Failover: 'PRIMARY'
            });

            template.hasResourceProperties('AWS::Route53::RecordSet', {
                Name: 'api.kimkao.io.',
                Type: 'A',
                Failover: 'SECONDARY'
            });
        });

        test('should create latency-based routing records', () => {
            template.hasResourceProperties('AWS::Route53::RecordSet', {
                Name: 'api-latency.kimkao.io.',
                Type: 'A',
                Region: 'ap-east-2'
            });

            template.hasResourceProperties('AWS::Route53::RecordSet', {
                Name: 'api-latency.kimkao.io.',
                Type: 'A',
                Region: 'ap-northeast-1'
            });
        });

        test('should create CloudWatch dashboard for failover monitoring', () => {
            template.hasResourceProperties('AWS::CloudWatch::Dashboard', {
                DashboardName: 'genai-demo-production-failover-monitoring'
            });
        });

        test('should create CloudWatch alarms for health check failures', () => {
            template.resourceCountIs('AWS::CloudWatch::Alarm', 2);

            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'genai-demo-production-primary-health-check-failure',
                ComparisonOperator: 'LessThanThreshold',
                EvaluationPeriods: 3,
                Threshold: 1,
                TreatMissingData: 'breaching'
            });

            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'genai-demo-production-secondary-health-check-failure',
                ComparisonOperator: 'LessThanThreshold',
                EvaluationPeriods: 3,
                Threshold: 1,
                TreatMissingData: 'breaching'
            });
        });

        test('should create SNS topic for failover alerts', () => {
            template.hasResourceProperties('AWS::SNS::Topic', {
                TopicName: 'genai-demo-production-failover-alerts'
            });
        });

        test('should have proper tags applied to health checks', () => {
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
                    },
                    {
                        Key: 'Service',
                        Value: 'Route53HealthCheck'
                    }
                ]
            });
        });

        test('should create outputs for DNS names and configuration', () => {
            const outputs = template.findOutputs('*');
            expect(outputs).toHaveProperty('PrimaryHealthCheckId');
            expect(outputs).toHaveProperty('SecondaryHealthCheckId');
            expect(outputs).toHaveProperty('FailoverDnsName');
            expect(outputs).toHaveProperty('LatencyBasedDnsName');
            expect(outputs).toHaveProperty('FailoverConfiguration');
        });

        test('should configure short TTL for faster failover', () => {
            template.hasResourceProperties('AWS::Route53::RecordSet', {
                TTL: 60
            });
        });
    });

    describe('when only primary region is configured', () => {
        beforeEach(() => {
            stack = new Route53FailoverStack(app, 'TestRoute53FailoverStackPrimaryOnly', {
                environment: 'production',
                projectName: 'genai-demo',
                domain: 'kimkao.io',
                hostedZone: mockHostedZone,
                primaryLoadBalancer: mockPrimaryALB,
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                env: {
                    account: '123456789012',
                    region: 'ap-east-2'
                }
            });

            template = Template.fromStack(stack);
        });

        test('should create only primary health check', () => {
            template.resourceCountIs('AWS::Route53::HealthCheck', 1);

            template.hasResourceProperties('AWS::Route53::HealthCheck', {
                FullyQualifiedDomainName: 'api.kimkao.io'
            });
        });

        test('should create only primary failover record', () => {
            template.hasResourceProperties('AWS::Route53::RecordSet', {
                Failover: 'PRIMARY'
            });
        });

        test('should create only one CloudWatch alarm', () => {
            template.resourceCountIs('AWS::CloudWatch::Alarm', 1);
        });

        test('should create outputs for primary region only', () => {
            const outputs = template.findOutputs('*');
            expect(outputs).toHaveProperty('PrimaryHealthCheckId');
            expect(outputs).not.toHaveProperty('SecondaryHealthCheckId');
        });
    });
});