import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { getEnvironmentConfig } from '../../src/config/environment-config';
import { AlertingStack } from '../../src/stacks/alerting-stack';
import { AnalyticsStack } from '../../src/stacks/analytics-stack';
import { CoreInfrastructureStack } from '../../src/stacks/core-infrastructure-stack';
import { NetworkStack } from '../../src/stacks/network-stack';
import { ObservabilityStack } from '../../src/stacks/observability-stack';
import { SecurityStack } from '../../src/stacks/security-stack';

describe('Full Deployment Integration Tests', () => {
    let app: cdk.App;

    beforeEach(() => {
        app = new cdk.App({
            context: {
                environment: 'test',
                region: 'us-east-1',
                enableAnalytics: 'true',
                enableCdkNag: 'false', // Disable for integration tests
            },
        });
    });

    test('should deploy complete infrastructure successfully', () => {
        const environment = 'test';
        const region = 'us-east-1';
        const config = getEnvironmentConfig(environment, region);

        // Create all stacks in correct order
        const networkStack = new NetworkStack(app, `${config.stackPrefix}NetworkStack`, {
            env: { region: config.region },
            description: `Network infrastructure for ${environment} environment`,
            tags: config.tags,
        });

        const securityStack = new SecurityStack(app, `${config.stackPrefix}SecurityStack`, {
            env: { region: config.region },
            description: `Security infrastructure for ${environment} environment`,
            tags: config.tags,
        });

        const alertingStack = new AlertingStack(app, `${config.stackPrefix}AlertingStack`, {
            environment,
            region: config.region,
            applicationName: 'genai-demo',
            alertingConfig: {
                criticalAlerts: {
                    emailAddresses: ['admin@example.com'],
                },
                warningAlerts: {
                    emailAddresses: ['admin@example.com'],
                },
                infoAlerts: {
                    emailAddresses: ['admin@example.com'],
                },
            },
            env: { region: config.region },
            description: `Alerting infrastructure for ${environment} environment`,
            tags: config.tags,
        });

        const coreStack = new CoreInfrastructureStack(app, `${config.stackPrefix}CoreInfrastructureStack`, {
            vpc: networkStack.vpc,
            securityGroups: networkStack.securityGroups,
            kmsKey: securityStack.kmsKey,
            env: { region: config.region },
            description: `Core infrastructure for ${environment} environment`,
            tags: config.tags,
        });

        const observabilityStack = new ObservabilityStack(app, `${config.stackPrefix}ObservabilityStack`, {
            vpc: networkStack.vpc,
            kmsKey: securityStack.kmsKey,
            env: { region: config.region },
            description: `Observability infrastructure for ${environment} environment`,
            tags: config.tags,
        });

        // Create mock MSK cluster for analytics
        const mockMskCluster = {
            ref: 'mock-msk-cluster',
            attrArn: `arn:aws:kafka:${config.region}:123456789012:cluster/mock-cluster/*`
        } as any;

        const analyticsStack = new AnalyticsStack(app, `${config.stackPrefix}AnalyticsStack`, {
            environment,
            projectName: 'genai-demo',
            vpc: networkStack.vpc,
            kmsKey: securityStack.kmsKey,
            mskCluster: mockMskCluster,
            alertingTopic: alertingStack.criticalAlertsTopic,
            region: config.region,
            env: { region: config.region },
            description: `Analytics infrastructure for ${environment} environment`,
            tags: config.tags,
        });

        // Set up dependencies
        coreStack.addDependency(networkStack);
        coreStack.addDependency(securityStack);
        observabilityStack.addDependency(networkStack);
        observabilityStack.addDependency(securityStack);
        alertingStack.addDependency(networkStack);
        analyticsStack.addDependency(networkStack);
        analyticsStack.addDependency(securityStack);
        analyticsStack.addDependency(alertingStack);

        // Synthesize and verify no errors
        const assembly = app.synth();

        // Verify all stacks are created
        expect(assembly.stacks).toHaveLength(6);

        // Verify stack names
        const stackNames = assembly.stacks.map(stack => stack.stackName);
        expect(stackNames).toContain('genai-demo-test-NetworkStack');
        expect(stackNames).toContain('genai-demo-test-SecurityStack');
        expect(stackNames).toContain('genai-demo-test-AlertingStack');
        expect(stackNames).toContain('genai-demo-test-CoreInfrastructureStack');
        expect(stackNames).toContain('genai-demo-test-ObservabilityStack');
        expect(stackNames).toContain('genai-demo-test-AnalyticsStack');
    });

    test('should validate resource counts across all stacks', () => {
        const environment = 'test';
        const region = 'us-east-1';
        const config = getEnvironmentConfig(environment, region);

        // Create minimal stack set for resource counting
        const networkStack = new NetworkStack(app, `${config.stackPrefix}NetworkStack`, {
            env: { region: config.region },
        });

        const securityStack = new SecurityStack(app, `${config.stackPrefix}SecurityStack`, {
            env: { region: config.region },
        });

        const coreStack = new CoreInfrastructureStack(app, `${config.stackPrefix}CoreInfrastructureStack`, {
            vpc: networkStack.vpc,
            securityGroups: networkStack.securityGroups,
            kmsKey: securityStack.kmsKey,
            env: { region: config.region },
        });

        // Get templates
        const networkTemplate = Template.fromStack(networkStack);
        const securityTemplate = Template.fromStack(securityStack);
        const coreTemplate = Template.fromStack(coreStack);

        // Validate resource counts
        networkTemplate.resourceCountIs('AWS::EC2::VPC', 1);
        networkTemplate.resourceCountIs('AWS::EC2::SecurityGroup', 3);
        networkTemplate.resourceCountIs('AWS::EC2::Subnet', 6);

        securityTemplate.resourceCountIs('AWS::KMS::Key', 1);
        securityTemplate.resourceCountIs('AWS::IAM::Role', 1);

        coreTemplate.resourceCountIs('AWS::ElasticLoadBalancingV2::LoadBalancer', 1);
        coreTemplate.resourceCountIs('AWS::ElasticLoadBalancingV2::TargetGroup', 1);
    });

    test('should validate stack outputs and cross-stack references', () => {
        const environment = 'test';
        const region = 'us-east-1';
        const config = getEnvironmentConfig(environment, region);

        const networkStack = new NetworkStack(app, `${config.stackPrefix}NetworkStack`, {
            env: { region: config.region },
        });

        const securityStack = new SecurityStack(app, `${config.stackPrefix}SecurityStack`, {
            env: { region: config.region },
        });

        // Verify outputs exist
        const networkTemplate = Template.fromStack(networkStack);
        const securityTemplate = Template.fromStack(securityStack);

        networkTemplate.hasOutput('VpcId', {});
        networkTemplate.hasOutput('ALBSecurityGroupId', {});

        securityTemplate.hasOutput('KMSKeyId', {});
        securityTemplate.hasOutput('ApplicationRoleArn', {});

        // Verify cross-stack references work
        const coreStack = new CoreInfrastructureStack(app, `${config.stackPrefix}CoreInfrastructureStack`, {
            vpc: networkStack.vpc,
            securityGroups: networkStack.securityGroups,
            kmsKey: securityStack.kmsKey,
            env: { region: config.region },
        });

        // Should not throw during synthesis (create new app to avoid multiple synth issue)
        const testApp = new cdk.App();
        const testNetworkStack = new NetworkStack(testApp, `${config.stackPrefix}NetworkStack`, {
            env: { region: config.region },
        });
        const testSecurityStack = new SecurityStack(testApp, `${config.stackPrefix}SecurityStack`, {
            env: { region: config.region },
        });
        const testCoreStack = new CoreInfrastructureStack(testApp, `${config.stackPrefix}CoreInfrastructureStack`, {
            vpc: testNetworkStack.vpc,
            securityGroups: testNetworkStack.securityGroups,
            kmsKey: testSecurityStack.kmsKey,
            env: { region: config.region },
        });

        expect(() => testApp.synth()).not.toThrow();
    });

    test('should handle optional analytics stack correctly', () => {
        const environment = 'test';
        const region = 'us-east-1';
        const config = getEnvironmentConfig(environment, region);

        // Test with analytics disabled
        const appWithoutAnalytics = new cdk.App({
            context: {
                environment: 'test',
                region: 'us-east-1',
                enableAnalytics: 'false',
            },
        });

        const networkStack = new NetworkStack(appWithoutAnalytics, `${config.stackPrefix}NetworkStack`, {
            env: { region: config.region },
        });

        const securityStack = new SecurityStack(appWithoutAnalytics, `${config.stackPrefix}SecurityStack`, {
            env: { region: config.region },
        });

        // Should synthesize successfully without analytics
        const assembly = appWithoutAnalytics.synth();
        expect(assembly.stacks).toHaveLength(2);

        // Test with analytics enabled
        const appWithAnalytics = new cdk.App({
            context: {
                environment: 'test',
                region: 'us-east-1',
                enableAnalytics: 'true',
            },
        });

        const networkStack2 = new NetworkStack(appWithAnalytics, `${config.stackPrefix}NetworkStack`, {
            env: { region: config.region },
        });

        const securityStack2 = new SecurityStack(appWithAnalytics, `${config.stackPrefix}SecurityStack`, {
            env: { region: config.region },
        });

        const alertingStack2 = new AlertingStack(appWithAnalytics, `${config.stackPrefix}AlertingStack`, {
            environment,
            region: config.region,
            applicationName: 'genai-demo',
            alertingConfig: {
                criticalAlerts: {
                    emailAddresses: ['admin@example.com'],
                },
                warningAlerts: {
                    emailAddresses: ['admin@example.com'],
                },
                infoAlerts: {
                    emailAddresses: ['admin@example.com'],
                },
            },
            env: { region: config.region },
        });

        const mockMskCluster = {
            ref: 'mock-msk-cluster',
            attrArn: `arn:aws:kafka:${config.region}:123456789012:cluster/mock-cluster/*`
        } as any;

        const analyticsStack = new AnalyticsStack(appWithAnalytics, `${config.stackPrefix}AnalyticsStack`, {
            environment,
            projectName: 'genai-demo',
            vpc: networkStack2.vpc,
            kmsKey: securityStack2.kmsKey,
            mskCluster: mockMskCluster,
            alertingTopic: alertingStack2.criticalAlertsTopic,
            region: config.region,
            env: { region: config.region },
        });

        // Should synthesize successfully with analytics
        const assemblyWithAnalytics = appWithAnalytics.synth();
        expect(assemblyWithAnalytics.stacks).toHaveLength(4);
    });
});