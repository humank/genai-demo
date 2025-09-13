# Structured Logging Implementation

This document describes the comprehensive structured logging pipeline implemented for the GenAI Demo application, providing observability capabilities for both development and production environments.

## Overview

The structured logging implementation provides:

- **JSON structured logging** for production environments
- **Human-readable logging** for development environments
- **Correlation ID tracking** across all requests
- **MDC (Mapped Diagnostic Context)** for contextual information
- **Automated log forwarding** to OpenSearch Service
- **Log lifecycle management** (CloudWatch → S3 → Glacier)
- **Business context logging** with AOP aspects

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Application   │───▶│   Logback       │───▶│   CloudWatch    │
│   Logs          │    │   (JSON/Text)   │    │   Logs          │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
                                                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   OpenSearch    │◀───│   Lambda        │◀───│   Log Stream    │
│   Service       │    │   Forwarder     │    │   Subscription  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
                                                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   S3 Glacier    │◀───│   S3 Standard   │◀───│   Export Task   │
│   (Long-term)   │    │   (Archive)     │    │   (Daily)       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Components

### 1. Logback Configuration (`logback-spring.xml`)

**Features:**

- Profile-specific logging patterns
- JSON structured logging for production
- Human-readable logging for development
- Async appenders for performance
- Proper MDC context inclusion

**Production JSON Format:**

```json
{
  "timestamp": "2024-01-15T10:30:45.123",
  "level": "INFO",
  "thread": "http-nio-8080-exec-1",
  "logger": "solid.humank.genaidemo.application.order.OrderApplicationService",
  "correlationId": "abc-123-def",
  "traceId": "1-5e1b4e5f-38a7c5c3f5a1b2c3d4e5f6a7",
  "spanId": "38a7c5c3f5a1b2c3",
  "userId": "USER-001",
  "orderId": "ORDER-456",
  "customerId": "CUST-001",
  "application": "genai-demo",
  "version": "2.0.0",
  "environment": "production",
  "message": "Order created successfully",
  "exception": ""
}
```

### 2. Correlation ID Filter

**Purpose:** Ensures every HTTP request has a unique correlation ID for tracing.

**Features:**

- Extracts correlation ID from `X-Correlation-ID` header
- Generates new UUID if not provided
- Sets correlation ID in MDC for all log messages
- Adds correlation ID to response header
- Automatic MDC cleanup to prevent memory leaks

**Usage:**

```java
@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {
    // Automatically applied to all HTTP requests
}
```

### 3. Logging Context Manager

**Purpose:** Provides convenient methods to manage MDC context.

**Features:**

- Set business context (customer ID, order ID, operation)
- Set tracing context (trace ID, span ID)
- Execute code with specific context
- Context cleanup and management

**Usage:**

```java
@Autowired
private LoggingContextManager loggingContextManager;

public void processOrder(String customerId, String orderId) {
    loggingContextManager.setBusinessContext(customerId, orderId, "processOrder");
    
    logger.info("Processing order"); // Includes customer and order context
    
    // Context is automatically included in all log messages
}
```

### 4. Business Logging Aspect

**Purpose:** Automatically adds business context to application and domain service methods.

**Features:**

- AOP-based automatic context setting
- Operation timing and performance logging
- Exception logging with context
- Component identification

**Usage:**

```java
@Aspect
@Component
public class BusinessLoggingAspect {
    @Around("applicationServiceMethods()")
    public Object aroundApplicationService(ProceedingJoinPoint joinPoint) {
        // Automatically sets operation context and logs performance
    }
}
```

## CloudWatch Logs Integration

### Fluent Bit Configuration

The EKS cluster uses Fluent Bit DaemonSet to collect and forward logs:

**Features:**

- Automatic log collection from all pods
- Kubernetes metadata enrichment
- Multi-line log parsing
- CloudWatch Logs forwarding
- Container insights integration

**Log Groups:**

- `/aws/containerinsights/genai-demo-cluster/application` - Application logs
- `/aws/containerinsights/genai-demo-cluster/dataplane` - Infrastructure logs
- `/aws/containerinsights/genai-demo-cluster/host` - Host system logs

### Log Forwarding to OpenSearch

**Lambda Function Features:**

- Real-time log streaming from CloudWatch to OpenSearch
- JSON log parsing and indexing
- Daily index rotation
- Error handling and retry logic

**OpenSearch Configuration:**

- Domain: `genai-demo-logs-{environment}`
- Daily indices: `genai-demo-logs-YYYY-MM-DD`
- VPC-based security
- Encryption at rest and in transit

## Log Lifecycle Management

### Retention Strategy

1. **CloudWatch Logs (Hot Data):** 7 days
   - Fast search and real-time monitoring
   - Cost: ~$0.50/GB/month

2. **S3 Standard (Warm Data):** 7-30 days
   - Automated daily export from CloudWatch
   - Cost: ~$0.023/GB/month

3. **S3 Glacier (Cold Data):** 30-365 days
   - Automatic lifecycle transition
   - Cost: ~$0.004/GB/month

4. **S3 Deep Archive (Archive):** 365+ days
   - Long-term compliance storage
   - Cost: ~$0.00099/GB/month

### Automated Export Process

**Daily Export Lambda:**

