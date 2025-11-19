---
adr_number: 037
title: "Active-Active Multi-Region Architecture (TPE-Tokyo)"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [017, 018, 035, 038, 039, 040, 041]
affected_viewpoints: ["deployment", "operational"]
affected_perspectives: ["availability", "performance", "location"]
---

# ADR-037: Active-Active Multi-Region Architecture (TPE-Tokyo)

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform faces critical geopolitical and operational risks:

**Geopolitical Risks**:

- **Taiwan-China Tensions**: Escalating military tensions with potential for conflict
- **Missile Attack Risk**: Taiwan within range of Chinese missile systems
- **Submarine Cable Vulnerability**: 99% of Taiwan's internet traffic via undersea cables
- **Cyber Warfare**: Frequent DDoS attacks and APT campaigns from state actors
- **Economic Sanctions**: Potential for trade restrictions affecting operations

**Operational Risks**:

- **Natural Disasters**: Taiwan in earthquake and typhoon zones
- **Single Point of Failure**: Single-region deployment creates business continuity risk
- **Latency**: Customers in Japan experience higher latency
- **Regulatory**: Data sovereignty requirements for different markets

**Business Impact**:

- Revenue loss during regional outages
- Customer trust erosion
- Regulatory non-compliance
- Competitive disadvantage
- Insurance and liability issues

### Business Context

**Business Drivers**:

- Business continuity during geopolitical crises
- Disaster recovery for natural disasters
- Market expansion to Japan
- Regulatory compliance (data residency)
- Customer experience improvement (lower latency)
- Competitive advantage (99.99% availability)

**Constraints**:

- Budget: $500,000/year for multi-region infrastructure
- Timeline: 6 months for initial deployment
- Existing Taiwan infrastructure must remain operational
- Zero downtime migration required
- Data consistency requirements vary by bounded context

### Technical Context

**Current State**:

- Single region deployment in Taiwan (ap-northeast-3)
- No disaster recovery capability
- Manual failover procedures
- RTO: 4+ hours, RPO: 1+ hour
- No geographic load distribution

**Requirements**:

- Active-active deployment in two regions
- Automatic failover capability
- RTO: < 5 minutes
- RPO: < 1 minute
- Geographic load distribution
- Data consistency across regions
- Cost-effective solution

## Decision Drivers

1. **Geopolitical Resilience**: Survive Taiwan-China conflict scenarios
2. **Business Continuity**: Maintain operations during regional disasters
3. **Performance**: Reduce latency for Japanese customers
4. **Availability**: Achieve 99.99% uptime SLA
5. **Cost**: Optimize infrastructure costs
6. **Complexity**: Manageable operational complexity
7. **Data Consistency**: Balance consistency with availability
8. **Regulatory**: Meet data residency requirements

## Considered Options

### Option 1: Active-Active Multi-Region (TPE + Tokyo) - Recommended

**Description**: Deploy fully operational infrastructure in both Taipei (ap-northeast-3) and Tokyo (ap-northeast-1) with active traffic serving from both regions

**Architecture**:

```text
┌─────────────────────────────────────────────────────────────┐
│                     Route 53 Global DNS                      │
│              Geolocation + Health Check Routing              │
└────────────────┬────────────────────────────┬────────────────┘
                 │                            │
        ┌────────▼────────┐          ┌───────▼────────┐
        │  Taiwan Region  │          │  Tokyo Region  │
        │ (ap-northeast-3)│          │(ap-northeast-1)│
        └────────┬────────┘          └───────┬────────┘
                 │                            │
        ┌────────▼────────┐          ┌───────▼────────┐
        │   EKS Cluster   │          │  EKS Cluster   │
        │   RDS Primary   │◄────────►│  RDS Primary   │
        │  ElastiCache    │  Sync    │ ElastiCache    │
        │   MSK Kafka     │◄────────►│   MSK Kafka    │
        └─────────────────┘          └────────────────┘
```

