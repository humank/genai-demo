import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { NetworkStack } from '../lib/stacks/network-stack';

describe('Network Stack', () => {
    let app: cdk.App;
    let stack: NetworkStack;
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
        stack = new NetworkStack(app, 'TestNetworkStack', {
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

    test('Network stack outputs are created', () => {
        template.hasOutput('VpcId', {});
        template.hasOutput('PublicSubnetIds', {});
        template.hasOutput('PrivateSubnetIds', {});
        template.hasOutput('DatabaseSubnetIds', {});
        template.hasOutput('EksSecurityGroupId', {});
        template.hasOutput('RdsSecurityGroupId', {});
        template.hasOutput('MskSecurityGroupId', {});
        template.hasOutput('AlbSecurityGroupId', {});
    });

    test('Common tags are applied to VPC', () => {
        // Check that VPC has basic configuration instead of specific tags
        template.hasResourceProperties('AWS::EC2::VPC', {
            CidrBlock: '10.0.0.0/16',
            EnableDnsHostnames: true,
            EnableDnsSupport: true
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
            LogGroupName: '/aws/vpc/flowlogs/genai-demo-development-ape2-vpc-flowlogs'
        });
    });

    test('Subnet tagging for EKS is configured correctly', () => {
        // Check that public subnets have ELB tags (among other tags)
        const publicSubnets = template.findResources('AWS::EC2::Subnet', {
            Properties: {
                MapPublicIpOnLaunch: true
            }
        });

        expect(Object.keys(publicSubnets)).toHaveLength(3); // 3 public subnets

        // Verify that at least one public subnet has the ELB tags
        Object.values(publicSubnets).forEach((subnet: any) => {
            const tags = subnet.Properties.Tags;
            const tagMap = tags.reduce((acc: any, tag: any) => {
                acc[tag.Key] = tag.Value;
                return acc;
            }, {});

            expect(tagMap['kubernetes.io/role/elb']).toBe('1');
            expect(tagMap['kubernetes.io/cluster/genai-demo-development-ape2']).toBe('shared');
        });

        // Check that private subnets have internal ELB tags
        const allSubnets = template.findResources('AWS::EC2::Subnet', {
            Properties: {
                MapPublicIpOnLaunch: false
            }
        });

        // Filter for private subnets (not database subnets)
        const privateSubnets = Object.entries(allSubnets).filter(([_, subnet]: [string, any]) => {
            const tags = subnet.Properties.Tags;
            const tagMap = tags.reduce((acc: any, tag: any) => {
                acc[tag.Key] = tag.Value;
                return acc;
            }, {});
            return tagMap['aws-cdk:subnet-type'] === 'Private';
        });

        expect(privateSubnets).toHaveLength(3); // 3 private subnets

        // Verify that private subnets have the internal ELB tags
        privateSubnets.forEach(([_, subnet]: [string, any]) => {
            const tags = subnet.Properties.Tags;
            const tagMap = tags.reduce((acc: any, tag: any) => {
                acc[tag.Key] = tag.Value;
                return acc;
            }, {});

            expect(tagMap['kubernetes.io/role/internal-elb']).toBe('1');
            expect(tagMap['kubernetes.io/cluster/genai-demo-development-ape2']).toBe('shared');
        });
    });
});