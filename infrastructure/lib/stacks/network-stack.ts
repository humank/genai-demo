import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as logs from 'aws-cdk-lib/aws-logs';
import { Construct } from 'constructs';
import {
    CostOptimizationManager,
    EnvironmentConfigManager,
    ParameterStoreManager
} from '../config/index';

export interface NetworkStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
}

export class NetworkStack extends cdk.Stack {
    public readonly vpc: ec2.Vpc;
    public readonly eksSecurityGroup: ec2.SecurityGroup;
    public readonly rdsSecurityGroup: ec2.SecurityGroup;
    public readonly mskSecurityGroup: ec2.SecurityGroup;
    public readonly albSecurityGroup: ec2.SecurityGroup;
    public readonly configManager: EnvironmentConfigManager;
    public readonly parameterStore: ParameterStoreManager;
    public readonly costOptimization: CostOptimizationManager;

    constructor(scope: Construct, id: string, props: NetworkStackProps) {
        super(scope, id, props);

        const { environment, projectName } = props;

        // Initialize configuration managers
        this.configManager = new EnvironmentConfigManager(this);
        this.parameterStore = new ParameterStoreManager(this, this.configManager);
        this.costOptimization = new CostOptimizationManager(this, this.configManager);

        // Apply standardized tags using configuration manager
        this.configManager.applyTags(this, {
            Component: 'Network',
            StackType: 'NetworkInfrastructure'
        });

        // Create VPC with enhanced configuration
        this.vpc = this.createVPC();

        // Create Security Groups with standardized naming
        const securityGroups = this.createSecurityGroups(this.vpc);
        this.eksSecurityGroup = securityGroups.eksSecurityGroup;
        this.rdsSecurityGroup = securityGroups.rdsSecurityGroup;
        this.mskSecurityGroup = securityGroups.mskSecurityGroup;
        this.albSecurityGroup = securityGroups.albSecurityGroup;

        // Create Parameter Store configuration
        this.parameterStore.createRuntimeParameters();

        // Create cost monitoring
        this.costOptimization.createCostMonitoringAlarms();

        // Export VPC and Security Group information for cross-stack references
        this.createOutputs();
    }

    private createVPC(): ec2.Vpc {
        // Get configuration from configuration manager
        const config = this.configManager.getConfig();
        const backupConfig = this.configManager.getBackupRetentionConfig();

        // Create VPC Flow Logs group with enhanced configuration
        let flowLogsGroup: logs.LogGroup | undefined;
        const networkingConfig = this.node.tryGetContext('genai-demo:networking') || {};

        if (networkingConfig['enable-vpc-flow-logs']) {
            const logGroupName = this.configManager.generateResourceName('vpc-flowlogs');

            flowLogsGroup = new logs.LogGroup(this, 'VPCFlowLogsGroup', {
                logGroupName: `/aws/vpc/flowlogs/${logGroupName}`,
                retention: this.getLogRetention(backupConfig.logRetention),
                removalPolicy: this.configManager.getRemovalPolicy()
            });

            // Apply tags to flow logs group
            this.configManager.applyTags(flowLogsGroup, {
                LogType: 'VPCFlowLogs',
                RetentionDays: backupConfig.logRetention.toString()
            });
        }

        const vpc = new ec2.Vpc(this, 'GenAIDemoVPC', {
            vpcName: this.configManager.generateResourceName('vpc'),
            maxAzs: config.availabilityZones,
            ipAddresses: ec2.IpAddresses.cidr(config.vpcCidr),
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
            enableDnsHostnames: networkingConfig['enable-dns-hostnames'] ?? true,
            enableDnsSupport: networkingConfig['enable-dns-support'] ?? true,
            natGateways: config.natGateways,
            // Configure NAT Gateway provider for cost optimization
            natGatewayProvider: config.environment === 'production'
                ? ec2.NatProvider.gateway()
                : ec2.NatProvider.gateway(),
            // Enable VPC Flow Logs for security monitoring
            flowLogs: flowLogsGroup ? {
                'VPCFlowLogs': {
                    destination: ec2.FlowLogDestination.toCloudWatchLogs(flowLogsGroup),
                    trafficType: ec2.FlowLogTrafficType.ALL
                }
            } : undefined
        });

        // Add standardized tags to VPC
        this.configManager.applyTags(vpc, {
            ResourceType: 'VPC',
            CIDR: config.vpcCidr,
            NATGateways: config.natGateways.toString()
        });

        // Tag subnets for EKS discovery with standardized naming
        vpc.publicSubnets.forEach((subnet, index) => {
            const subnetName = this.configManager.generateResourceName('public-subnet', (index + 1).toString());
            this.configManager.applyTags(subnet, {
                Name: subnetName,
                SubnetType: 'Public',
                'kubernetes.io/role/elb': '1',
                [`kubernetes.io/cluster/${config.resourcePrefix}`]: 'shared'
            });
        });

        vpc.privateSubnets.forEach((subnet, index) => {
            const subnetName = this.configManager.generateResourceName('private-subnet', (index + 1).toString());
            this.configManager.applyTags(subnet, {
                Name: subnetName,
                SubnetType: 'Private',
                'kubernetes.io/role/internal-elb': '1',
                [`kubernetes.io/cluster/${config.resourcePrefix}`]: 'shared'
            });
        });

        vpc.isolatedSubnets.forEach((subnet, index) => {
            const subnetName = this.configManager.generateResourceName('database-subnet', (index + 1).toString());
            this.configManager.applyTags(subnet, {
                Name: subnetName,
                SubnetType: 'Database'
            });
        });

        return vpc;
    }