**Traffic Distribution**:

- Taiwan/Hong Kong/Southeast Asia → Taiwan region
- Japan/Korea/Pacific → Tokyo region
- Automatic failover on region failure
- Manual override capability

**Data Replication**:

- **PostgreSQL**: Logical replication (quasi-synchronous)
- **Redis**: Redis Cluster with cross-region replication
- **Kafka**: MirrorMaker 2.0 for event streaming
- **S3**: Cross-Region Replication (CRR)

**Pros**:

- ✅ Survives complete Taiwan region failure
- ✅ Low latency for both markets
- ✅ True high availability (99.99%+)
- ✅ Automatic failover (< 5 minutes)
- ✅ Load distribution reduces costs
- ✅ Supports market expansion
- ✅ Meets data residency requirements

**Cons**:

- ⚠️ Higher infrastructure cost (2x compute)
- ⚠️ Data consistency complexity
- ⚠️ Cross-region data transfer costs
- ⚠️ Operational complexity

**Cost**:

- Infrastructure: $400,000/year (2x compute, storage)
- Data Transfer: $50,000/year (cross-region)
- Operations: $50,000/year (additional staff)
- **Total**: $500,000/year

**Risk**: **Low** - Proven architecture pattern

### Option 2: Active-Passive (TPE Primary + Tokyo DR)

**Description**: Taiwan as primary region, Tokyo as cold/warm standby

**Pros**:

- ✅ Lower cost ($250,000/year)
- ✅ Simpler operations
- ✅ Easier data consistency

**Cons**:

- ❌ Manual failover required (RTO: 30+ minutes)
- ❌ No latency improvement for Japan
- ❌ Underutilized DR resources
- ❌ Higher RPO (5-15 minutes)
- ❌ No load distribution

**Cost**: $250,000/year

**Risk**: **Medium** - Manual failover unreliable

### Option 3: Multi-Region with Third Region (TPE + Tokyo + Singapore)

**Description**: Three-region deployment for maximum resilience

**Pros**:

- ✅ Survives dual-region failure
- ✅ Southeast Asia coverage
- ✅ Maximum resilience

**Cons**:

- ❌ Very high cost ($750,000/year)
- ❌ High complexity
- ❌ Overkill for current needs
- ❌ Data consistency challenges

**Cost**: $750,000/year

**Risk**: **Low** - But unnecessary complexity

## Decision Outcome

**Chosen Option**: **Active-Active Multi-Region (TPE + Tokyo) - Option 1**

### Rationale

Active-active multi-region architecture was selected for the following critical reasons:

1. **Geopolitical Resilience**: Survives complete Taiwan region failure due to military conflict
2. **Business Continuity**: Maintains operations during natural disasters (earthquakes, typhoons)
3. **Performance**: 50-70ms latency reduction for Japanese customers
4. **Availability**: Achieves 99.99% uptime (52 minutes downtime/year)
5. **Cost-Effective**: Load distribution reduces per-region costs vs single region
6. **Market Expansion**: Supports Japan market growth
7. **Automatic Failover**: < 5 minute RTO without manual intervention

### Region Selection Rationale

**Taiwan (ap-northeast-3)**:

- Primary market (60% of customers)
- Existing infrastructure
- Lower latency for Taiwan/Hong Kong/SEA customers
- Data residency for Taiwan customers

**Tokyo (ap-northeast-1)**:

- Geographically separated from Taiwan (2,100 km)
- Politically stable
- Excellent AWS infrastructure
- Low latency for Japan/Korea customers (20-30ms)
- Data residency for Japanese customers
- Submarine cable diversity

**Why Not Other Regions**:

- **Singapore**: Too far from Taiwan (3,300 km), higher latency
- **Seoul**: Too close to North Korea, geopolitical risk
- **Hong Kong**: Too close to China, similar risks as Taiwan
- **Sydney**: Too far (7,000 km), very high latency

### Traffic Distribution Strategy

