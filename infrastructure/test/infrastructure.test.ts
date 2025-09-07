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
