---
adr_number: 038
title: "Cross-Region Data Replication Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [001, 004, 005, 037, 039, 040]
affected_viewpoints: ["information", "deployment"]
affected_perspectives: ["availability", "performance", "evolution"]
---

# ADR-038: Cross-Region Data Replication Strategy

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

Active-active multi-region architecture (ADR-037) requires data synchronization between Taiwan and Tokyo regions. Different bounded contexts have different consistency requirements:

**Challenges**:

- **Consistency vs Availability**: CAP theorem trade-offs
- **Replication Lag**: Network latency between regions (40-60ms)
- **Data Conflicts**: Concurrent updates in different regions
- **Cost**: Cross-region data transfer ($0.09/GB)
- **Complexity**: Multiple data stores (PostgreSQL, Redis, Kafka, S3)
- **Performance**: Replication impact on application performance

**Business Impact**:

- Order processing errors due to inconsistency
- Inventory overselling
- Customer data conflicts
- Payment processing failures
- Poor user experience

### Business Context

**Business Drivers**:

- Data consistency for critical operations (orders, payments)
- High availability for all operations
- Low latency for read operations
- Cost-effective replication
- Regulatory compliance (data residency)

**Constraints**:

- Network latency: 40-60ms between Taiwan-Tokyo
- Budget: $50,000/year for data transfer
- Replication lag tolerance varies by context
- Must support bidirectional replication
- Zero data loss for critical data

### Technical Context

**Current State**:

- Single-region deployment
- No cross-region replication
- No conflict resolution mechanisms

**Requirements**:

- Bidirectional replication for all data stores
- Bounded context-specific consistency levels
- Conflict detection and resolution
- Replication lag monitoring
- Cost optimization

## Decision Drivers

1. **Consistency**: Critical data must be consistent
2. **Availability**: System must remain available during failures
3. **Performance**: Minimal impact on application performance
4. **Cost**: Optimize data transfer costs
5. **Complexity**: Manageable operational complexity
6. **Scalability**: Support growing data volumes
7. **Compliance**: Meet data residency requirements

## Considered Options

### Option 1: Tiered Replication Strategy (Recommended)

**Description**: Different replication strategies based on bounded context requirements

**Tier 1 - Strong Consistency (CP)**:

- **Contexts**: Orders, Payments, Inventory
- **Strategy**: Quorum write (both regions acknowledge)
- **Technology**: PostgreSQL synchronous replication
- **Lag**: < 100ms
- **Cost**: Higher latency, lower throughput

**Tier 2 - Eventual Consistency (AP)**:

- **Contexts**: Product Catalog, Customer Profiles, Reviews
- **Strategy**: Asynchronous replication
- **Technology**: PostgreSQL logical replication
- **Lag**: 1-5 seconds acceptable
- **Cost**: Low latency, high throughput

**Tier 3 - Regional Isolation**:

- **Contexts**: Shopping Carts, Sessions
- **Strategy**: Regional data, merge on demand
- **Technology**: Redis with regional clusters
- **Lag**: N/A (no replication)
- **Cost**: Lowest

**Pros**:

