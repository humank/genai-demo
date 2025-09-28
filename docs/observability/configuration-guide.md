# Observability Configuration Guide

## Overview

This guide details how to configure the frontend-backend observability integration system, including environment-specific configurations, MSK topic setup, and monitoring configurations.

## Environment Configuration

### Development Environment Configuration

#### Backend Configuration (application-dev.yml)

```yaml
spring:
  profiles:
    active: dev
  datasource:
    url: jdbc:h2:file:./data/genai-demo-dev
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

# Observability configuration
genai-demo:
  events:
    publisher: in-memory  # Use in-memory event processing
    async: false         # Synchronous processing for debugging
  observability:
    analytics:
      enabled: true
      storage: in-memory  # Store metrics in memory
      retention-minutes: 60
      batch-size: 10      # Small batches for testing
      flush-interval: 10s # Quick flush for debugging
    tracing:
      enabled: true
      sample-rate: 1.0    # 100% sampling rate for development
    metrics:
      enabled: true
      export-interval: 30s

# Logging configuration
logging:
  level:
    solid.humank.genaidemo.infrastructure.observability: DEBUG
    solid.humank.genaidemo.application.observability: DEBUG
    org.springframework.kafka: INFO
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId:-},%X{traceId:-},%X{sessionId:-}] %logger{36} - %msg%n"
```

#### Frontend Configuration (environments/environment.ts)

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  observability: {
    enabled: true,
    batchSize: 10,
    flushInterval: 10000, // 10 seconds
    retryAttempts: 3,
    enableDebugLogs: true,
    sampleRate: 1.0, // 100% sampling for development
    endpoints: {
      analytics: '/../api/analytics/events',
      performance: '/api/analytics/performance',
      errors: '/api/monitoring/events'
    },
    webSocket: {
      url: 'ws://localhost:8080/ws/analytics',
      reconnectInterval: 5000,
      maxReconnectAttempts: 5
    }
  }
};
```

### Test Environment Configuration

#### Backend Configuration (application-test.yml)

```yaml
spring:
  profiles:
    active: test
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop

# Observability configuration
genai-demo:
  events:
    publisher: in-memory
    async: false
  observability:
    analytics:
      enabled: true
      storage: in-memory
      retention-minutes: 10  # Short retention for testing
      batch-size: 5
      flush-interval: 5s
    tracing:
      enabled: false  # Disable tracing in tests for speed
    metrics:
      enabled: false  # Disable metrics collection in tests

logging:
  level:
    solid.humank.genaidemo: WARN
    org.springframework.kafka: ERROR
```

#### Frontend Test Configuration (environments/environment.test.ts)

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  observability: {
    enabled: false, // Disable observability in tests
    batchSize: 5,
    flushInterval: 5000,
    retryAttempts: 1,
    enableDebugLogs: false,
    sampleRate: 0.1, // 10% sampling for testing
    endpoints: {
      analytics: '/../api/analytics/events',
      performance: '/api/analytics/performance',
      errors: '/api/monitoring/events'
    }
  }
};
```

### Production Environment Configuration

#### Backend Configuration (application-msk.yml)

