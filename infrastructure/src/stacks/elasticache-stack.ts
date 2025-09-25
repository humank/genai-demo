import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as elasticache from 'aws-cdk-lib/aws-elasticache';
import * as logs from 'aws-cdk-lib/aws-logs';
import { Construct } from 'constructs';

export interface ElastiCacheStackProps extends cdk.StackProps {
  vpc: ec2.IVpc;
  region: string;
  environment: string;
}

/**
 * ElastiCache Redis Cluster Stack for Distributed Locking
 * 
 * This stack creates a Multi-AZ Redis Cluster with:
 * - High availability across multiple AZs
 * - Automatic failover
 * - Backup and restore capabilities
 * - Security groups and subnet groups
 * - CloudWatch monitoring and logging
 */
export class ElastiCacheStack extends cdk.Stack {
  public readonly redisCluster: elasticache.CfnReplicationGroup;
  public readonly redisSecurityGroup: ec2.SecurityGroup;
  public readonly redisSubnetGroup: elasticache.CfnSubnetGroup;

  constructor(scope: Construct, id: string, props: ElastiCacheStackProps) {
    super(scope, id, props);

    // Create security group for Redis cluster
    this.redisSecurityGroup = new ec2.SecurityGroup(this, 'RedisSecurityGroup', {
      vpc: props.vpc,
      description: 'Security group for ElastiCache Redis cluster',
      allowAllOutbound: false,
    });

    // Allow Redis port access from VPC
    this.redisSecurityGroup.addIngressRule(
      ec2.Peer.ipv4(props.vpc.vpcCidrBlock),
      ec2.Port.tcp(6379),
      'Allow Redis access from VPC'
    );

    // Create subnet group for Redis cluster
    this.redisSubnetGroup = new elasticache.CfnSubnetGroup(this, 'RedisSubnetGroup', {
      description: 'Subnet group for ElastiCache Redis cluster',
      subnetIds: props.vpc.privateSubnets.map(subnet => subnet.subnetId),
      cacheSubnetGroupName: `redis-subnet-group-${props.environment}-${props.region}`,
    });

    // Create parameter group for Redis optimization
    const redisParameterGroup = new elasticache.CfnParameterGroup(this, 'RedisParameterGroup', {
      cacheParameterGroupFamily: 'redis7.x',
      description: 'Parameter group for Redis cluster optimization',
      properties: {
        // Memory optimization
        'maxmemory-policy': 'allkeys-lru',
        'timeout': '300',
        // Connection optimization
        'tcp-keepalive': '60',
        'tcp-backlog': '511',
        // Persistence optimization for distributed locks
        'save': '900 1 300 10 60 10000',
        'stop-writes-on-bgsave-error': 'no',
        // Replication optimization
        'repl-backlog-size': '1mb',
        'repl-timeout': '60',
      },
    });

    // Create CloudWatch log group for Redis logs
    const redisLogGroup = new logs.LogGroup(this, 'RedisLogGroup', {
      logGroupName: `/aws/elasticache/redis/${props.environment}-${props.region}`,
      retention: logs.RetentionDays.ONE_MONTH,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });

    // Create Redis replication group (cluster)
    this.redisCluster = new elasticache.CfnReplicationGroup(this, 'RedisCluster', {
      replicationGroupDescription: `Redis cluster for distributed locking - ${props.environment}`,
      replicationGroupId: `redis-cluster-${props.environment}-${props.region}`,
      
      // Node configuration
      cacheNodeType: 'cache.t3.micro', // Start with small instance, can be scaled up
      numCacheClusters: 3, // Multi-AZ deployment with 3 nodes
      
      // Engine configuration
      engine: 'redis',
      engineVersion: '7.0',
      port: 6379,
      
      // High availability configuration
      multiAzEnabled: true,
      automaticFailoverEnabled: true,
      
      // Security configuration
      atRestEncryptionEnabled: true,
      transitEncryptionEnabled: true,
      authToken: 'temp-auth-token-change-in-production', // TODO: Use AWS Secrets Manager in production
      
      // Network configuration
      cacheSubnetGroupName: this.redisSubnetGroup.ref,
      securityGroupIds: [this.redisSecurityGroup.securityGroupId],
      
      // Parameter group
      cacheParameterGroupName: redisParameterGroup.ref,
      
      // Backup configuration
      snapshotRetentionLimit: 7,
      snapshotWindow: '03:00-05:00',
      preferredMaintenanceWindow: 'sun:05:00-sun:07:00',
      
      // Logging configuration
      logDeliveryConfigurations: [
        {
          destinationType: 'cloudwatch-logs',
          destinationDetails: {
            cloudWatchLogsDetails: {
              logGroup: redisLogGroup.logGroupName,
            },
          },
          logFormat: 'json',
          logType: 'slow-log',
        },
      ],
      
      // Notification configuration
      notificationTopicArn: undefined, // Will be added when SNS topic is available
      
      // Tags
      tags: [
        {
          key: 'Environment',
          value: props.environment,
        },
        {
          key: 'Purpose',
          value: 'DistributedLocking',
        },
        {
          key: 'Region',
          value: props.region,
        },
        {
          key: 'Stack',
          value: 'ElastiCache',
        },
      ],
    });

    // Add dependency to ensure subnet group is created first
    this.redisCluster.addDependency(this.redisSubnetGroup);

    // Output important values
    new cdk.CfnOutput(this, 'RedisClusterEndpoint', {
      value: this.redisCluster.attrPrimaryEndPointAddress,
      description: 'Redis cluster primary endpoint',
      exportName: `${props.environment}-redis-cluster-endpoint-${props.region}`,
    });

    new cdk.CfnOutput(this, 'RedisClusterPort', {
      value: this.redisCluster.attrPrimaryEndPointPort.toString(),
      description: 'Redis cluster port',
      exportName: `${props.environment}-redis-cluster-port-${props.region}`,
    });

    new cdk.CfnOutput(this, 'RedisSecurityGroupId', {
      value: this.redisSecurityGroup.securityGroupId,
      description: 'Redis security group ID',
      exportName: `${props.environment}-redis-security-group-${props.region}`,
    });
  }
}