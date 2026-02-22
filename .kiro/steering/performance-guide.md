---
inclusion: always
last_updated: 2026-02-21
---

# Performance Guide

Complete guide for performance optimization, monitoring, and testing.

## Quick Reference

### Performance Targets
- API response: < 2s (p95)
- Database queries: < 100ms (p95)
- Unit tests: < 50ms
- Integration tests: < 500ms
- E2E tests: < 3s

### Common Commands
```bash
./gradlew test jacocoTestReport              # Coverage + performance
./gradlew generatePerformanceReport          # Detailed performance report
./gradlew :app:bootRun --args='--spring.profiles.active=local'
```

---

## Database Optimization

### Query Optimization

**Must Follow:**
- Use specific columns instead of SELECT *
- Use pagination for large result sets
- Use batch operations for bulk updates
- Use native queries for complex operations

**Example:**
```java
@Repository
public class OptimizedCustomerRepository {
    
    // ✅ Good: Use specific columns
    @Query("SELECT c.id, c.name, c.email FROM Customer c WHERE c.status = :status")
    List<CustomerSummary> findActiveCustomerSummaries(@Param("status") CustomerStatus status);
    
    // ✅ Good: Use pagination
    @Query("SELECT c FROM Customer c WHERE c.registrationDate >= :date ORDER BY c.registrationDate DESC")
    Page<Customer> findRecentCustomers(@Param("date") LocalDate date, Pageable pageable);
    
    // ✅ Good: Use batch operations
    @Modifying
    @Query("UPDATE Customer c SET c.lastLoginDate = :loginDate WHERE c.id IN :customerIds")
    int updateLastLoginDates(@Param("customerIds") List<String> customerIds, 
                           @Param("loginDate") LocalDateTime loginDate);
}
```

### Index Strategy

```sql
-- Primary indexes for frequent queries
CREATE INDEX idx_customer_email ON customers(email);
CREATE INDEX idx_customer_status_created ON customers(status, created_date);
CREATE INDEX idx_order_customer_date ON orders(customer_id, order_date);

-- Composite indexes for complex queries
CREATE INDEX idx_customer_segment_status ON customers(segment, status, created_date);

-- Partial indexes for specific conditions
CREATE INDEX idx_active_customers ON customers(created_date) WHERE status = 'ACTIVE';
```

---

## Caching Strategy

### Application-Level Caching

**Must Follow:**
- Cache frequently accessed data
- Use appropriate TTL
- Implement cache eviction
- Monitor cache hit rates

**Example:**
```java
@Service
@CacheConfig(cacheNames = "customers")
public class CustomerService {
    
    @Cacheable(key = "#customerId", unless = "#result == null")
    public Customer findById(String customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
    
    @Cacheable(key = "'active-customers'", condition = "#pageable.pageNumber < 10")
    public Page<Customer> findActiveCustomers(Pageable pageable) {
        return customerRepository.findByStatus(CustomerStatus.ACTIVE, pageable);
    }
    
    @CacheEvict(key = "#customer.id")
    public Customer updateCustomer(Customer customer) {
        return customerRepository.save(customer);
    }
    
    @CacheEvict(allEntries = true)
    public void clearCustomerCache() {
        // Cache will be cleared automatically
    }
}
```

### Redis Configuration

```java
@Configuration
@EnableCaching
public class CacheConfiguration {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withCacheConfiguration("customers", config.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration("products", config.entryTtl(Duration.ofMinutes(15)))
            .withCacheConfiguration("orders", config.entryTtl(Duration.ofMinutes(5)))
            .build();
    }
}
```

---

## Asynchronous Processing

### Async Method Implementation

**Must Follow:**
- Use `@Async` for long-running operations
- Configure thread pool appropriately
- Implement retry mechanism
- Handle exceptions properly

**Example:**
```java
@Service
public class AsyncCustomerService {
    
    @Async("taskExecutor")
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public CompletableFuture<Void> sendWelcomeEmail(String customerId) {
        try {
            Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
            
            emailService.sendWelcomeEmail(customer.getEmail(), customer.getName());
            
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Failed to send welcome email for customer: {}", customerId, e);
            throw e;
        }
    }
}

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
```

---

## Performance Monitoring

### Application Metrics

**Must Follow:**
- Monitor response times
- Track throughput
- Monitor resource usage
- Set up alerts

