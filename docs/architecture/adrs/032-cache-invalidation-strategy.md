---
adr_number: 032
title: "Cache Invalidation Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [003, 004, 005]
affected_viewpoints: ["information", "concurrency"]
affected_perspectives: ["performance", "consistency"]
---

# ADR-032: Cache Invalidation Strategy

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform uses Redis for distributed caching (ADR-004) and needs a cache invalidation strategy to:

- Maintain data consistency between cache and database
- Minimize stale data exposure to users
- Balance performance (cache hits) with freshness (data accuracy)
- Handle cache invalidation across multiple application instances
- Support different consistency requirements for different data types
- Prevent cache stampede during invalidation
- Enable efficient bulk invalidation for related data

### Business Context

**Business Drivers**:

- Product prices must be accurate (no stale pricing)
- Inventory levels must be reasonably current (< 1 minute stale acceptable)
- Customer profiles can tolerate some staleness (< 5 minutes acceptable)
- Order status must be accurate for customer inquiries
- Promotional pricing must be immediately effective

**Business Constraints**:

- Critical data (prices, inventory) must be fresh
- Non-critical data (product descriptions) can be stale
- Must support high cache hit rates (> 80%)
- Cache invalidation must not impact performance
- Must work across multiple application instances

### Technical Context

**Current State**:

- Redis distributed caching (ADR-004)
- Domain events for cross-context communication (ADR-003)
- Kafka for event streaming (ADR-005)
- Multiple application instances (horizontal scaling)
- PostgreSQL primary database

**Requirements**:

- Consistent cache invalidation across instances
- Support for different TTL strategies per data type
- Event-driven invalidation for critical data
- Bulk invalidation for related data
- Cache stampede prevention
- Low latency (< 5ms invalidation overhead)
- High availability (99.9% uptime)

## Decision Drivers

1. **Consistency**: Balance between performance and data freshness
2. **Performance**: Minimal impact on application performance
3. **Scalability**: Work across multiple instances
4. **Flexibility**: Different strategies for different data types
5. **Reliability**: Prevent cache stampede and thundering herd
6. **Simplicity**: Easy to implement and maintain
7. **Cost**: Leverage existing infrastructure
8. **Observability**: Monitor cache effectiveness

## Considered Options

### Option 1: TTL-Based Expiration Only

**Description**: Rely solely on Time-To-Live (TTL) for cache expiration

**Pros**:

- ✅ Simple to implement
- ✅ No additional infrastructure
- ✅ Automatic cleanup
- ✅ Low overhead

**Cons**:

- ❌ Stale data until TTL expires
- ❌ No immediate invalidation
- ❌ Inefficient for frequently updated data
- ❌ Cache stampede risk on expiration
- ❌ Doesn't meet consistency requirements

**Cost**: $0

**Risk**: **High** - Insufficient for requirements

### Option 2: Write-Through Cache

**Description**: Update cache synchronously on every write

**Pros**:

- ✅ Always consistent
- ✅ No stale data
- ✅ Simple logic

**Cons**:

- ❌ Increased write latency
- ❌ Cache write failures affect writes
- ❌ Doesn't scale well
- ❌ Tight coupling with cache

**Cost**: $0

**Risk**: **Medium** - Performance impact

### Option 3: Event-Driven Invalidation

**Description**: Use domain events to trigger cache invalidation

**Pros**:

- ✅ Near real-time consistency
- ✅ Decoupled from business logic
- ✅ Works across instances (pub/sub)
- ✅ Flexible (can invalidate related data)
- ✅ Leverages existing event infrastructure

**Cons**:

- ⚠️ Eventual consistency (small lag)
- ⚠️ Requires event infrastructure
- ⚠️ More complex implementation

**Cost**: $0 (uses existing Kafka/Redis)

**Risk**: **Low** - Proven pattern

### Option 4: Hybrid Approach (TTL + Event-Driven)

**Description**: Combine TTL for baseline expiration with event-driven invalidation for critical updates

**Pros**:

- ✅ Best of both worlds
- ✅ TTL as safety net
- ✅ Event-driven for immediate updates
- ✅ Flexible per data type
- ✅ Cache stampede prevention
- ✅ High cache hit rates

**Cons**:

- ⚠️ More complex implementation
- ⚠️ Requires careful configuration

