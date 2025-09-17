# Test Execution and Maintenance Guide

## Overview

This guide provides comprehensive instructions for executing, maintaining, and troubleshooting the integration test suite, particularly focusing on the reactivated tests that use the unified HTTP client configuration strategy.

## Test Categories and Classification

### Test Pyramid Structure

Our test suite follows the test pyramid principle with three main categories:

```
    /\
   /  \     E2E Tests (5%)
  /____\    ~500MB, ~3s each
 /      \   
/________\   Integration Tests (15%)
           ~50MB, ~500ms each
___________
           Unit Tests (80%)
           ~5MB, ~50ms each
```

### Test Categories

#### 1. Unit Tests

- **Purpose**: Test individual components in isolation
- **Execution Time**: < 50ms per test
- **Memory Usage**: < 5MB per test
- **Success Rate**: > 99%
- **Annotation**: `@ExtendWith(MockitoExtension.class)`
- **Tag**: `@Tag("unit")`

#### 2. Integration Tests

- **Purpose**: Test component interactions and HTTP endpoints
- **Execution Time**: < 500ms per test
- **Memory Usage**: < 50MB per test
- **Success Rate**: > 95%
- **Annotation**: `@SpringBootTest` with specific slices
- **Tag**: `@Tag("integration")`

#### 3. End-to-End Tests

- **Purpose**: Test complete user journeys and system integration
- **Execution Time**: < 3s per test
- **Memory Usage**: < 500MB per test
- **Success Rate**: > 90%
- **Annotation**: `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- **Tag**: `@Tag("end-to-end")`

## Test Execution Commands

### Daily Development Workflow

#### Quick Feedback Loop (< 2 minutes)

```bash
# Run only unit tests for fast feedback
./gradlew quickTest

# Alternative: Run unit tests directly
./gradlew unitTest
```

#### Pre-Commit Verification (< 5 minutes)

```bash
# Run unit and integration tests
./gradlew preCommitTest

# Alternative: Run specific test categories
./gradlew unitTest integrationTest
```

#### Pre-Release Validation (< 30 minutes)

```bash
# Run complete test suite including E2E and BDD tests
./gradlew fullTest

# Alternative: Run all test types separately
./gradlew test cucumber
```

### Specific Test Type Execution

#### Unit Tests Only

```bash
# Fast unit tests with minimal resource usage
./gradlew unitTest

# With specific tags
./gradlew test --tests "*UnitTest"
```

#### Integration Tests Only

```bash
# Integration tests with moderate resource allocation
./gradlew integrationTest

# With performance monitoring
./gradlew integrationTest -Dtest.performance.monitoring.enabled=true
```

#### End-to-End Tests Only

```bash
# E2E tests with maximum resource allocation
./gradlew e2eTest

# With detailed logging
./gradlew e2eTest --info --debug-jvm
```

#### BDD/Cucumber Tests

```bash
# Run Cucumber BDD tests
./gradlew cucumber

# Run specific feature files
./gradlew cucumber -Dcucumber.features=src/test/resources/features/customer.feature
```

### Performance and Reporting

#### Generate Performance Reports

```bash
# Generate comprehensive performance reports
./gradlew generatePerformanceReport

# Run tests with performance monitoring and generate reports
./gradlew runAllTestsWithReport
```

#### View Performance Reports

```bash
# Open HTML performance report
open build/reports/test-performance/performance-report.html

# View text-based summary
cat build/reports/test-performance/overall-performance-summary.txt
```

## Test Configuration Management

### Gradle Test Task Configuration

#### Memory Allocation by Test Type

```gradle
// Unit tests - minimal resources
tasks.register('unitTest', Test) {
    maxHeapSize = '2g'
    maxParallelForks = Runtime.runtime.availableProcessors()
    forkEvery = 0  // No JVM restart for speed
    
    useJUnitPlatform {
        excludeTags 'integration', 'end-to-end', 'slow'
        includeTags 'unit'
    }
}

