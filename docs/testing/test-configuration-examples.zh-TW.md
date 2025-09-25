# Test Configuration Examples (繁體中文版)

> **注意**: 此文件需要翻譯。原始英文版本請參考對應的英文文件。

# Test Configuration Examples

## Overview

This document provides practical examples of proper test configuration usage with the unified HTTP client strategy. These examples demonstrate best practices for different types of integration tests.

## Basic Integration Test Examples

### 1. Simple Health Check Test

```java
package solid.humank.genaidemo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import solid.humank.genaidemo.BaseIntegrationTest;
import solid.humank.genaidemo.infrastructure.config.TestPerformanceExtension;

import static org.assertj.core.api.Assertions.assertThat;

@TestPerformanceExtension(maxExecutionTimeMs = 5000, maxMemoryIncreaseMB = 50)
class HealthCheckIntegrationTest extends BaseIntegrationTest {
    
    @Test
    void should_return_healthy_status_when_application_is_running() {
        // Given
        String healthEndpoint = baseUrl + "/actuator/health";
        
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(healthEndpoint, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }
    
    @Test
    void should_provide_detailed_health_information() {
        // Given
        String healthEndpoint = baseUrl + "/actuator/health";
        
        // When
        ResponseEntity<HealthResponse> response = restTemplate.getForEntity(
            healthEndpoint, HealthResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo("UP");
        assertThat(response.getBody().getComponents()).isNotEmpty();
    }
    
    // Health response DTO for structured validation
    public static class HealthResponse {
        private String status;
        private Map<String, Object> components;
        
        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Map<String, Object> getComponents() { return components; }
        public void setComponents(Map<String, Object> components) { this.components = components; }
    }
}
```

### 2. API Endpoint Validation Test

```java
package solid.humank.genaidemo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import solid.humank.genaidemo.BaseIntegrationTest;
import solid.humank.genaidemo.infrastructure.config.TestPerformanceExtension;

import static org.assertj.core.api.Assertions.assertThat;

@TestPerformanceExtension(maxExecutionTimeMs = 10000, maxMemoryIncreaseMB = 100)
class CustomerApiIntegrationTest extends BaseIntegrationTest {
    
    @Test
    void should_create_customer_successfully() {
        // Given
        String createCustomerEndpoint = baseUrl + "/../api/v1/customers";
        
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
            createCustomerEndpoint, entity, CustomerResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("John Doe");
        assertThat(response.getBody().getEmail()).isEqualTo("john.doe@example.com");
        assertThat(response.getBody().getId()).isNotNull();
    }
    
    @Test
    void should_retrieve_customer_by_id() {
        // Given - Create a customer first
        String customerId = createTestCustomer();
        String getCustomerEndpoint = baseUrl + "/../api/v1/customers/" + customerId;
        
        // When
        ResponseEntity<CustomerResponse> response = restTemplate.getForEntity(
            getCustomerEndpoint, CustomerResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(customerId);
    }
    
    @Test
    void should_return_not_found_for_non_existent_customer() {
        // Given
        String nonExistentId = "non-existent-id";
        String getCustomerEndpoint = baseUrl + "/../api/v1/customers/" + nonExistentId;
        
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            getCustomerEndpoint, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    
    private String createTestCustomer() {
        CreateCustomerRequest request = new CreateCustomerRequest(
            "Test Customer",
            "test@example.com",
            "456 Test Ave"
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateCustomerRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<CustomerResponse> response = restTemplate.postForEntity(
            baseUrl + "/../api/v1/customers", entity, CustomerResponse.class);
        
        return response.getBody().getId();
    }
    
    // DTOs for request/response
    public static class CreateCustomerRequest {
        private String name;
        private String email;
        private String address;
        
        public CreateCustomerRequest(String name, String email, String address) {
            this.name = name;
            this.email = email;
            this.address = address;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
    }
    
    public static class CustomerResponse {
        private String id;
        private String name;
        private String email;
        private String address;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
    }
}
```

## Advanced Integration Test Examples

### 3. Observability Features Test

```java
package solid.humank.genaidemo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import solid.humank.genaidemo.BaseIntegrationTest;
import solid.humank.genaidemo.infrastructure.config.TestPerformanceExtension;

import static org.assertj.core.api.Assertions.assertThat;

@TestPerformanceExtension(maxExecutionTimeMs = 15000, maxMemoryIncreaseMB = 150)
class ObservabilityIntegrationTest extends BaseIntegrationTest {
    
    @Test
    void should_expose_prometheus_metrics_endpoint() {
        // Given
        String metricsEndpoint = baseUrl + "/actuator/prometheus";
        
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(metricsEndpoint, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("# HELP");
        assertThat(response.getBody()).contains("# TYPE");
        assertThat(response.getBody()).contains("jvm_memory_used_bytes");
    }
    
    @Test
    void should_provide_application_info() {
        // Given
        String infoEndpoint = baseUrl + "/actuator/info";
        
        // When
        ResponseEntity<InfoResponse> response = restTemplate.getForEntity(
            infoEndpoint, InfoResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }
    
    @Test
    void should_validate_structured_logging() {
        // Given
        String testEndpoint = baseUrl + "/../api/v1/test/logging";
        
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(testEndpoint, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Verify that structured logging is working (check logs in test output)
    }
    
    public static class InfoResponse {
        private Map<String, Object> build;
        private Map<String, Object> git;
        
        // Getters and setters
        public Map<String, Object> getBuild() { return build; }
        public void setBuild(Map<String, Object> build) { this.build = build; }
        public Map<String, Object> getGit() { return git; }
        public void setGit(Map<String, Object> git) { this.git = git; }
    }
}
```

