---
adr_number: 040
title: "Network Partition Handling Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [037, 038, 039, 041]
affected_viewpoints: ["deployment", "operational", "concurrency"]
affected_perspectives: ["availability", "performance"]
---

# ADR-040: Network Partition Handling Strategy

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

Active-active multi-region architecture faces the risk of network partitions where regions cannot communicate:

**Network Partition Scenarios**:
- **Submarine Cable Cut**: Taiwan-Japan undersea cables damaged
- **DDoS Attack**: Cross-region connectivity disrupted
- **AWS Network Issue**: Inter-region VPC peering failure
- **BGP Routing Problem**: Internet routing issues
- **Geopolitical Event**: Deliberate network isolation

**Split-Brain Problem**:
When regions cannot communicate but both remain operational, they may:
- Accept conflicting writes
- Diverge in data state
- Create irreconcilable conflicts
- Violate consistency guarantees
- Cause data corruption

**Business Impact**:
- Data inconsistency and conflicts
- Duplicate orders or payments
- Inventory overselling
- Customer confusion
- Financial losses
- Regulatory violations

### Business Context

**Business Drivers**:
- Prevent split-brain scenarios
- Maintain data consistency
- Ensure availability during partitions
- Minimize data conflicts
- Quick recovery after partition heals
- Clear operational procedures

**Constraints**:
- CAP theorem: Cannot have all three (Consistency, Availability, Partition tolerance)
- Network latency: 40-60ms between Taiwan-Tokyo
- Budget: $50,000/year for partition handling infrastructure
- Must support different consistency levels per bounded context
- Recovery must be automated

### Technical Context

**Current State**:
- Active-active multi-region deployed
- Basic health checks in place
- No partition detection mechanism
- No split-brain prevention
- No partition recovery procedures

**Requirements**:
- Detect network partitions quickly (< 30 seconds)
- Prevent split-brain scenarios
- Maintain availability where possible
- Minimize data conflicts
- Automated partition recovery
- Clear consistency guarantees per bounded context


## Decision Drivers

1. **Consistency**: Prevent data corruption and conflicts
2. **Availability**: Maintain service during partitions
3. **Detection**: Quickly detect network partitions
4. **Prevention**: Prevent split-brain scenarios
5. **Recovery**: Automated recovery when partition heals
6. **Flexibility**: Different strategies per bounded context
7. **Cost**: Optimize infrastructure costs
8. **Simplicity**: Manageable operational complexity

## Considered Options

### Option 1: Quorum-Based Consensus with Context-Specific CAP Trade-offs (Recommended)

**Description**: Use quorum-based consensus for critical data (CP), allow partition tolerance for non-critical data (AP)

**CAP Trade-offs by Bounded Context**:

**CP (Consistency + Partition Tolerance) - Sacrifice Availability**:
- **Orders**: Require quorum (majority) for writes
- **Payments**: Require quorum for writes
- **Inventory**: Require quorum for writes
- **Strategy**: During partition, minority partition becomes read-only

**AP (Availability + Partition Tolerance) - Sacrifice Consistency**:
- **Product Catalog**: Accept writes in both partitions
- **Customer Profiles**: Accept writes in both partitions
- **Reviews**: Accept writes in both partitions
- **Shopping Carts**: Regional isolation (no cross-region sync)
- **Strategy**: Resolve conflicts after partition heals

**Partition Detection**:
- Cross-region heartbeat (every 10 seconds)
- Multi-path health checks
- External monitoring (third-party service)
- Consensus on partition state

**Split-Brain Prevention**:
- Quorum requirement for critical operations
- Fencing mechanism (disable minority partition)
- Third-party arbitrator (AWS service in neutral region)
- Automatic read-only mode for minority

**Pros**:
- ✅ Prevents split-brain for critical data
- ✅ Maintains availability for non-critical data
- ✅ Clear consistency guarantees per context
- ✅ Automated partition handling
- ✅ Flexible and pragmatic
- ✅ Balances consistency and availability

**Cons**:
- ⚠️ Complexity in managing different strategies
- ⚠️ Minority partition becomes read-only (CP contexts)
- ⚠️ Conflict resolution needed (AP contexts)

**Cost**: $50,000/year

**Risk**: **Low** - Industry best practice

### Option 2: Full CP (Consistency + Partition Tolerance)

**Description**: All data requires quorum, minority partition becomes fully read-only