// Integration tests - moderate resources
tasks.register('integrationTest', Test) {
    maxHeapSize = '6g'
    minHeapSize = '2g'
    maxParallelForks = 1
    forkEvery = 5
    timeout = Duration.ofMinutes(30)
    
    useJUnitPlatform {
        includeTags 'integration'
        excludeTags 'end-to-end', 'slow'
    }
    
    // HttpComponents optimization
    jvmArgs += [
        '-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog',
        '-Dsun.net.useExclusiveBind=false',
        '-Djava.net.preferIPv4Stack=true'
    ]
}

// E2E tests - maximum resources
tasks.register('e2eTest', Test) {
    maxHeapSize = '8g'
    minHeapSize = '3g'
    maxParallelForks = 1
    forkEvery = 2
    timeout = Duration.ofHours(1)
    
    useJUnitPlatform {
        includeTags 'end-to-end'
    }
}
```

#### System Properties Configuration

```gradle
// Integration test system properties
systemProperties = [
    'junit.jupiter.execution.timeout.default': '2m',
    'spring.profiles.active': 'test',
    'http.client.connection.timeout': '10000',
    'http.client.socket.timeout': '30000',
    'test.resource.cleanup.enabled': 'true',
    'test.memory.monitoring.enabled': 'true'
]
```

### Environment-Specific Configuration

#### Test Profile Properties (`application-test.yml`)

```yaml
spring:
  profiles:
    active: test
  
  # Database configuration for tests
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
    properties:
      hibernate:
        format_sql: false
  
  # HTTP client configuration for tests
  http:
    client:
      factory: simple
      connection-timeout: 10s
      read-timeout: 30s
      max-connections: 20
  
  # Disable unnecessary features for tests
  main:
    lazy-initialization: true
  jmx:
    enabled: false
  
  # Observability configuration for tests
  management:
    endpoints:
      web:
        exposure:
          include: health,metrics,prometheus,info
    endpoint:
      health:
        show-details: always
    metrics:
      export:
        prometheus:
          enabled: true

# Logging configuration optimized for tests
logging:
  level:
    root: ERROR
    solid.humank.genaidemo: INFO
    org.springframework.web: WARN
    org.springframework.boot.test: WARN
    org.hibernate.SQL: WARN
```

## Test Maintenance Procedures

### Regular Maintenance Tasks

#### Weekly Maintenance Checklist

- [ ] **Performance Review**: Check test performance reports for regressions
- [ ] **Dependency Updates**: Review and update test dependencies
- [ ] **Test Coverage**: Ensure coverage remains above 80%
- [ ] **Flaky Test Analysis**: Identify and fix flaky tests
- [ ] **Resource Usage**: Monitor test resource consumption trends

#### Monthly Maintenance Checklist

- [ ] **Test Suite Optimization**: Review and optimize slow tests
- [ ] **Configuration Review**: Validate test configurations are up-to-date
- [ ] **Documentation Updates**: Update test documentation as needed
- [ ] **Tool Updates**: Update testing tools and frameworks
- [ ] **Performance Benchmarks**: Update performance benchmarks

### Test Configuration Maintenance

#### Updating HTTP Client Configuration

1. **Modify UnifiedTestHttpClientConfiguration**:

```java
@TestConfiguration
@Profile("test")
public class UnifiedTestHttpClientConfiguration {
    
    @Bean
    @Primary
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // Update timeout values as needed
        factory.setConnectTimeout(Duration.ofSeconds(15)); // Increased from 10s
        factory.setReadTimeout(Duration.ofSeconds(45));    // Increased from 30s
        
        return factory;
    }
}
```

2. **Test the configuration changes**:

```bash
# Run integration tests to verify configuration
./gradlew integrationTest

