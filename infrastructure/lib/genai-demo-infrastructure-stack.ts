import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { CertificateStack, CoreInfrastructureStack, MSKStack, NetworkStack, ObservabilityStack, SecurityStack } from './stacks';

export interface GenAIDemoInfrastructureStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly domain?: string;
}

/**
 * Main infrastructure stack that orchestrates all modular stacks
 * This stack creates and manages dependencies between:
 * - NetworkStack: VPC, subnets, and security groups
 * - CertificateStack: ACM certificates and Route 53 configuration
 * - CoreInfrastructureStack: ALB and shared resources
 * - SecurityStack: Security and compliance infrastructure
 */
export class GenAIDemoInfrastructureStack extends cdk.Stack {
    public readonly networkStack: NetworkStack;
    public readonly certificateStack: CertificateStack;
    public readonly coreInfrastructureStack: CoreInfrastructureStack;
    public readonly mskStack: MSKStack;
    public readonly observabilityStack: ObservabilityStack;
    public readonly securityStack: SecurityStack;
    public readonly costOptimizationStack: CostOptimizationStack;

    constructor(scope: Construct, id: string, props: GenAIDemoInfrastructureStackProps) {
        super(scope, id, props);

        const { environment, projectName, domain } = props;

        // Apply common tags to the main stack
        const commonTags = {
            Project: projectName,
            Environment: environment,
            ManagedBy: 'AWS-CDK',
            Component: 'MainInfrastructure'
        };

        Object.entries(commonTags).forEach(([key, value]) => {
            cdk.Tags.of(this).add(key, value);
        });

        // 1. Create Network Stack (VPC, subnets, security groups)
        this.networkStack = new NetworkStack(this, 'NetworkStack', {
            environment,
            projectName,
            env: props.env,
            description: `Network infrastructure for ${projectName} ${environment} environment`,
            tags: {
                ...commonTags,
                StackType: 'Network'
            }
        });

        // 2. Create Certificate Stack (ACM certificates and Route 53)
        this.certificateStack = new CertificateStack(this, 'CertificateStack', {
            environment,
            projectName,
            domain,
            env: props.env,
            description: `Certificate and DNS infrastructure for ${projectName} ${environment} environment`,
            tags: {
                ...commonTags,
                StackType: 'Certificate'
            }
        });

        // 3. Create Core Infrastructure Stack (ALB, EKS, and shared resources)
        this.coreInfrastructureStack = new CoreInfrastructureStack(this, 'CoreInfrastructureStack', {
            environment,
            projectName,
            domain,
            vpc: this.networkStack.vpc,
            albSecurityGroup: this.networkStack.albSecurityGroup,
            eksSecurityGroup: this.networkStack.eksSecurityGroup,
            hostedZone: this.certificateStack.hostedZone,
            certificate: this.certificateStack.certificate,
            wildcardCertificate: this.certificateStack.wildcardCertificate,
            env: props.env,
            description: `Core infrastructure (ALB, EKS, shared resources) for ${projectName} ${environment} environment`,
            tags: {
                ...commonTags,
                StackType: 'CoreInfrastructure'
            }
        });

        // 4. Create Observability Stack (Monitoring, logging, and tracing infrastructure)
        this.observabilityStack = new ObservabilityStack(this, 'ObservabilityStack', {
            environment,
            projectName,
            vpc: this.networkStack.vpc,
            region: props.env?.region,
            env: props.env,
            description: `Observability infrastructure (monitoring, logging, tracing) for ${projectName} ${environment} environment`,
            tags: {
                ...commonTags,
                StackType: 'Observability'
            }
        });

        // 5. Create Security Stack (Security and compliance infrastructure)
        this.securityStack = new SecurityStack(this, 'SecurityStack', {
            environment,
            projectName,
            vpc: this.networkStack.vpc,
            albArn: this.coreInfrastructureStack.loadBalancer.loadBalancerArn,
            region: props.env?.region,
            env: props.env,
            description: `Security and compliance infrastructure for ${projectName} ${environment} environment`,
            tags: {
                ...commonTags,
                StackType: 'Security'
            }
        });

        // 6. Create MSK Stack (Amazon MSK cluster for domain events)
        this.mskStack = new MSKStack(this, 'MSKStack', {
            environment,
            projectName,
            vpc: this.networkStack.vpc,
            mskSecurityGroup: this.networkStack.mskSecurityGroup,
            kmsKey: this.securityStack.kmsKey,
            alertingTopic: this.observabilityStack.alertingTopic,
            region: props.env?.region,
            env: props.env,
            description: `MSK cluster infrastructure for domain events in ${projectName} ${environment} environment`,
            tags: {
                ...commonTags,
                StackType: 'MSK'
            }
        });

        // 7. Create Cost Optimization Stack (Billing alerts and cost monitoring)
        this.costOptimizationStack = new CostOptimizationStack(this, 'CostOptimizationStack', {
            environment,
            alertEmail: process.env.ALERT_EMAIL || 'admin@example.com',
            slackWebhookUrl: process.env.SLACK_WEBHOOK_URL,
            env: props.env,
            description: `Cost optimization and billing alerts for ${projectName} ${environment} environment`,
            tags: {
                ...commonTags,
                StackType: 'CostOptimization'
            }
        });

        // Set up stack dependencies
        this.coreInfrastructureStack.addDependency(this.networkStack);
        this.coreInfrastructureStack.addDependency(this.certificateStack);
        this.observabilityStack.addDependency(this.networkStack);
        this.securityStack.addDependency(this.networkStack);
        this.securityStack.addDependency(this.coreInfrastructureStack);
        this.securityStack.addDependency(this.observabilityStack);
        this.mskStack.addDependency(this.networkStack);
        this.mskStack.addDependency(this.observabilityStack);
        this.mskStack.addDependency(this.securityStack);

        // Create main stack outputs that aggregate important information from all stacks
        this.createMainStackOutputs(projectName, environment);
    }

