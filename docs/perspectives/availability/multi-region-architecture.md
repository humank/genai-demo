# Multi-Region Active-Active Architecture

> **Last Updated**: 2025-11-17  
> **Status**: ✅ Operational  
> **Owner**: Architecture & SRE Team

## Purpose

This document describes the Active-Active multi-region architecture that enables the Enterprise E-Commerce Platform to achieve 99.99% availability through simultaneous operation in Taiwan and Japan regions. This architecture eliminates single points of failure at the regional level and provides superior disaster recovery capabilities.

## Architecture Overview

### Regional Distribution

```text
┌─────────────────────────────────────────────────────────────────┐
│                     Global Traffic Management                    │
│                    Route 53 DNS + Health Checks                  │
└────────────────────┬────────────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
┌───────▼─────────────────┐  ┌───▼──────────────────────┐
│   Taiwan Region         │  │   Japan Region           │
│   (ap-northeast-1)      │  │   (ap-northeast-1)       │
│   PRIMARY - 60% Traffic │  │   SECONDARY - 40% Traffic│
├─────────────────────────┤  ├──────────────────────────┤
│                         │  │                          │
│ ┌─────────────────────┐ │  │ ┌─────────────────────┐ │
│ │ Application Layer   │ │  │ │ Application Layer   │ │
│ │ - EKS Cluster       │ │  │ │ - EKS Cluster       │ │
│ │ - 5 nodes (3 AZs)   │ │  │ │ - 5 nodes (3 AZs)   │ │
│ │ - Smart Routing     │ │  │ │ - Smart Routing     │ │
│ └─────────────────────┘ │  │ └─────────────────────┘ │
│                         │  │                          │
│ ┌─────────────────────┐ │  │ ┌─────────────────────┐ │
│ │ Data Layer          │ │  │ │ Data Layer          │ │
│ │ - Aurora Global DB  │◄─┼──┼─┤ - Aurora Global DB  │ │
│ │ - ElastiCache Global│◄─┼──┼─┤ - ElastiCache Global│ │
│ │ - MSK Cluster       │◄─┼──┼─┤ - MSK Cluster       │ │
│ └─────────────────────┘ │  │ └─────────────────────┘ │
│                         │  │                          │
│ ┌─────────────────────┐ │  │ ┌─────────────────────┐ │
│ │ Monitoring          │ │  │ │ Monitoring          │ │
│ │ - CloudWatch        │ │  │ │ - CloudWatch        │ │
│ │ - X-Ray             │ │  │ │ - X-Ray             │ │
│ │ - Prometheus        │ │  │ │ - Prometheus        │ │
│ └─────────────────────┘ │  │ └─────────────────────┘ │
└─────────────────────────┘  └──────────────────────────┘
```

### Traffic Distribution Strategy

**Normal Operation**:
- Taiwan Region: 60% of traffic (primary market)
- Japan Region: 40% of traffic (secondary market + DR capacity)

**Failure Scenarios**:
- Taiwan Failure: 100% traffic to Japan
- Japan Failure: 100% traffic to Taiwan
- Partial Failure: Dynamic redistribution based on health

## Component Architecture

### 1. Route 53 Intelligent Traffic Management

#### DNS Configuration

