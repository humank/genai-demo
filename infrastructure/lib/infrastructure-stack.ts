import * as cdk from 'aws-cdk-lib';
import * as certificatemanager from 'aws-cdk-lib/aws-certificatemanager';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as kms from 'aws-cdk-lib/aws-kms';
import * as route53 from 'aws-cdk-lib/aws-route53';
import * as targets from 'aws-cdk-lib/aws-route53-targets';
import { Construct } from 'constructs';
import { getAlertingConfig } from './config/alerting-config';
import { AlertingStack } from './stacks/alerting-stack';
import { AnalyticsStack } from './stacks/analytics-stack';
import { MSKStack } from './stacks/msk-stack';
import { RdsStack } from './stacks/rds-stack';

export interface GenAIDemoInfrastructureStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly domain?: string;
}

export class GenAIDemoInfrastructureStack extends cdk.Stack {
    public readonly vpc: ec2.Vpc;
    public readonly eksSecurityGroup: ec2.SecurityGroup;
    public readonly rdsSecurityGroup: ec2.SecurityGroup;
    public readonly mskSecurityGroup: ec2.SecurityGroup;
    public readonly albSecurityGroup: ec2.SecurityGroup;
    public readonly hostedZone?: route53.IHostedZone;
    public readonly certificate?: certificatemanager.Certificate;
    public readonly wildcardCertificate?: certificatemanager.Certificate;
    public readonly loadBalancer: elbv2.ApplicationLoadBalancer;
    public readonly kmsKey: kms.Key;
    public readonly rdsStack: RdsStack;
    public readonly mskStack: MSKStack;
    public readonly alertingStack: AlertingStack;
    public readonly analyticsStack: AnalyticsStack;

    constructor(scope: Construct, id: string, props: GenAIDemoInfrastructureStackProps) {
        super(scope, id, props);

        // Get context values
        const environment = props.environment || this.node.tryGetContext('genai-demo:environment') || 'development';
        const projectName = props.projectName || this.node.tryGetContext('genai-demo:project-name') || 'genai-demo';
        const domain = props.domain || this.node.tryGetContext('genai-demo:domain');

        // Tags for all resources
        const commonTags = {
            Project: projectName,
            Environment: environment,
            ManagedBy: 'AWS-CDK',
            Component: 'Infrastructure'
        };

        // Apply tags to the stack
        Object.entries(commonTags).forEach(([key, value]) => {
            cdk.Tags.of(this).add(key, value);
        });

        // Core Infrastructure Components
        this.vpc = this.createVPC(projectName, environment);

        // Create Security Groups for different services
        const securityGroups = this.createSecurityGroups(this.vpc, projectName, environment);
        this.eksSecurityGroup = securityGroups.eksSecurityGroup;
        this.rdsSecurityGroup = securityGroups.rdsSecurityGroup;
        this.mskSecurityGroup = securityGroups.mskSecurityGroup;
        this.albSecurityGroup = securityGroups.albSecurityGroup;

        // DNS and Certificate Management
        if (domain) {
            this.hostedZone = this.lookupHostedZone(domain);
            this.certificate = this.createCertificate(domain, this.hostedZone, projectName, environment);
            this.wildcardCertificate = this.createWildcardCertificate(domain, this.hostedZone, projectName, environment);
        }

        // Application Load Balancer with SSL Termination
        this.loadBalancer = this.createApplicationLoadBalancer(this.vpc, projectName, environment, domain);

        // Create KMS Key for encryption
        this.kmsKey = this.createKMSKey(projectName, environment);

        // Create Alerting and Monitoring Stack
        this.alertingStack = new AlertingStack(this, 'AlertingStack', {
            environment: environment,
            region: this.region,
            applicationName: projectName,
            alertingConfig: getAlertingConfig(environment),
        });

        // Create RDS PostgreSQL Database
        this.rdsStack = new RdsStack(this, 'RdsStack', {
            environment: environment,
            projectName: projectName,
            vpc: this.vpc,
            rdsSecurityGroup: this.rdsSecurityGroup,
            region: this.region
        });

        // Create MSK Cluster
        this.mskStack = new MSKStack(this, 'MSKStack', {
            environment: environment,
            projectName: projectName,
            vpc: this.vpc,
            mskSecurityGroup: this.mskSecurityGroup,
            kmsKey: this.kmsKey,
            alertingTopic: this.alertingStack.criticalAlertsTopic,
            region: this.region
        });

        // Create Analytics Stack
        this.analyticsStack = new AnalyticsStack(this, 'AnalyticsStack', {
            environment: environment,
            projectName: projectName,
            vpc: this.vpc,
            kmsKey: this.kmsKey,
            mskCluster: this.mskStack.mskCluster,
            alertingTopic: this.alertingStack.criticalAlertsTopic,
            region: this.region
        });

        // Create outputs for cross-stack references
        this.createOutputs(projectName, environment, domain);
    }

