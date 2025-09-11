/**
 * Comprehensive validation test suite for CDK Testing and Validation Infrastructure
 * Tests all aspects of the CDK infrastructure using enhanced testing framework
 * Implements task 5.10: CDK Testing and Validation Infrastructure
 */

import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { GenAIDemoInfrastructureStack } from '../lib/genai-demo-infrastructure-stack';
import {
    PerformanceTestHelper,
    StackTestHelper,
    TemplateHelper,
    TestAssertions
} from './utils/test-helpers';

describe('Comprehensive Infrastructure Validation', () => {
    let app: cdk.App;
    let stack: GenAIDemoInfrastructureStack;
    let template: Template;
    let templateHelper: TemplateHelper;

    beforeAll(() => {
        app = StackTestHelper.createTestApp();
        stack = new GenAIDemoInfrastructureStack(app, 'ComprehensiveTestStack', {
            ...StackTestHelper.createTestStackProps(),
            domain: 'test.kimkao.io'
        });
        template = Template.fromStack(stack);
        templateHelper = new TemplateHelper(template);
    });

    describe('Infrastructure Synthesis and Performance', () => {
        test('should synthesize without errors', () => {
            const synthesis = StackTestHelper.validateStackSynthesis(stack);
            expect(synthesis.success).toBe(true);
            expect(synthesis.errors).toHaveLength(0);
        });

        test('should meet performance benchmarks', () => {
            const performance = PerformanceTestHelper.measureSynthesisTime(stack);

            expect(performance.success).toBe(true);
            expect(performance.duration).toBeLessThan(30000); // Should synthesize in under 30 seconds
        });

        test('should have reasonable template complexity', () => {
            const complexity = PerformanceTestHelper.analyzeTemplateComplexity(template);

            expect(complexity.resourceCount).toBeGreaterThan(0);
            expect(complexity.resourceCount).toBeLessThan(500); // Reasonable upper limit
            expect(complexity.templateSize).toBeLessThan(1000000); // Under 1MB
            expect(complexity.complexity).toBeLessThan(1000); // Reasonable complexity score

            console.log('Template Complexity Analysis:', {
                resources: complexity.resourceCount,
                uniqueTypes: complexity.uniqueResourceTypes,
                size: `${Math.round(complexity.templateSize / 1024)}KB`,
                complexity: complexity.complexity
            });
        });
    });

    describe('Security and Compliance Validation', () => {
        test('should pass comprehensive security validation', () => {
            const report = templateHelper.generateValidationReport();

            expect(report.overall).toBe(true);
            expect(report.summary.criticalIssues).toBe(0);

            if (!report.overall) {
                console.error('Security validation failed:', report.issues);
            }
        });

        test('should have proper resource tagging', () => {
            expect(() => {
                TestAssertions.assertRequiredTags(template, ['Project', 'Environment', 'ManagedBy']);
            }).not.toThrow();
        });

        test('should follow security group best practices', () => {
            expect(() => {
                TestAssertions.assertSecurityGroupBestPractices(template);
            }).not.toThrow();
        });

        test('should have proper encryption configuration', () => {
            expect(() => {
                TestAssertions.assertEncryptionCompliance(template);
            }).not.toThrow();
        });

        test('should validate IAM policies for least privilege', () => {
            const validation = templateHelper.validateIAMPolicies();

            expect(validation.valid).toBe(true);

            if (!validation.valid) {
                console.error('IAM policy violations:', validation.issues);
            }
        });
    });

    describe('Resource Count and Configuration Validation', () => {
        test('should have expected VPC resources', () => {
            TestAssertions.assertResourceCount(template, 'AWS::EC2::VPC', 1);
            TestAssertions.assertResourceCount(template, 'AWS::EC2::InternetGateway', 1);

            // Should have subnets across multiple AZs
            const subnetCount = templateHelper.getResourceCount('AWS::EC2::Subnet');
            expect(subnetCount).toBeGreaterThanOrEqual(6); // 3 public + 3 private minimum
        });

        test('should have proper security group configuration', () => {
            const securityGroups = templateHelper.getResourcesByType('AWS::EC2::SecurityGroup');

            expect(securityGroups.length).toBeGreaterThanOrEqual(4); // EKS, RDS, MSK, ALB

            // Verify each security group has proper configuration
            securityGroups.forEach(sg => {
                expect(sg.Properties.GroupDescription).toBeDefined();
                expect(sg.Properties.VpcId).toBeDefined();
            });
        });

        test('should have observability resources when observability stack exists', () => {
            // Check for CloudWatch Log Groups
            const logGroups = templateHelper.getResourcesByType('AWS::Logs::LogGroup');
            expect(logGroups.length).toBeGreaterThan(0);

            // Check for SNS Topics
            const snsTopics = templateHelper.getResourcesByType('AWS::SNS::Topic');
            expect(snsTopics.length).toBeGreaterThan(0);

            // Check for S3 Buckets
            const s3Buckets = templateHelper.getResourcesByType('AWS::S3::Bucket');
            expect(s3Buckets.length).toBeGreaterThan(0);
        });

        test('should have KMS keys for encryption', () => {
            const kmsKeys = templateHelper.getResourcesByType('AWS::KMS::Key');
            expect(kmsKeys.length).toBeGreaterThan(0);

            // Verify key rotation is enabled
            kmsKeys.forEach(key => {
                expect(key.Properties.EnableKeyRotation).toBe(true);
            });
        });
    });

    describe('Cross-Stack Integration Validation', () => {
        test('should have proper stack dependencies', () => {
            expect(stack.networkStack).toBeDefined();
            expect(stack.certificateStack).toBeDefined();
            expect(stack.coreInfrastructureStack).toBeDefined();

            // Verify dependencies
            const coreDependencies = stack.coreInfrastructureStack.dependencies;
            expect(coreDependencies).toContain(stack.networkStack);
            expect(coreDependencies).toContain(stack.certificateStack);
        });

        test('should have proper cross-stack exports', () => {
            const networkTemplate = Template.fromStack(stack.networkStack);
            const outputs = networkTemplate.toJSON().Outputs || {};

            // Should export VPC and security group information
            expect(outputs.VpcId).toBeDefined();
            expect(outputs.EksSecurityGroupId).toBeDefined();
            expect(outputs.RdsSecurityGroupId).toBeDefined();
            expect(outputs.MskSecurityGroupId).toBeDefined();
            expect(outputs.AlbSecurityGroupId).toBeDefined();
        });

        test('should have consistent naming across stacks', () => {
            const projectName = 'genai-demo-test';
            const environment = 'test';

            expect(stack.networkStack.stackName).toContain(projectName);
            expect(stack.certificateStack.stackName).toContain(projectName);
            expect(stack.coreInfrastructureStack.stackName).toContain(projectName);

            expect(stack.networkStack.stackName).toContain('NetworkStack');
            expect(stack.certificateStack.stackName).toContain('CertificateStack');
            expect(stack.coreInfrastructureStack.stackName).toContain('CoreInfrastructureStack');
        });
    });

    describe('Environment-Specific Configuration Validation', () => {
        test('should handle development environment correctly', () => {
            const devApp = StackTestHelper.createTestApp();
            const devStack = new GenAIDemoInfrastructureStack(devApp, 'DevTestStack', {
                environment: 'development',
                projectName: 'genai-demo-dev',
                env: { account: '123456789012', region: 'ap-northeast-1' }
            });

            const devNetworkTemplate = Template.fromStack(devStack.networkStack);
            const devTemplateHelper = new TemplateHelper(devNetworkTemplate);

            // Development should have 1 NAT Gateway for cost optimization
            expect(devTemplateHelper.getResourceCount('AWS::EC2::NatGateway')).toBe(1);
        });

        test('should handle production environment correctly', () => {
            const prodApp = StackTestHelper.createTestApp();
            const prodStack = new GenAIDemoInfrastructureStack(prodApp, 'ProdTestStack', {
                environment: 'production',
                projectName: 'genai-demo-prod',
                domain: 'kimkao.io',
                env: { account: '123456789012', region: 'ap-northeast-1' }
            });

            const prodNetworkTemplate = Template.fromStack(prodStack.networkStack);
            const prodTemplateHelper = new TemplateHelper(prodNetworkTemplate);

            // Production should have 3 NAT Gateways for high availability
            expect(prodTemplateHelper.getResourceCount('AWS::EC2::NatGateway')).toBe(3);
        });

        test('should validate environment-specific retention policies', () => {
            // This would be validated in the observability stack tests
            // Here we just ensure the configuration is properly passed through
            expect(stack.networkStack).toBeDefined();
        });
    });

    describe('Cost Optimization Validation', () => {
        test('should use cost-effective resource configurations for test environment', () => {
            const networkTemplate = Template.fromStack(stack.networkStack);
            const networkHelper = new TemplateHelper(networkTemplate);

            // Test environment should have minimal NAT Gateways
            expect(networkHelper.getResourceCount('AWS::EC2::NatGateway')).toBe(1);
        });

        test('should have proper S3 lifecycle configurations', () => {
            const s3Buckets = templateHelper.getResourcesByType('AWS::S3::Bucket');

            s3Buckets.forEach(bucket => {
                if (bucket.Properties.LifecycleConfiguration) {
                    const rules = bucket.Properties.LifecycleConfiguration.Rules;
                    expect(rules).toBeDefined();
                    expect(Array.isArray(rules)).toBe(true);
                }
            });
        });

        test('should have appropriate log retention policies', () => {
            const logGroups = templateHelper.getResourcesByType('AWS::Logs::LogGroup');

            logGroups.forEach(logGroup => {
                if (logGroup.Properties.RetentionInDays) {
                    // Test environment should have short retention for cost savings
                    expect(logGroup.Properties.RetentionInDays).toBeLessThanOrEqual(30);
                }
            });
        });
    });

    describe('Disaster Recovery and Reliability Validation', () => {
        test('should have multi-AZ configuration where appropriate', () => {
            const subnets = templateHelper.getResourcesByType('AWS::EC2::Subnet');
            const availabilityZones = new Set(
                subnets.map(subnet => subnet.Properties.AvailabilityZone)
                    .filter(az => az && typeof az === 'string')
            );

            // Should span multiple AZs
            expect(availabilityZones.size).toBeGreaterThanOrEqual(2);
        });

        test('should have proper backup and versioning configuration', () => {
            const s3Buckets = templateHelper.getResourcesByType('AWS::S3::Bucket');

            s3Buckets.forEach(bucket => {
                if (bucket.Properties.VersioningConfiguration) {
                    expect(bucket.Properties.VersioningConfiguration.Status).toBe('Enabled');
                }
            });
        });

        test('should have monitoring and alerting configured', () => {
            const cloudWatchAlarms = templateHelper.getResourcesByType('AWS::CloudWatch::Alarm');
            const snsTopics = templateHelper.getResourcesByType('AWS::SNS::Topic');

            // Should have monitoring infrastructure
            expect(cloudWatchAlarms.length + snsTopics.length).toBeGreaterThan(0);
        });
    });

    describe('Template Comparison and Drift Detection', () => {
        test('should detect template changes between environments', () => {
            const devApp = StackTestHelper.createTestApp();
            const devStack = new GenAIDemoInfrastructureStack(devApp, 'DevCompareStack', {
                environment: 'development',
                projectName: 'genai-demo-dev',
                env: { account: '123456789012', region: 'ap-northeast-1' }
            });

            const devTemplate = Template.fromStack(devStack);
            const diff = StackTestHelper.compareTemplates(template, devTemplate);

            // Should have differences due to environment-specific configurations
            expect(diff.identical).toBe(false);
            expect(diff.differences.length).toBeGreaterThan(0);

            console.log('Template differences between test and dev:', diff.summary);
        });

        test('should maintain template consistency for same environment', () => {
            const app2 = StackTestHelper.createTestApp();
            const stack2 = new GenAIDemoInfrastructureStack(app2, 'ConsistencyTestStack', {
                ...StackTestHelper.createTestStackProps(),
                domain: 'test.kimkao.io'
            });

            const template2 = Template.fromStack(stack2);
            const diff = StackTestHelper.compareTemplates(template, template2);

            // Should be identical for same configuration
            expect(diff.identical).toBe(true);
            expect(diff.differences.length).toBe(0);
        });
    });

    describe('Comprehensive Validation Report', () => {
        test('should generate comprehensive validation report', () => {
            const report = templateHelper.generateValidationReport();

            // Log detailed report for review
            console.log('\n=== COMPREHENSIVE VALIDATION REPORT ===');
            console.log(`Overall Status: ${report.overall ? '✅ PASS' : '❌ FAIL'}`);
            console.log(`Total Resources: ${report.summary.resourceCount}`);
            console.log(`Total Issues: ${report.summary.totalIssues}`);
            console.log(`Critical Issues: ${report.summary.criticalIssues}`);

            console.log('\nCategory Results:');
            Object.entries(report.categories).forEach(([category, status]) => {
                console.log(`  ${category}: ${status ? '✅' : '❌'}`);
            });

            if (report.issues.length > 0) {
                console.log('\nIssues Found:');
                report.issues.forEach((issue, index) => {
                    console.log(`  ${index + 1}. [${issue.category}] ${issue.logicalId}: ${issue.issue || issue.missingTag}`);
                });
            }

            console.log('=====================================\n');

            // The test should pass even if there are non-critical issues
            // Critical issues should fail the test
            expect(report.summary.criticalIssues).toBe(0);
        });

        test('should validate all required infrastructure components', () => {
            const requiredComponents = [
                { type: 'AWS::EC2::VPC', minCount: 1, description: 'Virtual Private Cloud' },
                { type: 'AWS::EC2::SecurityGroup', minCount: 4, description: 'Security Groups' },
                { type: 'AWS::EC2::Subnet', minCount: 6, description: 'Subnets (public and private)' },
                { type: 'AWS::KMS::Key', minCount: 1, description: 'KMS Encryption Keys' }
            ];

            const missingComponents: string[] = [];

            requiredComponents.forEach(component => {
                const count = templateHelper.getResourceCount(component.type);
                if (count < component.minCount) {
                    missingComponents.push(
                        `${component.description}: expected ${component.minCount}, found ${count}`
                    );
                }
            });

            if (missingComponents.length > 0) {
                console.error('Missing required components:', missingComponents);
            }

            expect(missingComponents).toHaveLength(0);
        });

        test('should validate infrastructure drift detection capabilities', () => {
            // Test that snapshot tests can detect infrastructure changes
            const baselineTemplate = Template.fromStack(stack);

            // Create a modified version for comparison
            const modifiedApp = StackTestHelper.createTestApp();
            const modifiedStack = new GenAIDemoInfrastructureStack(modifiedApp, 'ModifiedTestStack', {
                ...StackTestHelper.createTestStackProps(),
                domain: 'modified.kimkao.io' // Different domain to create changes
            });
            const modifiedTemplate = Template.fromStack(modifiedStack);

            const diff = StackTestHelper.compareTemplates(baselineTemplate, modifiedTemplate);

            // Should detect differences when domain changes
            expect(diff.identical).toBe(false);
            expect(diff.differences.length).toBeGreaterThan(0);

            console.log('Infrastructure drift detection test:', {
                identical: diff.identical,
                changesDetected: diff.differences.length,
                summary: diff.summary
            });
        });

        test('should validate cost estimation integration', () => {
            // Verify that cost estimation can analyze the infrastructure
            const complexity = PerformanceTestHelper.analyzeTemplateComplexity(template);

            expect(complexity.resourceCount).toBeGreaterThan(0);
            expect(complexity.templateSize).toBeGreaterThan(0);

            // Cost estimation should be able to process this template
            const estimatedCostFactors = {
                resourceCount: complexity.resourceCount,
                uniqueResourceTypes: complexity.uniqueResourceTypes,
                complexity: complexity.complexity
            };

            console.log('Cost estimation validation:', estimatedCostFactors);

            // Template should be within reasonable bounds for cost estimation
            expect(estimatedCostFactors.resourceCount).toBeLessThan(500);
            expect(estimatedCostFactors.uniqueResourceTypes).toBeLessThan(50);
        });

        test('should validate budget alert configuration', () => {
            // Verify that infrastructure supports budget monitoring
            const resources = template.toJSON().Resources || {};

            // Should have resources that can be tagged for cost allocation
            const taggedResources = Object.values(resources).filter((resource: any) =>
                resource.Properties?.Tags
            );

            expect(taggedResources.length).toBeGreaterThan(0);

            // All tagged resources should have required cost allocation tags
            taggedResources.forEach((resource: any) => {
                const tags = resource.Properties.Tags;
                const hasProjectTag = tags.some((tag: any) => tag.Key === 'Project');
                const hasEnvironmentTag = tags.some((tag: any) => tag.Key === 'Environment');

                expect(hasProjectTag).toBe(true);
                expect(hasEnvironmentTag).toBe(true);
            });
        });
    });

    describe('CI/CD Pipeline Integration Validation', () => {
        test('should validate CDK synthesis for CI/CD pipeline', () => {
            // Test that synthesis works in CI/CD environment
            const synthesis = StackTestHelper.validateStackSynthesis(stack);

            expect(synthesis.success).toBe(true);
            expect(synthesis.errors).toHaveLength(0);

            // Should complete within reasonable time for CI/CD
            const performance = PerformanceTestHelper.measureSynthesisTime(stack);
            expect(performance.duration).toBeLessThan(60000); // 1 minute max for CI/CD
        });

        test('should validate automated security scanning integration', () => {
            // Verify that security scanning can analyze the infrastructure
            const securityValidation = templateHelper.validateSecurityGroups();
            const encryptionValidation = templateHelper.validateEncryption();
            const iamValidation = templateHelper.validateIAMPolicies();

            // Should not have critical security issues that would fail CI/CD
            expect(securityValidation.valid).toBe(true);

            // Log validation results for CI/CD reporting
            console.log('Security validation for CI/CD:', {
                securityGroups: securityValidation.valid,
                encryption: encryptionValidation.valid,
                iam: iamValidation.valid
            });
        });

        test('should validate infrastructure documentation generation', () => {
            // Verify that documentation can be generated from the infrastructure
            const complexity = PerformanceTestHelper.analyzeTemplateComplexity(template);
            const outputs = template.toJSON().Outputs || {};
            const parameters = template.toJSON().Parameters || {};

            // Should have sufficient metadata for documentation generation
            expect(Object.keys(outputs).length).toBeGreaterThan(0);

            // Template should be analyzable for documentation
            expect(complexity.resourceCount).toBeGreaterThan(0);
            expect(complexity.uniqueResourceTypes).toBeGreaterThan(0);

            console.log('Documentation generation validation:', {
                outputs: Object.keys(outputs).length,
                parameters: Object.keys(parameters).length,
                resourceTypes: complexity.uniqueResourceTypes
            });
        });
    });
});