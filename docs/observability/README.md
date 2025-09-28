# Observability Guide

## Overview

This document provides a comprehensive observability implementation guide, including monitoring, logging, tracing, and alerting configuration and best practices.

## Monitoring Architecture

### Core Components
- **Prometheus**: Metrics collection and storage
- **Grafana**: Visualization dashboards
- **AWS X-Ray**: Distributed tracing
- **CloudWatch**: AWS native monitoring

### Application Monitoring
- **Health Checks**: `/actuator/health`
- **Metrics Endpoint**: `/actuator/metrics`
- **Prometheus Endpoint**: `/actuator/prometheus`

### Infrastructure Monitoring
- **Container Metrics**: CPU, memory, network, disk usage
- **Kubernetes Metrics**: Pod status, resource utilization, cluster health
- **Database Metrics**: Connection pools, query performance, lock waits
- **Network Metrics**: Latency, throughput, error rates

## Logging Management

### Structured Logging
- **Format**: JSON structured logs
- **Levels**: ERROR, WARN, INFO, DEBUG, TRACE
- **Context**: Trace ID, User ID, Request ID, Session ID

### Log Aggregation
- **Local Development**: Console output with colored formatting
- **Test Environment**: CloudWatch Logs with structured queries
- **Production Environment**: ELK Stack or CloudWatch Insights with advanced analytics

### Log Configuration
```yaml
# application.yml
logging:
  level:
    solid.humank.genaidemo: INFO
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{traceId},%X{spanId}] %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{traceId},%X{spanId}] %logger{36} - %msg%n"
  file:
    name: logs/application.log
    max-size: 100MB
    max-history: 30
```

## Distributed Tracing

### AWS X-Ray Integration
- **Automatic Tracing**: HTTP requests, database queries, external service calls
- **Custom Tracing**: Business logic trace points and custom segments
- **Performance Analysis**: Request latency and bottleneck identification
- **Error Tracking**: Exception propagation and error correlation

### Trace Context Propagation
```java
@Component
public class TraceContextFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        String traceId = generateOrExtractTraceId(request);
        String spanId = generateSpanId();
        
        try (MDCCloseable mdcCloseable = MDC.putCloseable("traceId", traceId)) {
            MDC.put("spanId", spanId);
            MDC.put("userId", getCurrentUserId());
            
            chain.doFilter(request, response);
        }
    }
}
```

### Custom Tracing
```java
@Service
public class OrderService {
    
    @XRayEnabled
    @Traced(operationName = "process-order")
    public Order processOrder(CreateOrderCommand command) {
        Span span = tracer.nextSpan()
            .name("order-processing")
            .tag("order.type", command.getType())
            .tag("customer.id", command.getCustomerId())
            .start();
            
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            // Business logic with automatic tracing
            return orderProcessor.process(command);
        } finally {
            span.end();
        }
    }
}
```

## Alerting Configuration

### Critical Metrics Alerts
- **Response Time**: 95th percentile > 2s
- **Error Rate**: > 1% over 5 minutes
- **Availability**: < 99.9% uptime
- **Resource Usage**: CPU > 80%, Memory > 85%

### Alert Channels
- **Immediate Notifications**: Slack/Teams integration
- **Incident Management**: PagerDuty for critical alerts
- **Email Notifications**: Non-urgent alerts and summaries
- **SMS Alerts**: Critical production issues

### Alert Rules Configuration
```yaml
# Prometheus Alert Rules
groups:
  - name: application-alerts
    rules:
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 2
        for: 2m
        labels:
          severity: warning
          team: backend
        annotations:
          summary: "High response time detected"
          description: "95th percentile response time is {{ $value }} seconds"
          
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.01
        for: 2m
        labels:
          severity: critical
          team: backend
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} errors per second"
```

## Dashboards

### Technical Dashboards

#### Application Performance Dashboard
- **Response Time Metrics**: Average, 95th, 99th percentiles
- **Throughput Metrics**: Requests per second, concurrent users
- **Error Metrics**: Error rates by endpoint and status code
- **Resource Utilization**: JVM memory, garbage collection, thread pools

#### Infrastructure Dashboard
- **Container Metrics**: CPU, memory, network, disk I/O
- **Kubernetes Metrics**: Pod status, resource requests/limits, cluster capacity
- **Database Metrics**: Connection pool usage, query performance, replication lag
- **Network Metrics**: Latency, packet loss, bandwidth utilization

### Business Dashboards

#### Key Business Metrics
- **Order Metrics**: Order volume, conversion rates, average order value
- **User Activity**: Active users, session duration, page views
- **Revenue Metrics**: Daily/monthly revenue, payment success rates
- **Customer Metrics**: New registrations, churn rate, customer lifetime value

#### Business Process Health
- **Registration Funnel**: Step completion rates, drop-off points
- **Purchase Process**: Cart abandonment, payment failures, fulfillment times
- **Customer Support**: Ticket volume, resolution times, satisfaction scores

### Dashboard Configuration
```json
{
  "dashboard": {
    "title": "Application Performance",
    "panels": [
      {
        "title": "Response Time",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))",
            "legendFormat": "95th Percentile"
          }
        ]
      }
    ]
  }
}
```

## Metrics Collection

### Application Metrics
```java
@Component
public class BusinessMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter ordersCreated;
    private final Timer orderProcessingTime;
    private final Gauge activeUsers;
    
    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.ordersCreated = Counter.builder("orders.created")
            .description("Total number of orders created")
            .register(meterRegistry);
        this.orderProcessingTime = Timer.builder("orders.processing.time")
            .description("Time taken to process orders")
            .register(meterRegistry);
        this.activeUsers = Gauge.builder("users.active")
            .description("Number of active users")
            .register(meterRegistry, this, BusinessMetrics::getActiveUserCount);
    }
    
    public void recordOrderCreated(String orderType) {
        ordersCreated.increment(Tags.of("type", orderType));
    }
    
    public Timer.Sample startOrderProcessing() {
        return Timer.start(meterRegistry);
    }
}
```

