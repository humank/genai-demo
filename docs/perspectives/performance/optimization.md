# Performance Optimization Guidelines

> **Last Updated**: 2025-10-23  
> **Status**: ✅ Active

## Overview

This document provides practical guidelines for optimizing performance across the e-commerce platform. It covers database optimization, caching strategies, code-level optimizations, and asynchronous processing patterns.

## Database Optimization

### Query Optimization

#### Use Specific Columns

❌ **Bad**: SELECT * queries
```java
@Query("SELECT * FROM customers WHERE status = :status")
List<Customer> findByStatus(@Param("status") String status);
```

✅ **Good**: Select only needed columns
```java
@Query("SELECT c.id, c.name, c.email FROM Customer c WHERE c.status = :status")
List<CustomerSummary> findByStatus(@Param("status") CustomerStatus status);
```

**Impact**: Reduces data transfer by 60-70%, improves query performance by 30-40%

#### Avoid N+1 Query Problem

❌ **Bad**: Lazy loading in loops
```java
List<Order> orders = orderRepository.findAll();
for (Order order : orders) {
    // This triggers N additional queries!
    List<OrderItem> items = order.getItems();
    processItems(items);
}
```

✅ **Good**: Use JOIN FETCH
```java
@Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.customerId = :customerId")
List<Order> findByCustomerIdWithItems(@Param("customerId") String customerId);
```

**Impact**: Reduces queries from N+1 to 1, improves performance by 10-100x

#### Implement Pagination

❌ **Bad**: Loading all results
```java
@Query("SELECT o FROM Order o WHERE o.customerId = :customerId")
List<Order> findByCustomerId(@Param("customerId") String customerId);
```

✅ **Good**: Use pagination
```java
@Query("SELECT o FROM Order o WHERE o.customerId = :customerId ORDER BY o.orderDate DESC")
Page<Order> findByCustomerId(@Param("customerId") String customerId, Pageable pageable);

// Usage
Page<Order> orders = orderRepository.findByCustomerId(customerId, 
    PageRequest.of(0, 20, Sort.by("orderDate").descending()));
```

**Impact**: Reduces memory usage by 90%+, improves response time by 80%+

#### Use Batch Operations

❌ **Bad**: Individual updates
```java
for (String customerId : customerIds) {
    Customer customer = customerRepository.findById(customerId).get();
    customer.setLastLoginDate(LocalDateTime.now());
    customerRepository.save(customer);
}
```

✅ **Good**: Batch update
```java
@Modifying
@Query("UPDATE Customer c SET c.lastLoginDate = :loginDate WHERE c.id IN :customerIds")
int updateLastLoginDates(@Param("customerIds") List<String> customerIds, 
                        @Param("loginDate") LocalDateTime loginDate);
```

**Impact**: Reduces database round-trips by 90%+, improves performance by 50-100x

### Index Strategy

#### Create Indexes for Frequent Queries

```sql
-- Single column indexes for exact matches
CREATE INDEX idx_customer_email ON customers(email);
CREATE INDEX idx_order_status ON orders(status);

-- Composite indexes for multi-column queries
CREATE INDEX idx_customer_status_created ON customers(status, created_date);
CREATE INDEX idx_order_customer_date ON orders(customer_id, order_date);

-- Partial indexes for specific conditions
CREATE INDEX idx_active_customers ON customers(created_date) 
WHERE status = 'ACTIVE';

-- Covering indexes (include all columns needed)
CREATE INDEX idx_order_summary ON orders(customer_id, order_date) 
INCLUDE (total_amount, status);
```

#### Index Selection Guidelines

**When to Create Index**:
- Columns used in WHERE clauses frequently
- Columns used in JOIN conditions
- Columns used in ORDER BY clauses
- Foreign key columns

**When NOT to Create Index**:
- Small tables (< 1000 rows)
- Columns with low cardinality (few distinct values)
- Columns that are frequently updated
- Tables with high write-to-read ratio

#### Monitor Index Usage

