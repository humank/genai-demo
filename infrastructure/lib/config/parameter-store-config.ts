import * as cdk from 'aws-cdk-lib';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as ssm from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';
import { EnvironmentConfigManager } from './environment-config';

/**
 * Parameter Store configuration interface
 */
export interface ParameterStoreConfig {
    parameterPrefix: string;
    encryptionKey?: string;
    allowedPrincipals: string[];
}

/**
 * Runtime configuration parameters
 */
export interface RuntimeConfigParameters {
    // Database configuration
    databaseUrl: string;
    databaseUsername: string;
    databasePassword: string;

    // Kafka configuration
    kafkaBrokers: string;
    kafkaTopicPrefix: string;

    // Observability configuration
    logLevel: string;
    metricsEnabled: boolean;
    tracingEnabled: boolean;

    // Feature flags
    featureFlags: Record<string, boolean>;

    // External service endpoints
    externalServices: Record<string, string>;
}

/**
 * AWS Systems Manager Parameter Store integration for runtime configuration
 */
export class ParameterStoreManager {
    private readonly scope: Construct;
    private readonly configManager: EnvironmentConfigManager;
    private readonly parameterPrefix: string;
    private readonly parameters: Map<string, ssm.StringParameter> = new Map();

    constructor(scope: Construct, configManager: EnvironmentConfigManager) {
        this.scope = scope;
        this.configManager = configManager;

        const config = configManager.getConfig();
        this.parameterPrefix = `/genai-demo/${config.environment}/${config.region}`;
    }

    /**
     * Create all runtime configuration parameters
     */
    public createRuntimeParameters(): void {
        const config = this.configManager.getConfig();

        // Database configuration parameters
        this.createDatabaseParameters();

        // Kafka configuration parameters
        this.createKafkaParameters();

        // Observability configuration parameters
        this.createObservabilityParameters();

        // Feature flags parameters
        this.createFeatureFlagsParameters();

        // External services parameters
        this.createExternalServicesParameters();

        // Environment-specific parameters
        this.createEnvironmentParameters();
    }

    /**
     * Create database configuration parameters
     */
    private createDatabaseParameters(): void {
        const config = this.configManager.getConfig();

        // Database connection configuration
        this.createParameter('database/host', 'Database host endpoint', 'String');
        this.createParameter('database/port', '5432', 'String');
        this.createParameter('database/name', `${config.projectName}_${config.environment}`, 'String');
        this.createParameter('database/username', `${config.projectName}_user`, 'String');

        // Database connection pool settings
        this.createParameter('database/pool/min-size', '5', 'String');
        this.createParameter('database/pool/max-size', config.environment === 'production' ? '20' : '10', 'String');
        this.createParameter('database/pool/timeout', '30000', 'String');

        // Database migration settings
        this.createParameter('database/migration/enabled', 'true', 'String');
        this.createParameter('database/migration/validate-on-migrate', 'true', 'String');
    }

    /**
     * Create Kafka configuration parameters
     */
    private createKafkaParameters(): void {
        const config = this.configManager.getConfig();

        // Kafka broker configuration
        this.createParameter('kafka/brokers', 'MSK broker endpoints will be set after deployment', 'String');
        this.createParameter('kafka/topic-prefix', `${config.projectName}.${config.environment}`, 'String');

        // Kafka producer settings
        this.createParameter('kafka/producer/acks', 'all', 'String');
        this.createParameter('kafka/producer/retries', '3', 'String');
        this.createParameter('kafka/producer/batch-size', '16384', 'String');
        this.createParameter('kafka/producer/linger-ms', '5', 'String');

        // Kafka consumer settings
        this.createParameter('kafka/consumer/group-id', `${config.projectName}-${config.environment}`, 'String');
        this.createParameter('kafka/consumer/auto-offset-reset', 'earliest', 'String');
        this.createParameter('kafka/consumer/enable-auto-commit', 'false', 'String');

        // Kafka topic configuration
        this.createParameter('kafka/topics/customer-events', `${config.projectName}.${config.environment}.customer`, 'String');
        this.createParameter('kafka/topics/order-events', `${config.projectName}.${config.environment}.order`, 'String');
        this.createParameter('kafka/topics/payment-events', `${config.projectName}.${config.environment}.payment`, 'String');
        this.createParameter('kafka/topics/inventory-events', `${config.projectName}.${config.environment}.inventory`, 'String');
    }

