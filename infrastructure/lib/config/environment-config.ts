import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';

/**
 * Environment-specific configuration interface
 */
export interface EnvironmentConfig {
    // Basic environment info
    environment: string;
    region: string;
    projectName: string;

    // Network configuration
    vpcCidr: string;
    natGateways: number;
    availabilityZones: number;

    // Compute configuration
    eksNodeType: string;
    eksMinNodes: number;
    eksMaxNodes: number;

    // Database configuration
    rdsInstanceType: string;
    rdsStorage: number;
    rdsMultiAz: boolean;
    rdsBackupRetention: number;

    // MSK configuration
    mskInstanceType: string;
    mskBrokers: number;

    // Cost optimization
    useSpotInstances: boolean;
    useReservedInstances: boolean;

    // Retention policies
    logRetentionDays: number;
    metricsRetentionDays: number;
    backupRetentionDays: number;

    // Resource naming
    resourcePrefix: string;

    // Observability
    traceSamplingRate: number;
    opensearchInstanceType: string;
    opensearchInstanceCount: number;
}

/**
 * Cost optimization configuration
 */
export interface CostOptimizationConfig {
    spotInstances: boolean;
    reservedInstances: boolean;
    rightSizing: boolean;
    scheduledScaling: boolean;
}

/**
 * Backup and retention configuration
 */
export interface BackupRetentionConfig {
    rdsBackupRetention: number;
    logRetention: number;
    metricsRetention: number;
    snapshotRetention: number;
}

/**
 * Resource naming configuration
 */
export interface ResourceNamingConfig {
    projectName: string;
    environment: string;
    region: string;
    prefix: string;
}

/**
 * Configuration manager for environment-specific settings
 */
export class EnvironmentConfigManager {
    private readonly scope: Construct;
    private readonly config: EnvironmentConfig;

    constructor(scope: Construct) {
        this.scope = scope;
        this.config = this.loadConfiguration();
    }

    /**
     * Load configuration from CDK context
     */
    private loadConfiguration(): EnvironmentConfig {
        const environment = this.scope.node.tryGetContext('genai-demo:environment') || 'development';
        const projectName = this.scope.node.tryGetContext('genai-demo:project-name') || 'genai-demo';
        const regionsConfig = this.scope.node.tryGetContext('genai-demo:regions') || {};
        const region = regionsConfig.primary || 'ap-east-2';

        // Get environment-specific configuration
        const envConfig = this.scope.node.tryGetContext('genai-demo:environments')?.[environment] || {};
        const networkingConfig = this.scope.node.tryGetContext('genai-demo:networking') || {};
        const observabilityConfig = this.scope.node.tryGetContext('genai-demo:observability') || {};
        const regionConfig = regionsConfig.regions?.[region] || {};

        return {
            // Basic environment info
            environment,
            region,
            projectName,

            // Network configuration
            vpcCidr: envConfig['vpc-cidr'] || this.getDefaultVpcCidr(environment),
            natGateways: envConfig['nat-gateways'] || this.getDefaultNatGateways(environment),
            availabilityZones: networkingConfig['availability-zones'] || 3,

            // Compute configuration
            eksNodeType: envConfig['eks-node-type'] || this.getDefaultEksNodeType(environment),
            eksMinNodes: envConfig['eks-min-nodes'] || this.getDefaultEksMinNodes(environment),
            eksMaxNodes: envConfig['eks-max-nodes'] || this.getDefaultEksMaxNodes(environment),

            // Database configuration
            rdsInstanceType: envConfig['rds-instance-type'] || this.getDefaultRdsInstanceType(environment),
            rdsStorage: envConfig['rds-storage'] || this.getDefaultRdsStorage(environment),
            rdsMultiAz: envConfig['rds-multi-az'] ?? this.getDefaultRdsMultiAz(environment),
            rdsBackupRetention: regionConfig['backup-retention']?.rds || this.getDefaultBackupRetention(environment),

            // MSK configuration
            mskInstanceType: envConfig['msk-instance-type'] || this.getDefaultMskInstanceType(environment),
            mskBrokers: envConfig['msk-brokers'] || this.getDefaultMskBrokers(environment),

            // Cost optimization
            useSpotInstances: regionConfig['cost-optimization']?.['spot-instances'] ?? this.getDefaultUseSpotInstances(environment),
            useReservedInstances: regionConfig['cost-optimization']?.['reserved-instances'] ?? this.getDefaultUseReservedInstances(environment),

            // Retention policies
            logRetentionDays: observabilityConfig['log-retention-days'] || this.getDefaultLogRetention(environment),
            metricsRetentionDays: observabilityConfig['metrics-retention-days'] || this.getDefaultMetricsRetention(environment),
            backupRetentionDays: regionConfig['backup-retention']?.logs || this.getDefaultBackupRetention(environment),

            // Resource naming
            resourcePrefix: this.generateResourcePrefix(projectName, environment, region),

            // Observability
            traceSamplingRate: observabilityConfig['trace-sampling-rate'] || 0.1,
            opensearchInstanceType: observabilityConfig['opensearch-instance-type'] || 't3.small.search',
            opensearchInstanceCount: observabilityConfig['opensearch-instance-count'] || 1
        };
    }

