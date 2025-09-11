import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as eks from 'aws-cdk-lib/aws-eks';
import * as rds from 'aws-cdk-lib/aws-rds';
import { Construct } from 'constructs';
import { EnvironmentConfigManager } from './environment-config';

/**
 * Cost optimization configuration
 */
export interface CostOptimizationSettings {
    useSpotInstances: boolean;
    useReservedInstances: boolean;
    enableRightSizing: boolean;
    enableScheduledScaling: boolean;
    enableAutoShutdown: boolean;
    spotInstancePercentage: number;
    reservedInstancePercentage: number;
}

/**
 * Spot instance configuration
 */
export interface SpotInstanceConfig {
    maxPrice?: string;
    spotAllocationStrategy: 'lowest-price' | 'diversified' | 'capacity-optimized';
    onDemandBaseCapacity: number;
    onDemandPercentageAboveBaseCapacity: number;
    spotInstancePools: number;
}

/**
 * Scheduled scaling configuration
 */
export interface ScheduledScalingConfig {
    workingHoursStart: string; // HH:MM format
    workingHoursEnd: string;   // HH:MM format
    workingDaysOnly: boolean;
    timezone: string;
    scaleUpCapacity: number;
    scaleDownCapacity: number;
}

/**
 * Cost optimization manager for AWS resources
 */
export class CostOptimizationManager {
    private readonly scope: Construct;
    private readonly configManager: EnvironmentConfigManager;
    private readonly settings: CostOptimizationSettings;

    constructor(scope: Construct, configManager: EnvironmentConfigManager) {
        this.scope = scope;
        this.configManager = configManager;
        this.settings = this.loadCostOptimizationSettings();
    }

    /**
     * Load cost optimization settings from configuration
     */
    private loadCostOptimizationSettings(): CostOptimizationSettings {
        const config = this.configManager.getConfig();
        const costConfig = this.configManager.getCostOptimizationConfig();

        return {
            useSpotInstances: costConfig.spotInstances,
            useReservedInstances: costConfig.reservedInstances,
            enableRightSizing: costConfig.rightSizing,
            enableScheduledScaling: costConfig.scheduledScaling,
            enableAutoShutdown: config.environment === 'development',
            spotInstancePercentage: this.getSpotInstancePercentage(config.environment),
            reservedInstancePercentage: this.getReservedInstancePercentage(config.environment)
        };
    }

    /**
     * Get spot instance percentage based on environment
     */
    private getSpotInstancePercentage(environment: string): number {
        const percentageMap: Record<string, number> = {
            'development': 80,
            'staging': 50,
            'production': 0,
            'production-dr': 0
        };
        return percentageMap[environment] || 0;
    }

    /**
     * Get reserved instance percentage based on environment
     */
    private getReservedInstancePercentage(environment: string): number {
        const percentageMap: Record<string, number> = {
            'development': 0,
            'staging': 20,
            'production': 70,
            'production-dr': 70
        };
        return percentageMap[environment] || 0;
    }

    /**
     * Configure EKS node group with cost optimization
     */
    public configureEksNodeGroup(
        cluster: eks.Cluster,
        nodeGroupName: string,
        options?: Partial<eks.NodegroupOptions>
    ): eks.Nodegroup {
        const config = this.configManager.getConfig();

        const nodeGroupOptions: eks.NodegroupOptions = {
            nodegroupName: nodeGroupName,
            instanceTypes: this.getOptimizedInstanceTypes(config.eksNodeType),
            minSize: config.eksMinNodes,
            maxSize: config.eksMaxNodes,
            desiredSize: config.eksMinNodes,

            // Cost optimization settings
            capacityType: this.settings.useSpotInstances
                ? eks.CapacityType.SPOT
                : eks.CapacityType.ON_DEMAND,

            // Spot instance configuration
            ...(this.settings.useSpotInstances && {
                capacityType: eks.CapacityType.SPOT,
                // Use multiple instance types for better spot availability
                instanceTypes: this.getSpotInstanceTypes(config.eksNodeType)
            }),

            // Tagging for cost allocation
            tags: this.configManager.generateResourceTags({
                CostOptimization: this.settings.useSpotInstances ? 'Spot' : 'OnDemand',
                NodeGroupType: 'EKS-Worker'
            }),

            // Note: Update configuration would be handled at the EKS cluster level

            // Override options
            ...options
        };

        const nodeGroup = cluster.addNodegroupCapacity(nodeGroupName, nodeGroupOptions);

        // Configure auto-scaling if enabled
        if (this.settings.enableScheduledScaling) {
            this.configureScheduledScaling(nodeGroup);
        }

        return nodeGroup;
    }

