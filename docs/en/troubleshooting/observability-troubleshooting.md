# Observability Troubleshooting Guide

## Overview

This guide provides common issue diagnosis and solutions for frontend-backend observability integration systems, including connection issues, performance problems, and data processing issues.

## Quick Diagnostic Checklist

### System Health Check

```bash
#!/bin/bash
# quick-health-check.sh

echo "=== Observability System Health Check ==="

# 1. Check backend service status
echo "1. Checking backend service..."
curl -s http://localhost:8080/actuator/health | jq '.status' || echo "❌ Backend service unavailable"

# 2. Check Kafka connection (production environment)
echo "2. Checking Kafka connection..."
curl -s http://localhost:8080/actuator/health/kafka | jq '.status' || echo "⚠️  Kafka connection issue"

# 3. Check analytics API
echo "3. Testing analytics API..."
response=$(curl -s -w "%{http_code}" -X POST http://localhost:8080/api/analytics/events \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: health-check-$(date +%s)" \
  -H "X-Session-Id: health-check-session" \
  -d '[{"eventId":"health-check","eventType":"page_view","sessionId":"health-check-session","traceId":"health-check-'$(date +%s)'","timestamp":'$(date +%s000)',"data":{"page":"/health-check"}}]')

if [[ "${response: -3}" == "200" ]]; then
    echo "✅ Analytics API normal"
else
    echo "❌ Analytics API abnormal: HTTP ${response: -3}"
fi

# 4. Check WebSocket connection (Planned Feature)
echo "4. WebSocket feature not yet implemented..."
echo "ℹ️ WebSocket real-time monitoring will be developed in the next phase"

# 5. Check metrics endpoint
echo "5. Checking metrics..."
curl -s http://localhost:8080/actuator/metrics/observability.events.received >/dev/null && echo "✅ Metrics endpoint normal" || echo "❌ Metrics endpoint abnormal"

echo "=== Health check completed ==="
```

## Common Issues and Solutions

### 1. Frontend Event Sending Issues

#### Issue: Frontend events not sent to backend

**Symptoms**:

- No `/api/analytics/events` requests visible in browser network tab
- No error messages in frontend console
- No event reception logs in backend logs

**Diagnostic Steps**:

```javascript
// Execute diagnostics in browser console
console.log('=== Frontend Observability Diagnostics ===');

// Check configuration
console.log('1. Check configuration:', window.environment?.observability);

// Check service status
const observabilityService = document.querySelector('app-root')?._ngElementStrategy?.componentRef?.instance?.observabilityService;
console.log('2. Service status:', observabilityService);

// Check batch processor
console.log('3. Batch processor status:', observabilityService?.batchProcessor);

// Check local storage
console.log('4. Local storage events:', localStorage.getItem('observability-events'));

// Manually send test event
observabilityService?.trackPageView('/test-page', { test: true });
console.log('5. Test event sent');
```

**Solutions**:

1. **Check configuration enabled status**:

```typescript
// environments/environment.ts
export const environment = {
  observability: {
    enabled: true, // Ensure enabled
    // ...other configuration
  }
};
```

2. **Check service injection**:

```typescript
// app.component.ts
constructor(private observabilityService: ObservabilityService) {
  console.log('ObservabilityService injected:', this.observabilityService);
}
```

3. **Check interceptor registration**:

```typescript
// app.config.ts
export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(
      withInterceptors([observabilityTraceInterceptor]) // Ensure interceptor is registered
    )
  ]
};
```

#### Issue: CORS Error

**Symptoms**:

```
Access to XMLHttpRequest at 'http://localhost:8080/api/analytics/events' 
from origin 'http://localhost:4200' has been blocked by CORS policy
```

**Solutions**:

1. **Check backend CORS configuration**:

```java
@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*") // Ensure CORS configuration is correct
public class AnalyticsController {
    // ...
}
```

2. **Global CORS configuration**:

```java
@Configuration
public class CorsConfiguration {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
```

### 2. Backend Event Processing Issues

#### Issue: Events received but not processed