    private createSecurityGroups(vpc: ec2.Vpc): {
        eksSecurityGroup: ec2.SecurityGroup;
        rdsSecurityGroup: ec2.SecurityGroup;
        mskSecurityGroup: ec2.SecurityGroup;
        albSecurityGroup: ec2.SecurityGroup;
    } {
        // Application Load Balancer Security Group with standardized naming
        const albSecurityGroup = new ec2.SecurityGroup(this, 'ALBSecurityGroup', {
            vpc,
            securityGroupName: this.configManager.generateResourceName('alb-sg'),
            description: 'Security group for Application Load Balancer',
            allowAllOutbound: true
        });

        // Allow HTTP and HTTPS traffic from internet
        albSecurityGroup.addIngressRule(
            ec2.Peer.anyIpv4(),
            ec2.Port.tcp(80),
            'Allow HTTP traffic from internet'
        );
        albSecurityGroup.addIngressRule(
            ec2.Peer.anyIpv4(),
            ec2.Port.tcp(443),
            'Allow HTTPS traffic from internet'
        );

        // Allow health check traffic (typically on port 8080 for Spring Boot)
        albSecurityGroup.addIngressRule(
            ec2.Peer.anyIpv4(),
            ec2.Port.tcp(8080),
            'Allow health check traffic'
        );

        // EKS Security Group with standardized naming
        const eksSecurityGroup = new ec2.SecurityGroup(this, 'EKSSecurityGroup', {
            vpc,
            securityGroupName: this.configManager.generateResourceName('eks-sg'),
            description: 'Security group for EKS cluster and worker nodes',
            allowAllOutbound: true
        });

        // Allow traffic from ALB to EKS
        eksSecurityGroup.addIngressRule(
            albSecurityGroup,
            ec2.Port.allTraffic(),
            'Allow traffic from ALB'
        );

        // Allow EKS nodes to communicate with each other
        eksSecurityGroup.addIngressRule(
            eksSecurityGroup,
            ec2.Port.allTraffic(),
            'Allow EKS nodes to communicate with each other'
        );

        // Allow HTTPS traffic for EKS API server
        eksSecurityGroup.addIngressRule(
            ec2.Peer.anyIpv4(),
            ec2.Port.tcp(443),
            'Allow HTTPS traffic to EKS API server'
        );

        // Allow Spring Boot application port
        eksSecurityGroup.addIngressRule(
            albSecurityGroup,
            ec2.Port.tcp(8080),
            'Allow Spring Boot application traffic from ALB'
        );

        // Allow NodePort range for Kubernetes services
        eksSecurityGroup.addIngressRule(
            eksSecurityGroup,
            ec2.Port.tcpRange(30000, 32767),
            'Allow NodePort range for Kubernetes services'
        );

        // RDS Security Group with standardized naming
        const rdsSecurityGroup = new ec2.SecurityGroup(this, 'RDSSecurityGroup', {
            vpc,
            securityGroupName: this.configManager.generateResourceName('rds-sg'),
            description: 'Security group for RDS PostgreSQL database',
            allowAllOutbound: false
        });

        // Allow PostgreSQL traffic from EKS
        rdsSecurityGroup.addIngressRule(
            eksSecurityGroup,
            ec2.Port.tcp(5432),
            'Allow PostgreSQL traffic from EKS'
        );

        // MSK Security Group with standardized naming
        const mskSecurityGroup = new ec2.SecurityGroup(this, 'MSKSecurityGroup', {
            vpc,
            securityGroupName: this.configManager.generateResourceName('msk-sg'),
            description: 'Security group for Amazon MSK cluster',
            allowAllOutbound: false
        });

        // Allow Kafka traffic from EKS
        mskSecurityGroup.addIngressRule(
            eksSecurityGroup,
            ec2.Port.tcp(9092),
            'Allow Kafka plaintext traffic from EKS'
        );
        mskSecurityGroup.addIngressRule(
            eksSecurityGroup,
            ec2.Port.tcp(9094),
            'Allow Kafka TLS traffic from EKS'
        );
        mskSecurityGroup.addIngressRule(
            eksSecurityGroup,
            ec2.Port.tcp(9096),
            'Allow Kafka SASL_SSL traffic from EKS'
        );
        mskSecurityGroup.addIngressRule(
            eksSecurityGroup,
            ec2.Port.tcp(2181),
            'Allow Zookeeper traffic from EKS'
        );
        mskSecurityGroup.addIngressRule(
            eksSecurityGroup,
            ec2.Port.tcp(2182),
            'Allow Zookeeper TLS traffic from EKS'
        );

        // Allow MSK brokers to communicate with each other
        mskSecurityGroup.addIngressRule(
            mskSecurityGroup,
            ec2.Port.allTraffic(),
            'Allow MSK brokers to communicate with each other'
        );

        // Add standardized tags to security groups
        const securityGroups = [
            { sg: albSecurityGroup, service: 'ALB', type: 'LoadBalancer' },
            { sg: eksSecurityGroup, service: 'EKS', type: 'Compute' },
            { sg: rdsSecurityGroup, service: 'RDS', type: 'Database' },
            { sg: mskSecurityGroup, service: 'MSK', type: 'Messaging' }
        ];

        securityGroups.forEach(({ sg, service, type }) => {
            this.configManager.applyTags(sg, {
                Service: service,
                ResourceType: 'SecurityGroup',
                ServiceType: type
            });
        });

        return {
            eksSecurityGroup,
            rdsSecurityGroup,
            mskSecurityGroup,
            albSecurityGroup
        };
    }