    /**
     * Create observability configuration parameters
     */
    private createObservabilityParameters(): void {
        const config = this.configManager.getConfig();

        // Logging configuration
        this.createParameter('logging/level/root', config.environment === 'production' ? 'INFO' : 'DEBUG', 'String');
        this.createParameter('logging/level/application', config.environment === 'production' ? 'INFO' : 'DEBUG', 'String');
        this.createParameter('logging/level/sql', config.environment === 'production' ? 'WARN' : 'DEBUG', 'String');
        this.createParameter('logging/pattern', '%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n', 'String');

        // Metrics configuration
        this.createParameter('metrics/enabled', 'true', 'String');
        this.createParameter('metrics/export/cloudwatch/enabled', 'true', 'String');
        this.createParameter('metrics/export/prometheus/enabled', 'true', 'String');
        this.createParameter('metrics/sampling/rate', config.traceSamplingRate.toString(), 'String');

        // Tracing configuration
        this.createParameter('tracing/enabled', 'true', 'String');
        this.createParameter('tracing/sampling/rate', config.traceSamplingRate.toString(), 'String');
        this.createParameter('tracing/export/xray/enabled', config.environment === 'production' ? 'true' : 'false', 'String');
        this.createParameter('tracing/export/jaeger/enabled', config.environment !== 'production' ? 'true' : 'false', 'String');

        // Health check configuration
        this.createParameter('health/check/database/enabled', 'true', 'String');
        this.createParameter('health/check/kafka/enabled', 'true', 'String');
        this.createParameter('health/check/external-services/enabled', 'true', 'String');
    }

    /**
     * Create feature flags parameters
     */
    private createFeatureFlagsParameters(): void {
        const config = this.configManager.getConfig();

        // Core feature flags
        this.createParameter('features/domain-events/enabled', 'true', 'String');
        this.createParameter('features/event-sourcing/enabled', 'false', 'String');
        this.createParameter('features/cqrs/enabled', 'true', 'String');

        // Business feature flags
        this.createParameter('features/loyalty-program/enabled', 'true', 'String');
        this.createParameter('features/promotions/enabled', 'true', 'String');
        this.createParameter('features/recommendations/enabled', config.environment !== 'development' ? 'true' : 'false', 'String');

        // Observability feature flags
        this.createParameter('features/distributed-tracing/enabled', 'true', 'String');
        this.createParameter('features/performance-monitoring/enabled', 'true', 'String');
        this.createParameter('features/error-tracking/enabled', 'true', 'String');

        // Security feature flags
        this.createParameter('features/audit-logging/enabled', config.environment === 'production' ? 'true' : 'false', 'String');
        this.createParameter('features/data-encryption/enabled', config.environment === 'production' ? 'true' : 'false', 'String');
    }

    /**
     * Create external services parameters
     */
    private createExternalServicesParameters(): void {
        const config = this.configManager.getConfig();

        // Payment service configuration
        this.createParameter('external/payment-service/url', 'https://api.payment-provider.com', 'String');
        this.createParameter('external/payment-service/timeout', '30000', 'String');
        this.createParameter('external/payment-service/retry-attempts', '3', 'String');

        // Notification service configuration
        this.createParameter('external/notification-service/url', 'https://api.notification-provider.com', 'String');
        this.createParameter('external/notification-service/timeout', '15000', 'String');

        // Analytics service configuration
        this.createParameter('external/analytics-service/url', 'https://api.analytics-provider.com', 'String');
        this.createParameter('external/analytics-service/enabled', config.environment !== 'development' ? 'true' : 'false', 'String');
    }

    /**
     * Create environment-specific parameters
     */
    private createEnvironmentParameters(): void {
        const config = this.configManager.getConfig();

        // Environment metadata
        this.createParameter('environment/name', config.environment, 'String');
        this.createParameter('environment/region', config.region, 'String');
        this.createParameter('environment/project', config.projectName, 'String');

        // Resource configuration
        this.createParameter('resources/eks/node-type', config.eksNodeType, 'String');
        this.createParameter('resources/eks/min-nodes', config.eksMinNodes.toString(), 'String');
        this.createParameter('resources/eks/max-nodes', config.eksMaxNodes.toString(), 'String');

        // Cost optimization settings
        this.createParameter('cost-optimization/spot-instances', config.useSpotInstances.toString(), 'String');
        this.createParameter('cost-optimization/reserved-instances', config.useReservedInstances.toString(), 'String');

        // Backup and retention settings
        this.createParameter('backup/retention/logs', config.logRetentionDays.toString(), 'String');
        this.createParameter('backup/retention/metrics', config.metricsRetentionDays.toString(), 'String');
        this.createParameter('backup/retention/database', config.rdsBackupRetention.toString(), 'String');
    }

