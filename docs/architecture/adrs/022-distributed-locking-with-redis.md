---
adr_number: 022
title: "Distributed Locking with Redis"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [004, 005, 025]
affected_viewpoints: ["concurrency", "deployment"]
affected_perspectives: ["performance", "availability", "scalability"]
---

# ADR-022: Distributed Locking with Redis

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform requires distributed locking to:

- Prevent race conditions in distributed systems (multiple application instances)
- Ensure data consistency for critical operations (inventory updates, order processing)
- Coordinate access to shared resources across services
- Handle concurrent requests safely without database-level locking overhead
- Support timeout and automatic lock release for fault tolerance
- Provide high performance with minimal latency impact

### Business Context

**Business Drivers**:

- Prevent overselling inventory (critical business requirement)
- Ensure order processing integrity (no duplicate charges)
- Support horizontal scaling (multiple application instances)
- Maintain data consistency in distributed environment
- Handle high concurrency during promotions (1000+ concurrent users)

**Business Constraints**:

- Must prevent inventory overselling (zero tolerance)
- Order processing must be atomic and consistent
- Lock acquisition must be fast (< 10ms)
- System must handle lock holder failures gracefully

### Technical Context

**Current State**:

- Redis distributed caching (ADR-004)
- Kafka for event streaming (ADR-005)
- Saga pattern for distributed transactions (ADR-025)
- Multiple application instances (horizontal scaling)
- PostgreSQL with optimistic locking

**Requirements**:

- Distributed lock across multiple instances
- Lock timeout and automatic release
- Deadlock prevention
- High availability (99.9% uptime)
- Low latency (< 10ms lock acquisition)
- Fair lock acquisition (FIFO when possible)

## Decision Drivers

1. **Consistency**: Prevent race conditions and data corruption
2. **Performance**: Lock acquisition < 10ms, minimal overhead
3. **Availability**: 99.9% uptime with automatic failover
4. **Scalability**: Support 1000+ concurrent lock requests
5. **Fault Tolerance**: Automatic lock release on failure
6. **Integration**: Seamless Spring Boot integration
7. **Cost**: Leverage existing Redis infrastructure
8. **Operations**: Simple to monitor and troubleshoot

## Considered Options

### Option 1: Redis Distributed Locks (Redisson)

**Description**: Use Redisson library for Redis-based distributed locks with advanced features

**Pros**:

- ✅ Built on existing Redis infrastructure (ADR-004)
- ✅ Excellent Spring Boot integration
- ✅ Automatic lock renewal (watchdog)
- ✅ Fair locks (FIFO) support
- ✅ Read-write locks support
- ✅ Semaphore and CountDownLatch support
- ✅ Proven in production (Netflix, Alibaba)
- ✅ Low latency (< 5ms)
- ✅ Comprehensive documentation

**Cons**:

- ⚠️ Requires Redis cluster for high availability
- ⚠️ Network partition can cause issues
- ⚠️ Additional library dependency

**Cost**:

- Development: $0 (uses existing Redis)
- Production: $0 (included in Redis cost)
- Learning: 2-3 days

**Risk**: **Low** - Industry-standard solution

### Option 2: Database-Level Locking (PostgreSQL)

**Description**: Use PostgreSQL advisory locks or SELECT FOR UPDATE

**Pros**:

- ✅ Strong consistency guarantees
- ✅ No additional infrastructure
- ✅ ACID transaction support
- ✅ Simple to implement

**Cons**:

- ❌ Higher latency (10-50ms)
- ❌ Increases database load
- ❌ Doesn't scale well with high concurrency
- ❌ Lock contention affects database performance
- ❌ Limited to single database instance
- ❌ Deadlock risk with complex transactions

**Cost**: $0 (existing database)

**Risk**: **Medium** - Performance bottleneck

### Option 3: ZooKeeper Distributed Locks

**Description**: Use Apache ZooKeeper for distributed coordination

**Pros**:

- ✅ Strong consistency (CP system)
- ✅ Proven for distributed coordination
- ✅ Automatic lock release on client failure
- ✅ Watch mechanism for lock notifications

**Cons**:

- ❌ Additional infrastructure to manage
- ❌ Higher operational complexity
- ❌ Higher latency (20-50ms)
- ❌ Overkill for our use case
- ❌ Requires ZooKeeper cluster (3-5 nodes)
- ❌ Higher cost ($300-500/month)

**Cost**: $400/month (managed ZooKeeper)

**Risk**: **Medium** - Operational overhead

### Option 4: Optimistic Locking Only

**Description**: Rely solely on database optimistic locking (version fields)

**Pros**:

- ✅ Simple to implement
- ✅ No additional infrastructure
- ✅ Works well for low contention

**Cons**:

- ❌ High retry rate under contention
- ❌ Poor user experience (frequent failures)
- ❌ Doesn't prevent race conditions
- ❌ Inefficient for high concurrency
- ❌ No coordination across services