    private createOutputs(): void {
        // Get configuration from configuration manager
        const config = this.configManager.getConfig();
        const networkingConfig = this.node.tryGetContext('genai-demo:networking') || {};

        // VPC Outputs
        new cdk.CfnOutput(this, 'VpcId', {
            value: this.vpc.vpcId,
            description: 'VPC ID for the GenAI Demo infrastructure',
            exportName: `${config.projectName}-${config.environment}-vpc-id`
        });

        new cdk.CfnOutput(this, 'VpcCidr', {
            value: this.vpc.vpcCidrBlock,
            description: 'VPC CIDR block',
            exportName: `${config.projectName}-${config.environment}-vpc-cidr`
        });

        new cdk.CfnOutput(this, 'PublicSubnetIds', {
            value: this.vpc.publicSubnets.map(subnet => subnet.subnetId).join(','),
            description: 'Public subnet IDs',
            exportName: `${config.projectName}-${config.environment}-public-subnet-ids`
        });

        new cdk.CfnOutput(this, 'PrivateSubnetIds', {
            value: this.vpc.privateSubnets.map(subnet => subnet.subnetId).join(','),
            description: 'Private subnet IDs',
            exportName: `${config.projectName}-${config.environment}-private-subnet-ids`
        });

        new cdk.CfnOutput(this, 'DatabaseSubnetIds', {
            value: this.vpc.isolatedSubnets.map(subnet => subnet.subnetId).join(','),
            description: 'Database subnet IDs',
            exportName: `${config.projectName}-${config.environment}-database-subnet-ids`
        });

        // Security Group Outputs
        new cdk.CfnOutput(this, 'EksSecurityGroupId', {
            value: this.eksSecurityGroup.securityGroupId,
            description: 'EKS Security Group ID',
            exportName: `${config.projectName}-${config.environment}-eks-sg-id`
        });

        new cdk.CfnOutput(this, 'RdsSecurityGroupId', {
            value: this.rdsSecurityGroup.securityGroupId,
            description: 'RDS Security Group ID',
            exportName: `${config.projectName}-${config.environment}-rds-sg-id`
        });

        new cdk.CfnOutput(this, 'MskSecurityGroupId', {
            value: this.mskSecurityGroup.securityGroupId,
            description: 'MSK Security Group ID',
            exportName: `${config.projectName}-${config.environment}-msk-sg-id`
        });

        new cdk.CfnOutput(this, 'AlbSecurityGroupId', {
            value: this.albSecurityGroup.securityGroupId,
            description: 'ALB Security Group ID',
            exportName: `${config.projectName}-${config.environment}-alb-sg-id`
        });

        // Network Configuration Outputs
        new cdk.CfnOutput(this, 'AvailabilityZones', {
            value: this.vpc.availabilityZones.join(','),
            description: 'Availability zones used by the VPC',
            exportName: `${config.projectName}-${config.environment}-availability-zones`
        });

        new cdk.CfnOutput(this, 'NatGatewayCount', {
            value: config.natGateways.toString(),
            description: 'Number of NAT Gateways deployed',
            exportName: `${config.projectName}-${config.environment}-nat-gateway-count`
        });

        new cdk.CfnOutput(this, 'VpcFlowLogsEnabled', {
            value: networkingConfig['enable-vpc-flow-logs'] ? 'true' : 'false',
            description: 'Whether VPC Flow Logs are enabled',
            exportName: `${config.projectName}-${config.environment}-vpc-flow-logs-enabled`
        });

        // Cost Optimization Outputs
        this.costOptimization.createCostOptimizationOutputs();

        // Parameter Store Outputs
        this.parameterStore.createParameterOutputs();
    }

