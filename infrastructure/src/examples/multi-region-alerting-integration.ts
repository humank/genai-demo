import * as cdk from 'aws-cdk-lib';
import { AlertingStack, MultiRegionAlertingConfig } from '../stacks/alerting-stack';
// import { SLAMonitoringStack } from '../stacks/observability/sla-monitoring-stack'; // File is empty
import { ObservabilityStack } from '../stacks/observability-stack';

/**
 * Example integration of multi-region alerting and SLA monitoring
 * This demonstrates how to configure and deploy the enhanced alerting system
 */
export class MultiRegionAlertingIntegrationExample {
    
    /**
     * Example configuration for multi-region alerting
     */
    static getMultiRegionAlertingConfig(): MultiRegionAlertingConfig {
        return {
            enabled: true,
            regions: ['ap-east-2', 'ap-northeast-1', 'us-west-2'],
            primaryRegion: 'ap-east-2',
            crossRegionAggregation: true,
            alertDeduplication: {
                enabled: true,
                timeWindow: 15, // 15 minutes
                similarityThreshold: 0.8, // 80% similarity
            },
            escalationPolicy: {
                regionalFailureThreshold: 2, // Escalate when 2+ regions fail
                globalEscalationDelay: 5, // Wait 5 minutes before escalating
            },
        };
    }

    /**
     * Example deployment of enhanced alerting stack
     */
    static deployEnhancedAlertingStack(
        app: cdk.App,
        environment: string,
        multiRegionConfig: MultiRegionAlertingConfig
    ): AlertingStack {
        
        const alertingStack = new AlertingStack(app, `AlertingStack-${environment}`, {
            env: {
                account: process.env.CDK_DEFAULT_ACCOUNT,
                region: multiRegionConfig.primaryRegion,
            },
            environment: environment,
            region: multiRegionConfig.primaryRegion,
            applicationName: 'genai-demo',
            alertingConfig: {
                criticalAlerts: {
                    emailAddresses: ['devops@company.com', 'oncall@company.com'],
                    phoneNumbers: ['+1234567890'], // For critical alerts only
                    slackWebhookUrl: 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK',
                },
                warningAlerts: {
                    emailAddresses: ['devops@company.com'],
                    slackWebhookUrl: 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK',
                },
                infoAlerts: {
                    emailAddresses: ['monitoring@company.com'],
                },
            },
            multiRegionConfig: multiRegionConfig,
        });

        return alertingStack;
    }

    /**
     * Example deployment of SLA monitoring stack
     */
    // static deploySLAMonitoringStack(
    //     app: cdk.App,
    //     environment: string,
    //     multiRegionConfig: MultiRegionAlertingConfig,
    //     observabilityStack: ObservabilityStack
    // ): SLAMonitoringStack {
    //     
    //     const slaMonitoringStack = new SLAMonitoringStack(app, `SLAMonitoringStack-${environment}`, {
    //         env: {
    //             account: process.env.CDK_DEFAULT_ACCOUNT,
    //             region: multiRegionConfig.primaryRegion,
    //         },
    //         environment: environment,
    //         multiRegionConfig: {
    //             enabled: multiRegionConfig.enabled,
    //             regions: multiRegionConfig.regions,
    //             primaryRegion: multiRegionConfig.primaryRegion,
    //             crossRegionReplication: multiRegionConfig.crossRegionAggregation,
    //         },
    //         dashboard: observabilityStack.dashboard,
    //     });

    //     return slaMonitoringStack;
    // }

    /**
     * Complete multi-region monitoring setup example
     */
    static setupCompleteMultiRegionMonitoring(app: cdk.App, environment: string): {
        alertingStack: AlertingStack;
        // slaMonitoringStack: SLAMonitoringStack;
        observabilityStack: ObservabilityStack;
    } {
        
        // Get multi-region configuration
        const multiRegionConfig = this.getMultiRegionAlertingConfig();

        // Deploy observability stack first (dependency)
        const observabilityStack = new ObservabilityStack(app, `ObservabilityStack-${environment}`, {
            env: {
                account: process.env.CDK_DEFAULT_ACCOUNT,
                region: multiRegionConfig.primaryRegion,
            },
            vpc: undefined as any, // Would be provided by VPC stack
            kmsKey: undefined as any, // Would be provided by KMS stack
            environment: environment,
            multiRegionConfig: {
                enabled: multiRegionConfig.enabled,
                regions: multiRegionConfig.regions,
                primaryRegion: multiRegionConfig.primaryRegion,
                crossRegionReplication: multiRegionConfig.crossRegionAggregation,
            },
        });

        // Deploy enhanced alerting stack
        const alertingStack = this.deployEnhancedAlertingStack(
            app, 
            environment, 
            multiRegionConfig
        );

        // Deploy SLA monitoring stack
        // const slaMonitoringStack = this.deploySLAMonitoringStack(
        //     app,
        //     environment,
        //     multiRegionConfig,
        //     observabilityStack
        // );

        // Add dependencies
        // slaMonitoringStack.addDependency(observabilityStack);
        alertingStack.addDependency(observabilityStack);

        return {
            alertingStack,
            // slaMonitoringStack,
            observabilityStack,
        };
    }
}

/**
 * Usage example in main CDK app
 */
export function createMultiRegionMonitoringExample() {
    const app = new cdk.App();
    
    // Set up complete multi-region monitoring for production
    const productionMonitoring = MultiRegionAlertingIntegrationExample
        .setupCompleteMultiRegionMonitoring(app, 'production');

    // Set up for staging with different configuration
    const stagingConfig: MultiRegionAlertingConfig = {
        enabled: true,
        regions: ['ap-east-2', 'ap-northeast-1'], // Fewer regions for staging
        primaryRegion: 'ap-east-2',
        crossRegionAggregation: true,
        alertDeduplication: {
            enabled: true,
            timeWindow: 30, // Longer window for staging
            similarityThreshold: 0.7, // Lower threshold for staging
        },
        escalationPolicy: {
            regionalFailureThreshold: 1, // More sensitive for staging
            globalEscalationDelay: 2, // Faster escalation for staging
        },
    };

    const stagingAlertingStack = MultiRegionAlertingIntegrationExample
        .deployEnhancedAlertingStack(app, 'staging', stagingConfig);

    return app;
}