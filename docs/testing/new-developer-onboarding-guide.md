# New Developer Onboarding Guide - Integration Testing

## Welcome to Integration Testing

This guide will help you get up to speed with our integration testing framework, particularly the unified HTTP client configuration strategy and reactivated test suite.

## Quick Start (5 Minutes)

### 1. Verify Your Environment

```bash
# Check Java version (requires Java 21+)
java -version

# Check Gradle version (requires 8.x)
./gradlew --version

# Run a quick test to verify setup
./gradlew quickTest
```

**Expected Output**:

```
BUILD SUCCESSFUL in 30s
5 actionable tasks: 5 executed
```

### 2. Run Your First Integration Test

```bash
# Run integration tests to see the framework in action
./gradlew integrationTest

# Check the performance report
open build/reports/test-performance/performance-report.html
```

### 3. Understand the Test Structure

Look at this simple example:

```java
@TestPerformanceExtension(maxExecutionTimeMs = 5000, maxMemoryIncreaseMB = 50)
class MyFirstIntegrationTest extends BaseIntegrationTest {
    
    @Test
    void should_validate_application_health() {
        // Given
        String healthEndpoint = baseUrl + "/actuator/health";
        
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(healthEndpoint, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }
}
```

## Understanding Our Testing Framework

### Test Architecture Overview

```
┌─────────────────────────────────────────┐
│           BaseIntegrationTest           │
│  ┌─────────────────────────────────────┐│
│  │  UnifiedTestHttpClientConfiguration ││
│  │  - TestRestTemplate                 ││
│  │  - HTTP Client Factory              ││
│  │  - Timeout Configuration            ││
│  └─────────────────────────────────────┘│
│  ┌─────────────────────────────────────┐│
│  │  @TestPerformanceExtension          ││
│  │  - Execution Time Monitoring        ││
│  │  - Memory Usage Tracking            ││
│  │  - Performance Report Generation    ││
│  └─────────────────────────────────────┘│
└─────────────────────────────────────────┘
```

### Key Components You Need to Know

#### 1. BaseIntegrationTest

Your integration tests should extend this class:

```java
public abstract class BaseIntegrationTest {
    @Autowired
    protected TestRestTemplate restTemplate;  // Pre-configured HTTP client
    
    @LocalServerPort
    protected int port;                       // Random port for test isolation
    
    protected String baseUrl;                 // Base URL for API calls
    
    // Utility methods for resource management
    protected void forceResourceCleanup() { ... }
    protected boolean isMemoryUsageAcceptable() { ... }
}
```

**What you get for free**:

- Pre-configured `TestRestTemplate` with proper timeouts
- Random port assignment to avoid conflicts
- Base URL setup for API calls
- Resource management utilities
- Performance monitoring integration

#### 2. @TestPerformanceExtension

This annotation provides automatic performance monitoring:

```java
@TestPerformanceExtension(
    maxExecutionTimeMs = 10000,    // Fail if test takes longer than 10s
    maxMemoryIncreaseMB = 100,     // Fail if memory increases by more than 100MB
    generateReports = true,        // Generate performance reports
    checkRegressions = true        // Check for performance regressions
)
```

**Performance Thresholds**:

- **Warning**: Tests > 5 seconds
- **Error**: Tests > 30 seconds
- **Memory Warning**: > 50MB increase
- **Memory Critical**: > 80% heap usage

#### 3. UnifiedTestHttpClientConfiguration

Provides consistent HTTP client setup across all tests:

```java
@TestConfiguration
@Profile("test")
public class UnifiedTestHttpClientConfiguration {
    // Provides properly configured TestRestTemplate
    // Handles HttpComponents dependencies
    // Sets appropriate timeouts for test environments
}
```

## Your First Integration Test

### Step 1: Create the Test Class

Create a new file: `src/test/java/solid/humank/genaidemo/integration/MyFeatureIntegrationTest.java`