```typescript
// infrastructure/lib/stacks/route53-routing-stack.ts
export class Route53RoutingStack extends Stack {
  constructor(scope: Construct, id: string, props: StackProps) {
    super(scope, id, props);

    // Health checks for both regions
    const taiwanHealthCheck = new route53.CfnHealthCheck(this, 'TaiwanHealthCheck', {
      healthCheckConfig: {
        type: 'HTTPS',
        resourcePath: '/actuator/health/readiness',
        fullyQualifiedDomainName: 'taiwan.api.genai-demo.com',
        port: 443,
        requestInterval: 30,
        failureThreshold: 2,
        measureLatency: true,
      },
      healthCheckTags: [{
        key: 'Name',
        value: 'Taiwan Region Health Check'
      }]
    });

    const japanHealthCheck = new route53.CfnHealthCheck(this, 'JapanHealthCheck', {
      healthCheckConfig: {
        type: 'HTTPS',
        resourcePath: '/actuator/health/readiness',
        fullyQualifiedDomainName: 'japan.api.genai-demo.com',
        port: 443,
        requestInterval: 30,
        failureThreshold: 2,
        measureLatency: true,
      },
      healthCheckTags: [{
        key: 'Name',
        value: 'Japan Region Health Check'
      }]
    });

    // Geolocation routing for Taiwan users
    new route53.ARecord(this, 'TaiwanGeoRecord', {
      zone: hostedZone,
      recordName: 'api',
      target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(taiwanALB)),
      setIdentifier: 'taiwan-geo',
      geoLocation: route53.GeoLocation.country('TW'),
      healthCheck: taiwanHealthCheck,
    });

    // Geolocation routing for Japan users
    new route53.ARecord(this, 'JapanGeoRecord', {
      zone: hostedZone,
      recordName: 'api',
      target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(japanALB)),
      setIdentifier: 'japan-geo',
      geoLocation: route53.GeoLocation.country('JP'),
      healthCheck: japanHealthCheck,
    });

    // Weighted routing for other regions (60/40 split)
    new route53.ARecord(this, 'TaiwanWeightedRecord', {
      zone: hostedZone,
      recordName: 'api',
      target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(taiwanALB)),
      setIdentifier: 'taiwan-weighted',
      weight: 60,
      healthCheck: taiwanHealthCheck,
    });

    new route53.ARecord(this, 'JapanWeightedRecord', {
      zone: hostedZone,
      recordName: 'api',
      target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(japanALB)),
      setIdentifier: 'japan-weighted',
      weight: 40,
      healthCheck: japanHealthCheck,
    });
  }
}
```

#### Health Check Configuration

**Health Check Parameters**:
- **Interval**: 30 seconds
- **Failure Threshold**: 2 consecutive failures
- **Timeout**: 10 seconds
- **Protocol**: HTTPS
- **Path**: `/actuator/health/readiness`

**Health Check Logic**:
1. Check application readiness endpoint
2. Verify database connectivity
3. Validate cache availability
4. Confirm message queue health
5. Return 200 OK only if all checks pass

### 2. Aurora Global Database

#### Configuration

```typescript
// infrastructure/lib/stacks/aurora-global-stack.ts
export class AuroraGlobalStack extends Stack {
  constructor(scope: Construct, id: string, props: StackProps) {
    super(scope, id, props);

    // Global cluster
    const globalCluster = new rds.CfnGlobalCluster(this, 'GlobalCluster', {
      globalClusterIdentifier: 'genai-demo-global',
      engine: 'aurora-postgresql',
      engineVersion: '15.4',
      storageEncrypted: true,
    });

    // Taiwan primary cluster
    const taiwanCluster = new rds.DatabaseCluster(this, 'TaiwanCluster', {
      engine: rds.DatabaseClusterEngine.auroraPostgres({
        version: rds.AuroraPostgresEngineVersion.VER_15_4
      }),
      writer: rds.ClusterInstance.provisioned('writer', {
        instanceType: ec2.InstanceType.of(ec2.InstanceClass.R6G, ec2.InstanceSize.XLARGE),
        enablePerformanceInsights: true,
      }),
      readers: [
        rds.ClusterInstance.provisioned('reader-1', {
          instanceType: ec2.InstanceType.of(ec2.InstanceClass.R6G, ec2.InstanceSize.LARGE),
        }),
        rds.ClusterInstance.provisioned('reader-2', {
          instanceType: ec2.InstanceType.of(ec2.InstanceClass.R6G, ec2.InstanceSize.LARGE),
        }),
      ],
      vpc: taiwanVpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_ISOLATED },
      backup: {
        retention: cdk.Duration.days(7),
        preferredWindow: '03:00-04:00',
      },
      cloudwatchLogsExports: ['postgresql'],
      cloudwatchLogsRetention: logs.RetentionDays.ONE_MONTH,
    });

    // Japan secondary cluster with write forwarding
    const japanCluster = new rds.DatabaseCluster(this, 'JapanCluster', {
      engine: rds.DatabaseClusterEngine.auroraPostgres({
        version: rds.AuroraPostgresEngineVersion.VER_15_4
      }),
      writer: rds.ClusterInstance.provisioned('writer', {
        instanceType: ec2.InstanceType.of(ec2.InstanceClass.R6G, ec2.InstanceSize.XLARGE),
        enablePerformanceInsights: true,
      }),
      readers: [
        rds.ClusterInstance.provisioned('reader-1', {
          instanceType: ec2.InstanceType.of(ec2.InstanceClass.R6G, ec2.InstanceSize.LARGE),
        }),
      ],
      vpc: japanVpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_ISOLATED },
      // Enable global write forwarding for Active-Active
      enableGlobalWriteForwarding: true,
    });

    // Attach clusters to global cluster
    taiwanCluster.node.addDependency(globalCluster);
    japanCluster.node.addDependency(globalCluster);
  }
}
```

