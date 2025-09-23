# Production Environment Observability Testing Guide

## 📋 Table of Contents

1. [Overview](#overview)
2. [Testing Objectives](#testing-objectives)
3. [Testing Architecture](#testing-architecture)
4. [Core Testing Scenarios](#core-testing-scenarios)
5. [Test Environment Configuration](#test-environment-configuration)
6. [Test Execution Plan](#test-execution-plan)
7. [Acceptance Criteria](#acceptance-criteria)
8. [Troubleshooting](#troubleshooting)
9. [Best Practices](#best-practices)

## Overview

This document provides a comprehensive testing guide for implementing a complete observability system in production environments. Based on the observability testing experience from the GenAI Demo project, it covers comprehensive observability verification including logging, metrics, tracing, and health checks.

### Scope

- **Target Environment**: AWS Production Environment
- **Technology Stack**: Spring Boot 3.x + Java 21 + AWS Observability Services
- **Test Types**: Functional Testing, Performance Testing, Disaster Recovery Testing
- **Test Levels**: Unit Testing, Integration Testing, End-to-End Testing

## Testing Objectives

### 🎯 Primary Objectives

1. **Observability Completeness Verification**
   - Ensure all observability components operate correctly
   - Verify data flow integrity and accuracy
   - Ensure cross-service tracing functionality works properly

2. **Performance and Reliability Verification**
   - Verify observability system stability under high load
   - Ensure observability overhead is within acceptable range (< 5%)
   - Verify system recovery capabilities

3. **Security and Compliance Verification**
   - Ensure sensitive data is properly masked
   - Verify data retention policies
   - Ensure audit log integrity

4. **Cost Optimization Verification**
   - Verify sampling strategy effectiveness
   - Ensure resource usage optimization
   - Verify cost monitoring and alerting

## Testing Architecture

### 🏗️ Observability Technology Stack

```text
┌─────────────────────────────────────────────────────────────┐
│                Production Environment Observability Architecture                │
├─────────────────────────────────────────────────────────────┤
│  Application Layer                                          │
│  ├── Spring Boot Actuator (Health Checks, Metrics)         │
│  ├── Micrometer (Metrics Collection)                       │
│  ├── OpenTelemetry (Distributed Tracing)                   │
│  └── Logback + MDC (Structured Logging)                    │
├─────────────────────────────────────────────────────────────┤
│  AWS Observability Services                                 │
│  ├── CloudWatch (Metrics, Logs, Alarms)                    │
│  ├── X-Ray (Distributed Tracing)                           │
│  ├── CloudWatch Insights (Log Analysis)                    │
│  └── CloudWatch Dashboards (Visualization)                 │
├─────────────────────────────────────────────────────────────┤
│  Third-party Tools (Optional)                              │
│  ├── Prometheus + Grafana                                  │
│  ├── ELK Stack (Elasticsearch, Logstash, Kibana)          │
│  └── Jaeger (Trace Visualization)                          │
└─────────────────────────────────────────────────────────────┘
```

## Core Testing Scenarios

### 🔍 1. Basic Observability Verification

#### 1.1 Structured Logging Tests

**Test Objective**: Verify structured logging and MDC context propagation

**Test Scenarios**:

```java
// Scenario 1: MDC Context Propagation Test
@Test
void shouldPropagateCorrelationIdThroughMDC() {
    // Given: Set correlation ID
    String correlationId = "prod-test-" + System.currentTimeMillis();
    MDC.put("correlationId", correlationId);
    MDC.put("userId", "test-user-123");
    MDC.put("requestId", "req-" + UUID.randomUUID());
    
    // When: Execute business operation
    performBusinessOperation();
    
    // Then: Verify logs contain correct context
    assertThat(getLogEntries())
        .allMatch(entry -> entry.contains(correlationId))
        .allMatch(entry -> entry.contains("test-user-123"));
}

// Scenario 2: Cross-service Log Correlation Test
@Test
void shouldMaintainCorrelationAcrossServices() {
    // Given: Initiate cross-service request
    String correlationId = generateCorrelationId();
    
    // When: Call multiple microservices
    callOrderService(correlationId);
    callPaymentService(correlationId);
    callInventoryService(correlationId);
    
    // Then: Verify all service logs contain same correlation ID
    assertThat(getAllServiceLogs())
        .allMatch(log -> log.getCorrelationId().equals(correlationId));
}
```

**Acceptance Criteria**:

- ✅ All log entries contain necessary MDC context
- ✅ Correlation ID remains consistent throughout request lifecycle
- ✅ Log format complies with JSON structured standards
- ✅ Sensitive information properly masked (PII, passwords, etc.)

#### 1.2 Metrics Collection Tests

**Test Objective**: Verify accurate collection of business and system metrics

**Test Scenarios**:

```java
// Scenario 1: Business Metrics Test
@Test
void shouldCollectBusinessMetrics() {
    // Given: Initial metric state
    double initialOrderCount = getMetricValue("orders.created.total");
    double initialRevenue = getMetricValue("revenue.total");
    
    // When: Execute business operation
    createOrder(OrderBuilder.newOrder()
        .withAmount(BigDecimal.valueOf(100.00))
        .withCustomerId("customer-123")
        .build());
    
    // Then: Verify metric updates
    await().atMost(30, SECONDS).untilAsserted(() -> {
        assertThat(getMetricValue("orders.created.total"))
            .isEqualTo(initialOrderCount + 1);
        assertThat(getMetricValue("revenue.total"))
            .isEqualTo(initialRevenue + 100.00);
    });
}

// Scenario 2: System Metrics Test
@Test
void shouldCollectSystemMetrics() {
    // When: System runs for a period
    generateSystemLoad();
    
    // Then: Verify system metrics are available
    assertThat(getMetricValue("jvm.memory.used")).isGreaterThan(0);
    assertThat(getMetricValue("jvm.gc.pause")).isGreaterThan(0);
    assertThat(getMetricValue("http.server.requests")).isGreaterThan(0);
    assertThat(getMetricValue("database.connections.active")).isGreaterThan(0);
}
```

**Acceptance Criteria**:

- ✅ Business metrics accurately reflect actual operations
- ✅ System metrics cover key components like JVM, HTTP, database
- ✅ Metric tags are correctly set
- ✅ Metric data is queryable in Prometheus/CloudWatch

#### 1.3 Distributed Tracing Tests

**Test Objective**: Verify completeness of cross-service request tracing

**Test Scenarios**:

```java
// Scenario 1: Single Service Tracing Test
@Test
void shouldCreateTraceForSingleService() {
    // Given: Enable tracing
    Span parentSpan = tracer.nextSpan().name("test-operation").start();
    
    try (Tracer.SpanInScope ws = tracer.withSpanInScope(parentSpan)) {
        // When: Execute business operation
        String result = orderService.createOrder(createOrderRequest());
        
        // Then: Verify span creation
        assertThat(result).isNotNull();
        assertThat(parentSpan.getTraceId()).isNotNull();
        assertThat(parentSpan.getSpanId()).isNotNull();
    } finally {
        parentSpan.end();
    }
}

// Scenario 2: Cross-service Tracing Test
@Test
void shouldTraceAcrossMultipleServices() {
    // Given: Generate trace ID
    String traceId = generateTraceId();
    
    // When: Execute cross-service operation
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Trace-ID", traceId);
    
    ResponseEntity<OrderResponse> response = restTemplate.exchange(
        "/../api/orders", HttpMethod.POST, 
        new HttpEntity<>(createOrderRequest(), headers),
        OrderResponse.class
    );
    
    // Then: Verify trace data
    await().atMost(60, SECONDS).untilAsserted(() -> {
        List<Span> spans = getSpansForTrace(traceId);
        assertThat(spans).hasSizeGreaterThan(3); // Order, Payment, Inventory services
        assertThat(spans).allMatch(span -> span.getTraceId().equals(traceId));
    });
}
```

**Acceptance Criteria**:

- ✅ Each request has unique trace ID
- ✅ Cross-service calls maintain trace context
- ✅ Spans contain necessary tags and attributes
- ✅ Trace data is visualized in X-Ray/Jaeger

### 🏥 2. Health Check and Monitoring Tests

#### 2.1 Application Health Check Tests

**Test Objective**: Verify application and dependency service health monitoring

**Test Scenarios**:

```java
// Scenario 1: Basic Health Check Test
@Test
void shouldReturnHealthyStatusWhenAllComponentsUp() {
    // When: Query health status
    ResponseEntity<HealthResponse> response = restTemplate.getForEntity(
        "/actuator/health", HealthResponse.class);
    
    // Then: Verify health status
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getStatus()).isEqualTo("UP");
    assertThat(response.getBody().getComponents())
        .containsKeys("db", "diskSpace", "redis", "kafka");
}

// Scenario 2: Dependency Service Failure Test
@Test
void shouldReturnUnhealthyWhenDependencyDown() {
    // Given: Simulate database failure
    simulateDatabaseFailure();
    
    // When: Query health status
    ResponseEntity<HealthResponse> response = restTemplate.getForEntity(
        "/actuator/health", HealthResponse.class);
    
    // Then: Verify unhealthy status
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody().getStatus()).isEqualTo("DOWN");
    assertThat(response.getBody().getComponents().get("db").getStatus())
        .isEqualTo("DOWN");
}
```

#### 2.2 Kubernetes Probe Tests

**Test Objective**: Verify Kubernetes liveness and readiness probes

**Test Scenarios**:

```java
// Scenario 1: Liveness Probe Test
@Test
void shouldRespondToLivenessProbe() {
    // When: Query liveness probe
    ResponseEntity<String> response = restTemplate.getForEntity(
        "/actuator/health/liveness", String.class);
    
    // Then: Verify response
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"status\":\"UP\"");
}

// Scenario 2: Readiness Probe Test
@Test
void shouldRespondToReadinessProbe() {
    // When: Query readiness probe
    ResponseEntity<String> response = restTemplate.getForEntity(
        "/actuator/health/readiness", String.class);
    
    // Then: Verify response
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"status\":\"UP\"");
}
```

**Acceptance Criteria**:

- ✅ Health check response time < 5 seconds
- ✅ Liveness probe response time < 2 seconds
- ✅ Readiness probe response time < 3 seconds
- ✅ Correctly report unhealthy status when dependencies fail

### 🚀 3. Performance and Load Tests

#### 3.1 Observability Overhead Tests

**Test Objective**: Verify observability system impact on application performance

**Test Scenarios**:

```java
// Scenario 1: Baseline Performance Test
@Test
void shouldMeasureObservabilityOverhead() {
    // Given: Disable observability
    disableObservability();
    
    // When: Execute baseline test
    long baselineLatency = measureAverageLatency(1000);
    double baselineThroughput = measureThroughput(60);
    
    // Given: Enable observability
    enableObservability();
    
    // When: Execute same test
    long observabilityLatency = measureAverageLatency(1000);
    double observabilityThroughput = measureThroughput(60);
    
    // Then: Verify performance impact within acceptable range
    double latencyOverhead = (observabilityLatency - baselineLatency) / (double) baselineLatency * 100;
    double throughputImpact = (baselineThroughput - observabilityThroughput) / baselineThroughput * 100;
    
    assertThat(latencyOverhead).isLessThan(5.0); // < 5% latency increase
    assertThat(throughputImpact).isLessThan(3.0); // < 3% throughput decrease
}

// Scenario 2: High Load Stability Test
@Test
void shouldMaintainStabilityUnderHighLoad() {
    // Given: Configure high load test
    int concurrentUsers = 100;
    Duration testDuration = Duration.ofMinutes(10);
    
    // When: Execute load test
    LoadTestResult result = executeLoadTest(concurrentUsers, testDuration);
    
    // Then: Verify system stability
    assertThat(result.getErrorRate()).isLessThan(0.1); // < 0.1% error rate
    assertThat(result.getP95Latency()).isLessThan(Duration.ofSeconds(2)); // P95 < 2s
    assertThat(result.getMemoryLeaks()).isEmpty(); // No memory leaks
    
    // Verify observability data integrity
    assertThat(getMetricsCompleteness()).isGreaterThan(0.99); // > 99% data integrity
    assertThat(getTraceCompleteness()).isGreaterThan(0.95); // > 95% trace integrity
}
```

#### 3.2 Sampling Strategy Tests

**Test Objective**: Verify effectiveness of trace sampling strategies

**Test Scenarios**:

```java
// Scenario 1: Sampling Rate Verification Test
@Test
void shouldRespectSamplingConfiguration() {
    // Given: Set 10% sampling rate
    setSamplingRate(0.1);
    
    // When: Generate large number of requests
    int totalRequests = 10000;
    for (int i = 0; i < totalRequests; i++) {
        makeRequest("/../api/test");
    }
    
    // Then: Verify sampling rate
    await().atMost(2, MINUTES).untilAsserted(() -> {
        long sampledTraces = countSampledTraces();
        double actualSamplingRate = sampledTraces / (double) totalRequests;
        
        assertThat(actualSamplingRate)
            .isBetween(0.08, 0.12); // Allow ±2% error
    });
}

// Scenario 2: Intelligent Sampling Test
@Test
void shouldApplyIntelligentSampling() {
    // Given: Configure intelligent sampling (100% error requests, 1% normal requests)
    configureIntelligentSampling();
    
    // When: Generate mixed requests
    generateNormalRequests(1000);
    generateErrorRequests(10);
    
    // Then: Verify sampling strategy
    await().atMost(1, MINUTES).untilAsserted(() -> {
        assertThat(getErrorTraceSamplingRate()).isEqualTo(1.0); // 100% error sampling
        assertThat(getNormalTraceSamplingRate()).isBetween(0.005, 0.015); // ~1% normal sampling
    });
}
```

### 🔒 4. Security and Compliance Tests

#### 4.1 Sensitive Data Masking Tests

**Test Objective**: Verify sensitive information is properly masked in logs and traces

**Test Scenarios**:

```java
// Scenario 1: PII Data Masking Test
@Test
void shouldMaskPiiDataInLogs() {
    // Given: Request containing PII
    CreateUserRequest request = CreateUserRequest.builder()
        .email("user@example.com")
        .phone("0912345678")
        .creditCard("4111111111111111")
        .ssn("123-45-6789")
        .build();
    
    // When: Process request
    userService.createUser(request);
    
    // Then: Verify PII is masked in logs
    List<LogEntry> logs = getRecentLogEntries();
    assertThat(logs).allMatch(log -> {
        String content = log.getMessage();
        return !content.contains("user@example.com") &&
               !content.contains("0912345678") &&
               !content.contains("4111111111111111") &&
               !content.contains("123-45-6789") &&
               content.contains("***@***.com") && // Masked format
               content.contains("091****678");
    });
}

// Scenario 2: Trace Data Security Test
@Test
void shouldNotExposeSecretsInTraces() {
    // Given: Operation containing sensitive configuration
    String apiKey = "secret-api-key-12345";
    String password = "super-secret-password";
    
    // When: Execute authenticated operation
    authenticatedService.performOperation(apiKey, password);
    
    // Then: Verify trace data doesn't contain sensitive information
    await().atMost(30, SECONDS).untilAsserted(() -> {
        List<Span> spans = getRecentSpans();
        assertThat(spans).allMatch(span -> {
            Map<String, String> tags = span.getTags();
            return !tags.values().stream().anyMatch(value -> 
                value.contains(apiKey) || value.contains(password));
        });
    });
}
```

#### 4.2 Audit Log Tests

**Test Objective**: Verify audit logging for critical operations

**Test Scenarios**:

```java
// Scenario 1: User Operation Audit Test
@Test
void shouldLogUserOperationsForAudit() {
    // Given: User executes critical operation
    String userId = "user-123";
    String operation = "DELETE_ORDER";
    String orderId = "order-456";
    
    // When: Execute delete operation
    orderService.deleteOrder(orderId, userId);
    
    // Then: Verify audit logs
    await().atMost(10, SECONDS).untilAsserted(() -> {
        List<AuditLogEntry> auditLogs = getAuditLogs();
        assertThat(auditLogs).anyMatch(entry ->
            entry.getUserId().equals(userId) &&
            entry.getOperation().equals(operation) &&
            entry.getResourceId().equals(orderId) &&
            entry.getTimestamp() != null &&
            entry.getIpAddress() != null
        );
    });
}
```

### 💰 5. Cost Optimization Tests

#### 5.1 Resource Usage Monitoring Tests

**Test Objective**: Verify observability resource usage monitoring and optimization

**Test Scenarios**:

```java
// Scenario 1: CloudWatch Cost Monitoring Test
@Test
void shouldMonitorCloudWatchCosts() {
    // Given: Run for a period to collect data
    Duration monitoringPeriod = Duration.ofHours(1);
    runApplicationUnderLoad(monitoringPeriod);
    
    // When: Query cost metrics
    CloudWatchCostMetrics costs = cloudWatchCostService.getCostMetrics(monitoringPeriod);
    
    // Then: Verify costs within expected range
    assertThat(costs.getLogIngestionCost()).isLessThan(10.0); // < $10/hour
    assertThat(costs.getMetricsCost()).isLessThan(5.0); // < $5/hour
    assertThat(costs.getTracingCost()).isLessThan(15.0); // < $15/hour
    
    // Verify cost alert configuration
    assertThat(costs.hasAlertConfigured()).isTrue();
    assertThat(costs.getAlertThreshold()).isLessThan(50.0); // < $50/hour alert
}

// Scenario 2: Data Retention Policy Test
@Test
void shouldApplyDataRetentionPolicies() {
    // Given: Configure data retention policies
    configureRetentionPolicies(
        logs: Duration.ofDays(30),
        metrics: Duration.ofDays(90),
        traces: Duration.ofDays(7)
    );
    
    // When: Wait for retention policy to take effect
    waitForRetentionPolicyApplication();
    
    // Then: Verify old data is cleaned up
    assertThat(getLogDataAge()).isLessThanOrEqualTo(Duration.ofDays(30));
    assertThat(getMetricDataAge()).isLessThanOrEqualTo(Duration.ofDays(90));
    assertThat(getTraceDataAge()).isLessThanOrEqualTo(Duration.ofDays(7));
}
```

### 🌐 6. Multi-Environment and Disaster Recovery Tests

#### 6.1 Multi-Region Deployment Tests

**Test Objective**: Verify cross-region observability data synchronization and failover

**Test Scenarios**:

```java
// Scenario 1: Cross-Region Data Synchronization Test
@Test
void shouldSynchronizeObservabilityDataAcrossRegions() {
    // Given: Multi-region deployment
    List<String> regions = Arrays.asList("us-east-1", "us-west-2", "eu-west-1");
    
    // When: Generate data in primary region
    String correlationId = generateDataInPrimaryRegion();
    
    // Then: Verify data is visible in all regions
    await().atMost(5, MINUTES).untilAsserted(() -> {
        for (String region : regions) {
            assertThat(isDataAvailableInRegion(correlationId, region)).isTrue();
        }
    });
}

// Scenario 2: Regional Failover Test
@Test
void shouldFailoverObservabilityServicesGracefully() {
    // Given: Primary region running normally
    String primaryRegion = "us-east-1";
    String secondaryRegion = "us-west-2";
    
    // When: Simulate primary region failure
    simulateRegionFailure(primaryRegion);
    
    // Then: Verify failover
    await().atMost(2, MINUTES).untilAsserted(() -> {
        assertThat(isObservabilityServiceActive(secondaryRegion)).isTrue();
        assertThat(getActiveRegion()).isEqualTo(secondaryRegion);
    });
    
    // Verify data integrity
    assertThat(getDataLossDuringFailover()).isLessThan(0.01); // < 1% data loss
}
```

#### 6.2 Disaster Recovery Tests

**Test Objective**: Verify observability system disaster recovery capabilities

**Test Scenarios**:

```java
// Scenario 1: Complete Disaster Recovery Test
@Test
void shouldRecoverFromCompleteSystemFailure() {
    // Given: System running normally
    generateBaselineData();
    
    // When: Simulate complete system failure
    simulateCompleteSystemFailure();
    
    // Execute disaster recovery procedure
    executeDisasterRecoveryProcedure();
    
    // Then: Verify system recovery
    await().atMost(30, MINUTES).untilAsserted(() -> {
        assertThat(isSystemFullyRecovered()).isTrue();
        assertThat(getDataIntegrityScore()).isGreaterThan(0.95); // > 95% data integrity
        assertThat(getRecoveryTimeObjective()).isLessThan(Duration.ofMinutes(30)); // RTO < 30min
    });
}

// Scenario 2: Partial Service Failure Recovery Test
@Test
void shouldRecoverFromPartialServiceFailure() {
    // Given: Simulate partial service failure
    simulateServiceFailure("tracing-service");
    
    // When: System continues operating
    continueOperationsWithDegradedService();
    
    // Then: Verify graceful degradation
    assertThat(isLoggingServiceActive()).isTrue();
    assertThat(isMetricsServiceActive()).isTrue();
    assertThat(isTracingServiceActive()).isFalse();
    
    // Verify service recovery
    restoreService("tracing-service");
    await().atMost(5, MINUTES).untilAsserted(() -> {
        assertThat(isTracingServiceActive()).isTrue();
        assertThat(getServiceHealthScore()).isEqualTo(1.0); // 100% healthy
    });
}
```

### 🔄 7. CI/CD Integration Tests

#### 7.1 Deployment Pipeline Observability Tests

**Test Objective**: Verify observability integration in CI/CD pipelines

**Test Scenarios**:

```java
// Scenario 1: Deployment Metrics Test
@Test
void shouldCollectDeploymentMetrics() {
    // Given: Trigger deployment
    String deploymentId = triggerDeployment();
    
    // When: Deployment executes
    waitForDeploymentCompletion(deploymentId);
    
    // Then: Verify deployment metrics
    DeploymentMetrics metrics = getDeploymentMetrics(deploymentId);
    assertThat(metrics.getDeploymentDuration()).isLessThan(Duration.ofMinutes(15));
    assertThat(metrics.getSuccessRate()).isEqualTo(1.0);
    assertThat(metrics.getRollbackCount()).isEqualTo(0);
}

// Scenario 2: Quality Gate Test
@Test
void shouldEnforceQualityGatesInPipeline() {
    // Given: Configure quality gates
    configureQualityGates(
        errorRate: 0.01,        // < 1% error rate
        responseTime: Duration.ofSeconds(2), // < 2s response time
        availability: 0.999     // > 99.9% availability
    );
    
    // When: Execute deployment validation
    QualityGateResult result = executeQualityGateValidation();
    
    // Then: Verify quality gates
    assertThat(result.getErrorRate()).isLessThan(0.01);
    assertThat(result.getAverageResponseTime()).isLessThan(Duration.ofSeconds(2));
    assertThat(result.getAvailability()).isGreaterThan(0.999);
    assertThat(result.isPassed()).isTrue();
}
```

## Test Environment Configuration

### 🛠️ Production Environment Configuration

#### AWS Service Configuration

```yaml
# CloudWatch Configuration
cloudwatch:
  region: us-east-1
  log-groups:
    - name: /aws/lambda/genai-demo
      retention-days: 30
    - name: /aws/ecs/genai-demo
      retention-days: 30
  metrics:
    namespace: GenAIDemo/Production
    dimensions:
      Environment: production
      Service: genai-demo
  alarms:
    - name: HighErrorRate
      threshold: 0.05
      comparison: GreaterThanThreshold
    - name: HighLatency
      threshold: 2000
      comparison: GreaterThanThreshold

# X-Ray Configuration
xray:
  tracing-config:
    sampling-rate: 0.1
    reservoir-size: 1
  service-map:
    enabled: true
  annotations:
    - service
    - environment
    - version

# Prometheus Configuration (Optional)
prometheus:
  scrape-configs:
    - job_name: genai-demo
      static_configs:
        - targets: ['localhost:8080']
      metrics_path: /actuator/prometheus
      scrape_interval: 30s
```

#### Spring Boot Configuration

```yaml
# application-production.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      show-components: always
      probes:
        enabled: true
    metrics:
      enabled: true
    prometheus:
      enabled: true
  metrics:
    export:
      cloudwatch:
        enabled: true
        namespace: GenAIDemo/Production
        batch-size: 20
      prometheus:
        enabled: true
    tags:
      environment: production
      service: genai-demo
      version: ${app.version:unknown}

# Logging Configuration
logging:
  level:
    solid.humank.genaidemo: INFO
    org.springframework.web: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{correlationId:-}] %logger{36} - %msg%n"
  appender:
    cloudwatch:
      enabled: true
      log-group: /aws/ecs/genai-demo
      log-stream: ${HOSTNAME:localhost}

# Tracing Configuration
tracing:
  enabled: true
  sampling:
    probability: 0.1
  zipkin:
    enabled: false
  jaeger:
    enabled: false
  xray:
    enabled: true
```

### 🧪 Test Data Preparation

#### Test Data Generator

```java
@Component
public class ObservabilityTestDataGenerator {
    
    public void generateBusinessOperations(int count) {
        for (int i = 0; i < count; i++) {
            // Generate orders
            createTestOrder();
            
            // Generate payments
            processTestPayment();
            
            // Generate inventory operations
            updateTestInventory();
            
            // Random delay
            randomDelay();
        }
    }
    
    public void generateErrorScenarios() {
        // Generate 4xx errors
        generate4xxErrors();
        
        // Generate 5xx errors
        generate5xxErrors();
        
        // Generate timeout errors
        generateTimeoutErrors();
        
        // Generate database connection errors
        generateDatabaseErrors();
    }
    
    public void generateHighLoadScenario(Duration duration) {
        long endTime = System.currentTimeMillis() + duration.toMillis();
        
        while (System.currentTimeMillis() < endTime) {
            CompletableFuture.runAsync(this::generateBusinessOperations);
            Thread.sleep(100); // Control load
        }
    }
}
```

## Test Execution Plan

### 📅 Test Phase Planning

#### Phase 1: Basic Verification (1-2 days)

**Objective**: Verify basic observability functionality

**Test Content**:

- ✅ Application startup and health checks
- ✅ Basic logging and formatting
- ✅ Basic metrics collection
- ✅ Simple tracing functionality

**Success Criteria**:

- All health check endpoints respond normally
- Log format complies with JSON standards
- Basic metrics visible in CloudWatch
- Single service tracing works normally

#### Phase 2: Integration Verification (3-5 days)

**Objective**: Verify cross-service observability integration

**Test Content**:

- ✅ Cross-service tracing
- ✅ Correlation ID propagation
- ✅ Business metrics accuracy
- ✅ Error handling and monitoring

**Success Criteria**:

- Cross-service tracing integrity > 95%
- Correlation ID propagation success rate > 99%
- Business metrics consistent with actual operations
- Error monitoring and alerting normal

#### Phase 3: Performance Verification (2-3 days)

**Objective**: Verify observability system performance impact

**Test Content**:

- ✅ Performance baseline testing
- ✅ Load testing
- ✅ Sampling strategy verification
- ✅ Resource usage monitoring

**Success Criteria**:

- Observability overhead < 5%
- System stable under high load
- Sampling strategy effective
- Resource usage within expected range

#### Phase 4: Security and Compliance Verification (2-3 days)

**Objective**: Verify security and compliance requirements

**Test Content**:

- ✅ Sensitive data masking
- ✅ Audit log integrity
- ✅ Data retention policies
- ✅ Access control

**Success Criteria**:

- PII data 100% masked
- Audit logs completely recorded
- Data retention policies effective
- Access control correctly configured

#### Phase 5: Disaster Recovery Verification (3-5 days)

**Objective**: Verify disaster recovery capabilities

**Test Content**:

- ✅ Multi-region failover
- ✅ Data backup and recovery
- ✅ Service degradation handling
- ✅ Complete disaster recovery

**Success Criteria**:

- RTO < 30 minutes
- RPO < 5 minutes
- Data integrity > 95%
- Service degradation normal

### 🎯 Test Execution Checklist

#### Pre-Test Preparation

- [ ] **Environment Preparation**
  - [ ] AWS account and permission configuration
  - [ ] CloudWatch, X-Ray service enablement
  - [ ] Test data preparation
  - [ ] Monitoring dashboard setup

- [ ] **Tool Preparation**
  - [ ] Load testing tools (JMeter/Gatling)
  - [ ] Monitoring tools (Grafana/CloudWatch Dashboard)
  - [ ] Log analysis tools (CloudWatch Insights)
  - [ ] Trace analysis tools (X-Ray Console)

- [ ] **Team Preparation**
  - [ ] Test team training
  - [ ] Role and responsibility assignment
  - [ ] Communication channel establishment
  - [ ] Emergency contact methods

#### Test Execution

- [ ] **Daily Checks**
  - [ ] Test environment health status
  - [ ] Test data integrity
  - [ ] Test result recording
  - [ ] Issue tracking and resolution

- [ ] **Weekly Reviews**
  - [ ] Test progress assessment
  - [ ] Risk identification and mitigation
  - [ ] Test plan adjustments
  - [ ] Stakeholder communication

#### Post-Test Cleanup

- [ ] **Data Cleanup**
  - [ ] Test data removal
  - [ ] Temporary resource release
  - [ ] Cost analysis and optimization
  - [ ] Environment reset

- [ ] **Documentation**
  - [ ] Test report writing
  - [ ] Issue and solution recording
  - [ ] Best practices summary
  - [ ] Knowledge transfer

## Acceptance Criteria

### 🎯 Functional Acceptance Criteria

#### Logging System

- ✅ **Structured Logging**: 100% of log entries comply with JSON format
- ✅ **MDC Propagation**: Correlation ID remains consistent throughout request lifecycle
- ✅ **Log Levels**: Support dynamic log level adjustment
- ✅ **Sensitive Data**: PII and confidential information 100% masked

#### Metrics System

- ✅ **Business Metrics**: Accurately reflect actual business operations
- ✅ **System Metrics**: Cover key components like JVM, HTTP, database
- ✅ **Custom Metrics**: Support business-specific metric definitions
- ✅ **Metric Tags**: Correctly set dimension tags

#### Tracing System

- ✅ **Cross-service Tracing**: Integrity > 95%
- ✅ **Trace Context**: Correctly propagate trace context
- ✅ **Sampling Strategy**: Sampling rate controlled within configured range
- ✅ **Trace Visualization**: Correctly displayed in X-Ray/Jaeger

#### Health Checks

- ✅ **Response Time**: Health checks < 5s, probes < 3s
- ✅ **Dependency Checks**: Correctly detect dependency service status
- ✅ **Failure Handling**: Correctly report status when dependencies fail
- ✅ **Kubernetes Integration**: Probes correctly integrated with K8s

### 📊 Performance Acceptance Criteria

#### System Performance

- ✅ **Latency Impact**: Observability overhead < 5%
- ✅ **Throughput Impact**: Throughput decrease < 3%
- ✅ **Memory Usage**: Memory increase < 10%
- ✅ **CPU Usage**: CPU increase < 5%

#### Scalability

- ✅ **High Load Stability**: Stable operation under 100 concurrent users
- ✅ **Data Integrity**: Data integrity > 99% under high load
- ✅ **Error Rate**: System error rate < 0.1%
- ✅ **Recovery Time**: Recovery time < 5 minutes after load

### 🔒 Security Acceptance Criteria

#### Data Protection

- ✅ **PII Masking**: Personal identifiable information 100% masked
- ✅ **Confidential Information**: API keys, passwords don't appear in logs
- ✅ **Data Transmission**: All data transmission uses TLS encryption
- ✅ **Access Control**: Role-based access control correctly configured

#### Compliance

- ✅ **Audit Logs**: Critical operations 100% recorded
- ✅ **Data Retention**: Automatically clean expired data per policy
- ✅ **Regulatory Compliance**: Comply with GDPR, SOX and other regulations
- ✅ **Data Sovereignty**: Data storage complies with regional regulations

### 💰 Cost Acceptance Criteria

#### Cost Control

- ✅ **CloudWatch Cost**: < $100/month (based on expected load)
- ✅ **X-Ray Cost**: < $50/month
- ✅ **Storage Cost**: < $30/month
- ✅ **Total Cost**: Total observability cost < 5% of operational cost

#### Cost Optimization

- ✅ **Sampling Strategy**: Effectively reduce tracing costs
- ✅ **Data Retention**: Automatic cleanup reduces storage costs
- ✅ **Alert Configuration**: Timely alerts for cost anomalies
- ✅ **Usage Monitoring**: Regular review and optimization of usage

## Troubleshooting

### 🚨 Common Issues and Solutions

#### Logging Issues

**Issue**: Incorrect log format

```text
Solutions:
1. Check logback-spring.xml configuration
2. Verify JSON encoder settings
3. Confirm MDC configuration is correct
4. Test log output format
```

**Issue**: Missing correlation ID

```text
Solutions:
1. Check MDC filter configuration
2. Verify context propagation in async processing
3. Confirm header passing in cross-service calls
4. Check thread pool configuration
```

#### Metrics Issues

**Issue**: Missing metric data

```text
Solutions:
1. Check Micrometer configuration
2. Verify CloudWatch permissions
3. Confirm metric registration is correct
4. Check network connectivity
```

**Issue**: Inaccurate metric data

```text
Solutions:
1. Verify metric calculation logic
2. Check tag settings
3. Confirm time synchronization
4. Check sampling configuration
```

#### Tracing Issues

**Issue**: Incomplete trace data

```text
Solutions:
1. Check OpenTelemetry configuration
2. Verify sampling rate settings
3. Confirm X-Ray permissions
4. Check network connectivity and firewall
```

**Issue**: Cross-service tracing interruption

```text
Solutions:
1. Check trace context propagation
2. Verify HTTP header configuration
3. Confirm inter-service communication configuration
4. Check trace ID format
```

### 🔧 Diagnostic Tools

#### Log Diagnostics

```bash
# Check log format
curl -s http://localhost:8080/actuator/loggers | jq .

# Test MDC functionality
curl -H "X-Correlation-ID: test-123" http://localhost:8080/../api/test

# View CloudWatch logs
aws logs describe-log-groups --log-group-name-prefix "/aws/ecs/genai-demo"
```

#### Metrics Diagnostics

```bash
# Check Prometheus endpoint
curl -s http://localhost:8080/actuator/prometheus | grep genai_demo

# View CloudWatch metrics
aws cloudwatch list-metrics --namespace "GenAIDemo/Production"

# Test custom metrics
curl -X POST http://localhost:8080/api/test/metrics
```

#### Tracing Diagnostics

```bash
# Check X-Ray service map
aws xray get-service-graph --start-time 2024-01-01T00:00:00Z --end-time 2024-01-01T23:59:59Z

# View trace summaries
aws xray get-trace-summaries --time-range-type TimeRangeByStartTime --start-time 2024-01-01T00:00:00Z --end-time 2024-01-01T01:00:00Z
```

## Best Practices

### 🏆 Development Best Practices

#### Logging Best Practices

1. **Use Structured Logging**: Always use JSON format
2. **Appropriate Log Levels**: Use INFO and above levels in production
3. **Avoid Sensitive Information**: Never log passwords, API keys, etc.
4. **Use MDC**: Set correlation ID for each request
5. **Exception Handling**: Log complete exception stack traces

#### Metrics Best Practices

1. **Business Metrics First**: Focus on business value metrics
2. **Reasonable Dimensions**: Avoid high cardinality tags
3. **Consistent Naming**: Use unified metric naming conventions
4. **Appropriate Types**: Choose correct metric types (Counter, Gauge, Timer)
5. **Regular Cleanup**: Remove unused metrics

#### Tracing Best Practices

1. **Intelligent Sampling**: 100% sampling for error requests, low sampling for normal requests
2. **Meaningful Spans**: Create spans for important operations
3. **Correct Tags**: Add tags that help with analysis
4. **Context Propagation**: Ensure cross-service context is correctly propagated
5. **Performance Considerations**: Avoid significant performance impact from tracing

### 🎯 Operations Best Practices

#### Monitoring Strategy

1. **Layered Monitoring**: Infrastructure, application, and business three-layer monitoring
2. **Proactive Alerting**: Set alerts based on SLI/SLO
3. **Alert Fatigue**: Avoid too many meaningless alerts
4. **Root Cause Analysis**: Establish problem root cause analysis process
5. **Continuous Improvement**: Regularly review and optimize monitoring strategy

#### Cost Management

1. **Regular Reviews**: Monthly review of observability costs
2. **Sampling Optimization**: Adjust sampling strategy based on value
3. **Data Lifecycle**: Implement automatic data cleanup
4. **Resource Tagging**: Use tags for cost allocation
5. **Budget Alerts**: Set cost budgets and alerts

#### Security Management

1. **Least Privilege**: Follow principle of least privilege
2. **Regular Audits**: Regularly audit access permissions
3. **Data Classification**: Classify observability data
4. **Compliance Checks**: Regular compliance checks
5. **Incident Response**: Establish security incident response process

---

## 📚 Reference Resources

### Technical Documentation

- [Spring Boot Actuator Official Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer Official Documentation](https://micrometer.io/docs)
- [OpenTelemetry Java Documentation](https://opentelemetry.io/docs/instrumentation/java/)
- [AWS CloudWatch Documentation](https://docs.aws.amazon.com/cloudwatch/)
- [AWS X-Ray Documentation](https://docs.aws.amazon.com/xray/)

### Best Practice Guides

- [Google SRE Book - Monitoring](https://sre.google/sre-book/monitoring-distributed-systems/)
- [AWS Well-Architected Framework - Observability](https://docs.aws.amazon.com/wellarchitected/latest/framework/ops_observability.html)
- [CNCF Observability Whitepaper](https://github.com/cncf/tag-observability/blob/main/whitepaper.md)

### Tools and Platforms

- [Prometheus](https://prometheus.io/)
- [Grafana](https://grafana.com/)
- [Jaeger](https://www.jaegertracing.io/)
- [ELK Stack](https://www.elastic.co/elastic-stack/)

---

**Document Version**: 1.0  
**Last Updated**: January 2025  
**Maintainer**: GenAI Demo Development Team  
**Reviewers**: Architecture Team, SRE Team
---

## 🌟 Industry Mainstream Observability Testing Practices

### 📊 **Real-World Industry Practice Analysis**

You've raised a very important question! In practice, the industry **rarely** uses Java programs directly to test observability systems. Here are the mainstream real-world practices:

### 🛠️ **1. Tool-Based Testing Methods**

#### **Synthetic Monitoring**

```yaml
# DataDog Synthetic Tests
synthetic_tests:
  - name: "API Health Check"
    type: api
    request:
      url: "https://api.example.com/health"
      method: GET
    assertions:
      - type: statusCode
        operator: is
        value: 200
      - type: responseTime
        operator: lessThan
        value: 2000
    locations: ["aws:us-east-1", "aws:eu-west-1"]
    frequency: 300 # 5 minutes

  - name: "User Journey Test"
    type: browser
    steps:
      - type: click
        element: "#login-button"
      - type: type
        element: "#username"
        text: "test@example.com"
    locations: ["aws:us-east-1"]
    frequency: 900 # 15 minutes
```

#### **Chaos Engineering Testing**

```yaml
# Chaos Monkey / Litmus Configuration
chaos_experiments:
  - name: "pod-delete"
    spec:
      components:
        env:
          - name: TOTAL_CHAOS_DURATION
            value: "60"
          - name: CHAOS_INTERVAL
            value: "10"
      probe:
        - name: "check-observability-metrics"
          type: "httpProbe"
          httpProbe/inputs:
            url: "http://prometheus:9090/../api/v1/query"
            method:
              get:
                criteria: "=="
                responseCode: "200"
```

### 🔧 **2. Script-Based Testing Methods**

#### **Bash/Shell Script Testing**

```bash
#!/bin/bash
# observability_health_check.sh

# Check Prometheus metrics
check_prometheus_metrics() {
    echo "Checking Prometheus metrics..."
    
    # Check application metrics
    APP_METRICS=$(curl -s "http://prometheus:9090/../api/v1/query?query=up{job=\"app\"}" | jq -r '.data.result[0].value[1]')
    if [ "$APP_METRICS" != "1" ]; then
        echo "❌ Application metrics not available"
        exit 1
    fi
    
    # Check error rate
    ERROR_RATE=$(curl -s "http://prometheus:9090/../api/v1/query?query=rate(http_requests_total{status=~\"5..\"}[5m])" | jq -r '.data.result[0].value[1]')
    if (( $(echo "$ERROR_RATE > 0.01" | bc -l) )); then
        echo "❌ Error rate too high: $ERROR_RATE"
        exit 1
    fi
    
    echo "✅ Prometheus metrics healthy"
}

# Check log completeness
check_log_completeness() {
    echo "Checking log completeness..."
    
    # Check logs from last 5 minutes
    LOG_COUNT=$(curl -s -G "http://elasticsearch:9200/logs-*/_search" \
        --data-urlencode 'q=@timestamp:[now-5m TO now]' | jq '.hits.total.value')
    
    if [ "$LOG_COUNT" -lt 100 ]; then
        echo "❌ Insufficient logs in last 5 minutes: $LOG_COUNT"
        exit 1
    fi
    
    echo "✅ Log completeness verified: $LOG_COUNT logs"
}

# Check tracing data
check_tracing_data() {
    echo "Checking tracing data..."
    
    # Check traces in Jaeger
    TRACE_COUNT=$(curl -s "http://jaeger:16686/../api/traces?service=app&lookback=5m" | jq '.data | length')
    
    if [ "$TRACE_COUNT" -lt 10 ]; then
        echo "❌ Insufficient traces: $TRACE_COUNT"
        exit 1
    fi
    
    echo "✅ Tracing data verified: $TRACE_COUNT traces"
}

# Main execution flow
main() {
    check_prometheus_metrics
    check_log_completeness  
    check_tracing_data
    echo "🎉 All observability checks passed!"
}

main "$@"
```

#### **Python Script Testing**

```python
#!/usr/bin/env python3
# observability_validator.py

import requests
import json
import time
from datetime import datetime, timedelta

class ObservabilityValidator:
    def __init__(self, config):
        self.prometheus_url = config['prometheus_url']
        self.elasticsearch_url = config['elasticsearch_url']
        self.jaeger_url = config['jaeger_url']
    
    def validate_sli_slo(self):
        """Validate SLI/SLO metrics"""
        print("🔍 Validating SLI/SLO metrics...")
        
        # Check availability SLI (target: 99.9%)
        availability_query = 'avg_over_time(up{job="app"}[5m])'
        availability = self._query_prometheus(availability_query)
        
        if availability < 0.999:
            raise Exception(f"❌ Availability SLI failed: {availability:.4f} < 0.999")
        
        # Check latency SLI (target: P95 < 2s)
        latency_query = 'histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))'
        p95_latency = self._query_prometheus(latency_query)
        
        if p95_latency > 2.0:
            raise Exception(f"❌ Latency SLI failed: P95={p95_latency:.2f}s > 2s")
        
        # Check error rate SLI (target: < 0.1%)
        error_rate_query = 'rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m])'
        error_rate = self._query_prometheus(error_rate_query)
        
        if error_rate > 0.001:
            raise Exception(f"❌ Error rate SLI failed: {error_rate:.4f} > 0.001")
        
        print("✅ All SLI/SLO metrics within targets")
    
    def validate_log_pipeline(self):
        """Validate log pipeline integrity"""
        print("🔍 Validating log pipeline...")
        
        # Generate test logs
        test_correlation_id = f"test-{int(time.time())}"
        self._generate_test_logs(test_correlation_id)
        
        # Wait for log processing
        time.sleep(30)
        
        # Verify logs reached Elasticsearch
        query = {
            "query": {
                "bool": {
                    "must": [
                        {"match": {"correlation_id": test_correlation_id}},
                        {"range": {"@timestamp": {"gte": "now-2m"}}}
                    ]
                }
            }
        }
        
        response = requests.post(
            f"{self.elasticsearch_url}/logs-*/_search",
            json=query
        )
        
        hits = response.json()['hits']['total']['value']
        if hits == 0:
            raise Exception(f"❌ Test logs not found in Elasticsearch")
        
        print(f"✅ Log pipeline validated: {hits} test logs processed")
    
    def validate_alerting_rules(self):
        """Validate alerting rules"""
        print("🔍 Validating alerting rules...")
        
        # Check Prometheus alerting rules
        response = requests.get(f"{self.prometheus_url}/../api/v1/rules")
        rules = response.json()['data']['groups']
        
        required_alerts = [
            'HighErrorRate',
            'HighLatency', 
            'ServiceDown',
            'DiskSpaceLow'
        ]
        
        active_alerts = []
        for group in rules:
            for rule in group['rules']:
                if rule['type'] == 'alerting':
                    active_alerts.append(rule['name'])
        
        missing_alerts = set(required_alerts) - set(active_alerts)
        if missing_alerts:
            raise Exception(f"❌ Missing alert rules: {missing_alerts}")
        
        print(f"✅ All required alert rules configured: {len(active_alerts)} rules")
    
    def _query_prometheus(self, query):
        """Query Prometheus metrics"""
        response = requests.get(
            f"{self.prometheus_url}/../api/v1/query",
            params={'query': query}
        )
        result = response.json()['data']['result']
        return float(result[0]['value'][1]) if result else 0.0
    
    def _generate_test_logs(self, correlation_id):
        """Generate test logs"""
        # Call application API to generate logs
        requests.get(
            "http://app:8080/../api/test/logs",
            headers={"X-Correlation-ID": correlation_id}
        )

# Usage example
if __name__ == "__main__":
    config = {
        'prometheus_url': 'http://prometheus:9090',
        'elasticsearch_url': 'http://elasticsearch:9200',
        'jaeger_url': 'http://jaeger:16686'
    }
    
    validator = ObservabilityValidator(config)
    
    try:
        validator.validate_sli_slo()
        validator.validate_log_pipeline()
        validator.validate_alerting_rules()
        print("🎉 All observability validations passed!")
    except Exception as e:
        print(f"💥 Validation failed: {e}")
        exit(1)
```

### 🚀 **3. CI/CD Pipeline Testing**

#### **GitLab CI Example**

```yaml
# .gitlab-ci.yml
stages:
  - build
  - test
  - deploy
  - observability-test

observability-smoke-test:
  stage: observability-test
  image: alpine/curl
  script:
    - apk add --no-cache jq bc
    - ./scripts/observability_health_check.sh
  only:
    - main
  when: on_success

observability-sli-validation:
  stage: observability-test
  image: python:3.9-alpine
  script:
    - pip install requests
    - python scripts/observability_validator.py
  only:
    - main
  when: on_success
  allow_failure: false

chaos-engineering-test:
  stage: observability-test
  image: litmuschaos/litmus-checker:latest
  script:
    - litmus run chaos-experiments/pod-delete.yaml
    - litmus validate --experiment-name pod-delete
  only:
    - schedules
  when: manual
```

#### **GitHub Actions Example**

```yaml
# .github/workflows/observability-test.yml
name: Observability Testing

on:
  push:
    branches: [main]
  schedule:
    - cron: '0 */6 * * *'  # Every 6 hours

jobs:
  synthetic-monitoring:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Run Synthetic Tests
        uses: datadog/synthetics-ci-github-action@v1
        with:
          api_key: ${{ secrets.DATADOG_API_KEY }}
          app_key: ${{ secrets.DATADOG_APP_KEY }}
          test_search_query: 'tag:observability'
          
  sli-slo-validation:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.9'
          
      - name: Install dependencies
        run: pip install requests prometheus-client
        
      - name: Validate SLI/SLO
        run: python scripts/sli_slo_validator.py
        env:
          PROMETHEUS_URL: ${{ secrets.PROMETHEUS_URL }}
          
  load-test-observability:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Run K6 Load Test
        uses: grafana/k6-action@v0.2.0
        with:
          filename: tests/load-test-observability.js
        env:
          K6_PROMETHEUS_RW_SERVER_URL: ${{ secrets.PROMETHEUS_URL }}
```

### 📊 **4. K6-Based Load Testing**

```javascript
// tests/load-test-observability.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const observabilityOverhead = new Trend('observability_overhead');

export const options = {
  stages: [
    { duration: '2m', target: 10 },   // Warm-up
    { duration: '5m', target: 50 },   // Normal load
    { duration: '2m', target: 100 },  // Peak load
    { duration: '5m', target: 100 },  // Maintain peak
    { duration: '2m', target: 0 },    // Cool down
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // P95 < 2s
    errors: ['rate<0.01'],             // Error rate < 1%
    observability_overhead: ['avg<50'], // Observability overhead < 50ms
  },
};

export default function () {
  // Test main business API
  const startTime = Date.now();
  
  const response = http.get('http://app:8080/../api/orders', {
    headers: {
      'X-Correlation-ID': `k6-test-${__VU}-${__ITER}`,
      'X-Load-Test': 'true',
    },
  });
  
  const endTime = Date.now();
  
  // Check response
  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 2s': (r) => r.timings.duration < 2000,
    'has correlation header': (r) => r.headers['X-Correlation-ID'] !== undefined,
  });
  
  errorRate.add(!success);
  
  // Test observability endpoint overhead
  const metricsStart = Date.now();
  const metricsResponse = http.get('http://app:8080/actuator/metrics');
  const metricsEnd = Date.now();
  
  observabilityOverhead.add(metricsEnd - metricsStart);
  
  sleep(1);
}

export function handleSummary(data) {
  return {
    'observability-test-results.json': JSON.stringify(data, null, 2),
  };
}
```

### 🔍 **5. Terraform-Based Infrastructure Testing**

```hcl
# tests/observability_test.tf
resource "test_assertions" "observability_stack" {
  component = "observability"

  equal "cloudwatch_log_groups_exist" {
    description = "CloudWatch log groups should exist"
    got         = length(data.aws_cloudwatch_log_groups.app_logs.log_group_names)
    want        = 3
  }

  equal "prometheus_targets_healthy" {
    description = "All Prometheus targets should be healthy"
    got = length([
      for target in data.prometheus_targets.all.targets :
      target if target.health == "up"
    ])
    want = length(data.prometheus_targets.all.targets)
  }

  check "xray_service_map_exists" {
    description = "X-Ray service map should show our services"
    condition = length(data.aws_xray_service_graph.app.services) > 0
  }
}

# Use Terratest for testing
resource "null_resource" "run_terratest" {
  provisioner "local-exec" {
    command = "cd tests && go test -v -timeout 30m"
  }
}
```

### 🎯 **Industry Real-World Testing Strategy Summary**

| Test Type | Main Tools | Use Cases | Automation Level |
|-----------|------------|-----------|------------------|
| **Synthetic Monitoring** | DataDog, New Relic, Pingdom | Continuous user experience monitoring | 🟢 Fully Automated |
| **SLI/SLO Validation** | Prometheus + Python/Bash | CI/CD pipeline quality gates | 🟢 Fully Automated |
| **Chaos Engineering** | Chaos Monkey, Litmus | Regular resilience testing | 🟡 Semi-Automated |
| **Load Testing** | K6, JMeter, Artillery | Performance and observability overhead testing | 🟢 Fully Automated |
| **Infrastructure Testing** | Terratest, InSpec | Infrastructure configuration validation | 🟢 Fully Automated |
| **Manual Validation** | Grafana, Kibana dashboards | Deep analysis and troubleshooting | 🔴 Manual Operation |

### 💡 **Key Insights**

1. **Java test programs are mainly used for**:
   - Unit testing and integration testing phases
   - Quick validation in development environments
   - Specific business logic observability testing

2. **Production environments mainly use**:
   - Script-based automated testing (Bash/Python)
   - Third-party monitoring tools' Synthetic Tests
   - Quality gate checks in CI/CD pipelines
   - Chaos Engineering tools

3. **Best practice combinations**:
   - Development phase: Java unit tests + integration tests
   - CI/CD phase: Script-based validation + SLI/SLO checks
   - Production phase: Synthetic Monitoring + Chaos Testing
   - Continuous improvement: Regular manual analysis + automated reporting

This combination provides appropriate test coverage at different stages, ensuring both development efficiency and production environment reliability.