### Custom Metrics
```java
@RestController
public class OrderController {
    
    @Timed(name = "orders.create", description = "Time taken to create order")
    @Counted(name = "orders.requests", description = "Number of order requests")
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        // Controller logic with automatic metrics
        return ResponseEntity.ok(orderService.createOrder(request));
    }
}
```

## Health Checks

### Application Health Checks
```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // Check database connectivity
            databaseService.ping();
            
            // Check external dependencies
            externalService.healthCheck();
            
            // Check business logic health
            validateBusinessRules();
            
            return Health.up()
                .withDetail("database", "UP")
                .withDetail("external-service", "UP")
                .withDetail("business-rules", "VALID")
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### Infrastructure Health Checks
```yaml
# Kubernetes Liveness and Readiness Probes
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

## Performance Monitoring

### Response Time Monitoring
- **API Endpoints**: Individual endpoint performance tracking
- **Database Queries**: Query execution time and optimization
- **External Services**: Third-party service response times
- **Background Jobs**: Asynchronous task performance

### Resource Monitoring
- **JVM Metrics**: Heap usage, garbage collection, thread pools
- **Container Resources**: CPU, memory, network, disk utilization
- **Database Resources**: Connection pools, query cache, buffer pools
- **Network Resources**: Bandwidth, latency, packet loss

## Security Monitoring

### Security Events
- **Authentication Failures**: Failed login attempts, suspicious patterns
- **Authorization Violations**: Unauthorized access attempts
- **Data Access**: Sensitive data access patterns
- **API Abuse**: Rate limiting violations, suspicious requests

### Security Metrics
```java
@Component
public class SecurityMetrics {
    
    @EventListener
    public void recordAuthenticationFailure(AuthenticationFailureEvent event) {
        Counter.builder("security.authentication.failures")
            .tag("reason", event.getReason())
            .tag("source", event.getSourceIp())
            .register(meterRegistry)
            .increment();
    }
    
    @EventListener
    public void recordSuspiciousActivity(SuspiciousActivityEvent event) {
        Counter.builder("security.suspicious.activity")
            .tag("type", event.getActivityType())
            .tag("severity", event.getSeverity())
            .register(meterRegistry)
            .increment();
    }
}
```

## Troubleshooting and Debugging

### Log Analysis
```bash
# Search for errors in logs
kubectl logs -f deployment/app-deployment | grep ERROR

# Filter logs by trace ID
kubectl logs deployment/app-deployment | grep "traceId:abc123"

# Analyze performance issues
kubectl logs deployment/app-deployment | grep "duration>" | sort -k5 -nr
```

### Metrics Analysis
```bash
# Query Prometheus metrics
curl 'http://prometheus:9090/api/v1/query?query=rate(http_requests_total[5m])'

# Check application health
curl http://app-service:8080/actuator/health

# View detailed metrics
curl http://app-service:8080/actuator/metrics/jvm.memory.used
```

### Distributed Tracing Analysis
- **Trace Visualization**: AWS X-Ray console for trace analysis
- **Performance Bottlenecks**: Identify slow components in request flow
- **Error Correlation**: Link errors across service boundaries
- **Dependency Mapping**: Visualize service dependencies and call patterns

## Configuration Management

### Environment-Specific Configuration
```yaml
# application-production.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true
      cloudwatch:
        enabled: true
        namespace: GenAIDemo/Production
```

### Monitoring Configuration
```yaml
# monitoring-config.yml
prometheus:
  scrape_configs:
    - job_name: 'genai-demo'
      static_configs:
        - targets: ['app-service:8080']
      metrics_path: '/actuator/prometheus'
      scrape_interval: 15s

grafana:
  datasources:
    - name: Prometheus
      type: prometheus
      url: http://prometheus:9090
    - name: CloudWatch
      type: cloudwatch
      jsonData:
        defaultRegion: us-west-2
```

## Related Documentation

- [Deployment Guide](../deployment/README.md) - Infrastructure and deployment setup
- Monitoring Configuration - Detailed monitoring architecture
- [Infrastructure Configuration](../deployment/aws-eks-architecture.md) - AWS EKS monitoring setup
- [Troubleshooting Guide](../troubleshooting/observability-troubleshooting.md) - Common observability issues

## Best Practices

### Monitoring Best Practices
1. **Monitor What Matters**: Focus on business-critical metrics
2. **Set Meaningful Alerts**: Avoid alert fatigue with actionable alerts
3. **Use SLIs and SLOs**: Define service level indicators and objectives
4. **Implement Gradual Rollouts**: Monitor during deployments

### Logging Best Practices
1. **Structured Logging**: Use consistent JSON format
2. **Contextual Information**: Include trace IDs and user context
3. **Appropriate Log Levels**: Use correct log levels for different scenarios
4. **Log Retention**: Implement appropriate retention policies

### Tracing Best Practices
1. **Trace Critical Paths**: Focus on important business flows
2. **Add Business Context**: Include relevant business information in traces
3. **Optimize Sampling**: Balance detail with performance impact
4. **Correlate with Logs**: Link traces with log entries

---

**Maintainer**: DevOps Team  
**Last Updated**: January 22, 2025  
**Version**: 1.1

**Quick Links**:
- [Monitoring Setup](configuration-guide.md) - Step-by-step monitoring configuration
- [Dashboard Templates](../deployment/observability-deployment.md) - Pre-built dashboard configurations
- [Alert Runbooks](../troubleshooting/observability-troubleshooting.md) - Alert response procedures