    /**
     * Get the loaded configuration
     */
    public getConfig(): EnvironmentConfig {
        return this.config;
    }

    /**
     * Get cost optimization configuration
     */
    public getCostOptimizationConfig(): CostOptimizationConfig {
        return {
            spotInstances: this.config.useSpotInstances,
            reservedInstances: this.config.useReservedInstances,
            rightSizing: this.config.environment === 'production',
            scheduledScaling: this.config.environment !== 'development'
        };
    }

    /**
     * Get backup and retention configuration
     */
    public getBackupRetentionConfig(): BackupRetentionConfig {
        return {
            rdsBackupRetention: this.config.rdsBackupRetention,
            logRetention: this.config.logRetentionDays,
            metricsRetention: this.config.metricsRetentionDays,
            snapshotRetention: this.config.backupRetentionDays
        };
    }

    /**
     * Get resource naming configuration
     */
    public getResourceNamingConfig(): ResourceNamingConfig {
        return {
            projectName: this.config.projectName,
            environment: this.config.environment,
            region: this.config.region,
            prefix: this.config.resourcePrefix
        };
    }

    /**
     * Generate standardized resource name
     */
    public generateResourceName(resourceType: string, suffix?: string): string {
        const parts = [this.config.resourcePrefix, resourceType];
        if (suffix) {
            parts.push(suffix);
        }
        return parts.join('-').toLowerCase();
    }

    /**
     * Generate standardized resource tags
     */
    public generateResourceTags(additionalTags?: Record<string, string>): Record<string, string> {
        const baseTags = {
            Project: this.config.projectName,
            Environment: this.config.environment,
            Region: this.config.region,
            ManagedBy: 'AWS-CDK',
            CostCenter: `${this.config.projectName}-${this.config.environment}`,
            Owner: 'DevOps-Team',
            CreatedBy: 'CDK-Infrastructure'
        };

        return { ...baseTags, ...additionalTags };
    }

    /**
     * Apply tags to a construct
     */
    public applyTags(construct: Construct, additionalTags?: Record<string, string>): void {
        const tags = this.generateResourceTags(additionalTags);
        Object.entries(tags).forEach(([key, value]) => {
            cdk.Tags.of(construct).add(key, value);
        });
    }

    /**
     * Get removal policy based on environment
     */
    public getRemovalPolicy(): cdk.RemovalPolicy {
        return this.config.environment === 'production'
            ? cdk.RemovalPolicy.RETAIN
            : cdk.RemovalPolicy.DESTROY;
    }

    /**
     * Get deletion protection based on environment
     */
    public getDeletionProtection(): boolean {
        return this.config.environment === 'production';
    }

