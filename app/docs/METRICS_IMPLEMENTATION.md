# Metrics Collection and Monitoring Implementation

This document describes the comprehensive metrics collection and monitoring system implemented for the GenAI Demo application.

## Overview

The metrics system provides multi-layered observability:

- **Application Metrics**: Spring Boot Actuator + Micrometer
- **Business Metrics**: Custom domain-specific KPIs
- **Infrastructure Metrics**: Kubernetes and AWS CloudWatch
- **Visualization**: Grafana dashboards with Prometheus and CloudWatch data sources

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Application   │───▶│    Micrometer    │───▶│   Prometheus    │
│   (Spring Boot) │    │   (Metrics)      │    │   (Storage)     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
                       ┌──────────────────┐    ┌─────────────────┐
                       │   CloudWatch     │    │     Grafana     │
                       │   (AWS Metrics)  │    │ (Visualization) │
                       └──────────────────┘    └─────────────────┘
```

## Components

### 1. Micrometer Configuration (`MetricsConfiguration.java`)

**Features:**

- Prometheus registry for all environments
- CloudWatch registry for production only
- Common tags for application, environment, and region
- Metric filtering to reduce noise
- Custom business metrics collectors

**Key Configurations:**

```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
        step: 10s
      cloudwatch:
        enabled: true  # Production only
        namespace: GenAIDemo/Application
        batch-size: 20
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
```

### 2. Business Metrics Collector (`BusinessMetricsCollector.java`)

**Tracked Metrics:**

**Counters:**

- `business.customer.registrations` - Total customer registrations
- `business.order.submissions` - Total order submissions
- `business.order.completions` - Total completed orders
- `business.payment.successes` - Successful payments
- `business.payment.failures` - Failed payments
- `business.inventory.updates` - Inventory updates

**Timers:**

- `business.order.processing.time` - Order processing duration
- `business.payment.processing.time` - Payment processing duration
- `business.inventory.check.time` - Inventory check duration

**Gauges:**

- `business.customer.active` - Current active customers
- `business.order.pending` - Current pending orders
- `business.inventory.low_stock` - Items with low stock

### 3. Domain Event Metrics (`DomainEventMetricsCollector.java`)

**Tracked Metrics:**

- `domain.events.published` - Events published by type and aggregate
- `domain.events.processed` - Events processed successfully
- `domain.events.processing.errors` - Event processing failures
- `domain.events.processing.time` - Event processing duration

**Automatic Instrumentation:**

- Listens to `DomainEventWrapper` events
- Automatically tags by event type and aggregate type
- Tracks processing success/failure rates

### 4. Metrics Aspect (`MetricsAspect.java`)

**Automatic Instrumentation:**

- Application services: `application.service.execution.time`
- Domain services: `domain.service.execution.time`
- Repository operations: `repository.operation.time`

**Smart Business Metrics:**

- Automatically increments business counters based on method names
- Tracks success/failure rates
- Correlates technical metrics with business outcomes

### 5. CloudWatch Integration

**Production Configuration:**

- Publishes metrics to `GenAIDemo/Application` namespace
- Custom business metrics to `GenAIDemo/Business` namespace
- Async publishing for performance
- Batch processing for cost optimization

**Custom Publisher (`CloudWatchBusinessMetricsPublisher.java`):**

- Domain-specific metric publishing
- Custom dimensions for filtering
- Async processing with error handling
- Separate methods for different business domains

## Kubernetes Deployment

### Prometheus Setup

**Features:**

- Service discovery for Kubernetes pods
- Automatic scraping of `/actuator/prometheus` endpoints
- Alert rules for application health
- Persistent storage for metrics data

**Key Configuration:**

```yaml
scrape_configs:
  - job_name: 'genai-demo-app'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names: [genai-demo]
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
```

### Grafana Dashboards

**Application Dashboard:**

- Application health status
- HTTP request rates and response times
- Business metrics (registrations, orders, payments)
- Domain event publishing rates
- JVM memory and database connection pools

**Infrastructure Dashboard:**

- Cluster CPU and memory usage
- Pod resource utilization
- Network I/O metrics
- Kubernetes cluster health

## Usage Examples

### 1. Recording Business Metrics

```java
@Service
public class CustomerApplicationService {
    
    private final BusinessMetricsCollector metricsCollector;
    
    public void registerCustomer(RegisterCustomerCommand command) {
        Timer.Sample sample = metricsCollector.startOrderProcessingTimer();
        
        try {
            // Business logic
            Customer customer = new Customer(command.getName(), command.getEmail());
            customerRepository.save(customer);
            
            // Record success metrics
            metricsCollector.incrementCustomerRegistrations();
            metricsCollector.incrementActiveCustomers();
            
        } finally {
            metricsCollector.recordOrderProcessingTime(sample);
        }
    }
}
```

### 2. Custom CloudWatch Metrics

```java
@Component
public class OrderService {
    
    private final CloudWatchBusinessMetricsPublisher cloudWatchPublisher;
    