# Check for timeout-related failures
./gradlew integrationTest --info | grep -i timeout
```

#### Managing Test Dependencies

1. **Update HttpComponents dependencies**:

```gradle
dependencies {
    // Update to latest stable versions
    implementation 'org.apache.httpcomponents.client5:httpclient5:5.3.2'
    implementation 'org.apache.httpcomponents.core5:httpcore5:5.2.5'
    
    testImplementation 'org.apache.httpcomponents.client5:httpclient5:5.3.2'
    testImplementation 'org.apache.httpcomponents.core5:httpcore5:5.2.5'
}
```

2. **Verify dependency compatibility**:

```bash
# Check for dependency conflicts
./gradlew dependencies --configuration testRuntimeClasspath | grep http

# Run tests to ensure compatibility
./gradlew integrationTest
```

### Performance Monitoring and Optimization

#### Performance Monitoring Setup

1. **Enable performance monitoring**:

```java
@TestPerformanceExtension(
    maxExecutionTimeMs = 10000, 
    maxMemoryIncreaseMB = 100,
    generateReports = true,
    checkRegressions = true
)
class MyIntegrationTest extends BaseIntegrationTest {
    // Test implementation
}
```

2. **Configure performance thresholds**:

```yaml
# application-test.yml
test:
  performance:
    thresholds:
      execution-time-warning: 5000ms
      execution-time-error: 30000ms
      memory-usage-warning: 50MB
      memory-usage-error: 200MB
```

#### Performance Optimization Strategies

1. **Optimize slow tests**:

```java
// Before: Slow test with multiple HTTP calls
@Test
void slow_test_with_multiple_calls() {
    for (int i = 0; i < 10; i++) {
        restTemplate.getForEntity(baseUrl + "/api/endpoint/" + i, String.class);
    }
}

// After: Optimized test with batch operations
@Test
void optimized_test_with_batch_operations() {
    List<String> ids = Arrays.asList("1", "2", "3", "4", "5");
    ResponseEntity<List<String>> response = restTemplate.postForEntity(
        baseUrl + "/api/batch/endpoint", ids, List.class);
}
```

2. **Implement resource cleanup**:

```java
@AfterEach
void cleanupResources() {
    // Clean up test data
    testDataCleanupService.cleanupTestData();
    
    // Force garbage collection if memory usage is high
    if (!isMemoryUsageAcceptable()) {
        forceResourceCleanup();
    }
}
```

## Troubleshooting Common Issues

### Test Execution Failures

#### 1. Memory-Related Issues

**Symptoms**:

- `OutOfMemoryError: Java heap space`
- Tests running extremely slowly
- Test failures due to resource exhaustion

**Solutions**:

```bash
# Increase memory allocation for specific test type
./gradlew integrationTest -Xmx8g

# Monitor memory usage during tests
./gradlew integrationTest -XX:+PrintGCDetails -XX:+PrintGCTimeStamps

# Force cleanup between tests
./gradlew integrationTest -Dtest.resource.cleanup.enabled=true
```

#### 2. HTTP Client Configuration Issues

**Symptoms**:

- `NoClassDefFoundError` for HttpComponents classes
- Connection timeout exceptions
- SSL handshake failures

**Solutions**:

```bash
# Verify HTTP client dependencies
./gradlew dependencies --configuration testRuntimeClasspath | grep http

# Check HTTP client configuration loading
./gradlew integrationTest -Dlogging.level.org.springframework.web=DEBUG

# Test with simplified HTTP client
./gradlew integrationTest -Dhttp.client.factory=simple
```

#### 3. Port Conflicts

**Symptoms**:

- `BindException: Address already in use`
- Random port assignment failures
- Tests failing intermittently

**Solutions**:

```bash
# Run tests sequentially to avoid port conflicts
./gradlew integrationTest --max-workers=1

# Use different port ranges for different test types
./gradlew integrationTest -Dserver.port=0

# Check for port usage
netstat -tulpn | grep :8080
```

### Test Performance Issues

#### 1. Slow Test Execution

**Diagnosis**:

```bash
# Generate performance report to identify slow tests
./gradlew generatePerformanceReport

# Run with performance profiling
./gradlew integrationTest -Dtest.performance.monitoring.enabled=true