- Runs at 2 AM daily via EventBridge
- Exports logs older than 7 days to S3
- Organized by date: `application-logs/YYYY/MM/DD/`
- Maintains CloudWatch for recent data

## Development vs Production

### Development Profile

```yaml
logging:
  level:
    solid.humank.genaidemo: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} [%X{correlationId:-}] - %msg%n"
```

**Features:**

- Human-readable console output
- Debug level logging
- File-based logging for persistence
- H2 console access logging

### Production Profile

```yaml
observability:
  logging:
    structured: true
    mdc:
      correlation-id: true
      trace-id: true
      span-id: true
```

**Features:**

- JSON structured logging
- Async appenders for performance
- CloudWatch integration
- OpenSearch forwarding
- S3 archival

## Usage Examples

### Basic Logging with Context

```java
@Service
public class OrderApplicationService {
    private static final Logger logger = LoggerFactory.getLogger(OrderApplicationService.class);
    private final LoggingContextManager loggingContextManager;
    
    public void createOrder(CreateOrderCommand command) {
        loggingContextManager.setBusinessContext(
            command.customerId(), 
            null, // Order ID not yet available
            "createOrder"
        );
        
        logger.info("Starting order creation for customer: {}", command.customerId());
        
        try {
            Order order = new Order(command.customerId(), command.items());
            
            // Update context with order ID
            loggingContextManager.setOrderId(order.getId().getValue());
            
            orderRepository.save(order);
            
            logger.info("Order created successfully with ID: {}", order.getId().getValue());
            
        } catch (Exception e) {
            logger.error("Failed to create order: {}", e.getMessage(), e);
            throw e;
        }
    }
}
```

### Contextual Execution

```java
public void processPayment(String orderId, Money amount) {
    Map<String, String> context = Map.of(
        "orderId", orderId,
        "operation", "processPayment",
        "component", "payment-service"
    );
    
    loggingContextManager.executeWithContext(context, () -> {
        logger.info("Processing payment of {} for order", amount);
        // All logs within this block include the context
        paymentGateway.charge(amount);
        logger.info("Payment processed successfully");
    });
}
```

### Exception Logging

```java
try {
    riskyOperation();
} catch (BusinessException e) {
    logger.error("Business rule violation: {} - Context: {}", 
                e.getMessage(), 
                loggingContextManager.getCurrentContext(), 
                e);
    throw e;
} catch (Exception e) {
    logger.error("Unexpected error in operation: {} - Context: {}", 
                e.getMessage(), 
                loggingContextManager.getCurrentContext(), 
                e);
    throw new SystemException("Operation failed", e);
}
```

## Monitoring and Alerting

### CloudWatch Alarms

- High error rate (>5% in 5 minutes)
- Log volume spikes
- Missing correlation IDs
- OpenSearch indexing failures

### OpenSearch Dashboards

- Real-time log analysis
- Business metrics from logs
- Error rate monitoring
- Performance analysis

### Cost Optimization

- Estimated monthly cost for 10GB/day: ~$140/month (vs $300 CloudWatch only)
- 53% cost savings through lifecycle management
- Automatic cleanup of old indices

## Testing

### Unit Tests

```java
@Test
void shouldSetBusinessContextInMdc() {
    loggingContextManager.setBusinessContext("CUST-001", "ORDER-123", "createOrder");
    
    assertThat(MDC.get("customerId")).isEqualTo("CUST-001");
    assertThat(MDC.get("orderId")).isEqualTo("ORDER-123");
    assertThat(MDC.get("operation")).isEqualTo("createOrder");
}
```

### Integration Tests

```java
@Test
void shouldIncludeCorrelationIdInLogs() {
    String correlationId = UUID.randomUUID().toString();
    
    mockMvc.perform(get("/api/orders")
            .header("X-Correlation-ID", correlationId))
           .andExpect(status().isOk());
    
    // Verify correlation ID is in response header
    // Verify logs include correlation ID
}
```

## Deployment

### Kubernetes Configuration

```yaml
env:
- name: SPRING_PROFILES_ACTIVE
  value: "prod,k8s"
- name: OBSERVABILITY_LOGGING_STRUCTURED
  value: "true"
- name: OBSERVABILITY_TRACING_ENABLED
  value: "true"
```

### CDK Deployment

```typescript
const observabilityStack = new ObservabilityStack(this, 'Observability', {
  vpc: vpc,
  environment: 'production'
});
```

## Troubleshooting

### Common Issues

1. **Missing Correlation IDs**
   - Check filter order (@Order(1))
   - Verify filter is registered

2. **MDC Memory Leaks**
   - Ensure MDC.clear() in finally blocks
   - Use try-with-resources pattern

3. **JSON Parsing Errors**
   - Validate JSON structure in logs
   - Check for special characters escaping

4. **OpenSearch Connection Issues**
   - Verify VPC security groups
   - Check Lambda execution role permissions

### Performance Considerations

1. **Async Appenders**
   - Use for production environments
   - Configure appropriate queue sizes

2. **Log Sampling**
   - Implement for high-volume applications
   - Use structured sampling strategies

3. **Context Cleanup**
   - Always clear MDC after request processing
   - Use aspects for automatic cleanup

This structured logging implementation provides comprehensive observability while maintaining performance and cost efficiency across development and production environments.