**Pros**:
- ✅ Strong consistency everywhere
- ✅ No data conflicts
- ✅ Simple to understand

**Cons**:
- ❌ Minority partition completely unavailable for writes
- ❌ Poor user experience during partitions
- ❌ Unnecessary for non-critical data

**Cost**: $40,000/year

**Risk**: **Medium** - Availability impact

### Option 3: Full AP (Availability + Partition Tolerance)

**Description**: All partitions accept writes, resolve conflicts later

**Pros**:
- ✅ Full availability during partitions
- ✅ Good user experience

**Cons**:
- ❌ Data conflicts for critical data
- ❌ Complex conflict resolution
- ❌ Risk of data corruption
- ❌ Unacceptable for orders/payments

**Cost**: $45,000/year

**Risk**: **High** - Data consistency issues

## Decision Outcome

**Chosen Option**: **Quorum-Based Consensus with Context-Specific CAP Trade-offs (Option 1)**

### Rationale

Context-specific CAP trade-offs provide optimal balance:

1. **Critical Data Protection**: CP for orders, payments, inventory prevents financial errors
2. **Availability**: AP for product catalog, profiles maintains user experience
3. **Pragmatic**: Different contexts have different consistency needs
4. **Proven**: Industry-standard approach (Cassandra, DynamoDB)
5. **Flexible**: Easy to adjust per context
6. **Automated**: Partition detection and handling automated

### CAP Trade-off Matrix

| Bounded Context | CAP Choice | Partition Behavior | Rationale |
|-----------------|------------|-------------------|-----------|
| **Orders** | CP | Minority read-only | Financial accuracy critical |
| **Payments** | CP | Minority read-only | Payment integrity required |
| **Inventory** | CP | Minority read-only | Prevent overselling |
| **Product Catalog** | AP | Both accept writes | Stale data acceptable |
| **Customer Profiles** | AP | Both accept writes | Profile conflicts rare |
| **Reviews** | AP | Both accept writes | Review conflicts acceptable |
| **Promotions** | AP | Both accept writes | Promotion changes infrequent |
| **Shopping Carts** | AP | Regional isolation | Merge on checkout |
| **Sessions** | AP | Regional isolation | No cross-region sessions |

### Partition Detection Architecture

**Multi-Layer Partition Detection**:
```typescript
interface PartitionDetector {
  // Layer 1: Direct connectivity check
  checkDirectConnectivity(): Promise<boolean>;
  
  // Layer 2: Application-level heartbeat
  checkApplicationHeartbeat(): Promise<boolean>;
  
  // Layer 3: External monitoring
  checkExternalMonitoring(): Promise<boolean>;
  
  // Consensus: Agree on partition state
  reachConsensus(): Promise<PartitionState>;
}

class NetworkPartitionDetector implements PartitionDetector {
  
  private readonly HEARTBEAT_INTERVAL = 10000; // 10 seconds
  private readonly HEARTBEAT_TIMEOUT = 30000; // 30 seconds
  private readonly CONSENSUS_THRESHOLD = 2; // 2 out of 3 checks
  
  async detectPartition(): Promise<PartitionDetection> {
    const checks = await Promise.all([
      this.checkDirectConnectivity(),
      this.checkApplicationHeartbeat(),
      this.checkExternalMonitoring(),
    ]);
    
    const failedChecks = checks.filter(c => !c).length;
    
    if (failedChecks >= this.CONSENSUS_THRESHOLD) {
      return {
        partitioned: true,
        detectedAt: new Date(),
        failedChecks: failedChecks,
        confidence: failedChecks === 3 ? 'HIGH' : 'MEDIUM',
      };
    }
    
    return {
      partitioned: false,
      detectedAt: new Date(),
      failedChecks: 0,
      confidence: 'HIGH',
    };
  }
  
  async checkDirectConnectivity(): Promise<boolean> {
    try {
      // TCP connection to other region
      const response = await fetch('https://tokyo-internal.ecommerce.com/health', {
        timeout: 5000,
      });
      return response.ok;
    } catch (error) {
      return false;
    }
  }
  
  async checkApplicationHeartbeat(): Promise<boolean> {
    try {
      // Application-level heartbeat via database
      const lastHeartbeat = await this.getLastHeartbeat('tokyo');
      const age = Date.now() - lastHeartbeat.getTime();
      return age < this.HEARTBEAT_TIMEOUT;
    } catch (error) {
      return false;
    }
  }
  
  async checkExternalMonitoring(): Promise<boolean> {
    try {
      // Third-party monitoring service (e.g., Pingdom, StatusCake)
      const response = await fetch('https://api.monitoring-service.com/check', {
        method: 'POST',
        body: JSON.stringify({
          source: 'taiwan',
          target: 'tokyo',
        }),
      });
      const result = await response.json();
      return result.reachable;
    } catch (error) {
      return false;
    }
  }
}
```