```yaml
spring:
  profiles:
    active: msk
  kafka:
    bootstrap-servers: ${MSK_BOOTSTRAP_SERVERS}
    security:
      protocol: SASL_SSL
    sasl:
      mechanism: AWS_MSK_IAM
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      batch-size: 16384
      linger-ms: 5
      buffer-memory: 33554432
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      group-id: ${spring.application.name}-${spring.profiles.active}
      auto-offset-reset: earliest
      enable-auto-commit: false
      properties:
        spring.json.trusted.packages: "solid.humank.genaidemo.domain.events"

# Observability configuration
genai-demo:
  events:
    publisher: kafka     # Use MSK event processing
    async: true         # Asynchronous processing
  domain-events:
    topic:
      prefix: genai-demo.${spring.profiles.active}
      partitions: 6
      replication-factor: 3
      # Observability-specific topics
      observability:
        user-behavior: genai-demo.${spring.profiles.active}.observability.user.behavior
        performance-metrics: genai-demo.${spring.profiles.active}.observability.performance.metrics
        business-analytics: genai-demo.${spring.profiles.active}.observability.business.analytics
    publishing:
      enabled: true
      async: true
      dlq:
        enabled: true    # Dead letter queue handling
        topic-suffix: .dlq
  observability:
    analytics:
      enabled: true
      storage: kafka      # Use Kafka storage
      retention-days: 90  # 90-day retention
      batch-size: 100     # Large batches for performance
      flush-interval: 30s # 30-second flush interval
    tracing:
      enabled: true
      sample-rate: 0.1    # 10% sampling rate
    metrics:
      enabled: true
      export-interval: 60s

# AWS X-Ray configuration
aws:
  xray:
    tracing-name: genai-demo-${spring.profiles.active}
    context-missing: LOG_ERROR

# CloudWatch configuration
management:
  metrics:
    export:
      cloudwatch:
        namespace: GenAI/Demo/${spring.profiles.active}
        batch-size: 20
        step: 60s
        enabled: true

# Logging configuration
logging:
  level:
    solid.humank.genaidemo: INFO
    org.springframework.kafka: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId:-}] %logger{36} - %msg%n"
```

#### Frontend Production Configuration (environments/environment.prod.ts)

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.genai-demo.com',
  observability: {
    enabled: true,
    batchSize: 50,
    flushInterval: 30000, // 30 seconds
    retryAttempts: 3,
    enableDebugLogs: false,
    sampleRate: 0.1, // 10% sampling for production
    endpoints: {
      analytics: '/../api/analytics/events',
      performance: '/api/analytics/performance',
      errors: '/api/monitoring/events'
    },
    webSocket: {
      url: 'wss://api.genai-demo.com/ws/analytics',
      reconnectInterval: 10000,
      maxReconnectAttempts: 10
    },
    privacy: {
      maskPII: true,
      anonymizeUserData: true,
      respectDoNotTrack: true
    }
  }
};
```

## MSK Topic Configuration

### Topic Naming Convention

```
Format: ${projectName}.${environment}.${domain}.${event}
Example: genai-demo.production.observability.user.behavior
```

### Observability Topic List

#### User Behavior Analytics Topics

```
genai-demo.${environment}.observability.user.behavior
genai-demo.${environment}.observability.user.behavior.dlq
```

**Configuration Parameters:**

- Partitions: 6
- Replication Factor: 3
- Retention: 7 days (dev), 30 days (prod)
- Compression: gzip

#### Performance Metrics Topics

```
genai-demo.${environment}.observability.performance.metrics
genai-demo.${environment}.observability.performance.metrics.dlq
```

**Configuration Parameters:**

- Partitions: 3
- Replication Factor: 3
- Retention: 3 days (dev), 14 days (prod)
- Compression: lz4

#### Business Analytics Topics

```
genai-demo.${environment}.observability.business.analytics
genai-demo.${environment}.observability.business.analytics.dlq
```

**Configuration Parameters:**

- Partitions: 6
- Replication Factor: 3
- Retention: 30 days (dev), 90 days (prod)
- Compression: gzip

### CDK Configuration Update

The topic configuration in `infrastructure/lib/stacks/msk-stack.ts` includes observability topics:

```typescript
const domainEventTopics = [
    // Existing business domain events
    'customer.created',
    'customer.updated',
    'order.created',
    'order.confirmed',
    'order.cancelled',
    'payment.processed',
    'payment.failed',
    'inventory.reserved',
    'inventory.released',
    'product.created',
    'product.updated',
    
    // New observability events
    'observability.user.behavior',
    'observability.performance.metrics',
    'observability.business.analytics'
];
```

## Monitoring and Alerting Configuration

### CloudWatch Metrics

#### Custom Business Metrics

```yaml
# application-msk.yml
management:
  metrics:
    export:
      cloudwatch:
        namespace: GenAI/Demo/Observability
        dimensions:
          Environment: ${spring.profiles.active}
          Service: ${spring.application.name}
        metrics:
          - name: observability.events.received
            description: "Number of observability events received"
          - name: observability.events.processed
            description: "Number of observability events processed"
          - name: observability.events.failed
            description: "Number of observability events failed"
          - name: observability.batch.size
            description: "Average batch size for event processing"
          - name: observability.processing.latency
            description: "Event processing latency in milliseconds"
