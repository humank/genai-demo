# Test Performance Guide

## Overview

This guide provides practical examples for monitoring and optimizing test performance using the `@TestPerformanceExtension` annotation and related utilities in our Spring Boot application.

**Purpose**: Ensure tests execute within acceptable time and memory limits while maintaining quality.

**Key Metrics**:
- **Unit Tests**: < 50ms, < 5MB
- **Integration Tests**: < 500ms, < 50MB
- **E2E Tests**: < 3s, < 500MB

---

## Test Performance Extension

### Basic Usage

Based on your actual code at `app/src/test/java/solid/humank/genaidemo/testutils/TestPerformanceExtension.java`:

```java
/**
 * Annotation to enable test performance monitoring
 * 
 * Provides:
 * - Automatic test execution time monitoring
 * - Memory usage tracking
 * - Performance regression detection
 * - Detailed execution reports
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(TestPerformanceMonitor.class)
public @interface TestPerformanceExtension {
    
    /**
     * Maximum allowed execution time in milliseconds
     * Default: 5000ms (5 seconds)
     */
    long maxExecutionTimeMs() default 5000;
    
    /**
     * Maximum allowed memory increase in MB
     * Default: 50MB
     */
    long maxMemoryIncreaseMB() default 50;
}
```

### Applying to Integration Tests

```java
@TestPerformanceExtension(maxExecutionTimeMs = 3000, maxMemoryIncreaseMB = 100)
@DisplayName("Customer API Integration Tests")
class CustomerApiIntegrationTest extends BaseIntegrationTest {
    
    @Test
    @DisplayName("Should complete customer registration flow")
    void shouldCompleteCustomerRegistrationFlow() {
        // Test automatically monitored for:
        // - Execution time (must be < 3000ms)
        // - Memory usage (must be < 100MB increase)
        
        CreateCustomerRequest request = new CreateCustomerRequest(
            "Jane Doe",
            "jane@example.com",
            "SecurePass123!"
        );
        
        ResponseEntity<CustomerResponse> response = performPost(
            "/api/v1/customers",
            request,
            CustomerResponse.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
```

### Method-Level Performance Monitoring

```java
@DisplayName("Order Processing Tests")
class OrderProcessingTest extends BaseIntegrationTest {
    
    @Test
    @TestPerformanceExtension(maxExecutionTimeMs = 1000, maxMemoryIncreaseMB = 30)
    @DisplayName("Should process order quickly")
    void shouldProcessOrderQuickly() {
        // This specific test has stricter performance requirements
        Order order = createOrder();
        orderService.processOrder(order);
        
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSED);
    }
}
```

---

## BaseIntegrationTest Utilities

Based on your actual code at `app/src/test/java/solid/humank/genaidemo/testutils/base/BaseIntegrationTest.java`:

### Resource Management

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({
    UnifiedTestHttpClientConfiguration.class,
    TestProfileConfiguration.class,
    TestPerformanceConfiguration.class
})
public abstract class BaseIntegrationTest {
    
    @Autowired
    protected TestRestTemplate restTemplate;
    
    @LocalServerPort
    protected int port;
    
    protected String baseUrl;
    
    @BeforeEach
    void setUpBaseTest() {
        baseUrl = "http://localhost:" + port;
        
        // Validate test environment
        if (!environmentValidator.validateTestProfile()) {
            throw new IllegalStateException("Test profile is not active");
        }
        
        // Allocate test resources
        resourceManager.allocateTestResources();
    }
    
    @AfterEach
    void tearDownBaseTest() {
        // Cleanup test resources
        resourceManager.cleanupTestResources();
    }
    
    /**
     * Force cleanup of test resources
     */
    protected void forceResourceCleanup() {
        logger.debug("Force cleanup requested");
        System.gc();
    }
    
    /**
     * Check if memory usage is acceptable
     */
    protected boolean isMemoryUsageAcceptable() {
        return true; // Simplified
    }
    
    /**
     * Wait for condition with timeout
     */
    protected void waitForCondition(
        BooleanSupplier condition,
        Duration timeout,
        String description
    ) throws InterruptedException {
        long timeoutMs = timeout.toMillis();
        long startTime = System.currentTimeMillis();
        
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw new AssertionError("Timeout waiting for: " + description);
            }
            Thread.sleep(100);
        }
    }
}
```

### Using Resource Management

```java
@TestPerformanceExtension(maxExecutionTimeMs = 5000, maxMemoryIncreaseMB = 100)
class ResourceIntensiveTest extends BaseIntegrationTest {
    
    @Test
    @DisplayName("Should handle large data set efficiently")
    void shouldHandleLargeDataSetEfficiently() {
        // Create large dataset
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            customers.add(createCustomer("customer" + i + "@example.com"));
        }
        
        // Process data
        customerService.batchCreate(customers);
        
        // Check memory usage
        if (!isMemoryUsageAcceptable()) {
            logger.warn("High memory usage detected");
            forceResourceCleanup();
        }
        