    /**
     * Get mixed instance policy configuration
     * Note: This returns configuration data that can be used to create a mixed instance policy
     */
    public getMixedInstancePolicyConfig(): {
        onDemandBaseCapacity: number;
        onDemandPercentageAboveBaseCapacity: number;
        spotAllocationStrategy: string;
        spotInstancePools: number;
        spotMaxPrice: string;
    } {
        const config = this.configManager.getConfig();

        return {
            onDemandBaseCapacity: this.getOnDemandBaseCapacity(),
            onDemandPercentageAboveBaseCapacity: 100 - this.settings.spotInstancePercentage,
            spotAllocationStrategy: 'capacity-optimized',
            spotInstancePools: 3,
            spotMaxPrice: this.getSpotMaxPrice(config.eksNodeType)
        };
    }

    /**
     * Get optimized instance types for cost efficiency
     */
    private getOptimizedInstanceTypes(baseInstanceType: string): ec2.InstanceType[] {
        const instanceFamily = baseInstanceType.split('.')[0];
        const instanceSize = baseInstanceType.split('.')[1];

        // For cost optimization, include multiple instance types in the same family
        const instanceTypes = [
            ec2.InstanceType.of(this.getInstanceClass(instanceFamily), this.getInstanceSize(instanceSize))
        ];

        // Add alternative instance types for better spot availability
        if (this.settings.useSpotInstances) {
            const spotTypes = this.getSpotInstanceTypes(baseInstanceType);
            instanceTypes.push(...spotTypes.slice(1, 3)); // Add 2 more types
        }

        return instanceTypes;
    }

    /**
     * Get spot instance types for better availability
     */
    private getSpotInstanceTypes(baseInstanceType: string): ec2.InstanceType[] {
        const instanceFamily = baseInstanceType.split('.')[0];
        const instanceSize = baseInstanceType.split('.')[1];

        // Include multiple instance families for better spot availability
        const spotInstanceTypes: ec2.InstanceType[] = [];

        // Current generation instances
        const families = this.getSpotInstanceFamilies(instanceFamily);
        const sizes = this.getSpotInstanceSizes(instanceSize);

        families.forEach(family => {
            sizes.forEach(size => {
                spotInstanceTypes.push(
                    ec2.InstanceType.of(family, size)
                );
            });
        });

        return spotInstanceTypes.slice(0, 5); // Limit to 5 types for better management
    }

    /**
     * Get alternative instance families for spot instances
     */
    private getSpotInstanceFamilies(baseFamily: string): ec2.InstanceClass[] {
        const familyMap: Record<string, ec2.InstanceClass[]> = {
            't3': [ec2.InstanceClass.T3, ec2.InstanceClass.T3A, ec2.InstanceClass.T2],
            't3a': [ec2.InstanceClass.T3A, ec2.InstanceClass.T3, ec2.InstanceClass.T2],
            'm5': [ec2.InstanceClass.M5, ec2.InstanceClass.M5A, ec2.InstanceClass.M4],
            'm5a': [ec2.InstanceClass.M5A, ec2.InstanceClass.M5, ec2.InstanceClass.M4],
            'm6g': [ec2.InstanceClass.M6G, ec2.InstanceClass.M5, ec2.InstanceClass.M5A],
            'c5': [ec2.InstanceClass.C5, ec2.InstanceClass.C5A, ec2.InstanceClass.C4],
            'r5': [ec2.InstanceClass.R5, ec2.InstanceClass.R5A, ec2.InstanceClass.R4]
        };

        return familyMap[baseFamily] || [this.getInstanceClass(baseFamily)];
    }