```

#### System Metrics

- JVM memory usage
- Kafka consumer lag
- API response times
- Error rates

### CloudWatch Alarms

#### Critical Alarms

```yaml
# High event processing failure rate
EventProcessingFailureRate:
  MetricName: observability.events.failed
  Threshold: 5  # More than 5 failed events per minute
  ComparisonOperator: GreaterThanThreshold
  EvaluationPeriods: 2
  Period: 60

# High API response time
AnalyticsAPILatency:
  MetricName: http.server.requests
  Dimensions:
    uri: /../api/analytics/events
  Threshold: 1000  # 1 second
  ComparisonOperator: GreaterThanThreshold
  Statistic: Average
  EvaluationPeriods: 3
  Period: 300

# Kafka consumer lag
KafkaConsumerLag:
  MetricName: kafka.consumer.lag
  Threshold: 1000  # 1000 message lag
  ComparisonOperator: GreaterThanThreshold
  EvaluationPeriods: 2
  Period: 300
```

### Prometheus Metrics Configuration

#### Custom Metrics

```java
// ObservabilityMetrics.java
@Component
public class ObservabilityMetrics {
    
    private final Counter eventsReceived;
    private final Counter eventsProcessed;
    private final Counter eventsFailed;
    private final Timer processingLatency;
    private final Gauge batchSize;
    
    public ObservabilityMetrics(MeterRegistry meterRegistry) {
        this.eventsReceived = Counter.builder("observability.events.received")
            .description("Number of observability events received")
            .tag("type", "analytics")
            .register(meterRegistry);
            
        this.eventsProcessed = Counter.builder("observability.events.processed")
            .description("Number of observability events processed successfully")
            .register(meterRegistry);
            
        this.eventsFailed = Counter.builder("observability.events.failed")
            .description("Number of observability events failed to process")
            .register(meterRegistry);
            
        this.processingLatency = Timer.builder("observability.processing.latency")
            .description("Event processing latency")
            .register(meterRegistry);
            
        this.batchSize = Gauge.builder("observability.batch.size")
            .description("Current batch size for event processing")
            .register(meterRegistry, this, ObservabilityMetrics::getCurrentBatchSize);
    }
}
```

## Security Configuration

### Data Encryption

#### Encryption in Transit

```yaml
# TLS configuration
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12

# Kafka TLS configuration
spring:
  kafka:
    security:
      protocol: SASL_SSL
    ssl:
      trust-store-location: ${KAFKA_TRUSTSTORE_LOCATION}
      trust-store-password: ${KAFKA_TRUSTSTORE_PASSWORD}
```

#### Encryption at Rest

```yaml
# Database encryption
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}?sslmode=require
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        
# KMS configuration
aws:
  kms:
    key-id: ${KMS_KEY_ID}
    region: ${AWS_REGION}
```

### Access Control

#### IAM Role Configuration

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "kafka-cluster:Connect",
        "kafka-cluster:WriteData",
        "kafka-cluster:ReadData"
      ],
      "Resource": [
        "arn:aws:kafka:*:*:cluster/genai-demo-*",
        "arn:aws:kafka:*:*:topic/genai-demo.*.observability.*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "cloudwatch:PutMetricData"
      ],
      "Resource": "*",
      "Condition": {
        "StringEquals": {
          "cloudwatch:namespace": "GenAI/Demo/Observability"
        }
      }
    }
  ]
}
```

## Performance Tuning