        // Verify results
        assertThat(customerRepository.count()).isEqualTo(1000);
    }
}
```

---

## Performance Testing Patterns

### 1. Concurrent Request Testing

```java
@Test
@TestPerformanceExtension(maxExecutionTimeMs = 10000, maxMemoryIncreaseMB = 200)
@DisplayName("Should handle concurrent requests")
void shouldHandleConcurrentRequests() throws InterruptedException {
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
                    
                    ResponseEntity<CustomerResponse> response = performGet(
                        "/api/v1/customers/test-customer-" + j,
                        CustomerResponse.class
                    );
                    
                    long responseTime = System.currentTimeMillis() - startTime;
                    responseTimes.add(responseTime);
                    
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
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
    
    logger.info("Average response time: {} ms", averageResponseTime);
    logger.info("95th percentile: {} ms", p95ResponseTime);
}
```

### 2. Memory Usage Monitoring

```java
@Test
@TestPerformanceExtension(maxExecutionTimeMs = 5000, maxMemoryIncreaseMB = 100)
@DisplayName("Should not cause memory leak")
void shouldNotCauseMemoryLeak() {
    Runtime runtime = Runtime.getRuntime();
    
    // Record initial memory
    runtime.gc();
    long initialMemory = runtime.totalMemory() - runtime.freeMemory();
    
    // Perform memory-intensive operation
    List<Order> orders = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
        orders.add(createOrder());
    }
    
    orderRepository.saveAll(orders);
    
    // Clear references
    orders.clear();
    orders = null;
    
    // Force garbage collection
    runtime.gc();
    Thread.sleep(100);
    
    // Check memory usage
    long finalMemory = runtime.totalMemory() - runtime.freeMemory();
    long memoryIncrease = (finalMemory - initialMemory) / (1024 * 1024); // MB
    
    logger.info("Memory increase: {} MB", memoryIncrease);
    assertThat(memoryIncrease).isLessThan(50);
}
```

### 3. Database Query Performance

```java
@DataJpaTest
@ActiveProfiles("test")
@TestPerformanceExtension(maxExecutionTimeMs = 500, maxMemoryIncreaseMB = 50)
class QueryPerformanceTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    @DisplayName("Should execute query efficiently with pagination")
    void shouldExecuteQueryEfficientlyWithPagination() {
        // Given - Create test data
        for (int i = 0; i < 100; i++) {
            CustomerEntity customer = createCustomer("customer" + i + "@example.com");
            entityManager.persist(customer);
        }
        entityManager.flush();
        entityManager.clear();
        
        // When - Execute paginated query
        long startTime = System.currentTimeMillis();
        Page<CustomerEntity> page = customerRepository.findAll(
            PageRequest.of(0, 10, Sort.by("createdAt").descending())
        );
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Then - Verify performance
        assertThat(page.getContent()).hasSize(10);
        assertThat(executionTime).isLessThan(100); // Should be < 100ms
        
        logger.info("Query execution time: {} ms", executionTime);
    }
    
    @Test
    @DisplayName("Should avoid N+1 query problem")
    void shouldAvoidNPlusOneQueryProblem() {
        // Given
        OrderEntity order = createOrderWithItems();
        entityManager.persist(order);
        entityManager.flush();
        entityManager.clear();
        
        // When - Use JOIN FETCH
        long startTime = System.currentTimeMillis();
        Optional<OrderEntity> found = orderRepository.findByIdWithItems(order.getId());
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Then - Should load in single query
        assertThat(found).isPresent();
        assertThat(found.get().getItems()).hasSize(2);
        assertThat(executionTime).isLessThan(50);
        
        // Access items without additional queries
        found.get().getItems().forEach(item -> {
            assertThat(item.getProduct()).isNotNull();
        });
    }
}
```

---

## Gradle Test Task Configuration

### Optimized Test Tasks

```gradle
// Unit tests - fast feedback
tasks.register('unitTest', Test) {
    description = 'Fast unit tests (~5MB, ~50ms each)'
    useJUnitPlatform {
        excludeTags 'integration', 'end-to-end'
        includeTags 'unit'
    }
    maxHeapSize = '2g'
    maxParallelForks = Runtime.runtime.availableProcessors()
    forkEvery = 0  // No JVM restart for speed
}

// Integration tests - pre-commit verification
tasks.register('integrationTest', Test) {
    description = 'Integration tests (~50MB, ~500ms each)'
    useJUnitPlatform {
        includeTags 'integration'
        excludeTags 'end-to-end'
    }
    maxHeapSize = '6g'
    minHeapSize = '2g'
    maxParallelForks = 1
    forkEvery = 5
    timeout = Duration.ofMinutes(30)
    
    jvmArgs += [
        '--enable-preview',
        '-XX:MaxMetaspaceSize=1g',
        '-XX:+UseG1GC',
        '-XX:+UseStringDeduplication'
    ]
}