**Route 53 Geolocation Routing**:

```typescript
// CDK Configuration
const globalDNS = new route53.HostedZone(this, 'GlobalDNS', {
  zoneName: 'ecommerce-platform.com',
});

// Taiwan region record
new route53.ARecord(this, 'TaiwanRegion', {
  zone: globalDNS,
  recordName: 'api',
  target: route53.RecordTarget.fromAlias(
    new targets.LoadBalancerTarget(taiwanALB)
  ),
  geoLocation: route53.GeoLocation.country('TW'),
});

// Tokyo region record
new route53.ARecord(this, 'TokyoRegion', {
  zone: globalDNS,
  recordName: 'api',
  target: route53.RecordTarget.fromAlias(
    new targets.LoadBalancerTarget(tokyoALB)
  ),
  geoLocation: route53.GeoLocation.country('JP'),
});

// Default to closest region
new route53.ARecord(this, 'DefaultRegion', {
  zone: globalDNS,
  recordName: 'api',
  target: route53.RecordTarget.fromAlias(
    new targets.LoadBalancerTarget(taiwanALB)
  ),
  geoLocation: route53.GeoLocation.default(),
});
```

**Health Check Configuration**:

```typescript
// Health checks for automatic failover
const taiwanHealthCheck = new route53.CfnHealthCheck(this, 'TaiwanHealth', {
  healthCheckConfig: {
    type: 'HTTPS',
    resourcePath: '/health',
    fullyQualifiedDomainName: 'taiwan.ecommerce-platform.com',
    port: 443,
    requestInterval: 30,
    failureThreshold: 3,
  },
  healthCheckTags: [
    { key: 'Name', value: 'Taiwan Region Health' },
    { key: 'Region', value: 'ap-northeast-3' },
  ],
});

const tokyoHealthCheck = new route53.CfnHealthCheck(this, 'TokyoHealth', {
  healthCheckConfig: {
    type: 'HTTPS',
    resourcePath: '/health',
    fullyQualifiedDomainName: 'tokyo.ecommerce-platform.com',
    port: 443,
    requestInterval: 30,
    failureThreshold: 3,
  },
  healthCheckTags: [
    { key: 'Name', value: 'Tokyo Region Health' },
    { key: 'Region', value: 'ap-northeast-1' },
  ],
});
```

### Data Synchronization Strategy

**Bounded Context Classification**:

**Strong Consistency (CP - Consistency + Partition Tolerance)**:

- **Orders**: Quorum write (both regions must acknowledge)
- **Payments**: Synchronous replication
- **Inventory**: Distributed locks + dual-write

**Eventual Consistency (AP - Availability + Partition Tolerance)**:

- **Product Catalog**: Asynchronous replication (5-10s lag acceptable)
- **Customer Profiles**: Asynchronous replication
- **Shopping Carts**: Regional isolation, merge on checkout
- **Reviews**: Asynchronous replication

**Replication Technologies**:

**PostgreSQL Logical Replication**:

```sql
-- Taiwan region (publisher)
CREATE PUBLICATION taiwan_pub FOR ALL TABLES;

-- Tokyo region (subscriber)
CREATE SUBSCRIPTION tokyo_sub
  CONNECTION 'host=taiwan-db.region.rds.amazonaws.com port=5432 dbname=ecommerce'
  PUBLICATION taiwan_pub
  WITH (copy_data = true, create_slot = true);

-- Bidirectional replication
CREATE PUBLICATION tokyo_pub FOR ALL TABLES;

CREATE SUBSCRIPTION taiwan_sub
  CONNECTION 'host=tokyo-db.region.rds.amazonaws.com port=5432 dbname=ecommerce'
  PUBLICATION tokyo_pub
  WITH (copy_data = true, create_slot = true);
```

**Redis Cross-Region Replication**:

```typescript
// Redis Global Datastore
const globalDatastore = new elasticache.CfnGlobalReplicationGroup(this, 'GlobalRedis', {
  globalReplicationGroupIdSuffix: 'ecommerce',
  primaryReplicationGroupId: taiwanRedisCluster.ref,
  globalReplicationGroupDescription: 'Cross-region Redis replication',
  
  // Add Tokyo as member
  members: [
    {
      replicationGroupId: tokyoRedisCluster.ref,
      replicationGroupRegion: 'ap-northeast-1',
      role: 'SECONDARY',
    }
  ],
});
```

**Kafka MirrorMaker 2.0**:

```yaml
# MirrorMaker 2.0 configuration
clusters:
  taiwan:
    bootstrap.servers: taiwan-kafka.region.amazonaws.com:9092
  tokyo:
    bootstrap.servers: tokyo-kafka.region.amazonaws.com:9092

mirrors:

  - source: taiwan

    target: tokyo
    topics: ".*"
    sync.topic.configs.enabled: true
    sync.topic.acls.enabled: false
    
  - source: tokyo

    target: taiwan
    topics: ".*"
    sync.topic.configs.enabled: true
    sync.topic.acls.enabled: false
```

### Conflict Resolution Strategy

**Last-Write-Wins (LWW)**:

```java
// For customer profile updates
public class CustomerProfile {
    private String id;
    private String name;
    private String email;
    private long version;  // Timestamp-based version
    private String lastModifiedRegion;
    
    public void merge(CustomerProfile other) {
        if (other.version > this.version) {
            // Other version is newer, accept it
            this.name = other.name;
            this.email = other.email;
            this.version = other.version;
            this.lastModifiedRegion = other.lastModifiedRegion;
        }
    }
}
```

**Application-Level Resolution**:

```java
// For inventory updates
public class InventoryConflictResolver {
    
    public Inventory resolve(Inventory taiwan, Inventory tokyo) {
        // Conservative approach: use minimum quantity
        int resolvedQuantity = Math.min(
            taiwan.getQuantity(),
            tokyo.getQuantity()
        );
        
        // Log conflict for investigation
        logger.warn("Inventory conflict detected for product {}: Taiwan={}, Tokyo={}",
            taiwan.getProductId(), taiwan.getQuantity(), tokyo.getQuantity());
        
        // Create resolved inventory
        return Inventory.builder()
            .productId(taiwan.getProductId())
            .quantity(resolvedQuantity)
            .lastModified(Instant.now())
            .resolvedFrom(List.of(taiwan, tokyo))
            .build();
    }
}
```

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | Multi-region code complexity, data consistency | Training, patterns, tools |
| Operations Team | High | Multi-region monitoring, incident response | Automation, runbooks, training |
| End Users | Low | Improved latency, higher availability | Transparent migration |
| Business | Medium | Higher infrastructure cost, better resilience | ROI analysis, phased rollout |
| Security Team | Medium | Cross-region security, compliance | Security controls, audits |

### Impact Radius

**Selected Impact Radius**: **Enterprise**

Affects:

- All application services
- All databases and caches
- All message queues
- All monitoring and logging
- All deployment pipelines
- All disaster recovery procedures

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Data inconsistency | Medium | High | Conflict resolution, monitoring, alerts |
| Cross-region latency | Low | Medium | Optimize replication, caching |
| Increased costs | High | Medium | Cost monitoring, optimization |
| Operational complexity | Medium | High | Automation, training, documentation |
| Split-brain scenario | Low | Critical | Quorum-based consensus, fencing |

**Overall Risk Level**: **Medium**

## Implementation Plan

### Phase 1: Foundation (Month 1-2)

**Objectives**:

- Set up Tokyo region infrastructure
- Establish cross-region networking
- Deploy monitoring and observability

**Tasks**:

- [ ] Provision Tokyo VPC and networking
- [ ] Set up VPC peering between regions
- [ ] Deploy EKS cluster in Tokyo
- [ ] Set up cross-region monitoring
- [ ] Configure Route 53 global DNS
- [ ] Deploy health check endpoints