- ✅ Optimal balance of consistency and availability
- ✅ Cost-effective (only replicate what's needed)
- ✅ Performance optimized per context
- ✅ Clear consistency guarantees

**Cons**:

- ⚠️ Complexity in managing different strategies
- ⚠️ Requires careful context classification

**Cost**: $40,000/year

**Risk**: **Low**

### Option 2: Full Synchronous Replication

**Description**: All data synchronously replicated

**Pros**:

- ✅ Strong consistency everywhere
- ✅ Simple to understand

**Cons**:

- ❌ High latency (100ms+ for all writes)
- ❌ Lower throughput
- ❌ Availability impact
- ❌ High cost ($80,000/year)

**Risk**: **High** - Performance impact

### Option 3: Full Asynchronous Replication

**Description**: All data asynchronously replicated

**Pros**:

- ✅ Low latency
- ✅ High throughput
- ✅ Low cost ($30,000/year)

**Cons**:

- ❌ Consistency issues for critical data
- ❌ Risk of data loss
- ❌ Complex conflict resolution

**Risk**: **High** - Data consistency issues

## Decision Outcome

**Chosen Option**: **Tiered Replication Strategy (Option 1)**

### Rationale

Tiered replication provides optimal balance:

1. **Strong Consistency**: Where needed (orders, payments)
2. **High Availability**: Where acceptable (product catalog)
3. **Performance**: Optimized per context
4. **Cost**: Only pay for what's needed
5. **Flexibility**: Easy to adjust per context

### Bounded Context Classification

**Tier 1 - Strong Consistency (CP)**:

| Context | Replication | Lag Target | Rationale |
|---------|-------------|------------|-----------|
| Orders | Quorum Write | < 100ms | Financial accuracy critical |
| Payments | Synchronous | < 100ms | Payment integrity required |
| Inventory | Distributed Lock | < 100ms | Prevent overselling |

**Tier 2 - Eventual Consistency (AP)**:

| Context | Replication | Lag Target | Rationale |
|---------|-------------|------------|-----------|
| Product Catalog | Async | < 5s | Stale data acceptable |
| Customer Profiles | Async | < 5s | Profile updates infrequent |
| Reviews | Async | < 10s | Review display can lag |
| Promotions | Async | < 5s | Promotion changes infrequent |
| Sellers | Async | < 5s | Seller data changes rare |

**Tier 3 - Regional Isolation**:

| Context | Strategy | Rationale |
|---------|----------|-----------|
| Shopping Carts | Regional | Merge on checkout |
| Sessions | Regional | No cross-region sessions |
| Notifications | Regional | Region-specific notifications |

### PostgreSQL Replication Configuration

**Tier 1 - Synchronous Replication**:

```sql
-- postgresql.conf
synchronous_commit = remote_apply
synchronous_standby_names = 'tokyo_standby'

-- On Taiwan primary
ALTER SYSTEM SET synchronous_standby_names = 'tokyo_standby';

-- On Tokyo primary  
ALTER SYSTEM SET synchronous_standby_names = 'taiwan_standby';
```

**Tier 2 - Logical Replication**:

```sql
-- Taiwan publisher
CREATE PUBLICATION taiwan_catalog FOR TABLE 
  products, categories, product_images, customer_profiles;

-- Tokyo subscriber
CREATE SUBSCRIPTION tokyo_catalog
  CONNECTION 'host=taiwan-db port=5432 dbname=ecommerce'
  PUBLICATION taiwan_catalog
  WITH (
    copy_data = true,
    create_slot = true,
    streaming = true
  );
```

### Redis Replication Strategy

**Global Datastore for Critical Data**:

```typescript
const globalRedis = new elasticache.CfnGlobalReplicationGroup(this, 'GlobalRedis', {
  globalReplicationGroupIdSuffix: 'critical',
  primaryReplicationGroupId: taiwanRedis.ref,
  members: [{
    replicationGroupId: tokyoRedis.ref,
    replicationGroupRegion: 'ap-northeast-1',
    role: 'SECONDARY',
  }],
});
```

**Regional Clusters for Sessions**:

```typescript
// Separate clusters, no replication
const taiwanSessionRedis = new elasticache.CfnReplicationGroup(this, 'TaiwanSessions', {
  replicationGroupDescription: 'Taiwan session storage',
  cacheNodeType: 'cache.r6g.large',
  numCacheClusters: 2,
});

const tokyoSessionRedis = new elasticache.CfnReplicationGroup(this, 'TokyoSessions', {
  replicationGroupDescription: 'Tokyo session storage',
  cacheNodeType: 'cache.r6g.large',
  numCacheClusters: 2,
});
```

### Kafka Cross-Region Replication

**MirrorMaker 2.0 Configuration**:

```yaml
clusters:
  taiwan:
    bootstrap.servers: taiwan-kafka:9092
    security.protocol: SSL
  tokyo:
    bootstrap.servers: tokyo-kafka:9092
    security.protocol: SSL

mirrors:

  - source: taiwan

    target: tokyo
    topics: "orders.*, payments.*, inventory.*"
    sync.topic.configs.enabled: true
    replication.factor: 3
    
  - source: tokyo

    target: taiwan
    topics: "orders.*, payments.*, inventory.*"
    sync.topic.configs.enabled: true
    replication.factor: 3
```

### Conflict Resolution Mechanisms

**Last-Write-Wins (LWW)**:

```java
@Entity
public class CustomerProfile {
    @Id
    private String id;
    
    @Version
    private Long version;  // Optimistic locking
    
    @Column(name = "last_modified_timestamp")
    private Instant lastModified;
    
    @Column(name = "last_modified_region")
    private String lastModifiedRegion;
    
    public void mergeWith(CustomerProfile other) {
        if (other.lastModified.isAfter(this.lastModified)) {
            // Accept newer version
            this.name = other.name;
            this.email = other.email;
            this.lastModified = other.lastModified;
            this.lastModifiedRegion = other.lastModifiedRegion;
        }
    }
}
```

**Application-Level Resolution**:

```java
public class InventoryConflictResolver {
    
    public void resolveConflict(InventoryUpdate taiwan, InventoryUpdate tokyo) {
        // Conservative: use minimum quantity
        int resolved = Math.min(taiwan.getQuantity(), tokyo.getQuantity());
        
        // Log for investigation
        conflictLogger.warn("Inventory conflict: product={}, taiwan={}, tokyo={}, resolved={}",
            taiwan.getProductId(), taiwan.getQuantity(), tokyo.getQuantity(), resolved);
        
        // Update both regions
        inventoryService.updateQuantity(taiwan.getProductId(), resolved);
        
        // Alert if significant discrepancy
        if (Math.abs(taiwan.getQuantity() - tokyo.getQuantity()) > 10) {
            alertService.sendAlert("Significant inventory conflict detected");
        }
    }
}
```

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | Multi-region data handling | Patterns, libraries, training |
| Operations Team | High | Replication monitoring | Automation, dashboards |
| End Users | Low | Transparent replication | Proper conflict resolution |
| Business | Medium | Data consistency guarantees | Clear SLAs per context |

### Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Data conflicts | Medium | High | Conflict resolution, monitoring |
| Replication lag | Low | Medium | Monitoring, alerts, capacity |
| Split-brain | Low | Critical | Quorum, fencing (ADR-040) |
| Cost overrun | Medium | Medium | Monitoring, optimization |

**Overall Risk Level**: **Medium**

## Implementation Plan

### Phase 1: Foundation (Month 1)

- [ ] Set up PostgreSQL logical replication
- [ ] Configure Redis Global Datastore
- [ ] Deploy Kafka MirrorMaker 2.0
- [ ] Implement replication monitoring

### Phase 2: Tier 1 Implementation (Month 2)

- [ ] Configure synchronous replication for orders
- [ ] Implement distributed locking for inventory
- [ ] Test quorum writes
- [ ] Validate consistency

### Phase 3: Tier 2 Implementation (Month 3)

- [ ] Configure async replication for catalog
- [ ] Implement conflict resolution
- [ ] Test replication lag
- [ ] Validate eventual consistency

### Phase 4: Production (Month 4)

- [ ] Gradual rollout
- [ ] Monitor replication metrics
- [ ] Tune performance
- [ ] Document procedures

## Monitoring and Success Criteria

### Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Tier 1 replication lag | < 100ms | Replication monitoring |
| Tier 2 replication lag | < 5s | Replication monitoring |
| Conflict rate | < 0.1% | Conflict logs |
| Data consistency | 100% | Validation queries |
| Replication uptime | 99.99% | Monitoring |

### Key Metrics

```typescript
const metrics = {
  'replication.postgres.tier1.lag_ms': 'Milliseconds',
  'replication.postgres.tier2.lag_seconds': 'Seconds',
  'replication.redis.lag_seconds': 'Seconds',
  'replication.kafka.lag_messages': 'Count',
  'conflicts.detected_per_hour': 'Count',
  'conflicts.resolved_per_hour': 'Count',
  'replication.data_transfer_gb': 'Gigabytes',
};
```

## Consequences

### Positive Consequences

- ✅ **Optimal Consistency**: Strong where needed, eventual where acceptable
- ✅ **High Performance**: Minimal latency impact
- ✅ **Cost-Effective**: $40K/year vs $80K for full sync
- ✅ **Flexibility**: Easy to adjust per context
- ✅ **Clear Guarantees**: Well-defined consistency per context

### Negative Consequences

- ⚠️ **Complexity**: Multiple replication strategies
- ⚠️ **Monitoring**: Need comprehensive monitoring
- ⚠️ **Conflicts**: Require resolution mechanisms
- ⚠️ **Training**: Team needs to understand strategies

## Related Decisions

- [ADR-040: Network Partition Handling Strategy](040-network-partition-handling-strategy.md)

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