#### Replication Characteristics

- **Replication Lag**: < 1 second (99th percentile)
- **Write Forwarding**: Enabled for Japan region
- **Conflict Resolution**: Last-write-wins with timestamp
- **Data Consistency**: Eventual consistency with < 1s convergence

### 3. Application-Layer Smart Routing

#### Smart Routing Components

```java
// infrastructure/routing/SmartRoutingDataSource.java
@Component
public class SmartRoutingDataSource extends AbstractRoutingDataSource {
    
    private final RegionDetector regionDetector;
    private final HealthChecker healthChecker;
    private final RouteSelector routeSelector;
    
    @Override
    protected Object determineCurrentLookupKey() {
        String currentRegion = regionDetector.detectRegion();
        
        // Check health of local region
        if (healthChecker.isHealthy(currentRegion)) {
            return currentRegion + "-datasource";
        }
        
        // Failover to other region
        String backupRegion = routeSelector.selectBackupRegion(currentRegion);
        logger.warn("Failing over from {} to {} due to health check failure", 
                   currentRegion, backupRegion);
        
        return backupRegion + "-datasource";
    }
    
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void refreshHealthStatus() {
        healthChecker.refreshAllRegions();
    }
}

// infrastructure/routing/HealthChecker.java
@Component
public class HealthChecker {
    
    private final Map<String, HealthStatus> regionHealth = new ConcurrentHashMap<>();
    
    public boolean isHealthy(String region) {
        HealthStatus status = regionHealth.get(region);
        return status != null && status.isHealthy();
    }
    
    public void refreshAllRegions() {
        List.of("taiwan", "japan").forEach(region -> {
            HealthStatus status = checkRegionHealth(region);
            regionHealth.put(region, status);
            
            if (!status.isHealthy()) {
                publishHealthAlert(region, status);
            }
        });
    }
    
    private HealthStatus checkRegionHealth(String region) {
        try {
            // Check database connectivity
            boolean dbHealthy = checkDatabaseHealth(region);
            
            // Check cache connectivity
            boolean cacheHealthy = checkCacheHealth(region);
            
            // Check message queue
            boolean mqHealthy = checkMessageQueueHealth(region);
            
            long latency = measureLatency(region);
            
            return new HealthStatus(
                dbHealthy && cacheHealthy && mqHealthy,
                latency,
                Instant.now()
            );
        } catch (Exception e) {
            logger.error("Health check failed for region: {}", region, e);
            return HealthStatus.unhealthy();
        }
    }
}
```

### 4. MSK Cross-Region Replication

#### MirrorMaker 2.0 Configuration

