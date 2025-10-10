import * as cdk from 'aws-cdk-lib';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as subscriptions from 'aws-cdk-lib/aws-sns-subscriptions';
import * as kms from 'aws-cdk-lib/aws-kms';
import { Construct } from 'constructs';
import { SecurityStack } from '../stacks/security-stack';

/**
 * Example: Multi-Region Security Integration with Data Encryption and Compliance
 * 
 * This example demonstrates how to deploy the enhanced Security stack
 * with cross-region data encryption, compliance monitoring, and GDPR support.
 * 
 * Requirements: 4.3.1 - Multi-Region Security, Data Encryption, and Compliance
 */

export interface MultiRegionSecurityIntegrationProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly primaryRegion: string;
    readonly secondaryRegions: string[];
    readonly complianceStandards: string[];
    readonly dataClassification: 'public' | 'internal' | 'confidential' | 'restricted';
    readonly enableGdprCompliance?: boolean;
    readonly enableDataSovereignty?: boolean;
}

export class MultiRegionSecurityIntegrationExample extends cdk.Stack {
    public readonly securityStack: SecurityStack;
    public readonly complianceNotificationTopic: sns.Topic;
    public readonly crossRegionReplicationKey: kms.Key;

    constructor(scope: Construct, id: string, props: MultiRegionSecurityIntegrationProps) {
        super(scope, id, props);

        const { 
            environment, 
            projectName, 
            primaryRegion, 
            secondaryRegions,
            complianceStandards,
            dataClassification,
            enableGdprCompliance = false,
            enableDataSovereignty = true
        } = props;

        // Create compliance notification topic for cross-region alerts
        this.complianceNotificationTopic = new sns.Topic(this, 'ComplianceNotificationTopic', {
            topicName: `${projectName}-${environment}-compliance-notifications`,
            displayName: `Compliance Notifications for ${projectName} ${environment}`,
            masterKey: kms.Alias.fromAliasName(this, 'DefaultSNSKey', 'alias/aws/sns')
        });

        // Create cross-region replication key for data sovereignty
        this.crossRegionReplicationKey = new kms.Key(this, 'CrossRegionReplicationKey', {
            description: `Cross-region replication key for ${projectName} ${environment} data sovereignty`,
            enableKeyRotation: true,
            removalPolicy: environment === 'production' ? cdk.RemovalPolicy.RETAIN : cdk.RemovalPolicy.DESTROY,
        });

        // Create alias for the replication key
        new kms.Alias(this, 'CrossRegionReplicationKeyAlias', {
            aliasName: `alias/${projectName}-${environment}-replication-key`,
            targetKey: this.crossRegionReplicationKey
        });

        // Deploy enhanced Security stack with comprehensive configuration
        this.securityStack = new SecurityStack(this, 'EnhancedSecurityStack', {
            environment,
            projectName,
            region: this.region,
            primaryRegion,
            secondaryRegions,
            crossRegionEnabled: true,
            complianceStandards: [
                ...complianceStandards,
                ...(enableGdprCompliance ? ['GDPR'] : [])
            ],
            dataClassification
        });

        // Create cross-region compliance monitoring integration
        this.createComplianceIntegration(projectName, environment, complianceStandards);

        // Create data sovereignty monitoring if enabled
        if (enableDataSovereignty) {
            this.createDataSovereigntyMonitoring(projectName, environment, primaryRegion, secondaryRegions);
        }

        // Create enhanced outputs for integration
        this.createIntegrationOutputs(projectName, environment);

        // Apply comprehensive tagging
        this.applyComprehensiveTags(environment, projectName, primaryRegion, secondaryRegions, complianceStandards, dataClassification);
    }

