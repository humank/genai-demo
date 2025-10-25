# Fault Tolerance Patterns

> **Last Updated**: 2025-10-24  
> **Status**: Active  
> **Owner**: Development & Architecture Team

## Overview

This document describes the fault tolerance patterns implemented in the Enterprise E-Commerce Platform to handle failures gracefully and prevent cascading failures. These patterns are mandatory for all external service integrations and critical internal service communications.

## Pattern Catalog

### 1. Circuit Breaker Pattern

#### Purpose
Prevent cascading failures by stopping requests to a failing service and allowing it time to recover.

#### When to Use
- External service integrations (payment gateway, email service, SMS service)
- Internal service calls that may fail or timeout
- Any dependency that could cause cascading failures

#### Implementation

```java
@Service
public class PaymentService {
    
    private final PaymentGatewayClient paymentGateway;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "paymentFallback")
    @Retry(name = "paymentGateway")
    public PaymentResult processPayment(PaymentRequest request) {
        return paymentGateway.charge(request);
    }
    
    private PaymentResult paymentFallback(PaymentRequest request, Exception ex) {
        // Queue payment for later processing
        paymentQueue.enqueue(request);
        
        return PaymentResult.pending()
            .withMessage("Payment is being processed. You will receive confirmation shortly.")
            .withOrderId(request.getOrderId());
    }
}
```

#### Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentGateway:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 30s
        failureRateThreshold: 50
        slowCallRateThreshold: 50
        slowCallDurationThreshold: 5s
```

#### States

```
┌─────────────┐
│   CLOSED    │ ◄─── Normal operation
│  (Healthy)  │      All requests pass through
└──────┬──────┘
       │ Failure rate > threshold
       ▼
┌─────────────┐
│    OPEN     │ ◄─── Service is failing
│  (Failing)  │      All requests fail fast
└──────┬──────┘
       │ After wait duration
       ▼
┌─────────────┐
│ HALF_OPEN   │ ◄─── Testing recovery
│  (Testing)  │      Limited requests allowed
└──────┬──────┘
       │
       ├─── Success → CLOSED
       └─── Failure → OPEN
```

#### Monitoring

```java
@Component
public class CircuitBreakerMetrics {
    
    @EventListener
    public void onCircuitBreakerEvent(CircuitBreakerOnStateTransitionEvent event) {
        logger.warn("Circuit breaker state transition: {} -> {}",
            event.getStateTransition().getFromState(),
            event.getStateTransition().getToState());
        
        meterRegistry.counter("circuit.breaker.state.transition",
            "name", event.getCircuitBreakerName(),
            "from", event.getStateTransition().getFromState().name(),
            "to", event.getStateTransition().getToState().name()
        ).increment();
    }
}
```

---

### 2. Retry Pattern

#### Purpose
Automatically retry failed operations with exponential backoff to handle transient failures.

#### When to Use
- Network timeouts
- Temporary service unavailability
- Database connection failures
- Rate limiting responses

#### Implementation

```java
@Service
public class InventoryService {
    
    @Retry(
        name = "inventoryService",
        fallbackMethod = "reserveInventoryFallback"
    )
    public ReservationResult reserveInventory(ReservationRequest request) {
        return inventoryRepository.reserve(request);
    }
    
    private ReservationResult reserveInventoryFallback(
        ReservationRequest request, 
        Exception ex
    ) {
        logger.error("Failed to reserve inventory after retries", ex);
        
        // Return pessimistic result
        return ReservationResult.unavailable()
            .withMessage("Inventory check temporarily unavailable. Order will be verified shortly.");
    }
}
```

#### Configuration

```yaml
resilience4j:
  retry:
    instances:
      inventoryService:
        maxAttempts: 3
        waitDuration: 1s
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.net.SocketTimeoutException
          - org.springframework.web.client.ResourceAccessException
        ignoreExceptions:
          - solid.humank.genaidemo.domain.BusinessRuleViolationException
