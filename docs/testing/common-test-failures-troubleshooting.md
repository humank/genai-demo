# Common Test Failures Troubleshooting Guide

## Overview

This guide provides solutions to the most common test failures encountered in the integration test suite, with specific focus on issues related to the unified HTTP client configuration and reactivated tests.

## Test Failure Categories

### 1. HTTP Client and Connection Issues

#### NoClassDefFoundError: HttpComponents Classes

**Error Messages**:

```
java.lang.NoClassDefFoundError: org/apache/http/client/HttpClient
java.lang.NoClassDefFoundError: org/apache/http/impl/client/CloseableHttpClient
java.lang.ClassNotFoundException: org.apache.http.client.config.RequestConfig
```

**Root Causes**:

- Missing HttpComponents dependencies
- Version conflicts between HttpComponents libraries
- Incomplete dependency resolution

**Solutions**:

1. **Verify dependency configuration**:

```bash
# Check current HttpComponents dependencies
./gradlew dependencies --configuration testRuntimeClasspath | grep -i http

# Expected output should include:
# org.apache.httpcomponents.client5:httpclient5:5.3.1
# org.apache.httpcomponents.core5:httpcore5:5.2.4
```

2. **Update build.gradle if needed**:

```gradle
dependencies {
    // Ensure consistent HttpComponents5 versions
    implementation 'org.apache.httpcomponents.client5:httpclient5:5.3.1'
    implementation 'org.apache.httpcomponents.core5:httpcore5:5.2.4'
    implementation 'org.apache.httpcomponents.core5:httpcore5-h2:5.2.4'
    
    testImplementation 'org.apache.httpcomponents.client5:httpclient5:5.3.1'
    testImplementation 'org.apache.httpcomponents.core5:httpcore5:5.2.4'
}
```

3. **Clean and rebuild**:

```bash
./gradlew clean build
./gradlew integrationTest
```

#### Connection Timeout Exceptions

**Error Messages**:

```
java.net.SocketTimeoutException: Read timed out
java.net.ConnectException: Connection timed out
org.springframework.web.client.ResourceAccessException: I/O error on GET request
```

**Root Causes**:

- Application startup delays
- Network latency in CI environments
- Insufficient timeout configuration

**Solutions**:

1. **Verify timeout configuration**:

```java
// Check UnifiedTestHttpClientConfiguration
@Bean
public ClientHttpRequestFactory clientHttpRequestFactory() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(10)); // Should be adequate
    factory.setReadTimeout(Duration.ofSeconds(30));    // Should be adequate
    return factory;
}
```

2. **Add application readiness check**:

```java
@BeforeEach
void waitForApplicationReady() {
    await().atMost(30, SECONDS)
        .pollInterval(1, SECONDS)
        .until(() -> {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(
                    baseUrl + "/actuator/health", String.class);
                return response.getStatusCode() == HttpStatus.OK;
            } catch (Exception e) {
                return false;
            }
        });
}
```

3. **Implement retry logic for flaky connections**:

```java
@Test
void should_handle_endpoint_with_retry() {
    RetryTemplate retryTemplate = RetryTemplate.builder()
        .maxAttempts(3)
        .fixedBackoff(1000)
        .retryOn(ResourceAccessException.class)
        .build();
    
    ResponseEntity<String> response = retryTemplate.execute(context -> 
        restTemplate.getForEntity(baseUrl + "/../api/endpoint", String.class)
    );
    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
}
```

### 2. Configuration and Bean Creation Issues

#### Multiple Bean Definition Conflicts

**Error Messages**:

```
BeanCreationException: Error creating bean with name 'testRestTemplate'
ConflictingBeanDefinitionException: Annotation-specified bean name 'testRestTemplate' conflicts with existing
```

**Root Causes**:

- Multiple `@TestConfiguration` classes defining TestRestTemplate
- Conflicting `@Primary` annotations
- Legacy test configurations not removed

**Solutions**:

1. **Remove conflicting configurations**:

```java
// ❌ Remove these old configurations
@TestConfiguration
static class OldTestHttpClientConfiguration {
    @Bean TestRestTemplate testRestTemplate() { ... }
}

@TestConfiguration  
static class SimpleTestHttpClientConfiguration {
    @Bean TestRestTemplate simpleTestRestTemplate() { ... }
}
```

2. **Use unified configuration**:

```java
// ✅ Use this approach
@Import(UnifiedTestHttpClientConfiguration.class)
class MyTest extends BaseIntegrationTest {
    // Inherits properly configured TestRestTemplate
}
```

3. **Verify bean configuration**:

```java
@Test
void should_have_correct_test_rest_template_configuration() {
    assertThat(restTemplate).isNotNull();
    assertThat(restTemplate.getRestTemplate()).isNotNull();
    
    // Verify only one TestRestTemplate bean exists
    Map<String, TestRestTemplate> beans = applicationContext
        .getBeansOfType(TestRestTemplate.class);
    assertThat(beans).hasSize(1);
}
```

#### Spring Context Loading Failures

**Error Messages**:

```
ApplicationContextException: Unable to start web server
ContextLoadException: Failed to load ApplicationContext
BeanCreationException: Error creating bean with name 'dataSource'
```

**Root Causes**:

- Missing test profile activation
- Database configuration issues
- Port conflicts

**Solutions**:

1. **Verify test profile activation**:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")  // Ensure test profile is active
@Import(UnifiedTestHttpClientConfiguration.class)
class MyTest extends BaseIntegrationTest {
    // Test implementation
}
```

2. **Check test database configuration**:

```yaml
# application-test.yml
spring:
  profiles:
    active: test
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  jpa:
    hibernate:
      ddl-auto: create-drop
```

3. **Use random port assignment**:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MyTest extends BaseIntegrationTest {
    // Port is automatically assigned via @LocalServerPort
}
```

### 3. Memory and Performance Issues

#### OutOfMemoryError During Tests

**Error Messages**:

```
java.lang.OutOfMemoryError: Java heap space
java.lang.OutOfMemoryError: Metaspace
Test execution extremely slow or hanging
```

**Root Causes**:

- Insufficient heap memory allocation
- Memory leaks in test setup/teardown
- Excessive object creation

**Solutions**:

1. **Increase memory allocation**:

```gradle
tasks.withType(Test) {
    maxHeapSize = '6g'  // Increase from default
    minHeapSize = '2g'
    
    jvmArgs += [
        '-XX:MaxMetaspaceSize=1g',
        '-XX:+UseG1GC',
        '-XX:+UseStringDeduplication'
    ]
}
```

2. **Implement proper resource cleanup**:

```java
@AfterEach
void cleanupResources() {
    // Clear test data
    testDataRepository.deleteAll();
    
    // Clear caches
    cacheManager.getCacheNames().forEach(name -> 
        cacheManager.getCache(name).clear());
    
    // Force cleanup if memory usage is high
    if (!isMemoryUsageAcceptable()) {
        forceResourceCleanup();
    }
}
```

3. **Monitor memory usage**:

```java
@TestPerformanceExtension(
    maxExecutionTimeMs = 10000, 
    maxMemoryIncreaseMB = 100,
    generateReports = true
)
class MyTest extends BaseIntegrationTest {
    // Automatic memory monitoring enabled
}
```

#### Test Performance Degradation

**Symptoms**:

- Tests taking significantly longer than expected
- Performance reports showing regression
- Memory usage continuously increasing

**Solutions**:

1. **Identify slow tests**:

```bash
# Generate performance report
./gradlew generatePerformanceReport

# Check for slow tests (>5s warning, >30s error)
grep "SLOW TEST" build/reports/test-performance/overall-performance-summary.txt
```

2. **Optimize database operations**:

```java
// ❌ Slow: Multiple individual queries
@Test
void slow_test_with_n_plus_one_queries() {
    List<Customer> customers = customerRepository.findAll();
    for (Customer customer : customers) {
        List<Order> orders = orderRepository.findByCustomerId(customer.getId());
        // N+1 query problem
    }
}

// ✅ Fast: Single query with JOIN FETCH
@Test
void optimized_test_with_join_fetch() {
    List<Customer> customers = customerRepository.findAllWithOrders();
    // Single query retrieves all data
}
```

3. **Use batch operations**:

```java
// ❌ Slow: Multiple HTTP requests
@Test
void slow_test_with_multiple_requests() {
    for (int i = 0; i < 10; i++) {
        restTemplate.getForEntity(baseUrl + "/../api/resource/" + i, String.class);
    }
}

// ✅ Fast: Single batch request
@Test
void optimized_test_with_batch_request() {
    List<String> ids = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
    ResponseEntity<List<String>> response = restTemplate.postForEntity(
        baseUrl + "/../api/batch/resources", ids, List.class);
}
```

