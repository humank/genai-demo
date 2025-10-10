import * as cdk from 'aws-cdk-lib';
import * as sns from 'aws-cdk-lib/aws-sns';
import { Construct } from 'constructs';
import { SSOStack } from '../stacks/sso-stack';

/**
 * Example: Multi-Region SSO Integration
 * 
 * This example demonstrates how to deploy the enhanced SSO stack
 * with cross-region unified identity authentication support.
 * 
 * Requirements: 4.3.1 - Multi-Region Security and Identity Management
 */

export interface MultiRegionSSOIntegrationProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly ssoInstanceArn: string;
    readonly primaryRegion: string;
    readonly secondaryRegions: string[];
}

export class MultiRegionSSOIntegrationExample extends cdk.Stack {
    public readonly ssoStack: SSOStack;
    public readonly securityMonitoringTopic: sns.Topic;

    constructor(scope: Construct, id: string, props: MultiRegionSSOIntegrationProps) {
        super(scope, id, props);

        const { environment, projectName, ssoInstanceArn, primaryRegion, secondaryRegions } = props;

        // Create security monitoring topic for cross-region alerts
        this.securityMonitoringTopic = new sns.Topic(this, 'SecurityMonitoringTopic', {
            topicName: `${projectName}-${environment}-security-monitoring`,
            displayName: `Security Monitoring for ${projectName} ${environment}`
        });

        // Deploy enhanced SSO stack with cross-region support
        this.ssoStack = new SSOStack(this, 'EnhancedSSOStack', {
            environment,
            projectName,
            region: this.region,
            ssoInstanceArn,
            primaryRegion,
            secondaryRegions,
            crossRegionEnabled: true,
            securityMonitoringTopic: this.securityMonitoringTopic
        });

        // Create outputs for integration with other stacks
        new cdk.CfnOutput(this, 'SSOStackName', {
            value: this.ssoStack.stackName,
            exportName: `${this.stackName}-SSOStackName`,
            description: 'Name of the enhanced SSO stack'
        });

        new cdk.CfnOutput(this, 'SecurityMonitoringTopicArn', {
            value: this.securityMonitoringTopic.topicArn,
            exportName: `${this.stackName}-SecurityMonitoringTopicArn`,
            description: 'ARN of the security monitoring topic'
        });

        // Tag all resources
        cdk.Tags.of(this).add('Environment', environment);
        cdk.Tags.of(this).add('Project', projectName);
        cdk.Tags.of(this).add('Component', 'MultiRegionSSO');
        cdk.Tags.of(this).add('PrimaryRegion', primaryRegion);
        cdk.Tags.of(this).add('SecondaryRegions', secondaryRegions.join(','));
    }
}

/**
 * Usage Example:
 * 
 * ```typescript
 * const app = new cdk.App();
 * 
 * // Deploy in primary region (us-east-1)
 * new MultiRegionSSOIntegrationExample(app, 'MultiRegionSSO-Primary', {
 *     env: { region: 'us-east-1', account: '123456789012' },
 *     environment: 'production',
 *     projectName: 'genai-demo',
 *     ssoInstanceArn: 'arn:aws:sso:::instance/ssoins-1234567890abcdef',
 *     primaryRegion: 'us-east-1',
 *     secondaryRegions: ['us-west-2', 'eu-west-1']
 * });
 * 
 * // Deploy in secondary regions with cross-region references
 * new MultiRegionSSOIntegrationExample(app, 'MultiRegionSSO-Secondary-USWest2', {
 *     env: { region: 'us-west-2', account: '123456789012' },
 *     environment: 'production',
 *     projectName: 'genai-demo',
 *     ssoInstanceArn: 'arn:aws:sso:::instance/ssoins-1234567890abcdef',
 *     primaryRegion: 'us-east-1',
 *     secondaryRegions: ['us-west-2', 'eu-west-1']
 * });
 * 
 * new MultiRegionSSOIntegrationExample(app, 'MultiRegionSSO-Secondary-EUWest1', {
 *     env: { region: 'eu-west-1', account: '123456789012' },
 *     environment: 'production',
 *     projectName: 'genai-demo',
 *     ssoInstanceArn: 'arn:aws:sso:::instance/ssoins-1234567890abcdef',
 *     primaryRegion: 'us-east-1',
 *     secondaryRegions: ['us-west-2', 'eu-west-1']
 * });
 * ```
 * 
 * Key Features Enabled:
 * 
 * 1. **Cross-Region SSO Integration**:
 *    - Unified permission sets across all regions
 *    - Cross-region admin role for multi-region operations
 *    - Region-specific execution roles
 * 
 * 2. **Unified RBAC Strategy**:
 *    - Enhanced permission sets with cross-region access
 *    - Consistent role-based access control across regions
 *    - Privilege escalation detection and prevention
 * 
 * 3. **Cross-Region Audit Log Collection**:
 *    - Multi-region CloudTrail with centralized logging
 *    - Encrypted audit bucket with lifecycle policies
 *    - Cross-region log aggregation and analysis
 * 
 * 4. **Security Event Correlation**:
 *    - Lambda function for cross-region security event analysis
 *    - EventBridge rules for SSO and IAM event capture
 *    - Automated threat detection and alerting
 * 
 * 5. **Enhanced Security Monitoring**:
 *    - Impossible travel detection for SSO events
 *    - Privilege escalation attempt monitoring
 *    - Cross-region GuardDuty finding correlation
 *    - Automated security incident response
 */