# Production Observability Testing Guide

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

This document provides a detailed testing guide for implementing a complete observability system in production environments. Based on the observability testing experience of the GenAI Demo project, it covers comprehensive observability verification including logs, metrics, tracing, and health checks.

### Scope of Application

- **Target Environment**: AWS Production Environment
- **Technology Stack**: Spring Boot 3.x + Java 21 + AWS Observability Services
- **Test Types**: Functional testing, performance testing, disaster recovery testing
- **Test Levels**: Unit testing, integration testing, end-to-end testing

## Testing Objectives

### 🎯 Primary Objectives

1. **Observability Completeness Verification**
   - Ensure all observability components function properly
   - Verify data flow completeness and accuracy
   - Ensure cross-service tracing functionality

2. **Performance and Reliability Verification**
   - Verify observability system stability under high load
   - Ensure observability overhead within acceptable range (< 5%)
   - Verify system recovery capabilities

3. **Security and Compliance Verification**
   - Ensure sensitive data is properly masked
   - Verify data retention policies
   - Ensure audit log completeness

4. **Cost Optimization Verification**
   - Verify sampling strategy effectiveness
   - Ensure resource usage optimization
   - Verify cost monitoring and alerting
## 
Testing Architecture

### 🏗️ Observability Technology Stack

```
┌─────────────────────────────────────────────────────────────┐
│                Production Observability Architecture        │
├─────────────────────────────────────────────────────────────┤
│  Application Layer                                          │
│  ├── Spring Boot Actuator (health checks, metrics)         │
│  ├── Micrometer (metrics collection)                        │
│  ├── OpenTelemetry (distributed tracing)                    │
│  └── Logback + MDC (structured logging)                     │
├─────────────────────────────────────────────────────────────┤
│  AWS Observability Services                                 │
│  ├── CloudWatch (metrics, logs, alarms)                     │
│  ├── X-Ray (distributed tracing)                            │
│  ├── CloudWatch Insights (log analysis)                     │
│  └── CloudWatch Dashboards (visualization)                  │
├─────────────────────────────────────────────────────────────┤
│  Third-party Tools (Optional)                               │
│  ├── Prometheus + Grafana                                   │
│  ├── ELK Stack (Elasticsearch, Logstash, Kibana)           │
│  └── Jaeger (trace visualization)                           │
└─────────────────────────────────────────────────────────────┘
```

## Core Testing Scenarios

### 🔍 1. Basic Observability Verification

#### 1.1 Structured Logging Tests

**Test Objective**: Verify structured log recording and MDC context propagation

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
    // Given: Initial metrics state
    double initialOrderCount = getMetricValue("orders.created.total");
    double initialRevenue = getMetricValue("revenue.total");
    
    // When: Execute business operation
    createOrder(OrderBuilder.newOrder()
        .withAmount(BigDecimal.valueOf(100.00))
        .withCustomerId("customer-123")
        .build());
    
    // Then: Verify metrics update
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
- ✅ System metrics cover key components: JVM, HTTP, database
- ✅ Metric tags are correctly configured
- ✅ Metric data is queryable in Prometheus/CloudWatch###
# 1.3 Distributed Tracing Tests

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
        "/api/orders", HttpMethod.POST, 
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
- ✅ Trace data is visualizable in X-Ray/Jaeger

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
- ✅ Correctly reports unhealthy status when dependencies fail## T
est Environment Configuration

### 🛠️ Production Environment Configuration

#### AWS Services Configuration

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

## Acceptance Criteria

### 🎯 Functional Acceptance Criteria

#### Logging System

- ✅ **Structured Logs**: 100% of log entries comply with JSON format
- ✅ **MDC Propagation**: Correlation ID remains consistent throughout request lifecycle
- ✅ **Log Levels**: Support dynamic log level adjustment
- ✅ **Sensitive Data**: PII and confidential information 100% masked

#### Metrics System

