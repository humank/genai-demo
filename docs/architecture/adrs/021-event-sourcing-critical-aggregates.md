---
adr_number: 021
title: "Event Sourcing for Critical Aggregates (Optional Pattern)"
date: 2025-10-25
status: "proposed"
supersedes: []
superseded_by: null
related_adrs: [020, 025, 026]
affected_viewpoints: ["information", "functional"]
affected_perspectives: ["availability", "evolution", "performance"]
---

# ADR-021: Event Sourcing for Critical Aggregates (Optional Pattern)

## Status

**Proposed** - 2025-10-25

*Note: This is an optional pattern for future consideration. Not required for initial implementation.*

## Context

### Problem Statement

Certain critical aggregates in the E-Commerce Platform could benefit from event sourcing to:

**Business Requirements**:

- **Complete Audit Trail**: Full history of all state changes
- **Temporal Queries**: Query state at any point in time
- **Regulatory Compliance**: Meet audit and compliance requirements
- **Debugging**: Reproduce issues by replaying events
- **Analytics**: Analyze business patterns from event history
- **Undo/Redo**: Support complex business workflows

**Technical Challenges**:

- Current state-based persistence loses history
- Difficult to audit changes
- Cannot replay past states
- Limited debugging capabilities
- Complex temporal queries
- Compliance requirements for financial data

**Candidate Aggregates**:

- **Order**: Complete order lifecycle tracking
- **Payment**: Financial transaction history
- **Inventory**: Stock movement tracking
- **Pricing**: Price change history
- **Customer**: Account activity history

### Business Context

**Business Drivers**:

- Regulatory compliance (financial auditing)
- Customer dispute resolution
- Business intelligence and analytics
- Fraud detection and prevention
- Customer service improvements

**Constraints**:

- Budget: $80,000 for implementation
- Timeline: 3 months
- Team: 2 senior developers
- Must coexist with existing CRUD approach
- Cannot impact current performance
- Gradual adoption strategy

### Technical Context

**Current Approach**:

- Traditional CRUD with JPA
- Domain events for integration
- Limited audit logging
- No event history

**Target Approach**:

- Event sourcing for critical aggregates
- Event store for persistence
- Projections for read models
- Hybrid approach (not all aggregates)

## Decision Drivers

1. **Auditability**: Complete audit trail for compliance
2. **Temporal Queries**: Query historical states
3. **Debugging**: Reproduce issues from events
4. **Analytics**: Rich event data for analysis
5. **Complexity**: Manage additional complexity
6. **Performance**: Maintain acceptable performance
7. **Team Skills**: Team capability to implement
8. **Gradual Adoption**: Start small, expand if successful

## Considered Options

### Option 1: Selective Event Sourcing (Recommended)

**Description**: Apply event sourcing only to critical aggregates that benefit most

**Event Sourcing Architecture**:

```java
// Event-sourced aggregate
@AggregateRoot
public class Order extends EventSourcedAggregateRoot {
    
    private OrderId id;
    private CustomerId customerId;
    private OrderStatus status;
    private List<OrderItem> items;
    private Money totalAmount;
    
    // Constructor for new aggregate
    public Order(OrderId id, CustomerId customerId, List<OrderItem> items) {
        // Apply event (not save state directly)
        apply(OrderCreatedEvent.create(id, customerId, items));
    }
    
    // Constructor for reconstitution from events
    protected Order(OrderId id) {
        this.id = id;
    }
    
    // Business method
    public void submit() {
        if (status != OrderStatus.DRAFT) {
            throw new BusinessRuleViolationException("Order already submitted");
        }
        
        apply(OrderSubmittedEvent.create(id, LocalDateTime.now()));
    }
    
    public void cancel(String reason) {
        if (status == OrderStatus.CANCELLED) {
            throw new BusinessRuleViolationException("Order already cancelled");
        }
        
        apply(OrderCancelledEvent.create(id, reason, LocalDateTime.now()));
    }
    
    // Event handlers (update state)
    @EventHandler
    private void on(OrderCreatedEvent event) {
        this.id = event.orderId();
        this.customerId = event.customerId();
        this.items = event.items();
        this.status = OrderStatus.DRAFT;
        this.totalAmount = calculateTotal(items);
    }
    
    @EventHandler
    private void on(OrderSubmittedEvent event) {
        this.status = OrderStatus.PENDING;
    }
    
    @EventHandler
    private void on(OrderCancelledEvent event) {
        this.status = OrderStatus.CANCELLED;
    }
}
```

