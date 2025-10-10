/**
 * Unit tests for NetworkStack
 * Tests the basic VPC and security group functionality
 */

import * as cdk from 'aws-cdk-lib';
import { Match, Template } from 'aws-cdk-lib/assertions';
import { NetworkStack } from '../../src/stacks/network-stack';

describe('NetworkStack Unit Tests', () => {
    let app: cdk.App;
    let stack: NetworkStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App();
        stack = new NetworkStack(app, 'TestNetworkStack', {
            env: { region: 'us-east-1', account: '123456789012' },
            environment: 'test',
            projectName: 'test-project',
        });
        template = Template.fromStack(stack);
    });

    describe('VPC Configuration', () => {
        test('should create VPC with correct CIDR and configuration', () => {
            template.hasResourceProperties('AWS::EC2::VPC', {
                CidrBlock: '10.4.0.0/16', // us-east-1 specific CIDR
                EnableDnsHostnames: true,
                EnableDnsSupport: true,
            });
        });

        test('should create subnets', () => {
            // Should create public and private subnets
            template.hasResourceProperties('AWS::EC2::Subnet', {
                Tags: Match.arrayWith([
                    { Key: 'aws-cdk:subnet-type', Value: 'Public' }
                ])
            });

            template.hasResourceProperties('AWS::EC2::Subnet', {
                Tags: Match.arrayWith([
                    { Key: 'aws-cdk:subnet-type', Value: 'Private' }
                ])
            });
        });
    });

    describe('Security Groups', () => {
        test('should create ALB security group', () => {
            template.hasResourceProperties('AWS::EC2::SecurityGroup', {
                GroupDescription: 'Security group for Application Load Balancer',
                SecurityGroupIngress: Match.arrayWith([
                    Match.objectLike({
                        FromPort: 80,
                        ToPort: 80,
                        IpProtocol: 'tcp',
                        CidrIp: '0.0.0.0/0'
                    })
                ])
            });
        });

        test('should create App security group', () => {
            template.hasResourceProperties('AWS::EC2::SecurityGroup', {
                GroupDescription: 'Security group for application instances'
            });
        });

        test('should create Database security group', () => {
            template.hasResourceProperties('AWS::EC2::SecurityGroup', {
                GroupDescription: 'Security group for database instances'
            });
        });
    });

    describe('NAT Gateways', () => {
        test('should create NAT gateway', () => {
            template.resourceCountIs('AWS::EC2::NatGateway', 1);
        });

        test('should create Elastic IP for NAT gateway', () => {
            template.resourceCountIs('AWS::EC2::EIP', 1);
        });
    });

    describe('Internet Gateway', () => {
        test('should create Internet Gateway', () => {
            template.hasResource('AWS::EC2::InternetGateway', {});
        });

        test('should attach Internet Gateway to VPC', () => {
            template.hasResource('AWS::EC2::VPCGatewayAttachment', {
                Properties: {
                    InternetGatewayId: Match.anyValue(),
                    VpcId: Match.anyValue()
                }
            });
        });
    });

    describe('Route Tables', () => {
        test('should create route tables', () => {
            // Should have route tables for public and private subnets
            template.hasResource('AWS::EC2::RouteTable', {});
        });

        test('should create routes', () => {
            // Should have routes to internet gateway and NAT gateway
            template.hasResource('AWS::EC2::Route', {});
        });
    });

    describe('Stack Outputs', () => {
        test('should export VPC ID', () => {
            template.hasOutput('VpcId', {
                Export: {
                    Name: 'test-project-test-VpcId'
                }
            });
        });

        test('should export ALB Security Group ID', () => {
            template.hasOutput('ALBSecurityGroupId', {
                Export: {
                    Name: 'test-project-test-ALBSecurityGroupId'
                }
            });
        });
    });
});