- ✅ **Business Metrics**: Accurately reflect actual business operations
- ✅ **System Metrics**: Cover key components: JVM, HTTP, database
- ✅ **Custom Metrics**: Support business-specific metric definitions
- ✅ **Metric Tags**: Correctly configured dimension tags

#### Tracing System

- ✅ **Cross-service Tracing**: Completeness > 95%
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
- ✅ **Recovery Time**: Recovery time after load < 5 minutes

### 🔒 Security Acceptance Criteria

#### Data Protection

- ✅ **PII Masking**: Personal identifiable information 100% masked
- ✅ **Confidential Information**: API keys, passwords do not appear in logs
- ✅ **Data Transmission**: All data transmission uses TLS encryption
- ✅ **Access Control**: Role-based access control correctly configured

#### Compliance

- ✅ **Audit Logs**: Critical operations 100% recorded
- ✅ **Data Retention**: Automatic cleanup of expired data per policy
- ✅ **Regulatory Compliance**: Complies with GDPR, SOX regulations
- ✅ **Data Sovereignty**: Data storage complies with regional regulations

### 💰 Cost Acceptance Criteria

#### Cost Control

- ✅ **CloudWatch Cost**: < $100/month (based on expected load)
- ✅ **X-Ray Cost**: < $50/month
- ✅ **Storage Cost**: < $30/month
- ✅ **Total Cost**: Total observability cost < 5% of operational cost

#### Cost Optimization

- ✅ **Sampling Strategy**: Effectively reduces tracing costs
- ✅ **Data Retention**: Automatic cleanup reduces storage costs
- ✅ **Alert Configuration**: Timely alerts for cost anomalies
- ✅ **Usage Monitoring**: Regular review and optimization of usage##
 Best Practices

### 🏆 Development Best Practices

#### Logging Best Practices

1. **Use Structured Logging**: Always use JSON format
2. **Appropriate Log Levels**: Use INFO level and above in production
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

1. **Layered Monitoring**: Infrastructure, application, business three-layer monitoring
2. **Proactive Alerting**: Set alerts based on SLI/SLO
3. **Alert Fatigue**: Avoid too many meaningless alerts
4. **Root Cause Analysis**: Establish problem root cause analysis process
5. **Continuous Improvement**: Regularly review and optimize monitoring strategy

#### Cost Management

1. **Regular Review**: Monthly review of observability costs
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

## Industry Best Practices for Observability Testing

### 📊 **Real-world Industry Practices Analysis**

In practice, the industry **rarely** uses Java programs directly to test observability systems. Here are the mainstream industry practices:

### 🛠️ **1. Tool-based Testing Methods**

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
            url: "http://prometheus:9090/api/v1/query"
            method:
              get:
                criteria: "=="
                responseCode: "200"
```

### 🔧 **2. Script-based Testing Methods**

#### **Bash/Shell Script Testing**

```bash
#!/bin/bash
# observability_health_check.sh

# Check Prometheus metrics
check_prometheus_metrics() {
    echo "Checking Prometheus metrics..."
    
    # Check application metrics
    APP_METRICS=$(curl -s "http://prometheus:9090/api/v1/query?query=up{job=\"app\"}" | jq -r '.data.result[0].value[1]')
    if [ "$APP_METRICS" != "1" ]; then
        echo "❌ Application metrics not available"
        exit 1
    fi
    
    # Check error rate
    ERROR_RATE=$(curl -s "http://prometheus:9090/api/v1/query?query=rate(http_requests_total{status=~\"5..\"}[5m])" | jq -r '.data.result[0].value[1]')
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
        echo "❌ Log count too low: $LOG_COUNT"
        exit 1
    fi
    
    echo "✅ Log completeness verified"
}

# Main execution
main() {
    check_prometheus_metrics
    check_log_completeness
    echo "🎉 All observability checks passed!"
}

main "$@"
```

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
**Last Updated**: September 2025  
**Maintainer**: GenAI Demo Development Team  
**Reviewer**: Architecture Team, SRE Team