---
title: "Monitoring and Alerting"
viewpoint: "Operational"
status: "active"
last_updated: "2025-10-23"
stakeholders: ["SRE Team", "DevOps Engineers", "Operations Team", "Development Team"]
---

# Monitoring and Alerting

> **Viewpoint**: Operational  
> **Purpose**: Define comprehensive monitoring and alerting strategies for the E-Commerce Platform  
> **Audience**: SRE Team, DevOps Engineers, Operations Team, Development Team

## Overview

This document describes the monitoring and alerting infrastructure, metrics collection, dashboard design, and alert configuration for maintaining system health and performance.

## Monitoring Architecture

### High-Level Architecture

```text
┌─────────────────────────────────────────────────────────────────┐
│                    Application Layer                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │ Order    │  │ Customer │  │ Product  │  │ Payment  │       │
│  │ Service  │  │ Service  │  │ Service  │  │ Service  │       │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘       │
│       │             │             │             │               │
│       └─────────────┴─────────────┴─────────────┘               │
│                         │                                        │
└─────────────────────────┼────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Metrics Collection Layer                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  CloudWatch Agent (on each EKS node)                     │  │
│  │  • Collects system metrics (CPU, memory, disk, network) │  │
│  │  • Collects application logs                            │  │
│  │  • Sends to CloudWatch                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Prometheus (in EKS cluster)                             │  │
│  │  • Scrapes application metrics                           │  │
│  │  • Custom business metrics                               │  │
│  │  • Service mesh metrics (Istio)                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  X-Ray (distributed tracing)                             │  │
│  │  • Request tracing across services                       │  │
│  │  • Performance bottleneck identification                 │  │
│  │  • Service dependency mapping                            │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Storage and Analysis Layer                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │ CloudWatch   │  │ Prometheus   │  │ X-Ray        │         │
│  │ Metrics      │  │ TSDB         │  │ Traces       │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Visualization Layer                             │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Grafana Dashboards                                      │  │
│  │  • System health dashboard                               │  │
│  │  • Application performance dashboard                     │  │
│  │  • Business metrics dashboard                            │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  CloudWatch Dashboards                                   │  │
│  │  • Infrastructure metrics                                │  │
│  │  • AWS service metrics                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Alerting Layer                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │ CloudWatch   │  │ Prometheus   │  │ PagerDuty    │         │
│  │ Alarms       │  │ Alertmanager │  │              │         │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
│         │                  │                  │                  │
│         └──────────────────┴──────────────────┘                  │
│                            │                                      │
│                            ▼                                      │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Notification Channels                                   │  │
│  │  • PagerDuty (P1, P2)                                   │  │
│  │  • Slack (P2, P3, P4)                                   │  │
│  │  • Email (P3, P4)                                       │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Metrics Collection

### Infrastructure Metrics

#### EKS Cluster Metrics

**Node-Level Metrics**:

```yaml
Metrics:
  CPU:

    - node_cpu_utilization
    - node_cpu_limit
    - node_cpu_reserved_capacity

    Target: < 70% utilization
    
  Memory:

    - node_memory_utilization
    - node_memory_limit
    - node_memory_reserved_capacity

    Target: < 80% utilization
    
  Disk:

    - node_filesystem_utilization
    - node_filesystem_available

    Target: < 80% utilization
    
  Network:

    - node_network_total_bytes
    - node_network_rx_bytes
    - node_network_tx_bytes
    - node_network_rx_dropped
    - node_network_tx_dropped

```

**Pod-Level Metrics**:

```yaml
Metrics:
  CPU:

    - pod_cpu_utilization
    - pod_cpu_utilization_over_pod_limit

    Target: < 70% of limit
    
  Memory:

    - pod_memory_utilization
    - pod_memory_utilization_over_pod_limit

    Target: < 80% of limit
    
  Network:

    - pod_network_rx_bytes
    - pod_network_tx_bytes
    
  Status:

    - pod_number_of_container_restarts
    - pod_status_ready
    - pod_status_scheduled

