/**
 * Snapshot tests for infrastructure drift detection
 * Captures CDK template snapshots to detect unintended changes
 */

import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { GenAIDemoInfrastructureStack } from '../../lib/genai-demo-infrastructure-stack';
import { CertificateStack } from '../../lib/stacks/certificate-stack';
import { NetworkStack } from '../../lib/stacks/network-stack';
import { ObservabilityStack } from '../../lib/stacks/observability-stack';
import { TestDataFactory } from '../setup';

describe('Infrastructure Drift Detection Snapshots', () => {
    let app: cdk.App;

    beforeEach(() => {
        app = TestDataFactory.createTestApp({
            'hosted-zone:account=123456789012:domainName=kimkao.io:region=ap-east-2': {
                'Id': '/hostedzone/Z2KTO3AQUJG1DT',
                'Name': 'kimkao.io.'
            }
        });
    });

    describe('Complete Infrastructure Snapshots', () => {
        test('should match main infrastructure stack snapshot', () => {
            const stack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            const template = Template.fromStack(stack);
            expect(template.toJSON()).toMatchSnapshot('main-infrastructure-stack.json');
        });

        test('should match network stack snapshot', () => {
            const stack = new NetworkStack(app, 'TestNetworkStack', TestDataFactory.createTestStackProps());
            const template = Template.fromStack(stack);
            expect(template.toJSON()).toMatchSnapshot('network-stack.json');
        });

        test('should match certificate stack snapshot', () => {
            const stack = new CertificateStack(app, 'TestCertificateStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });
            const template = Template.fromStack(stack);
            expect(template.toJSON()).toMatchSnapshot('certificate-stack.json');
        });

        test('should match observability stack snapshot', () => {
            const networkStack = new NetworkStack(app, 'TestNetworkStack', TestDataFactory.createTestStackProps());

            const stack = new ObservabilityStack(app, 'TestObservabilityStack', {
                ...TestDataFactory.createTestStackProps(),
                vpc: networkStack.vpc,
                region: 'ap-northeast-1'
            });

            const template = Template.fromStack(stack);
            expect(template.toJSON()).toMatchSnapshot('observability-stack.json');
        });
    });

    describe('Environment-Specific Snapshots', () => {
        test('should match development environment snapshot', () => {
            const stack = new GenAIDemoInfrastructureStack(app, 'TestDevStack', {
                environment: 'development',
                projectName: 'genai-demo-dev',
                env: {
                    account: '123456789012',
                    region: 'ap-northeast-1'
                }
            });

            const template = Template.fromStack(stack);
            expect(template.toJSON()).toMatchSnapshot('development-environment.json');
        });

        test('should match production environment snapshot', () => {
            const stack = new GenAIDemoInfrastructureStack(app, 'TestProdStack', {
                environment: 'production',
                projectName: 'genai-demo-prod',
                domain: 'kimkao.io',
                env: {
                    account: '123456789012',
                    region: 'ap-northeast-1'
                }
            });

            const template = Template.fromStack(stack);
            expect(template.toJSON()).toMatchSnapshot('production-environment.json');
        });

        test('should match test environment snapshot', () => {
            const stack = new GenAIDemoInfrastructureStack(app, 'TestTestStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            const template = Template.fromStack(stack);
            expect(template.toJSON()).toMatchSnapshot('test-environment.json');
        });
    });

    describe('Component-Specific Snapshots', () => {
        test('should match VPC configuration snapshot', () => {
            const stack = new NetworkStack(app, 'TestNetworkStack', TestDataFactory.createTestStackProps());
            const template = Template.fromStack(stack);

            const vpcResources = Object.entries(template.toJSON().Resources || {})
                .filter(([_, resource]: [string, any]) =>
                    resource.Type.startsWith('AWS::EC2::') &&
                    ['VPC', 'Subnet', 'RouteTable', 'Route', 'InternetGateway', 'NatGateway'].some(type =>
                        resource.Type.includes(type)
                    )
                )
                .reduce((acc, [key, value]) => ({ ...acc, [key]: value }), {});

            expect(vpcResources).toMatchSnapshot('vpc-configuration.json');
        });

        test('should match security groups snapshot', () => {
            const stack = new NetworkStack(app, 'TestNetworkStack', TestDataFactory.createTestStackProps());
            const template = Template.fromStack(stack);

            const securityGroupResources = Object.entries(template.toJSON().Resources || {})
                .filter(([_, resource]: [string, any]) =>
                    resource.Type === 'AWS::EC2::SecurityGroup' ||
                    resource.Type === 'AWS::EC2::SecurityGroupIngress' ||
                    resource.Type === 'AWS::EC2::SecurityGroupEgress'
                )
                .reduce((acc, [key, value]) => ({ ...acc, [key]: value }), {});

            expect(securityGroupResources).toMatchSnapshot('security-groups.json');
        });

        test('should match observability components snapshot', () => {
            const networkStack = new NetworkStack(app, 'TestNetworkStack', TestDataFactory.createTestStackProps());

            const stack = new ObservabilityStack(app, 'TestObservabilityStack', {
                ...TestDataFactory.createTestStackProps(),
                vpc: networkStack.vpc,
                region: 'ap-northeast-1'
            });

            const template = Template.fromStack(stack);

            const observabilityResources = Object.entries(template.toJSON().Resources || {})
                .filter(([_, resource]: [string, any]) =>
                    ['AWS::Logs::LogGroup', 'AWS::SNS::Topic', 'AWS::CloudWatch::Alarm',
                        'AWS::Events::Rule', 'AWS::S3::Bucket', 'AWS::KMS::Key'].includes(resource.Type)
                )
                .reduce((acc, [key, value]) => ({ ...acc, [key]: value }), {});

            expect(observabilityResources).toMatchSnapshot('observability-components.json');
        });
    });

    describe('Cross-Stack Dependencies Snapshots', () => {
        test('should match cross-stack exports snapshot', () => {
            const stack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            // Collect all outputs from all stacks
            const allOutputs = {
                main: Template.fromStack(stack).toJSON().Outputs || {},
                network: Template.fromStack(stack.networkStack).toJSON().Outputs || {},
                certificate: Template.fromStack(stack.certificateStack).toJSON().Outputs || {},
                core: Template.fromStack(stack.coreInfrastructureStack).toJSON().Outputs || {}
            };

            expect(allOutputs).toMatchSnapshot('cross-stack-exports.json');
        });

        test('should match stack dependencies snapshot', () => {
            const stack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            const dependencies = {
                main: stack.dependencies.map(dep => dep.stackName),
                network: stack.networkStack.dependencies.map(dep => dep.stackName),
                certificate: stack.certificateStack.dependencies.map(dep => dep.stackName),
                core: stack.coreInfrastructureStack.dependencies.map(dep => dep.stackName)
            };

            expect(dependencies).toMatchSnapshot('stack-dependencies.json');
        });
    });

    describe('Resource Tagging Snapshots', () => {
        test('should match resource tagging patterns snapshot', () => {
            const stack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            // Extract all tags from all resources across all stacks
            const extractTags = (template: Template) => {
                const resources = template.toJSON().Resources || {};
                return Object.entries(resources)
                    .filter(([_, resource]: [string, any]) => resource.Properties?.Tags)
                    .map(([logicalId, resource]: [string, any]) => ({
                        logicalId,
                        resourceType: resource.Type,
                        tags: resource.Properties.Tags
                    }));
            };

            const allTags = {
                main: extractTags(Template.fromStack(stack)),
                network: extractTags(Template.fromStack(stack.networkStack)),
                certificate: extractTags(Template.fromStack(stack.certificateStack)),
                core: extractTags(Template.fromStack(stack.coreInfrastructureStack))
            };

            expect(allTags).toMatchSnapshot('resource-tagging-patterns.json');
        });
    });

    describe('Configuration Drift Detection', () => {
        test('should detect changes in CDK context configuration', () => {
            const contextSnapshot = app.node.tryGetContext('genai-demo:environments');
            expect(contextSnapshot).toMatchSnapshot('cdk-context-configuration.json');
        });

        test('should detect changes in stack properties', () => {
            const stackProps = TestDataFactory.createTestStackProps();
            expect(stackProps).toMatchSnapshot('stack-properties.json');
        });

        test('should detect changes in environment-specific configurations', () => {
            const environments = ['development', 'test', 'production'];
            const envConfigs = environments.map(env => ({
                environment: env,
                config: app.node.tryGetContext(`genai-demo:environments:${env}`) || {}
            }));

            expect(envConfigs).toMatchSnapshot('environment-configurations.json');
        });
    });

    describe('Metadata and Annotations Snapshots', () => {
        test('should match CDK metadata snapshot', () => {
            const stack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            const metadata = {
                main: Template.fromStack(stack).toJSON().Metadata || {},
                network: Template.fromStack(stack.networkStack).toJSON().Metadata || {},
                certificate: Template.fromStack(stack.certificateStack).toJSON().Metadata || {},
                core: Template.fromStack(stack.coreInfrastructureStack).toJSON().Metadata || {}
            };

            expect(metadata).toMatchSnapshot('cdk-metadata.json');
        });

        test('should match stack annotations snapshot', () => {
            const stack = new GenAIDemoInfrastructureStack(app, 'TestMainStack', {
                ...TestDataFactory.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            const annotations = {
                main: stack.node.metadata,
                network: stack.networkStack.node.metadata,
                certificate: stack.certificateStack.node.metadata,
                core: stack.coreInfrastructureStack.node.metadata
            };

            expect(annotations).toMatchSnapshot('stack-annotations.json');
        });
    });
});