    /**
     * Create a parameter in Parameter Store
     */
    private createParameter(
        name: string,
        value: string,
        type: 'String' | 'StringList' | 'SecureString' = 'String',
        description?: string
    ): ssm.StringParameter {
        const fullName = `${this.parameterPrefix}/${name}`;

        const parameter = new ssm.StringParameter(this.scope, `Parameter-${name.replace(/\//g, '-')}`, {
            parameterName: fullName,
            stringValue: value,
            description: description || `Runtime configuration parameter: ${name}`,
            tier: ssm.ParameterTier.STANDARD,
            allowedPattern: '.*'
        });

        // Apply tags
        this.configManager.applyTags(parameter, {
            ParameterType: 'RuntimeConfiguration',
            ParameterCategory: this.getParameterCategory(name)
        });

        this.parameters.set(name, parameter);
        return parameter;
    }

    /**
     * Get parameter category for tagging
     */
    private getParameterCategory(name: string): string {
        if (name.startsWith('database/')) return 'Database';
        if (name.startsWith('kafka/')) return 'Messaging';
        if (name.startsWith('logging/') || name.startsWith('metrics/') || name.startsWith('tracing/')) return 'Observability';
        if (name.startsWith('features/')) return 'FeatureFlags';
        if (name.startsWith('external/')) return 'ExternalServices';
        if (name.startsWith('environment/')) return 'Environment';
        if (name.startsWith('resources/')) return 'Resources';
        if (name.startsWith('cost-optimization/')) return 'CostOptimization';
        if (name.startsWith('backup/')) return 'Backup';
        return 'General';
    }

    /**
     * Create IAM policy for parameter access
     */
    public createParameterAccessPolicy(): iam.PolicyDocument {
        const config = this.configManager.getConfig();

        return new iam.PolicyDocument({
            statements: [
                new iam.PolicyStatement({
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'ssm:GetParameter',
                        'ssm:GetParameters',
                        'ssm:GetParametersByPath'
                    ],
                    resources: [
                        `arn:aws:ssm:${config.region}:*:parameter${this.parameterPrefix}/*`
                    ]
                }),
                new iam.PolicyStatement({
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'ssm:DescribeParameters'
                    ],
                    resources: ['*'],
                    conditions: {
                        StringLike: {
                            'ssm:ParameterName': `${this.parameterPrefix}/*`
                        }
                    }
                })
            ]
        });
    }

    /**
     * Create IAM role for parameter access
     */
    public createParameterAccessRole(roleName?: string): iam.Role {
        const config = this.configManager.getConfig();
        const name = roleName || this.configManager.generateResourceName('parameter-access-role');

        const role = new iam.Role(this.scope, 'ParameterAccessRole', {
            roleName: name,
            assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
            inlinePolicies: {
                ParameterStoreAccess: this.createParameterAccessPolicy()
            },
            description: `IAM role for accessing Parameter Store configuration in ${config.environment} environment`
        });

        // Apply tags
        this.configManager.applyTags(role, {
            RoleType: 'ParameterStoreAccess'
        });

        return role;
    }

    /**
     * Get parameter ARN
     */
    public getParameterArn(name: string): string {
        const config = this.configManager.getConfig();
        return `arn:aws:ssm:${config.region}:*:parameter${this.parameterPrefix}/${name}`;
    }

    /**
     * Get all parameter ARNs
     */
    public getAllParameterArns(): string[] {
        return Array.from(this.parameters.keys()).map(name => this.getParameterArn(name));
    }

    /**
     * Create outputs for parameter access
     */
    public createParameterOutputs(): void {
        const config = this.configManager.getConfig();

        new cdk.CfnOutput(this.scope, 'ParameterStorePrefix', {
            value: this.parameterPrefix,
            description: 'Parameter Store prefix for runtime configuration',
            exportName: `${config.projectName}-${config.environment}-parameter-prefix`
        });

        new cdk.CfnOutput(this.scope, 'ParameterStoreRegion', {
            value: config.region,
            description: 'AWS region for Parameter Store',
            exportName: `${config.projectName}-${config.environment}-parameter-region`
        });

        const parameterAccessRole = this.createParameterAccessRole();

        new cdk.CfnOutput(this.scope, 'ParameterAccessPolicyArn', {
            value: parameterAccessRole.roleArn,
            description: 'IAM role ARN for Parameter Store access',
            exportName: `${config.projectName}-${config.environment}-parameter-access-role-arn`
        });
    }
}