// E2E tests - pre-release verification
tasks.register('e2eTest', Test) {
    description = 'End-to-end tests (~500MB, ~3s each)'
    useJUnitPlatform {
        includeTags 'end-to-end'
    }
    maxHeapSize = '8g'
    minHeapSize = '3g'
    maxParallelForks = 1
    forkEvery = 2
    timeout = Duration.ofHours(1)
}
```

### Running Tests

```bash
# Daily development - fast feedback
./gradlew quickTest              # Unit tests only (< 2 min)

# Pre-commit verification
./gradlew preCommitTest          # Unit + Integration (< 5 min)

# Pre-release verification
./gradlew fullTest               # All tests including E2E

# Specific test types
./gradlew unitTest               # Fast unit tests
./gradlew integrationTest        # Integration tests
./gradlew e2eTest               # End-to-end tests
```

---

## Performance Report Generation

### Generating Reports

```bash
# Generate performance reports
./gradlew generatePerformanceReport

# View reports
open build/reports/test-performance/performance-report.html
```

### Report Contents

The performance report includes:
- **Test Execution Times**: Per test and per class
- **Memory Usage**: Before/after each test
- **Slow Test Identification**: Tests exceeding thresholds
- **Performance Trends**: Historical comparison
- **Resource Utilization**: CPU, memory, database connections

---

## Performance Thresholds

### Warning Thresholds

- **Slow Test Warning**: > 5 seconds
- **Very Slow Test Error**: > 30 seconds
- **Memory Usage Warning**: > 50MB increase
- **Memory Usage Critical**: > 80% of available heap

### Automatic Actions

When thresholds are exceeded:
1. Warning logged to console
2. Details added to performance report
3. Test marked for review
4. Metrics collected for trend analysis

---

## Best Practices

### 1. Use Appropriate Test Types

```java
// GOOD: Unit test for business logic (fast)
@ExtendWith(MockitoExtension.class)
class CustomerTest {
    @Test
    void shouldCalculateDiscount() {
        // < 50ms, < 5MB
    }
}

// GOOD: Integration test for database (moderate)
@DataJpaTest
@TestPerformanceExtension(maxExecutionTimeMs = 500)
class CustomerRepositoryTest {
    @Test
    void shouldFindCustomers() {
        // < 500ms, < 50MB
    }
}

// GOOD: E2E test for complete flow (slow)
@SpringBootTest
@TestPerformanceExtension(maxExecutionTimeMs = 3000)
class CustomerE2ETest {
    @Test
    void shouldCompleteRegistrationFlow() {
        // < 3s, < 500MB
    }
}
```

### 2. Clean Up Resources

```java
@AfterEach
void cleanUp() {
    // Clear test data
    customerRepository.deleteAll();
    
    // Force garbage collection if needed
    if (!isMemoryUsageAcceptable()) {
        forceResourceCleanup();
    }
}
```

### 3. Use Pagination for Large Datasets

```java
@Test
void shouldHandleLargeDataset() {
    // BAD: Load all data at once
    List<Customer> allCustomers = customerRepository.findAll();
    
    // GOOD: Use pagination
    Page<Customer> page = customerRepository.findAll(
        PageRequest.of(0, 100)
    );
}
```

### 4. Monitor and Optimize Queries

```java
@Test
void shouldOptimizeQuery() {
    // BAD: N+1 query problem
    List<Order> orders = orderRepository.findAll();
    orders.forEach(order -> order.getItems().size()); // N+1!
    
    // GOOD: Use JOIN FETCH
    List<Order> orders = orderRepository.findAllWithItems();
    orders.forEach(order -> order.getItems().size()); // Single query
}
```

---

## Quick Reference

### Performance Annotations

```java
// Class-level monitoring
@TestPerformanceExtension(maxExecutionTimeMs = 5000, maxMemoryIncreaseMB = 50)
class MyTest { }

// Method-level monitoring
@Test
@TestPerformanceExtension(maxExecutionTimeMs = 1000, maxMemoryIncreaseMB = 30)
void myTest() { }
```

### Performance Targets

| Test Type | Time Limit | Memory Limit | Use Case |
|-----------|------------|--------------|----------|
| Unit | < 50ms | < 5MB | Business logic |
| Integration | < 500ms | < 50MB | Database, API |
| E2E | < 3s | < 500MB | Complete flows |

### Gradle Commands

```bash
./gradlew quickTest              # Fast unit tests
./gradlew preCommitTest          # Unit + Integration
./gradlew fullTest               # All tests
./gradlew generatePerformanceReport  # Generate report
```

---

## Related Documentation

- **Testing Strategy**: #[[file:../../steering/testing-strategy.md]]
- **Unit Testing**: #[[file:unit-testing-guide.md]]
- **Integration Testing**: #[[file:integration-testing-guide.md]]
- **BDD Testing**: #[[file:bdd-cucumber-guide.md]]
- **Test Performance Standards**: #[[file:../../steering/test-performance-standards.md]]

---

**Document Version**: 1.0  
**Last Updated**: 2025-01-22  
**Owner**: Development Team
