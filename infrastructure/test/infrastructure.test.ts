import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { GenAIDemoInfrastructureStack } from '../lib/infrastructure-stack';

describe('GenAI Demo Infrastructure Stack', () => {
    let app: cdk.App;
    let stack: GenAIDemoInfrastructureStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App({
            context: {
                'genai-demo:networking': {
                    'availability-zones': 3,
                    'enable-vpc-flow-logs': true,
                    'enable-dns-hostnames': true,
                    'enable-dns-support': true
                },
                'genai-demo:environments': {
                    'test': {
                        'vpc-cidr': '10.0.0.0/16',
                        'nat-gateways': 1
                    }
                }
            }
        });
        stack = new GenAIDemoInfrastructureStack(app, 'TestStack', {
            environment: 'test',
            projectName: 'genai-demo-test',
            env: {
                account: '123456789012',
                region: 'ap-east-2'
            }
        });
        template = Template.fromStack(stack);
    });

    test('VPC is created with correct configuration', () => {
        template.hasResourceProperties('AWS::EC2::VPC', {
            CidrBlock: '10.0.0.0/16',
            EnableDnsHostnames: true,
            EnableDnsSupport: true
        });
    });

    test('Public subnets are created', () => {
        template.resourceCountIs('AWS::EC2::Subnet', 9); // 3 public + 3 private + 3 database

        template.hasResourceProperties('AWS::EC2::Subnet', {
            MapPublicIpOnLaunch: true
        });
    });

    test('Private subnets are created', () => {
        template.hasResourceProperties('AWS::EC2::Subnet', {
            MapPublicIpOnLaunch: false
        });
    });

    test('Internet Gateway is created', () => {
        template.hasResourceProperties('AWS::EC2::InternetGateway', {});
    });

    test('NAT Gateway is created for development environment', () => {
        template.resourceCountIs('AWS::EC2::NatGateway', 1);
    });

    test('Stack outputs are created', () => {
        template.hasOutput('VpcId', {});
        template.hasOutput('Environment', {
            Value: 'test'
        });
        template.hasOutput('ProjectName', {
            Value: 'genai-demo-test'
        });
    });

    test('Common tags are applied', () => {
        template.hasResourceProperties('AWS::EC2::VPC', {
            Tags: [
                { Key: 'Component', Value: 'Infrastructure' },
                { Key: 'Environment', Value: 'test' },
                { Key: 'ManagedBy', Value: 'AWS-CDK' },
                { Key: 'Name', Value: 'genai-demo-test-test-vpc' },
                { Key: 'Project', Value: 'genai-demo-test' }
            ]
        });
    });

    test('Security groups are created for all services', () => {
        // ALB Security Group
        template.hasResourceProperties('AWS::EC2::SecurityGroup', {
            GroupDescription: 'Security group for Application Load Balancer'
        });

        // EKS Security Group
        template.hasResourceProperties('AWS::EC2::SecurityGroup', {
            GroupDescription: 'Security group for EKS cluster and worker nodes'
        });

        // RDS Security Group
        template.hasResourceProperties('AWS::EC2::SecurityGroup', {
            GroupDescription: 'Security group for RDS PostgreSQL database'
        });

        // MSK Security Group
        template.hasResourceProperties('AWS::EC2::SecurityGroup', {
            GroupDescription: 'Security group for Amazon MSK cluster'
        });
    });

    test('Security group rules are properly configured', () => {
        // ALB Security Group has inline ingress rules for HTTP and HTTPS
        template.hasResourceProperties('AWS::EC2::SecurityGroup', {
            GroupDescription: 'Security group for Application Load Balancer',
            SecurityGroupIngress: [
                {
                    CidrIp: '0.0.0.0/0',
                    Description: 'Allow HTTP traffic from internet',
                    FromPort: 80,
                    IpProtocol: 'tcp',
                    ToPort: 80
                },
                {
                    CidrIp: '0.0.0.0/0',
                    Description: 'Allow HTTPS traffic from internet',
                    FromPort: 443,
                    IpProtocol: 'tcp',
                    ToPort: 443
                },
                {
                    CidrIp: '0.0.0.0/0',
                    Description: 'Allow health check traffic',
                    FromPort: 8080,
                    IpProtocol: 'tcp',
                    ToPort: 8080
                }
            ]
        });

        // Check that separate SecurityGroupIngress resources are created for cross-SG references
        template.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
            IpProtocol: 'tcp',
            FromPort: 8080,
            ToPort: 8080,
            Description: 'Allow Spring Boot application traffic from ALB'
        });

        // RDS allows PostgreSQL from EKS
        template.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
            IpProtocol: 'tcp',
            FromPort: 5432,
            ToPort: 5432,
            Description: 'Allow PostgreSQL traffic from EKS'
        });

        // MSK allows Kafka traffic from EKS
        template.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
            IpProtocol: 'tcp',
            FromPort: 9092,
            ToPort: 9092,
            Description: 'Allow Kafka plaintext traffic from EKS'
        });
    });

    test('VPC Flow Logs are configured when enabled', () => {
        // Check if CloudWatch Log Group is created for VPC Flow Logs
        template.hasResourceProperties('AWS::Logs::LogGroup', {
            LogGroupName: '/aws/vpc/flowlogs/genai-demo-test-test'
        });
    });
});