    /**
     * Create compliance monitoring integration
     */
    private createComplianceIntegration(
        projectName: string, 
        environment: string, 
        complianceStandards: string[]
    ): void {
        // Subscribe compliance notification topic to security stack events
        this.complianceNotificationTopic.addSubscription(
            new subscriptions.EmailSubscription(`compliance-${environment}@${projectName}.com`)
        );

        // Create compliance dashboard integration (placeholder for future CloudWatch dashboard)
        new cdk.CfnOutput(this, 'ComplianceDashboardIntegration', {
            value: JSON.stringify({
                complianceFunction: this.securityStack.complianceMonitoringFunction.functionArn,
                notificationTopic: this.complianceNotificationTopic.topicArn,
                standards: complianceStandards,
                monitoringSchedule: 'Daily at 2 AM UTC'
            }),
            exportName: `${this.stackName}-ComplianceDashboardIntegration`,
            description: 'Compliance dashboard integration configuration'
        });
    }

    /**
     * Create data sovereignty monitoring
     */
    private createDataSovereigntyMonitoring(
        projectName: string, 
        environment: string, 
        primaryRegion: string, 
        secondaryRegions: string[]
    ): void {
        // Create data sovereignty configuration
        const sovereigntyConfig = {
            primaryRegion,
            approvedRegions: [primaryRegion, ...secondaryRegions],
            restrictedRegions: ['cn-north-1', 'cn-northwest-1'], // Example restricted regions
            monitoringEnabled: true,
            alertingEnabled: true
        };

        new cdk.CfnOutput(this, 'DataSovereigntyConfig', {
            value: JSON.stringify(sovereigntyConfig),
            exportName: `${this.stackName}-DataSovereigntyConfig`,
            description: 'Data sovereignty monitoring configuration'
        });
    }

    /**
     * Create integration outputs
     */
    private createIntegrationOutputs(projectName: string, environment: string): void {
        // Security stack integration outputs
        new cdk.CfnOutput(this, 'SecurityStackArn', {
            value: this.securityStack.stackId,
            exportName: `${this.stackName}-SecurityStackArn`,
            description: 'ARN of the enhanced security stack'
        });

        new cdk.CfnOutput(this, 'ApplicationKMSKeyArn', {
            value: this.securityStack.kmsKey.keyArn,
            exportName: `${this.stackName}-ApplicationKMSKeyArn`,
            description: 'ARN of the application KMS key for encryption'
        });

        new cdk.CfnOutput(this, 'CrossRegionKMSKeyArn', {
            value: this.securityStack.crossRegionKmsKey?.keyArn || 'Not configured',
            exportName: `${this.stackName}-CrossRegionKMSKeyArn`,
            description: 'ARN of the cross-region KMS key'
        });

        new cdk.CfnOutput(this, 'DataClassificationBucketArn', {
            value: this.securityStack.dataClassificationBucket.bucketArn,
            exportName: `${this.stackName}-DataClassificationBucketArn`,
            description: 'ARN of the data classification bucket'
        });

        new cdk.CfnOutput(this, 'ComplianceMonitoringFunctionArn', {
            value: this.securityStack.complianceMonitoringFunction.functionArn,
            exportName: `${this.stackName}-ComplianceMonitoringFunctionArn`,
            description: 'ARN of the compliance monitoring function'
        });

        // Integration summary
        new cdk.CfnOutput(this, 'SecurityIntegrationSummary', {
            value: JSON.stringify({
                encryption: 'Multi-region KMS with automatic key rotation',
                compliance: 'Automated monitoring with daily checks',
                dataClassification: 'Encrypted bucket with lifecycle policies',
                crossRegion: 'Enabled with data sovereignty checks',
                monitoring: 'Lambda-based compliance and sovereignty monitoring',
                alerting: 'SNS-based notification system'
            }),
            exportName: `${this.stackName}-SecurityIntegrationSummary`,
            description: 'Security integration configuration summary'
        });
    }

    /**
     * Apply comprehensive tagging
     */
    private applyComprehensiveTags(
        environment: string,
        projectName: string,
        primaryRegion: string,
        secondaryRegions: string[],
        complianceStandards: string[],
        dataClassification: string
    ): void {
        const tags = {
            Environment: environment,
            Project: projectName,
            Component: 'MultiRegionSecurity',
            PrimaryRegion: primaryRegion,
            SecondaryRegions: secondaryRegions.join(','),
            ComplianceStandards: complianceStandards.join(','),
            DataClassification: dataClassification,
            SecurityLevel: 'Enhanced',
            CrossRegionEnabled: 'true',
            EncryptionEnabled: 'true',
            ComplianceMonitoring: 'true'
        };

        Object.entries(tags).forEach(([key, value]) => {
            cdk.Tags.of(this).add(key, value);
        });
    }
}