```

#### Database Metrics (RDS)

```yaml
Metrics:
  Performance:

    - DatabaseConnections
    - CPUUtilization (Target: < 70%)
    - FreeableMemory (Target: > 20%)
    - ReadLatency (Target: < 10ms)
    - WriteLatency (Target: < 10ms)
    
  Throughput:

    - ReadThroughput
    - WriteThroughput
    - ReadIOPS
    - WriteIOPS
    
  Storage:

    - FreeStorageSpace (Target: > 20%)
    - DiskQueueDepth (Target: < 10)
    
  Replication:

    - ReplicaLag (Target: < 1 second)

```

#### Cache Metrics (ElastiCache Redis)

```yaml
Metrics:
  Performance:

    - CPUUtilization (Target: < 70%)
    - EngineCPUUtilization (Target: < 70%)
    - DatabaseMemoryUsagePercentage (Target: < 80%)
    
  Operations:

    - CacheHits
    - CacheMisses
    - CacheHitRate (Target: > 90%)
    - Evictions (Target: < 100/min)
    
  Network:

    - NetworkBytesIn
    - NetworkBytesOut
    - NetworkPacketsIn
    - NetworkPacketsOut
    
  Connections:

    - CurrConnections
    - NewConnections

```

#### Message Queue Metrics (MSK Kafka)

```yaml
Metrics:
  Broker:

    - CpuUser (Target: < 70%)
    - MemoryUsed (Target: < 80%)
    - KafkaDataLogsDiskUsed (Target: < 80%)
    
  Throughput:

    - BytesInPerSec
    - BytesOutPerSec
    - MessagesInPerSec
    
  Consumer Lag:

    - MaxOffsetLag (Target: < 1000)
    - SumOffsetLag
    
  Partitions:

    - UnderReplicatedPartitions (Target: 0)
    - OfflinePartitionsCount (Target: 0)

```

### Application Metrics

#### API Metrics

**Request Metrics**:

```java
// Spring Boot Actuator + Micrometer
@RestController
public class OrderController {
    
    private final MeterRegistry meterRegistry;
    
    @GetMapping("/api/v1/orders/{id}")
    @Timed(value = "api.orders.get", description = "Get order by ID")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String id) {
        // Automatic metrics:
        // - api.orders.get.count (counter)
        // - api.orders.get.sum (timer)
        // - api.orders.get.max (timer)
        
        Counter.builder("api.orders.get.requests")
            .tag("endpoint", "/orders/{id}")
            .tag("method", "GET")
            .register(meterRegistry)
            .increment();
        
        // Business logic
        return ResponseEntity.ok(orderService.getOrder(id));
    }
}
```

**Metrics Collected**:

```yaml
API Metrics:
  Request Rate:

    - http_server_requests_seconds_count
    - Grouped by: endpoint, method, status
    
  Response Time:

    - http_server_requests_seconds_sum
    - http_server_requests_seconds_max
    - Percentiles: p50, p95, p99
    
  Error Rate:

    - http_server_requests_errors_total
    - Grouped by: endpoint, error_type
    
  Active Requests:

    - http_server_requests_active

```

#### Business Metrics

**Order Service Metrics**:

```java
@Component
public class OrderMetrics {
    
    private final Counter ordersCreated;
    private final Counter ordersCompleted;
    private final Counter ordersCancelled;
    private final Timer orderProcessingTime;
    private final Gauge activeOrders;
    
    public OrderMetrics(MeterRegistry registry) {
        this.ordersCreated = Counter.builder("orders.created")
            .description("Total orders created")
            .tag("service", "order")
            .register(registry);
            
        this.ordersCompleted = Counter.builder("orders.completed")
            .description("Total orders completed")
            .tag("service", "order")
            .register(registry);
            
        this.ordersCancelled = Counter.builder("orders.cancelled")
            .description("Total orders cancelled")
            .tag("service", "order")
            .register(registry);
            
        this.orderProcessingTime = Timer.builder("orders.processing.time")
            .description("Order processing time")
            .tag("service", "order")
            .register(registry);
            
        this.activeOrders = Gauge.builder("orders.active")
            .description("Number of active orders")
            .tag("service", "order")
            .register(registry, this, OrderMetrics::getActiveOrderCount);
    }
    
