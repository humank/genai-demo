---
adr_number: 004
title: "Use Redis for Distributed Caching"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [001, 002]
affected_viewpoints: ["deployment", "concurrency"]
affected_perspectives: ["performance", "availability", "scalability"]
---

# ADR-004: Use Redis for Distributed Caching

## Status

**Accepted** - 2025-10-24

## Context

### Problem Statement

The Enterprise E-Commerce Platform needs a caching solution to:

- Reduce database load and improve response times
- Support distributed caching across multiple application instances
- Handle high read volumes (5000+ reads/second)
- Provide sub-millisecond response times for cached data
- Support cache invalidation and TTL management
- Enable session management for stateless applications
- Support rate limiting and distributed locking

### Business Context

**Business Drivers**:

- Need for sub-second API response times (< 500ms target)
- Expected traffic spikes during promotions (10x normal load)
- Cost optimization by reducing database queries
- Improved user experience with faster page loads
- Support for 10K → 1M users growth

**Constraints**:

- Budget: $500/month for caching infrastructure
- Must integrate with AWS infrastructure
- Need for high availability (99.9% uptime)
- Data consistency requirements vary by use case

### Technical Context

**Current State**:

- PostgreSQL primary database (ADR-001)
- Hexagonal Architecture (ADR-002)
- Spring Boot 3.4.5 with Spring Cache abstraction
- AWS cloud infrastructure

**Requirements**:

- Distributed caching across multiple instances
- Sub-millisecond read latency
- Support for complex data structures (lists, sets, sorted sets)
- Pub/sub capabilities for cache invalidation
- Persistence options for critical data
- Cluster mode for high availability

## Decision Drivers

1. **Performance**: Sub-millisecond response times
2. **Scalability**: Handle 5000+ reads/second
3. **Availability**: 99.9% uptime with automatic failover
4. **Cost**: Within $500/month budget
5. **Spring Integration**: Seamless Spring Boot integration
6. **Data Structures**: Support for complex data types
7. **Persistence**: Optional persistence for critical data
8. **Operations**: Managed service to reduce operational overhead

## Considered Options

### Option 1: Redis (AWS ElastiCache)

**Description**: In-memory data store with optional persistence, managed by AWS ElastiCache

**Pros**:

- ✅ Sub-millisecond latency (< 1ms)
- ✅ Rich data structures (strings, lists, sets, sorted sets, hashes)
- ✅ Excellent Spring Boot integration (Spring Data Redis)
- ✅ Pub/sub for cache invalidation
- ✅ Cluster mode for high availability
- ✅ Optional persistence (RDB, AOF)
- ✅ AWS ElastiCache managed service
- ✅ Supports distributed locking
- ✅ Large community and ecosystem

**Cons**:

- ⚠️ In-memory only (requires sufficient RAM)
- ⚠️ Persistence has performance trade-offs
- ⚠️ Cluster mode adds complexity

**Cost**:

- Development: $200/month (cache.t3.medium)
- Production: $500/month (cache.r6g.large with replica)

**Risk**: **Low** - Industry-standard solution

### Option 2: Memcached (AWS ElastiCache)

**Description**: Simple in-memory key-value store

**Pros**:

- ✅ Very fast (sub-millisecond)
- ✅ Simple to use
- ✅ Multi-threaded architecture
- ✅ AWS ElastiCache managed service
- ✅ Lower memory overhead

**Cons**:

- ❌ Only supports simple key-value pairs
- ❌ No persistence
- ❌ No pub/sub
- ❌ No complex data structures
- ❌ Limited Spring Boot integration
- ❌ No distributed locking

**Cost**: Similar to Redis

**Risk**: **Low** - But limited features

### Option 3: Hazelcast

**Description**: In-memory data grid with distributed caching

**Pros**:

- ✅ Distributed caching built-in
- ✅ Rich data structures
- ✅ Strong consistency options
- ✅ Good Spring Boot integration

**Cons**:

- ❌ Higher memory usage
- ❌ More complex setup
- ❌ No AWS managed service
- ❌ Smaller community than Redis
- ❌ Higher operational overhead

**Cost**: $800/month (self-managed on EC2)

**Risk**: **Medium** - More operational complexity

### Option 4: Application-Level Caching (Caffeine)

