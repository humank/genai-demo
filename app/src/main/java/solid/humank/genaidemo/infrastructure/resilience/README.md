# Application Resilience Patterns

This package provides comprehensive resilience patterns for disaster recovery and business continuity.

## 📋 Overview

The resilience infrastructure implements the following patterns:
- **Circuit Breaker**: Prevents cascading failures
- **Retry**: Handles transient failures with exponential backoff
- **Time Limiter**: Prevents long-running operations
- **Fallback**: Provides degraded functionality
- **Bulkhead**: Isolates resources
- **Rate Limiter**: Controls request rates
- **Business Continuity Metrics**: Tracks RTO/RPO and business transactions

## 🚀 Quick Start

### Example Service

See `ExampleResilientService` for comprehensive examples of all resilience patterns:
- Circuit Breaker with Fallback
- Retry with Exponential Backoff
- Combined Patterns (Circuit Breaker + Retry + Time Limiter)
- Circuit Breaker with Caching
- Using ResilientServiceWrapper Directly
- Critical Service with Strict Circuit Breaker
- Lenient Service with Relaxed Circuit Breaker

### 1. Using Annotations (Recommended)

```java
@Service
public class MyService {
    
    @CircuitBreaker(name = "myService", fallbackMethod = "fallbackMethod")
    @Retry(name = "database")
    @Timed(value = "myService.operation")
    public String performOperation() {
        // Your business logic
        return "result";
    }
    
    private String fallbackMethod(Exception e) {
        return "fallback-result";
    }
}
```

### 2. Using ResilientServiceWrapper

```java
@Service
public class MyService {
    
    private final ResilientServiceWrapper resilientWrapper;
    
    public String performOperation() {
        return resilientWrapper.executeWithResilience(
            "myService",
            () -> {
                // Your business logic
                return "result";
            },
            () -> "fallback-result"
        );
    }
}
```

## 🔧 Configuration

### Circuit Breaker Configuration

Located in `application-resilience.yml`:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      myService:
        failure-rate-threshold: 50        # Open circuit at 50% failure
        slow-call-rate-threshold: 50      # Open circuit at 50% slow calls
        slow-call-duration-threshold: 2s  # Call is slow if > 2s
        wait-duration-in-open-state: 30s  # Wait 30s before half-open
        minimum-number-of-calls: 10       # Need 10 calls before calculation
```

### Retry Configuration

```yaml
resilience4j:
  retry:
    instances:
      myService:
        max-attempts: 3                   # Maximum 3 attempts
        wait-duration: 500ms              # Initial wait
        exponential-backoff-multiplier: 2 # Exponential backoff
```

## 📊 Monitoring

### Circuit Breaker Metrics

```java
// Get circuit breaker state
String state = resilientWrapper.getCircuitBreakerState("myService");

// Get circuit breaker metrics
CircuitBreakerMetrics metrics = resilientWrapper.getCircuitBreakerMetrics("myService");
System.out.println("Failure Rate: " + metrics.failureRate());
System.out.println("Successful Calls: " + metrics.successfulCalls());
System.out.println("Failed Calls: " + metrics.failedCalls());
```

### Business Continuity Metrics

```java
@Service
public class MyService {
    
    private final RecoveryMetricsTracker recoveryTracker;
    private final BusinessTransactionMetricsTracker transactionTracker;
    
    public void handleIncident() {
        // Record incident start
        recoveryTracker.recordIncidentStart("incident-123", "database-failure");
        
        // Perform recovery
        performRecovery();
        
        // Record successful recovery
        recoveryTracker.recordRecoverySuccess("incident-123", "database-failure");
    }
    
    public void performTransaction() {
        Instant start = Instant.now();
        boolean success = false;
        
        try {
            // Business logic
            success = true;
        } finally {
            Duration duration = Duration.between(start, Instant.now());
            transactionTracker.recordTransaction("order.create", success, duration);
        }
    }
}
```

## 🎯 Use Cases

### 1. Database Operations

```java
@Service
public class CustomerService {
    
    @CircuitBreaker(name = "database", fallbackMethod = "findByIdFallback")
    @Retry(name = "database")
    @Cacheable("customers")
    public Optional<Customer> findById(String id) {
        return customerRepository.findById(id);
    }
    
    private Optional<Customer> findByIdFallback(String id, Exception e) {
        logger.warn("Using fallback for customer: {}", id);
        return Optional.empty();
    }
}
```

### 2. External API Calls

```java
@Service
public class PaymentService {
    
    @CircuitBreaker(name = "paymentService", fallbackMethod = "processPaymentFallback")
    @Retry(name = "externalApi")
    @TimeLimiter(name = "externalApi")
    public PaymentResult processPayment(PaymentRequest request) {
        return paymentGateway.process(request);
    }
    