    private createVPC(projectName: string, environment: string): ec2.Vpc {
        // Get environment-specific configuration
        const envConfig = this.node.tryGetContext('genai-demo:environments')?.[environment] || {};
        const networkingConfig = this.node.tryGetContext('genai-demo:networking') || {};

        const vpcCidr = envConfig['vpc-cidr'] || '10.0.0.0/16';
        const natGateways = envConfig['nat-gateways'] || (environment === 'production' ? 3 : 1);
        const maxAzs = networkingConfig['availability-zones'] || 3;

        const vpc = new ec2.Vpc(this, 'GenAIDemoVPC', {
            vpcName: `${projectName}-${environment}-vpc`,
            maxAzs: maxAzs,
            ipAddresses: ec2.IpAddresses.cidr(vpcCidr),
            subnetConfiguration: [
                {
                    cidrMask: 24,
                    name: 'Public',
                    subnetType: ec2.SubnetType.PUBLIC,
                },
                {
                    cidrMask: 24,
                    name: 'Private',
                    subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
                },
                {
                    cidrMask: 24,
                    name: 'Database',
                    subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
                }
            ],
            enableDnsHostnames: true,
            enableDnsSupport: true,
            natGateways: natGateways
        });

        // Add tags to VPC and subnets
        cdk.Tags.of(vpc).add('Name', `${projectName}-${environment}-vpc`);
        cdk.Tags.of(vpc).add('Environment', environment);
        cdk.Tags.of(vpc).add('Project', projectName);

        // Tag subnets for EKS discovery
        vpc.publicSubnets.forEach((subnet, index) => {
            cdk.Tags.of(subnet).add('Name', `${projectName}-${environment}-public-subnet-${index + 1}`);
            cdk.Tags.of(subnet).add('kubernetes.io/role/elb', '1');
            cdk.Tags.of(subnet).add(`kubernetes.io/cluster/${projectName}-${environment}`, 'shared');
        });

        vpc.privateSubnets.forEach((subnet, index) => {
            cdk.Tags.of(subnet).add('Name', `${projectName}-${environment}-private-subnet-${index + 1}`);
            cdk.Tags.of(subnet).add('kubernetes.io/role/internal-elb', '1');
            cdk.Tags.of(subnet).add(`kubernetes.io/cluster/${projectName}-${environment}`, 'shared');
        });

        vpc.isolatedSubnets.forEach((subnet, index) => {
            cdk.Tags.of(subnet).add('Name', `${projectName}-${environment}-database-subnet-${index + 1}`);
        });

        return vpc;
    }