**Description**: In-process caching with Caffeine library

**Pros**:

- ✅ Zero network latency
- ✅ Simple to implement
- ✅ No additional infrastructure
- ✅ Excellent Spring Boot integration

**Cons**:

- ❌ Not distributed (each instance has own cache)
- ❌ Cache invalidation across instances is complex
- ❌ Limited by JVM heap size
- ❌ No session sharing
- ❌ No distributed locking

**Cost**: $0 (included in application)

**Risk**: **Medium** - Doesn't scale well

## Decision Outcome

**Chosen Option**: **Redis (AWS ElastiCache)**

### Rationale

Redis was selected for the following reasons:

1. **Performance**: Sub-millisecond latency meets our < 500ms API response target
2. **Rich Features**: Complex data structures support various caching patterns
3. **Spring Integration**: Excellent Spring Data Redis and Spring Cache support
4. **Distributed**: Works across multiple application instances
5. **Pub/Sub**: Enables cache invalidation across instances
6. **Managed Service**: AWS ElastiCache reduces operational overhead
7. **High Availability**: Cluster mode with automatic failover
8. **Cost-Effective**: Meets requirements within budget
9. **Proven**: Industry-standard solution with large community

**Caching Strategy**:

- **Read-Through**: Cache customer profiles, product catalog
- **Write-Through**: Update cache on data changes
- **Cache-Aside**: For complex queries and aggregations
- **TTL-Based**: Automatic expiration for time-sensitive data
- **Pub/Sub**: Cache invalidation across instances

**Why Not Memcached**: Lacks complex data structures, pub/sub, and persistence needed for our use cases.

**Why Not Hazelcast**: Higher operational overhead and cost without significant benefits over Redis.

**Why Not Caffeine**: Doesn't support distributed caching needed for multiple instances.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Need to learn Redis patterns | Training, examples, documentation |
| Operations Team | Low | Managed service reduces overhead | ElastiCache runbooks |
| End Users | Positive | Faster response times | N/A |
| Business | Positive | Cost savings, better UX | N/A |
| Database Team | Positive | Reduced database load | Monitor cache hit rates |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:

- All bounded contexts (caching layer)
- Application services (cache annotations)
- Infrastructure layer (Redis configuration)
- Performance characteristics
- Monitoring and alerting

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Cache stampede | Medium | High | Use locking, staggered TTLs |
| Stale data | Medium | Medium | Proper cache invalidation, short TTLs |
| Memory exhaustion | Low | High | Eviction policies, monitoring |
| Cache unavailability | Low | Medium | Fallback to database, circuit breaker |
| Cost overrun | Low | Low | Monitor usage, set alarms |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Setup (Week 1)

- [x] Provision ElastiCache Redis cluster (cache.r6g.large)
- [x] Configure security groups and VPC
- [x] Set up Redis cluster mode with replica
- [x] Configure CloudWatch monitoring
- [x] Set up backup schedule

### Phase 2: Integration (Week 2-3)

- [x] Add Spring Data Redis dependencies
- [x] Configure Redis connection (Lettuce client)
- [x] Implement cache configuration
- [x] Add cache annotations to services
- [x] Implement cache key strategies

### Phase 3: Caching Patterns (Week 4-5)

- [x] Implement read-through caching for customer profiles
- [x] Implement cache-aside for product catalog
- [x] Add cache invalidation on updates
- [x] Implement distributed locking for critical sections
- [x] Add session management with Redis

### Phase 4: Optimization (Week 6)

- [x] Tune cache TTLs based on usage patterns
- [x] Implement cache warming for critical data
- [x] Add cache metrics and monitoring
- [x] Load testing and performance tuning
- [x] Document caching patterns

### Rollback Strategy

**Trigger Conditions**:

- Cache hit rate < 50%
- Cache-related errors > 1%
- Cost exceeds budget by > 50%
- Performance degradation with cache

**Rollback Steps**:

1. Disable caching annotations
2. Route all requests to database
3. Investigate root cause
4. Fix issues and re-enable gradually

**Rollback Time**: < 1 hour

## Monitoring and Success Criteria

### Success Metrics

