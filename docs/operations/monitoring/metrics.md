# Metrics and Monitoring

> **Status**: ✅ Active  
> **Last Updated**: 2024-11-19

## Overview

This document describes the metrics collection and monitoring strategy for the GenAI Demo platform.

---

## Quick Reference

For related operational guides, see:
- [Monitoring Overview](README.md) - Monitoring index
- [Troubleshooting](../troubleshooting/README.md) - Debugging guides
- [Performance Standards](.kiro/steering/performance-standards.md) - Performance requirements

---

## Metrics Architecture

### Components

```
Application → Micrometer → Prometheus → Grafana
                ↓
            CloudWatch
                ↓
            X-Ray Traces
```

### Metrics Types

1. **Application Metrics**: Business and technical metrics
2. **Infrastructure Metrics**: System resources
3. **Custom Metrics**: Domain-specific measurements

---

## Application Metrics

### Spring Boot Actuator

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: genai-demo
      environment: ${ENVIRONMENT}
```

### Custom Metrics

```java
@Component
public class BusinessMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // Counter: Monotonically increasing value
    private final Counter ordersCreated;
    
    // Gauge: Current value that can go up or down
    private final AtomicInteger activeUsers = new AtomicInteger(0);
    
    // Timer: Duration and rate of events
    private final Timer orderProcessingTime;
    
    // Distribution Summary: Distribution of values
    private final DistributionSummary orderAmount;
    
    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Register counter
        this.ordersCreated = Counter.builder("orders.created")
            .description("Total number of orders created")
            .tag("type", "business")
            .register(meterRegistry);
        
        // Register gauge
        Gauge.builder("users.active", activeUsers, AtomicInteger::get)
            .description("Number of active users")
            .register(meterRegistry);
        
        // Register timer
        this.orderProcessingTime = Timer.builder("order.processing.time")
            .description("Time taken to process an order")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
        
        // Register distribution summary
        this.orderAmount = DistributionSummary.builder("order.amount")
            .description("Distribution of order amounts")
            .baseUnit("USD")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }
    
    public void recordOrderCreated(Order order) {
        ordersCreated.increment();
        orderAmount.record(order.getTotalAmount().doubleValue());
    }
    
    public void recordOrderProcessing(Runnable orderProcessing) {
        orderProcessingTime.record(orderProcessing);
    }
    
    public void incrementActiveUsers() {
        activeUsers.incrementAndGet();
    }
    
    public void decrementActiveUsers() {
        activeUsers.decrementAndGet();
    }
}
```

---

## Key Metrics

### Business Metrics

```java
// Orders
orders.created.total                    // Total orders created
orders.completed.total                  // Total orders completed
orders.cancelled.total                  // Total orders cancelled
orders.amount.sum                       // Total order value
orders.amount.percentile{p=0.95}       // 95th percentile order value

// Customers
customers.registered.total              // Total customers registered
customers.active.count                  // Active customers
customers.churn.rate                    // Customer churn rate

// Products
products.views.total                    // Product page views
products.added_to_cart.total           // Products added to cart
products.purchased.total               // Products purchased
```

### Technical Metrics

```java
// HTTP Requests
http.server.requests.count              // Total HTTP requests
http.server.requests.duration          // Request duration
http.server.requests.errors            // HTTP errors

// Database
hikaricp.connections.active            // Active DB connections
hikaricp.connections.pending           // Pending connections
hikaricp.connections.timeout           // Connection timeouts

// JVM
jvm.memory.used                        // JVM memory usage
jvm.memory.max                         // JVM max memory
jvm.gc.pause                           // GC pause time
jvm.threads.live                       // Live threads

// Cache
cache.gets.total                       // Cache get operations
cache.puts.total                       // Cache put operations
cache.evictions.total                  // Cache evictions
cache.hit.ratio                        // Cache hit ratio
```

---

## Prometheus Configuration

### Scrape Configuration

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'genai-demo'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
            - production
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
        target_label: __address__
```

### Recording Rules