**Example:**
```java
@Component
public class PerformanceMonitor {
    
    private final MeterRegistry meterRegistry;
    private final Timer responseTimeTimer;
    private final Counter requestCounter;
    
    public PerformanceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.responseTimeTimer = Timer.builder("http.request.duration")
            .description("HTTP request duration")
            .register(meterRegistry);
        this.requestCounter = Counter.builder("http.requests.total")
            .description("Total HTTP requests")
            .register(meterRegistry);
    }
    
    @EventListener
    public void recordHttpRequest(HttpRequestEvent event) {
        responseTimeTimer.record(event.getDuration(), TimeUnit.MILLISECONDS);
        requestCounter.increment(
            Tags.of(
                "method", event.getMethod(),
                "status", String.valueOf(event.getStatus()),
                "endpoint", event.getEndpoint()
            )
        );
    }
}
```

### Database Performance Monitoring

```java
@Component
public class DatabasePerformanceMonitor {
    
    private final Timer queryTimer;
    private final Counter slowQueryCounter;
    
    public DatabasePerformanceMonitor(MeterRegistry meterRegistry) {
        this.queryTimer = Timer.builder("database.query.duration")
            .description("Database query duration")
            .register(meterRegistry);
        this.slowQueryCounter = Counter.builder("database.slow.queries")
            .description("Number of slow database queries")
            .register(meterRegistry);
    }
    
    @EventListener
    public void recordDatabaseQuery(DatabaseQueryEvent event) {
        Duration duration = event.getDuration();
        queryTimer.record(duration);
        
        if (duration.toMillis() > 100) { // Slow query threshold
            slowQueryCounter.increment(
                Tags.of(
                    "query_type", event.getQueryType(),
                    "table", event.getTableName()
                )
            );
        }
    }
}
```

---

## Connection Pool Optimization

### HikariCP Configuration

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
      validation-timeout: 5000
      leak-detection-threshold: 60000
      pool-name: CustomerServicePool
      connection-test-query: SELECT 1
```

### Connection Pool Monitoring

```java
@Component
public class ConnectionPoolMonitor {
    
    private final HikariDataSource dataSource;
    
    public ConnectionPoolMonitor(HikariDataSource dataSource, MeterRegistry meterRegistry) {
        this.dataSource = dataSource;
        
        Gauge.builder("hikari.connections.active")
            .register(meterRegistry, dataSource, ds -> ds.getHikariPoolMXBean().getActiveConnections());
        
        Gauge.builder("hikari.connections.idle")
            .register(meterRegistry, dataSource, ds -> ds.getHikariPoolMXBean().getIdleConnections());
        
        Gauge.builder("hikari.connections.total")
            .register(meterRegistry, dataSource, ds -> ds.getHikariPoolMXBean().getTotalConnections());
    }
}
```

---

## Performance Testing

### Load Testing

**Must Follow:**
- Test with realistic data volumes
- Test concurrent users
- Monitor resource usage
- Identify bottlenecks

**Example:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPerformanceExtension(maxExecutionTimeMs = 30000, maxMemoryIncreaseMB = 200)
class PerformanceTest {
    
    @Test
    void should_handle_concurrent_customer_requests() throws InterruptedException {
        int numberOfThreads = 50;
        int requestsPerThread = 10;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        long startTime = System.currentTimeMillis();
                        
                        ResponseEntity<CustomerResponse> response = restTemplate.getForEntity(
                            "http://localhost:" + port + "/api/v1/customers/test-customer-" + j,
                            CustomerResponse.class
                        );
                        
                        long responseTime = System.currentTimeMillis() - startTime;
                        responseTimes.add(responseTime);
                        
                        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                        assertThat(responseTime).isLessThan(1000);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        // Analyze results
        double averageResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        long p95ResponseTime = responseTimes.stream()
            .sorted()
            .skip((long) (responseTimes.size() * 0.95))
            .findFirst()
            .orElse(0L);
        
        assertThat(averageResponseTime).isLessThan(500);
        assertThat(p95ResponseTime).isLessThan(1000);
    }
}
```

---

## Performance Alerts

### Alert Configuration

```yaml
# Prometheus Alert Rules
groups:
  - name: performance-alerts
    rules:
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High response time detected"
          
      - alert: HighMemoryUsage
        expr: (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage detected"
          
      - alert: DatabaseConnectionPoolExhausted
        expr: hikari_connections_active / hikari_connections_max > 0.9
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool nearly exhausted"
```

---

## Related Guides

- **Development**: See `development-guide.md` for testing standards
- **Architecture**: See `architecture-guide.md` for design patterns
- **Security**: See `security-guide.md` for secure coding

---

**Last Updated**: 2026-02-21
**Owner**: Performance Team
