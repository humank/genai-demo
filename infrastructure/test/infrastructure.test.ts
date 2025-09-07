import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { GenAIDemoInfrastructureStack } from '../lib/infrastructure-stack';

describe('GenAI Demo Infrastructure Stack', () => {
    let app: cdk.App;
    let stack: GenAIDemoInfrastructureStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App();
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
});