```sql
-- PostgreSQL: Check unused indexes
SELECT schemaname, tablename, indexname, idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
AND indexname NOT LIKE 'pg_toast%'
ORDER BY schemaname, tablename;

-- PostgreSQL: Check index size
SELECT schemaname, tablename, indexname,
       pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
ORDER BY pg_relation_size(indexrelid) DESC;
```

### Connection Pool Optimization

#### HikariCP Configuration

```yaml
spring:
  datasource:
    hikari:
      # Core pool settings
      minimum-idle: 5
      maximum-pool-size: 20
      
      # Connection lifecycle
      connection-timeout: 20000      # 20 seconds
      idle-timeout: 300000           # 5 minutes
      max-lifetime: 1200000          # 20 minutes
      
      # Leak detection
      leak-detection-threshold: 60000  # 60 seconds
      
      # Performance tuning
      connection-test-query: SELECT 1
      validation-timeout: 5000
      
      # Pool name for monitoring
      pool-name: OrderServicePool
```

#### Pool Sizing Formula

```
connections = ((core_count * 2) + effective_spindle_count)

Example for 4-core CPU with SSD:
connections = ((4 * 2) + 1) = 9

Add buffer for spikes: 9 * 1.5 = 13-15 connections
```

**Guidelines**:
- Start with formula-based size
- Monitor actual usage
- Adjust based on workload characteristics
- Leave headroom for spikes (20-30%)

### Query Performance Analysis

#### Use EXPLAIN ANALYZE

```sql
EXPLAIN ANALYZE
SELECT o.id, o.total_amount, c.name
FROM orders o
JOIN customers c ON o.customer_id = c.id
WHERE o.status = 'PENDING'
AND o.created_date >= '2025-01-01'
ORDER BY o.created_date DESC
LIMIT 20;
```

**Look for**:
- Sequential scans (should use indexes)
- High cost estimates
- Large row counts
- Nested loops on large tables

#### Enable Slow Query Log

```yaml
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        session:
          events:
            log:
              LOG_QUERIES_SLOWER_THAN_MS: 100
```

**Review Process**:
1. Collect slow queries weekly
2. Analyze query plans
3. Add missing indexes
4. Optimize query structure
5. Monitor improvements

## Caching Strategy

### Multi-Level Caching

#### Level 1: Application Cache (Caffeine)

```java
@Configuration
@EnableCaching
public class CacheConfiguration {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats());
        return cacheManager;
    }
}
```

**Use Cases**:
- Hot data accessed very frequently
- Small data sets (< 100MB)
- Low latency requirements (< 1ms)

**Benefits**:
- No network latency
- Very fast access (< 1ms)
- Automatic memory management

#### Level 2: Distributed Cache (Redis)

```java
@Configuration
@EnableCaching
public class RedisCacheConfiguration {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withCacheConfiguration("products", 
                config.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration("customers", 
                config.entryTtl(Duration.ofMinutes(15)))
            .build();
    }
}
```

**Use Cases**:
- Data shared across service instances
- Medium-sized data sets (< 1GB per key)
- Moderate latency requirements (< 10ms)

**Benefits**:
- Shared across instances
- Persistent across restarts
- Supports complex data structures

### Cache Patterns

#### Cache-Aside Pattern

```java
@Service
public class ProductService {
    
    @Cacheable(value = "products", key = "#productId", unless = "#result == null")
    public Product findById(String productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
    }
    
    @CacheEvict(value = "products", key = "#product.id")
    public Product updateProduct(Product product) {
        Product updated = productRepository.save(product);
        return updated;
    }
    
    @CacheEvict(value = "products", allEntries = true)
    public void clearProductCache() {
        // Cache cleared automatically
    }
}
```

#### Cache Warming

```java
@Component
public class CacheWarmer {
    
    @Scheduled(cron = "0 0 * * * *")  // Every hour
    public void warmProductCache() {
        List<Product> popularProducts = productRepository.findPopularProducts();
        
        for (Product product : popularProducts) {
            cacheManager.getCache("products").put(product.getId(), product);
        }
        
        logger.info("Warmed cache with {} popular products", popularProducts.size());
    }
}
```

### Cache Optimization

#### TTL Strategy