**Success Criteria**:

- Tokyo infrastructure operational
- Cross-region connectivity verified
- Monitoring dashboards functional

### Phase 2: Data Replication (Month 3-4)

**Objectives**:

- Establish database replication
- Configure cache replication
- Set up event streaming

**Tasks**:

- [ ] Configure PostgreSQL logical replication
- [ ] Set up Redis Global Datastore
- [ ] Deploy Kafka MirrorMaker 2.0
- [ ] Configure S3 Cross-Region Replication
- [ ] Test data synchronization
- [ ] Implement conflict resolution

**Success Criteria**:

- Data replication lag < 5 seconds
- Conflict resolution working
- Zero data loss in tests

### Phase 3: Application Deployment (Month 5)

**Objectives**:

- Deploy applications to Tokyo
- Configure traffic routing
- Test failover scenarios

**Tasks**:

- [ ] Deploy application services to Tokyo
- [ ] Configure Route 53 geolocation routing
- [ ] Test traffic distribution
- [ ] Perform failover drills
- [ ] Validate data consistency
- [ ] Load testing

**Success Criteria**:

- All services operational in both regions
- Traffic routing working correctly
- Failover < 5 minutes

### Phase 4: Production Cutover (Month 6)

**Objectives**:

- Gradual traffic migration
- Production validation
- Full active-active operation

**Tasks**:

- [ ] Migrate 10% traffic to Tokyo
- [ ] Monitor and validate
- [ ] Migrate 50% traffic to Tokyo
- [ ] Monitor and validate
- [ ] Enable full active-active
- [ ] 24/7 monitoring for 2 weeks

**Success Criteria**:

- 99.99% availability achieved
- RTO < 5 minutes validated
- RPO < 1 minute validated
- No data inconsistencies

### Rollback Strategy

**Trigger Conditions**:

- Data consistency issues
- Failover failures
- Performance degradation > 20%
- Critical bugs in multi-region code

**Rollback Steps**:

1. **Immediate**: Route all traffic to Taiwan region
2. **Data**: Stop replication, validate Taiwan data integrity
3. **Services**: Scale down Tokyo services
4. **Verification**: Validate single-region operation
5. **Investigation**: Root cause analysis

**Rollback Time**: < 30 minutes

## Monitoring and Success Criteria

### Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Availability | 99.99% | Uptime monitoring |
| RTO | < 5 minutes | Failover drills |
| RPO | < 1 minute | Replication lag |
| Latency (Taiwan) | < 50ms | APM |
| Latency (Japan) | < 30ms | APM |
| Data sync lag | < 5 seconds | Replication monitoring |
| Failover success | 100% | Quarterly drills |

### Monitoring Plan

**Key Metrics**:

```typescript
// CloudWatch metrics
const metrics = {
  // Regional health
  'region.taiwan.health': 'Healthy/Unhealthy',
  'region.tokyo.health': 'Healthy/Unhealthy',
  
  // Traffic distribution
  'traffic.taiwan.requests_per_second': 'Count',
  'traffic.tokyo.requests_per_second': 'Count',
  
  // Replication lag
  'replication.postgres.lag_seconds': 'Seconds',
  'replication.redis.lag_seconds': 'Seconds',
  'replication.kafka.lag_messages': 'Count',
  
  // Failover
  'failover.last_event': 'Timestamp',
  'failover.duration_seconds': 'Seconds',
  
  // Data consistency
  'consistency.conflicts_per_hour': 'Count',
  'consistency.resolution_success_rate': 'Percentage',
};
```

**Alerts**:

- **P0 Critical**: Region failure, failover failure
- **P1 High**: Replication lag > 10s, data conflicts
- **P2 Medium**: Replication lag > 5s, latency increase
- **P3 Low**: Cost anomalies, capacity warnings

**Dashboards**:

- Multi-region overview dashboard
- Regional health dashboard
- Replication status dashboard
- Traffic distribution dashboard
- Cost analysis dashboard

### Review Schedule

- **Daily**: Health check review
- **Weekly**: Metrics review, cost analysis
- **Monthly**: Failover drill, capacity planning
- **Quarterly**: Architecture review, optimization

## Consequences

### Positive Consequences

- ✅ **Resilience**: Survives Taiwan region failure (war, disaster)
- ✅ **Availability**: 99.99% uptime (52 min downtime/year)
- ✅ **Performance**: 50-70ms latency reduction for Japan
- ✅ **Business Continuity**: Operations continue during crises
- ✅ **Market Expansion**: Supports Japan market growth
- ✅ **Competitive Advantage**: Superior availability vs competitors
- ✅ **Customer Trust**: Demonstrates commitment to reliability

### Negative Consequences

- ⚠️ **Cost**: $500,000/year infrastructure cost (2x single region)
- ⚠️ **Complexity**: Multi-region operations complexity
- ⚠️ **Data Consistency**: Eventual consistency challenges
- ⚠️ **Cross-Region Costs**: $50,000/year data transfer
- ⚠️ **Operational Overhead**: 24/7 monitoring required
- ⚠️ **Development Effort**: Multi-region aware code

### Technical Debt

**Identified Debt**:

1. Manual conflict resolution for some scenarios
2. Basic traffic distribution (no intelligent routing)
3. Limited automated failover testing
4. Manual capacity planning

**Debt Repayment Plan**:

- **Q2 2026**: Automated conflict resolution for all scenarios
- **Q3 2026**: Intelligent traffic routing based on load
- **Q4 2026**: Automated failover testing (chaos engineering)
- **2027**: ML-powered capacity prediction

## Related Decisions

- [ADR-017: Multi-Region Deployment Strategy](017-multi-region-deployment-strategy.md) - Superseded by this ADR
- [ADR-018: Container Orchestration with AWS EKS](018-container-orchestration-eks.md) - EKS in both regions
- [ADR-035: Disaster Recovery Strategy](035-disaster-recovery-strategy.md) - DR integrated with multi-region
- [ADR-040: Network Partition Handling Strategy](040-network-partition-handling-strategy.md) - Split-brain prevention
- [ADR-041: Data Residency and Sovereignty Strategy](041-data-residency-sovereignty-strategy.md) - Compliance requirements

## Notes

### Geopolitical Risk Assessment

**Taiwan-China Conflict Scenarios**:

1. **Missile Attack**: Taiwan region destroyed → Tokyo continues operations
2. **Submarine Cable Cut**: Taiwan isolated → Tokyo serves all traffic
3. **Cyber Attack**: DDoS on Taiwan → Automatic failover to Tokyo
4. **Economic Sanctions**: Taiwan access restricted → Tokyo primary region

**Mitigation**: Active-active architecture ensures business continuity in all scenarios

### Cost Breakdown

**Infrastructure Costs** ($400,000/year):

- EKS: $100,000/year (2 clusters)
- RDS: $120,000/year (2 primary databases)
- ElastiCache: $60,000/year (2 clusters)
- MSK: $80,000/year (2 clusters)
- Load Balancers: $20,000/year
- Storage: $20,000/year

**Data Transfer** ($50,000/year):

- Cross-region replication: $0.09/GB
- Estimated: 50TB/month = $4,500/month

**Operations** ($50,000/year):

- Additional DevOps staff
- Training and tools
- Monitoring and alerting

### Performance Benchmarks

**Latency Improvements**:

- Taiwan customers: 20-30ms (no change)
- Japan customers: 150ms → 30ms (80% improvement)
- Korea customers: 180ms → 50ms (72% improvement)

**Availability Calculation**:

- Single region: 99.9% (8.76 hours downtime/year)
- Active-active: 99.99% (52 minutes downtime/year)
- Improvement: 10x reduction in downtime

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
