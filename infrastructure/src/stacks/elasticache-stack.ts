import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as elasticache from 'aws-cdk-lib/aws-elasticache';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as sns from 'aws-cdk-lib/aws-sns';
import { Construct } from 'constructs';

export interface ElastiCacheStackProps extends cdk.StackProps {
  vpc: ec2.IVpc;
  region: string;
  environment: string;
  // Cross-region configuration
  globalDatastoreEnabled?: boolean;
  primaryRegion?: string;
  replicationRegions?: string[];
  globalDatastoreId?: string;
  // Monitoring configuration
  alertingTopic?: sns.ITopic;
}

/**
 * ElastiCache Redis Cluster Stack with Global Datastore Support
 * 
 * This stack creates a Multi-AZ Redis Cluster with:
 * - High availability across multiple AZs
 * - Cross-region replication via Global Datastore
 * - Automatic failover and conflict resolution
 * - Backup and restore capabilities
 * - Security groups and subnet groups
 * - CloudWatch monitoring and alerting
 * - Cache consistency monitoring
 * 
 * Requirements: 4.1.4 - Cross-region cache synchronization
 */
export class ElastiCacheStack extends cdk.Stack {
  public readonly redisCluster: elasticache.CfnReplicationGroup;
  public readonly redisSecurityGroup: ec2.SecurityGroup;
  public readonly redisSubnetGroup: elasticache.CfnSubnetGroup;
  public readonly globalDatastore?: elasticache.CfnGlobalReplicationGroup;

  constructor(scope: Construct, id: string, props: ElastiCacheStackProps) {
    super(scope, id, props);

    // Determine if this is the primary region for Global Datastore
    const isPrimaryRegion = props.primaryRegion === props.region;
    const globalDatastoreEnabled = props.globalDatastoreEnabled ?? false;

    // Create security group for Redis cluster
    this.redisSecurityGroup = new ec2.SecurityGroup(this, 'RedisSecurityGroup', {
      vpc: props.vpc,
      description: 'Security group for ElastiCache Redis cluster with Global Datastore support',
      allowAllOutbound: false,
    });

    // Allow Redis port access from VPC
    this.redisSecurityGroup.addIngressRule(
      ec2.Peer.ipv4(props.vpc.vpcCidrBlock),
      ec2.Port.tcp(6379),
      'Allow Redis access from VPC'
    );

    // Allow cross-region replication if Global Datastore is enabled
    if (globalDatastoreEnabled && props.replicationRegions) {
      // Add rules for cross-region communication (this would typically be handled by AWS internally)
      this.redisSecurityGroup.addIngressRule(
        ec2.Peer.anyIpv4(),
        ec2.Port.tcp(6379),
        'Allow cross-region Redis replication'
      );
    }

    // Create subnet group for Redis cluster
    this.redisSubnetGroup = new elasticache.CfnSubnetGroup(this, 'RedisSubnetGroup', {
      description: 'Subnet group for ElastiCache Redis cluster',
      subnetIds: props.vpc.privateSubnets.map(subnet => subnet.subnetId),
      cacheSubnetGroupName: `redis-subnet-group-${props.environment}-${props.region}`,
    });

    // Create parameter group for Redis optimization with Global Datastore support
    const redisParameterGroup = new elasticache.CfnParameterGroup(this, 'RedisParameterGroup', {
      cacheParameterGroupFamily: 'redis7.x',
      description: 'Parameter group for Redis cluster with Global Datastore optimization',
      properties: {
        // Memory optimization
        'maxmemory-policy': 'allkeys-lru',
        'timeout': '300',
        // Connection optimization
        'tcp-keepalive': '60',
        'tcp-backlog': '511',
        // Persistence optimization for distributed locks and cross-region sync
        'save': globalDatastoreEnabled ? '900 1 300 10 60 10000' : '900 1 300 10 60 10000',
        'stop-writes-on-bgsave-error': 'no',
        // Replication optimization for Global Datastore
        'repl-backlog-size': globalDatastoreEnabled ? '16mb' : '1mb',
        'repl-timeout': '60',
        // Global Datastore specific optimizations
        ...(globalDatastoreEnabled && {
          'replica-read-only': 'no', // Allow writes to replicas in Global Datastore
          'cluster-enabled': 'no', // Global Datastore doesn't support cluster mode
        }),
      },
    });

    // Create CloudWatch log group for Redis logs
    const redisLogGroup = new logs.LogGroup(this, 'RedisLogGroup', {
      logGroupName: `/aws/elasticache/redis/${props.environment}-${props.region}`,
      retention: logs.RetentionDays.ONE_MONTH,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });

    // Create Global Datastore if enabled and this is the primary region
    if (globalDatastoreEnabled && isPrimaryRegion) {
      this.globalDatastore = new elasticache.CfnGlobalReplicationGroup(this, 'RedisGlobalDatastore', {
        globalReplicationGroupDescription: `Global Redis datastore for cross-region caching - ${props.environment}`,
        globalReplicationGroupIdSuffix: `global-redis-${props.environment}`,
        
        // Primary region configuration
        members: [
          {
            replicationGroupId: `redis-cluster-${props.environment}-${props.region}`,
            replicationGroupRegion: props.region,
            role: 'PRIMARY',
          },
        ],
        
        // Engine configuration
        engineVersion: '7.0',
        cacheNodeType: 'cache.t3.micro',
        
        // Security configuration (encryption settings handled at cluster level)
        
        // Automatic failover configuration
        automaticFailoverEnabled: true,
        
        // Regional configurations will be added when secondary regions are deployed
      });
    }

    // Create Redis replication group (cluster)
    this.redisCluster = new elasticache.CfnReplicationGroup(this, 'RedisCluster', {
      replicationGroupDescription: `Redis cluster for distributed locking - ${props.environment}`,
      replicationGroupId: `redis-cluster-${props.environment}-${props.region}`,
      
      // Global Datastore configuration
      ...(globalDatastoreEnabled && props.globalDatastoreId && !isPrimaryRegion && {
        globalReplicationGroupId: props.globalDatastoreId,
      }),
      
      // Node configuration
      cacheNodeType: 'cache.t3.micro', // Start with small instance, can be scaled up
      numCacheClusters: globalDatastoreEnabled ? 2 : 3, // Fewer nodes for Global Datastore to reduce costs
      
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
      authToken: globalDatastoreEnabled ? undefined : 'temp-auth-token-change-in-production', // Global Datastore manages auth
      
      // Network configuration
      cacheSubnetGroupName: this.redisSubnetGroup.ref,
      securityGroupIds: [this.redisSecurityGroup.securityGroupId],
      
      // Parameter group
      cacheParameterGroupName: redisParameterGroup.ref,
      
      // Backup configuration (only for primary region in Global Datastore)
      ...((!globalDatastoreEnabled || isPrimaryRegion) && {
        snapshotRetentionLimit: 7,
        snapshotWindow: '03:00-05:00',
        preferredMaintenanceWindow: 'sun:05:00-sun:07:00',
      }),
      
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
      notificationTopicArn: props.alertingTopic?.topicArn,
      
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
        {
          key: 'GlobalDatastore',
          value: globalDatastoreEnabled ? 'Enabled' : 'Disabled',
        },
        {
          key: 'RegionRole',
          value: isPrimaryRegion ? 'Primary' : 'Secondary',
        },
      ],
    });