**Event Store Interface**:

```java
public interface EventStore {
    
    /**

     * Save events for an aggregate

     */
    void saveEvents(String aggregateId, 
                   List<DomainEvent> events, 
                   long expectedVersion);
    
    /**

     * Load all events for an aggregate

     */
    List<DomainEvent> getEvents(String aggregateId);
    
    /**

     * Load events after a specific version

     */
    List<DomainEvent> getEventsAfterVersion(String aggregateId, long version);
    
    /**

     * Load events in a time range

     */
    List<DomainEvent> getEventsBetween(String aggregateId, 
                                       LocalDateTime start, 
                                       LocalDateTime end);
}
```

**Event Store Implementation (PostgreSQL)**:

```sql
-- Event store table
CREATE TABLE event_store (
    event_id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_data JSONB NOT NULL,
    event_metadata JSONB,
    version BIGINT NOT NULL,
    occurred_on TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT uk_aggregate_version UNIQUE (aggregate_id, version)
);

-- Indexes for performance
CREATE INDEX idx_event_store_aggregate 
ON event_store(aggregate_id, version);

CREATE INDEX idx_event_store_type 
ON event_store(aggregate_type, occurred_on);

CREATE INDEX idx_event_store_occurred 
ON event_store(occurred_on);

-- Snapshots table for performance
CREATE TABLE aggregate_snapshots (
    snapshot_id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    snapshot_data JSONB NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT uk_aggregate_snapshot UNIQUE (aggregate_id, version)
);

CREATE INDEX idx_snapshots_aggregate 
ON aggregate_snapshots(aggregate_id, version DESC);
```

**Event Store Repository**:

```java
@Repository
public class PostgresEventStore implements EventStore {
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public void saveEvents(String aggregateId, 
                          List<DomainEvent> events, 
                          long expectedVersion) {
        
        // Optimistic concurrency check
        Long currentVersion = getCurrentVersion(aggregateId);
        if (currentVersion != null && currentVersion != expectedVersion) {
            throw new ConcurrencyException(
                "Aggregate version mismatch. Expected: " + expectedVersion + 
                ", Current: " + currentVersion);
        }
        
        // Save events
        long version = expectedVersion;
        for (DomainEvent event : events) {
            version++;
            
            jdbcTemplate.update("""
                INSERT INTO event_store (
                    event_id, aggregate_id, aggregate_type, event_type,
                    event_data, event_metadata, version, occurred_on
                ) VALUES (?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?)
                """,
                event.getEventId(),
                aggregateId,
                getAggregateType(event),
                event.getEventType(),
                serializeEvent(event),
                serializeMetadata(event),
                version,
                event.getOccurredOn()
            );
        }
    }
    
    @Override
    public List<DomainEvent> getEvents(String aggregateId) {
        return jdbcTemplate.query("""
            SELECT event_data, event_type
            FROM event_store
            WHERE aggregate_id = ?
            ORDER BY version ASC
            """,
            (rs, rowNum) -> deserializeEvent(
                rs.getString("event_data"),
                rs.getString("event_type")
            ),
            aggregateId
        );
    }
    
    @Override
    public List<DomainEvent> getEventsBetween(String aggregateId,
                                              LocalDateTime start,
                                              LocalDateTime end) {
        return jdbcTemplate.query("""
            SELECT event_data, event_type
            FROM event_store
            WHERE aggregate_id = ?
              AND occurred_on BETWEEN ? AND ?
            ORDER BY version ASC
            """,
            (rs, rowNum) -> deserializeEvent(
                rs.getString("event_data"),
                rs.getString("event_type")
            ),
            aggregateId, start, end
        );
    }
    
    private Long getCurrentVersion(String aggregateId) {
        return jdbcTemplate.queryForObject("""
            SELECT MAX(version)
            FROM event_store
            WHERE aggregate_id = ?
            """,
            Long.class,
            aggregateId
        );
    }
}
```

