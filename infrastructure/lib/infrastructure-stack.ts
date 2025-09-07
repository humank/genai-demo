import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as eks from 'aws-cdk-lib/aws-eks';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as msk from 'aws-cdk-lib/aws-msk';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as certificatemanager from 'aws-cdk-lib/aws-certificatemanager';
import * as route53 from 'aws-cdk-lib/aws-route53';
import { Construct } from 'constructs';

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
  public readonly hostedZone: route53.IHostedZone;
  public readonly certificate: certificatemanager.Certificate;
  public readonly wildcardCertificate: certificatemanager.Certificate;
  public readonly eksCluster: eks.Cluster;
  public readonly rdsDatabase: rds.DatabaseInstance;
  public readonly mskCluster: msk.CfnCluster;
  public readonly loadBalancer: elbv2.ApplicationLoadBalancer;

  constructor(scope: Construct, id: string, props: GenAIDemoInfrastructureStackProps) {
    super(scope, id, props);

    // Get context values
    const environment = props.environment || this.node.tryGetContext('genai-demo:environment') || 'development';
    const projectName = props.projectName || this.node.tryGetContext('genai-demo:project-name') || 'genai-demo';
    const domain = props.domain || this.node.tryGetContext('genai-demo:domain');
    
    // Get environment-specific configuration for outputs
    const envConfig = this.node.tryGetContext('genai-demo:environments')?.[environment] || {};
    const networkingConfig = this.node.tryGetContext('genai-demo:networking') || {};
    const natGateways = envConfig['nat-gateways'] || (environment === 'production' ? 3 : 1);

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

    // Placeholder for future infrastructure components
    // These will be implemented in subsequent tasks:
    // - this.eksCluster = this.createEKSCluster(this.vpc, projectName, environment);
    // - this.rdsDatabase = this.createRDSDatabase(this.vpc, projectName, environment);
    // - this.mskCluster = this.createMSKCluster(this.vpc, projectName, environment);
    // - this.loadBalancer = this.createApplicationLoadBalancer(this.vpc, projectName, environment);

    // Output important values
    new cdk.CfnOutput(this, 'VpcId', {
      value: this.vpc.vpcId,
      description: 'VPC ID for the GenAI Demo infrastructure',
      exportName: `${projectName}-${environment}-vpc-id`
    });

    new cdk.CfnOutput(this, 'VpcCidr', {
      value: this.vpc.vpcCidrBlock,
      description: 'VPC CIDR block',
      exportName: `${projectName}-${environment}-vpc-cidr`
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

    new cdk.CfnOutput(this, 'DatabaseSubnetIds', {
      value: this.vpc.isolatedSubnets.map(subnet => subnet.subnetId).join(','),
      description: 'Database subnet IDs',
      exportName: `${projectName}-${environment}-database-subnet-ids`
    });

    new cdk.CfnOutput(this, 'EksSecurityGroupId', {
      value: this.eksSecurityGroup.securityGroupId,
      description: 'EKS Security Group ID',
      exportName: `${projectName}-${environment}-eks-sg-id`
    });

    new cdk.CfnOutput(this, 'RdsSecurityGroupId', {
      value: this.rdsSecurityGroup.securityGroupId,
      description: 'RDS Security Group ID',
      exportName: `${projectName}-${environment}-rds-sg-id`
    });

    new cdk.CfnOutput(this, 'MskSecurityGroupId', {
      value: this.mskSecurityGroup.securityGroupId,
      description: 'MSK Security Group ID',
      exportName: `${projectName}-${environment}-msk-sg-id`
    });

    new cdk.CfnOutput(this, 'AlbSecurityGroupId', {
      value: this.albSecurityGroup.securityGroupId,
      description: 'ALB Security Group ID',
      exportName: `${projectName}-${environment}-alb-sg-id`
    });

    new cdk.CfnOutput(this, 'Environment', {
      value: environment,
      description: 'Deployment environment'
    });

    new cdk.CfnOutput(this, 'ProjectName', {
      value: projectName,
      description: 'Project name'
    });

    new cdk.CfnOutput(this, 'AvailabilityZones', {
      value: this.vpc.availabilityZones.join(','),
      description: 'Availability zones used by the VPC',
      exportName: `${projectName}-${environment}-availability-zones`
    });

    new cdk.CfnOutput(this, 'NatGatewayCount', {
      value: natGateways.toString(),
      description: 'Number of NAT Gateways deployed',
      exportName: `${projectName}-${environment}-nat-gateway-count`
    });

    new cdk.CfnOutput(this, 'VpcFlowLogsEnabled', {
      value: networkingConfig['enable-vpc-flow-logs'] ? 'true' : 'false',
      description: 'Whether VPC Flow Logs are enabled',
      exportName: `${projectName}-${environment}-vpc-flow-logs-enabled`
    });
  }

  private createVPC(projectName: string, environment: string): ec2.Vpc {
    // Get environment-specific configuration
    const envConfig = this.node.tryGetContext('genai-demo:environments')?.[environment] || {};
    const networkingConfig = this.node.tryGetContext('genai-demo:networking') || {};
    
    const vpcCidr = envConfig['vpc-cidr'] || '10.0.0.0/16';
    const natGateways = envConfig['nat-gateways'] || (environment === 'production' ? 3 : 1);
    const maxAzs = networkingConfig['availability-zones'] || 3;
    
    // Create VPC Flow Logs group first if enabled
    let flowLogsGroup: logs.LogGroup | undefined;
    if (networkingConfig['enable-vpc-flow-logs']) {
      flowLogsGroup = new logs.LogGroup(this, 'VPCFlowLogsGroup', {
        logGroupName: `/aws/vpc/flowlogs/${projectName}-${environment}`,
        retention: environment === 'production' 
          ? logs.RetentionDays.ONE_MONTH 
          : logs.RetentionDays.ONE_WEEK,
        removalPolicy: environment === 'production' 
          ? cdk.RemovalPolicy.RETAIN 
          : cdk.RemovalPolicy.DESTROY
      });
    }
    
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
      enableDnsHostnames: networkingConfig['enable-dns-hostnames'] ?? true,
      enableDnsSupport: networkingConfig['enable-dns-support'] ?? true,
      natGateways: natGateways,
      // Configure NAT Gateway provider for cost optimization
      natGatewayProvider: environment === 'production' 
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

    // EKS Security Group
    const eksSecurityGroup = new ec2.SecurityGroup(this, 'EKSSecurityGroup', {
      vpc,
      securityGroupName: `${projectName}-${environment}-eks-sg`,
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

    // RDS Security Group
    const rdsSecurityGroup = new ec2.SecurityGroup(this, 'RDSSecurityGroup', {
      vpc,
      securityGroupName: `${projectName}-${environment}-rds-sg`,
      description: 'Security group for RDS PostgreSQL database',
      allowAllOutbound: false
    });

    // Allow PostgreSQL traffic from EKS
    rdsSecurityGroup.addIngressRule(
      eksSecurityGroup,
      ec2.Port.tcp(5432),
      'Allow PostgreSQL traffic from EKS'
    );

    // MSK Security Group
    const mskSecurityGroup = new ec2.SecurityGroup(this, 'MSKSecurityGroup', {
      vpc,
      securityGroupName: `${projectName}-${environment}-msk-sg`,
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

    return {
      eksSecurityGroup,
      rdsSecurityGroup,
      mskSecurityGroup,
      albSecurityGroup
    };
  }

  // Placeholder methods for future implementation
  // These will be implemented in subsequent tasks

  /*
  private createEKSCluster(vpc: ec2.Vpc, projectName: string, environment: string): eks.Cluster {
    // To be implemented in task 6
  }

  private createRDSDatabase(vpc: ec2.Vpc, projectName: string, environment: string): rds.DatabaseInstance {
    // To be implemented in task 7
  }

  private createMSKCluster(vpc: ec2.Vpc, projectName: string, environment: string): msk.Cluster {
    // To be implemented in task 8
  }

  private createApplicationLoadBalancer(vpc: ec2.Vpc, projectName: string, environment: string): elbv2.ApplicationLoadBalancer {
    // To be implemented in task 5
  }
  */
}