    public void processOrder(Order order) {
        // Process order
        
        // Publish business metrics to CloudWatch
        cloudWatchPublisher.publishOrderMetrics("OrderValue", 
                order.getTotalAmount().doubleValue(), "None");
        cloudWatchPublisher.publishOrderMetrics("OrderProcessingTime", 
                processingTime, "Milliseconds");
    }
}
```

### 3. Automatic Instrumentation

Methods in application services are automatically instrumented:

```java
@Service
public class PaymentApplicationService {
    
    // This method is automatically instrumented by MetricsAspect
    public void processPayment(ProcessPaymentCommand command) {
        // Method execution time is automatically recorded
        // Success/failure metrics are automatically tracked
        // Business metrics are automatically updated based on method name
    }
}
```

## Monitoring and Alerting

### Prometheus Alerts

**Application Health:**

- `ApplicationDown`: Application unavailable for > 1 minute
- `HighErrorRate`: Error rate > 10% for 5 minutes
- `HighResponseTime`: 95th percentile > 1 second for 5 minutes

**Resource Usage:**

- `HighMemoryUsage`: Memory usage > 80% for 5 minutes
- `HighCPUUsage`: CPU usage > 80% for 5 minutes

### CloudWatch Alarms

**Business Metrics:**

- Payment failure rate > 5%
- Order processing time > 30 seconds
- Customer registration rate drops > 50%

**Infrastructure Metrics:**

- EKS cluster resource utilization
- Database connection pool exhaustion
- Application instance health checks

## Performance Considerations

### Metric Collection Optimization

1. **Sampling Strategy:**
   - High-frequency metrics use sampling
   - Business metrics are always collected
   - Infrastructure metrics use appropriate intervals

2. **Batch Processing:**
   - CloudWatch metrics sent in batches of 20
   - Async publishing to avoid blocking
   - Error handling with retry logic

3. **Memory Management:**
   - Metric filters to reduce noise
   - Appropriate retention policies
   - Gauge cleanup for dynamic labels

### Cost Optimization

1. **CloudWatch Costs:**
   - Batch publishing reduces API calls
   - Metric filtering reduces storage costs
   - Appropriate retention periods

2. **Prometheus Storage:**
   - 15-day retention for detailed metrics
   - Persistent volumes with appropriate sizing
   - Query optimization for dashboards

## Testing

### Unit Tests

**BusinessMetricsCollectorTest:**

- Counter increment validation
- Gauge value updates
- Timer recording accuracy

**MetricsConfigurationTest:**

- Bean creation validation
- Common tags configuration
- Registry type verification

### Integration Tests

**Metrics Collection:**

- End-to-end metric flow
- Prometheus scraping validation
- CloudWatch publishing verification

## Deployment

### Local Development

```bash
# Start application with metrics enabled
./gradlew bootRun --args='--spring.profiles.active=dev'

# Access metrics endpoint
curl http://localhost:8080/actuator/prometheus
```

### Kubernetes Deployment

```bash
# Deploy monitoring namespace and Prometheus
kubectl apply -f infrastructure/k8s/observability/prometheus-deployment.yaml

# Deploy Grafana with dashboards
kubectl apply -f infrastructure/k8s/observability/grafana-deployment.yaml

# Deploy application with metrics annotations
kubectl apply -f infrastructure/k8s/application/genai-demo-deployment.yaml
```

### Access URLs

- **Application Metrics**: `http://localhost:8080/actuator/prometheus`
- **Prometheus**: `https://prometheus.kimkao.io`
- **Grafana**: `https://grafana.kimkao.io` (admin/admin123)

## Troubleshooting

### Common Issues

1. **Metrics Not Appearing:**
   - Check Prometheus scraping configuration
   - Verify application annotations
   - Confirm endpoint accessibility

2. **CloudWatch Publishing Failures:**
   - Verify AWS credentials and permissions
   - Check network connectivity
   - Review CloudWatch logs

3. **High Memory Usage:**
   - Review metric cardinality
   - Check for metric label explosion
   - Adjust retention policies

### Debug Commands

```bash
# Check Prometheus targets
kubectl port-forward -n monitoring svc/prometheus-service 9090:9090
# Visit http://localhost:9090/targets

# Check application metrics
kubectl port-forward -n genai-demo svc/genai-demo-service 8080:80
curl http://localhost:8080/actuator/prometheus

# View Grafana dashboards
kubectl port-forward -n monitoring svc/grafana-service 3000:3000
# Visit http://localhost:3000
```

## Future Enhancements

1. **Advanced Analytics:**
   - Machine learning-based anomaly detection
   - Predictive scaling based on metrics
   - Business intelligence integration

2. **Enhanced Alerting:**
   - Smart alert correlation
   - Escalation policies
   - Integration with incident management

3. **Cost Optimization:**
   - Intelligent metric sampling
   - Automated retention management
   - Multi-tier storage strategies

This comprehensive metrics system provides full observability into both technical and business aspects of the GenAI Demo application, enabling proactive monitoring and data-driven decision making.