    private double getActiveOrderCount() {
        return orderRepository.countByStatus(OrderStatus.ACTIVE);
    }
}
```

**Business Metrics Collected**:

```yaml
Order Metrics:

  - orders.created (counter)
  - orders.completed (counter)
  - orders.cancelled (counter)
  - orders.processing.time (timer)
  - orders.active (gauge)
  - orders.revenue.total (gauge)
  
Customer Metrics:

  - customers.registered (counter)
  - customers.active (gauge)
  - customers.login.success (counter)
  - customers.login.failed (counter)
  
Product Metrics:

  - products.viewed (counter)
  - products.added_to_cart (counter)
  - products.purchased (counter)
  - products.out_of_stock (gauge)
  
Payment Metrics:

  - payments.initiated (counter)
  - payments.successful (counter)
  - payments.failed (counter)
  - payments.processing.time (timer)

```

### Distributed Tracing

#### X-Ray Configuration

**Spring Boot Integration**:

```java
@Configuration
public class XRayConfiguration {
    
    @Bean
    public Filter TracingFilter() {
        return new AWSXRayServletFilter("ECommerceApp");
    }
    
    @Bean
    public AWSXRayRecorder awsXRayRecorder() {
        return AWSXRayRecorderBuilder
            .standard()
            .withPlugin(new EKSPlugin())
            .withPlugin(new EC2Plugin())
            .build();
    }
}

// Usage in service
@Service
public class OrderService {
    
    @XRayEnabled
    public Order createOrder(CreateOrderCommand command) {
        Subsegment subsegment = AWSXRay.beginSubsegment("createOrder");
        try {
            subsegment.putAnnotation("customerId", command.getCustomerId());
            subsegment.putMetadata("orderDetails", command);
            
            // Business logic
            Order order = processOrder(command);
            
            subsegment.putAnnotation("orderId", order.getId());
            return order;
        } finally {
            AWSXRay.endSubsegment();
        }
    }
}
```

**Trace Data Collected**:

```yaml
Trace Information:

  - Request ID
  - Service name
  - Operation name
  - Start time and duration
  - HTTP method and URL
  - Response status code
  - Error information (if any)
  - Custom annotations
  - Custom metadata
  
Service Map:

  - Service dependencies
  - Call patterns
  - Error rates per service
  - Latency per service

```

## Dashboard Design

### System Health Dashboard

**Purpose**: Overall system health at a glance

**Panels**:

1. **System Status** (Top Row)
   - Overall system status (Green/Yellow/Red)
   - Active incidents count
   - Current error rate
   - Current response time (p95)

2. **Service Health** (Second Row)
   - Service availability (per service)
   - Service error rate (per service)
   - Service response time (per service)

3. **Infrastructure Health** (Third Row)
   - EKS cluster health
   - RDS database health
   - Redis cache health
   - Kafka broker health

4. **Key Metrics** (Fourth Row)
   - Requests per second
   - Active users
   - Orders per minute
   - Revenue per hour

**Grafana Dashboard JSON**:

```json
{
  "dashboard": {
    "title": "System Health Dashboard",
    "panels": [
      {
        "id": 1,
        "title": "Overall System Status",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job=\"order-service\"} == 1",
            "legendFormat": "Order Service"
          }
        ]
      },
      {
        "id": 2,
        "title": "Error Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_errors_total[5m])",
            "legendFormat": "{{service}}"
          }
        ]
      }
    ]
  }
}
```

### Application Performance Dashboard

**Purpose**: Monitor application performance metrics

**Panels**:

1. **API Performance**
   - Request rate (requests/second)
   - Response time (p50, p95, p99)
   - Error rate (%)
   - Active requests

2. **Database Performance**
   - Query response time
   - Connection pool usage
   - Slow queries count
   - Transaction rate

3. **Cache Performance**
   - Cache hit rate
   - Cache miss rate
   - Eviction rate
   - Memory usage

4. **Event Processing**
   - Events published/second
   - Events consumed/second
   - Consumer lag
   - Processing time

### Business Metrics Dashboard

**Purpose**: Track key business metrics

**Panels**:

1. **Orders**
   - Orders created (per hour)
   - Orders completed (per hour)
   - Order completion rate
   - Average order value

2. **Customers**
   - Active users
   - New registrations
   - Login success rate
   - Customer retention rate

3. **Revenue**
   - Revenue per hour
   - Revenue per day
   - Revenue by product category
   - Revenue by region

4. **Conversion**
   - Conversion rate
   - Cart abandonment rate
   - Checkout completion rate
   - Payment success rate

## Alert Configuration

### Critical Alerts (P1)

#### Service Down Alert

```yaml
Alert: ServiceDown
Severity: Critical (P1)
Condition: up{job="order-service"} == 0
Duration: 1 minute
Notification: PagerDuty + Phone + Slack
Runbook: https://runbooks.ecommerce-platform.com/service-down