| Data Type | TTL | Rationale |
|-----------|-----|-----------|
| Product catalog | 1 hour | Changes infrequently |
| Customer profile | 15 minutes | May change during session |
| Shopping cart | 5 minutes | Changes frequently |
| Inventory | 1 minute | Real-time accuracy needed |
| Static content | 24 hours | Rarely changes |

#### Cache Key Design

✅ **Good**: Descriptive, hierarchical keys
```java
String cacheKey = String.format("product:%s:details", productId);
String cacheKey = String.format("customer:%s:orders:page:%d", customerId, pageNumber);
```

❌ **Bad**: Generic, flat keys
```java
String cacheKey = productId;  // Too generic
String cacheKey = "data";     // Not descriptive
```

#### Cache Monitoring

```java
@Component
public class CacheMetrics {
    
    @Scheduled(fixedRate = 60000)  // Every minute
    public void recordCacheMetrics() {
        Cache cache = cacheManager.getCache("products");
        CaffeineCache caffeineCache = (CaffeineCache) cache;
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = 
            caffeineCache.getNativeCache();
        
        CacheStats stats = nativeCache.stats();
        
        meterRegistry.gauge("cache.hit.rate", stats.hitRate());
        meterRegistry.gauge("cache.miss.rate", stats.missRate());
        meterRegistry.gauge("cache.eviction.count", stats.evictionCount());
        meterRegistry.gauge("cache.size", nativeCache.estimatedSize());
    }
}
```

## Asynchronous Processing

### Async Method Execution

```java
@Configuration
@EnableAsync
public class AsyncConfiguration {
    
    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}

@Service
public class NotificationService {
    
    @Async("taskExecutor")
    @Retryable(value = {Exception.class}, maxAttempts = 3, 
               backoff = @Backoff(delay = 1000))
    public CompletableFuture<Void> sendOrderConfirmation(String orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
            
            emailService.sendOrderConfirmation(order.getCustomerEmail(), order);
            
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Failed to send order confirmation: {}", orderId, e);
            throw e;
        }
    }
}
```

**Benefits**:
- Reduces API response time by 200-300ms
- Improves user experience
- Enables independent scaling

### Event-Driven Architecture

```java
@Service
@Transactional
public class OrderService {
    
    public Order createOrder(CreateOrderCommand command) {
        // Synchronous: Create order
        Order order = orderFactory.create(command);
        orderRepository.save(order);
        
        // Asynchronous: Publish events
        domainEventService.publishEventsFromAggregate(order);
        
        return order;
    }
}

@Component
public class OrderEventHandler {
    
    @KafkaListener(topics = "order-events", groupId = "notification-service")
    public void handleOrderCreated(OrderCreatedEvent event) {
        // Process asynchronously
        notificationService.sendOrderConfirmation(event.getOrderId());
        analyticsService.recordOrderCreated(event);
        loyaltyService.awardPoints(event.getCustomerId(), event.getTotalAmount());
    }
}
```

## Code-Level Optimization

### Lazy Initialization

```java
public class ExpensiveService {
    
    // Lazy initialization with double-checked locking
    private volatile ExpensiveResource resource;
    
    public ExpensiveResource getResource() {
        if (resource == null) {
            synchronized (this) {
                if (resource == null) {
                    resource = new ExpensiveResource();
                }
            }
        }
        return resource;
    }
}
```

### Object Pooling

```java
@Configuration
public class ObjectPoolConfiguration {
    
    @Bean
    public GenericObjectPool<ExpensiveObject> expensiveObjectPool() {
        GenericObjectPoolConfig<ExpensiveObject> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(20);
        config.setMaxIdle(10);
        config.setMinIdle(5);
        
        return new GenericObjectPool<>(new ExpensiveObjectFactory(), config);
    }
}
```

### String Optimization

❌ **Bad**: String concatenation in loops
```java
String result = "";
for (String item : items) {
    result += item + ",";  // Creates new String object each iteration
}
```

✅ **Good**: Use StringBuilder
```java
StringBuilder result = new StringBuilder();
for (String item : items) {
    result.append(item).append(",");
}
String finalResult = result.toString();
```

### Collection Optimization