```java
package solid.humank.genaidemo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import solid.humank.genaidemo.BaseIntegrationTest;
import solid.humank.genaidemo.infrastructure.config.TestPerformanceExtension;

import static org.assertj.core.api.Assertions.assertThat;

@TestPerformanceExtension(maxExecutionTimeMs = 8000, maxMemoryIncreaseMB = 80)
class MyFeatureIntegrationTest extends BaseIntegrationTest {
    
    @Test
    void should_return_application_info() {
        // Given
        String infoEndpoint = baseUrl + "/actuator/info";
        
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(infoEndpoint, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }
}
```

### Step 2: Run Your Test

```bash
# Run your specific test
./gradlew test --tests "MyFeatureIntegrationTest"

# Run with verbose output to see what's happening
./gradlew test --tests "MyFeatureIntegrationTest" --info
```

### Step 3: Check Performance Report

```bash
# Generate performance report
./gradlew generatePerformanceReport

# Open the report
open build/reports/test-performance/performance-report.html
```

## Common Patterns You'll Use

### 1. Testing REST Endpoints

#### GET Request

```java
@Test
void should_retrieve_customer_by_id() {
    // Given
    String customerId = "test-customer-123";
    String endpoint = baseUrl + "/../api/v1/customers/" + customerId;
    
    // When
    ResponseEntity<CustomerResponse> response = restTemplate.getForEntity(
        endpoint, CustomerResponse.class);
    
    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getId()).isEqualTo(customerId);
}
```

#### POST Request with JSON Body

```java
@Test
void should_create_new_customer() {
    // Given
    CreateCustomerRequest request = new CreateCustomerRequest(
        "John Doe", 
        "john.doe@example.com", 
        "123 Main St"
    );
    
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<CreateCustomerRequest> entity = new HttpEntity<>(request, headers);
    
    // When
    ResponseEntity<CustomerResponse> response = restTemplate.postForEntity(
        baseUrl + "/../api/v1/customers", entity, CustomerResponse.class);
    
    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().getName()).isEqualTo("John Doe");
}
```

#### Error Response Testing

```java
@Test
void should_return_not_found_for_invalid_customer_id() {
    // Given
    String invalidId = "non-existent-customer";
    String endpoint = baseUrl + "/../api/v1/customers/" + invalidId;
    
    // When
    ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
        endpoint, ErrorResponse.class);
    
    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody().getErrorCode()).isEqualTo("CUSTOMER_NOT_FOUND");
}
```

### 2. Testing Observability Features

```java
@Test
void should_expose_prometheus_metrics() {
    // Given
    String metricsEndpoint = baseUrl + "/actuator/prometheus";
    
    // When
    ResponseEntity<String> response = restTemplate.getForEntity(metricsEndpoint, String.class);
    
    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("# HELP");
    assertThat(response.getBody()).contains("jvm_memory_used_bytes");
}
```

### 3. Resource Cleanup Pattern

```java
@AfterEach
void cleanupTestResources() {
    // Clean up any test data created during the test
    testDataRepository.deleteAll();
    
    // Check memory usage and cleanup if needed
    if (!isMemoryUsageAcceptable()) {
        forceResourceCleanup();
    }
}
```

## Test Data Management

### Using Test Data Builders

Create reusable test data builders for consistent test data:

```java
public class CustomerTestDataBuilder {
    private String name = "Test Customer";
    private String email = "test@example.com";
    private String address = "123 Test St";
    
    public static CustomerTestDataBuilder aCustomer() {
        return new CustomerTestDataBuilder();
    }
    
    public CustomerTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public CustomerTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }
    
    public CreateCustomerRequest buildRequest() {
        return new CreateCustomerRequest(name, email, address);
    }
}

// Usage in tests
@Test
void should_create_premium_customer() {
    CreateCustomerRequest request = aCustomer()
        .withName("Premium Customer")
        .withEmail("premium@example.com")
        .buildRequest();
    
    // Test implementation
}
```

### Generating Unique Test Data

