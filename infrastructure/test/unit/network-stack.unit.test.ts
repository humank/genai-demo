/**
 * Enhanced unit tests for NetworkStack
 * Tests individual components and their configurations in isolation
 */

import * as cdk from 'aws-cdk-lib';
import { Match, Template } from 'aws-cdk-lib/assertions';
import { NetworkStack } from '../../lib/stacks/network-stack';
import { TestDataFactory, TestUtils } from '../setup';

describe('NetworkStack Unit Tests', () => {
    let app: cdk.App;
    let stack: NetworkStack;
    let template: Template;

    beforeEach(() => {
        app = TestDataFactory.createTestApp();
        stack = new NetworkStack(app, 'TestNetworkStack', TestDataFactory.createTestStackProps());
        template = Template.fromStack(stack);
    });

    describe('VPC Configuration', () => {
        test('should create VPC with correct CIDR and configuration', () => {
            template.hasResourceProperties('AWS::EC2::VPC', {
                CidrBlock: '10.0.0.0/16',
                EnableDnsHostnames: true,
                EnableDnsSupport: true,
                Tags: Match.arrayWith([
                    { Key: 'Name', Value: Match.stringLikeRegexp('genai-demo-test.*VPC') }
                ])
            });
        });

        test('should enable VPC Flow Logs when configured', () => {
            template.hasResourceProperties('AWS::EC2::FlowLog', {
                ResourceType: 'VPC',
                TrafficType: 'ALL',
                LogDestinationType: 'cloud-watch-logs'
            });
        });

        test('should create correct number of subnets across AZs', () => {
            // Should create 3 public and 3 private subnets (3 AZs)
            const publicSubnets = TestUtils.getResourcesByType(template, 'AWS::EC2::Subnet')
                .filter(subnet => subnet.Properties.Tags?.some((tag: any) =>
                    tag.Key === 'aws-cdk:subnet-type' && tag.Value === 'Public'
                ));

            const privateSubnets = TestUtils.getResourcesByType(template, 'AWS::EC2::Subnet')
                .filter(subnet => subnet.Properties.Tags?.some((tag: any) =>
                    tag.Key === 'aws-cdk:subnet-type' && tag.Value === 'Private'
                ));

            expect(publicSubnets).toHaveLength(3);
            expect(privateSubnets).toHaveLength(3);
        });
    });

    describe('Security Groups', () => {
        test('should create EKS security group with proper rules', () => {
            template.hasResourceProperties('AWS::EC2::SecurityGroup', {
                GroupDescription: Match.stringLikeRegexp('.*EKS.*'),
                SecurityGroupIngress: Match.arrayWith([
                    {
                        IpProtocol: 'tcp',
                        FromPort: 443,
                        ToPort: 443,
                        CidrIp: '10.0.0.0/16'
                    }
                ])
            });
        });

        test('should create RDS security group with database port access', () => {
            template.hasResourceProperties('AWS::EC2::SecurityGroup', {
                GroupDescription: Match.stringLikeRegexp('.*RDS.*'),
                SecurityGroupIngress: Match.arrayWith([
                    {
                        IpProtocol: 'tcp',
                        FromPort: 5432,
                        ToPort: 5432
                    }
                ])
            });
        });

        test('should create MSK security group with Kafka ports', () => {
            template.hasResourceProperties('AWS::EC2::SecurityGroup', {
                GroupDescription: Match.stringLikeRegexp('.*MSK.*'),
                SecurityGroupIngress: Match.arrayWith([
                    {
                        IpProtocol: 'tcp',
                        FromPort: 9092,
                        ToPort: 9092
                    },
                    {
                        IpProtocol: 'tcp',
                        FromPort: 9094,
                        ToPort: 9094
                    }
                ])
            });
        });

        test('should create ALB security group with HTTP/HTTPS access', () => {
            template.hasResourceProperties('AWS::EC2::SecurityGroup', {
                GroupDescription: Match.stringLikeRegexp('.*ALB.*'),
                SecurityGroupIngress: Match.arrayWith([
                    {
                        IpProtocol: 'tcp',
                        FromPort: 80,
                        ToPort: 80,
                        CidrIp: '0.0.0.0/0'
                    },
                    {
                        IpProtocol: 'tcp',
                        FromPort: 443,
                        ToPort: 443,
                        CidrIp: '0.0.0.0/0'
                    }
                ])
            });
        });
    });

    describe('NAT Gateways', () => {
        test('should create correct number of NAT gateways based on environment', () => {
            // Test environment should have 1 NAT gateway
            expect(template).toHaveResourceCount('AWS::EC2::NatGateway', 1);
        });

        test('should create Elastic IPs for NAT gateways', () => {
            expect(template).toHaveResourceCount('AWS::EC2::EIP', 1);
        });
    });

    describe('Internet Gateway', () => {
        test('should create and attach Internet Gateway', () => {
            template.hasResource('AWS::EC2::InternetGateway', {});
            template.hasResource('AWS::EC2::VPCGatewayAttachment', {
                Properties: {
                    GatewayType: 'InternetGateway'
                }
            });
        });
    });

    describe('Route Tables', () => {
        test('should create public route tables with internet gateway routes', () => {
            template.hasResourceProperties('AWS::EC2::Route', {
                DestinationCidrBlock: '0.0.0.0/0',
                GatewayId: Match.anyValue()
            });
        });

        test('should create private route tables with NAT gateway routes', () => {
            template.hasResourceProperties('AWS::EC2::Route', {
                DestinationCidrBlock: '0.0.0.0/0',
                NatGatewayId: Match.anyValue()
            });
        });
    });

    describe('Resource Tagging', () => {
        test('should tag all resources with standard tags', () => {
            const vpcs = TestUtils.getResourcesByType(template, 'AWS::EC2::VPC');
            expect(vpcs).toHaveLength(1);

            const vpc = vpcs[0];
            expect(TestUtils.hasTag(vpc, 'Project', 'genai-demo-test')).toBe(true);
            expect(TestUtils.hasTag(vpc, 'Environment', 'test')).toBe(true);
            expect(TestUtils.hasTag(vpc, 'ManagedBy', 'AWS-CDK')).toBe(true);
        });

        test('should tag security groups with component-specific tags', () => {
            const securityGroups = TestUtils.getResourcesByType(template, 'AWS::EC2::SecurityGroup');

            securityGroups.forEach(sg => {
                expect(TestUtils.hasTag(sg, 'Project')).toBe(true);
                expect(TestUtils.hasTag(sg, 'Environment')).toBe(true);
                expect(TestUtils.hasTag(sg, 'Component')).toBe(true);
            });
        });
    });

    describe('Stack Outputs', () => {
        test('should export VPC ID and CIDR', () => {
            template.hasOutput('VpcId', {
                Export: {
                    Name: 'genai-demo-test-test-vpc-id'
                }
            });

            template.hasOutput('VpcCidr', {
                Export: {
                    Name: 'genai-demo-test-test-vpc-cidr'
                }
            });
        });

        test('should export security group IDs', () => {
            template.hasOutput('EksSecurityGroupId', {
                Export: {
                    Name: 'genai-demo-test-test-eks-security-group-id'
                }
            });

            template.hasOutput('RdsSecurityGroupId', {
                Export: {
                    Name: 'genai-demo-test-test-rds-security-group-id'
                }
            });

            template.hasOutput('MskSecurityGroupId', {
                Export: {
                    Name: 'genai-demo-test-test-msk-security-group-id'
                }
            });

            template.hasOutput('AlbSecurityGroupId', {
                Export: {
                    Name: 'genai-demo-test-test-alb-security-group-id'
                }
            });
        });

        test('should export subnet IDs', () => {
            template.hasOutput('PublicSubnetIds', {
                Export: {
                    Name: 'genai-demo-test-test-public-subnet-ids'
                }
            });

            template.hasOutput('PrivateSubnetIds', {
                Export: {
                    Name: 'genai-demo-test-test-private-subnet-ids'
                }
            });
        });
    });

    describe('Environment-Specific Configuration', () => {
        test('should use production configuration for production environment', () => {
            const prodStack = new NetworkStack(app, 'TestNetworkStackProd', {
                ...TestDataFactory.createTestStackProps(),
                environment: 'production'
            });
            const prodTemplate = Template.fromStack(prodStack);

            // Production should have 3 NAT gateways
            expect(prodTemplate).toHaveResourceCount('AWS::EC2::NatGateway', 3);
            expect(prodTemplate).toHaveResourceCount('AWS::EC2::EIP', 3);
        });

        test('should use development configuration for development environment', () => {
            const devStack = new NetworkStack(app, 'TestNetworkStackDev', {
                ...TestDataFactory.createTestStackProps(),
                environment: 'development'
            });
            const devTemplate = Template.fromStack(devStack);

            // Development should have 1 NAT gateway
            expect(devTemplate).toHaveResourceCount('AWS::EC2::NatGateway', 1);
            expect(devTemplate).toHaveResourceCount('AWS::EC2::EIP', 1);
        });
    });

    describe('Network ACLs', () => {
        test('should create network ACLs for additional security', () => {
            template.hasResource('AWS::EC2::NetworkAcl', {});
        });

        test('should have proper network ACL rules', () => {
            template.hasResourceProperties('AWS::EC2::NetworkAclEntry', {
                Protocol: -1,
                RuleAction: 'allow'
            });
        });
    });

    describe('VPC Endpoints', () => {
        test('should create VPC endpoints for AWS services', () => {
            // S3 Gateway endpoint
            template.hasResourceProperties('AWS::EC2::VPCEndpoint', {
                ServiceName: Match.stringLikeRegexp('.*s3'),
                VpcEndpointType: 'Gateway'
            });

            // DynamoDB Gateway endpoint
            template.hasResourceProperties('AWS::EC2::VPCEndpoint', {
                ServiceName: Match.stringLikeRegexp('.*dynamodb'),
                VpcEndpointType: 'Gateway'
            });
        });
    });
});