**Aggregate Repository with Event Sourcing**:

```java
@Repository
public class EventSourcedOrderRepository implements OrderRepository {
    
    private final EventStore eventStore;
    private final SnapshotStore snapshotStore;
    
    @Override
    public Optional<Order> findById(OrderId orderId) {
        String aggregateId = orderId.getValue();
        
        // Try to load from snapshot
        Optional<AggregateSnapshot> snapshot = 
            snapshotStore.getLatestSnapshot(aggregateId);
        
        Order order;
        long version;
        
        if (snapshot.isPresent()) {
            // Reconstitute from snapshot
            order = deserializeSnapshot(snapshot.get());
            version = snapshot.get().getVersion();
            
            // Load events after snapshot
            List<DomainEvent> events = 
                eventStore.getEventsAfterVersion(aggregateId, version);
            order.loadFromHistory(events);
        } else {
            // Load all events
            List<DomainEvent> events = eventStore.getEvents(aggregateId);
            if (events.isEmpty()) {
                return Optional.empty();
            }
            
            // Reconstitute from events
            order = new Order(orderId);
            order.loadFromHistory(events);
        }
        
        return Optional.of(order);
    }
    
    @Override
    @Transactional
    public Order save(Order order) {
        List<DomainEvent> uncommittedEvents = order.getUncommittedEvents();
        
        if (!uncommittedEvents.isEmpty()) {
            // Save events
            eventStore.saveEvents(
                order.getId().getValue(),
                uncommittedEvents,
                order.getVersion()
            );
            
            // Mark events as committed
            order.markEventsAsCommitted();
            
            // Create snapshot if needed
            if (shouldCreateSnapshot(order)) {
                snapshotStore.saveSnapshot(
                    order.getId().getValue(),
                    serializeAggregate(order),
                    order.getVersion()
                );
            }
        }
        
        return order;
    }
    
    private boolean shouldCreateSnapshot(Order order) {
        // Create snapshot every 50 events
        return order.getVersion() % 50 == 0;
    }
}
```

**Projection for Read Model**:

```java
@Component
public class OrderProjection {
    
    private final OrderReadModelRepository readModelRepository;
    
    @EventListener
    @Transactional
    public void on(OrderCreatedEvent event) {
        OrderReadModel readModel = new OrderReadModel(
            event.orderId().getValue(),
            event.customerId().getValue(),
            OrderStatus.DRAFT,
            event.totalAmount(),
            event.occurredOn()
        );
        
        readModelRepository.save(readModel);
    }
    
    @EventListener
    @Transactional
    public void on(OrderSubmittedEvent event) {
        OrderReadModel readModel = readModelRepository
            .findById(event.orderId().getValue())
            .orElseThrow();
        
        readModel.setStatus(OrderStatus.PENDING);
        readModel.setSubmittedAt(event.occurredOn());
        
        readModelRepository.save(readModel);
    }
    
    @EventListener
    @Transactional
    public void on(OrderCancelledEvent event) {
        OrderReadModel readModel = readModelRepository
            .findById(event.orderId().getValue())
            .orElseThrow();
        
        readModel.setStatus(OrderStatus.CANCELLED);
        readModel.setCancelledAt(event.occurredOn());
        readModel.setCancellationReason(event.reason());
        
        readModelRepository.save(readModel);
    }
}
```

**Temporal Queries**:

```java
@Service
public class OrderHistoryService {
    
    private final EventStore eventStore;
    
    /**

     * Get order state at specific point in time

     */
    public Order getOrderAtTime(OrderId orderId, LocalDateTime timestamp) {
        List<DomainEvent> events = eventStore.getEventsBetween(
            orderId.getValue(),
            LocalDateTime.MIN,
            timestamp
        );
        
        Order order = new Order(orderId);
        order.loadFromHistory(events);
        
        return order;
    }
    
    /**

     * Get all changes to order in time range

     */
    public List<OrderChange> getOrderChanges(OrderId orderId,
                                             LocalDateTime start,
                                             LocalDateTime end) {
        List<DomainEvent> events = eventStore.getEventsBetween(
            orderId.getValue(),
            start,
            end
        );
        
        return events.stream()
            .map(this::toOrderChange)
            .collect(Collectors.toList());
    }
    
    /**

     * Replay events for debugging

     */
    public void replayOrderEvents(OrderId orderId) {
        List<DomainEvent> events = eventStore.getEvents(orderId.getValue());
        
        Order order = new Order(orderId);
        
        for (DomainEvent event : events) {
            logger.info("Replaying event: {} at {}",
                event.getEventType(),
                event.getOccurredOn());
            
            order.loadFromHistory(List.of(event));
            
            logger.info("Order state after event: {}", order);
        }
    }
}
```

**Pros**:

- ✅ Complete audit trail for critical aggregates
- ✅ Temporal queries and time travel
- ✅ Excellent debugging capabilities
- ✅ Natural fit for event-driven architecture
- ✅ Supports complex business workflows
- ✅ Regulatory compliance
- ✅ Can coexist with CRUD approach

**Cons**:

- ⚠️ Increased complexity
- ⚠️ Learning curve for team
- ⚠️ More storage required
- ⚠️ Eventual consistency for read models
- ⚠️ Snapshot management needed
- ⚠️ Event versioning challenges

**Cost**: $80,000 implementation + $10,000/year operational

**Risk**: **Medium** - Significant complexity increase

### Option 2: Full Event Sourcing

**Description**: Apply event sourcing to all aggregates

**Pros**:

- ✅ Consistent approach across system
- ✅ Maximum auditability
- ✅ Simplified architecture (one pattern)

**Cons**:

- ❌ Very high complexity
- ❌ Significant performance overhead
- ❌ Large storage requirements
- ❌ Steep learning curve
- ❌ Overkill for simple aggregates

**Cost**: $200,000 implementation + $30,000/year

**Risk**: **High** - Too complex for current needs

### Option 3: Enhanced Audit Logging

**Description**: Keep CRUD but add comprehensive audit logging

**Pros**:

- ✅ Simple to implement
- ✅ Low complexity
- ✅ Familiar to team

**Cons**:

- ❌ Limited temporal queries
- ❌ Cannot replay state
- ❌ Less powerful for debugging
- ❌ Not true event sourcing

**Cost**: $20,000 implementation

**Risk**: **Low** - But limited capabilities

## Decision Outcome

**Chosen Option**: **Selective Event Sourcing (Option 1)** - Proposed for future implementation

### Rationale

Selective event sourcing for critical aggregates (Order, Payment) provides the best balance of benefits and complexity, allowing gradual adoption and learning while delivering value for compliance and debugging.

**Implementation Recommendation**: Start with Order aggregate as pilot, expand to Payment if successful.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | New programming model | Training, pair programming, examples |
| Database Team | Medium | New storage patterns | Training, monitoring tools |
| QA Team | Medium | New testing approaches | Test frameworks, examples |
| Operations Team | Medium | New monitoring needs | Dashboards, runbooks |
| Compliance Team | Low | Better audit capabilities | Documentation, reports |

### Impact Radius Assessment

**Selected Impact Radius**: **Bounded Context**

Affects:

- Order bounded context (initially)
- Payment bounded context (future)
- Event store infrastructure
- Read model projections
- Reporting systems

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Complexity overwhelms team | Medium | High | Start small, extensive training |
| Performance issues | Low | Medium | Snapshots, caching, monitoring |
| Event versioning problems | Medium | Medium | Upcasting strategy, testing |
| Storage growth | Low | Low | Archiving strategy, compression |
| Eventual consistency issues | Medium | Medium | Clear documentation, monitoring |

**Overall Risk Level**: **Medium**

## Implementation Plan

### Phase 1: Proof of Concept (Month 1)

**Tasks**:

- [ ] Implement event store
- [ ] Create Order aggregate with event sourcing
- [ ] Build simple projection
- [ ] Test temporal queries
- [ ] Measure performance
- [ ] Document learnings

**Success Criteria**:

- POC working
- Performance acceptable
- Team understands approach

### Phase 2: Production Implementation (Month 2)

**Tasks**:

- [ ] Production-ready event store
- [ ] Snapshot mechanism
- [ ] Complete Order projections
- [ ] Migration strategy
- [ ] Monitoring and alerting
- [ ] Documentation

**Success Criteria**:

- Production ready
- All features working
- Monitoring in place

### Phase 3: Rollout and Validation (Month 3)

**Tasks**:

- [ ] Deploy to production
- [ ] Monitor performance
- [ ] Validate audit capabilities
- [ ] Gather team feedback
- [ ] Decide on expansion

**Success Criteria**:

- Production stable
- Benefits realized
- Team comfortable

### Rollback Strategy

**Trigger Conditions**:

- Unacceptable performance
- Team cannot maintain
- Benefits not realized

**Rollback Steps**:

1. Stop using event-sourced aggregates
2. Migrate to CRUD approach
3. Archive event store
4. Update documentation

**Rollback Time**: 2 weeks

## Monitoring and Success Criteria

### Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Audit Query Time | < 1s | Event store metrics |
| Aggregate Load Time | < 100ms | Application metrics |
| Storage Growth | < 10GB/month | Database metrics |
| Projection Lag | < 1s | Monitoring |
| Team Satisfaction | > 7/10 | Survey |

### Review Schedule

- **Monthly**: Performance and usage review
- **Quarterly**: Value assessment
- **Annually**: Expansion decision

## Consequences

### Positive Consequences

- ✅ **Complete Audit Trail**: Full history for compliance
- ✅ **Temporal Queries**: Query any historical state
- ✅ **Debugging**: Replay events to reproduce issues
- ✅ **Analytics**: Rich event data for analysis
- ✅ **Compliance**: Meet regulatory requirements
- ✅ **Flexibility**: Support complex workflows

### Negative Consequences

- ⚠️ **Complexity**: Significant increase in complexity
- ⚠️ **Learning Curve**: Team needs training
- ⚠️ **Storage**: More storage required
- ⚠️ **Eventual Consistency**: Read models lag behind
- ⚠️ **Maintenance**: More code to maintain

### Technical Debt

**Identified Debt**:

1. Event versioning strategy needed
2. Snapshot optimization required
3. Projection rebuild mechanism
4. Event archiving strategy

**Debt Repayment Plan**:

- **Q2 2026**: Event versioning framework
- **Q3 2026**: Snapshot optimization
- **Q4 2026**: Projection rebuild tools
- **Q1 2027**: Event archiving

## Related Decisions

- [ADR-020: Database Migration Strategy with Flyway](020-database-migration-strategy-flyway.md)
- [ADR-025: Saga Pattern for Distributed Transactions](025-saga-pattern-distributed-transactions.md)
- [ADR-026: CQRS Pattern for Read/Write Separation](026-cqrs-pattern-read-write-separation.md)

---

**Document Status**: 📋 Proposed (Optional Pattern)  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-04-25 (After evaluation period)

## Notes

### When to Use Event Sourcing

**Good Candidates**:

- ✅ Aggregates requiring complete audit trail
- ✅ Financial transactions
- ✅ Regulatory compliance requirements
- ✅ Complex business workflows
- ✅ Temporal queries needed

**Poor Candidates**:

- ❌ Simple CRUD entities
- ❌ Reference data
- ❌ High-volume, low-value data
- ❌ Frequently changing schemas

### Event Sourcing Best Practices

**DO**:

- ✅ Start with one aggregate
- ✅ Use snapshots for performance
- ✅ Version events properly
- ✅ Keep events immutable
- ✅ Use projections for queries
- ✅ Monitor projection lag

**DON'T**:

- ❌ Apply to all aggregates
- ❌ Store large payloads in events
- ❌ Modify past events
- ❌ Query event store directly
- ❌ Ignore event versioning
- ❌ Skip snapshot strategy