    // Default value methods
    private getDefaultVpcCidr(environment: string): string {
        const cidrMap: Record<string, string> = {
            'development': '10.0.0.0/16',
            'staging': '10.1.0.0/16',
            'production': '10.2.0.0/16',
            'production-dr': '10.3.0.0/16'
        };
        return cidrMap[environment] || '10.0.0.0/16';
    }

    private getDefaultNatGateways(environment: string): number {
        return environment === 'production' ? 3 : 1;
    }

    private getDefaultEksNodeType(environment: string): string {
        const nodeTypeMap: Record<string, string> = {
            'development': 't3.medium',
            'staging': 't3.large',
            'production': 'm6g.large',
            'production-dr': 'm6g.large'
        };
        return nodeTypeMap[environment] || 't3.medium';
    }

    private getDefaultEksMinNodes(environment: string): number {
        return environment === 'production' ? 2 : 1;
    }

    private getDefaultEksMaxNodes(environment: string): number {
        const maxNodesMap: Record<string, number> = {
            'development': 3,
            'staging': 5,
            'production': 10,
            'production-dr': 8
        };
        return maxNodesMap[environment] || 3;
    }

    private getDefaultRdsInstanceType(environment: string): string {
        const instanceTypeMap: Record<string, string> = {
            'development': 'db.t3.micro',
            'staging': 'db.t3.small',
            'production': 'db.r6g.large',
            'production-dr': 'db.r6g.large'
        };
        return instanceTypeMap[environment] || 'db.t3.micro';
    }

    private getDefaultRdsStorage(environment: string): number {
        const storageMap: Record<string, number> = {
            'development': 20,
            'staging': 50,
            'production': 100,
            'production-dr': 100
        };
        return storageMap[environment] || 20;
    }

    private getDefaultRdsMultiAz(environment: string): boolean {
        return environment === 'production' || environment === 'production-dr' || environment === 'staging';
    }

    private getDefaultMskInstanceType(environment: string): string {
        const instanceTypeMap: Record<string, string> = {
            'development': 'kafka.t3.small',
            'staging': 'kafka.t3.small',
            'production': 'kafka.m5.large',
            'production-dr': 'kafka.m5.large'
        };
        return instanceTypeMap[environment] || 'kafka.t3.small';
    }

    private getDefaultMskBrokers(environment: string): number {
        const brokersMap: Record<string, number> = {
            'development': 1,
            'staging': 2,
            'production': 3,
            'production-dr': 3
        };
        return brokersMap[environment] || 1;
    }

    private getDefaultUseSpotInstances(environment: string): boolean {
        return environment === 'development';
    }

    private getDefaultUseReservedInstances(environment: string): boolean {
        return environment === 'production' || environment === 'production-dr';
    }

    private getDefaultLogRetention(environment: string): number {
        const retentionMap: Record<string, number> = {
            'development': 7,
            'staging': 14,
            'production': 30,
            'production-dr': 30
        };
        return retentionMap[environment] || 7;
    }

    private getDefaultMetricsRetention(environment: string): number {
        const retentionMap: Record<string, number> = {
            'development': 7,
            'staging': 30,
            'production': 90,
            'production-dr': 90
        };
        return retentionMap[environment] || 7;
    }

    private getDefaultBackupRetention(environment: string): number {
        const retentionMap: Record<string, number> = {
            'development': 7,
            'staging': 14,
            'production': 30,
            'production-dr': 30
        };
        return retentionMap[environment] || 7;
    }

    private generateResourcePrefix(projectName: string, environment: string, region: string): string {
        // Convert region to short form for naming
        const regionShort = this.getRegionShortName(region);
        return `${projectName}-${environment}-${regionShort}`;
    }

    private getRegionShortName(region: string): string {
        const regionMap: Record<string, string> = {
            'ap-east-2': 'ape2',
            'ap-northeast-1': 'apne1',
            'us-east-1': 'use1',
            'us-west-2': 'usw2',
            'eu-west-1': 'euw1'
        };
        return regionMap[region] || region.replace(/-/g, '');
    }
}