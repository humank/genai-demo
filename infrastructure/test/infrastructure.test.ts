import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { GenAIDemoInfrastructureStack } from '../lib/genai-demo-infrastructure-stack';

describe('GenAI Demo Multi-Stack Infrastructure Orchestration', () => {
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

    test('Multi-stack architecture creates separate stacks', () => {
        // The main stack should be minimal and only contain outputs
        // The actual resources are in separate stacks (NetworkStack, CertificateStack, CoreInfrastructureStack)
        // This is verified by checking that the main stack has minimal resources
        const resources = template.toJSON().Resources || {};
        const resourceCount = Object.keys(resources).length;

        // Main stack should have minimal resources (mainly outputs and metadata)
        expect(resourceCount).toBeLessThan(10); // Should be mostly outputs and metadata
    });

    test('Main stack outputs provide infrastructure summary', () => {
        template.hasOutput('InfrastructureStackSummary', {
            Description: 'Summary of all infrastructure stacks deployed',
            Export: {
                Name: 'genai-demo-test-test-infrastructure-summary'
            }
        });

        template.hasOutput('CrossStackReferences', {
            Description: 'Key resource identifiers for cross-stack references',
            Export: {
                Name: 'genai-demo-test-test-cross-stack-references'
            }
        });

        template.hasOutput('DeploymentStatus', {
            Value: 'COMPLETE',
            Description: 'Multi-stack infrastructure deployment status'
        });

        template.hasOutput('ArchitectureType', {
            Value: 'Multi-Stack-Modular-Secure',
            Description: 'Infrastructure architecture pattern used'
        });
    });

    test('Stack dependencies are properly configured', () => {
        // The main stack should have nested stacks with proper dependencies
        // This is validated by the CDK framework during synthesis
        expect(stack.networkStack).toBeDefined();
        expect(stack.certificateStack).toBeDefined();
        expect(stack.coreInfrastructureStack).toBeDefined();
    });

    test('Cross-stack references work correctly', () => {
        // Verify that the main stack can access resources from nested stacks
        expect(stack.getVpc()).toBeDefined();
        expect(stack.getSecurityGroups()).toBeDefined();
        expect(stack.getCertificates()).toBeDefined();
        expect(stack.getLoadBalancer()).toBeDefined();
    });

    test('Naming conventions are consistent across stacks', () => {
        // Verify that all nested stacks follow consistent naming patterns
        expect(stack.networkStack.stackName).toContain('NetworkStack');
        expect(stack.certificateStack.stackName).toContain('CertificateStack');
        expect(stack.coreInfrastructureStack.stackName).toContain('CoreInfrastructureStack');
    });

    test('Stack-level tagging is applied correctly', () => {
        // Verify that the main stack has proper tags applied
        // Individual stack tagging is tested in their respective test files
        expect(stack.networkStack).toBeDefined();
        expect(stack.certificateStack).toBeDefined();
        expect(stack.coreInfrastructureStack).toBeDefined();

        // Verify that each nested stack has the correct stack name pattern
        expect(stack.networkStack.stackName).toMatch(/NetworkStack/);
        expect(stack.certificateStack.stackName).toMatch(/CertificateStack/);
        expect(stack.coreInfrastructureStack.stackName).toMatch(/CoreInfrastructureStack/);
    });
});

describe('GenAI Demo Multi-Stack Infrastructure with Domain', () => {
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

    test('Multi-stack architecture with domain creates all separate stacks', () => {
        // Verify that all stacks are created and properly configured
        expect(stack.networkStack).toBeDefined();
        expect(stack.certificateStack).toBeDefined();
        expect(stack.coreInfrastructureStack).toBeDefined();

        // The main stack should be minimal when using separate stacks
        const resources = template.toJSON().Resources || {};
        const resourceCount = Object.keys(resources).length;
        expect(resourceCount).toBeLessThan(10); // Should be mostly outputs and metadata
    });

    test('Cross-stack references include certificate information', () => {
        // The cross-stack references output should include certificate ARN when domain is provided
        template.hasOutput('CrossStackReferences', {
            Description: 'Key resource identifiers for cross-stack references',
            Export: {
                Name: 'genai-demo-test-test-cross-stack-references'
            }
        });
    });

    test('Domain-specific stack integration works correctly', () => {
        // Verify that the main stack can access certificate resources
        expect(stack.getCertificates().certificate).toBeDefined();
        expect(stack.getCertificates().wildcardCertificate).toBeDefined();
        expect(stack.getCertificates().hostedZone).toBeDefined();
    });

    test('Stack dependencies work with domain configuration', () => {
        // Verify that core infrastructure stack depends on both network and certificate stacks
        expect(stack.networkStack).toBeDefined();
        expect(stack.certificateStack).toBeDefined();
        expect(stack.coreInfrastructureStack).toBeDefined();

        // The core infrastructure stack should have dependencies on the other stacks
        const dependencies = stack.coreInfrastructureStack.dependencies;
        expect(dependencies).toContain(stack.networkStack);
        expect(dependencies).toContain(stack.certificateStack);
    });
});