### 4. Concurrent Request Test

```java
package solid.humank.genaidemo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import solid.humank.genaidemo.BaseIntegrationTest;
import solid.humank.genaidemo.infrastructure.config.TestPerformanceExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@TestPerformanceExtension(maxExecutionTimeMs = 30000, maxMemoryIncreaseMB = 200)
class ConcurrentRequestIntegrationTest extends BaseIntegrationTest {
    
    @Test
    void should_handle_concurrent_health_check_requests() throws Exception {
        // Given
        int numberOfRequests = 10;
        String healthEndpoint = baseUrl + "/actuator/health";
        ExecutorService executor = Executors.newFixedThreadPool(numberOfRequests);
        
        // When
        List<CompletableFuture<ResponseEntity<String>>> futures = new ArrayList<>();
        
        for (int i = 0; i < numberOfRequests; i++) {
            CompletableFuture<ResponseEntity<String>> future = CompletableFuture.supplyAsync(() -> 
                restTemplate.getForEntity(healthEndpoint, String.class), executor);
            futures.add(future);
        }
        
        // Wait for all requests to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));
        allFutures.get(30, TimeUnit.SECONDS);
        
        // Then
        for (CompletableFuture<ResponseEntity<String>> future : futures) {
            ResponseEntity<String> response = future.get();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("UP");
        }
        
        executor.shutdown();
        
        // Verify memory usage is still acceptable after concurrent requests
        assertThat(isMemoryUsageAcceptable()).isTrue();
    }
    
    @Test
    void should_maintain_performance_under_load() throws Exception {
        // Given
        int numberOfRequests = 50;
        String endpoint = baseUrl + "/actuator/health";
        List<Long> responseTimes = new ArrayList<>();
        
        // When
        for (int i = 0; i < numberOfRequests; i++) {
            long startTime = System.currentTimeMillis();
            
            ResponseEntity<String> response = restTemplate.getForEntity(endpoint, String.class);
            
            long responseTime = System.currentTimeMillis() - startTime;
            responseTimes.add(responseTime);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
        
        // Then - Analyze performance
        double averageResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        long maxResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0L);
        
        // Performance assertions
        assertThat(averageResponseTime).isLessThan(1000); // Average < 1 second
        assertThat(maxResponseTime).isLessThan(5000);     // Max < 5 seconds
        
        // Cleanup resources if needed
        if (!isMemoryUsageAcceptable()) {
            forceResourceCleanup();
        }
    }
}
```

## Custom Configuration Examples

### 5. Custom Timeout Configuration

```java
package solid.humank.genaidemo.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import solid.humank.genaidemo.BaseIntegrationTest;

import java.time.Duration;

@SpringJUnitConfig
class CustomTimeoutIntegrationTest extends BaseIntegrationTest {
    
    @TestConfiguration
    static class CustomTimeoutConfiguration {
        
        @Bean
        @Primary
        public ClientHttpRequestFactory customTimeoutRequestFactory() {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofSeconds(20)); // Longer connect timeout
            factory.setReadTimeout(Duration.ofSeconds(60));    // Longer read timeout
            return factory;
        }
    }
    
    @Test
    void should_handle_slow_endpoint_with_custom_timeout() {
        // Given
        String slowEndpoint = baseUrl + "/../api/v1/slow-operation";
        
        // When - This endpoint might take up to 45 seconds
        ResponseEntity<String> response = restTemplate.getForEntity(slowEndpoint, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

### 6. Authentication Test Configuration

```java
package solid.humank.genaidemo.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import solid.humank.genaidemo.BaseIntegrationTest;
import solid.humank.genaidemo.infrastructure.config.TestPerformanceExtension;

import static org.assertj.core.api.Assertions.assertThat;

@TestPerformanceExtension(maxExecutionTimeMs = 10000, maxMemoryIncreaseMB = 100)
class AuthenticatedEndpointIntegrationTest extends BaseIntegrationTest {
    
    private String authToken;
    
    @BeforeEach
    void authenticateUser() {
        // Obtain authentication token for tests
        authToken = obtainAuthToken();
    }
    