```java
// Pre-size collections when size is known
List<Order> orders = new ArrayList<>(expectedSize);
Map<String, Product> productMap = new HashMap<>(expectedSize);

// Use appropriate collection types
Set<String> uniqueIds = new HashSet<>();  // Fast lookup
List<Order> orderedList = new ArrayList<>();  // Fast iteration
Map<String, Customer> customerMap = new HashMap<>();  // Fast key lookup
```

## Frontend Optimization

### Bundle Size Optimization

**Code Splitting**:
```typescript
// Lazy load routes
const ProductCatalog = lazy(() => import('./pages/ProductCatalog'));
const OrderHistory = lazy(() => import('./pages/OrderHistory'));

// Lazy load components
const HeavyComponent = lazy(() => import('./components/HeavyComponent'));
```

**Tree Shaking**:
```javascript
// Import only what you need
import { debounce } from 'lodash-es';  // ✅ Good
import _ from 'lodash';  // ❌ Bad (imports entire library)
```

### Image Optimization

```typescript
// Use next/image for automatic optimization
import Image from 'next/image';

<Image
  src="/product.jpg"
  alt="Product"
  width={500}
  height={500}
  loading="lazy"
  placeholder="blur"
/>
```

### Lazy Loading

```typescript
// Lazy load images
<img src="product.jpg" loading="lazy" alt="Product" />

// Intersection Observer for custom lazy loading
const observer = new IntersectionObserver((entries) => {
  entries.forEach(entry => {
    if (entry.isIntersecting) {
      loadComponent(entry.target);
    }
  });
});
```

## Monitoring and Profiling

### Application Profiling

```java
@Component
@Aspect
public class PerformanceMonitoringAspect {
    
    @Around("@annotation(Monitored)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            return joinPoint.proceed();
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (executionTime > 1000) {
                logger.warn("Slow method execution: {} took {}ms",
                    joinPoint.getSignature(), executionTime);
            }
            
            meterRegistry.timer("method.execution.time",
                "class", joinPoint.getTarget().getClass().getSimpleName(),
                "method", joinPoint.getSignature().getName())
                .record(executionTime, TimeUnit.MILLISECONDS);
        }
    }
}
```

### Database Query Profiling

```java
@Component
public class QueryPerformanceInterceptor extends EmptyInterceptor {
    
    @Override
    public String onPrepareStatement(String sql) {
        long startTime = System.currentTimeMillis();
        
        return sql;
    }
    
    @Override
    public void afterTransactionCompletion(Transaction tx) {
        long executionTime = System.currentTimeMillis() - startTime;
        
        if (executionTime > 100) {
            logger.warn("Slow query detected: {}ms", executionTime);
        }
    }
}
```

## Performance Checklist

### Development Phase

- [ ] Use specific columns in queries (avoid SELECT *)
- [ ] Implement pagination for large result sets
- [ ] Use JOIN FETCH to avoid N+1 queries
- [ ] Create indexes for frequently queried columns
- [ ] Implement caching for frequently accessed data
- [ ] Use async processing for non-critical operations
- [ ] Configure connection pools appropriately
- [ ] Implement proper error handling and timeouts

### Code Review Phase

- [ ] No N+1 query problems
- [ ] Proper use of caching
- [ ] Appropriate use of async processing
- [ ] No unbounded result sets
- [ ] Proper connection pool configuration
- [ ] Performance-critical paths optimized

### Testing Phase

- [ ] Load testing completed
- [ ] Performance benchmarks met
- [ ] No performance regressions
- [ ] Database query performance verified
- [ ] Cache hit rates acceptable
- [ ] Resource utilization within limits

### Production Phase

- [ ] Performance monitoring enabled
- [ ] Alerts configured
- [ ] Slow query logging enabled
- [ ] Cache metrics tracked
- [ ] Regular performance reviews scheduled

## Related Documentation

- [Performance Overview](overview.md) - High-level performance perspective
- [Performance Requirements](requirements.md) - Specific performance targets
- [Scalability Strategy](scalability.md) - Horizontal scaling approach
- [Verification](verification.md) - Testing and monitoring details

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-23  
**Owner**: Architecture Team