    private PaymentResult processPaymentFallback(PaymentRequest request, Exception e) {
        // Queue for later processing
        paymentQueue.add(request);
        return PaymentResult.pending();
    }
}
```

### 3. Async Operations with Timeout

```java
@Service
public class ReportService {
    
    public CompletableFuture<Report> generateReport(String reportId) {
        return resilientWrapper.executeWithTimeout(
            "reportService",
            () -> CompletableFuture.supplyAsync(() -> {
                // Long-running report generation
                return reportGenerator.generate(reportId);
            }),
            Duration.ofSeconds(30)
        );
    }
}
```

## 📈 Available Metrics

### Resilience Metrics

- `resilience.operation.success` - Successful operations count
- `resilience.operation.failure` - Failed operations count
- `resilience.operation.duration` - Operation duration
- `resilience.fallback.success` - Successful fallback executions
- `resilience.fallback.failure` - Failed fallback executions

### Business Continuity Metrics

- `business.continuity.rto.target.seconds` - Target RTO (120s)
- `business.continuity.rpo.target.seconds` - Target RPO (1s)
- `business.continuity.rto.actual` - Actual recovery time
- `business.continuity.rpo.actual` - Actual data loss time
- `business.continuity.incidents.total` - Total incidents
- `business.continuity.recoveries.successful` - Successful recoveries
- `business.continuity.recovery.success.rate` - Recovery success rate

### Business Transaction Metrics

- `business.transactions.total` - Total transactions
- `business.transactions.success` - Successful transactions
- `business.transactions.failure` - Failed transactions
- `business.transactions.duration` - Transaction duration
- `business.value.*` - Business value metrics (revenue, orders, etc.)

## 🔍 Health Checks

Circuit breakers and rate limiters are automatically exposed as health indicators:

```bash
# Check application health
curl http://localhost:8080/actuator/health

# Response includes circuit breaker states
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "customerService": "CLOSED",
        "database": "CLOSED",
        "externalApi": "CLOSED"
      }
    }
  }
}
```

## 📊 Prometheus Metrics

All metrics are automatically exported to Prometheus:

```bash
# Access Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Example metrics
resilience4j_circuitbreaker_state{name="customerService",state="closed"} 1.0
resilience4j_circuitbreaker_failure_rate{name="customerService"} 0.0
resilience4j_retry_calls_total{name="database",kind="successful_without_retry"} 100.0
business_continuity_rto_actual_seconds_max{type="database-failure"} 45.0
business_transactions_total{type="order.create",status="success"} 1000.0
```

## 🎨 Best Practices

### 1. Choose Appropriate Configuration

- **Critical Services**: Use `critical` configuration (more aggressive)
- **Non-Critical Services**: Use `lenient` configuration (more tolerant)
- **Default**: Use `default` configuration for most services

### 2. Implement Meaningful Fallbacks

```java
// ✅ Good: Provides degraded functionality
private List<Product> findProductsFallback(Exception e) {
    return cachedProducts.getRecentProducts();
}

// ❌ Bad: Just returns empty
private List<Product> findProductsFallback(Exception e) {
    return Collections.emptyList();
}
```

### 3. Don't Retry Non-Idempotent Operations

```java
// ✅ Good: No retry for create operations
@CircuitBreaker(name = "orderService")
public Order createOrder(OrderRequest request) {
    return orderRepository.save(request);
}

// ❌ Bad: Retry might create duplicates
@Retry(name = "orderService")
public Order createOrder(OrderRequest request) {
    return orderRepository.save(request);
}
```

### 4. Use Caching with Circuit Breaker

```java
@CircuitBreaker(name = "productService", fallbackMethod = "findByIdFallback")
@Cacheable("products")
public Product findById(String id) {
    return productRepository.findById(id);
}

private Product findByIdFallback(String id, Exception e) {
    // Cache will be used if available
    return null;
}
```

### 5. Monitor and Alert

Set up alerts for:
- Circuit breaker state changes
- High failure rates
- RTO/RPO target misses
- Fallback usage spikes

## 🧪 Testing

### Unit Testing

```java
@Test
void should_use_fallback_when_service_fails() {
    // Given
    when(externalService.call()).thenThrow(new RuntimeException());
    
    // When
    String result = myService.performOperation();
    
    // Then
    assertThat(result).isEqualTo("fallback-result");
}
```

### Integration Testing

```java
@SpringBootTest
@TestPerformanceExtension
class ResilientServiceIntegrationTest {
    
    @Test
    void should_recover_from_database_failure() {
        // Simulate database failure
        // Verify circuit breaker opens
        // Verify fallback is used
        // Simulate database recovery
        // Verify circuit breaker closes
    }
}
```

## 📚 References

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Micrometer Documentation](https://micrometer.io/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)

## 🤝 Contributing

When adding new resilience patterns:
1. Add configuration to `application-resilience.yml`
2. Document usage in this README
3. Add unit tests
4. Add integration tests
5. Update metrics documentation