```java
@Test
void should_handle_unique_email_constraint() {
    // Generate unique test data to avoid conflicts
    String uniqueEmail = "test-" + UUID.randomUUID() + "@example.com";
    String uniqueName = "Test Customer " + System.currentTimeMillis();
    
    CreateCustomerRequest request = new CreateCustomerRequest(uniqueName, uniqueEmail, "123 Test St");
    
    // Test implementation
}
```

## Performance Best Practices

### 1. Choose Appropriate Performance Thresholds

```java
// For simple health checks
@TestPerformanceExtension(maxExecutionTimeMs = 3000, maxMemoryIncreaseMB = 30)

// For standard API tests
@TestPerformanceExtension(maxExecutionTimeMs = 8000, maxMemoryIncreaseMB = 80)

// For complex integration tests
@TestPerformanceExtension(maxExecutionTimeMs = 15000, maxMemoryIncreaseMB = 150)

// For end-to-end tests
@TestPerformanceExtension(maxExecutionTimeMs = 30000, maxMemoryIncreaseMB = 200)
```

### 2. Optimize Test Performance

```java
// ❌ Slow: Multiple individual requests
@Test
void slow_test_multiple_requests() {
    for (int i = 1; i <= 5; i++) {
        restTemplate.getForEntity(baseUrl + "/../api/customers/" + i, CustomerResponse.class);
    }
}

// ✅ Fast: Single batch request
@Test
void optimized_test_batch_request() {
    List<String> customerIds = Arrays.asList("1", "2", "3", "4", "5");
    ResponseEntity<List<CustomerResponse>> response = restTemplate.postForEntity(
        baseUrl + "/../api/customers/batch", customerIds, List.class);
}
```

### 3. Monitor Resource Usage

```java
@Test
void should_monitor_resource_usage() {
    // Perform test operations
    performTestOperations();
    
    // Check resource usage
    if (!isMemoryUsageAcceptable()) {
        fail("Test consumed too much memory");
    }
}
```

## Common Mistakes to Avoid

### ❌ Don't Do This

```java
// Don't create custom TestRestTemplate instances
TestRestTemplate customTemplate = new TestRestTemplate();

// Don't skip performance monitoring
class MyTest extends BaseIntegrationTest {
    // Missing @TestPerformanceExtension
}

// Don't ignore resource cleanup
@Test
void test_without_cleanup() {
    // Create lots of test data
    // No cleanup - causes memory issues
}

// Don't use hardcoded URLs
String url = "http://localhost:8080/../api/endpoint";
```

### ✅ Do This Instead

```java
// Use the provided TestRestTemplate
ResponseEntity<String> response = restTemplate.getForEntity(endpoint, String.class);

// Always use performance monitoring
@TestPerformanceExtension(maxExecutionTimeMs = 10000, maxMemoryIncreaseMB = 100)
class MyTest extends BaseIntegrationTest {
    // Test implementation
}

// Implement proper cleanup
@AfterEach
void cleanup() {
    testDataRepository.deleteAll();
    if (!isMemoryUsageAcceptable()) {
        forceResourceCleanup();
    }
}

// Use the provided baseUrl
String endpoint = baseUrl + "/../api/endpoint";
```

## Debugging Your Tests

### 1. Enable Debug Logging

```bash
# Debug HTTP client issues
./gradlew integrationTest -Dlogging.level.org.springframework.web=DEBUG

# Debug Spring context loading
./gradlew integrationTest -Dlogging.level.org.springframework.boot.test=DEBUG

# Debug test performance
./gradlew integrationTest -Dtest.performance.monitoring.enabled=true --info
```

### 2. Common Debug Scenarios

#### Test Fails with Connection Timeout

```java
@Test
void debug_connection_timeout() {
    try {
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/../api/slow-endpoint", String.class);
    } catch (ResourceAccessException e) {
        // Log the actual error for debugging
        System.out.println("Connection error: " + e.getMessage());
        System.out.println("Base URL: " + baseUrl);
        throw e;
    }
}
```

#### Test Fails with Memory Issues