    /**
     * Convert retention days to CloudWatch retention enum
     */
    private getLogRetention(days: number): logs.RetentionDays {
        const retentionMap: Record<number, logs.RetentionDays> = {
            1: logs.RetentionDays.ONE_DAY,
            3: logs.RetentionDays.THREE_DAYS,
            5: logs.RetentionDays.FIVE_DAYS,
            7: logs.RetentionDays.ONE_WEEK,
            14: logs.RetentionDays.TWO_WEEKS,
            30: logs.RetentionDays.ONE_MONTH,
            60: logs.RetentionDays.TWO_MONTHS,
            90: logs.RetentionDays.THREE_MONTHS,
            120: logs.RetentionDays.FOUR_MONTHS,
            150: logs.RetentionDays.FIVE_MONTHS,
            180: logs.RetentionDays.SIX_MONTHS,
            365: logs.RetentionDays.ONE_YEAR,
            400: logs.RetentionDays.THIRTEEN_MONTHS,
            545: logs.RetentionDays.EIGHTEEN_MONTHS,
            731: logs.RetentionDays.TWO_YEARS,
            1827: logs.RetentionDays.FIVE_YEARS,
            3653: logs.RetentionDays.TEN_YEARS
        };

        return retentionMap[days] || logs.RetentionDays.ONE_WEEK;
    }
}