**Cost**: $0

**Risk**: **High** - Insufficient for high concurrency

## Decision Outcome

**Chosen Option**: **Redis Distributed Locks (Redisson)**

### Rationale

Redisson was selected for the following reasons:

1. **Leverages Existing Infrastructure**: Uses Redis already deployed for caching (ADR-004)
2. **Performance**: Sub-5ms lock acquisition meets our < 10ms requirement
3. **Rich Features**: Watchdog, fair locks, read-write locks, semaphores
4. **Proven**: Used by Netflix, Alibaba, and other large-scale systems
5. **Spring Integration**: Excellent Spring Boot support
6. **Cost-Effective**: No additional infrastructure cost
7. **Fault Tolerance**: Automatic lock release and renewal
8. **Scalability**: Handles 1000+ concurrent requests

**Lock Strategy**:

- **Inventory Updates**: Exclusive locks with 30-second timeout
- **Order Processing**: Fair locks (FIFO) with 60-second timeout
- **Payment Processing**: Exclusive locks with 30-second timeout
- **Cache Updates**: Read-write locks for read-heavy operations
- **Rate Limiting**: Semaphores for concurrent request limits

**Why Not Database Locking**: Higher latency and database load, doesn't scale well.

**Why Not ZooKeeper**: Overkill for our use case, higher cost and complexity.

**Why Not Optimistic Locking Only**: Insufficient for high concurrency scenarios.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Need to learn Redisson patterns | Training, code examples, documentation |
| Operations Team | Low | Monitor lock metrics | Add lock monitoring dashboards |
| End Users | Positive | Prevents overselling, better consistency | N/A |
| Business | Positive | Prevents revenue loss from overselling | N/A |
| Database Team | Positive | Reduced database lock contention | N/A |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:

- Inventory bounded context (critical)
- Order bounded context (critical)
- Payment bounded context (critical)
- Shopping cart bounded context (medium)
- Application services (lock annotations)
- Infrastructure layer (Redisson configuration)

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Lock not released | Low | High | Automatic timeout, watchdog |
| Redis unavailability | Low | High | Fallback to optimistic locking, circuit breaker |
| Deadlock | Low | Medium | Lock ordering, timeout |
| Lock contention | Medium | Medium | Fair locks, monitoring |
| Network partition | Low | High | Redis cluster, health checks |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Setup (Week 1)

- [x] Add Redisson dependency to project
- [x] Configure Redisson client with Redis cluster
- [x] Set up lock monitoring and metrics
- [x] Create lock utility classes
- [x] Document lock patterns

### Phase 2: Critical Operations (Week 2-3)

- [x] Implement inventory reservation locks
- [x] Add order processing locks
- [x] Implement payment processing locks
- [x] Add lock timeout handling
- [x] Implement lock retry logic

### Phase 3: Advanced Features (Week 4)

- [x] Implement fair locks for order queue
- [x] Add read-write locks for cache updates
- [x] Implement semaphores for rate limiting
- [x] Add lock metrics and monitoring
- [x] Performance testing under load

### Phase 4: Optimization (Week 5)

- [x] Tune lock timeouts based on metrics
- [x] Optimize lock granularity
- [x] Add lock contention monitoring
- [x] Document best practices
- [x] Team training

### Rollback Strategy

**Trigger Conditions**:

- Lock acquisition failure rate > 5%
- Lock timeout rate > 10%
- Redis unavailability > 1%
- Performance degradation > 20%

**Rollback Steps**:

1. Disable distributed locks
2. Fall back to optimistic locking
3. Reduce concurrent request limits
4. Investigate and fix issues
5. Re-enable gradually

**Rollback Time**: < 30 minutes

## Monitoring and Success Criteria

### Success Metrics

- ✅ Lock acquisition time < 10ms (95th percentile)
- ✅ Lock acquisition success rate > 99%
- ✅ Zero inventory overselling incidents
- ✅ Lock timeout rate < 1%
- ✅ Lock contention < 5% of requests
- ✅ System availability > 99.9%

### Monitoring Plan

**Application Metrics**:

```java
@Component
public class LockMetrics {
    private final Timer lockAcquisitionTime;
    private final Counter lockAcquisitions;
    private final Counter lockFailures;
    private final Counter lockTimeouts;
    private final Gauge activeLocks;
    
    // Track lock performance
}
```

**CloudWatch Metrics**:

- Lock acquisition time (p50, p95, p99)
- Lock acquisition success rate
- Lock timeout rate
- Active locks count
- Lock contention rate
- Lock wait time

**Alerts**:

- Lock acquisition time > 50ms
- Lock failure rate > 5%
- Lock timeout rate > 10%
- Active locks > 1000
- Lock contention > 10%

**Review Schedule**:

- Daily: Check lock metrics
- Weekly: Review lock patterns and timeouts
- Monthly: Lock performance optimization
- Quarterly: Lock strategy review

## Consequences