```yaml
# recording_rules.yml
groups:
  - name: business_metrics
    interval: 30s
    rules:
      - record: order:creation:rate5m
        expr: rate(orders_created_total[5m])
      
      - record: order:amount:avg5m
        expr: avg_over_time(order_amount_sum[5m]) / avg_over_time(orders_created_total[5m])
      
      - record: http:request:error_rate5m
        expr: rate(http_server_requests_count{status=~"5.."}[5m]) / rate(http_server_requests_count[5m])
```

---

## Grafana Dashboards

### Application Dashboard

```json
{
  "dashboard": {
    "title": "GenAI Demo - Application Metrics",
    "panels": [
      {
        "title": "Request Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_count[5m])",
            "legendFormat": "{{method}} {{uri}}"
          }
        ]
      },
      {
        "title": "Response Time (95th percentile)",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_server_requests_duration_bucket[5m]))",
            "legendFormat": "{{uri}}"
          }
        ]
      },
      {
        "title": "Error Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_count{status=~\"5..\"}[5m])",
            "legendFormat": "{{status}}"
          }
        ]
      }
    ]
  }
}
```

### Business Dashboard

```json
{
  "dashboard": {
    "title": "GenAI Demo - Business Metrics",
    "panels": [
      {
        "title": "Orders Created",
        "targets": [
          {
            "expr": "rate(orders_created_total[5m])",
            "legendFormat": "Orders/sec"
          }
        ]
      },
      {
        "title": "Revenue",
        "targets": [
          {
            "expr": "rate(orders_amount_sum[5m])",
            "legendFormat": "Revenue/sec"
          }
        ]
      },
      {
        "title": "Active Users",
        "targets": [
          {
            "expr": "users_active_count",
            "legendFormat": "Active Users"
          }
        ]
      }
    ]
  }
}
```

---

## Alerting

### Alert Rules

```yaml
# alert_rules.yml
groups:
  - name: application_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_count{status=~"5.."}[5m]) > 0.05
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} errors/sec"
      
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_server_requests_duration_bucket[5m])) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High response time detected"
          description: "95th percentile response time is {{ $value }}s"
      
      - alert: DatabaseConnectionPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool nearly exhausted"
      
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High JVM memory usage"
```

---

## CloudWatch Integration

### Custom Metrics

```java
@Component
public class CloudWatchMetrics {
    
    private final AmazonCloudWatch cloudWatch;
    
    public void publishMetric(String metricName, double value, String unit) {
        PutMetricDataRequest request = new PutMetricDataRequest()
            .withNamespace("GenAIDemo/Application")
            .withMetricData(new MetricDatum()
                .withMetricName(metricName)
                .withValue(value)
                .withUnit(unit)
                .withTimestamp(new Date())
                .withDimensions(
                    new Dimension()
                        .withName("Environment")
                        .withValue(environment),
                    new Dimension()
                        .withName("Application")
                        .withValue("genai-demo")
                ));
        
        cloudWatch.putMetricData(request);
    }
}
```

---

## Best Practices

### Metric Naming

✅ **Do**:
- Use descriptive names: `orders.created.total`
- Use consistent naming: `resource.action.result`
- Include units: `order.amount.usd`
- Use tags for dimensions: `{status="success"}`

❌ **Don't**:
- Use abbreviations: `ord.crt.tot`
- Mix naming styles
- Omit units
- Use high-cardinality tags

### Performance

✅ **Do**:
- Use counters for totals
- Use gauges for current values
- Use timers for durations
- Sample high-frequency metrics

❌ **Don't**:
- Create metrics in loops
- Use unbounded tag values
- Record every single event
- Create too many metrics

---

## Related Documentation

- [Monitoring Overview](README.md)
- [Performance Standards](.kiro/steering/performance-standards.md)
- [Troubleshooting Guide](../troubleshooting/debugging-guide.md)
- [Operational Runbooks](../runbooks/README.md)

---

**Document Version**: 1.0  
**Last Updated**: 2024-11-19  
**Owner**: Operations Team