Description: |
  Service {{$labels.service}} is down.
  
Actions:

  1. Check service logs
  2. Check pod status
  3. Check recent deployments
  4. Rollback if necessary
  
Escalation:

  - 5 minutes: Escalate to senior engineer
  - 15 minutes: Escalate to engineering manager

```

#### High Error Rate Alert

```yaml
Alert: HighErrorRate
Severity: Critical (P1)
Condition: |
  rate(http_server_requests_errors_total[5m]) / 
  rate(http_server_requests_total[5m]) > 0.05
Duration: 5 minutes
Notification: PagerDuty + Slack
Runbook: https://runbooks.ecommerce-platform.com/high-error-rate

Description: |
  Error rate for {{$labels.service}} is {{$value}}% (threshold: 5%)
  
Actions:

  1. Check error logs
  2. Check recent changes
  3. Check external dependencies
  4. Consider rollback

```

#### Database Connection Failure

```yaml
Alert: DatabaseConnectionFailure
Severity: Critical (P1)
Condition: aws_rds_database_connections < 1
Duration: 1 minute
Notification: PagerDuty + Phone + Slack
Runbook: https://runbooks.ecommerce-platform.com/database-connection-failure

Description: |
  Unable to connect to database {{$labels.database}}
  
Actions:

  1. Check RDS instance status
  2. Check security groups
  3. Check network connectivity
  4. Check connection pool configuration

```

### High Priority Alerts (P2)

#### High Response Time Alert

```yaml
Alert: HighResponseTime
Severity: High (P2)
Condition: |
  histogram_quantile(0.95, 
    rate(http_server_requests_seconds_bucket[5m])
  ) > 1
Duration: 5 minutes
Notification: PagerDuty + Slack
Runbook: https://runbooks.ecommerce-platform.com/high-response-time

Description: |
  95th percentile response time for {{$labels.endpoint}} is {{$value}}s
  
Actions:

  1. Check database query performance
  2. Check cache hit rate
  3. Check external API latency
  4. Review recent code changes

