/**
 * Test helper utilities for CDK infrastructure testing
 * Provides common testing patterns and utilities
 */

import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';

/**
 * Enhanced template matcher with additional utilities
 */
export class TemplateHelper {
    constructor(private template: Template) { }

    /**
     * Check if template has resource with specific properties
     */
    hasResourceWithProperties(resourceType: string, properties: any): boolean {
        try {
            this.template.hasResourceProperties(resourceType, properties);
            return true;
        } catch {
            return false;
        }
    }

    /**
     * Get all resources of a specific type
     */
    getResourcesByType(resourceType: string): any[] {
        const resources = this.template.toJSON().Resources || {};
        return Object.entries(resources)
            .filter(([_, resource]: [string, any]) => resource.Type === resourceType)
            .map(([logicalId, resource]) => ({ logicalId, ...(resource as object) }));
    }

    /**
     * Get resource count by type
     */
    getResourceCount(resourceType: string): number {
        return this.getResourcesByType(resourceType).length;
    }

    /**
     * Check if all resources have required tags
     */
    validateResourceTags(requiredTags: string[]): { valid: boolean; violations: any[] } {
        const violations: any[] = [];
        const resources = this.template.toJSON().Resources || {};

        for (const [logicalId, resource] of Object.entries(resources)) {
            const resourceData = resource as any;
            if (resourceData.Type.startsWith('AWS::')) {
                const tags = resourceData.Properties?.Tags || [];
                const tagKeys = tags.map((tag: any) => tag.Key);

                for (const requiredTag of requiredTags) {
                    if (!tagKeys.includes(requiredTag)) {
                        violations.push({
                            logicalId,
                            resourceType: resourceData.Type,
                            missingTag: requiredTag
                        });
                    }
                }
            }
        }

        return {
            valid: violations.length === 0,
            violations
        };
    }

    /**
     * Validate security group rules
     */
    validateSecurityGroups(): { valid: boolean; issues: any[] } {
        const issues: any[] = [];
        const securityGroups = this.getResourcesByType('AWS::EC2::SecurityGroup');

        for (const sg of securityGroups) {
            const ingressRules = sg.Properties?.SecurityGroupIngress || [];

            for (const rule of ingressRules) {
                // Check for overly permissive rules
                if (rule.CidrIp === '0.0.0.0/0' && rule.FromPort !== 80 && rule.FromPort !== 443) {
                    issues.push({
                        logicalId: sg.logicalId,
                        issue: 'Overly permissive ingress rule',
                        details: `Port ${rule.FromPort} open to 0.0.0.0/0`
                    });
                }

                // Check for SSH access from internet
                if (rule.FromPort === 22 && rule.CidrIp === '0.0.0.0/0') {
                    issues.push({
                        logicalId: sg.logicalId,
                        issue: 'SSH access from internet',
                        details: 'Port 22 accessible from 0.0.0.0/0'
                    });
                }
            }
        }

        return {
            valid: issues.length === 0,
            issues
        };
    }

    /**
     * Validate encryption configuration
     */
    validateEncryption(): { valid: boolean; issues: any[] } {
        const issues: any[] = [];

        // Check S3 buckets
        const s3Buckets = this.getResourcesByType('AWS::S3::Bucket');
        for (const bucket of s3Buckets) {
            if (!bucket.Properties?.BucketEncryption) {
                issues.push({
                    logicalId: bucket.logicalId,
                    resourceType: 'S3::Bucket',
                    issue: 'Missing encryption configuration'
                });
            }
        }

        // Check RDS instances
        const rdsInstances = this.getResourcesByType('AWS::RDS::DBInstance');
        for (const instance of rdsInstances) {
            if (!instance.Properties?.StorageEncrypted) {
                issues.push({
                    logicalId: instance.logicalId,
                    resourceType: 'RDS::DBInstance',
                    issue: 'Storage encryption not enabled'
                });
            }
        }

        // Check KMS keys
        const kmsKeys = this.getResourcesByType('AWS::KMS::Key');
        for (const key of kmsKeys) {
            if (!key.Properties?.EnableKeyRotation) {
                issues.push({
                    logicalId: key.logicalId,
                    resourceType: 'KMS::Key',
                    issue: 'Key rotation not enabled'
                });
            }
        }

        return {
            valid: issues.length === 0,
            issues
        };
    }

