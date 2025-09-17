# TestRestTemplate Troubleshooting Guide

## Overview

This guide provides solutions to common TestRestTemplate issues encountered in integration tests, particularly those related to HttpComponents dependency conflicts and configuration problems.

## Common Issues and Solutions

### 1. NoClassDefFoundError: HttpComponents Classes

#### Symptoms

```
java.lang.NoClassDefFoundError: org/apache/http/client/HttpClient
java.lang.NoClassDefFoundError: org/apache/http/impl/client/CloseableHttpClient
java.lang.ClassNotFoundException: org.apache.http.client.config.RequestConfig
```

#### Root Cause

- Missing or conflicting HttpComponents dependencies
- Incomplete dependency sets in build.gradle
- Version mismatches between HttpComponents libraries

#### Solution

1. **Update build.gradle with unified HttpComponents5 dependencies**:

```gradle
dependencies {
    // Unified HttpComponents5 dependencies with consistent versions
    implementation 'org.apache.httpcomponents.client5:httpclient5:5.3.1'
    implementation 'org.apache.httpcomponents.core5:httpcore5:5.2.4'
    implementation 'org.apache.httpcomponents.core5:httpcore5-h2:5.2.4'
    
    // Test-specific dependencies
    testImplementation 'org.apache.httpcomponents.client5:httpclient5:5.3.1'
    testImplementation 'org.apache.httpcomponents.core5:httpcore5:5.2.4'
    testImplementation 'org.apache.httpcomponents.client5:httpclient5-fluent:5.3.1'
}
```

2. **Use UnifiedTestHttpClientConfiguration**:

```java
@Import(UnifiedTestHttpClientConfiguration.class)
class MyIntegrationTest extends BaseIntegrationTest {
    // Test implementation
}
```

3. **Verify dependency resolution**:

```bash
./gradlew dependencies --configuration testRuntimeClasspath | grep http
```

### 2. TestRestTemplate Connection Timeouts

#### Symptoms

```
java.net.SocketTimeoutException: Read timed out
java.net.ConnectException: Connection timed out
org.springframework.web.client.ResourceAccessException: I/O error on GET request
```

#### Root Cause

- Default timeout values too low for test environments
- Network latency in CI/CD environments
- Application startup delays during tests

#### Solution

1. **Use configured timeouts from UnifiedTestHttpClientConfiguration**:

```java
// Timeouts are automatically configured:
// - Connect timeout: 10 seconds
// - Read timeout: 30 seconds
```

2. **For custom timeout requirements**:

```java
@TestConfiguration
static class CustomTimeoutConfig {
    @Bean
    @Primary
    public ClientHttpRequestFactory customRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(20));
        factory.setReadTimeout(Duration.ofSeconds(60));
        return factory;
    }
}
```

3. **Add retry logic for flaky connections**:

```java
@Test
void should_handle_connection_with_retry() {
    RetryTemplate retryTemplate = RetryTemplate.builder()
        .maxAttempts(3)
        .fixedBackoff(1000)
        .retryOn(ResourceAccessException.class)
        .build();
    
    ResponseEntity<String> response = retryTemplate.execute(context -> 
        restTemplate.getForEntity(baseUrl + "/api/endpoint", String.class)
    );
    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
}
```

### 3. Multiple HTTP Client Configurations Conflict

#### Symptoms

```
BeanCreationException: Error creating bean with name 'testRestTemplate'
ConflictingBeanDefinitionException: Annotation-specified bean name 'testRestTemplate' for bean class conflicts with existing
```

#### Root Cause

- Multiple `@TestConfiguration` classes defining TestRestTemplate beans
- Conflicting `@Primary` annotations
- Production and test configurations interfering

#### Solution

1. **Remove redundant test configurations**:

```java
// ❌ Remove these configurations
@TestConfiguration
static class TestHttpClientConfiguration {
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

3. **Verify bean uniqueness**:

```java
@Test
void should_have_single_test_rest_template_bean() {
    Map<String, TestRestTemplate> beans = applicationContext
        .getBeansOfType(TestRestTemplate.class);
    assertThat(beans).hasSize(1);
}
```

### 4. Memory Issues During Integration Tests

#### Symptoms

```
java.lang.OutOfMemoryError: Java heap space
java.lang.OutOfMemoryError: Metaspace
Test execution extremely slow
```

#### Root Cause

- Insufficient heap memory allocation
- Memory leaks in test setup/teardown
- Excessive object creation during tests

#### Solution

1. **Use proper Gradle test configuration**:

```gradle
tasks.withType(Test) {
    maxHeapSize = '6g'
    minHeapSize = '2g'
    
    jvmArgs += [
        '-XX:MaxMetaspaceSize=1g',
        '-XX:+UseG1GC',
        '-XX:+UseStringDeduplication'
    ]
}
```

2. **Implement resource cleanup**:

```java
class MyIntegrationTest extends BaseIntegrationTest {
    