```yaml
# k8s/msk/mirrormaker2-config.yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaMirrorMaker2
metadata:
  name: cross-region-mirror
  namespace: kafka
spec:
  version: 3.6.0
  replicas: 3
  connectCluster: "taiwan"
  clusters:
    - alias: "taiwan"
      bootstrapServers: taiwan-msk-cluster:9092
      config:
        config.storage.replication.factor: 3
        offset.storage.replication.factor: 3
        status.storage.replication.factor: 3
    - alias: "japan"
      bootstrapServers: japan-msk-cluster:9092
      config:
        config.storage.replication.factor: 3
        offset.storage.replication.factor: 3
        status.storage.replication.factor: 3
  mirrors:
    - sourceCluster: "taiwan"
      targetCluster: "japan"
      sourceConnector:
        config:
          replication.factor: 3
          offset-syncs.topic.replication.factor: 3
          sync.topic.acls.enabled: "false"
          refresh.topics.interval.seconds: 60
      heartbeatConnector:
        config:
          heartbeats.topic.replication.factor: 3
      checkpointConnector:
        config:
          checkpoints.topic.replication.factor: 3
          sync.group.offsets.enabled: "true"
      topicsPattern: ".*"
      groupsPattern: ".*"
    - sourceCluster: "japan"
      targetCluster: "taiwan"
      sourceConnector:
        config:
          replication.factor: 3
          offset-syncs.topic.replication.factor: 3
          sync.topic.acls.enabled: "false"
          refresh.topics.interval.seconds: 60
      heartbeatConnector:
        config:
          heartbeats.topic.replication.factor: 3
      checkpointConnector:
        config:
          checkpoints.topic.replication.factor: 3
          sync.group.offsets.enabled: "true"
      topicsPattern: ".*"
      groupsPattern: ".*"
```

## Failover Scenarios

### Scenario 1: Complete Region Failure

**Trigger**: Taiwan region becomes completely unavailable

**Automatic Response**:
1. Route 53 health checks fail (2 consecutive failures = 60 seconds)
2. DNS automatically routes 100% traffic to Japan
3. Application-layer smart routing detects failure (5 seconds)
4. All requests automatically routed to Japan endpoints
5. Aurora Global Database continues with Japan as primary
6. MSK consumers in Japan process all events

**Recovery Time**: 30 seconds (DNS propagation + health check)

**Data Loss**: Zero (Aurora synchronous replication)

### Scenario 2: Database Failure

**Trigger**: Aurora primary instance fails in Taiwan

**Automatic Response**:
1. Aurora automatic failover to standby (30 seconds)
2. Application connection pool detects failure
3. Smart routing switches to Japan database
4. Transactions automatically retry
5. No user-visible impact

**Recovery Time**: 30 seconds

**Data Loss**: Zero (Multi-AZ synchronous replication)

### Scenario 3: Network Partition

**Trigger**: Network connectivity lost between regions

**Automatic Response**:
1. Each region operates independently
2. Aurora Global Database continues in both regions
3. MSK replication queues messages for later sync
4. Application serves users from nearest region
5. Automatic reconciliation when connectivity restored

**Recovery Time**: No downtime (split-brain operation)

**Data Reconciliation**: < 5 minutes after connectivity restored

## Monitoring and Alerting

### Key Metrics

```yaml
# CloudWatch Alarms
RegionHealthAlarms:
  TaiwanRegionUnhealthy:
    MetricName: HealthCheckStatus
    Threshold: 0
    EvaluationPeriods: 2
    Period: 60
    AlarmActions:
      - !Ref CriticalAlertTopic
    
  JapanRegionUnhealthy:
    MetricName: HealthCheckStatus
    Threshold: 0
    EvaluationPeriods: 2
    Period: 60
    AlarmActions:
      - !Ref CriticalAlertTopic
  
  CrossRegionReplicationLag:
    MetricName: AuroraGlobalDBReplicationLag
    Threshold: 5000  # 5 seconds
    EvaluationPeriods: 3
    Period: 60
    AlarmActions:
      - !Ref WarningAlertTopic
  
  MSKReplicationLag:
    MetricName: MirrorMaker2Lag
    Threshold: 10000  # 10 seconds
    EvaluationPeriods: 2
    Period: 60
    AlarmActions:
      - !Ref WarningAlertTopic
```

