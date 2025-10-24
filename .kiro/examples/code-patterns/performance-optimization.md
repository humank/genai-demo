# Performance Optimization Patterns

## Overview

Performance optimization techniques and patterns for our project.

**Related Standards**: [Performance Standards](../../steering/performance-standards.md)

---

## Database Optimization

### Query Optimization

```java
// ❌ BAD: N+1 query problem
@Service
public class OrderService {
    public List<OrderSummary> getOrderSummaries(String customerId) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        
        return orders.stream()
            .map(order -> {
                // This causes N+1 queries!
                List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                return new OrderSummary(order, items);
            })
            .collect(Collectors.toList());
    }
}

// ✅ GOOD: Use JOIN FETCH
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    
    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.customerId = :customerId")
    List<Order> findByCustomerIdWithItems(@Param("customerId") String customerId);
}

@Service
public class OrderService {
    public List<OrderSummary> getOrderSummaries(String customerId) {
        List<Order> orders = orderRepository.findByCustomerIdWithItems(customerId);
        return orders.stream()
            .map(OrderSummary::from)
            .collect(Collectors.toList());
    }
}
```

### Pagination

```java
// ✅ GOOD: Always paginate large result sets
@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    
    @Query("SELECT c FROM Customer c WHERE c.registrationDate >= :date ORDER BY c.registrationDate DESC")
    Page<Customer> findRecentCustomers(@Param("date") LocalDate date, Pageable pageable);
}

@Service
public class CustomerService {
    public Page<CustomerDto> getRecentCustomers(LocalDate since, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Customer> customers = customerRepository.findRecentCustomers(since, pageable);
        return customers.map(CustomerDto::from);
    }
}
```

### Batch Operations

```java
// ❌ BAD: Individual updates
public void updateCustomerStatuses(List<String> customerIds, CustomerStatus newStatus) {
    for (String customerId : customerIds) {
        Customer customer = customerRepository.findById(customerId).get();
        customer.setStatus(newStatus);
        customerRepository.save(customer); // N database calls!
    }
}

// ✅ GOOD: Batch update
@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    
    @Modifying
    @Query("UPDATE Customer c SET c.status = :status WHERE c.id IN :customerIds")
    int updateStatusBatch(@Param("customerIds") List<String> customerIds, 
                         @Param("status") CustomerStatus status);
}

public void updateCustomerStatuses(List<String> customerIds, CustomerStatus newStatus) {
    customerRepository.updateStatusBatch(customerIds, newStatus); // Single query
}
```

---

## Caching Strategies

### Application-Level Caching

```java
@Service
@CacheConfig(cacheNames = "customers")
public class CustomerService {
    
    @Cacheable(key = "#customerId", unless = "#result == null")
    public Customer findById(String customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
    
    @Cacheable(key = "'active-customers-' + #pageable.pageNumber", 
               condition = "#pageable.pageNumber < 10")
    public Page<Customer> findActiveCustomers(Pageable pageable) {
        return customerRepository.findByStatus(CustomerStatus.ACTIVE, pageable);
    }
    
    @CacheEvict(key = "#customer.id")
    public Customer updateCustomer(Customer customer) {
        return customerRepository.save(customer);
    }
    
    @CacheEvict(allEntries = true)
    public void clearCustomerCache() {
        // Cache cleared automatically
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
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
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

### Async Methods

```java
@Service
public class NotificationService {
    
    @Async("taskExecutor")
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public CompletableFuture<Void> sendWelcomeEmail(String customerId) {
        try {
            Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
            
            emailService.sendWelcomeEmail(customer.getEmail(), customer.getName());
            
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Failed to send welcome email", kv("customerId", customerId), e);
            throw e;
        }
    }
    
    @Async("taskExecutor")
    public CompletableFuture<CustomerAnalytics> generateAnalytics(String customerId) {
        return CompletableFuture.supplyAsync(() -> 
            analyticsService.generateAnalytics(customerId)
        );
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
```

### Monitoring

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

## Lazy Loading

```java
@Entity
public class Customer {
    
    @Id
    private String id;
    
    private String name;
    private String email;
    
    // Lazy load expensive relationships
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    @BatchSize(size = 25)
    private List<Order> orders = new ArrayList<>();
    
    @OneToOne(mappedBy = "customer", fetch = FetchType.LAZY)
    private CustomerProfile profile;
}

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    
    // Eager load when needed
    @Query("SELECT c FROM Customer c JOIN FETCH c.profile WHERE c.id = :id")
    Optional<Customer> findByIdWithProfile(@Param("id") String id);
    
    @Query("SELECT c FROM Customer c JOIN FETCH c.orders WHERE c.id = :id")
    Optional<Customer> findByIdWithOrders(@Param("id") String id);
}
```

---

## Performance Monitoring

```java
@Component
public class PerformanceMonitor {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void recordHttpRequest(HttpRequestEvent event) {
        Timer.builder("http.request.duration")
            .tag("method", event.getMethod())
            .tag("endpoint", event.getEndpoint())
            .tag("status", String.valueOf(event.getStatus()))
            .register(meterRegistry)
            .record(event.getDuration(), TimeUnit.MILLISECONDS);
    }
    
    @EventListener
    public void recordDatabaseQuery(DatabaseQueryEvent event) {
        Timer.builder("database.query.duration")
            .tag("query_type", event.getQueryType())
            .tag("table", event.getTableName())
            .register(meterRegistry)
            .record(event.getDuration());
        
        if (event.getDuration().toMillis() > 100) {
            Counter.builder("database.slow.queries")
                .tag("query_type", event.getQueryType())
                .register(meterRegistry)
                .increment();
        }
    }
}
```

---

## Performance Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPerformanceExtension(maxExecutionTimeMs = 30000, maxMemoryIncreaseMB = 200)
class PerformanceTest {
    
    @Test
    void should_handle_concurrent_requests() throws InterruptedException {
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
                            "http://localhost:" + port + "/api/v1/customers/test-" + j,
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
        
        double avgResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        long p95ResponseTime = responseTimes.stream()
            .sorted()
            .skip((long) (responseTimes.size() * 0.95))
            .findFirst()
            .orElse(0L);
        
        assertThat(avgResponseTime).isLessThan(500);
        assertThat(p95ResponseTime).isLessThan(1000);
    }
}
```

---

## Summary

Key performance optimization strategies:

1. **Optimize database queries** - Avoid N+1, use JOIN FETCH, paginate
2. **Implement caching** - Application and distributed caching
3. **Use async processing** - For long-running operations
4. **Configure connection pools** - Proper sizing and monitoring
5. **Lazy load relationships** - Load only when needed
6. **Monitor performance** - Track metrics and identify bottlenecks
7. **Test performance** - Load testing and benchmarking

---

**Related Documentation**:
- [Performance Standards](../../steering/performance-standards.md)
- [Test Performance Standards](../../steering/test-performance-standards.md)
- [Development Standards](../../steering/development-standards.md)
