import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import * as certificatemanager from 'aws-cdk-lib/aws-certificatemanager';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as route53 from 'aws-cdk-lib/aws-route53';
import { CoreInfrastructureStack } from '../lib/stacks/core-infrastructure-stack';

describe('Core Infrastructure Stack', () => {
    describe('Without Domain', () => {
        let app: cdk.App;
        let stack: CoreInfrastructureStack;
        let template: Template;
        let vpc: ec2.Vpc;
        let albSecurityGroup: ec2.SecurityGroup;

        beforeEach(() => {
            app = new cdk.App();

            // Create VPC and security group within the core infrastructure stack to avoid cross-stack references
            const testStack = new cdk.Stack(app, 'TestStack', {
                env: {
                    account: '123456789012',
                    region: 'ap-east-2'
                }
            });

            vpc = new ec2.Vpc(testStack, 'TestVpc');
            albSecurityGroup = new ec2.SecurityGroup(testStack, 'TestAlbSg', { vpc });
            const eksSecurityGroup = new ec2.SecurityGroup(testStack, 'TestEksSg', { vpc });

            stack = new CoreInfrastructureStack(testStack, 'CoreInfrastructureStack', {
                environment: 'development',
                projectName: 'genai-demo',
                vpc,
                albSecurityGroup,
                eksSecurityGroup,
                env: {
                    account: '123456789012',
                    region: 'ap-east-2'
                }
            });
            template = Template.fromStack(testStack);
        });

        test('Application Load Balancer is created', () => {
            template.hasResourceProperties('AWS::ElasticLoadBalancingV2::LoadBalancer', {
                Name: 'genai-demo-test-test-alb',
                Scheme: 'internet-facing',
                Type: 'application'
            });
        });

        test('Default target group is created with health checks', () => {
            template.hasResourceProperties('AWS::ElasticLoadBalancingV2::TargetGroup', {
                Name: 'genai-demo-test-test-tg',
                Port: 8080,
                Protocol: 'HTTP',
                TargetType: 'ip',
                HealthCheckPath: '/actuator/health',
                HealthCheckProtocol: 'HTTP',
                HealthCheckPort: '8080'
            });
        });

        test('HTTP listener is created without domain', () => {
            template.hasResourceProperties('AWS::ElasticLoadBalancingV2::Listener', {
                Port: 80,
                Protocol: 'HTTP'
            });
        });

        test('Load balancer outputs are created', () => {
            template.hasOutput('LoadBalancerArn', {
                Description: 'Application Load Balancer ARN',
                Export: {
                    Name: 'genai-demo-test-test-alb-arn'
                }
            });

            template.hasOutput('LoadBalancerDnsName', {
                Description: 'Application Load Balancer DNS name',
                Export: {
                    Name: 'genai-demo-test-test-alb-dns-name'
                }
            });

            template.hasOutput('LoadBalancerHostedZoneId', {
                Description: 'Application Load Balancer hosted zone ID',
                Export: {
                    Name: 'genai-demo-test-test-alb-hosted-zone-id'
                }
            });
        });

        test('Environment and project outputs are created', () => {
            template.hasOutput('Environment', {
                Value: 'development',
                Description: 'Deployment environment'
            });

            template.hasOutput('ProjectName', {
                Value: 'genai-demo',
                Description: 'Project name'
            });
        });
    });

    describe('With Domain and Certificates', () => {
        let app: cdk.App;
        let stack: CoreInfrastructureStack;
        let template: Template;
        let vpc: ec2.Vpc;
        let albSecurityGroup: ec2.SecurityGroup;
        let certificate: certificatemanager.Certificate;
        let wildcardCertificate: certificatemanager.Certificate;
        let hostedZone: route53.IHostedZone;

        beforeEach(() => {
            app = new cdk.App({
                context: {
                    'hosted-zone:account=123456789012:domainName=kimkao.io:region=ap-east-2': {
                        'Id': '/hostedzone/Z2KTO3AQUJG1DT',
                        'Name': 'kimkao.io.'
                    }
                }
            });

            // Create a test VPC, security group, certificates, and hosted zone
            const testStack = new cdk.Stack(app, 'TestStack');
            vpc = new ec2.Vpc(testStack, 'TestVpc');
            albSecurityGroup = new ec2.SecurityGroup(testStack, 'TestAlbSg', { vpc });

            hostedZone = route53.HostedZone.fromLookup(testStack, 'TestHostedZone', {
                domainName: 'kimkao.io'
            });

            certificate = new certificatemanager.Certificate(testStack, 'TestCertificate', {
                domainName: 'test.kimkao.io',
                validation: certificatemanager.CertificateValidation.fromDns(hostedZone)
            });

            wildcardCertificate = new certificatemanager.Certificate(testStack, 'TestWildcardCertificate', {
                domainName: '*.kimkao.io',
                validation: certificatemanager.CertificateValidation.fromDns(hostedZone)
            });

            const eksSecurityGroup = new ec2.SecurityGroup(app, 'TestEksSg2', { vpc });

            stack = new CoreInfrastructureStack(app, 'TestCoreInfrastructureStackWithDomain', {
                environment: 'test',
                projectName: 'genai-demo-test',
                domain: 'test.kimkao.io',
                vpc,
                albSecurityGroup,
                eksSecurityGroup,
                hostedZone,
                certificate,
                wildcardCertificate,
                env: {
                    account: '123456789012',
                    region: 'ap-east-2'
                }
            });
            template = Template.fromStack(stack);
        });

        test('HTTPS listener is created with certificates', () => {
            template.hasResourceProperties('AWS::ElasticLoadBalancingV2::Listener', {
                Port: 443,
                Protocol: 'HTTPS',
                SslPolicy: 'ELBSecurityPolicy-TLS-1-2-Ext-2018-06'
            });
        });

        test('HTTP listener redirects to HTTPS', () => {
            template.hasResourceProperties('AWS::ElasticLoadBalancingV2::Listener', {
                Port: 80,
                Protocol: 'HTTP',
                DefaultActions: [
                    {
                        Type: 'redirect',
                        RedirectConfig: {
                            Protocol: 'HTTPS',
                            Port: '443',
                            StatusCode: 'HTTP_301'
                        }
                    }
                ]
            });
        });

        test('DNS A records are created pointing to ALB', () => {
            // Main domain A record
            template.hasResourceProperties('AWS::Route53::RecordSet', {
                Type: 'A',
                Name: 'test.kimkao.io.',
                Comment: 'Main A record for test.kimkao.io pointing to ALB'
            });

            // Wildcard A record for subdomains
            template.hasResourceProperties('AWS::Route53::RecordSet', {
                Type: 'A',
                Name: '*.test.kimkao.io.',
                Comment: 'Wildcard A record for *.test.kimkao.io pointing to ALB'
            });

            // Specific subdomain A records
            const subdomains = ['api', 'cmc', 'shop', 'grafana', 'logs'];
            subdomains.forEach(subdomain => {
                template.hasResourceProperties('AWS::Route53::RecordSet', {
                    Type: 'A',
                    Name: `${subdomain}.test.kimkao.io.`,
                    Comment: `A record for ${subdomain}.test.kimkao.io pointing to ALB`
                });
            });
        });

        test('DNS A record target output is created', () => {
            template.hasOutput('DnsARecordTarget', {
                Description: 'DNS A record target (ALB DNS name)',
                Export: {
                    Name: 'genai-demo-test-test-dns-a-record-target'
                }
            });
        });

        test('Kubernetes Ingress annotations are provided', () => {
            template.hasOutput('KubernetesIngressAnnotations', {
                Description: 'Kubernetes Ingress annotations for ALB integration with SSL',
                Export: {
                    Name: 'genai-demo-test-test-k8s-ingress-annotations'
                }
            });
        });

        test('Common tags are applied to load balancer', () => {
            template.hasResourceProperties('AWS::ElasticLoadBalancingV2::LoadBalancer', {
                Tags: [
                    { Key: 'Environment', Value: 'test' },
                    { Key: 'Project', Value: 'genai-demo-test' },
                    { Key: 'Service', Value: 'ALB' }
                ]
            });
        });
    });
});