```

#### High Memory Usage Alert

```yaml
Alert: HighMemoryUsage
Severity: High (P2)
Condition: |
  (node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / 
  node_memory_MemTotal_bytes > 0.90
Duration: 10 minutes
Notification: PagerDuty + Slack
Runbook: https://runbooks.ecommerce-platform.com/high-memory-usage

Description: |
  Memory usage on {{$labels.instance}} is {{$value}}%
  
Actions:

  1. Check for memory leaks
  2. Review application logs
  3. Consider scaling up
  4. Restart pods if necessary

```

### Medium Priority Alerts (P3)

#### Elevated Error Rate Alert

```yaml
Alert: ElevatedErrorRate
Severity: Medium (P3)
Condition: |
  rate(http_server_requests_errors_total[5m]) / 
  rate(http_server_requests_total[5m]) > 0.01
Duration: 10 minutes
Notification: Slack
Runbook: https://runbooks.ecommerce-platform.com/elevated-error-rate

Description: |
  Error rate for {{$labels.service}} is {{$value}}% (threshold: 1%)
  
Actions:

  1. Monitor error trends
  2. Review error logs
  3. Investigate if rate increases

```

#### Low Cache Hit Rate Alert

```yaml
Alert: LowCacheHitRate
Severity: Medium (P3)
Condition: |
  redis_keyspace_hits_total / 
  (redis_keyspace_hits_total + redis_keyspace_misses_total) < 0.80
Duration: 15 minutes
Notification: Slack
Runbook: https://runbooks.ecommerce-platform.com/low-cache-hit-rate

Description: |
  Cache hit rate is {{$value}}% (threshold: 80%)
  
Actions:

  1. Review cache configuration
  2. Check cache key patterns
  3. Consider cache warming
  4. Review TTL settings

```

### Low Priority Alerts (P4)

#### Disk Space Warning

```yaml
Alert: DiskSpaceWarning
Severity: Low (P4)
Condition: |
  (node_filesystem_size_bytes - node_filesystem_free_bytes) / 
  node_filesystem_size_bytes > 0.80
Duration: 30 minutes
Notification: Email + Slack
Runbook: https://runbooks.ecommerce-platform.com/disk-space-warning

Description: |
  Disk usage on {{$labels.instance}} is {{$value}}%
  
Actions:

  1. Review disk usage trends
  2. Clean up old logs
  3. Plan for capacity increase

```

## Alert Management

### Alert Routing

**PagerDuty Integration**:

```yaml
Routing Rules:
  Critical (P1):

    - Primary: On-call SRE
    - Escalation (5 min): Senior SRE
    - Escalation (15 min): Engineering Manager
    - Notification: Phone + SMS + Push
    
  High (P2):

    - Primary: On-call SRE
    - Escalation (15 min): Senior SRE
    - Notification: Push + SMS
    
  Medium (P3):

    - Primary: On-call SRE
    - No escalation
    - Notification: Push only
    
  Low (P4):

    - Primary: Ops team
    - No escalation
    - Notification: Email

```

### Alert Suppression

**Maintenance Windows**:

```yaml
Suppression Rules:

  - name: "Scheduled Maintenance"

    schedule: "Sunday 02:00-04:00 EST"
    suppress:

      - ServiceDown
      - HighErrorRate
      - HighResponseTime
    
  - name: "Deployment Window"

    trigger: "deployment_in_progress == true"
    suppress:

      - ServiceDown (for 5 minutes)
      - HighErrorRate (for 5 minutes)

```

### Alert Aggregation

**Grouping Rules**:

```yaml
Aggregation:

  - name: "Service Errors"

    group_by: ["service", "error_type"]
    group_wait: 30s
    group_interval: 5m
    repeat_interval: 4h
    
  - name: "Infrastructure Issues"

    group_by: ["instance", "issue_type"]
    group_wait: 1m
    group_interval: 10m
    repeat_interval: 12h
```

## Monitoring Best Practices

### Metric Naming Conventions

```yaml
Format: <namespace>.<subsystem>.<metric_name>.<unit>

Examples:

  - api.orders.requests.total (counter)
  - api.orders.response.time.seconds (histogram)
  - database.connections.active (gauge)
  - cache.hits.total (counter)
  - events.processed.total (counter)

```

### Dashboard Best Practices

1. **Keep it Simple**: Focus on key metrics
2. **Use Consistent Colors**: Red for errors, green for success, yellow for warnings
3. **Show Trends**: Include time-series graphs
4. **Add Context**: Include thresholds and targets
5. **Group Related Metrics**: Organize by service or function
6. **Update Regularly**: Review and update dashboards quarterly

### Alert Best Practices

1. **Actionable Alerts**: Every alert should require action
2. **Clear Messages**: Include context and next steps
3. **Appropriate Severity**: Don't over-alert
4. **Include Runbooks**: Link to troubleshooting guides
5. **Regular Review**: Review and tune alerts monthly
6. **Avoid Alert Fatigue**: Suppress noisy alerts

## Related Documentation

- [Operational Overview](overview.md) - Overall operational approach
- [Backup and Recovery](backup-recovery.md) - Backup and recovery procedures
- [Operational Procedures](procedures.md) - Step-by-step procedures
- [Deployment Process](../deployment/deployment-process.md) - Deployment and rollback

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-23  
**Owner**: SRE Team
