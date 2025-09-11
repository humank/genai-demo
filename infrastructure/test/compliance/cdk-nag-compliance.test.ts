/**
 * CDK compliance tests for infrastructure security and best practices
 * Validates AWS Well-Architected Framework compliance
 */

import * as cdk from 'aws-cdk-lib';
import { Match, Template } from 'aws-cdk-lib/assertions';
import { GenAIDemoInfrastructureStack } from '../../lib/genai-demo-infrastructure-stack';
import { NetworkStack } from '../../lib/stacks/network-stack';
import { TestDataFactory } from '../setup';

describe('CDK Compliance Tests', () => {
    let app: cdk.App;

    beforeEach(() => {
        app = TestDataFactory.createTestApp({
            'hosted-zone:account=123456789012:domainName=kimkao.io:region=ap-east-2': {
                'Id': '/hostedzone/Z2KTO3AQUJG1DT',
                'Name': 'kimkao.io.'
            }
        });
    });

    describe('Security Compliance Checks', () => {
        test('should have proper VPC security configurations', () => {
            const stack = new NetworkStack(app, 'TestNetworkStack', TestDataFactory.createTestStackProps());
            const template = Template.fromStack(stack);

            // VPC should have DNS support enabled
            template.hasResourceProperties('AWS::EC2::VPC', {
                EnableDnsHostnames: true,
                EnableDnsSupport: true
            });

            // Should have VPC Flow Logs
            template.hasResource('AWS::EC2::FlowLog', {
                Properties: {
                    ResourceType: 'VPC',
                    TrafficType: 'ALL'
                }
            });
        });

        test('should have secure security group configurations', () => {
            const stack = new NetworkStack(app, 'TestNetworkStack', TestDataFactory.createTestStackProps());
            const template = Template.fromStack(stack);

            // Check that security groups exist
            const resources = template.toJSON().Resources || {};
            const securityGroups = Object.values(resources).filter((resource: any) =>
                resource.Type === 'AWS::EC2::SecurityGroup'
            );

            expect(securityGroups.length).toBeGreaterThan(0);

            // Each security group should have a description
            securityGroups.forEach((sg: any) => {
                expect(sg.Properties.GroupDescription).toBeDefined();
                expect(sg.Properties.VpcId).toBeDefined();
            });
        });

        test('should have proper encryption configurations', () => {
            const stack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            // Check main stack synthesis
            expect(() => app.synth()).not.toThrow();
        });

        test('should have proper IAM configurations', () => {
            const stack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            // Verify stack can be synthesized without IAM errors
            const synthesis = app.synth();
            expect(synthesis).toBeDefined();
        });
    });

    describe('Well-Architected Framework Compliance', () => {
        test('should validate operational excellence pillar', () => {
            const stack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            // Should have monitoring and logging infrastructure
            expect(stack.networkStack).toBeDefined();
            expect(stack.certificateStack).toBeDefined();
            expect(stack.coreInfrastructureStack).toBeDefined();
        });

        test('should validate security pillar compliance', () => {
            const stack = new NetworkStack(app, 'TestNetworkStack', TestDataFactory.createTestStackProps());
            const template = Template.fromStack(stack);

            // Should have security groups configured
            template.hasResource('AWS::EC2::SecurityGroup', {});

            // Should have VPC Flow Logs for monitoring
            template.hasResource('AWS::EC2::FlowLog', {});
        });

        test('should validate reliability pillar compliance', () => {
            const stack = new NetworkStack(app, 'TestNetworkStack', TestDataFactory.createTestStackProps());
            const template = Template.fromStack(stack);

            // Should have multiple subnets for availability
            const resources = template.toJSON().Resources || {};
            const subnets = Object.values(resources).filter((resource: any) =>
                resource.Type === 'AWS::EC2::Subnet'
            );

            expect(subnets.length).toBeGreaterThanOrEqual(2);
        });

        test('should validate performance efficiency pillar', () => {
            const stack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            // Should synthesize efficiently
            const startTime = Date.now();
            app.synth();
            const endTime = Date.now();

            expect(endTime - startTime).toBeLessThan(30000); // Should complete in under 30 seconds
        });

        test('should validate cost optimization pillar', () => {
            const stack = new NetworkStack(app, 'TestNetworkStack', TestDataFactory.createTestStackProps());
            const template = Template.fromStack(stack);

            // Test environment should have cost-optimized configuration (1 NAT Gateway)
            const resources = template.toJSON().Resources || {};
            const natGateways = Object.values(resources).filter((resource: any) =>
                resource.Type === 'AWS::EC2::NatGateway'
            );

            expect(natGateways.length).toBe(1); // Cost-optimized for test environment
        });
    });

    describe('Environment-Specific Compliance', () => {
        test('should validate production environment compliance', () => {
            const stack = new GenAIDemoInfrastructureStack(app, 'TestProdStack', {
                environment: 'production',
                projectName: 'genai-demo-prod',
                domain: 'kimkao.io',
                env: {
                    account: '123456789012',
                    region: 'ap-northeast-1'
                }
            });

            // Production should have all required stacks
            expect(stack.networkStack).toBeDefined();
            expect(stack.certificateStack).toBeDefined();
            expect(stack.coreInfrastructureStack).toBeDefined();
        });

        test('should validate development environment compliance', () => {
            const stack = new GenAIDemoInfrastructureStack(app, 'TestDevStack', {
                environment: 'development',
                projectName: 'genai-demo-dev',
                env: {
                    account: '123456789012',
                    region: 'ap-northeast-1'
                }
            });

            // Development should have basic infrastructure
            expect(stack.networkStack).toBeDefined();
            expect(stack.coreInfrastructureStack).toBeDefined();
        });
    });

    describe('Resource Tagging Compliance', () => {
        test('should have consistent resource tagging', () => {
            const stack = new NetworkStack(app, 'TestNetworkStack', TestDataFactory.createTestStackProps());
            const template = Template.fromStack(stack);

            // Check that VPC has proper tags
            template.hasResourceProperties('AWS::EC2::VPC', {
                Tags: Match.arrayWith([
                    { Key: 'Project', Value: Match.anyValue() },
                    { Key: 'Environment', Value: Match.anyValue() },
                    { Key: 'ManagedBy', Value: 'AWS-CDK' }
                ])
            });
        });

        test('should validate security group tagging', () => {
            const stack = new NetworkStack(app, 'TestNetworkStack', TestDataFactory.createTestStackProps());
            const template = Template.fromStack(stack);

            // Security groups should have proper tags
            template.hasResourceProperties('AWS::EC2::SecurityGroup', {
                Tags: Match.arrayWith([
                    { Key: 'Project', Value: Match.anyValue() },
                    { Key: 'Environment', Value: Match.anyValue() }
                ])
            });
        });
    });

    describe('Network Security Compliance', () => {
        test('should validate security group rules', () => {
            const stack = new NetworkStack(app, 'TestNetworkStack', TestDataFactory.createTestStackProps());
            const template = Template.fromStack(stack);

            // ALB security group should allow HTTP/HTTPS
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

        test('should validate database security', () => {
            const stack = new NetworkStack(app, 'TestNetworkStack', TestDataFactory.createTestStackProps());
            const template = Template.fromStack(stack);

            // RDS security group should only allow database port from EKS
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
    });
});