**Symptoms**:

- Backend logs show "Received X analytics events"
- But no "Successfully processed analytics events" logs
- Event counter metrics don't increase

**Diagnostic Steps**:

```bash
# Check backend logs
tail -f logs/genai-demo.log | grep -E "(analytics|observability)"

# Check event processing metrics
curl -s http://localhost:8080/actuator/metrics/observability.events.processed | jq

# Check error metrics
curl -s http://localhost:8080/actuator/metrics/observability.events.failed | jq

# Check MDC context
grep "correlationId" logs/genai-demo.log | tail -10
```

**Solutions**:

1. **Check event transformation logic**:

```java
@Service
public class ObservabilityEventService {
    
    public void processAnalyticsEvents(List<AnalyticsEventDto> events, String traceId, String sessionId) {
        try {
            // Add detailed logging
            logger.info("Processing {} events with traceId: {}", events.size(), traceId);
            
            for (AnalyticsEventDto event : events) {
                logger.debug("Processing event: {}", event.eventId());
                // Processing logic...
            }
            
        } catch (Exception e) {
            logger.error("Failed to process events", e);
            throw e;
        }
    }
}
```

2. **Check Domain Event publishing**:

```java
@Component
public class ObservabilityEventPublisher {
    
    @EventListener
    public void handleAnalyticsEvent(UserBehaviorAnalyticsEvent event) {
        logger.info("Handling analytics event: {} for session: {}", 
            event.eventId(), event.sessionId());
        
        try {
            if ("kafka".equals(publisherType)) {
                publishToKafka(event);
            } else {
                processInMemory(event);
            }
            logger.info("Successfully handled event: {}", event.eventId());
        } catch (Exception e) {
            logger.error("Failed to handle event: {}", event.eventId(), e);
            throw e;
        }
    }
}
```

#### Issue: Kafka Connection Failure

**Symptoms**:

```
org.apache.kafka.common.errors.TimeoutException: 
Failed to update metadata after 60000 ms.
```

**Diagnostic Steps**:

```bash
# Check MSK cluster status
aws kafka describe-cluster --cluster-arn ${MSK_CLUSTER_ARN}

# Check network connection
telnet ${MSK_BOOTSTRAP_SERVERS} 9098

# Check IAM permissions
aws sts get-caller-identity

# Check security groups
aws ec2 describe-security-groups --group-ids ${MSK_SECURITY_GROUP_ID}
```

**Solutions**:

1. **Check MSK configuration**:

```yaml
spring:
  kafka:
    bootstrap-servers: ${MSK_BOOTSTRAP_SERVERS}
    security:
      protocol: SASL_SSL
    sasl:
      mechanism: AWS_MSK_IAM
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: AWS_MSK_IAM
      sasl.jaas.config: software.amazon.msk.auth.iam.IAMLoginModule required;
      sasl.client.callback.handler.class: software.amazon.msk.auth.iam.IAMClientCallbackHandler
```

2. **Check IAM role permissions**:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "kafka-cluster:Connect",
        "kafka-cluster:WriteData",
        "kafka-cluster:ReadData",
        "kafka-cluster:CreateTopic",
        "kafka-cluster:DescribeTopic"
      ],
      "Resource": [
        "arn:aws:kafka:*:*:cluster/genai-demo-*",
        "arn:aws:kafka:*:*:topic/genai-demo.*.observability.*"
      ]
    }
  ]
}
```

### 3. WebSocket Connection Issues

#### Issue: WebSocket connection failure

**Symptoms**:

- Frontend cannot establish WebSocket connection
- Browser console shows WebSocket errors
- Real-time update functionality doesn't work

**Diagnostic Steps**:

```javascript
// Browser console test
const testWebSocket = () => {
  const ws = new WebSocket('ws://localhost:8080/ws/analytics?sessionId=test-session');
  
  ws.onopen = () => console.log('✅ WebSocket connection successful');
  ws.onerror = (error) => console.error('❌ WebSocket error:', error);
  ws.onclose = (event) => console.log('WebSocket closed:', event.code, event.reason);
  ws.onmessage = (message) => console.log('Received message:', message.data);
  
  // Close connection after 5 seconds
  setTimeout(() => ws.close(), 5000);
};