```java
@BeforeEach
void checkMemoryBefore() {
    System.out.println("Memory before test: " + 
        Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
}

@AfterEach
void checkMemoryAfter() {
    System.out.println("Memory after test: " + 
        Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
}
```

### 3. Using Performance Reports for Debugging

```bash
# Generate detailed performance report
./gradlew generatePerformanceReport

# Look for slow tests
grep "SLOW TEST" build/reports/test-performance/overall-performance-summary.txt

# Check memory usage patterns
cat build/reports/test-performance/MyTest-performance-report.txt
```

## Test Execution Workflow

### Daily Development

```bash
# Quick feedback during development
./gradlew quickTest

# Run integration tests for your feature
./gradlew test --tests "*MyFeature*"

# Check performance impact
./gradlew generatePerformanceReport
```

### Before Committing

```bash
# Run comprehensive pre-commit tests
./gradlew preCommitTest

# Verify no performance regressions
./gradlew integrationTest generatePerformanceReport
```

### Troubleshooting Failed Tests

```bash
# Run with detailed output
./gradlew integrationTest --info --stacktrace

# Check dependencies
./gradlew dependencies --configuration testRuntimeClasspath | grep -i http

# Clean and retry
./gradlew clean integrationTest
```

## Getting Help

### Documentation Resources

1. **[HTTP Client Configuration Guide](http-client-configuration-guide.md)** - Detailed HTTP client setup
2. **[Test Configuration Examples](test-configuration-examples.md)** - Practical examples
3. **[Troubleshooting Guide](testresttemplate-troubleshooting-guide.md)** - Common issues and solutions
4. **[Test Execution Guide](test-execution-maintenance-guide.md)** - Comprehensive execution guide

### Code Examples to Study

1. **SimpleEndToEndValidationTest** - Basic health check validation
2. **EndToEndIntegrationTest** - Complex observability testing
3. **BaseIntegrationTest** - Base class implementation

### Performance Monitoring

- **Reports Location**: `build/reports/test-performance/`
- **HTML Report**: Interactive charts and analysis
- **Text Reports**: Quick summaries and statistics

### When to Ask for Help

- **Configuration Issues**: If tests won't run due to setup problems
- **Performance Problems**: If tests are consistently slow or failing performance checks
- **Complex Scenarios**: If you need to test complex business workflows
- **CI/CD Issues**: If tests pass locally but fail in CI

### Team Resources

- **Pair Programming**: Schedule sessions with experienced team members
- **Code Reviews**: Get feedback on your test implementations
- **Team Meetings**: Discuss testing strategies and best practices

## Next Steps

### Week 1: Get Comfortable

- [ ] Run existing tests successfully
- [ ] Write your first simple integration test
- [ ] Understand performance monitoring basics
- [ ] Learn the common patterns

### Week 2: Build Confidence

- [ ] Write tests for your assigned features
- [ ] Implement proper resource cleanup
- [ ] Use test data builders effectively
- [ ] Debug failing tests independently

### Week 3: Contribute

- [ ] Optimize slow tests you encounter
- [ ] Help other team members with testing questions
- [ ] Suggest improvements to test infrastructure
- [ ] Contribute to testing documentation

## Checklist for Your First Test

Before submitting your first integration test:

- [ ] **Extends BaseIntegrationTest**: Your test class extends the base class
- [ ] **Performance Monitoring**: Uses `@TestPerformanceExtension` with appropriate thresholds
- [ ] **Proper Assertions**: Uses meaningful assertions that verify expected behavior
- [ ] **Resource Cleanup**: Implements cleanup in `@AfterEach` if needed
- [ ] **Unique Test Data**: Uses unique test data to avoid conflicts
- [ ] **Error Handling**: Tests both success and error scenarios
- [ ] **Performance Check**: Runs within expected time and memory limits
- [ ] **Documentation**: Includes clear test method names and comments

Welcome to the team! The integration testing framework is designed to make your life easier while ensuring high-quality, reliable tests. Don't hesitate to ask questions and experiment with the framework to learn how it works.