**Cost**: $0 (uses existing infrastructure)

**Risk**: **Low** - Industry best practice

## Decision Outcome

**Chosen Option**: **Hybrid Approach (TTL + Event-Driven Invalidation)**

### Rationale

The hybrid approach was selected for the following reasons:

1. **Flexibility**: Different strategies for different data types
2. **Consistency**: Event-driven invalidation for critical data
3. **Safety Net**: TTL prevents indefinite stale data
4. **Performance**: High cache hit rates with timely updates
5. **Scalability**: Works across multiple instances
6. **Proven**: Industry best practice (Netflix, Amazon, Alibaba)
7. **Cost-Effective**: Uses existing infrastructure
8. **Reliability**: Cache stampede prevention built-in

**Cache Invalidation Strategy by Data Type**:

**Critical Data (Immediate Invalidation)**:

- **Product Prices**: Event-driven + 5-minute TTL
- **Inventory Levels**: Event-driven + 1-minute TTL
- **Order Status**: Event-driven + 5-minute TTL
- **Payment Status**: Event-driven + 5-minute TTL

**Semi-Critical Data (Near Real-Time)**:

- **Customer Profiles**: Event-driven + 30-minute TTL
- **Shopping Cart**: Event-driven + 15-minute TTL
- **Promotions**: Event-driven + 10-minute TTL

**Non-Critical Data (Lazy Invalidation)**:

- **Product Descriptions**: 1-hour TTL only
- **Product Images**: 24-hour TTL only
- **Static Content**: 24-hour TTL only

**Invalidation Patterns**:

1. **Single Key**: Invalidate specific cache entry
2. **Pattern-Based**: Invalidate all keys matching pattern
3. **Tag-Based**: Invalidate all entries with specific tag
4. **Bulk**: Invalidate multiple related entries

**Cache Stampede Prevention**:

- **Probabilistic Early Expiration**: Refresh before TTL expires
- **Lock-Based Refresh**: Only one instance refreshes
- **Stale-While-Revalidate**: Serve stale while refreshing

**Why Not TTL Only**: Doesn't meet consistency requirements for critical data.

**Why Not Write-Through**: Performance impact, tight coupling.

**Why Not Event-Driven Only**: No safety net for missed events.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Implement invalidation logic | Code examples, patterns, documentation |
| Operations Team | Low | Monitor cache metrics | Dashboards and alerts |
| End Users | Positive | More accurate data, better UX | N/A |
| Business | Positive | Accurate pricing, inventory | N/A |
| Database Team | Positive | Reduced database load | N/A |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:

- All bounded contexts (cache invalidation)
- Application services (invalidation logic)
- Infrastructure layer (Redis pub/sub)
- Event handlers (invalidation triggers)
- Monitoring and alerting

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Missed invalidation events | Low | Medium | TTL as safety net, monitoring |
| Cache stampede | Medium | High | Probabilistic expiration, locking |
| Over-invalidation | Medium | Low | Careful pattern design, monitoring |
| Event lag | Low | Low | Monitor event processing lag |
| Redis pub/sub failure | Low | Medium | Fallback to TTL, circuit breaker |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Core Implementation (Week 1-2)

- [x] Implement cache invalidation service
- [x] Add Redis pub/sub for invalidation
- [x] Create invalidation event handlers
- [x] Implement TTL configuration per data type
- [x] Add cache key tagging

### Phase 2: Event-Driven Invalidation (Week 3-4)

- [x] Implement product price invalidation
- [x] Add inventory level invalidation
- [x] Implement order status invalidation
- [x] Add customer profile invalidation
- [x] Implement shopping cart invalidation

### Phase 3: Advanced Features (Week 5)

- [x] Implement pattern-based invalidation
- [x] Add tag-based invalidation
- [x] Implement bulk invalidation
- [x] Add cache stampede prevention
- [x] Implement stale-while-revalidate

### Phase 4: Monitoring & Optimization (Week 6)

- [x] Add cache invalidation metrics
- [x] Create invalidation dashboards
- [x] Implement invalidation alerts
- [x] Performance testing
- [x] Documentation and training

### Rollback Strategy

**Trigger Conditions**:

- Cache hit rate drops > 20%
- Invalidation errors > 5%
- Performance degradation > 10%
- Data consistency issues