    /**
     * Get alternative instance sizes for spot instances
     */
    private getSpotInstanceSizes(baseSize: string): ec2.InstanceSize[] {
        const sizeMap: Record<string, ec2.InstanceSize[]> = {
            'micro': [ec2.InstanceSize.MICRO, ec2.InstanceSize.SMALL],
            'small': [ec2.InstanceSize.SMALL, ec2.InstanceSize.MEDIUM, ec2.InstanceSize.MICRO],
            'medium': [ec2.InstanceSize.MEDIUM, ec2.InstanceSize.LARGE, ec2.InstanceSize.SMALL],
            'large': [ec2.InstanceSize.LARGE, ec2.InstanceSize.XLARGE, ec2.InstanceSize.MEDIUM],
            'xlarge': [ec2.InstanceSize.XLARGE, ec2.InstanceSize.LARGE, ec2.InstanceSize.XLARGE2]
        };

        return sizeMap[baseSize] || [this.getInstanceSize(baseSize)];
    }

    /**
     * Configure scheduled scaling for development environments
     */
    private configureScheduledScaling(nodeGroup: eks.Nodegroup): void {
        const config = this.configManager.getConfig();

        if (config.environment !== 'development') {
            return; // Only apply to development environment
        }

        const scalingConfig: ScheduledScalingConfig = {
            workingHoursStart: '09:00',
            workingHoursEnd: '18:00',
            workingDaysOnly: true,
            timezone: 'Asia/Taipei',
            scaleUpCapacity: config.eksMinNodes,
            scaleDownCapacity: 0
        };

        // Note: Scheduled scaling would be implemented using AWS Application Auto Scaling
        // This is a placeholder for the configuration structure
        const scaleDownSchedule = {
            hour: '18',
            minute: '0',
            weekDay: '1-5' // Monday to Friday
        };

        const scaleUpSchedule = {
            hour: '9',
            minute: '0',
            weekDay: '1-5' // Monday to Friday
        };

        // Note: Actual scheduled scaling implementation would require additional setup
        // This is a placeholder for the configuration structure
    }

    /**
     * Configure RDS cost optimization
     */
    public configureRdsOptimization(
        database: rds.DatabaseInstance
    ): void {
        const config = this.configManager.getConfig();

        // Configure automated backup retention based on environment
        const backupRetention = this.configManager.getBackupRetentionConfig();

        // Add cost optimization tags
        this.configManager.applyTags(database, {
            CostOptimization: this.settings.useReservedInstances ? 'Reserved' : 'OnDemand',
            BackupRetention: backupRetention.rdsBackupRetention.toString(),
            DatabaseType: 'Primary'
        });

        // Configure performance insights based on environment
        if (config.environment === 'production') {
            // Enable performance insights for production (additional cost but valuable)
            // This would be configured in the RDS construct
        }
    }

    /**
     * Create cost monitoring alarms
     */
    public createCostMonitoringAlarms(): void {
        const config = this.configManager.getConfig();

        // Create billing alarm for the environment
        const monthlyBudget = this.getMonthlyBudget(config.environment);

        const billingAlarm = new cloudwatch.Alarm(this.scope, 'BillingAlarm', {
            alarmName: this.configManager.generateResourceName('billing-alarm'),
            alarmDescription: `Monthly billing alarm for ${config.environment} environment`,
            metric: new cloudwatch.Metric({
                namespace: 'AWS/Billing',
                metricName: 'EstimatedCharges',
                dimensionsMap: {
                    Currency: 'USD'
                },
                statistic: 'Maximum',
                period: cdk.Duration.hours(6)
            }),
            threshold: monthlyBudget,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            evaluationPeriods: 1,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
        });

        // Apply tags
        this.configManager.applyTags(billingAlarm, {
            AlarmType: 'CostMonitoring',
            Budget: monthlyBudget.toString()
        });
    }

    /**
     * Get monthly budget based on environment
     */
    private getMonthlyBudget(environment: string): number {
        const budgetMap: Record<string, number> = {
            'development': 100,   // $100/month
            'staging': 300,       // $300/month
            'production': 1000,   // $1000/month
            'production-dr': 800  // $800/month
        };
        return budgetMap[environment] || 100;
    }