    @Test
    void should_access_protected_endpoint_with_valid_token() {
        // Given
        String protectedEndpoint = baseUrl + "/../api/v1/protected/resource";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        // When
        ResponseEntity<String> response = restTemplate.exchange(
            protectedEndpoint, HttpMethod.GET, entity, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    
    @Test
    void should_reject_access_without_token() {
        // Given
        String protectedEndpoint = baseUrl + "/../api/v1/protected/resource";
        
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            protectedEndpoint, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    private String obtainAuthToken() {
        // Implementation to obtain auth token for testing
        LoginRequest loginRequest = new LoginRequest("testuser", "testpass");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(loginRequest, headers);
        
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
            baseUrl + "/../api/v1/auth/login", entity, LoginResponse.class);
        
        return response.getBody().getToken();
    }
    
    public static class LoginRequest {
        private String username;
        private String password;
        
        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
        
        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    public static class LoginResponse {
        private String token;
        
        // Getters and setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}
```

## Error Handling Examples

### 7. Error Response Validation

```java
package solid.humank.genaidemo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import solid.humank.genaidemo.BaseIntegrationTest;
import solid.humank.genaidemo.infrastructure.config.TestPerformanceExtension;

import static org.assertj.core.api.Assertions.assertThat;

@TestPerformanceExtension(maxExecutionTimeMs = 8000, maxMemoryIncreaseMB = 80)
class ErrorHandlingIntegrationTest extends BaseIntegrationTest {
    
    @Test
    void should_return_validation_error_for_invalid_request() {
        // Given
        String createCustomerEndpoint = baseUrl + "/../api/v1/customers";
        
        // Invalid request - missing required fields
        InvalidCustomerRequest request = new InvalidCustomerRequest("", "", "");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<InvalidCustomerRequest> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            createCustomerEndpoint, entity, ErrorResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getFieldErrors()).isNotEmpty();
    }
    
    @Test
    void should_handle_internal_server_error_gracefully() {
        // Given
        String errorEndpoint = baseUrl + "/../api/v1/test/error";
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
            errorEndpoint, ErrorResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isNotNull();
        assertThat(response.getBody().getMessage()).isNotNull();
    }
    
    public static class InvalidCustomerRequest {
        private String name;
        private String email;
        private String address;
        
        public InvalidCustomerRequest(String name, String email, String address) {
            this.name = name;
            this.email = email;
            this.address = address;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
    }
    
    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private List<FieldError> fieldErrors;
        
        // Getters and setters
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public List<FieldError> getFieldErrors() { return fieldErrors; }
        public void setFieldErrors(List<FieldError> fieldErrors) { this.fieldErrors = fieldErrors; }
    }
    
    public static class FieldError {
        private String field;
        private String message;
        
        // Getters and setters
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
```

## Best Practices Summary

### Configuration Best Practices

1. **Always extend BaseIntegrationTest**:

```java
class MyTest extends BaseIntegrationTest {
    // Inherits proper HTTP client configuration
}
```

2. **Use @TestPerformanceExtension for monitoring**:

```java
@TestPerformanceExtension(maxExecutionTimeMs = 10000, maxMemoryIncreaseMB = 100)
class MyTest extends BaseIntegrationTest {
    // Automatic performance monitoring
}
```

3. **Import UnifiedTestHttpClientConfiguration when needed**:

```java
@Import(UnifiedTestHttpClientConfiguration.class)
class MyTest extends BaseIntegrationTest {
    // Explicit HTTP client configuration import
}
```

### HTTP Client Usage Best Practices

1. **Use injected TestRestTemplate**:

```java
// ✅ Good
ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

// ❌ Bad
TestRestTemplate customTemplate = new TestRestTemplate();
```

2. **Implement proper error handling**:

```java
try {
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
} catch (ResourceAccessException e) {
    // Handle connection issues
    fail("Connection failed: " + e.getMessage());
}
```

3. **Use structured assertions**:

```java
// ✅ Good - Structured validation
assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
assertThat(response.getBody()).isNotNull();
assertThat(response.getBody().getId()).isNotNull();

// ❌ Bad - Weak validation
assertThat(response).isNotNull();
```

### Performance Best Practices

1. **Monitor resource usage**:

```java
@AfterEach
void checkResourceUsage() {
    if (!isMemoryUsageAcceptable()) {
        forceResourceCleanup();
    }
}
```

2. **Set appropriate performance thresholds**:

```java
// For simple tests
@TestPerformanceExtension(maxExecutionTimeMs = 5000, maxMemoryIncreaseMB = 50)

// For complex tests
@TestPerformanceExtension(maxExecutionTimeMs = 30000, maxMemoryIncreaseMB = 200)
```

3. **Clean up resources properly**:

```java
@AfterEach
void cleanup() {
    // Clean up test data
    // Close connections
    // Reset state
}
```

These examples provide a comprehensive guide for implementing integration tests with the unified HTTP client configuration strategy, ensuring reliable and maintainable test code.


---
*此文件由自動翻譯系統生成，可能需要人工校對。*