**Heartbeat Implementation**:
```java
@Component
@Scheduled(fixedRate = 10000) // Every 10 seconds
public class CrossRegionHeartbeat {
    
    private final JdbcTemplate jdbcTemplate;
    private final String currentRegion;
    
    public void sendHeartbeat() {
        try {
            // Write heartbeat to shared database table
            jdbcTemplate.update(
                "INSERT INTO region_heartbeats (region, timestamp, status) " +
                "VALUES (?, ?, ?) " +
                "ON CONFLICT (region) DO UPDATE SET timestamp = ?, status = ?",
                currentRegion,
                Instant.now(),
                "HEALTHY",
                Instant.now(),
                "HEALTHY"
            );
            
            // Check other region's heartbeat
            Instant lastHeartbeat = jdbcTemplate.queryForObject(
                "SELECT timestamp FROM region_heartbeats WHERE region = ?",
                Instant.class,
                getOtherRegion()
            );
            
            Duration age = Duration.between(lastHeartbeat, Instant.now());
            
            if (age.getSeconds() > 30) {
                // Other region heartbeat stale - possible partition
                handlePossiblePartition(age);
            }
            
        } catch (Exception e) {
            logger.error("Heartbeat failed", e);
            handleHeartbeatFailure(e);
        }
    }
    
    private void handlePossiblePartition(Duration age) {
        logger.warn("Possible network partition detected. Last heartbeat: {} seconds ago", 
            age.getSeconds());
        
        // Trigger partition detection
        partitionDetector.detectPartition();
    }
}
```

### Quorum-Based Write Strategy

**Quorum Configuration**:
```java
public class QuorumConfiguration {
    
    // Total number of regions
    private static final int N = 2; // Taiwan + Tokyo
    
    // Write quorum (majority)
    private static final int W = 2; // Both regions must acknowledge
    
    // Read quorum
    private static final int R = 1; // Read from any region
    
    // Quorum formula: W + R > N ensures consistency
    // 2 + 1 > 2 ✓
}

@Service
public class QuorumWriteService {
    
    private final OrderRepository taiwanRepo;
    private final OrderRepository tokyoRepo;
    private final PartitionDetector partitionDetector;
    
    public Order createOrder(CreateOrderCommand command) {
        // Check for partition
        if (partitionDetector.isPartitioned()) {
            return handlePartitionedWrite(command);
        }
        
        // Normal case: write to both regions
        return writeWithQuorum(command);
    }
    
    private Order writeWithQuorum(CreateOrderCommand command) {
        Order order = Order.create(command);
        
        // Write to both regions in parallel
        CompletableFuture<Order> taiwanWrite = CompletableFuture.supplyAsync(
            () -> taiwanRepo.save(order)
        );
        
        CompletableFuture<Order> tokyoWrite = CompletableFuture.supplyAsync(
            () -> tokyoRepo.save(order)
        );
        
        try {
            // Wait for both writes (quorum = 2)
            CompletableFuture.allOf(taiwanWrite, tokyoWrite)
                .get(5, TimeUnit.SECONDS);
            
            return order;
            
        } catch (TimeoutException e) {
            // One region didn't respond - possible partition
            throw new QuorumNotAchievedException(
                "Failed to achieve write quorum", e
            );
        }
    }
    
    private Order handlePartitionedWrite(CreateOrderCommand command) {
        // Determine if we're in majority partition
        if (partitionDetector.isInMajorityPartition()) {
            // Majority partition: allow writes
            return writeToLocalRegion(command);
        } else {
            // Minority partition: reject writes
            throw new PartitionedWriteException(
                "Cannot write to minority partition. System is read-only."
            );
        }
    }
}
```

### Split-Brain Prevention