- ✅ Cache hit rate > 80%
- ✅ Cache response time < 1ms (95th percentile)
- ✅ Database query reduction > 60%
- ✅ API response time improvement > 40%
- ✅ Cache availability > 99.9%
- ✅ Cost within budget ($500/month)

### Monitoring Plan

**CloudWatch Metrics**:

- CacheHits / CacheMisses
- CPUUtilization
- NetworkBytesIn / NetworkBytesOut
- CurrConnections
- Evictions
- ReplicationLag

**Application Metrics**:

```java
@Component
public class CacheMetrics {
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Timer cacheOperationTime;
    
    // Track cache performance
}
```

**Alerts**:

- Cache hit rate < 70%
- CPU utilization > 80%
- Evictions > 100/minute
- Replication lag > 5 seconds
- Connection count > 80% of max

**Review Schedule**:

- Daily: Check cache hit rates
- Weekly: Review cache patterns and TTLs
- Monthly: Cost optimization review
- Quarterly: Caching strategy review

## Consequences

### Positive Consequences

- ✅ **Performance**: 40-60% reduction in API response times
- ✅ **Scalability**: Handles 10x traffic with same database capacity
- ✅ **Cost Savings**: Reduced database instance size needs
- ✅ **User Experience**: Faster page loads and API responses
- ✅ **Database Health**: Reduced load on PostgreSQL
- ✅ **Flexibility**: Rich data structures support various patterns
- ✅ **Reliability**: High availability with automatic failover

### Negative Consequences

- ⚠️ **Complexity**: Additional layer to manage
- ⚠️ **Consistency**: Potential for stale data
- ⚠️ **Cost**: Additional infrastructure cost ($500/month)
- ⚠️ **Debugging**: Harder to trace cache-related issues
- ⚠️ **Memory Management**: Need to monitor and tune

### Technical Debt

**Identified Debt**:

1. Manual cache invalidation (can be improved with CDC)
2. No cache warming on deployment (future enhancement)
3. Simple TTL-based expiration (can add smarter policies)

**Debt Repayment Plan**:

- **Q2 2026**: Implement CDC-based cache invalidation
- **Q3 2026**: Add cache warming on deployment
- **Q4 2026**: Implement adaptive TTL based on access patterns

## Related Decisions

- [ADR-001: Use PostgreSQL for Primary Database](001-use-postgresql-for-primary-database.md) - Cache reduces database load
- [ADR-002: Adopt Hexagonal Architecture](002-adopt-hexagonal-architecture.md) - Cache in infrastructure layer
- [ADR-005: Use Apache Kafka for Event Streaming](005-use-kafka-for-event-streaming.md) - Event-based cache invalidation

## Notes

### Cache Configuration

```java
@Configuration
@EnableCaching
public class CacheConfiguration {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withCacheConfiguration("customers", 
                config.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration("products", 
                config.entryTtl(Duration.ofMinutes(15)))
            .withCacheConfiguration("orders", 
                config.entryTtl(Duration.ofMinutes(5)))
            .build();
    }
}
```

### Caching Patterns

**Read-Through Caching**:

```java
@Service
@CacheConfig(cacheNames = "customers")
public class CustomerService {
    
    @Cacheable(key = "#customerId")
    public Customer findById(String customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
    
    @CacheEvict(key = "#customer.id")
    public Customer updateCustomer(Customer customer) {
        return customerRepository.save(customer);
    }
}
```

**Distributed Locking**:

```java
@Service
public class OrderService {
    
    @Autowired
    private RedissonClient redissonClient;
    
    public void processOrder(String orderId) {
        RLock lock = redissonClient.getLock("order:" + orderId);
        
        try {
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                // Process order with exclusive lock
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### Cache Key Strategy

```text
Pattern: {context}:{entity}:{id}:{version}

Examples:

- customer:profile:CUST-123:v1
- product:details:PROD-456:v2
- order:summary:ORD-789:v1

```

### ElastiCache Configuration

```yaml
# Production Configuration
Node Type: cache.r6g.large
Engine: Redis 7.0
Cluster Mode: Enabled
Replicas: 1 per shard
Shards: 2
Multi-AZ: Enabled
Backup: Daily, 7-day retention
Maintenance Window: Sun 03:00-04:00 UTC
```

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-24  
**Next Review**: 2026-01-24 (Quarterly)