```

#### Retry Strategy

```
Attempt 1: Immediate
Attempt 2: Wait 1s  (1s × 2^0)
Attempt 3: Wait 2s  (1s × 2^1)
Attempt 4: Wait 4s  (1s × 2^2)
```

#### Best Practices

- **Idempotency**: Ensure operations are idempotent
- **Max Attempts**: Limit to 3-5 attempts
- **Exponential Backoff**: Use exponential backoff to avoid overwhelming the service
- **Jitter**: Add random jitter to prevent thundering herd
- **Timeout**: Set appropriate timeouts for each attempt

```java
@Component
public class RetryConfiguration {
    
    @Bean
    public RetryConfig customRetryConfig() {
        return RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofSeconds(1))
            .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(
                Duration.ofSeconds(1),
                2.0,
                Duration.ofSeconds(10)
            ))
            .build();
    }
}
```

---

### 3. Fallback Pattern

#### Purpose
Provide alternative responses when primary operations fail.

#### When to Use
- Non-critical features that can degrade gracefully
- Services with cached alternatives
- Operations with default values

#### Implementation Strategies

##### Strategy 1: Cached Response

```java
@Service
public class ProductService {
    
    @Cacheable("products")
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFromCache")
    public Product getProduct(String productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
    }
    
    private Product getProductFromCache(String productId, Exception ex) {
        logger.warn("Falling back to cache for product: {}", productId);
        
        return cacheManager.getCache("products")
            .get(productId, Product.class);
    }
}
```

##### Strategy 2: Default Value

```java
@Service
public class RecommendationService {
    
    @CircuitBreaker(name = "recommendations", fallbackMethod = "getDefaultRecommendations")
    public List<Product> getRecommendations(String customerId) {
        return recommendationEngine.getPersonalizedRecommendations(customerId);
    }
    
    private List<Product> getDefaultRecommendations(String customerId, Exception ex) {
        logger.warn("Using default recommendations for customer: {}", customerId);
        
        // Return popular products as fallback
        return productService.getPopularProducts(10);
    }
}
```

##### Strategy 3: Degraded Functionality

```java
@Service
public class OrderService {
    
    @CircuitBreaker(name = "orderService", fallbackMethod = "createOrderWithoutInventoryCheck")
    public Order createOrder(CreateOrderCommand command) {
        // Full validation including real-time inventory check
        inventoryService.validateAvailability(command.getItems());
        
        return orderRepository.save(Order.create(command));
    }
    
    private Order createOrderWithoutInventoryCheck(CreateOrderCommand command, Exception ex) {
        logger.warn("Creating order without real-time inventory check");
        
        // Create order with pending inventory verification
        Order order = Order.createPending(command);
        order.markForInventoryVerification();
        
        return orderRepository.save(order);
    }
}
```

---

### 4. Bulkhead Pattern

#### Purpose
Isolate resources to prevent failures in one area from affecting others.

#### When to Use
- Thread pool isolation for different services
- Connection pool separation
- Resource quota management

#### Implementation

```java
@Configuration
public class BulkheadConfiguration {
    
    @Bean
    public ThreadPoolBulkhead paymentBulkhead() {
        return ThreadPoolBulkhead.of("payment",
            ThreadPoolBulkheadConfig.custom()
                .maxThreadPoolSize(10)
                .coreThreadPoolSize(5)
                .queueCapacity(20)
                .build()
        );
    }
    
    @Bean
    public ThreadPoolBulkhead inventoryBulkhead() {
        return ThreadPoolBulkhead.of("inventory",
            ThreadPoolBulkheadConfig.custom()
                .maxThreadPoolSize(15)
                .coreThreadPoolSize(8)
                .queueCapacity(30)
                .build()
        );
    }
}
```

```java
@Service
public class OrderProcessingService {
    