**Fencing Mechanism**:
```java
@Component
public class PartitionFencingService {
    
    private final DynamoDbClient dynamoDb;
    private final String FENCING_TABLE = "region_fencing";
    
    public FencingToken acquireFencingToken(String region) {
        try {
            // Attempt to acquire fencing token with conditional write
            PutItemRequest request = PutItemRequest.builder()
                .tableName(FENCING_TABLE)
                .item(Map.of(
                    "partition_key", AttributeValue.builder().s("ACTIVE_REGION").build(),
                    "region", AttributeValue.builder().s(region).build(),
                    "timestamp", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build(),
                    "token", AttributeValue.builder().s(UUID.randomUUID().toString()).build()
                ))
                .conditionExpression("attribute_not_exists(partition_key) OR #ts < :expiry")
                .expressionAttributeNames(Map.of("#ts", "timestamp"))
                .expressionAttributeValues(Map.of(
                    ":expiry", AttributeValue.builder()
                        .n(String.valueOf(System.currentTimeMillis() - 60000)) // 1 minute expiry
                        .build()
                ))
                .build();
            
            dynamoDb.putItem(request);
            
            return new FencingToken(region, true);
            
        } catch (ConditionalCheckFailedException e) {
            // Another region holds the token
            return new FencingToken(region, false);
        }
    }
    
    public void enterReadOnlyMode(String reason) {
        logger.warn("Entering read-only mode: {}", reason);
        
        // Set global read-only flag
        applicationContext.setReadOnly(true);
        
        // Reject all write operations
        writeOperationGuard.blockWrites();
        
        // Send alerts
        alertService.sendAlert(
            "Region entered read-only mode",
            AlertSeverity.HIGH,
            Map.of("reason", reason)
        );
        
        // Update monitoring
        metricsService.recordReadOnlyMode(true);
    }
}
```

**Third-Party Arbitrator**:
```typescript
// Use AWS service in neutral region (Singapore) as arbitrator
class ThirdPartyArbitrator {
  
  private readonly dynamoDb: DynamoDB;
  private readonly ARBITRATOR_TABLE = 'partition_arbitrator';
  
  async determineActiveRegion(): Promise<string> {
    // Both regions attempt to register with arbitrator
    // First one to register becomes active
    
    try {
      await this.dynamoDb.putItem({
        TableName: this.ARBITRATOR_TABLE,
        Item: {
          partition_key: { S: 'ACTIVE_PARTITION' },
          region: { S: this.currentRegion },
          timestamp: { N: Date.now().toString() },
        },
        ConditionExpression: 'attribute_not_exists(partition_key)',
      });
      
      // Successfully registered - we are active
      return this.currentRegion;
      
    } catch (error) {
      if (error.code === 'ConditionalCheckFailedException') {
        // Another region already registered
        const item = await this.dynamoDb.getItem({
          TableName: this.ARBITRATOR_TABLE,
          Key: {
            partition_key: { S: 'ACTIVE_PARTITION' },
          },
        });
        
        return item.Item.region.S;
      }
      
      throw error;
    }
  }
}
```

### Conflict Resolution for AP Contexts

**Conflict Detection**:
```java
public class ConflictDetector {
    
    public List<DataConflict> detectConflicts(String region1, String region2) {
        List<DataConflict> conflicts = new ArrayList<>();
        
        // Check product catalog conflicts
        conflicts.addAll(detectProductConflicts(region1, region2));
        
        // Check customer profile conflicts
        conflicts.addAll(detectCustomerConflicts(region1, region2));
        
        // Check review conflicts
        conflicts.addAll(detectReviewConflicts(region1, region2));
        
        return conflicts;
    }
    
    private List<DataConflict> detectProductConflicts(String r1, String r2) {
        List<Product> products1 = productRepository.findAll(r1);
        List<Product> products2 = productRepository.findAll(r2);
        
        List<DataConflict> conflicts = new ArrayList<>();
        
        for (Product p1 : products1) {
            Product p2 = findById(products2, p1.getId());
            
            if (p2 != null && !p1.equals(p2)) {
                // Same product, different versions
                if (p1.getVersion() != p2.getVersion()) {
                    conflicts.add(new DataConflict(
                        "Product",
                        p1.getId(),
                        p1,
                        p2,
                        ConflictType.VERSION_MISMATCH
                    ));
                }
            }
        }
        
        return conflicts;
    }
}
```

