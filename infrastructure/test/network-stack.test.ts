import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { NetworkStack } from '../src/stacks/network-stack';

describe('Network Stack', () => {
    let app: cdk.App;
    let stack: NetworkStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App();
        stack = new NetworkStack(app, 'TestNetworkStack', {});
        template = Template.fromStack(stack);
    });

    test('VPC is created with correct configuration', () => {
        template.hasResourceProperties('AWS::EC2::VPC', {
            CidrBlock: '10.0.0.0/16',
            EnableDnsHostnames: true,
            EnableDnsSupport: true
        });
    });

    test('Subnets are created correctly', () => {
        // Should have 6 subnets: 2 public + 2 private + 2 database (maxAzs: 2)
        template.resourceCountIs('AWS::EC2::Subnet', 6);

        // Public subnets
        template.hasResourceProperties('AWS::EC2::Subnet', {
            MapPublicIpOnLaunch: true
        });

        // Private subnets
        template.hasResourceProperties('AWS::EC2::Subnet', {
            MapPublicIpOnLaunch: false
        });
    });

    test('Internet Gateway is created', () => {
        template.resourceCountIs('AWS::EC2::InternetGateway', 1);
    });

    test('NAT Gateway is created', () => {
        template.resourceCountIs('AWS::EC2::NatGateway', 1);
    });

    test('Security groups are created', () => {
        // Should have 3 security groups: ALB, App, Database
        template.resourceCountIs('AWS::EC2::SecurityGroup', 3);

        // ALB Security Group
        template.hasResourceProperties('AWS::EC2::SecurityGroup', {
            GroupDescription: 'Security group for Application Load Balancer'
        });

        // App Security Group
        template.hasResourceProperties('AWS::EC2::SecurityGroup', {
            GroupDescription: 'Security group for application instances'
        });

        // Database Security Group
        template.hasResourceProperties('AWS::EC2::SecurityGroup', {
            GroupDescription: 'Security group for database instances'
        });
    });

    test('Security group rules are properly configured', () => {
        // ALB Security Group allows HTTP and HTTPS
        template.hasResourceProperties('AWS::EC2::SecurityGroup', {
            GroupDescription: 'Security group for Application Load Balancer',
            SecurityGroupIngress: [
                {
                    CidrIp: '0.0.0.0/0',
                    Description: 'Allow HTTPS traffic',
                    FromPort: 443,
                    IpProtocol: 'tcp',
                    ToPort: 443
                },
                {
                    CidrIp: '0.0.0.0/0',
                    Description: 'Allow HTTP traffic',
                    FromPort: 80,
                    IpProtocol: 'tcp',
                    ToPort: 80
                }
            ]
        });

        // Check SecurityGroupIngress resources for cross-SG references
        template.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
            IpProtocol: 'tcp',
            FromPort: 8080,
            ToPort: 8080,
            Description: 'Allow traffic from ALB'
        });

        template.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
            IpProtocol: 'tcp',
            FromPort: 5432,
            ToPort: 5432,
            Description: 'Allow PostgreSQL traffic from app'
        });
    });

    test('Network stack outputs are created', () => {
        template.hasOutput('VpcId', {
            Value: {
                Ref: 'VPCB9E5F0B4'
            },
            Export: {
                Name: 'TestNetworkStack-VpcId'
            }
        });

        template.hasOutput('ALBSecurityGroupId', {
            Value: {
                'Fn::GetAtt': ['ALBSecurityGroup29A3BDEF', 'GroupId']
            },
            Export: {
                Name: 'TestNetworkStack-ALBSecurityGroupId'
            }
        });
    });

    test('Route tables are configured correctly', () => {
        // Should have route tables for public and private subnets (CDK creates additional ones)
        template.resourceCountIs('AWS::EC2::RouteTable', 6);
    });

    test('Routes are configured correctly', () => {
        // Public subnets should have route to Internet Gateway
        template.hasResourceProperties('AWS::EC2::Route', {
            DestinationCidrBlock: '0.0.0.0/0',
            GatewayId: {
                Ref: 'VPCIGWB7E252D3'
            }
        });

        // Private subnets should have route to NAT Gateway
        template.hasResourceProperties('AWS::EC2::Route', {
            DestinationCidrBlock: '0.0.0.0/0',
            NatGatewayId: {
                Ref: 'VPCPublicSubnet1NATGatewayE0556630'
            }
        });
    });
});