# Check for slow database queries
./gradlew integrationTest -Dlogging.level.org.hibernate.SQL=DEBUG
```

**Optimization**:

```java
// Use @TestPerformanceExtension to monitor and optimize
@TestPerformanceExtension(maxExecutionTimeMs = 5000, maxMemoryIncreaseMB = 50)
class OptimizedTest extends BaseIntegrationTest {
    
    @Test
    void optimized_test_method() {
        // Implement efficient test logic
        // Use batch operations where possible
        // Minimize HTTP calls
        // Clean up resources promptly
    }
}
```

#### 2. Memory Leaks in Tests

**Detection**:

```bash
# Monitor memory usage trends
./gradlew integrationTest -XX:+PrintGCDetails

# Generate heap dumps for analysis
./gradlew integrationTest -XX:+HeapDumpOnOutOfMemoryError
```

**Prevention**:

```java
@AfterEach
void preventMemoryLeaks() {
    // Clear caches
    cacheManager.getCacheNames().forEach(name -> 
        cacheManager.getCache(name).clear());
    
    // Close resources
    if (dataSource instanceof Closeable) {
        ((Closeable) dataSource).close();
    }
    
    // Force cleanup if needed
    if (!isMemoryUsageAcceptable()) {
        forceResourceCleanup();
    }
}
```

## Onboarding Guide for New Developers

### Getting Started with Integration Tests

#### 1. Environment Setup

**Prerequisites**:

- Java 21 or higher
- Gradle 8.x
- IDE with JUnit 5 support

**Setup Steps**:

```bash
# Clone the repository
git clone <repository-url>
cd <project-directory>

# Run initial test to verify setup
./gradlew quickTest

# Verify integration test configuration
./gradlew integrationTest --dry-run
```

#### 2. Understanding the Test Structure

**Key Components**:

- `BaseIntegrationTest`: Base class for all integration tests
- `UnifiedTestHttpClientConfiguration`: HTTP client configuration
- `@TestPerformanceExtension`: Performance monitoring annotation

**Example Test Structure**:

```java
@TestPerformanceExtension(maxExecutionTimeMs = 10000, maxMemoryIncreaseMB = 100)
class MyFirstIntegrationTest extends BaseIntegrationTest {
    