testWebSocket();
```

**Solutions**:

1. **Check WebSocket configuration (Planned Feature)**:

**Note: WebSocket functionality is not yet fully implemented and will be developed in the next phase**

```java
// Planned WebSocket configuration
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new AnalyticsWebSocketHandler(), "/ws/analytics")
                .setAllowedOrigins("*") // Ensure cross-origin is allowed
                .withSockJS(); // Add SockJS support
    }
}
```

2. **Check firewall and proxy settings**:

```nginx
# nginx.conf
location /ws/ {
    proxy_pass http://backend;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
}
```

### 4. Performance Issues

#### Issue: High event processing latency

**Symptoms**:

- Event processing delay from sending to processing exceeds 1 second
- Batch processing backlog
- Memory usage continuously rising

**Diagnostic Steps**:

```bash
# Check JVM memory usage
curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq

# Check GC statistics
curl -s http://localhost:8080/actuator/metrics/jvm.gc.pause | jq

# Check event processing latency
curl -s http://localhost:8080/actuator/metrics/observability.processing.latency | jq

# Check Kafka consumer lag
curl -s http://localhost:8080/actuator/metrics/kafka.consumer.lag | jq
```

**Solutions**:

1. **Adjust batch processing configuration**:

```yaml
genai-demo:
  observability:
    analytics:
      batch-size: 50        # Reduce batch size
      flush-interval: 15s   # Reduce flush interval
      max-queue-size: 1000  # Limit queue size
```

2. **Optimize JVM parameters**:

```bash
# Increase heap memory
export JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

3. **Increase Kafka partition count**:

```bash
# Use Kafka management tools to increase partitions
kafka-topics.sh --bootstrap-server ${MSK_BOOTSTRAP_SERVERS} \
  --alter --topic genai-demo.production.observability.user.behavior \
  --partitions 12
```

#### Issue: Memory leak

**Symptoms**:

- Memory usage continuously rising
- Frequent Full GC
- Eventually leads to OutOfMemoryError

**Diagnostic Steps**:

```bash
# Generate heap dump
jcmd <pid> GC.run_finalization
jcmd <pid> VM.gc
jmap -dump:format=b,file=heap-dump.hprof <pid>

# Analyze heap dump (using Eclipse MAT or VisualVM)
# Look for root causes of memory leaks
```

**Solutions**:

1. **Check event cache cleanup**:

```java
@Component
public class EventCacheManager {
    
    private final Map<String, List<ObservabilityEvent>> eventCache = new ConcurrentHashMap<>();
    
    @Scheduled(fixedRate = 300000) // Clean up every 5 minutes
    public void cleanupExpiredEvents() {
        long cutoffTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
        
        eventCache.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(event -> event.getTimestamp() < cutoffTime);
            return entry.getValue().isEmpty();
        });
        
        logger.debug("Cleaned up expired events, cache size: {}", eventCache.size());
    }
}
```

2. **Limit event queue size**:

```java
@Component
public class BatchProcessor {
    
    private final BlockingQueue<ObservabilityEvent> eventQueue = 
        new ArrayBlockingQueue<>(1000); // Limit queue size
    
    public void addEvent(ObservabilityEvent event) {
        if (!eventQueue.offer(event)) {
            logger.warn("Event queue is full, dropping event: {}", event.getId());
            // Optional: trigger alert
        }
    }
}
```

### 5. Data Consistency Issues

#### Issue: Duplicate event processing

**Symptoms**:

- Same event processed multiple times
- Business metrics calculation errors
- Duplicate Domain Events

**Solutions**:

1. **Implement idempotency check**:

```java
@Component
public class IdempotentEventProcessor {
    
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();
    
    @Scheduled(fixedRate = 600000) // Clean up every 10 minutes
    public void cleanupProcessedEvents() {
        // Clean up event IDs from 1 hour ago
        // Actual implementation might need to use Redis or database
    }
    
    public boolean isEventProcessed(String eventId) {
        return !processedEventIds.add(eventId);
    }
}
```