**Rollback Steps**:

1. Disable event-driven invalidation
2. Rely on TTL only
3. Investigate and fix issues
4. Re-enable gradually
5. Monitor metrics

**Rollback Time**: < 30 minutes

## Monitoring and Success Criteria

### Success Metrics

- ✅ Cache hit rate > 80%
- ✅ Cache invalidation latency < 5ms
- ✅ Stale data exposure < 1% of requests
- ✅ Cache stampede incidents = 0
- ✅ Invalidation success rate > 99.9%
- ✅ Event processing lag < 100ms

### Monitoring Plan

**Application Metrics**:

```java
@Component
public class CacheInvalidationMetrics {
    private final Counter invalidations;
    private final Counter invalidationErrors;
    private final Timer invalidationTime;
    private final Gauge cacheHitRate;
    private final Counter cacheStampede;
    
    // Track invalidation performance
}
```

**CloudWatch Metrics**:

- Cache invalidations per second
- Invalidation latency (p50, p95, p99)
- Cache hit rate by data type
- Stale data rate
- Cache stampede incidents
- Event processing lag

**Alerts**:

- Cache hit rate < 70%
- Invalidation latency > 10ms
- Invalidation error rate > 1%
- Cache stampede detected
- Event lag > 1 second

**Review Schedule**:

- Daily: Check cache metrics
- Weekly: Review invalidation patterns
- Monthly: Optimize TTL and strategies
- Quarterly: Cache strategy review

## Consequences

### Positive Consequences

- ✅ **Consistency**: Near real-time data freshness
- ✅ **Performance**: High cache hit rates (> 80%)
- ✅ **Flexibility**: Different strategies per data type
- ✅ **Reliability**: Cache stampede prevention
- ✅ **Scalability**: Works across multiple instances
- ✅ **Safety**: TTL as fallback for missed events
- ✅ **Observability**: Comprehensive metrics

### Negative Consequences

- ⚠️ **Complexity**: More complex than simple TTL
- ⚠️ **Eventual Consistency**: Small lag (< 100ms)
- ⚠️ **Monitoring**: Need to track invalidation metrics
- ⚠️ **Configuration**: Requires careful TTL tuning
- ⚠️ **Debugging**: Harder to trace invalidation issues

### Technical Debt

**Identified Debt**:

1. Manual TTL configuration (can be adaptive)
2. Simple pattern matching (can use advanced patterns)
3. No automatic invalidation optimization (future enhancement)

**Debt Repayment Plan**:

- **Q2 2026**: Implement adaptive TTL based on access patterns
- **Q3 2026**: Add ML-based invalidation optimization
- **Q4 2026**: Implement automatic pattern optimization

## Related Decisions


## Notes

### Cache Invalidation Service

```java
@Service
public class CacheInvalidationService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private RedissonClient redissonClient;
    
    // Single key invalidation
    public void invalidate(String key) {
        redisTemplate.delete(key);
        publishInvalidation(key);
    }
    
    // Pattern-based invalidation
    public void invalidatePattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            publishInvalidation(pattern);
        }
    }
    
    // Tag-based invalidation
    public void invalidateByTag(String tag) {
        Set<String> keys = getKeysByTag(tag);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            publishInvalidation("tag:" + tag);
        }
    }
    
    // Bulk invalidation
    public void invalidateBulk(Collection<String> keys) {
        redisTemplate.delete(keys);
        keys.forEach(this::publishInvalidation);
    }
    
    // Publish invalidation to other instances
    private void publishInvalidation(String key) {
        RTopic topic = redissonClient.getTopic("cache:invalidation");
        topic.publish(new InvalidationMessage(key, Instant.now()));
    }
    
    // Subscribe to invalidation messages
    @PostConstruct
    public void subscribeToInvalidations() {
        RTopic topic = redissonClient.getTopic("cache:invalidation");
        topic.addListener(InvalidationMessage.class, (channel, msg) -> {
            // Invalidate local cache if needed
            localCache.invalidate(msg.getKey());
        });
    }
}
```

### Event-Driven Invalidation