    /**
     * Validate IAM policies for least privilege
     */
    validateIAMPolicies(): { valid: boolean; issues: any[] } {
        const issues: any[] = [];

        // Check IAM roles
        const iamRoles = this.getResourcesByType('AWS::IAM::Role');
        for (const role of iamRoles) {
            const assumeRolePolicy = role.Properties?.AssumeRolePolicyDocument;

            if (assumeRolePolicy?.Statement) {
                for (const statement of assumeRolePolicy.Statement) {
                    if (statement.Principal === '*' || statement.Principal?.AWS === '*') {
                        issues.push({
                            logicalId: role.logicalId,
                            resourceType: 'IAM::Role',
                            issue: 'Overly permissive assume role policy',
                            details: 'Principal allows any AWS account'
                        });
                    }
                }
            }

            // Check for dangerous managed policies
            const managedPolicies = role.Properties?.ManagedPolicyArns || [];
            const dangerousPolicies = [
                'arn:aws:iam::aws:policy/PowerUserAccess',
                'arn:aws:iam::aws:policy/IAMFullAccess'
            ];

            for (const policy of managedPolicies) {
                if (dangerousPolicies.includes(policy)) {
                    issues.push({
                        logicalId: role.logicalId,
                        resourceType: 'IAM::Role',
                        issue: 'Dangerous managed policy attached',
                        details: `Policy: ${policy}`
                    });
                }
            }
        }

        return {
            valid: issues.length === 0,
            issues
        };
    }

    /**
     * Generate comprehensive validation report
     */
    generateValidationReport(): ValidationReport {
        const tagValidation = this.validateResourceTags(['Project', 'Environment', 'ManagedBy']);
        const securityValidation = this.validateSecurityGroups();
        const encryptionValidation = this.validateEncryption();
        const iamValidation = this.validateIAMPolicies();

        const allIssues = [
            ...tagValidation.violations.map(v => ({ category: 'Tagging', ...v })),
            ...securityValidation.issues.map(i => ({ category: 'Security Groups', ...i })),
            ...encryptionValidation.issues.map(i => ({ category: 'Encryption', ...i })),
            ...iamValidation.issues.map(i => ({ category: 'IAM', ...i }))
        ];

        return {
            overall: allIssues.length === 0,
            categories: {
                tagging: tagValidation.valid,
                security: securityValidation.valid,
                encryption: encryptionValidation.valid,
                iam: iamValidation.valid
            },
            issues: allIssues,
            summary: {
                totalIssues: allIssues.length,
                criticalIssues: allIssues.filter(i => i.category === 'Security Groups' || i.category === 'IAM').length,
                resourceCount: Object.keys(this.template.toJSON().Resources || {}).length
            }
        };
    }
}

/**
 * Validation report interface
 */
export interface ValidationReport {
    overall: boolean;
    categories: {
        tagging: boolean;
        security: boolean;
        encryption: boolean;
        iam: boolean;
    };
    issues: any[];
    summary: {
        totalIssues: number;
        criticalIssues: number;
        resourceCount: number;
    };
}

/**
 * Stack testing utilities
 */