    /**
     * Get on-demand base capacity
     */
    private getOnDemandBaseCapacity(): number {
        const config = this.configManager.getConfig();
        return config.environment === 'production' ? 2 : 0;
    }

    /**
     * Get spot max price
     */
    private getSpotMaxPrice(instanceType: string): string {
        // Set spot max price to 50% of on-demand price
        // This is a simplified calculation - in practice, you'd query AWS pricing API
        return '0.05'; // $0.05 per hour max for spot instances
    }

    /**
     * Get instance type overrides for mixed instance policy
     */
    private getInstanceTypeOverrides(baseInstanceType: string): string[] {
        const instanceTypes = this.getSpotInstanceTypes(baseInstanceType);

        return instanceTypes.map(instanceType => instanceType.toString());
    }

    /**
     * Helper method to convert string to InstanceClass
     */
    private getInstanceClass(family: string): ec2.InstanceClass {
        const classMap: Record<string, ec2.InstanceClass> = {
            't2': ec2.InstanceClass.T2,
            't3': ec2.InstanceClass.T3,
            't3a': ec2.InstanceClass.T3A,
            'm4': ec2.InstanceClass.M4,
            'm5': ec2.InstanceClass.M5,
            'm5a': ec2.InstanceClass.M5A,
            'm6g': ec2.InstanceClass.M6G,
            'c4': ec2.InstanceClass.C4,
            'c5': ec2.InstanceClass.C5,
            'c5a': ec2.InstanceClass.C5A,
            'r4': ec2.InstanceClass.R4,
            'r5': ec2.InstanceClass.R5,
            'r5a': ec2.InstanceClass.R5A
        };
        return classMap[family] || ec2.InstanceClass.T3;
    }

    /**
     * Helper method to convert string to InstanceSize
     */
    private getInstanceSize(size: string): ec2.InstanceSize {
        const sizeMap: Record<string, ec2.InstanceSize> = {
            'micro': ec2.InstanceSize.MICRO,
            'small': ec2.InstanceSize.SMALL,
            'medium': ec2.InstanceSize.MEDIUM,
            'large': ec2.InstanceSize.LARGE,
            'xlarge': ec2.InstanceSize.XLARGE,
            '2xlarge': ec2.InstanceSize.XLARGE2,
            '4xlarge': ec2.InstanceSize.XLARGE4,
            '8xlarge': ec2.InstanceSize.XLARGE8,
            '16xlarge': ec2.InstanceSize.XLARGE16
        };
        return sizeMap[size] || ec2.InstanceSize.MEDIUM;
    }

    /**
     * Get cost optimization settings
     */
    public getSettings(): CostOptimizationSettings {
        return this.settings;
    }

    /**
     * Create cost optimization outputs
     */
    public createCostOptimizationOutputs(): void {
        const config = this.configManager.getConfig();

        new cdk.CfnOutput(this.scope, 'CostOptimizationSpotInstances', {
            value: this.settings.useSpotInstances.toString(),
            description: 'Whether spot instances are enabled for cost optimization',
            exportName: `${config.projectName}-${config.environment}-spot-instances-enabled`
        });

        new cdk.CfnOutput(this.scope, 'CostOptimizationReservedInstances', {
            value: this.settings.useReservedInstances.toString(),
            description: 'Whether reserved instances are recommended for cost optimization',
            exportName: `${config.projectName}-${config.environment}-reserved-instances-recommended`
        });

        new cdk.CfnOutput(this.scope, 'CostOptimizationSpotPercentage', {
            value: this.settings.spotInstancePercentage.toString(),
            description: 'Percentage of spot instances in the fleet',
            exportName: `${config.projectName}-${config.environment}-spot-percentage`
        });

        new cdk.CfnOutput(this.scope, 'CostOptimizationMonthlyBudget', {
            value: this.getMonthlyBudget(config.environment).toString(),
            description: 'Monthly budget alert threshold in USD',
            exportName: `${config.projectName}-${config.environment}-monthly-budget`
        });
    }
}