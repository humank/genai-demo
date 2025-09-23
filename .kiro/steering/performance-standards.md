---
inclusion: always
---

# Performance Standards and Guidelines

## Performance Requirements

### Response Time Standards

#### API Response Time Targets

- **Critical APIs** (authentication, payment): ≤ 500ms (95th percentile)
- **Business APIs** (orders, customers): ≤ 1000ms (95th percentile)
- **Reporting APIs** (analytics, reports): ≤ 3000ms (95th percentile)
- **Batch APIs** (imports, exports): ≤ 30000ms (95th percentile)

#### Database Query Performance

- **Simple queries** (single table, indexed): ≤ 10ms (95th percentile)
- **Complex queries** (joins, aggregations): ≤ 100ms (95th percentile)
- **Reporting queries** (large datasets): ≤ 1000ms (95th percentile)

#### Frontend Performance

- **First Contentful Paint (FCP)**: ≤ 1.5s
- **Largest Contentful Paint (LCP)**: ≤ 2.5s
- **First Input Delay (FID)**: ≤ 100ms
- **Cumulative Layout Shift (CLS)**: ≤ 0.1

### Throughput Standards

#### API Throughput Targets

- **Peak load handling**: 1000 requests/second
- **Sustained load**: 500 requests/second
- **Database connections**: Max 20 per service instance
- **Memory usage**: ≤ 512MB per service instance

## Performance Monitoring Implementation

### Application Performance Monitoring

```java
@Component
public class PerformanceMonitor {
    
    private final MeterRegistry meterRegistry;
    private final Timer responseTimeTimer;
    private final Counter requestCounter;
    private final Gauge memoryUsage;
    
    public PerformanceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.responseTimeTimer = Timer.builder("http.request.duration")
            .description("HTTP request duration")
            .register(meterRegistry);
        this.requestCounter = Counter.builder("http.requests.total")
            .description("Total HTTP requests")
            .register(meterRegistry);
        this.memoryUsage = Gauge.builder("jvm.memory.used")
            .description("JVM memory usage")
            .register(meterRegistry, this, PerformanceMonitor::getMemoryUsage);
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
    
    private double getMemoryUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        return memoryBean.getHeapMemoryUsage().getUsed();
    }
}
```

### Database Performance Monitoring

```java
@Component
public class DatabasePerformanceMonitor {
    
    private final Timer queryTimer;
    private final Counter slowQueryCounter;
    private final Gauge connectionPoolUsage;
    
    public DatabasePerformanceMonitor(MeterRegistry meterRegistry, HikariDataSource dataSource) {
        this.queryTimer = Timer.builder("database.query.duration")
            .description("Database query duration")
            .register(meterRegistry);
        this.slowQueryCounter = Counter.builder("database.slow.queries")
            .description("Number of slow database queries")
            .register(meterRegistry);
        this.connectionPoolUsage = Gauge.builder("database.connection.pool.usage")
            .description("Database connection pool usage")
            .register(meterRegistry, dataSource, ds -> (double) ds.getHikariPoolMXBean().getActiveConnections() / ds.getMaximumPoolSize());
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

## Performance Optimization Strategies

### Database Optimization

#### Query Optimization Guidelines

```java
@Repository
public class OptimizedCustomerRepository {
    
    // ✅ Good: Use specific columns instead of SELECT *
    @Query("SELECT c.id, c.name, c.email FROM Customer c WHERE c.status = :status")
    List<CustomerSummary> findActiveCustomerSummaries(@Param("status") CustomerStatus status);
    
    // ✅ Good: Use pagination for large result sets
    @Query("SELECT c FROM Customer c WHERE c.registrationDate >= :date ORDER BY c.registrationDate DESC")
    Page<Customer> findRecentCustomers(@Param("date") LocalDate date, Pageable pageable);
    
    // ✅ Good: Use batch operations for bulk updates
    @Modifying
    @Query("UPDATE Customer c SET c.lastLoginDate = :loginDate WHERE c.id IN :customerIds")
    int updateLastLoginDates(@Param("customerIds") List<String> customerIds, 
                           @Param("loginDate") LocalDateTime loginDate);
    
    // ✅ Good: Use native queries for complex operations
    @Query(value = """
        SELECT c.segment, COUNT(*) as customer_count, AVG(o.total_amount) as avg_order_value
        FROM customers c 
        LEFT JOIN orders o ON c.id = o.customer_id 
        WHERE c.created_date >= :startDate 
        GROUP BY c.segment
        """, nativeQuery = true)
    List<CustomerSegmentStats> getCustomerSegmentStatistics(@Param("startDate") LocalDate startDate);
}
```

#### Index Strategy

```sql
-- Primary indexes for frequent queries
CREATE INDEX idx_customer_email ON customers(email);
CREATE INDEX idx_customer_status_created ON customers(status, created_date);
CREATE INDEX idx_order_customer_date ON orders(customer_id, order_date);

-- Composite indexes for complex queries
CREATE INDEX idx_customer_segment_status ON customers(segment, status, created_date);
CREATE INDEX idx_order_status_amount ON orders(status, total_amount, order_date);