export class StackTestHelper {
    /**
     * Create a test app with standard configuration
     */
    static createTestApp(additionalContext?: Record<string, any>): cdk.App {
        return new cdk.App({
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
                        'nat-gateways': 1,
                        'retention-policies': {
                            'logs': 7,
                            'metrics': 7,
                            'backups': 7,
                            'snapshots': 3
                        }
                    },
                    'development': {
                        'vpc-cidr': '10.1.0.0/16',
                        'nat-gateways': 1,
                        'retention-policies': {
                            'logs': 7,
                            'metrics': 30,
                            'backups': 30,
                            'snapshots': 7
                        }
                    },
                    'production': {
                        'vpc-cidr': '10.2.0.0/16',
                        'nat-gateways': 3,
                        'retention-policies': {
                            'logs': 90,
                            'metrics': 365,
                            'backups': 365,
                            'snapshots': 30
                        }
                    }
                },
                'genai-demo:observability': {
                    'log-retention-days': 7,
                    'metrics-retention-days': 30,
                    'trace-sampling-rate': 0.1
                },
                'hosted-zone:account=123456789012:domainName=kimkao.io:region=ap-east-2': {
                    'Id': '/hostedzone/Z2KTO3AQUJG1DT',
                    'Name': 'kimkao.io.'
                },
                ...additionalContext
            }
        });
    }

    /**
     * Create standard test stack properties
     */
    static createTestStackProps(overrides?: any): any {
        return {
            environment: 'test',
            projectName: 'genai-demo-test',
            env: {
                account: '123456789012',
                region: 'ap-northeast-1'
            },
            ...overrides
        };
    }

    /**
     * Validate stack synthesis
     */
    static validateStackSynthesis(stack: cdk.Stack): { success: boolean; errors: string[] } {
        try {
            const app = stack.node.root as cdk.App;
            app.synth();
            return { success: true, errors: [] };
        } catch (error) {
            return {
                success: false,
                errors: [error instanceof Error ? error.message : String(error)]
            };
        }
    }

    /**
     * Compare two templates for differences
     */
    static compareTemplates(template1: Template, template2: Template): TemplateDiff {
        const json1 = template1.toJSON();
        const json2 = template2.toJSON();

        const differences: any[] = [];

        // Compare resources
        const resources1 = json1.Resources || {};
        const resources2 = json2.Resources || {};

        const allResourceIds = new Set([...Object.keys(resources1), ...Object.keys(resources2)]);

        for (const resourceId of allResourceIds) {
            if (!(resourceId in resources1)) {
                differences.push({
                    type: 'resource-added',
                    resourceId,
                    details: resources2[resourceId]
                });
            } else if (!(resourceId in resources2)) {
                differences.push({
                    type: 'resource-removed',
                    resourceId,
                    details: resources1[resourceId]
                });
            } else if (JSON.stringify(resources1[resourceId]) !== JSON.stringify(resources2[resourceId])) {
                differences.push({
                    type: 'resource-modified',
                    resourceId,
                    before: resources1[resourceId],
                    after: resources2[resourceId]
                });
            }
        }

        return {
            identical: differences.length === 0,
            differences,
            summary: {
                added: differences.filter(d => d.type === 'resource-added').length,
                removed: differences.filter(d => d.type === 'resource-removed').length,
                modified: differences.filter(d => d.type === 'resource-modified').length
            }
        };
    }
}

/**
 * Template difference interface
 */
export interface TemplateDiff {
    identical: boolean;
    differences: any[];
    summary: {
        added: number;
        removed: number;
        modified: number;
    };
}

/**
 * Performance testing utilities
 */
export class PerformanceTestHelper {
    /**
     * Measure stack synthesis time
     */
    static measureSynthesisTime(stack: cdk.Stack): { duration: number; success: boolean } {
        const startTime = Date.now();

        try {
            const app = stack.node.root as cdk.App;
            app.synth();
            const endTime = Date.now();

            return {
                duration: endTime - startTime,
                success: true
            };
        } catch (error) {
            const endTime = Date.now();

            return {
                duration: endTime - startTime,
                success: false
            };
        }
    }

    /**
     * Analyze template size and complexity
     */
    static analyzeTemplateComplexity(template: Template): TemplateComplexity {
        const json = template.toJSON();
        const resources = json.Resources || {};
        const outputs = json.Outputs || {};
        const parameters = json.Parameters || {};

        const resourceTypes = Object.values(resources).map((r: any) => r.Type);
        const uniqueResourceTypes = new Set(resourceTypes);

        return {
            resourceCount: Object.keys(resources).length,
            outputCount: Object.keys(outputs).length,
            parameterCount: Object.keys(parameters).length,
            uniqueResourceTypes: uniqueResourceTypes.size,
            templateSize: JSON.stringify(json).length,
            complexity: this.calculateComplexityScore(resources)
        };
    }