    @Test
    void should_validate_basic_functionality() {
        // Given
        String endpoint = baseUrl + "/actuator/health";
        
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(endpoint, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

#### 3. Writing Your First Integration Test

**Step-by-Step Guide**:

1. **Create test class**:

```java
package solid.humank.genaidemo.integration;

import solid.humank.genaidemo.BaseIntegrationTest;
import solid.humank.genaidemo.infrastructure.config.TestPerformanceExtension;

@TestPerformanceExtension(maxExecutionTimeMs = 8000, maxMemoryIncreaseMB = 80)
class MyFeatureIntegrationTest extends BaseIntegrationTest {
    // Test methods go here
}
```

2. **Implement test methods**:

```java
@Test
void should_create_and_retrieve_resource() {
    // Given - Create test data
    CreateResourceRequest request = new CreateResourceRequest("test-name", "test-value");
    
    // When - Create resource
    ResponseEntity<ResourceResponse> createResponse = restTemplate.postForEntity(
        baseUrl + "/api/v1/resources", request, ResourceResponse.class);
    
    // Then - Verify creation
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    String resourceId = createResponse.getBody().getId();
    
    // When - Retrieve resource
    ResponseEntity<ResourceResponse> getResponse = restTemplate.getForEntity(
        baseUrl + "/api/v1/resources/" + resourceId, ResourceResponse.class);
    
    // Then - Verify retrieval
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getResponse.getBody().getName()).isEqualTo("test-name");
}
```

3. **Run and verify tests**:

```bash
# Run your specific test
./gradlew test --tests "MyFeatureIntegrationTest"

# Run with performance monitoring
./gradlew integrationTest --tests "*MyFeature*"

# Check performance report
open build/reports/test-performance/performance-report.html
```

### Best Practices for New Developers

#### 1. Test Design Principles

**✅ Do**:

- Extend `BaseIntegrationTest` for integration tests
- Use `@TestPerformanceExtension` for performance monitoring
- Follow Given-When-Then structure
- Use meaningful test method names
- Clean up resources in `@AfterEach` methods

**❌ Don't**:

- Create custom HTTP client configurations
- Skip performance monitoring annotations
- Write tests that depend on external services
- Ignore memory usage warnings
- Leave resources uncleaned after tests

#### 2. Common Patterns

**HTTP Request Pattern**:

```java
// POST request with JSON body
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);
HttpEntity<RequestDto> entity = new HttpEntity<>(requestDto, headers);

ResponseEntity<ResponseDto> response = restTemplate.postForEntity(
    baseUrl + "/api/v1/endpoint", entity, ResponseDto.class);
```

**Error Handling Pattern**:

```java
// Test error responses
ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
    baseUrl + "/api/v1/nonexistent", ErrorResponse.class);

assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
assertThat(response.getBody().getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
```

**Resource Cleanup Pattern**:

```java
@AfterEach
void cleanup() {
    // Clean up test data
    testDataRepository.deleteAll();
    
    // Check memory usage
    if (!isMemoryUsageAcceptable()) {
        forceResourceCleanup();
    }
}
```

#### 3. Debugging Tips

**Enable Debug Logging**:

```bash
# Debug HTTP client issues
./gradlew integrationTest -Dlogging.level.org.springframework.web=DEBUG

# Debug test configuration loading
./gradlew integrationTest -Dlogging.level.org.springframework.boot.test=DEBUG

# Debug performance issues
./gradlew integrationTest -Dtest.performance.monitoring.enabled=true --info
```

**Common Debugging Commands**:

```bash
# Check test dependencies
./gradlew dependencies --configuration testRuntimeClasspath

# Verify test configuration
./gradlew integrationTest --dry-run --info

# Monitor resource usage
./gradlew integrationTest -XX:+PrintGCDetails -XX:+PrintGCTimeStamps
```

### Learning Resources

#### Documentation References

- [HTTP Client Configuration Guide](http-client-configuration-guide.md)
- [TestRestTemplate Troubleshooting Guide](testresttemplate-troubleshooting-guide.md)
- [Test Configuration Examples](test-configuration-examples.md)
- [Performance Standards](../performance-standards.md)
- [Development Standards](../development-standards.md)

#### Code Examples

- `SimpleEndToEndValidationTest`: Basic health check validation
- `EndToEndIntegrationTest`: Comprehensive observability testing
- `BaseIntegrationTest`: Base class with common utilities

#### Performance Monitoring

- Performance reports: `build/reports/test-performance/`
- Memory usage monitoring with `@TestPerformanceExtension`
- Resource cleanup utilities in `BaseIntegrationTest`

## Continuous Integration Integration

### CI/CD Pipeline Configuration

#### GitHub Actions Workflow

```yaml
name: Integration Tests

on:
  pull_request:
    branches: [ main, develop ]
  push:
    branches: [ main ]

jobs:
  integration-tests:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
    
    - name: Run Unit Tests
      run: ./gradlew unitTest
    
    - name: Run Integration Tests
      run: ./gradlew integrationTest
      
    - name: Generate Performance Report
      run: ./gradlew generatePerformanceReport
      
    - name: Upload Test Reports
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: test-reports
        path: |
          build/reports/tests/
          build/reports/test-performance/
```

#### Test Execution Strategy in CI

```bash
# Fast feedback for PR validation
./gradlew quickTest

# Comprehensive testing for main branch
./gradlew fullTest

# Performance regression detection
./gradlew integrationTest generatePerformanceReport
```

This comprehensive guide provides all the necessary information for executing, maintaining, and troubleshooting the integration test suite, ensuring reliable and efficient test execution across different environments and development workflows.
