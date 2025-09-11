/**
 * Jest setup file for CDK testing infrastructure
 * Configures global test environment and utilities
 */

import * as cdk from 'aws-cdk-lib';

// Global test configuration
beforeAll(() => {
    // Set default CDK context for all tests
    process.env.CDK_DEFAULT_ACCOUNT = '123456789012';
    process.env.CDK_DEFAULT_REGION = 'ap-northeast-1';
});

// Global test utilities
declare global {
    namespace jest {
        interface Matchers<R> {
            toHaveResource(resourceType: string, properties?: any): R;
            toHaveResourceCount(resourceType: string, count: number): R;
            toMatchSnapshot(): R;
        }
    }
}

// Custom Jest matchers for CDK testing
expect.extend({
    toHaveResource(template: any, resourceType: string, properties?: any) {
        const resources = template.toJSON().Resources || {};
        const matchingResources = Object.values(resources).filter(
            (resource: any) => resource.Type === resourceType
        );

        if (matchingResources.length === 0) {
            return {
                message: () => `Expected template to have resource of type ${resourceType}`,
                pass: false,
            };
        }

        if (properties) {
            const hasMatchingProperties = matchingResources.some((resource: any) =>
                this.equals(resource.Properties, expect.objectContaining(properties))
            );

            if (!hasMatchingProperties) {
                return {
                    message: () => `Expected template to have resource of type ${resourceType} with properties ${JSON.stringify(properties)}`,
                    pass: false,
                };
            }
        }

        return {
            message: () => `Expected template not to have resource of type ${resourceType}`,
            pass: true,
        };
    },

    toHaveResourceCount(template: any, resourceType: string, expectedCount: number) {
        const resources = template.toJSON().Resources || {};
        const matchingResources = Object.values(resources).filter(
            (resource: any) => resource.Type === resourceType
        );

        const actualCount = matchingResources.length;

        return {
            message: () => `Expected ${expectedCount} resources of type ${resourceType}, but found ${actualCount}`,
            pass: actualCount === expectedCount,
        };
    }
});

// Test data factories
export class TestDataFactory {
    static createTestApp(context?: Record<string, any>): cdk.App {
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
                ...context
            }
        });
    }

    static createTestStackProps(overrides?: Partial<any>): any {
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
}

// Test utilities
export class TestUtils {
    static getResourcesByType(template: any, resourceType: string): any[] {
        const resources = template.toJSON().Resources || {};
        return Object.entries(resources)
            .filter(([_, resource]: [string, any]) => resource.Type === resourceType)
            .map(([logicalId, resource]) => ({ logicalId, ...(resource as object) }));
    }

    static getOutputs(template: any): Record<string, any> {
        return template.toJSON().Outputs || {};
    }

    static getParameters(template: any): Record<string, any> {
        return template.toJSON().Parameters || {};
    }

    static hasTag(resource: any, key: string, value?: string): boolean {
        const tags = resource.Properties?.Tags || [];
        const tag = tags.find((t: any) => t.Key === key);
        return tag && (value === undefined || tag.Value === value);
    }
}