    /**
     * Calculate complexity score based on resource relationships
     */
    private static calculateComplexityScore(resources: any): number {
        let score = 0;

        for (const resource of Object.values(resources)) {
            const resourceData = resource as any;

            // Add base score for each resource
            score += 1;

            // Add complexity for dependencies
            if (resourceData.DependsOn) {
                score += Array.isArray(resourceData.DependsOn) ? resourceData.DependsOn.length : 1;
            }

            // Add complexity for properties
            const propertyCount = Object.keys(resourceData.Properties || {}).length;
            score += propertyCount * 0.1;
        }

        return Math.round(score * 100) / 100;
    }
}

/**
 * Template complexity interface
 */
export interface TemplateComplexity {
    resourceCount: number;
    outputCount: number;
    parameterCount: number;
    uniqueResourceTypes: number;
    templateSize: number;
    complexity: number;
}

/**
 * Mock AWS service responses for testing
 */
export class MockAWSServices {
    /**
     * Mock Route 53 hosted zone lookup
     */
    static mockHostedZoneLookup(hostedZoneId: string, domainName: string) {
        return {
            Id: hostedZoneId,
            Name: domainName,
            CallerReference: 'mock-caller-reference',
            Config: {
                PrivateZone: false
            }
        };
    }

    /**
     * Mock VPC lookup
     */
    static mockVpcLookup(vpcId: string, cidr: string) {
        return {
            VpcId: vpcId,
            CidrBlock: cidr,
            State: 'available',
            Tags: [
                { Key: 'Name', Value: 'mock-vpc' }
            ]
        };
    }

    /**
     * Mock availability zones
     */
    static mockAvailabilityZones(region: string, count: number = 3) {
        return Array.from({ length: count }, (_, i) => ({
            ZoneName: `${region}${String.fromCharCode(97 + i)}`, // a, b, c, etc.
            ZoneId: `${region}-az${i + 1}`,
            State: 'available'
        }));
    }
}

/**
 * Test assertion helpers
 */
export class TestAssertions {
    /**
     * Assert that template has expected resource count
     */
    static assertResourceCount(template: Template, resourceType: string, expectedCount: number) {
        const helper = new TemplateHelper(template);
        const actualCount = helper.getResourceCount(resourceType);

        if (actualCount !== expectedCount) {
            throw new Error(
                `Expected ${expectedCount} resources of type ${resourceType}, but found ${actualCount}`
            );
        }
    }

    /**
     * Assert that all resources have required tags
     */
    static assertRequiredTags(template: Template, requiredTags: string[]) {
        const helper = new TemplateHelper(template);
        const validation = helper.validateResourceTags(requiredTags);

        if (!validation.valid) {
            const violations = validation.violations.map(v =>
                `${v.logicalId} (${v.resourceType}) missing tag: ${v.missingTag}`
            ).join('\n');

            throw new Error(`Resources missing required tags:\n${violations}`);
        }
    }

    /**
     * Assert that security groups follow best practices
     */
    static assertSecurityGroupBestPractices(template: Template) {
        const helper = new TemplateHelper(template);
        const validation = helper.validateSecurityGroups();

        if (!validation.valid) {
            const issues = validation.issues.map(i =>
                `${i.logicalId}: ${i.issue} - ${i.details}`
            ).join('\n');

            throw new Error(`Security group violations:\n${issues}`);
        }
    }

    /**
     * Assert that encryption is properly configured
     */
    static assertEncryptionCompliance(template: Template) {
        const helper = new TemplateHelper(template);
        const validation = helper.validateEncryption();

        if (!validation.valid) {
            const issues = validation.issues.map(i =>
                `${i.logicalId} (${i.resourceType}): ${i.issue}`
            ).join('\n');

            throw new Error(`Encryption compliance violations:\n${issues}`);
        }
    }
}