-- Partial indexes for specific conditions
CREATE INDEX idx_active_customers ON customers(created_date) WHERE status = 'ACTIVE';
CREATE INDEX idx_pending_orders ON orders(order_date) WHERE status = 'PENDING';
```

### Caching Strategy

#### Application-Level Caching

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

#### Redis Configuration for Distributed Caching

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

### Asynchronous Processing

#### Async Method Implementation

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
    
    @Async("taskExecutor")
    public CompletableFuture<CustomerAnalytics> generateCustomerAnalytics(String customerId) {
        return CompletableFuture.supplyAsync(() -> {
            // Heavy computation
            return analyticsService.generateAnalytics(customerId);
        });
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

## Performance Testing Standards

### Load Testing Implementation

#### JMeter Test Plan Structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan testname="Customer API Load Test">
      <elementProp name="TestPlan.arguments" elementType="Arguments" guiclass="ArgumentsPanel">
        <collectionProp name="Arguments.arguments">
          <elementProp name="baseUrl" elementType="Argument">
            <stringProp name="Argument.name">baseUrl</stringProp>
            <stringProp name="Argument.value">${__P(baseUrl,http://localhost:8080)}</stringProp>
          </elementProp>
          <elementProp name="users" elementType="Argument">
            <stringProp name="Argument.name">users</stringProp>
            <stringProp name="Argument.value">${__P(users,100)}</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>
    
    <ThreadGroup testname="Customer Operations">
      <stringProp name="ThreadGroup.num_threads">${users}</stringProp>
      <stringProp name="ThreadGroup.ramp_time">60</stringProp>
      <stringProp name="ThreadGroup.duration">300</stringProp>
      
      <HTTPSamplerProxy testname="Get Customer">
        <stringProp name="HTTPSampler.domain">${baseUrl}</stringProp>
        <stringProp name="HTTPSampler.path">/../api/v1/customers/${customerId}</stringProp>
        <stringProp name="HTTPSampler.method">GET</stringProp>
      </HTTPSamplerProxy>
      
      <ResponseAssertion testname="Response Time Assertion">
        <stringProp name="Assertion.test_field">Assertion.response_time</stringProp>
        <stringProp name="Assertion.test_type">Assertion.duration</stringProp>
        <stringProp name="Assertion.test_string">1000</stringProp>
      </ResponseAssertion>
    </ThreadGroup>
  </hashTree>
</jmeterTestPlan>
```

#### Automated Performance Tests

```java
// Modern performance testing with monitoring
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPerformanceExtension(maxExecutionTimeMs = 30000, maxMemoryIncreaseMB = 200)
@TestMethodOrder(OrderAnnotation.class)
class PerformanceTest extends BaseIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @LocalServerPort
    private int port;
    
    @Test
    @Order(1)
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
                            "http://localhost:" + port + "/../api/v1/customers/test-customer-" + j,
                            CustomerResponse.class
                        );
                        
                        long responseTime = System.currentTimeMillis() - startTime;
                        responseTimes.add(responseTime);
                        
                        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                        assertThat(responseTime).isLessThan(1000); // 1 second max
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
        
        logger.info("Performance test results:");
        logger.info("Total requests: {}", responseTimes.size());
        logger.info("Average response time: {} ms", averageResponseTime);
        logger.info("95th percentile response time: {} ms", p95ResponseTime);
        
        // Check resource usage after test
        if (!isMemoryUsageAcceptable()) {
            logger.warn("Memory usage exceeded acceptable limits");
            forceResourceCleanup();
        }
    }
}
```

### Memory and Resource Testing

#### Memory Leak Detection

```java
@Component
public class MemoryMonitor {
    
    private final MeterRegistry meterRegistry;
    private final MemoryMXBean memoryBean;
    
    public MemoryMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        
        Gauge.builder("jvm.memory.heap.used")
            .register(meterRegistry, this, monitor -> monitor.memoryBean.getHeapMemoryUsage().getUsed());
        
        Gauge.builder("jvm.memory.heap.max")
            .register(meterRegistry, this, monitor -> monitor.memoryBean.getHeapMemoryUsage().getMax());
        
        Gauge.builder("jvm.memory.non.heap.used")
            .register(meterRegistry, this, monitor -> monitor.memoryBean.getNonHeapMemoryUsage().getUsed());
    }
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void checkMemoryUsage() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        double usagePercentage = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;
        
        if (usagePercentage > 80) {
            logger.warn("High memory usage detected: {}%", usagePercentage);
            
            // Trigger garbage collection if usage is very high
            if (usagePercentage > 90) {
                System.gc();
                logger.info("Garbage collection triggered due to high memory usage");
            }
        }
    }
}
```

## Performance Optimization Patterns

### Connection Pool Optimization

#### HikariCP Configuration

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