**Conflict Resolution Strategies**:
```java
public class ConflictResolver {
    
    public void resolveConflicts(List<DataConflict> conflicts) {
        for (DataConflict conflict : conflicts) {
            switch (conflict.getType()) {
                case VERSION_MISMATCH:
                    resolveVersionConflict(conflict);
                    break;
                case CONCURRENT_UPDATE:
                    resolveConcurrentUpdate(conflict);
                    break;
                case DELETE_UPDATE:
                    resolveDeleteUpdate(conflict);
                    break;
            }
        }
    }
    
    private void resolveVersionConflict(DataConflict conflict) {
        // Last-Write-Wins based on timestamp
        Object newer = conflict.getVersion1().getLastModified()
            .isAfter(conflict.getVersion2().getLastModified())
            ? conflict.getVersion1()
            : conflict.getVersion2();
        
        // Update both regions with newer version
        updateBothRegions(conflict.getEntityType(), conflict.getEntityId(), newer);
        
        // Log resolution
        logger.info("Resolved version conflict for {} {}: chose version from {}",
            conflict.getEntityType(),
            conflict.getEntityId(),
            newer.getLastModifiedRegion()
        );
    }
    
    private void resolveConcurrentUpdate(DataConflict conflict) {
        // Merge strategy for concurrent updates
        Object merged = mergeVersions(
            conflict.getVersion1(),
            conflict.getVersion2()
        );
        
        updateBothRegions(conflict.getEntityType(), conflict.getEntityId(), merged);
    }
    
    private Object mergeVersions(Object v1, Object v2) {
        // Field-level merge
        // Keep non-null values from both versions
        // For conflicts, use Last-Write-Wins per field
        
        if (v1 instanceof Product p1 && v2 instanceof Product p2) {
            return Product.builder()
                .id(p1.getId())
                .name(p1.getNameLastModified().isAfter(p2.getNameLastModified()) 
                    ? p1.getName() : p2.getName())
                .price(p1.getPriceLastModified().isAfter(p2.getPriceLastModified())
                    ? p1.getPrice() : p2.getPrice())
                .description(p1.getDescLastModified().isAfter(p2.getDescLastModified())
                    ? p1.getDescription() : p2.getDescription())
                .build();
        }
        
        throw new UnsupportedOperationException("Merge not supported for type");
    }
}
```


### Partition Recovery

**Automatic Recovery Process**:
```java
@Component
public class PartitionRecoveryService {
    
    @Scheduled(fixedRate = 30000) // Check every 30 seconds
    public void checkPartitionRecovery() {
        if (partitionDetector.isPartitioned()) {
            // Still partitioned
            return;
        }
        
        if (wasPartitioned()) {
            // Partition healed - initiate recovery
            initiateRecovery();
        }
    }
    
    private void initiateRecovery() {
        logger.info("Network partition healed. Initiating recovery...");
        
        try {
            // Phase 1: Detect conflicts
            List<DataConflict> conflicts = conflictDetector.detectConflicts(
                "taiwan", "tokyo"
            );
            
            logger.info("Detected {} conflicts during partition", conflicts.size());
            
            // Phase 2: Resolve conflicts
            conflictResolver.resolveConflicts(conflicts);
            
            // Phase 3: Resume normal replication
            replicationService.resumeReplication();
            
            // Phase 4: Exit read-only mode (if applicable)
            if (applicationContext.isReadOnly()) {
                fencingService.exitReadOnlyMode();
            }
            
            // Phase 5: Verify consistency
            boolean consistent = consistencyChecker.verifyConsistency();
            
            if (!consistent) {
                throw new RecoveryException("Consistency check failed after recovery");
            }
            
            // Phase 6: Send notifications
            alertService.sendAlert(
                "Partition recovery completed successfully",
                AlertSeverity.INFO,
                Map.of(
                    "conflicts_resolved", conflicts.size(),
                    "duration", getPartitionDuration()
                )
            );
            
            // Phase 7: Clear partition state
            clearPartitionState();
            
        } catch (Exception e) {
            logger.error("Partition recovery failed", e);
            alertService.sendAlert(
                "Partition recovery failed - manual intervention required",
                AlertSeverity.CRITICAL,
                Map.of("error", e.getMessage())
            );
        }
    }
}
```

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | Partition-aware code, CAP understanding | Training, patterns, libraries |
| Operations Team | High | Partition monitoring, recovery procedures | Automation, runbooks, alerts |
| End Users (CP contexts) | Medium | Write unavailability in minority partition | Clear error messages, retry logic |
| End Users (AP contexts) | Low | Transparent operation | Conflict resolution |
| Business | Medium | Potential write unavailability | Clear SLAs, monitoring |

### Impact Radius