describe('GenAI Demo Infrastructure Stack with Domain', () => {
    let app: cdk.App;
    let stack: GenAIDemoInfrastructureStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App({
            context: {
                'genai-demo:networking': {
                    'availability-zones': 3,
                    'enable-vpc-flow-logs': true,
                    'enable-dns-hostnames': true,
                    'enable-dns-support': true
                },
                'genai-demo:environments': {
                    'test': {
                        'vpc-cidr': '10.0.0.0/16',
                        'nat-gateways': 1
                    }
                },
                'hosted-zone:account=123456789012:domainName=kimkao.io:region=ap-east-2': {
                    'Id': '/hostedzone/Z2KTO3AQUJG1DT',
                    'Name': 'kimkao.io.'
                }
            }
        });
        stack = new GenAIDemoInfrastructureStack(app, 'TestStackWithDomain', {
            environment: 'test',
            projectName: 'genai-demo-test',
            domain: 'test.kimkao.io',
            env: {
                account: '123456789012',
                region: 'ap-east-2'
            }
        });
        template = Template.fromStack(stack);
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

    test('ACM certificates are created with DNS validation', () => {
        // Main certificate
        template.hasResourceProperties('AWS::CertificateManager::Certificate', {
            DomainName: 'test.kimkao.io',
            ValidationMethod: 'DNS',
            SubjectAlternativeNames: [
                'api.test.kimkao.io',
                'cmc.test.kimkao.io',
                'shop.test.kimkao.io',
                'grafana.test.kimkao.io',
                'logs.test.kimkao.io'
            ]
        });

        // Wildcard certificate
        template.hasResourceProperties('AWS::CertificateManager::Certificate', {
            DomainName: '*.kimkao.io',
            ValidationMethod: 'DNS',
            SubjectAlternativeNames: ['kimkao.io']
        });
    });

    test('Certificate validation monitoring is configured', () => {
        // SNS topic for certificate alerts
        template.hasResourceProperties('AWS::SNS::Topic', {
            TopicName: 'genai-demo-test-test-certificate-alerts',
            DisplayName: 'Certificate Alerts for genai-demo-test test'
        });

        // CloudWatch alarms for certificate expiration
        template.hasResourceProperties('AWS::CloudWatch::Alarm', {
            AlarmName: 'genai-demo-test-test-certificate-validation-status',
            AlarmDescription: 'Monitor certificate validation status',
            MetricName: 'DaysToExpiry',
            Namespace: 'AWS/CertificateManager',
            Threshold: 30,
            ComparisonOperator: 'LessThanThreshold'
        });

        template.hasResourceProperties('AWS::CloudWatch::Alarm', {
            AlarmName: 'genai-demo-test-test-wildcard-certificate-validation-status',
            AlarmDescription: 'Monitor wildcard certificate validation status',
            MetricName: 'DaysToExpiry',
            Namespace: 'AWS/CertificateManager',
            Threshold: 30,
            ComparisonOperator: 'LessThanThreshold'
        });
    });

    test('Certificate ARNs are exported for Kubernetes Ingress use', () => {
        template.hasOutput('CertificateArn', {
            Description: 'ACM Certificate ARN for domain - use in Kubernetes Ingress annotations',
            Export: {
                Name: 'genai-demo-test-test-certificate-arn'
            }
        });

        template.hasOutput('WildcardCertificateArn', {
            Description: 'ACM Wildcard Certificate ARN for domain - use in Kubernetes Ingress annotations',
            Export: {
                Name: 'genai-demo-test-test-wildcard-certificate-arn'
            }
        });
    });

    test('Hosted Zone ID is exported for cross-stack references', () => {
        template.hasOutput('HostedZoneId', {
            Description: 'Route 53 Hosted Zone ID for cross-stack DNS management',
            Export: {
                Name: 'genai-demo-test-test-hosted-zone-id'
            }
        });

        template.hasOutput('HostedZoneName', {
            Description: 'Route 53 Hosted Zone Name for cross-stack DNS management',
            Export: {
                Name: 'genai-demo-test-test-hosted-zone-name'
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

    test('Certificate validation status monitoring outputs are created', () => {
        template.hasOutput('CertMonitoringAlertsTopicArnOutput', {
            Description: 'SNS topic ARN for certificate alerts',
            Export: {
                Name: 'genai-demo-test-test-certificate-alerts-topic'
            }
        });

        template.hasOutput('CertMonitoringValidationAlarmOutput', {
            Description: 'CloudWatch alarm name for certificate validation monitoring',
            Export: {
                Name: 'genai-demo-test-test-certificate-validation-alarm'
            }
        });

        template.hasOutput('WildcardCertMonitoringValidationAlarmOutput', {
            Description: 'CloudWatch alarm name for wildcard certificate validation monitoring',
            Export: {
                Name: 'genai-demo-test-test-wildcard-certificate-validation-alarm'
            }
        });
    });
});