    @Bulkhead(name = "payment", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<PaymentResult> processPaymentAsync(PaymentRequest request) {
        return CompletableFuture.supplyAsync(() -> 
            paymentService.processPayment(request)
        );
    }
    
    @Bulkhead(name = "inventory", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<ReservationResult> reserveInventoryAsync(ReservationRequest request) {
        return CompletableFuture.supplyAsync(() ->
            inventoryService.reserveInventory(request)
        );
    }
}
```

#### Resource Isolation

```
┌─────────────────────────────────────────┐
│         Application Resources           │
├─────────────────────────────────────────┤
│  Payment Pool    │  Inventory Pool      │
│  (10 threads)    │  (15 threads)        │
│  ┌────┐ ┌────┐   │  ┌────┐ ┌────┐      │
│  │ T1 │ │ T2 │   │  │ T1 │ │ T2 │      │
│  └────┘ └────┘   │  └────┘ └────┘      │
├──────────────────┼──────────────────────┤
│  Email Pool      │  Notification Pool   │
│  (5 threads)     │  (8 threads)         │
│  ┌────┐ ┌────┐   │  ┌────┐ ┌────┐      │
│  │ T1 │ │ T2 │   │  │ T1 │ │ T2 │      │
│  └────┘ └────┘   │  └────┘ └────┘      │
└─────────────────────────────────────────┘
```

---

### 5. Timeout Pattern

#### Purpose
Prevent indefinite waiting for responses and free up resources.

#### When to Use
- All external service calls
- Database queries
- Network operations

#### Implementation

```java
@Service
public class ExternalServiceClient {
    
    @TimeLimiter(name = "externalService")
    public CompletableFuture<Response> callExternalService(Request request) {
        return CompletableFuture.supplyAsync(() ->
            restTemplate.postForObject(serviceUrl, request, Response.class)
        );
    }
}
```

#### Configuration

```yaml
resilience4j:
  timelimiter:
    instances:
      externalService:
        timeoutDuration: 5s
        cancelRunningFuture: true
```

#### Timeout Hierarchy

```
┌─────────────────────────────────────────┐
│  HTTP Client Timeout: 30s               │
│  ┌───────────────────────────────────┐  │
│  │  Service Call Timeout: 10s        │  │
│  │  ┌─────────────────────────────┐  │  │
│  │  │  Database Query Timeout: 5s │  │  │
│  │  └─────────────────────────────┘  │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

---

### 6. Health Check Pattern

#### Purpose
Continuously monitor service health and remove unhealthy instances from load balancing.

#### Implementation

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    private final RedisTemplate<String, String> redisTemplate;
    
    @Override
    public Health health() {
        try {
            // Check database connectivity
            checkDatabaseHealth();
            
            // Check cache connectivity
            checkCacheHealth();
            
            // Check external dependencies
            checkExternalDependencies();
            
            return Health.up()
                .withDetail("database", "UP")
                .withDetail("cache", "UP")
                .withDetail("external", "UP")
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
    
    private void checkDatabaseHealth() {
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("SELECT 1");
        } catch (SQLException e) {
            throw new HealthCheckException("Database unhealthy", e);
        }
    }
}
```

#### Kubernetes Health Checks

```yaml
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: order-service
    livenessProbe:
      httpGet:
        path: /actuator/health/liveness
        port: 8080
      initialDelaySeconds: 30
      periodSeconds: 10
      timeoutSeconds: 5
      failureThreshold: 3
    
    readinessProbe:
      httpGet:
        path: /actuator/health/readiness
        port: 8080
      initialDelaySeconds: 10
      periodSeconds: 5
      timeoutSeconds: 3
      failureThreshold: 3
```

---

## Pattern Combination

### Recommended Combinations

#### External Service Integration
```
Circuit Breaker + Retry + Timeout + Fallback
```

#### Database Operations
```
Retry + Timeout + Connection Pool (Bulkhead)
```

#### Async Processing
```
Bulkhead + Timeout + Retry
```

### Example: Complete Fault Tolerance Stack

```java
@Service
public class ResilientOrderService {
    
    @CircuitBreaker(name = "orderService", fallbackMethod = "createOrderFallback")
    @Retry(name = "orderService")
    @TimeLimiter(name = "orderService")
    @Bulkhead(name = "orderService", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<Order> createOrderAsync(CreateOrderCommand command) {
        return CompletableFuture.supplyAsync(() -> {
            // Validate inventory
            inventoryService.validateAvailability(command.getItems());
            
            // Process payment
            paymentService.processPayment(command.getPaymentInfo());
            
            // Create order
            return orderRepository.save(Order.create(command));
        });
    }
    
    private CompletableFuture<Order> createOrderFallback(
        CreateOrderCommand command,
        Exception ex
    ) {
        logger.error("Order creation failed, using fallback", ex);
        
        // Create pending order for manual review
        Order pendingOrder = Order.createPending(command);
        pendingOrder.markForManualReview();
        
        return CompletableFuture.completedFuture(
            orderRepository.save(pendingOrder)
        );
    }
}
```

---

## Monitoring and Metrics

### Key Metrics

```java
@Component
public class FaultToleranceMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordCircuitBreakerState(String name, String state) {
        meterRegistry.gauge("circuit.breaker.state",
            Tags.of("name", name, "state", state), 1.0);
    }
    
    public void recordRetryAttempt(String name, int attempt) {
        meterRegistry.counter("retry.attempts",
            Tags.of("name", name, "attempt", String.valueOf(attempt)))
            .increment();
    }
    
    public void recordFallbackExecution(String name) {
        meterRegistry.counter("fallback.executions",
            Tags.of("name", name))
            .increment();
    }
}
```

### Alerting Rules

```yaml
# Prometheus Alert Rules
groups:
  - name: fault_tolerance
    rules:
      - alert: CircuitBreakerOpen
        expr: circuit_breaker_state{state="open"} == 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Circuit breaker {{ $labels.name }} is open"
          
      - alert: HighRetryRate
        expr: rate(retry_attempts_total[5m]) > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High retry rate for {{ $labels.name }}"
          
      - alert: FrequentFallbacks
        expr: rate(fallback_executions_total[5m]) > 5
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Frequent fallback executions for {{ $labels.name }}"
```

---

## Testing

### Chaos Engineering

```java
@Profile("chaos")
@Component
public class ChaosMonkey {
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void injectRandomFailure() {
        double random = Math.random();
        
        if (random < 0.1) {
            // 10% chance: Simulate slow response
            simulateLatency();
        } else if (random < 0.15) {
            // 5% chance: Simulate service failure
            simulateServiceFailure();
        }
    }
    
    private void simulateLatency() {
        logger.warn("Chaos: Injecting latency");
        Thread.sleep(5000);
    }
    
    private void simulateServiceFailure() {
        logger.warn("Chaos: Simulating service failure");
        throw new ServiceUnavailableException("Chaos monkey strike!");
    }
}
```

### Integration Tests

```java
@SpringBootTest
class FaultToleranceTest {
    
    @Test
    void should_open_circuit_breaker_after_failures() {
        // Simulate 5 consecutive failures
        for (int i = 0; i < 5; i++) {
            assertThrows(Exception.class, () ->
                paymentService.processPayment(request)
            );
        }
        
        // Verify circuit breaker is open
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("paymentGateway");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
    
    @Test
    void should_use_fallback_when_service_fails() {
        // Simulate service failure
        when(externalService.call()).thenThrow(new ServiceException());
        
        // Verify fallback is used
        Result result = resilientService.callWithFallback();
        assertThat(result.isFromFallback()).isTrue();
    }
}
```

---

**Related Documents**:
- [Overview](overview.md) - Availability perspective introduction
- [Requirements](requirements.md) - Availability targets and scenarios
- [High Availability](high-availability.md) - Infrastructure design
- [Disaster Recovery](disaster-recovery.md) - DR procedures