**Selected Impact Radius**: **Enterprise**

Affects:
- All application services
- All data stores
- Write operations (CP contexts)
- Conflict resolution (AP contexts)
- Monitoring and alerting
- Operational procedures

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| False partition detection | Low | High | Multi-layer detection, consensus |
| Split-brain | Low | Critical | Quorum, fencing, arbitrator |
| Data conflicts (AP) | Medium | Medium | Automated conflict resolution |
| Write unavailability (CP) | Medium | High | Clear error messages, retry logic |
| Recovery failure | Low | High | Automated recovery, manual procedures |

**Overall Risk Level**: **Medium**

## Implementation Plan

### Phase 1: Partition Detection (Month 1)

**Objectives**:
- Implement multi-layer partition detection
- Deploy heartbeat system
- Set up external monitoring

**Tasks**:
- [ ] Implement partition detector
- [ ] Deploy heartbeat system
- [ ] Configure external monitoring
- [ ] Set up monitoring dashboards
- [ ] Test partition detection

**Success Criteria**:
- Partition detection < 30 seconds
- False positive rate < 1%
- Monitoring operational

### Phase 2: Split-Brain Prevention (Month 2)

**Objectives**:
- Implement quorum-based writes
- Deploy fencing mechanism
- Set up third-party arbitrator

**Tasks**:
- [ ] Implement quorum write service
- [ ] Deploy fencing service
- [ ] Configure DynamoDB arbitrator
- [ ] Implement read-only mode
- [ ] Test split-brain prevention

**Success Criteria**:
- Quorum writes working
- Fencing mechanism operational
- No split-brain in tests

### Phase 3: Conflict Resolution (Month 3)

**Objectives**:
- Implement conflict detection
- Deploy conflict resolution
- Test AP context behavior

**Tasks**:
- [ ] Implement conflict detector
- [ ] Deploy conflict resolver
- [ ] Test Last-Write-Wins
- [ ] Test merge strategies
- [ ] Validate AP contexts

**Success Criteria**:
- Conflict detection working
- Resolution automated
- AP contexts operational

### Phase 4: Partition Recovery (Month 4)

**Objectives**:
- Implement automated recovery
- Test recovery procedures
- Validate end-to-end flow

**Tasks**:
- [ ] Implement recovery service
- [ ] Test automatic recovery
- [ ] Create manual procedures
- [ ] Conduct partition drills
- [ ] Document procedures

**Success Criteria**:
- Automated recovery working
- Manual procedures documented
- Drills successful

### Phase 5: Production Readiness (Month 5-6)

**Objectives**:
- Comprehensive testing
- Team training
- Production deployment

**Tasks**:
- [ ] Conduct partition simulation drills
- [ ] Train operations team
- [ ] Update monitoring
- [ ] Deploy to production
- [ ] Monitor for 30 days

**Success Criteria**:
- All tests passing
- Team trained
- Production stable

### Rollback Strategy

**Trigger Conditions**:
- Excessive false positives
- Split-brain scenarios
- Data corruption
- Operational issues

**Rollback Steps**:
1. **Immediate**: Disable partition detection
2. **Revert**: Return to basic health checks
3. **Manual**: Manual partition handling
4. **Investigation**: Root cause analysis

**Rollback Time**: < 1 hour

## Monitoring and Success Criteria

### Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Partition Detection Time | < 30 seconds | Monitoring logs |
| False Positive Rate | < 1% | Detection logs |
| Split-Brain Incidents | 0 | Incident reports |
| Conflict Resolution Rate | > 99% | Resolution logs |
| Recovery Time | < 5 minutes | Recovery logs |
| Data Consistency | 100% | Validation queries |

### Key Metrics

```typescript
const partitionMetrics = {
  // Partition detection
  'partition.detected.total': 'Count',
  'partition.detection.time_seconds': 'Seconds',
  'partition.false_positives': 'Count',
  
  // Split-brain prevention
  'partition.quorum.failures': 'Count',
  'partition.fencing.activations': 'Count',
  'partition.readonly_mode.duration': 'Seconds',
  
  // Conflict resolution
  'partition.conflicts.detected': 'Count',
  'partition.conflicts.resolved': 'Count',
  'partition.conflicts.manual': 'Count',
  
  // Recovery
  'partition.recovery.attempts': 'Count',
  'partition.recovery.successes': 'Count',
  'partition.recovery.duration_seconds': 'Seconds',
};
```

### Monitoring Dashboards

