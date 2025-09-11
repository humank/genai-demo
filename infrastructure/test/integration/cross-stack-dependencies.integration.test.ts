/**
 * Integration tests for cross-stack dependencies
 * Tests that stacks work together correctly and share resources properly
 */

import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { GenAIDemoInfrastructureStack } from '../../lib/genai-demo-infrastructure-stack';
import { NetworkStack } from '../../lib/stacks/network-stack';
import { TestDataFactory } from '../setup';

describe('Cross-Stack Dependencies Integration Tests', () => {
    let app: cdk.App;
    let mainStack: GenAIDemoInfrastructureStack;

    beforeEach(() => {
        app = TestDataFactory.createTestApp({
            'hosted-zone:account=123456789012:domainName=kimkao.io:region=ap-east-2': {
                'Id': '/hostedzone/Z2KTO3AQUJG1DT',
                'Name': 'kimkao.io.'
            }
        });
    });

    describe('Multi-Stack Architecture Integration', () => {
        test('should create all stacks with proper dependencies', () => {
            mainStack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            // Verify all nested stacks are created
            expect(mainStack.networkStack).toBeDefined();
            expect(mainStack.certificateStack).toBeDefined();
            expect(mainStack.coreInfrastructureStack).toBeDefined();

            // Verify stack dependencies
            const coreDependencies = mainStack.coreInfrastructureStack.dependencies;
            expect(coreDependencies).toContain(mainStack.networkStack);
            expect(coreDependencies).toContain(mainStack.certificateStack);
        });

        test('should share VPC resources across stacks', () => {
            mainStack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            const vpc = mainStack.getVpc();
            const securityGroups = mainStack.getSecurityGroups();

            expect(vpc).toBeDefined();
            expect(vpc.vpcId).toBeDefined();
            expect(securityGroups.eks).toBeDefined();
            expect(securityGroups.rds).toBeDefined();
            expect(securityGroups.msk).toBeDefined();
            expect(securityGroups.alb).toBeDefined();
        });

        test('should share certificate resources across stacks', () => {
            mainStack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            const certificates = mainStack.getCertificates();

            expect(certificates.certificate).toBeDefined();
            expect(certificates.wildcardCertificate).toBeDefined();
            expect(certificates.hostedZone).toBeDefined();
        });

        test('should create load balancer with proper certificate integration', () => {
            mainStack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            const loadBalancer = mainStack.getLoadBalancer();

            expect(loadBalancer).toBeDefined();
            expect(loadBalancer.loadBalancerArn).toBeDefined();
        });
    });

    describe('Cross-Stack Resource References', () => {
        test('should properly reference VPC from other stacks', () => {
            const networkStack = new NetworkStack(app, 'TestNetworkStack', TestDataFactory.createTestStackProps());

            // Verify network stack creates VPC
            expect(networkStack.vpc).toBeDefined();
            expect(networkStack.vpc.vpcId).toBeDefined();
        });

        test('should create proper CloudFormation exports and imports', () => {
            mainStack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            const networkTemplate = Template.fromStack(mainStack.networkStack);

            // Network stack should export VPC ID
            networkTemplate.hasOutput('VpcId', {
                Export: {
                    Name: 'genai-demo-test-test-vpc-id'
                }
            });

            // Core stack should be able to reference the VPC
            expect(mainStack.getLoadBalancer()).toBeDefined();
        });
    });

    describe('Stack Naming and Tagging Consistency', () => {
        test('should use consistent naming across all stacks', () => {
            mainStack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            expect(mainStack.networkStack.stackName).toContain('NetworkStack');
            expect(mainStack.certificateStack.stackName).toContain('CertificateStack');
            expect(mainStack.coreInfrastructureStack.stackName).toContain('CoreInfrastructureStack');

            // All stacks should have the same project name prefix
            expect(mainStack.networkStack.stackName).toContain('genai-demo-test');
            expect(mainStack.certificateStack.stackName).toContain('genai-demo-test');
            expect(mainStack.coreInfrastructureStack.stackName).toContain('genai-demo-test');
        });

        test('should apply consistent tags across all stacks', () => {
            mainStack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            // Verify that all stacks have consistent tagging
            const networkTemplate = Template.fromStack(mainStack.networkStack);
            const certTemplate = Template.fromStack(mainStack.certificateStack);
            const coreTemplate = Template.fromStack(mainStack.coreInfrastructureStack);

            // Check that resources in all stacks have consistent project tags
            [networkTemplate, certTemplate, coreTemplate].forEach(template => {
                const resources = template.toJSON().Resources || {};
                const resourcesWithTags = Object.values(resources).filter((resource: any) =>
                    resource.Properties?.Tags
                );

                resourcesWithTags.forEach((resource: any) => {
                    const tags = resource.Properties.Tags;
                    const projectTag = tags.find((tag: any) => tag.Key === 'Project');
                    expect(projectTag?.Value).toBe('genai-demo-test');
                });
            });
        });
    });

    describe('Environment-Specific Cross-Stack Integration', () => {
        test('should handle production environment with all stacks', () => {
            const prodStack = new GenAIDemoInfrastructureStack(app, 'TestProdStack', {
                environment: 'production',
                projectName: 'genai-demo-prod',
                domain: 'kimkao.io',
                env: {
                    account: '123456789012',
                    region: 'ap-northeast-1'
                }
            });

            expect(prodStack.networkStack).toBeDefined();
            expect(prodStack.certificateStack).toBeDefined();
            expect(prodStack.coreInfrastructureStack).toBeDefined();

            // Production should have enhanced configurations
            const networkTemplate = Template.fromStack(prodStack.networkStack);
            const resources = networkTemplate.toJSON().Resources || {};
            const natGateways = Object.values(resources).filter((resource: any) =>
                resource.Type === 'AWS::EC2::NatGateway'
            );
            expect(natGateways.length).toBe(3);
        });

        test('should handle development environment with cost optimizations', () => {
            const devStack = new GenAIDemoInfrastructureStack(app, 'TestDevStack', {
                environment: 'development',
                projectName: 'genai-demo-dev',
                env: {
                    account: '123456789012',
                    region: 'ap-northeast-1'
                }
            });

            expect(devStack.networkStack).toBeDefined();
            expect(devStack.coreInfrastructureStack).toBeDefined();

            // Development should have cost optimizations
            const networkTemplate = Template.fromStack(devStack.networkStack);
            const resources = networkTemplate.toJSON().Resources || {};
            const natGateways = Object.values(resources).filter((resource: any) =>
                resource.Type === 'AWS::EC2::NatGateway'
            );
            expect(natGateways.length).toBe(1);
        });
    });

    describe('Stack Synthesis and Deployment Order', () => {
        test('should synthesize all stacks without errors', () => {
            mainStack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            // This should not throw any errors
            expect(() => {
                app.synth();
            }).not.toThrow();
        });

        test('should have correct deployment dependencies', () => {
            mainStack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            // Core infrastructure should depend on both network and certificate stacks
            const coreDependencies = mainStack.coreInfrastructureStack.dependencies;
            expect(coreDependencies).toHaveLength(2);
            expect(coreDependencies).toContain(mainStack.networkStack);
            expect(coreDependencies).toContain(mainStack.certificateStack);

            // Network and certificate stacks should not depend on each other
            expect(mainStack.networkStack.dependencies).toHaveLength(0);
            expect(mainStack.certificateStack.dependencies).toHaveLength(0);
        });
    });

    describe('Resource Sharing Validation', () => {
        test('should validate that shared resources are accessible', () => {
            mainStack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            // Test that all shared resources are accessible through the main stack
            const vpc = mainStack.getVpc();
            const securityGroups = mainStack.getSecurityGroups();
            const certificates = mainStack.getCertificates();
            const loadBalancer = mainStack.getLoadBalancer();

            expect(vpc.vpcId).toBeDefined();
            expect(vpc.publicSubnets).toHaveLength(3);
            expect(vpc.privateSubnets).toHaveLength(3);

            expect(securityGroups.eks.securityGroupId).toBeDefined();
            expect(securityGroups.rds.securityGroupId).toBeDefined();
            expect(securityGroups.msk.securityGroupId).toBeDefined();
            expect(securityGroups.alb.securityGroupId).toBeDefined();

            if (certificates.certificate) {
                expect(certificates.certificate.certificateArn).toBeDefined();
            }
            if (certificates.wildcardCertificate) {
                expect(certificates.wildcardCertificate.certificateArn).toBeDefined();
            }
            if (certificates.hostedZone) {
                expect(certificates.hostedZone.hostedZoneId).toBeDefined();
            }

            expect(loadBalancer.loadBalancerArn).toBeDefined();
        });

        test('should validate cross-stack parameter passing', () => {
            mainStack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            // Verify that parameters are correctly passed between stacks
            const coreTemplate = Template.fromStack(mainStack.coreInfrastructureStack);

            // The core stack should reference resources from other stacks
            // This is validated by the successful creation of resources that depend on cross-stack references
            expect(mainStack.getLoadBalancer()).toBeDefined();
        });
    });
});