    private createMainStackOutputs(projectName: string, environment: string): void {
        // Main stack summary outputs
        new cdk.CfnOutput(this, 'InfrastructureStackSummary', {
            value: JSON.stringify({
                networkStack: this.networkStack.stackName,
                certificateStack: this.certificateStack.stackName,
                coreInfrastructureStack: this.coreInfrastructureStack.stackName,
                mskStack: this.mskStack.stackName,
                observabilityStack: this.observabilityStack.stackName,
                securityStack: this.securityStack.stackName,
                environment: environment,
                projectName: projectName
            }),
            description: 'Summary of all infrastructure stacks deployed',
            exportName: `${projectName}-${environment}-infrastructure-summary`
        });

        // Cross-stack reference helper
        new cdk.CfnOutput(this, 'CrossStackReferences', {
            value: JSON.stringify({
                vpcId: this.networkStack.vpc.vpcId,
                albArn: this.coreInfrastructureStack.loadBalancer.loadBalancerArn,
                albDnsName: this.coreInfrastructureStack.loadBalancer.loadBalancerDnsName,
                eksSecurityGroupId: this.networkStack.eksSecurityGroup.securityGroupId,
                rdsSecurityGroupId: this.networkStack.rdsSecurityGroup.securityGroupId,
                mskSecurityGroupId: this.networkStack.mskSecurityGroup.securityGroupId,
                certificateArn: this.certificateStack.certificate?.certificateArn || 'N/A',
                hostedZoneId: this.certificateStack.hostedZone?.hostedZoneId || 'N/A',
                kmsKeyArn: this.securityStack.kmsKey.keyArn,
                secretsManagerKmsKeyArn: this.securityStack.secretsManagerKey.keyArn,
                webAclArn: this.securityStack.webAcl.attrArn,
                securityNotificationsTopicArn: this.securityStack.securityNotificationsTopic.topicArn,
                observabilityAlertingTopicArn: this.observabilityStack.alertingTopic.topicArn,
                observabilityWarningTopicArn: this.observabilityStack.warningTopic.topicArn,
                observabilityInfoTopicArn: this.observabilityStack.infoTopic.topicArn,
                logArchiveBucketName: this.observabilityStack.logArchiveBucket.bucketName,
                dataLakeBucketName: this.observabilityStack.dataLakeBucket.bucketName,
                observabilityRoleArn: this.observabilityStack.observabilityRole.roleArn,
                mskClusterArn: cdk.Fn.getAtt(this.mskStack.mskCluster.logicalId, 'Arn').toString(),
                mskClusterName: this.mskStack.mskCluster.clusterName!,
                mskBootstrapServersIAM: cdk.Fn.getAtt(this.mskStack.mskCluster.logicalId, 'BootstrapBrokerStringSaslIam').toString(),
                mskBootstrapServersTLS: cdk.Fn.getAtt(this.mskStack.mskCluster.logicalId, 'BootstrapBrokerStringTls').toString(),
                mskConnectRoleArn: this.mskStack.mskConnectRole.roleArn
            }),
            description: 'Key resource identifiers for cross-stack references',
            exportName: `${projectName}-${environment}-cross-stack-references`
        });

        // Stack deployment status
        new cdk.CfnOutput(this, 'DeploymentStatus', {
            value: 'COMPLETE',
            description: 'Multi-stack infrastructure deployment status'
        });

        // Architecture information
        new cdk.CfnOutput(this, 'ArchitectureType', {
            value: 'Multi-Stack-Modular-Secure',
            description: 'Infrastructure architecture pattern used'
        });

        // Security compliance summary
        new cdk.CfnOutput(this, 'SecurityComplianceSummary', {
            value: JSON.stringify({
                vpcFlowLogs: 'Enabled',
                cloudTrail: 'Enabled',
                awsConfig: 'Enabled',
                guardDuty: 'Enabled',
                kmsEncryption: 'Enabled',
                secretsManager: 'Enabled',
                waf: 'Enabled',
                keyRotation: 'Enabled',
                observabilityLogging: 'Enabled',
                xrayTracing: 'Enabled',
                cloudWatchAlarming: 'Enabled',
                eventBridgeRouting: 'Enabled',
                crossRegionReplication: 'Configurable'
            }),
            description: 'Security and compliance features summary',
            exportName: `${projectName}-${environment}-security-compliance-summary`
        });
    }