    // Add dependency to ensure subnet group is created first
    this.redisCluster.addDependency(this.redisSubnetGroup);

    // Add dependency for Global Datastore if applicable
    if (this.globalDatastore) {
      this.redisCluster.addDependency(this.globalDatastore);
    }

    // Create CloudWatch monitoring and alerting
    this.createMonitoringAndAlerts(props);

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

    // Global Datastore outputs
    if (this.globalDatastore) {
      new cdk.CfnOutput(this, 'GlobalDatastoreId', {
        value: this.globalDatastore.ref,
        description: 'Global Datastore ID',
        exportName: `${props.environment}-redis-global-datastore-id`,
      });
    }
  }

  /**
   * Create CloudWatch monitoring and alerts for Redis cluster and Global Datastore
   */
  private createMonitoringAndAlerts(props: ElastiCacheStackProps): void {
    const clusterName = this.redisCluster.replicationGroupId!;
    const globalDatastoreEnabled = props.globalDatastoreEnabled ?? false;

    // Cache Hit Rate Monitoring
    const cacheHitRateMetric = new cloudwatch.Metric({
      namespace: 'AWS/ElastiCache',
      metricName: 'CacheHitRate',
      dimensionsMap: {
        CacheClusterId: clusterName,
      },
      statistic: 'Average',
      period: cdk.Duration.minutes(5),
    });

    // Cache Hit Rate Alarm (target > 90%)
    new cloudwatch.Alarm(this, 'CacheHitRateAlarm', {
      metric: cacheHitRateMetric,
      threshold: 90,
      comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
      evaluationPeriods: 2,
      datapointsToAlarm: 2,
      alarmDescription: 'Redis cache hit rate is below 90%',
      alarmName: `${props.environment}-redis-cache-hit-rate-${props.region}`,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
      ...(props.alertingTopic && {
        alarmActions: [props.alertingTopic],
        okActions: [props.alertingTopic],
      }),
    });

    // CPU Utilization Monitoring
    const cpuUtilizationMetric = new cloudwatch.Metric({
      namespace: 'AWS/ElastiCache',
      metricName: 'CPUUtilization',
      dimensionsMap: {
        CacheClusterId: clusterName,
      },
      statistic: 'Average',
      period: cdk.Duration.minutes(5),
    });

    // CPU Utilization Alarm
    new cloudwatch.Alarm(this, 'CPUUtilizationAlarm', {
      metric: cpuUtilizationMetric,
      threshold: 80,
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 3,
      datapointsToAlarm: 2,
      alarmDescription: 'Redis CPU utilization is above 80%',
      alarmName: `${props.environment}-redis-cpu-utilization-${props.region}`,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
      ...(props.alertingTopic && {
        alarmActions: [props.alertingTopic],
        okActions: [props.alertingTopic],
      }),
    });

    // Memory Utilization Monitoring
    const memoryUtilizationMetric = new cloudwatch.Metric({
      namespace: 'AWS/ElastiCache',
      metricName: 'DatabaseMemoryUsagePercentage',
      dimensionsMap: {
        CacheClusterId: clusterName,
      },
      statistic: 'Average',
      period: cdk.Duration.minutes(5),
    });

    // Memory Utilization Alarm
    new cloudwatch.Alarm(this, 'MemoryUtilizationAlarm', {
      metric: memoryUtilizationMetric,
      threshold: 85,
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 2,
      datapointsToAlarm: 2,
      alarmDescription: 'Redis memory utilization is above 85%',
      alarmName: `${props.environment}-redis-memory-utilization-${props.region}`,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
      ...(props.alertingTopic && {
        alarmActions: [props.alertingTopic],
        okActions: [props.alertingTopic],
      }),
    });

    // Connection Count Monitoring
    const connectionCountMetric = new cloudwatch.Metric({
      namespace: 'AWS/ElastiCache',
      metricName: 'CurrConnections',
      dimensionsMap: {
        CacheClusterId: clusterName,
      },
      statistic: 'Average',
      period: cdk.Duration.minutes(5),
    });

    // Connection Count Alarm
    new cloudwatch.Alarm(this, 'ConnectionCountAlarm', {
      metric: connectionCountMetric,
      threshold: 1000,
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 2,
      datapointsToAlarm: 2,
      alarmDescription: 'Redis connection count is above 1000',
      alarmName: `${props.environment}-redis-connection-count-${props.region}`,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
      ...(props.alertingTopic && {
        alarmActions: [props.alertingTopic],
        okActions: [props.alertingTopic],
      }),
    });

    // Global Datastore specific monitoring
    if (globalDatastoreEnabled) {
      // Replication Lag Monitoring
      const replicationLagMetric = new cloudwatch.Metric({
        namespace: 'AWS/ElastiCache',
        metricName: 'ReplicationLag',
        dimensionsMap: {
          CacheClusterId: clusterName,
        },
        statistic: 'Average',
        period: cdk.Duration.minutes(1),
      });

      // Replication Lag Alarm (target < 1 second)
      new cloudwatch.Alarm(this, 'ReplicationLagAlarm', {
        metric: replicationLagMetric,
        threshold: 1000, // 1 second in milliseconds
        comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
        evaluationPeriods: 3,
        datapointsToAlarm: 2,
        alarmDescription: 'Redis cross-region replication lag is above 1 second',
        alarmName: `${props.environment}-redis-replication-lag-${props.region}`,
        treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        ...(props.alertingTopic && {
          alarmActions: [props.alertingTopic],
          okActions: [props.alertingTopic],
        }),
      });

      // Cross-Region Consistency Check (custom metric)
      const consistencyCheckMetric = new cloudwatch.Metric({
        namespace: 'Custom/ElastiCache',
        metricName: 'CrossRegionConsistencyCheck',
        dimensionsMap: {
          GlobalDatastoreId: this.globalDatastore?.ref || 'unknown',
          Region: props.region,
        },
        statistic: 'Average',
        period: cdk.Duration.minutes(5),
      });

      // Consistency Check Alarm
      new cloudwatch.Alarm(this, 'ConsistencyCheckAlarm', {
        metric: consistencyCheckMetric,
        threshold: 95, // 95% consistency threshold
        comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
        evaluationPeriods: 2,
        datapointsToAlarm: 2,
        alarmDescription: 'Redis cross-region consistency is below 95%',
        alarmName: `${props.environment}-redis-consistency-check-${props.region}`,
        treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        ...(props.alertingTopic && {
          alarmActions: [props.alertingTopic],
          okActions: [props.alertingTopic],
        }),
      });
    }

    // Create CloudWatch Dashboard for Redis monitoring
    const dashboard = new cloudwatch.Dashboard(this, 'RedisDashboard', {
      dashboardName: `${props.environment}-redis-monitoring-${props.region}`,
      widgets: [
        [
          new cloudwatch.GraphWidget({
            title: 'Cache Hit Rate',
            left: [cacheHitRateMetric],
            width: 12,
            height: 6,
          }),
          new cloudwatch.GraphWidget({
            title: 'CPU Utilization',
            left: [cpuUtilizationMetric],
            width: 12,
            height: 6,
          }),
        ],
        [
          new cloudwatch.GraphWidget({
            title: 'Memory Utilization',
            left: [memoryUtilizationMetric],
            width: 12,
            height: 6,
          }),
          new cloudwatch.GraphWidget({
            title: 'Connection Count',
            left: [connectionCountMetric],
            width: 12,
            height: 6,
          }),
        ],
      ],
    });
  }
}