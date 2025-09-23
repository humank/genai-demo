import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as kms from 'aws-cdk-lib/aws-kms';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as route53 from 'aws-cdk-lib/aws-route53';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as ssm from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';
import { DisasterRecoveryAutomation } from '../constructs/disaster-recovery-automation';
import { CertificateStack } from './certificate-stack';
import { CoreInfrastructureStack } from './core-infrastructure-stack';
import { NetworkStack } from './network-stack';

export interface DisasterRecoveryStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly primaryRegion: string;
    readonly secondaryRegion: string;
    readonly domain?: string;
    readonly kmsKey: kms.Key;
    readonly primaryStackOutputs?: { [key: string]: string };
    readonly primaryAuroraCluster?: rds.IDatabaseCluster;
    readonly primaryHostedZone?: route53.IHostedZone;
    readonly primaryHealthCheck?: route53.CfnHealthCheck;
    readonly secondaryHealthCheck?: route53.CfnHealthCheck;
}

export class DisasterRecoveryStack extends cdk.Stack {
    public readonly networkStack: NetworkStack;
    public readonly certificateStack: CertificateStack;
    public readonly coreInfrastructureStack: CoreInfrastructureStack;
    public readonly drMonitoring: cloudwatch.Dashboard;
    public readonly drAutomation: DisasterRecoveryAutomation;
    public readonly alertingTopic: sns.Topic;

    constructor(scope: Construct, id: string, props: DisasterRecoveryStackProps) {
        super(scope, id, props);

        const {
            environment,
            projectName,
            primaryRegion,
            secondaryRegion,
            domain,
            primaryStackOutputs,
            primaryAuroraCluster,
            primaryHostedZone,
            primaryHealthCheck,
            secondaryHealthCheck
        } = props;

        // Apply common tags to the stack
        const commonTags = {
            Project: projectName,
            Environment: `${environment}-dr`,
            ManagedBy: 'AWS-CDK',
            Component: 'DisasterRecovery',
            PrimaryRegion: primaryRegion,
            SecondaryRegion: secondaryRegion,
            RegionType: 'secondary'
        };

        Object.entries(commonTags).forEach(([key, value]) => {
            cdk.Tags.of(this).add(key, value);
        });

        // Get region-specific configuration
        const envConfig = this.getRegionSpecificConfig(environment, secondaryRegion);
        const multiRegionConfig = this.node.tryGetContext('genai-demo:multi-region') || {};

        // Only deploy DR infrastructure if multi-region is enabled and environment is production
        if (multiRegionConfig['enable-dr'] && environment === 'production') {
            // Create DR domain name
            const drDomain = domain ? `dr.${domain}` : undefined;

            // Deploy Network Stack for DR region
            this.networkStack = new NetworkStack(this, 'DRNetworkStack', {
                env: {
                    account: this.account,
                    region: secondaryRegion
                }
            });

            // Deploy Certificate Stack for DR region
            this.certificateStack = new CertificateStack(this, 'DRCertificateStack', {
                environment: `${environment}-dr`,
                projectName,
                domain: drDomain,
                env: {
                    account: this.account,
                    region: secondaryRegion
                }
            });

            // Deploy Core Infrastructure Stack for DR region
            this.coreInfrastructureStack = new CoreInfrastructureStack(this, 'DRCoreInfrastructureStack', {
                vpc: this.networkStack.vpc,
                securityGroups: this.networkStack.securityGroups,
                kmsKey: props.kmsKey,
                env: {
                    account: this.account,
                    region: secondaryRegion
                }
            });

            // Set up stack dependencies
            this.certificateStack.addDependency(this.networkStack);
            this.coreInfrastructureStack.addDependency(this.certificateStack);

            // Create SNS topic for DR alerting
            this.alertingTopic = new sns.Topic(this, 'DRAlertingTopic', {
                topicName: `${projectName}-${environment}-dr-alerts`,
                displayName: `DR Alerts for ${projectName} ${environment}`
            });

            // Create enhanced DR automation
            this.drAutomation = new DisasterRecoveryAutomation(this, 'DRAutomation', {
                projectName,
                environment,
                primaryRegion,
                secondaryRegion,
                auroraCluster: primaryAuroraCluster,
                hostedZone: primaryHostedZone,
                primaryHealthCheck,
                secondaryHealthCheck,
                alertingTopic: this.alertingTopic
            });

            // Create DR-specific monitoring and automation
            this.drMonitoring = this.createDRMonitoring(projectName, environment, envConfig);

            // Store DR configuration in Systems Manager Parameter Store
            this.storeDRConfiguration(projectName, environment, primaryRegion, secondaryRegion, envConfig);

            // Create cross-region replication setup
            this.setupCrossRegionReplication(projectName, environment, primaryStackOutputs);

            // Create outputs for DR stack
            this.createOutputs(projectName, environment, secondaryRegion, drDomain);
        }
    }