2. **Use database unique constraints**:

```sql
CREATE TABLE processed_events (
    event_id VARCHAR(255) PRIMARY KEY,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    session_id VARCHAR(255),
    trace_id VARCHAR(255)
);

CREATE INDEX idx_processed_events_timestamp ON processed_events(processed_at);
```

#### Issue: Event order confusion

**Symptoms**:

- Page view events processed after user action events
- Business process order incorrect

**Solutions**:

1. **Use session ID as partition key**:

```java
@Component
public class SessionBasedPartitioner implements Partitioner {
    
    @Override
    public int partition(String topic, Object key, byte[] keyBytes, 
                        Object value, byte[] valueBytes, Cluster cluster) {
        if (key instanceof String sessionId) {
            return Math.abs(sessionId.hashCode()) % cluster.partitionCountForTopic(topic);
        }
        return 0;
    }
}
```

2. **Add sequence number**:

```java
public record AnalyticsEventDto(
    String eventId,
    String eventType,
    String sessionId,
    Long sequenceNumber, // Add sequence number
    Long timestamp,
    Map<String, Object> data
) {}
```

## Monitoring and Alerting

### Key Metrics Monitoring

#### 1. System Health Metrics

```bash
# Script to check system health metrics
#!/bin/bash

echo "=== Observability System Monitoring ==="

# API response time
api_latency=$(curl -s http://localhost:8080/actuator/metrics/http.server.requests | jq -r '.measurements[] | select(.statistic=="MEAN") | .value')
echo "API average response time: ${api_latency}ms"

# Event processing rate
events_processed=$(curl -s http://localhost:8080/actuator/metrics/observability.events.processed | jq -r '.measurements[0].value')
echo "Events processed: ${events_processed}"

# Error rate
events_failed=$(curl -s http://localhost:8080/actuator/metrics/observability.events.failed | jq -r '.measurements[0].value // 0')
echo "Failed events: ${events_failed}"

# Memory usage
memory_used=$(curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq -r '.measurements[0].value')
memory_max=$(curl -s http://localhost:8080/actuator/metrics/jvm.memory.max | jq -r '.measurements[0].value')
memory_usage=$(echo "scale=2; $memory_used / $memory_max * 100" | bc)
echo "Memory usage: ${memory_usage}%"

# Alert checks
if (( $(echo "$api_latency > 1000" | bc -l) )); then
    echo "⚠️  Alert: API response time too high"
fi

if (( $(echo "$memory_usage > 80" | bc -l) )); then
    echo "⚠️  Alert: Memory usage too high"
fi

if (( events_failed > 10 )); then
    echo "⚠️  Alert: Event processing failure rate too high"
fi
```

#### 2. Business Metrics Monitoring

```java
@Component
public class BusinessMetricsMonitor {
    
    private final MeterRegistry meterRegistry;
    
    @Scheduled(fixedRate = 60000) // Check every minute
    public void checkBusinessMetrics() {
        // Check page views
        Counter pageViews = meterRegistry.counter("business.page.views");
        if (pageViews.count() == 0) {
            logger.warn("No page views recorded in the last minute");
        }
        
        // Check user activity
        Counter userActions = meterRegistry.counter("business.user.actions");
        if (userActions.count() < 10) {
            logger.warn("Low user activity detected: {} actions", userActions.count());
        }
        
        // Check conversion rate
        double conversionRate = calculateConversionRate();
        if (conversionRate < 0.02) { // 2%
            logger.warn("Low conversion rate detected: {}%", conversionRate * 100);
        }
    }
}
```

### Automated Failure Recovery

#### 1. Automatic Restart Mechanism