### 4. Test Data and State Issues

#### Test Data Conflicts

**Error Messages**:

```
DataIntegrityViolationException: could not execute statement
ConstraintViolationException: Unique index or primary key violation
Test failures due to existing data
```

**Root Causes**:

- Test data not cleaned up between tests
- Shared test data causing conflicts
- Database state not reset properly

**Solutions**:

1. **Implement proper test data cleanup**:

```java
@Transactional
@Rollback  // Automatically rollback after each test
class MyTest extends BaseIntegrationTest {
    
    @BeforeEach
    void setupCleanState() {
        // Clean up any existing test data
        testDataRepository.deleteAll();
    }
    
    @AfterEach
    void cleanupTestData() {
        // Additional cleanup if needed
        testDataRepository.deleteAll();
    }
}
```

2. **Use unique test data**:

```java
@Test
void should_create_customer_with_unique_data() {
    // Generate unique test data
    String uniqueEmail = "test-" + UUID.randomUUID() + "@example.com";
    String uniqueName = "Test Customer " + System.currentTimeMillis();
    
    CreateCustomerRequest request = new CreateCustomerRequest(uniqueName, uniqueEmail);
    // Test implementation
}
```

3. **Use test data builders**:

```java
public class CustomerTestDataBuilder {
    private String name = "Test Customer " + UUID.randomUUID();
    private String email = "test-" + UUID.randomUUID() + "@example.com";
    
    public static CustomerTestDataBuilder aCustomer() {
        return new CustomerTestDataBuilder();
    }
    
    public Customer build() {
        return new Customer(name, email);
    }
}

// Usage in tests
@Test
void should_create_customer() {
    Customer customer = aCustomer().build();
    // Test implementation with unique data
}
```

#### Flaky Test Failures

**Symptoms**:

- Tests pass sometimes, fail other times
- Different results in different environments
- Timing-dependent failures

**Solutions**:

1. **Add proper wait conditions**:

```java
@Test
void should_wait_for_async_operation() {
    // Trigger async operation
    restTemplate.postForEntity(baseUrl + "/../api/async-operation", request, String.class);
    
    // Wait for completion with timeout
    await().atMost(10, SECONDS)
        .pollInterval(500, MILLISECONDS)
        .until(() -> {
            ResponseEntity<String> status = restTemplate.getForEntity(
                baseUrl + "/../api/operation-status", String.class);
            return "COMPLETED".equals(status.getBody());
        });
}
```

2. **Use deterministic test data**:

```java
@Test
void should_use_deterministic_test_data() {
    // ❌ Flaky: Random data might cause conflicts
    String randomId = UUID.randomUUID().toString();
    
    // ✅ Stable: Predictable test data
    String testId = "test-customer-" + testName.getMethodName();
    
    // Use testId for consistent behavior
}
```

3. **Implement proper synchronization**:

```java
@Test
void should_handle_concurrent_operations_safely() {
    CountDownLatch latch = new CountDownLatch(1);
    
    // Start async operation
    CompletableFuture.runAsync(() -> {
        // Async operation
        latch.countDown();
    });
    
    // Wait for completion
    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
}
```

### 5. Environment and CI/CD Issues

#### CI Environment Failures

**Symptoms**:

- Tests pass locally but fail in CI
- Different behavior in different CI environments
- Resource constraints in CI

**Solutions**:

1. **Adjust CI-specific configuration**:

```yaml
# GitHub Actions workflow
- name: Run Integration Tests
  run: ./gradlew integrationTest
  env:
    # CI-specific environment variables
    SPRING_PROFILES_ACTIVE: test,ci
    JVM_OPTS: "-Xmx4g -XX:+UseG1GC"
```

2. **Use CI-specific test configuration**:

```yaml
# application-ci.yml
spring:
  profiles:
    active: test,ci
  
  # Longer timeouts for CI environment
  http:
    client:
      connection-timeout: 20s
      read-timeout: 60s
  
  # Reduced resource usage
  jpa:
    hibernate:
      jdbc:
        batch_size: 10
```

3. **Implement CI-specific retry logic**:

```java
@Test
void should_handle_ci_environment_delays() {
    // Detect CI environment
    boolean isCI = System.getenv("CI") != null;
    int maxAttempts = isCI ? 5 : 3;
    
    RetryTemplate retryTemplate = RetryTemplate.builder()
        .maxAttempts(maxAttempts)
        .exponentialBackoff(1000, 2, 10000)
        .retryOn(ResourceAccessException.class)
        .build();
    
    ResponseEntity<String> response = retryTemplate.execute(context -> 
        restTemplate.getForEntity(baseUrl + "/../api/endpoint", String.class)
    );
}
```

## Diagnostic Tools and Commands

### 1. Dependency Analysis

```bash
# Check all HTTP-related dependencies
./gradlew dependencies --configuration testRuntimeClasspath | grep -i http

# Check for version conflicts
./gradlew dependencyInsight --dependency org.apache.httpcomponents

# Verify test classpath
./gradlew printTestClasspath
```

### 2. Test Configuration Debugging

```bash
# Debug Spring context loading
./gradlew integrationTest -Dlogging.level.org.springframework.boot.test=DEBUG

# Debug HTTP client configuration
./gradlew integrationTest -Dlogging.level.org.springframework.web=DEBUG

# Debug test execution
./gradlew integrationTest --info --debug-jvm
```

### 3. Performance Analysis

```bash
# Generate detailed performance report
./gradlew generatePerformanceReport

# Monitor memory usage during tests
./gradlew integrationTest -XX:+PrintGCDetails -XX:+PrintGCTimeStamps

# Profile test execution
./gradlew integrationTest -Dtest.performance.monitoring.enabled=true
```

### 4. Network and Connectivity Testing

```bash
# Test application health endpoint directly
curl -v http://localhost:8080/actuator/health

# Test with timeout
curl --connect-timeout 10 --max-time 30 http://localhost:8080/actuator/health

# Check port availability
netstat -tulpn | grep :8080
```

## Prevention Strategies

### 1. Proactive Monitoring

- **Performance Monitoring**: Use `@TestPerformanceExtension` on all integration tests
- **Resource Monitoring**: Implement memory usage checks in test cleanup
- **Dependency Monitoring**: Regular dependency updates and conflict resolution

### 2. Test Design Best Practices

- **Isolation**: Ensure tests don't depend on each other
- **Cleanup**: Implement proper resource cleanup in `@AfterEach`
- **Timeouts**: Use appropriate timeouts for different operations
- **Retry Logic**: Implement retry for potentially flaky operations

### 3. Configuration Management

- **Unified Configuration**: Always use `UnifiedTestHttpClientConfiguration`
- **Profile Management**: Proper test profile activation and configuration
- **Environment Consistency**: Maintain consistent configuration across environments

## Quick Reference Checklist

When encountering test failures:

- [ ] **Check Dependencies**: Verify HttpComponents dependencies are correct
- [ ] **Verify Configuration**: Ensure `UnifiedTestHttpClientConfiguration` is used
- [ ] **Check Memory**: Monitor memory usage and implement cleanup
- [ ] **Review Logs**: Check application and test logs for errors
- [ ] **Test Isolation**: Verify tests don't interfere with each other
- [ ] **Environment**: Check for environment-specific issues
- [ ] **Performance**: Review performance reports for regressions
- [ ] **Network**: Verify network connectivity and timeouts
- [ ] **Data Cleanup**: Ensure proper test data cleanup
- [ ] **CI Configuration**: Check CI-specific configuration and resources

## Getting Additional Help

If issues persist after following this troubleshooting guide:

1. **Generate Diagnostic Report**:

```bash
./gradlew generatePerformanceReport
./gradlew dependencies --configuration testRuntimeClasspath > dependencies.txt
./gradlew integrationTest --info > test-execution.log 2>&1
```

2. **Collect Relevant Information**:
   - Test failure logs and stack traces
   - Performance reports
   - Dependency information
   - Environment details (local vs CI)

3. **Consult Documentation**:
   - [HTTP Client Configuration Guide](http-client-configuration-guide.md)
   - [Test Execution and Maintenance Guide](test-execution-maintenance-guide.md)
   - **Performance Standards** (請參考專案內部文檔)

4. **Team Escalation**:
   - Reach out to team members familiar with test infrastructure
   - Create detailed issue reports with diagnostic information
   - Schedule pair programming sessions for complex issues

This troubleshooting guide should resolve the majority of common test failures encountered in the integration test suite.