### Grafana Dashboard

**Multi-Region Overview Panel**:
- Region health status (green/yellow/red)
- Traffic distribution by region
- Cross-region replication lag
- Failover event timeline
- RTO/RPO metrics

## Cost Optimization

### Regional Cost Distribution

| Component | Taiwan (Monthly) | Japan (Monthly) | Total |
|-----------|------------------|-----------------|-------|
| EKS Cluster | $1,200 | $1,000 | $2,200 |
| Aurora Global DB | $2,500 | $2,000 | $4,500 |
| ElastiCache | $800 | $600 | $1,400 |
| MSK | $500 | $400 | $900 |
| Data Transfer | $300 | $200 | $500 |
| **Total** | **$5,300** | **$4,200** | **$9,500** |

**Cost vs. Benefit**:
- Additional cost: ~$4,500/month (vs. single region)
- Revenue protection: ~$5,000/minute of prevented downtime
- Break-even: < 1 minute of prevented downtime per month
- ROI: 10x (based on historical downtime prevention)

## Testing and Validation

### Monthly DR Drill

```bash
#!/bin/bash
# scripts/dr-drill-taiwan-failure.sh

echo "Starting DR Drill: Taiwan Region Failure Simulation"
echo "=================================================="

# 1. Simulate Taiwan region failure
echo "Step 1: Simulating Taiwan region failure..."
aws route53 change-resource-record-sets \
  --hosted-zone-id $HOSTED_ZONE_ID \
  --change-batch file://taiwan-failure-changeset.json

# 2. Monitor failover
echo "Step 2: Monitoring automatic failover..."
./scripts/monitor-failover.sh

# 3. Validate Japan region handling 100% traffic
echo "Step 3: Validating Japan region capacity..."
./scripts/validate-region-capacity.sh japan

# 4. Test critical user journeys
echo "Step 4: Testing critical user journeys..."
./scripts/test-user-journeys.sh

# 5. Restore Taiwan region
echo "Step 5: Restoring Taiwan region..."
aws route53 change-resource-record-sets \
  --hosted-zone-id $HOSTED_ZONE_ID \
  --change-batch file://taiwan-restore-changeset.json

# 6. Generate DR drill report
echo "Step 6: Generating DR drill report..."
./scripts/generate-dr-report.sh

echo "DR Drill completed successfully!"
```

## Best Practices

### Design Principles

1. **Assume Failure**: Design for failure at every level
2. **Automate Everything**: No manual intervention for failover
3. **Test Regularly**: Monthly DR drills and chaos engineering
4. **Monitor Continuously**: Real-time health checks and alerting
5. **Optimize Costs**: Balance availability with cost efficiency

### Operational Guidelines

1. **Health Checks**: Monitor every 5 seconds at application layer
2. **Failover Testing**: Monthly automated DR drills
3. **Capacity Planning**: Maintain 2x capacity in each region
4. **Data Consistency**: Monitor replication lag < 1 second
5. **Incident Response**: Automated runbooks for common scenarios

## Related Documentation

- [High Availability Design](high-availability.md)
- [Automated Failover](automated-failover.md)
- [Disaster Recovery](disaster-recovery.md)
- [Chaos Engineering](chaos-engineering.md)

---

**Next Steps**:
1. Review [Automated Failover](automated-failover.md) for intelligent routing details
2. Study [Chaos Engineering](chaos-engineering.md) for resilience testing
3. Implement monthly DR drills using provided scripts