**Partition Status Dashboard**:
- Current partition state
- Heartbeat status
- Quorum status
- Read-only mode status
- Recent partition events

**Conflict Resolution Dashboard**:
- Conflicts detected
- Conflicts resolved
- Resolution success rate
- Manual interventions
- Conflict types

### Review Schedule

- **Daily**: Partition metrics review
- **Weekly**: Conflict resolution review
- **Monthly**: Partition drill
- **Quarterly**: Comprehensive review

## Consequences

### Positive Consequences

- ✅ **Prevents Split-Brain**: Quorum and fencing prevent data corruption
- ✅ **Maintains Availability**: AP contexts remain available
- ✅ **Automated**: Detection and recovery automated
- ✅ **Flexible**: Different strategies per context
- ✅ **Clear Guarantees**: Well-defined consistency per context
- ✅ **Quick Detection**: < 30 second partition detection
- ✅ **Quick Recovery**: < 5 minute automated recovery

### Negative Consequences

- ⚠️ **Write Unavailability**: CP contexts unavailable in minority partition
- ⚠️ **Complexity**: Complex partition handling logic
- ⚠️ **Conflicts**: AP contexts require conflict resolution
- ⚠️ **Cost**: $50,000/year for infrastructure
- ⚠️ **Monitoring**: Comprehensive monitoring required
- ⚠️ **Training**: Team needs CAP theorem understanding

### Technical Debt

**Identified Debt**:
1. Basic conflict resolution (Last-Write-Wins only)
2. Manual intervention for complex conflicts
3. Limited partition simulation testing
4. Basic arbitrator (DynamoDB only)

**Debt Repayment Plan**:
- **Q2 2026**: Advanced conflict resolution (CRDTs, vector clocks)
- **Q3 2026**: Automated complex conflict resolution
- **Q4 2026**: Chaos engineering for partition testing
- **2027**: Multi-arbitrator consensus (Raft/Paxos)

## Related Decisions

- [ADR-037: Active-Active Multi-Region Architecture](037-active-active-multi-region-architecture.md) - Multi-region foundation
- [ADR-038: Cross-Region Data Replication Strategy](038-cross-region-data-replication-strategy.md) - Replication during partitions
- [ADR-039: Regional Failover and Failback Strategy](039-regional-failover-failback-strategy.md) - Failover vs partition handling
- [ADR-041: Data Residency and Sovereignty Strategy](041-data-residency-sovereignty-strategy.md) - Compliance during partitions

## Notes

### CAP Theorem Refresher

**CAP Theorem**: In a distributed system, you can only guarantee 2 out of 3:
- **C**onsistency: All nodes see the same data
- **A**vailability: Every request receives a response
- **P**artition Tolerance: System continues despite network partitions

**Our Choices**:
- **CP (Orders, Payments, Inventory)**: Sacrifice availability for consistency
- **AP (Catalog, Profiles, Reviews)**: Sacrifice consistency for availability

### Partition vs Failover

**Network Partition**:
- Both regions operational
- Cannot communicate
- Risk of split-brain
- Requires quorum/fencing

**Regional Failure**:
- One region down
- Clear failure
- No split-brain risk
- Failover to healthy region

### Submarine Cable Risks

**Taiwan's Internet Connectivity**:
- 99% via undersea cables
- 14 cables connecting Taiwan
- Vulnerable to:
  - Earthquakes
  - Ship anchors
  - Deliberate sabotage
  - Military action

**Historical Incidents**:
- 2006: Hengchun earthquake cut 8 cables
- 2008: Multiple cable cuts
- Regular maintenance disruptions

**Mitigation**: Multi-path detection, satellite backup (future)

### Testing Partition Scenarios

**Partition Simulation**:
```bash
# Simulate partition by blocking cross-region traffic
# Taiwan region
sudo iptables -A OUTPUT -d tokyo-region-ip -j DROP
sudo iptables -A INPUT -s tokyo-region-ip -j DROP

# Wait for partition detection (< 30 seconds)

# Verify behavior:
# - CP contexts: writes fail in minority
# - AP contexts: writes succeed
# - Monitoring: partition detected

# Heal partition
sudo iptables -D OUTPUT -d tokyo-region-ip -j DROP
sudo iptables -D INPUT -s tokyo-region-ip -j DROP

# Verify recovery:
# - Conflicts detected and resolved
# - Replication resumed
# - Normal operation restored
```

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