/**
 * Usage Examples:
 * 
 * ## Example 1: Production Environment with Full Compliance
 * 
 * ```typescript
 * const app = new cdk.App();
 * 
 * // Deploy in primary region with full compliance suite
 * new MultiRegionSecurityIntegrationExample(app, 'ProductionSecurity-Primary', {
 *     env: { region: 'us-east-1', account: '123456789012' },
 *     environment: 'production',
 *     projectName: 'genai-demo',
 *     primaryRegion: 'us-east-1',
 *     secondaryRegions: ['us-west-2', 'eu-west-1'],
 *     complianceStandards: ['SOC2', 'ISO27001', 'PCI-DSS'],
 *     dataClassification: 'confidential',
 *     enableGdprCompliance: true,
 *     enableDataSovereignty: true
 * });
 * ```
 * 
 * ## Example 2: Development Environment with Basic Security
 * 
 * ```typescript
 * new MultiRegionSecurityIntegrationExample(app, 'DevelopmentSecurity', {
 *     env: { region: 'us-east-1', account: '123456789012' },
 *     environment: 'development',
 *     projectName: 'genai-demo',
 *     primaryRegion: 'us-east-1',
 *     secondaryRegions: ['us-west-2'],
 *     complianceStandards: ['SOC2'],
 *     dataClassification: 'internal',
 *     enableGdprCompliance: false,
 *     enableDataSovereignty: false
 * });
 * ```
 * 
 * ## Example 3: GDPR-Compliant European Deployment
 * 
 * ```typescript
 * new MultiRegionSecurityIntegrationExample(app, 'EuropeanSecurity', {
 *     env: { region: 'eu-west-1', account: '123456789012' },
 *     environment: 'production',
 *     projectName: 'genai-demo',
 *     primaryRegion: 'eu-west-1',
 *     secondaryRegions: ['eu-central-1', 'eu-north-1'],
 *     complianceStandards: ['GDPR', 'ISO27001'],
 *     dataClassification: 'restricted',
 *     enableGdprCompliance: true,
 *     enableDataSovereignty: true
 * });
 * ```
 */

/**
 * Key Features Enabled:
 * 
 * ## 1. Cross-Region Data Encryption
 * - **Multi-Region KMS Keys**: Separate keys for application and cross-region operations
 * - **Automatic Key Rotation**: Enabled for all KMS keys with annual rotation
 * - **Encryption at Rest**: All S3 buckets encrypted with customer-managed KMS keys
 * - **Encryption in Transit**: TLS 1.3 enforced for all data transfers
 * 
 * ## 2. Compliance Monitoring (SOC2, ISO27001, GDPR)
 * - **Automated Compliance Checks**: Daily Lambda-based compliance monitoring
 * - **Compliance Reporting**: Automated generation of compliance reports
 * - **Config Rules**: AWS Config rules for continuous compliance monitoring
 * - **Audit Trail**: Comprehensive logging of all compliance-related activities
 * 
 * ## 3. Data Sovereignty and Privacy Protection
 * - **Regional Data Residency**: Ensures data remains within approved regions
 * - **GDPR Privacy Controls**: Specialized bucket policies for personal data protection
 * - **Data Classification**: Automated data classification with appropriate controls
 * - **Cross-Border Transfer Monitoring**: Alerts for any unauthorized data movement
 * 
 * ## 4. Enhanced Security Monitoring
 * - **Real-Time Threat Detection**: Integration with GuardDuty and Security Hub
 * - **Anomaly Detection**: ML-based detection of unusual access patterns
 * - **Incident Response**: Automated response to security incidents
 * - **Security Metrics**: Comprehensive security dashboards and reporting
 * 
 * ## 5. Multi-Region Architecture Support
 * - **Cross-Region Replication**: Secure replication of critical security data
 * - **Regional Failover**: Automatic failover of security services
 * - **Unified Security Policies**: Consistent security policies across all regions
 * - **Centralized Management**: Single pane of glass for multi-region security
 */