#### Connection Pool Monitoring

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
        
        Gauge.builder("hikari.connections.pending")
            .register(meterRegistry, dataSource, ds -> ds.getHikariPoolMXBean().getThreadsAwaitingConnection());
        
        Gauge.builder("hikari.connections.total")
            .register(meterRegistry, dataSource, ds -> ds.getHikariPoolMXBean().getTotalConnections());
    }
    
    @EventListener
    public void handleConnectionPoolEvent(ConnectionPoolEvent event) {
        if (event.getType() == ConnectionPoolEvent.Type.CONNECTION_LEAK_DETECTED) {
            logger.error("Connection leak detected: {}", event.getDetails());
            // Send alert to monitoring system
        }
    }
}
```

### Lazy Loading and Pagination

#### JPA Lazy Loading Best Practices

```java
@Entity
public class Customer {
    
    @Id
    private String id;
    
    private String name;
    private String email;
    
    // Lazy load expensive relationships
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();
    
    @OneToOne(mappedBy = "customer", fetch = FetchType.LAZY)
    private CustomerProfile profile;
    
    // Use @BatchSize to optimize N+1 queries
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    @BatchSize(size = 25)
    private List<Address> addresses = new ArrayList<>();
}

@Repository
public class CustomerRepository {
    
    // Use JOIN FETCH for eager loading when needed
    @Query("SELECT c FROM Customer c JOIN FETCH c.profile WHERE c.id = :id")
    Optional<Customer> findByIdWithProfile(@Param("id") String id);
    
    // Use pagination for large result sets
    @Query("SELECT c FROM Customer c WHERE c.registrationDate >= :date ORDER BY c.registrationDate DESC")
    Page<Customer> findRecentCustomers(@Param("date") LocalDate date, Pageable pageable);
}
```

## Performance Alerts and Monitoring

### Alert Configuration

```yaml
# Prometheus Alert Rules for Performance
groups:
  - name: performance-alerts
    rules:
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 1
        for: 2m
        labels:
          severity: warning
          team: backend
        annotations:
          summary: "High response time detected"
          description: "95th percentile response time is {{ $value }} seconds"
          
      - alert: HighMemoryUsage
        expr: (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.8
        for: 5m
        labels:
          severity: warning
          team: backend
        annotations:
          summary: "High memory usage detected"
          description: "Memory usage is {{ $value }}%"
          
      - alert: DatabaseConnectionPoolExhausted
        expr: hikari_connections_active / hikari_connections_max > 0.9
        for: 1m
        labels:
          severity: critical
          team: backend
        annotations:
          summary: "Database connection pool nearly exhausted"
          description: "Connection pool usage is {{ $value }}%"
          
      - alert: SlowDatabaseQueries
        expr: rate(database_slow_queries_total[5m]) > 0.1
        for: 2m
        labels:
          severity: warning
          team: backend
        annotations:
          summary: "High rate of slow database queries"
          description: "{{ $value }} slow queries per second"
```

### Performance Dashboard Requirements

#### Technical Performance Dashboard

- **Response Time Trends**: 95th percentile over time by endpoint
- **Throughput**: Requests per second by endpoint
- **Error Rates**: 4xx and 5xx errors by endpoint
- **Database Performance**: Query response times, connection pool usage
- **JVM Metrics**: Memory usage, garbage collection, thread count
- **Cache Performance**: Hit rates, eviction rates

#### Business Performance Dashboard

- **User Experience**: Page load times, conversion rates
- **Transaction Performance**: Order processing times, payment success rates
- **System Capacity**: Current load vs. capacity limits
- **Resource Utilization**: CPU, memory, disk usage trends

## Test Performance Integration

> **🧪 測試效能**: 詳細的測試效能監控標準請參考 [Development Standards - Test Performance Framework](development-standards.md#advanced-test-performance-framework)

**快速參考**:
- 使用 `@TestPerformanceExtension` 進行自動效能監控
- 整合測試: < 500ms, < 50MB
- 端到端測試: < 3s, < 500MB
- 效能報告生成: `./gradlew generatePerformanceReport`

## Performance Testing Checklist

### Pre-Production Performance Testing

- [ ] Load testing completed with expected traffic patterns
- [ ] Stress testing performed to identify breaking points
- [ ] Endurance testing conducted for memory leaks
- [ ] Database performance tested with production-like data volumes
- [ ] Cache performance validated
- [ ] Connection pool sizing optimized
- [ ] Test performance monitoring enabled with @TestPerformanceExtension
- [ ] Performance reports generated and reviewed
- [ ] Memory usage thresholds validated
- [ ] Test execution time benchmarks established

### Production Performance Monitoring

- [ ] Response time monitoring configured
- [ ] Throughput monitoring implemented
- [ ] Resource utilization alerts set up
- [ ] Database performance monitoring active
- [ ] Cache performance tracking enabled
- [ ] Performance regression detection in place

### Performance Optimization Review

- [ ] Database queries optimized and indexed
- [ ] Caching strategy implemented
- [ ] Asynchronous processing utilized where appropriate
- [ ] Connection pooling configured optimally
- [ ] Memory usage patterns analyzed
- [ ] Performance bottlenecks identified and addressed