    private createSecurityGroups(vpc: ec2.Vpc, projectName: string, environment: string): {
        eksSecurityGroup: ec2.SecurityGroup;
        rdsSecurityGroup: ec2.SecurityGroup;
        mskSecurityGroup: ec2.SecurityGroup;
        albSecurityGroup: ec2.SecurityGroup;
    } {
        // Application Load Balancer Security Group
        const albSecurityGroup = new ec2.SecurityGroup(this, 'ALBSecurityGroup', {
            vpc,
            securityGroupName: `${projectName}-${environment}-alb-sg`,
            description: 'Security group for Application Load Balancer',
            allowAllOutbound: true
        });

        albSecurityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(80), 'Allow HTTP traffic from internet');
        albSecurityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(443), 'Allow HTTPS traffic from internet');

        // EKS Security Group
        const eksSecurityGroup = new ec2.SecurityGroup(this, 'EKSSecurityGroup', {
            vpc,
            securityGroupName: `${projectName}-${environment}-eks-sg`,
            description: 'Security group for EKS cluster and worker nodes',
            allowAllOutbound: true
        });

        eksSecurityGroup.addIngressRule(albSecurityGroup, ec2.Port.allTraffic(), 'Allow traffic from ALB');
        eksSecurityGroup.addIngressRule(eksSecurityGroup, ec2.Port.allTraffic(), 'Allow EKS nodes to communicate');

        // RDS Security Group
        const rdsSecurityGroup = new ec2.SecurityGroup(this, 'RDSSecurityGroup', {
            vpc,
            securityGroupName: `${projectName}-${environment}-rds-sg`,
            description: 'Security group for RDS PostgreSQL database',
            allowAllOutbound: false
        });

        rdsSecurityGroup.addIngressRule(eksSecurityGroup, ec2.Port.tcp(5432), 'Allow PostgreSQL traffic from EKS');

        // MSK Security Group
        const mskSecurityGroup = new ec2.SecurityGroup(this, 'MSKSecurityGroup', {
            vpc,
            securityGroupName: `${projectName}-${environment}-msk-sg`,
            description: 'Security group for Amazon MSK cluster',
            allowAllOutbound: false
        });

        mskSecurityGroup.addIngressRule(eksSecurityGroup, ec2.Port.tcp(9092), 'Allow Kafka traffic from EKS');
        mskSecurityGroup.addIngressRule(eksSecurityGroup, ec2.Port.tcp(9094), 'Allow Kafka TLS traffic from EKS');

        // Add tags to security groups
        const securityGroups = [
            { sg: albSecurityGroup, name: 'alb' },
            { sg: eksSecurityGroup, name: 'eks' },
            { sg: rdsSecurityGroup, name: 'rds' },
            { sg: mskSecurityGroup, name: 'msk' }
        ];

        securityGroups.forEach(({ sg, name }) => {
            cdk.Tags.of(sg).add('Name', `${projectName}-${environment}-${name}-sg`);
            cdk.Tags.of(sg).add('Environment', environment);
            cdk.Tags.of(sg).add('Project', projectName);
            cdk.Tags.of(sg).add('Service', name.toUpperCase());
        });

        return { eksSecurityGroup, rdsSecurityGroup, mskSecurityGroup, albSecurityGroup };
    }

    private lookupHostedZone(domain: string): route53.IHostedZone {
        const domainParts = domain.split('.');
        const rootDomain = domainParts.length > 2 ? domainParts.slice(-2).join('.') : domain;
        return route53.HostedZone.fromLookup(this, 'HostedZone', { domainName: rootDomain });
    }

    private createCertificate(
        domain: string,
        hostedZone: route53.IHostedZone,
        projectName: string,
        environment: string
    ): certificatemanager.Certificate {
        return new certificatemanager.Certificate(this, 'Certificate', {
            certificateName: `${projectName}-${environment}-certificate`,
            domainName: domain,
            validation: certificatemanager.CertificateValidation.fromDns(hostedZone),
            subjectAlternativeNames: [`api.${domain}`, `cmc.${domain}`, `shop.${domain}`]
        });
    }

    private createWildcardCertificate(
        domain: string,
        hostedZone: route53.IHostedZone,
        projectName: string,
        environment: string
    ): certificatemanager.Certificate {
        const domainParts = domain.split('.');
        const rootDomain = domainParts.length > 2 ? domainParts.slice(-2).join('.') : domain;
        return new certificatemanager.Certificate(this, 'WildcardCertificate', {
            certificateName: `${projectName}-${environment}-wildcard-certificate`,
            domainName: `*.${rootDomain}`,
            validation: certificatemanager.CertificateValidation.fromDns(hostedZone),
            subjectAlternativeNames: [rootDomain]
        });
    }

    private createApplicationLoadBalancer(
        vpc: ec2.Vpc,
        projectName: string,
        environment: string,
        domain?: string
    ): elbv2.ApplicationLoadBalancer {
        const alb = new elbv2.ApplicationLoadBalancer(this, 'ApplicationLoadBalancer', {
            loadBalancerName: `${projectName}-${environment}-alb`,
            vpc: vpc,
            internetFacing: true,
            securityGroup: this.albSecurityGroup,
            vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
            deletionProtection: environment === 'production'
        });

        const defaultTargetGroup = new elbv2.ApplicationTargetGroup(this, 'DefaultTargetGroup', {
            targetGroupName: `${projectName}-${environment}-tg`,
            port: 8080,
            protocol: elbv2.ApplicationProtocol.HTTP,
            vpc: vpc,
            targetType: elbv2.TargetType.IP,
            healthCheck: {
                enabled: true,
                path: '/actuator/health',
                protocol: elbv2.Protocol.HTTP,
                port: '8080',
                healthyThresholdCount: 2,
                unhealthyThresholdCount: 3,
                timeout: cdk.Duration.seconds(10),
                interval: cdk.Duration.seconds(30),
                healthyHttpCodes: '200'
            }
        });

        // HTTP Listener (redirects to HTTPS)
        alb.addListener('HttpListener', {
            port: 80,
            protocol: elbv2.ApplicationProtocol.HTTP,
            defaultAction: elbv2.ListenerAction.redirect({
                protocol: 'HTTPS',
                port: '443',
                permanent: true
            })
        });

        // HTTPS Listener (if certificates are available)
        if (domain && this.certificate) {
            alb.addListener('HttpsListener', {
                port: 443,
                protocol: elbv2.ApplicationProtocol.HTTPS,
                certificates: [this.certificate],
                defaultTargetGroups: [defaultTargetGroup],
                sslPolicy: elbv2.SslPolicy.TLS12_EXT
            });

            // Create DNS records
            if (this.hostedZone) {
                new route53.ARecord(this, 'AliasRecord', {
                    zone: this.hostedZone,
                    recordName: domain,
                    target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(alb))
                });
            }
        } else {
            alb.addListener('HttpOnlyListener', {
                port: 80,
                protocol: elbv2.ApplicationProtocol.HTTP,
                defaultTargetGroups: [defaultTargetGroup]
            });
        }

        cdk.Tags.of(alb).add('Name', `${projectName}-${environment}-alb`);
        cdk.Tags.of(alb).add('Environment', environment);
        cdk.Tags.of(alb).add('Project', projectName);

        return alb;
    }

    private createKMSKey(projectName: string, environment: string): kms.Key {
        const key = new kms.Key(this, 'KMSKey', {
            alias: `${projectName}-${environment}-key`,
            description: `KMS key for ${projectName} ${environment} environment encryption`,
            enableKeyRotation: true,
            removalPolicy: environment === 'production' ? cdk.RemovalPolicy.RETAIN : cdk.RemovalPolicy.DESTROY
        });

        // Add tags
        cdk.Tags.of(key).add('Name', `${projectName}-${environment}-kms-key`);
        cdk.Tags.of(key).add('Environment', environment);
        cdk.Tags.of(key).add('Project', projectName);
        cdk.Tags.of(key).add('Service', 'Encryption');

        return key;
    }

    private createOutputs(projectName: string, environment: string, domain?: string): void {
        // VPC Outputs
        new cdk.CfnOutput(this, 'VpcId', {
            value: this.vpc.vpcId,
            description: 'VPC ID for the GenAI Demo infrastructure',
            exportName: `${projectName}-${environment}-vpc-id`
        });

        new cdk.CfnOutput(this, 'PublicSubnetIds', {
            value: this.vpc.publicSubnets.map(subnet => subnet.subnetId).join(','),
            description: 'Public subnet IDs',
            exportName: `${projectName}-${environment}-public-subnet-ids`
        });

        new cdk.CfnOutput(this, 'PrivateSubnetIds', {
            value: this.vpc.privateSubnets.map(subnet => subnet.subnetId).join(','),
            description: 'Private subnet IDs',
            exportName: `${projectName}-${environment}-private-subnet-ids`
        });

        // Security Group Outputs
        new cdk.CfnOutput(this, 'EksSecurityGroupId', {
            value: this.eksSecurityGroup.securityGroupId,
            description: 'EKS Security Group ID',
            exportName: `${projectName}-${environment}-eks-sg-id`
        });

        new cdk.CfnOutput(this, 'AlbSecurityGroupId', {
            value: this.albSecurityGroup.securityGroupId,
            description: 'ALB Security Group ID',
            exportName: `${projectName}-${environment}-alb-sg-id`
        });

        // Load Balancer Outputs
        new cdk.CfnOutput(this, 'LoadBalancerArn', {
            value: this.loadBalancer.loadBalancerArn,
            description: 'Application Load Balancer ARN',
            exportName: `${projectName}-${environment}-alb-arn`
        });

        new cdk.CfnOutput(this, 'LoadBalancerDnsName', {
            value: this.loadBalancer.loadBalancerDnsName,
            description: 'Application Load Balancer DNS name',
            exportName: `${projectName}-${environment}-alb-dns-name`
        });

        // Certificate Outputs (if available)
        if (domain && this.certificate) {
            new cdk.CfnOutput(this, 'CertificateArn', {
                value: this.certificate.certificateArn,
                description: 'ACM Certificate ARN for domain',
                exportName: `${projectName}-${environment}-certificate-arn`
            });
        }

        if (this.wildcardCertificate) {
            new cdk.CfnOutput(this, 'WildcardCertificateArn', {
                value: this.wildcardCertificate.certificateArn,
                description: 'ACM Wildcard Certificate ARN',
                exportName: `${projectName}-${environment}-wildcard-certificate-arn`
            });
        }

        if (this.hostedZone) {
            new cdk.CfnOutput(this, 'HostedZoneId', {
                value: this.hostedZone.hostedZoneId,
                description: 'Route 53 Hosted Zone ID',
                exportName: `${projectName}-${environment}-hosted-zone-id`
            });
        }

        // KMS Key outputs
        new cdk.CfnOutput(this, 'KMSKeyId', {
            value: this.kmsKey.keyId,
            description: 'KMS Key ID for encryption',
            exportName: `${projectName}-${environment}-kms-key-id`
        });

        new cdk.CfnOutput(this, 'KMSKeyArn', {
            value: this.kmsKey.keyArn,
            description: 'KMS Key ARN for encryption',
            exportName: `${projectName}-${environment}-kms-key-arn`
        });

        // RDS Database outputs (from nested stack)
        new cdk.CfnOutput(this, 'RdsStackName', {
            value: this.rdsStack.stackName,
            description: 'RDS stack name',
            exportName: `${projectName}-${environment}-rds-stack-name`
        });

        // MSK Cluster outputs (from nested stack)
        new cdk.CfnOutput(this, 'MSKStackName', {
            value: this.mskStack.stackName,
            description: 'MSK stack name',
            exportName: `${projectName}-${environment}-msk-stack-name`
        });

        // Analytics Stack outputs (from nested stack)
        new cdk.CfnOutput(this, 'AnalyticsStackName', {
            value: this.analyticsStack.stackName,
            description: 'Analytics stack name',
            exportName: `${projectName}-${environment}-analytics-stack-name`
        });

        // Alerting Stack outputs (from nested stack)
        new cdk.CfnOutput(this, 'AlertingStackName', {
            value: this.alertingStack.stackName,
            description: 'Alerting stack name',
            exportName: `${projectName}-${environment}-alerting-stack-name`
        });
    }

    // Placeholder methods for future implementation
    /*
    private createMSKCluster(vpc: ec2.Vpc, projectName: string, environment: string): msk.Cluster {
      // To be implemented in task 8
    }
    */
}