    @AfterEach
    void cleanupResources() {
        if (!isMemoryUsageAcceptable()) {
            forceResourceCleanup();
        }
    }
}
```

3. **Monitor memory usage**:

```java
@TestPerformanceExtension(maxExecutionTimeMs = 10000, maxMemoryIncreaseMB = 100)
class MyTest extends BaseIntegrationTest {
    // Automatic memory monitoring enabled
}
```

### 5. SSL/TLS Certificate Issues in Tests

#### Symptoms

```
javax.net.ssl.SSLHandshakeException: PKIX path building failed
sun.security.validator.ValidatorException: PKIX path validation failed
```

#### Root Cause

- Self-signed certificates in test environments
- Certificate validation enabled for test HTTP clients

#### Solution

1. **Disable SSL validation for tests**:

```java
@TestConfiguration
static class TestSSLConfig {
    @Bean
    @Primary
    public ClientHttpRequestFactory sslIgnoringRequestFactory() throws Exception {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
        
        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
            .loadTrustMaterial(null, acceptingTrustStrategy)
            .build();
            
        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
        
        CloseableHttpClient httpClient = HttpClients.custom()
            .setSSLSocketFactory(csf)
            .build();
            
        HttpComponentsClientHttpRequestFactory requestFactory = 
            new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        
        return requestFactory;
    }
}
```

2. **Use HTTP instead of HTTPS for tests**:

```yaml
# application-test.yml
server:
  ssl:
    enabled: false
  port: 8080  # Use HTTP port for tests
```

### 6. Port Conflicts in Parallel Test Execution

#### Symptoms

```
java.net.BindException: Address already in use
Port 8080 is already in use
Random port assignment failed
```

#### Root Cause

- Multiple test instances trying to use same port
- Parallel test execution without proper port management
- Previous test instances not properly shut down

#### Solution

1. **Use random port assignment**:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MyTest extends BaseIntegrationTest {
    // Port is automatically assigned and available via @LocalServerPort
}
```

2. **Configure test task parallelism**:

```gradle
tasks.withType(Test) {
    maxParallelForks = 1  // Prevent parallel execution conflicts
    forkEvery = 5         // Restart JVM every 5 tests
}
```

3. **Implement proper test isolation**:

```java
@TestMethodOrder(OrderAnnotation.class)
class MyTest extends BaseIntegrationTest {
    
    @BeforeEach
    void waitForApplicationReady() {
        await().atMost(30, SECONDS)
            .until(() -> {
                try {
                    restTemplate.getForEntity(baseUrl + "/actuator/health", String.class);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            });
    }
}
```

## Diagnostic Commands

### 1. Check HTTP Client Dependencies

```bash
# List all HTTP-related dependencies
./gradlew dependencies --configuration testRuntimeClasspath | grep -i http

# Check for version conflicts
./gradlew dependencyInsight --dependency org.apache.httpcomponents
```

### 2. Verify Test Configuration Loading

```bash
# Run tests with debug logging
./gradlew test --debug-jvm --info

# Check Spring context loading
./gradlew test -Dlogging.level.org.springframework=DEBUG
```

### 3. Monitor Test Performance

```bash
# Run tests with performance monitoring
./gradlew integrationTest

# Generate performance reports
./gradlew generatePerformanceReport

# Check memory usage during tests
./gradlew test -Xmx8g -XX:+PrintGCDetails
```

### 4. Test HTTP Connectivity

```bash
# Test application health endpoint
curl -v http://localhost:8080/actuator/health

# Test with timeout
curl --connect-timeout 10 --max-time 30 http://localhost:8080/actuator/health
```

## Prevention Strategies

### 1. Consistent Configuration Management

- Always use `UnifiedTestHttpClientConfiguration`
- Extend `BaseIntegrationTest` for integration tests
- Avoid creating custom HTTP client configurations

### 2. Dependency Management

- Use consistent HttpComponents5 versions
- Regularly update dependencies to latest stable versions
- Monitor for security vulnerabilities in HTTP client libraries

### 3. Resource Management

- Implement proper test cleanup procedures
- Monitor memory usage with `@TestPerformanceExtension`
- Use appropriate JVM settings for test execution

### 4. Test Environment Isolation

- Use test-specific profiles and configurations
- Implement proper port management for parallel execution
- Ensure clean state between test runs

## Quick Reference Checklist

When encountering TestRestTemplate issues:

- [ ] Check HttpComponents dependencies are properly configured
- [ ] Verify `UnifiedTestHttpClientConfiguration` is imported
- [ ] Ensure test extends `BaseIntegrationTest`
- [ ] Check for conflicting HTTP client configurations
- [ ] Verify adequate memory allocation for tests
- [ ] Confirm proper test profile activation
- [ ] Check for port conflicts in parallel execution
- [ ] Validate SSL/TLS configuration for test environment
- [ ] Review test cleanup and resource management
- [ ] Check application startup and readiness

## Getting Help

If issues persist after following this troubleshooting guide:

1. **Check Test Performance Reports**: Review generated performance reports for insights
2. **Enable Debug Logging**: Add debug logging for HTTP client and Spring context
3. **Isolate the Problem**: Create minimal test case to reproduce the issue
4. **Review Recent Changes**: Check for recent dependency or configuration changes
5. **Consult Team**: Reach out to team members familiar with the test infrastructure

## Related Documentation

- [HTTP Client Configuration Guide](http-client-configuration-guide.md)
- [Test Execution and Maintenance Guide](test-execution-maintenance-guide.md)
- [Performance Standards](../performance-standards.md)
- [Development Standards](../development-standards.md)
