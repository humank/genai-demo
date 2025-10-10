import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { NetworkStack } from '../../src/stacks/network-stack';

describe('NetworkStack', () => {
    let app: cdk.App;
    let stack: NetworkStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App();
        stack = new NetworkStack(app, 'TestNetworkStack', {
            env: { region: 'us-east-1', account: '123456789012' },
            environment: 'test',
            projectName: 'genai-demo'
        });
        template = Template.fromStack(stack);
    });

    test('should create VPC with correct configuration', () => {
        template.hasResourceProperties('AWS::EC2::VPC', {
            CidrBlock: '10.4.0.0/16', // us-east-1 specific CIDR
            EnableDnsHostnames: true,
            EnableDnsSupport: true,
        });
    });

    test('should create security groups', () => {
        template.resourceCountIs('AWS::EC2::SecurityGroup', 7);

        // Check ALB security group
        template.hasResourceProperties('AWS::EC2::SecurityGroup', {
            GroupDescription: 'Security group for Application Load Balancer',
        });

        // Check App security group
        template.hasResourceProperties('AWS::EC2::SecurityGroup', {
            GroupDescription: 'Security group for application instances',
        });

        // Check Database security group
        template.hasResourceProperties('AWS::EC2::SecurityGroup', {
            GroupDescription: 'Security group for database instances',
        });
    });

    test('should create subnets in multiple AZs', () => {
        template.resourceCountIs('AWS::EC2::Subnet', 12); // 4 AZs * 3 subnet types
    });

    test('should create NAT Gateway', () => {
        template.resourceCountIs('AWS::EC2::NatGateway', 1);
    });

    test('should create Internet Gateway', () => {
        template.resourceCountIs('AWS::EC2::InternetGateway', 1);
    });

    test('should export VPC ID', () => {
        template.hasOutput('VpcId', {});
    });

    test('should export ALB Security Group ID', () => {
        template.hasOutput('ALBSecurityGroupId', {});
    });
});