    private getRegionSpecificConfig(environment: string, region: string): any {
        const environments = this.node.tryGetContext('genai-demo:environments') || {};
        const regions = this.node.tryGetContext('genai-demo:regions') || {};

        // Get base environment config
        const baseConfig = environments[environment] || environments['production'] || {};

        // Get region-specific overrides
        const regionConfig = regions.regions?.[region] || {};

        // Merge configurations with region-specific overrides
        return {
            ...baseConfig,
            ...regionConfig,
            // DR-specific adjustments
            'eks-min-nodes': Math.max(1, Math.floor((baseConfig['eks-min-nodes'] || 2) * 0.5)),
            'eks-max-nodes': Math.floor((baseConfig['eks-max-nodes'] || 10) * 0.8),
            'rds-multi-az': true, // Always enable Multi-AZ for DR
            'backup-retention': regionConfig['backup-retention'] || { rds: 30, logs: 90 }
        };
    }

    private createDRMonitoring(
        projectName: string,
        environment: string,
        envConfig: any
    ): cloudwatch.Dashboard {
        // Create CloudWatch Dashboard for DR monitoring
        const dashboard = new cloudwatch.Dashboard(this, 'DRMonitoringDashboard', {
            dashboardName: `${projectName}-${environment}-dr-monitoring`,
            defaultInterval: cdk.Duration.minutes(5)
        });

        // Add widgets for DR monitoring
        dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `# Disaster Recovery Monitoring Dashboard\n\n**Environment:** ${environment}-dr\n**Region:** ${this.region}\n**Last Updated:** ${new Date().toISOString()}`,
                width: 24,
                height: 3
            })
        );

        // Add VPC monitoring widget
        if (this.networkStack) {
            dashboard.addWidgets(
                new cloudwatch.GraphWidget({
                    title: 'VPC Network Metrics',
                    left: [
                        new cloudwatch.Metric({
                            namespace: 'AWS/VPC',
                            metricName: 'PacketsDropped',
                            dimensionsMap: {
                                VpcId: this.networkStack.vpc.vpcId
                            }
                        })
                    ],
                    width: 12,
                    height: 6
                })
            );
        }

        // Add ALB monitoring widget
        if (this.coreInfrastructureStack) {
            dashboard.addWidgets(
                new cloudwatch.GraphWidget({
                    title: 'Application Load Balancer Metrics',
                    left: [
                        new cloudwatch.Metric({
                            namespace: 'AWS/ApplicationELB',
                            metricName: 'TargetResponseTime',
                            dimensionsMap: {
                                LoadBalancer: this.coreInfrastructureStack.loadBalancer.loadBalancerFullName
                            }
                        })
                    ],
                    right: [
                        new cloudwatch.Metric({
                            namespace: 'AWS/ApplicationELB',
                            metricName: 'HTTPCode_Target_2XX_Count',
                            dimensionsMap: {
                                LoadBalancer: this.coreInfrastructureStack.loadBalancer.loadBalancerFullName
                            }
                        })
                    ],
                    width: 12,
                    height: 6
                })
            );
        }

        return dashboard;
    }

    private storeDRConfiguration(
        projectName: string,
        environment: string,
        primaryRegion: string,
        secondaryRegion: string,
        envConfig: any
    ): void {
        // Store DR configuration in Systems Manager Parameter Store
        const drConfig = {
            primaryRegion,
            secondaryRegion,
            environment: `${environment}-dr`,
            projectName,
            deploymentTimestamp: new Date().toISOString(),
            resourceConfiguration: envConfig,
            failoverSettings: {
                rto: this.node.tryGetContext('genai-demo:multi-region')?.['failover-rto-minutes'] || 1,
                rpo: this.node.tryGetContext('genai-demo:multi-region')?.['failover-rpo-minutes'] || 0
            }
        };

        new ssm.StringParameter(this, 'DRConfiguration', {
            parameterName: `/${projectName}/${environment}/dr/configuration`,
            stringValue: JSON.stringify(drConfig),
            description: `Disaster Recovery configuration for ${projectName} ${environment}`,
            tier: ssm.ParameterTier.STANDARD
        });

        // Store individual configuration values for easy access
        Object.entries(envConfig).forEach(([key, value]) => {
            new ssm.StringParameter(this, `DRConfig${key.replace(/[^a-zA-Z0-9]/g, '')}`, {
                parameterName: `/${projectName}/${environment}/dr/config/${key}`,
                stringValue: String(value),
                description: `DR configuration value for ${key}`,
                tier: ssm.ParameterTier.STANDARD
            });
        });
    }

    private setupCrossRegionReplication(
        projectName: string,
        environment: string,
        primaryStackOutputs?: { [key: string]: string }
    ): void {
        // Create SNS topic for cross-region replication notifications
        const replicationTopic = new sns.Topic(this, 'CrossRegionReplicationTopic', {
            topicName: `${projectName}-${environment}-dr-replication-notifications`,
            displayName: `Cross-Region Replication Notifications for ${projectName} ${environment}`
        });

        // Store replication configuration
        const replicationConfig = {
            enabled: true,
            primaryRegion: primaryStackOutputs?.primaryRegion || 'ap-east-2',
            secondaryRegion: this.region,
            replicationTargets: [
                'rds-snapshots',
                'cloudwatch-logs',
                'application-data'
            ],
            notificationTopic: replicationTopic.topicArn
        };

        new ssm.StringParameter(this, 'CrossRegionReplicationConfig', {
            parameterName: `/${projectName}/${environment}/dr/replication-config`,
            stringValue: JSON.stringify(replicationConfig),
            description: `Cross-region replication configuration for ${projectName} ${environment}`,
            tier: ssm.ParameterTier.STANDARD
        });

        // Add tags
        cdk.Tags.of(replicationTopic).add('Name', `${projectName}-${environment}-dr-replication-notifications`);
        cdk.Tags.of(replicationTopic).add('Environment', `${environment}-dr`);
        cdk.Tags.of(replicationTopic).add('Project', projectName);
        cdk.Tags.of(replicationTopic).add('Service', 'CrossRegionReplication');
    }

    private createOutputs(
        projectName: string,
        environment: string,
        secondaryRegion: string,
        drDomain?: string
    ): void {
        // DR Stack outputs
        new cdk.CfnOutput(this, 'DRRegion', {
            value: secondaryRegion,
            description: 'Disaster Recovery region',
            exportName: `${projectName}-${environment}-dr-region`
        });

        new cdk.CfnOutput(this, 'DREnvironment', {
            value: `${environment}-dr`,
            description: 'Disaster Recovery environment name',
            exportName: `${projectName}-${environment}-dr-environment`
        });

        if (drDomain) {
            new cdk.CfnOutput(this, 'DRDomain', {
                value: drDomain,
                description: 'Disaster Recovery domain name',
                exportName: `${projectName}-${environment}-dr-domain`
            });
        }

        // Network Stack outputs
        if (this.networkStack) {
            new cdk.CfnOutput(this, 'DRVpcId', {
                value: this.networkStack.vpc.vpcId,
                description: 'DR VPC ID',
                exportName: `${projectName}-${environment}-dr-vpc-id`
            });
        }

        // Core Infrastructure outputs
        if (this.coreInfrastructureStack) {
            new cdk.CfnOutput(this, 'DRLoadBalancerDnsName', {
                value: this.coreInfrastructureStack.loadBalancer.loadBalancerDnsName,
                description: 'DR Application Load Balancer DNS name',
                exportName: `${projectName}-${environment}-dr-alb-dns-name`
            });
        }

        // Certificate outputs
        if (this.certificateStack?.certificate) {
            new cdk.CfnOutput(this, 'DRCertificateArn', {
                value: this.certificateStack.certificate.certificateArn,
                description: 'DR Certificate ARN',
                exportName: `${projectName}-${environment}-dr-certificate-arn`
            });
        }

        // Monitoring outputs
        if (this.drMonitoring) {
            new cdk.CfnOutput(this, 'DRMonitoringDashboardUrl', {
                value: `https://${secondaryRegion}.console.aws.amazon.com/cloudwatch/home?region=${secondaryRegion}#dashboards:name=${this.drMonitoring.dashboardName}`,
                description: 'DR Monitoring Dashboard URL',
                exportName: `${projectName}-${environment}-dr-monitoring-dashboard-url`
            });
        }

        // DR Automation outputs
        if (this.drAutomation) {
            new cdk.CfnOutput(this, 'DRAutomationFailoverStateMachine', {
                value: this.drAutomation.failoverStateMachine.stateMachineArn,
                description: 'DR automation failover state machine ARN',
                exportName: `${projectName}-${environment}-dr-automation-failover-sm`
            });

            new cdk.CfnOutput(this, 'DRAutomationChaosTestingStateMachine', {
                value: this.drAutomation.chaosTestingStateMachine.stateMachineArn,
                description: 'DR automation chaos testing state machine ARN',
                exportName: `${projectName}-${environment}-dr-automation-chaos-sm`
            });

            new cdk.CfnOutput(this, 'DRAutomationMonitoringDashboard', {
                value: `https://${secondaryRegion}.console.aws.amazon.com/cloudwatch/home?region=${secondaryRegion}#dashboards:name=${this.drAutomation.drMonitoringDashboard.dashboardName}`,
                description: 'DR automation monitoring dashboard URL',
                exportName: `${projectName}-${environment}-dr-automation-dashboard-url`
            });
        }

        // Alerting topic output
        if (this.alertingTopic) {
            new cdk.CfnOutput(this, 'DRAlertingTopicArn', {
                value: this.alertingTopic.topicArn,
                description: 'DR alerting SNS topic ARN',
                exportName: `${projectName}-${environment}-dr-alerting-topic-arn`
            });
        }

        // Deployment timestamp
        new cdk.CfnOutput(this, 'DRDeploymentTimestamp', {
            value: new Date().toISOString(),
            description: 'DR stack deployment timestamp',
            exportName: `${projectName}-${environment}-dr-deployment-timestamp`
        });
    }
}