```bash
#!/bin/bash
# auto-recovery.sh

check_service_health() {
    response=$(curl -s -w "%{http_code}" http://localhost:8080/actuator/health)
    if [[ "${response: -3}" != "200" ]]; then
        return 1
    fi
    return 0
}

restart_service() {
    echo "$(date): Service anomaly detected, restarting..."
    systemctl restart genai-demo
    sleep 30
    
    if check_service_health; then
        echo "$(date): Service restart successful"
    else
        echo "$(date): Service restart failed, manual intervention required"
        # Send alert notification
    fi
}

# Main monitoring loop
while true; do
    if ! check_service_health; then
        restart_service
    fi
    sleep 60
done
```

#### 2. Auto-scaling Configuration

```yaml
# kubernetes/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: genai-demo-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: genai-demo-backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  - type: Pods
    pods:
      metric:
        name: observability_events_processing_rate
      target:
        type: AverageValue
        averageValue: "100"
```

## Log Analysis and Debugging

### Structured Log Queries

```bash
# Query all logs for specific trace ID
grep "correlationId:trace-123" logs/genai-demo.log

# Query event processing errors
grep -E "(ERROR|WARN).*observability" logs/genai-demo.log | tail -20

# Query performance issues
grep "processing.*took.*ms" logs/genai-demo.log | awk '{print $NF}' | sort -n | tail -10

# Query Kafka-related issues
grep -i kafka logs/genai-demo.log | grep -E "(ERROR|WARN)"
```

### Distributed Tracing Analysis

```bash
# Query traces using AWS X-Ray
aws xray get-trace-summaries \
  --time-range-type TimeRangeByStartTime \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-01T23:59:59Z \
  --filter-expression 'service("genai-demo") AND error'

# Query specific trace details
aws xray batch-get-traces --trace-ids trace-123-456-789
```

## Performance Tuning Recommendations

### 1. JVM Tuning

```bash
# Recommended JVM parameters for production
JAVA_OPTS="-server \
  -Xms4g -Xmx8g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -XX:+UseCompressedOops \
  -XX:+UseCompressedClassPointers \
  -Djava.awt.headless=true \
  -Dspring.profiles.active=msk"
```

### 2. Database Tuning

```sql
-- Add indexes for observability-related tables
CREATE INDEX CONCURRENTLY idx_observability_events_timestamp 
ON observability_events(timestamp DESC);

CREATE INDEX CONCURRENTLY idx_observability_events_session_id 
ON observability_events(session_id, timestamp DESC);

CREATE INDEX CONCURRENTLY idx_observability_events_trace_id 
ON observability_events(trace_id);

-- Regularly clean up old data
DELETE FROM observability_events 
WHERE timestamp < NOW() - INTERVAL '90 days';
```

### 3. Network Tuning

```bash
# Adjust TCP parameters to optimize Kafka connections
echo 'net.core.rmem_max = 134217728' >> /etc/sysctl.conf
echo 'net.core.wmem_max = 134217728' >> /etc/sysctl.conf
echo 'net.ipv4.tcp_rmem = 4096 65536 134217728' >> /etc/sysctl.conf
echo 'net.ipv4.tcp_wmem = 4096 65536 134217728' >> /etc/sysctl.conf
sysctl -p
```

## Contact Support

### Emergency Contact Information

- **Technical Support**: <tech-support@company.com>
- **On-call Phone**: +1-xxx-xxx-xxxx
- **Slack Channel**: #observability-support

### Issue Report Format

```markdown
## Issue Description
[Brief description of the issue]

## Environment Information
- Environment: [dev/test/production]
- Version: [application version]
- Time: [time when issue occurred]

## Reproduction Steps
1. [Step 1]
2. [Step 2]
3. [Step 3]

## Expected Behavior
[Describe expected normal behavior]

## Actual Behavior
[Describe actual abnormal behavior]

## Logs and Error Messages
```

[Paste relevant logs]

```

## Attempted Solutions
[List solutions already tried]
```

## Related Documentation

- [Configuration Guide](../observability/configuration-guide.md)
- [API Documentation](../api/observability-api.md)
- [Deployment Guide](../deployment/observability-deployment.md)
- [Performance Tuning Guide](../performance/observability-performance.md)
