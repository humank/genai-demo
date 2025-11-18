import * as cdk from 'aws-cdk-lib';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as snsSubscriptions from 'aws-cdk-lib/aws-sns-subscriptions';
import * as ssmIncidents from 'aws-cdk-lib/aws-ssmincidents';
import { Construct } from 'constructs';

export interface IncidentManagerStackProps extends cdk.StackProps {
    environment: string;
    projectName: string;
    alertingTopic: sns.ITopic;
    oncallEmail?: string;
    slackWebhookUrl?: string;
}

/**
 * AWS Systems Manager Incident Manager Stack
 * 
 * Automates incident response with:
 * - Response Plans for different incident types
 * - Escalation Plans for on-call rotation
 * - Automated Runbooks for common issues
 * - Integration with SNS for notifications
 * 
 * Incident Types:
 * 1. Critical - Production down, data loss
 * 2. High - Service degradation, high error rate
 * 3. Medium - Performance issues, non-critical failures
 * 4. Low - Warnings, minor issues
 */
export class IncidentManagerStack extends cdk.Stack {
    public readonly replicationSet: ssmIncidents.CfnReplicationSet;
    public readonly criticalResponsePlan: ssmIncidents.CfnResponsePlan;
    public readonly highResponsePlan: ssmIncidents.CfnResponsePlan;
    public readonly mediumResponsePlan: ssmIncidents.CfnResponsePlan;

    constructor(scope: Construct, id: string, props: IncidentManagerStackProps) {
        super(scope, id, props);

        const { environment, projectName, alertingTopic, oncallEmail, slackWebhookUrl } = props;

        // Create Replication Set (required for Incident Manager)
        this.replicationSet = this.createReplicationSet();

        // Create Contacts for escalation
        const contacts = this.createContacts(oncallEmail);

        // Create Response Plans
        this.criticalResponsePlan = this.createCriticalResponsePlan(
            projectName,
            environment,
            alertingTopic,
            contacts
        );

        this.highResponsePlan = this.createHighResponsePlan(
            projectName,
            environment,
            alertingTopic,
            contacts
        );

        this.mediumResponsePlan = this.createMediumResponsePlan(
            projectName,
            environment,
            alertingTopic,
            contacts
        );

        // Create Outputs
        this.createOutputs(projectName, environment);
    }

    /**
     * Create Replication Set for Incident Manager
     * Required before creating response plans
     */
    private createReplicationSet(): ssmIncidents.CfnReplicationSet {
        return new ssmIncidents.CfnReplicationSet(this, 'ReplicationSet', {
            regions: [
                {
                    regionName: this.region,
                },
            ],
            deletionProtected: false, // Set to true for production
        });
    }

    /**
     * Create Contacts for incident escalation
     * Note: CfnContact is not yet available in AWS CDK
     * TODO: Update when AWS CDK adds support for SSM Incidents Contacts
     * 
     * For now, contacts should be created manually in the AWS Console:
     * https://console.aws.amazon.com/systems-manager/incidents/contacts
     */
    private createContacts(oncallEmail?: string): any[] {
        // Return empty array - contacts must be created manually
        // until AWS CDK adds CfnContact support
        console.warn('SSM Incidents Contacts must be created manually in AWS Console');
        return [];
    }

    /**
     * Create Email Contact Channel
     * Note: Disabled until CfnContact is available in AWS CDK
     */
    private createEmailChannel(id: string, email: string): any {
        // Placeholder - not used until CfnContact is available
        return null;
    }

    /**
     * Create Critical Response Plan
     * For: Production down, data loss, security breach
     */
    private createCriticalResponsePlan(
        projectName: string,
        environment: string,
        alertingTopic: sns.ITopic,
        contacts: any[]
    ): ssmIncidents.CfnResponsePlan {
        return new ssmIncidents.CfnResponsePlan(this, 'CriticalResponsePlan', {
            name: `${projectName}-${environment}-critical`,
            displayName: `${projectName} ${environment} - Critical Incident`,
            incidentTemplate: {
                title: 'Critical Incident - Production Impact',
                impact: 1, // Critical (1-5 scale)
                summary: 'Critical incident affecting production services',
                dedupeString: 'critical-{{alarm-name}}',
                notificationTargets: [
                    {
                        snsTopicArn: alertingTopic.topicArn,
                    },
                ],
            },
            // engagements: contacts.map(contact => contact.attrArn), // Disabled until CfnContact is available
            actions: [
                {
                    ssmAutomation: {
                        documentName: 'AWS-CreateSnapshot', // Example runbook
                        roleArn: this.createIncidentResponseRole().roleArn,
                        documentVersion: '$DEFAULT',
                        targetAccount: 'RESPONSE_PLAN_OWNER_ACCOUNT',
                    },
                },
            ],
            chatChannel: {
                chatbotSns: [alertingTopic.topicArn],
            },
        });
    }