```java
@Component
public class ProductEventHandler {
    
    @Autowired
    private CacheInvalidationService cacheInvalidationService;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductPriceUpdated(ProductPriceUpdatedEvent event) {
        // Invalidate product cache
        cacheInvalidationService.invalidate("product:" + event.getProductId());
        
        // Invalidate related caches
        cacheInvalidationService.invalidatePattern("product:list:*");
        cacheInvalidationService.invalidateByTag("category:" + event.getCategoryId());
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInventoryUpdated(InventoryUpdatedEvent event) {
        // Invalidate inventory cache
        cacheInvalidationService.invalidate("inventory:" + event.getProductId());
        
        // Invalidate product cache (includes stock status)
        cacheInvalidationService.invalidate("product:" + event.getProductId());
    }
}
```

### Cache Stampede Prevention

```java
@Service
public class CacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private RedissonClient redissonClient;
    
    // Probabilistic early expiration
    public <T> T getWithProbabilisticRefresh(String key, 
                                             long ttl, 
                                             Supplier<T> loader) {
        T value = (T) redisTemplate.opsForValue().get(key);
        
        if (value != null) {
            // Calculate probability of early refresh
            long timeLeft = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            double refreshProbability = 1.0 - (double) timeLeft / ttl;
            
            // Probabilistically refresh before expiration
            if (Math.random() < refreshProbability) {
                refreshAsync(key, ttl, loader);
            }
            
            return value;
        }
        
        // Cache miss - load with lock
        return loadWithLock(key, ttl, loader);
    }
    
    // Lock-based refresh (prevent stampede)
    private <T> T loadWithLock(String key, long ttl, Supplier<T> loader) {
        RLock lock = redissonClient.getLock("lock:" + key);
        
        try {
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                // Double-check cache
                T value = (T) redisTemplate.opsForValue().get(key);
                if (value != null) {
                    return value;
                }
                
                // Load from source
                value = loader.get();
                
                // Cache with TTL
                redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
                
                return value;
            } else {
                // Failed to acquire lock, wait and retry
                Thread.sleep(100);
                return (T) redisTemplate.opsForValue().get(key);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CacheException("Lock interrupted", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    // Stale-while-revalidate
    public <T> T getWithStaleWhileRevalidate(String key, 
                                             long ttl, 
                                             Supplier<T> loader) {
        T value = (T) redisTemplate.opsForValue().get(key);
        
        if (value != null) {
            long timeLeft = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            
            // If close to expiration, refresh async
            if (timeLeft < ttl * 0.1) {
                refreshAsync(key, ttl, loader);
            }
            
            return value;
        }
        
        // Cache miss - load synchronously
        return loadWithLock(key, ttl, loader);
    }
    
    private <T> void refreshAsync(String key, long ttl, Supplier<T> loader) {
        CompletableFuture.runAsync(() -> {
            T value = loader.get();
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
        });
    }
}
```

### Cache Configuration

```yaml
# application.yml
cache:
  ttl:
    # Critical data (event-driven + short TTL)
    product-price: 300  # 5 minutes
    inventory: 60  # 1 minute
    order-status: 300  # 5 minutes
    payment-status: 300  # 5 minutes
    
    # Semi-critical data (event-driven + medium TTL)
    customer-profile: 1800  # 30 minutes
    shopping-cart: 900  # 15 minutes
    promotions: 600  # 10 minutes
    
    # Non-critical data (TTL only)
    product-description: 3600  # 1 hour
    product-images: 86400  # 24 hours
    static-content: 86400  # 24 hours
  
  stampede-prevention:
    enabled: true
    probabilistic-refresh: true
    stale-while-revalidate: true
```

### Cache Key Tagging

```java
@Service
public class CacheTagService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Add tag to cache key
    public void tagKey(String key, String... tags) {
        for (String tag : tags) {
            redisTemplate.opsForSet().add("tag:" + tag, key);
        }
    }
    
    // Get keys by tag
    public Set<String> getKeysByTag(String tag) {
        return redisTemplate.opsForSet().members("tag:" + tag);
    }
    
    // Remove tag
    public void removeTag(String key, String tag) {
        redisTemplate.opsForSet().remove("tag:" + tag, key);
    }
}

// Usage
cacheService.set("product:123", product, 3600);
cacheTagService.tagKey("product:123", "category:electronics", "brand:apple");

// Invalidate all products in category
cacheInvalidationService.invalidateByTag("category:electronics");
```

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