### Positive Consequences

- ✅ **Data Consistency**: Prevents race conditions and overselling
- ✅ **Performance**: Sub-10ms lock acquisition
- ✅ **Scalability**: Supports horizontal scaling
- ✅ **Reliability**: Automatic lock release on failure
- ✅ **Cost-Effective**: Uses existing Redis infrastructure
- ✅ **Flexibility**: Multiple lock types (exclusive, fair, read-write)
- ✅ **Monitoring**: Comprehensive lock metrics

### Negative Consequences

- ⚠️ **Complexity**: Additional coordination layer
- ⚠️ **Dependency**: Relies on Redis availability
- ⚠️ **Network**: Network latency affects lock performance
- ⚠️ **Debugging**: Harder to trace lock-related issues
- ⚠️ **Learning Curve**: Team needs to learn Redisson patterns

### Technical Debt

**Identified Debt**:

1. Manual lock timeout tuning (can be automated)
2. No automatic deadlock detection (future enhancement)
3. Simple lock ordering (can add priority locks)

**Debt Repayment Plan**:

- **Q2 2026**: Implement adaptive lock timeouts
- **Q3 2026**: Add deadlock detection and resolution
- **Q4 2026**: Implement priority-based lock queues

## Related Decisions

- [ADR-004: Use Redis for Distributed Caching](004-use-redis-for-distributed-caching.md) - Redisson uses same Redis infrastructure
- [ADR-005: Use Apache Kafka for Event Streaming](005-use-kafka-for-event-streaming.md) - Events after lock release
- [ADR-025: Saga Pattern for Distributed Transactions](025-saga-pattern-distributed-transactions.md) - Locks coordinate saga steps

## Notes

### Redisson Configuration

```java
@Configuration
public class RedissonConfiguration {
    
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useClusterServers()
            .addNodeAddress("redis://redis-cluster:6379")
            .setPassword("your-password")
            .setConnectTimeout(10000)
            .setTimeout(3000)
            .setRetryAttempts(3)
            .setRetryInterval(1500);
        
        return Redisson.create(config);
    }
}
```

### Lock Usage Patterns

**Exclusive Lock**:

```java
@Service
public class InventoryService {
    
    @Autowired
    private RedissonClient redissonClient;
    
    public void reserveInventory(String productId, int quantity) {
        RLock lock = redissonClient.getLock("inventory:" + productId);
        
        try {
            // Wait up to 10 seconds, auto-unlock after 30 seconds
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                // Critical section: update inventory
                Inventory inventory = inventoryRepository.findById(productId);
                inventory.reserve(quantity);
                inventoryRepository.save(inventory);
            } else {
                throw new LockAcquisitionException("Failed to acquire lock");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockInterruptedException("Lock interrupted", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

**Fair Lock (FIFO)**:

```java
@Service
public class OrderService {
    
    public void processOrder(String orderId) {
        RLock fairLock = redissonClient.getFairLock("order:queue");
        
        try {
            if (fairLock.tryLock(30, 60, TimeUnit.SECONDS)) {
                // Process order in FIFO order
                processOrderInternal(orderId);
            }
        } finally {
            if (fairLock.isHeldByCurrentThread()) {
                fairLock.unlock();
            }
        }
    }
}
```

**Read-Write Lock**:

```java
@Service
public class ProductCatalogService {
    
    public Product getProduct(String productId) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock("product:" + productId);
        RLock readLock = rwLock.readLock();
        
        try {
            readLock.lock(10, TimeUnit.SECONDS);
            return productRepository.findById(productId);
        } finally {
            readLock.unlock();
        }
    }
    
    public void updateProduct(Product product) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock("product:" + product.getId());
        RLock writeLock = rwLock.writeLock();
        
        try {
            writeLock.lock(30, TimeUnit.SECONDS);
            productRepository.save(product);
            cacheService.invalidate("product:" + product.getId());
        } finally {
            writeLock.unlock();
        }
    }
}
```

**Semaphore (Rate Limiting)**:

```java
@Service
public class ApiRateLimiter {
    
    public boolean tryAcquire(String userId) {
        RSemaphore semaphore = redissonClient.getSemaphore("rate:limit:" + userId);
        semaphore.trySetPermits(100); // 100 requests per minute
        
        try {
            return semaphore.tryAcquire(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
```

### Lock Naming Convention

```text
Pattern: {context}:{operation}:{resource-id}

Examples:

- inventory:reserve:PROD-123
- order:process:ORD-456
- payment:charge:PAY-789
- cache:update:customer:CUST-001

```

### Lock Timeout Guidelines

| Operation | Timeout | Rationale |
|-----------|---------|-----------|
| Inventory Reserve | 30s | Database update + validation |
| Order Processing | 60s | Multiple service calls |
| Payment Processing | 30s | External API call |
| Cache Update | 10s | Fast in-memory operation |
| Batch Processing | 300s | Long-running operation |

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