    /**
     * Create High Priority Response Plan
     * For: Service degradation, high error rate
     */
    private createHighResponsePlan(
        projectName: string,
        environment: string,
        alertingTopic: sns.ITopic,
        contacts: any[]
    ): ssmIncidents.CfnResponsePlan {
        return new ssmIncidents.CfnResponsePlan(this, 'HighResponsePlan', {
            name: `${projectName}-${environment}-high`,
            displayName: `${projectName} ${environment} - High Priority Incident`,
            incidentTemplate: {
                title: 'High Priority Incident - Service Degradation',
                impact: 2, // High
                summary: 'High priority incident with service degradation',
                dedupeString: 'high-{{alarm-name}}',
                notificationTargets: [
                    {
                        snsTopicArn: alertingTopic.topicArn,
                    },
                ],
            },
            // engagements: [contacts[0].attrArn, contacts[1].attrArn], // L1 and L2 - Disabled until CfnContact is available
            chatChannel: {
                chatbotSns: [alertingTopic.topicArn],
            },
        });
    }

    /**
     * Create Medium Priority Response Plan
     * For: Performance issues, non-critical failures
     */
    private createMediumResponsePlan(
        projectName: string,
        environment: string,
        alertingTopic: sns.ITopic,
        contacts: any[]
    ): ssmIncidents.CfnResponsePlan {
        return new ssmIncidents.CfnResponsePlan(this, 'MediumResponsePlan', {
            name: `${projectName}-${environment}-medium`,
            displayName: `${projectName} ${environment} - Medium Priority Incident`,
            incidentTemplate: {
                title: 'Medium Priority Incident - Performance Issue',
                impact: 3, // Medium
                summary: 'Medium priority incident with performance impact',
                dedupeString: 'medium-{{alarm-name}}',
                notificationTargets: [
                    {
                        snsTopicArn: alertingTopic.topicArn,
                    },
                ],
            },
            // engagements: [contacts[0].attrArn], // L1 only - Disabled until CfnContact is available
            chatChannel: {
                chatbotSns: [alertingTopic.topicArn],
            },
        });
    }

    /**
     * Create IAM Role for Incident Response automation
     */
    private createIncidentResponseRole(): iam.Role {
        const role = new iam.Role(this, 'IncidentResponseRole', {
            assumedBy: new iam.ServicePrincipal('ssm-incidents.amazonaws.com'),
            description: 'Role for Incident Manager automated actions',
        });

        // Add permissions for common incident response actions
        role.addToPolicy(
            new iam.PolicyStatement({
                effect: iam.Effect.ALLOW,
                actions: [
                    'ec2:CreateSnapshot',
                    'ec2:DescribeInstances',
                    'ec2:DescribeSnapshots',
                    'rds:CreateDBSnapshot',
                    'rds:DescribeDBInstances',
                    'logs:CreateLogGroup',
                    'logs:CreateLogStream',
                    'logs:PutLogEvents',
                    'cloudwatch:PutMetricData',
                ],
                resources: ['*'],
            })
        );

        return role;
    }

    /**
     * Create CloudFormation Outputs
     */
    private createOutputs(projectName: string, environment: string): void {
        new cdk.CfnOutput(this, 'CriticalResponsePlanArn', {
            value: this.criticalResponsePlan.attrArn,
            description: 'Critical Response Plan ARN',
            exportName: `${projectName}-${environment}-critical-response-plan-arn`,
        });

        new cdk.CfnOutput(this, 'HighResponsePlanArn', {
            value: this.highResponsePlan.attrArn,
            description: 'High Priority Response Plan ARN',
            exportName: `${projectName}-${environment}-high-response-plan-arn`,
        });

        new cdk.CfnOutput(this, 'MediumResponsePlanArn', {
            value: this.mediumResponsePlan.attrArn,
            description: 'Medium Priority Response Plan ARN',
            exportName: `${projectName}-${environment}-medium-response-plan-arn`,
        });

        // Output instructions for triggering incidents
        new cdk.CfnOutput(this, 'TriggerIncidentInstructions', {
            value: `aws ssm-incidents start-incident --response-plan-arn ${this.criticalResponsePlan.attrArn} --title "Incident Title"`,
            description: 'Command to manually trigger a critical incident',
        });
    }
}