    /**
     * Get VPC from the network stack
     */
    public getVpc() {
        return this.networkStack.vpc;
    }

    /**
     * Get security groups from the network stack
     */
    public getSecurityGroups() {
        return {
            eks: this.networkStack.eksSecurityGroup,
            rds: this.networkStack.rdsSecurityGroup,
            msk: this.networkStack.mskSecurityGroup,
            alb: this.networkStack.albSecurityGroup
        };
    }

    /**
     * Get certificates from the certificate stack
     */
    public getCertificates() {
        return {
            certificate: this.certificateStack.certificate,
            wildcardCertificate: this.certificateStack.wildcardCertificate,
            hostedZone: this.certificateStack.hostedZone
        };
    }

    /**
     * Get load balancer from the core infrastructure stack
     */
    public getLoadBalancer() {
        return this.coreInfrastructureStack.loadBalancer;
    }

    /**
     * Get observability resources from the observability stack
     */
    public getObservabilityResources() {
        return {
            logGroups: this.observabilityStack.logGroups,
            alertingTopic: this.observabilityStack.alertingTopic,
            warningTopic: this.observabilityStack.warningTopic,
            infoTopic: this.observabilityStack.infoTopic,
            logArchiveBucket: this.observabilityStack.logArchiveBucket,
            dataLakeBucket: this.observabilityStack.dataLakeBucket,
            observabilityRole: this.observabilityStack.observabilityRole,
            xrayTracingEnabled: this.observabilityStack.xrayTracingEnabled,
            eventBridgeRules: this.observabilityStack.eventBridgeRules,
            cloudWatchAlarms: this.observabilityStack.cloudWatchAlarms
        };
    }

    /**
     * Get MSK resources from the MSK stack
     */
    public getMSKResources() {
        return {
            mskCluster: this.mskStack.mskCluster,
            mskConfiguration: this.mskStack.mskConfiguration,
            mskClusterPolicy: this.mskStack.mskClusterPolicy,
            mskConnectRole: this.mskStack.mskConnectRole,
            mskConnectLogGroup: this.mskStack.mskConnectLogGroup
        };
    }

    /**
     * Get security resources from the security stack
     */
    public getSecurityResources() {
        return {
            kmsKey: this.securityStack.kmsKey,
            secretsManagerKey: this.securityStack.secretsManagerKey,
            cloudTrail: this.securityStack.cloudTrail,
            guardDutyDetector: this.securityStack.guardDutyDetector,
            webAcl: this.securityStack.webAcl,
            securityNotificationsTopic: this.securityStack.securityNotificationsTopic,
            auditLogsBucket: this.securityStack.auditLogsBucket
        };
    }
}