### JVM Configuration

```bash
# Production JVM parameters
JAVA_OPTS="-Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -Dspring.profiles.active=msk"
```

### Kafka Producer Configuration

```yaml
spring:
  kafka:
    producer:
      batch-size: 16384      # 16KB batch size
      linger-ms: 5           # 5ms delay to increase batch efficiency
      buffer-memory: 33554432 # 32MB buffer
      compression-type: gzip  # Compression to reduce network transfer
      acks: all              # Wait for all replicas to acknowledge
      retries: 3             # Number of retries
      enable-idempotence: true # Enable idempotence
```

### Kafka Consumer Configuration

```yaml
spring:
  kafka:
    consumer:
      fetch-min-size: 1024   # Minimum fetch size 1KB
      fetch-max-wait: 500    # Maximum wait time 500ms
      max-poll-records: 500  # Maximum records per poll
      session-timeout-ms: 30000 # Session timeout 30 seconds
      heartbeat-interval-ms: 3000 # Heartbeat interval 3 seconds
```

## Troubleshooting

### Common Configuration Issues

#### 1. MSK Connection Failure

**Symptoms**: Unable to connect to MSK cluster

**Solutions**:

```bash
# Check network connectivity
telnet ${MSK_BOOTSTRAP_SERVERS} 9098

# Check IAM permissions
aws sts get-caller-identity

# Check security group rules
aws ec2 describe-security-groups --group-ids ${MSK_SECURITY_GROUP_ID}
```

#### 2. Events Not Being Processed

**Symptoms**: Frontend sends events but backend doesn't receive them

**Checklist**:

- [ ] Confirm API endpoint URL is correct
- [ ] Check CORS configuration
- [ ] Verify request header format
- [ ] Review network error logs

#### 3. Performance Issues

**Symptoms**: High event processing latency

**Optimization Suggestions**:

- Increase Kafka partition count
- Adjust batch sizes
- Optimize JVM parameters
- Check database query performance

### Log Analysis

#### Key Log Patterns

```bash
# Event reception logs
grep "Received.*analytics events" /var/log/genai-demo/application.log

# Event processing failure logs
grep "Failed to process.*event" /var/log/genai-demo/application.log

# Kafka connection issues
grep "kafka.*connection" /var/log/genai-demo/application.log

# Performance warnings
grep "processing.*took.*ms" /var/log/genai-demo/application.log
```

## Configuration Validation

### Automated Validation Script

```bash
#!/bin/bash
# validate-observability-config.sh

echo "Validating observability configuration..."

# Check backend health
curl -f http://localhost:8080/actuator/health || exit 1

# Check Kafka connectivity
curl -f http://localhost:8080/actuator/health/kafka || exit 1

# Test analytics API
curl -X POST http://localhost:8080/../api/analytics/events \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: test-trace-123" \
  -H "X-Session-Id: test-session-456" \
  -d '[{"eventId":"test","eventType":"page_view","sessionId":"test-session-456","traceId":"test-trace-123","timestamp":1640995200000,"data":{"page":"/test"}}]' || exit 1

echo "Configuration validation completed!"
```

### Configuration Checklist

#### Development Environment

- [ ] H2 database accessible
- [ ] In-memory event processor enabled
- [ ] Debug logging enabled
- [ ] Local WebSocket connection working

#### Test Environment

- [ ] Test database isolated
- [ ] Observability features optionally enabled
- [ ] Fast event processing configuration
- [ ] Automatic test data cleanup

#### Production Environment

- [ ] MSK cluster connection working
- [ ] SSL/TLS encryption enabled
- [ ] IAM permissions correctly configured
- [ ] CloudWatch metrics reporting normally
- [ ] Alert rules configured
- [ ] Data retention policies configured

## Related Documentation

- [API Documentation](../api/observability-api.md)
- [Deployment Guide](../deployment/observability-deployment.md)
- [Troubleshooting Guide](../troubleshooting/observability-troubleshooting.md)