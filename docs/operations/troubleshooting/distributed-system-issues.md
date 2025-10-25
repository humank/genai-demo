# Distributed System Troubleshooting Guide

## Overview

This guide provides troubleshooting procedures for distributed system issues in the e-commerce platform, including event-driven architecture with Apache Kafka, distributed tracing with AWS X-Ray, saga patterns, and cross-service transactions.

**Technology Stack**:
- **Messaging**: Apache Kafka (Amazon MSK)
- **Container Orchestration**: Amazon EKS
- **Distributed Tracing**: AWS X-Ray
- **Load Balancing**: AWS Application Load Balancer (ALB)
- **Database**: PostgreSQL (Amazon RDS)
- **Cache**: Redis (Amazon ElastiCache)

**Last Updated**: 2025-10-25  
**Severity Levels**: Critical, High, Medium, Low

---

## Table of Contents

1. [Event-Driven Architecture Issues](#event-driven-architecture-issues)
2. [Distributed Tracing Analysis](#distributed-tracing-analysis)
3. [Saga Pattern Failures](#saga-pattern-failures)
4. [Eventual Consistency Issues](#eventual-consistency-issues)
5. [Cross-Service Transaction Failures](#cross-service-transaction-failures)
6. [Circuit Breaker Issues](#circuit-breaker-issues)
7. [Rate Limiting and Throttling](#rate-limiting-and-throttling)

---

## Event-Driven Architecture Issues

### Kafka Consumer Lag

#### Symptoms
- Events are not being processed in real-time
- Consumer lag metrics increasing in CloudWatch
- Delayed order confirmations or inventory updates
- Dashboard shows high consumer lag values

#### Detection
```bash
# Check consumer lag using Kafka tools
kubectl exec -it kafka-client -n production -- \
  kafka-consumer-groups.sh --bootstrap-server <msk-broker>:9092 \
  --describe --group order-processing-group

# Monitor lag metrics in CloudWatch
aws cloudwatch get-metric-statistics \
  --namespace AWS/Kafka \
  --metric-name EstimatedMaxTimeLag \
  --dimensions Name=Consumer Group,Value=order-processing-group \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average
```

#### Diagnosis

**Step 1: Check Consumer Health**
```bash
# Check if consumers are running
kubectl get pods -n production -l app=order-consumer

# Check consumer logs
kubectl logs -f deployment/order-consumer -n production --tail=100

# Check for errors
kubectl logs deployment/order-consumer -n production | grep -i error | tail -20
```

**Step 2: Analyze Consumer Performance**
```bash
# Check consumer processing rate via Spring Boot Actuator
kubectl exec -it deployment/order-consumer -n production -- \
  curl localhost:8080/actuator/metrics/kafka.consumer.records.consumed.rate

# Check consumer thread utilization
kubectl top pods -n production -l app=order-consumer

# Check JVM metrics
kubectl exec -it deployment/order-consumer -n production -- \
  curl localhost:8080/actuator/metrics/jvm.threads.live
```

**Step 3: Identify Bottlenecks**
Common causes:
- Slow message processing (check application logs)
- Database connection pool exhaustion
- External service timeouts
- Memory pressure causing GC pauses
- Network issues with MSK brokers

#### Resolution

**Immediate Actions**

1. **Scale Consumer Instances**
```bash
# Increase consumer replicas
kubectl scale deployment order-consumer -n production --replicas=5

# Verify scaling
kubectl get pods -n production -l app=order-consumer -w
```

2. **Increase Partition Count** (if consumer instances > partitions)
```bash
# Check current partition count
kubectl exec -it kafka-client -n production -- \
  kafka-topics.sh --bootstrap-server <msk-broker>:9092 \
  --describe --topic order-events

# Add partitions (cannot be decreased later!)
kubectl exec -it kafka-client -n production -- \
  kafka-topics.sh --bootstrap-server <msk-broker>:9092 \
  --alter --topic order-events --partitions 10
```

3. **Optimize Consumer Configuration**
```yaml
# application.yml
spring:
  kafka:
    consumer:
      max-poll-records: 100  # Reduce batch size if processing is slow
      max-poll-interval-ms: 300000  # Increase timeout (5 minutes)
      fetch-min-bytes: 1024  # Optimize fetch size
      enable-auto-commit: false  # Manual commit for better control
    listener:
      concurrency: 3  # Number of consumer threads per instance
```

**Root Cause Fixes**
- Optimize message processing logic
- Add database connection pooling (HikariCP configuration)
- Implement async processing for slow operations
- Add circuit breakers for external service calls
- Optimize database queries

#### Prevention
- Monitor consumer lag continuously in CloudWatch
- Set up CloudWatch alarms for lag > 1000 messages
- Implement auto-scaling based on lag metrics using KEDA
- Regular performance testing with production-like load
- Review consumer configuration quarterly

---

### Kafka Partition Rebalancing

#### Symptoms
- Frequent consumer group rebalancing
- Temporary processing delays during rebalance
- "Rebalance in progress" errors in logs
- Consumers repeatedly joining and leaving group
- Consumer lag spikes during rebalance

#### Detection
```bash
# Check rebalance frequency in logs
kubectl logs deployment/order-consumer -n production | \
  grep "Rebalance" | tail -20

# Monitor rebalance metrics via Actuator
kubectl exec -it deployment/order-consumer -n production -- \
  curl localhost:8080/actuator/metrics/kafka.consumer.rebalance.total
```

#### Diagnosis

**Step 1: Identify Rebalance Triggers**
```bash
# Check consumer logs for rebalance reasons
kubectl logs deployment/order-consumer -n production | \
  grep -A 5 "Rebalance triggered"

# Common causes:
# - Consumer heartbeat timeout
# - Consumer processing timeout (max.poll.interval.ms exceeded)
# - Consumer crashes or restarts
# - New consumers joining group
# - Network issues with MSK brokers
```

**Step 2: Check Consumer Configuration**
```yaml
# Verify consumer settings in application.yml
spring:
  kafka:
    consumer:
      heartbeat-interval-ms: 3000  # Should be < session-timeout-ms/3
      session-timeout-ms: 30000  # Time before consumer is considered dead
      max-poll-interval-ms: 300000  # Max time between polls
```

**Step 3: Check Pod Stability**
```bash
# Check for pod restarts
kubectl get pods -n production -l app=order-consumer

# Check pod events
kubectl describe pod <pod-name> -n production | grep -A 10 Events
```

#### Resolution

**Immediate Actions**

1. **Increase Timeout Values**
```yaml
spring:
  kafka:
    consumer:
      session-timeout-ms: 45000  # Increase from 30s
      max-poll-interval-ms: 600000  # Increase from 5min to 10min
      heartbeat-interval-ms: 10000  # Increase from 3s
```

2. **Reduce Processing Time**
- Optimize message processing logic
- Reduce batch size (max-poll-records)
- Implement async processing for slow operations
- Add timeouts for external service calls

3. **Fix Pod Stability Issues**
```bash
# Check resource limits
kubectl describe deployment order-consumer -n production | grep -A 5 Limits

# Increase resources if needed
kubectl set resources deployment order-consumer -n production \
  --limits=cpu=2,memory=2Gi \
  --requests=cpu=1,memory=1Gi
```

**Root Cause Fixes**
- Fix slow message processing
- Ensure consumers send heartbeats regularly
- Implement graceful shutdown
- Add readiness/liveness probes
- Use static consumer group membership (Kafka 2.3+)

#### Prevention
- Monitor rebalance frequency in CloudWatch
- Set appropriate timeout values based on processing time
- Implement health checks
- Use Kubernetes readiness probes properly
- Test consumer behavior under load

---

## Distributed Tracing Analysis

### AWS X-Ray Trace Analysis

#### Symptoms
- Slow API responses
- Timeout errors
- Unclear where latency is occurring
- Need to trace request across multiple services

#### Detection
```bash
# Access X-Ray console via AWS CLI
aws xray get-trace-summaries \
  --start-time $(date -u -d '1 hour ago' +%s) \
  --end-time $(date -u +%s) \
  --filter-expression 'service("order-service") AND duration > 2'

# Check X-Ray service map in AWS Console
# AWS Console → X-Ray → Service Map
```

#### Diagnosis

**Step 1: Identify Slow Traces**
```bash
# Find traces with high latency
aws xray get-trace-summaries \
  --start-time $(date -u -d '1 hour ago' +%s) \
  --end-time $(date -u +%s) \
  --filter-expression 'duration > 2' \
  --query 'TraceSummaries[*].[Id,Duration]' \
  --output table

# Get top 10 slowest traces
aws xray get-trace-summaries \
  --start-time $(date -u -d '1 hour ago' +%s) \
  --end-time $(date -u +%s) \
  --query 'sort_by(TraceSummaries, &Duration)[-10:].[Id,Duration]' \
  --output table
```

**Step 2: Analyze Trace Details**
```bash
# Get detailed trace
aws xray batch-get-traces --trace-ids <trace-id>

# Look for:
# - Slow database queries (subsegments with high duration)
# - External service timeouts
# - High processing time in specific services
# - Error subsegments
# - Throttling indicators
```

**Step 3: Check Service Dependencies**
- Review X-Ray service map for bottlenecks
- Identify services with high error rates
- Check for circular dependencies
- Verify timeout configurations

**Step 4: Analyze Subsegments**
```bash
# Extract subsegment information
aws xray batch-get-traces --trace-ids <trace-id> | \
  jq '.Traces[0].Segments[0].Document | fromjson | .subsegments'
```

#### Resolution

**Immediate Actions**

1. **Add Detailed X-Ray Instrumentation**
```java
// Add subsegments for detailed tracing
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;

@Service
public class OrderService {
    
    public void processOrder(Order order) {
        Subsegment subsegment = AWSXRay.beginSubsegment("validate-order");
        try {
            validateOrder(order);
        } finally {
            AWSXRay.endSubsegment();
        }
        
        subsegment = AWSXRay.beginSubsegment("save-order");
        try {
            orderRepository.save(order);
        } finally {
            AWSXRay.endSubsegment();
        }
    }
}
```

2. **Add Custom Annotations and Metadata**
```java
// Add business context to traces
AWSXRay.getCurrentSegment().putAnnotation("orderId", order.getId());
AWSXRay.getCurrentSegment().putAnnotation("customerId", order.getCustomerId());
AWSXRay.getCurrentSegment().putMetadata("orderAmount", order.getTotal());
AWSXRay.getCurrentSegment().putMetadata("itemCount", order.getItems().size());
```

3. **Configure X-Ray Sampling**
```json
// sampling-rules.json
{
  "version": 2,
  "rules": [
    {
      "description": "Sample all errors",
      "host": "*",
      "http_method": "*",
      "url_path": "*",
      "fixed_target": 1,
      "rate": 1.0,
      "priority": 1,
      "attributes": {
        "http.status": "5*"
      }
    },
    {
      "description": "Sample slow requests",
      "host": "*",
      "http_method": "*",
      "url_path": "*",
      "fixed_target": 1,
      "rate": 1.0,
      "priority": 2,
      "attributes": {
        "response_time": ">2"
      }
    },
    {
      "description": "Sample 10% of normal traffic",
      "host": "*",
      "http_method": "*",
      "url_path": "*",
      "fixed_target": 1,
      "rate": 0.1,
      "priority": 100
    }
  ],
  "default": {
    "fixed_target": 1,
    "rate": 0.05
  }
}
```

**Root Cause Fixes**
- Optimize slow database queries
- Add caching for frequently accessed data
- Implement async processing for non-critical operations
- Add circuit breakers for external services
- Optimize serialization/deserialization

#### Prevention
- Monitor trace latency continuously in CloudWatch
- Set up CloudWatch alarms for slow traces (p95 > 2s)
- Regular performance testing
- Review X-Ray service map weekly
- Implement performance budgets

---

## Saga Pattern Failures

### Order Processing Saga Failure

#### Symptoms
- Order stuck in intermediate state
- Inventory reserved but payment failed
- Compensation not triggered automatically
- Inconsistent state across services
- Customer sees "processing" status indefinitely

#### Detection
```bash
# Check saga state via application endpoint
kubectl exec -it deployment/order-service -n production -- \
  curl localhost:8080/api/v1/sagas/status?orderId=<order-id>

# Check for stuck sagas in logs
kubectl logs deployment/order-service -n production | \
  grep "Saga failed" | tail -20

# Check saga metrics
kubectl exec -it deployment/order-service -n production -- \
  curl localhost:8080/actuator/metrics/saga.failures
```

#### Diagnosis

**Step 1: Identify Saga State**
```sql
-- Query saga state from database
SELECT 
    saga_id, 
    saga_type, 
    current_step, 
    status, 
    error_message,
    created_at,
    updated_at
FROM saga_state
WHERE order_id = '<order-id>'
ORDER BY updated_at DESC;

-- Check saga execution history
SELECT 
    step_name,
    status,
    executed_at,
    error_message
FROM saga_execution_log
WHERE saga_id = '<saga-id>'
ORDER BY executed_at;
```

**Step 2: Check Compensation Status**
```bash
# Verify compensation events in Kafka
kubectl exec -it kafka-client -n production -- \
  kafka-console-consumer.sh --bootstrap-server <msk-broker>:9092 \
  --topic compensation-events \
  --from-beginning | grep "<order-id>"

# Check compensation logs
kubectl logs deployment/order-service -n production | \
  grep "Compensation" | grep "<order-id>"
```

**Step 3: Analyze Failure Point**
```bash
# Check which step failed
kubectl logs deployment/order-service -n production | \
  grep "Saga step failed" | grep "<saga-id>"

# Verify service availability at failure time
kubectl get events -n production --sort-by='.lastTimestamp' | \
  grep -E "payment-service|inventory-service"
```

**Step 4: Check Event Delivery**
```bash
# Verify events were published
kubectl logs deployment/order-service -n production | \
  grep "Event published" | grep "<order-id>"

# Check Kafka consumer lag for saga events
kubectl exec -it kafka-client -n production -- \
  kafka-consumer-groups.sh --bootstrap-server <msk-broker>:9092 \
  --describe --group saga-coordinator-group
```

#### Resolution

**Immediate Actions**

1. **Manual Compensation**
```bash
# Trigger manual compensation via API
curl -X POST http://order-service:8080/api/v1/sagas/<saga-id>/compensate \
  -H "Content-Type: application/json"

# Verify compensation execution
curl http://order-service:8080/api/v1/sagas/<saga-id>/status
```

2. **Retry Failed Step**
```bash
# Retry specific saga step
curl -X POST http://order-service:8080/api/v1/sagas/<saga-id>/retry \
  -H "Content-Type: application/json" \
  -d '{"step": "process-payment"}'
```

3. **Verify Compensating Transactions**
```java
// Example compensation logic
@Component
public class OrderSagaCompensation {
    
    @TransactionalEventListener
    public void compensateInventoryReservation(InventoryReservationFailed event) {
        // Release reserved inventory
        inventoryService.releaseReservation(event.getOrderId());
        
        // Publish compensation event
        eventPublisher.publish(InventoryReservationCompensated.create(
            event.getOrderId(),
            event.getReservationId()
        ));
    }
    
    @TransactionalEventListener
    public void compensatePayment(PaymentProcessingFailed event) {
        // Refund payment if already processed
        if (event.getPaymentId() != null) {
            paymentService.refund(event.getPaymentId());
        }
        
        // Publish compensation event
        eventPublisher.publish(PaymentCompensated.create(
            event.getOrderId(),
            event.getPaymentId()
        ));
    }
}
```

**Root Cause Fixes**
- Implement idempotent compensation operations
- Add saga timeout handling with automatic compensation
- Implement saga recovery mechanism for stuck sagas
- Add comprehensive error handling and retry logic
- Ensure all saga steps publish events reliably

#### Prevention
- Monitor saga completion rates in CloudWatch
- Set up alarms for stuck sagas (> 5 minutes in progress)
- Implement saga timeout policies (e.g., 10 minutes max)
- Regular saga state cleanup for completed sagas
- Test compensation logic thoroughly in staging
- Implement saga state machine visualization

---

## Eventual Consistency Issues

### Data Inconsistency Between Services

#### Symptoms
- Customer sees different data in different views
- Order total doesn't match inventory records
- Payment processed but order not confirmed
- Stale data displayed in UI
- Read-after-write inconsistency

#### Detection
```bash
# Check event processing lag
kubectl exec -it deployment/order-service -n production -- \
  curl localhost:8080/actuator/metrics/event.processing.lag

# Verify data consistency across services
curl http://order-service:8080/api/v1/orders/<order-id>
curl http://inventory-service:8080/api/v1/inventory/order/<order-id>
curl http://payment-service:8080/api/v1/payments/order/<order-id>

# Check Kafka consumer lag
kubectl exec -it kafka-client -n production -- \
  kafka-consumer-groups.sh --bootstrap-server <msk-broker>:9092 \
  --describe --group inventory-consumer-group
```

#### Diagnosis

**Step 1: Check Event Delivery**
```bash
# Verify events were published
kubectl logs deployment/order-service -n production | \
  grep "Event published" | grep "<order-id>"

# Check if events were consumed
kubectl logs deployment/inventory-service -n production | \
  grep "Event received" | grep "<order-id>"

# Check Kafka topic for events
kubectl exec -it kafka-client -n production -- \
  kafka-console-consumer.sh --bootstrap-server <msk-broker>:9092 \
  --topic order-events \
  --from-beginning | grep "<order-id>"
```

**Step 2: Identify Consistency Window**
```bash
# Compare event timestamps
# Event published time
kubectl logs deployment/order-service -n production | \
  grep "OrderCreated" | grep "<order-id>" | \
  awk '{print $1, $2}'

# Event processed time
kubectl logs deployment/inventory-service -n production | \
  grep "OrderCreated" | grep "<order-id>" | \
  awk '{print $1, $2}'
```

**Step 3: Check for Failed Events**
```bash
# Check dead letter queue
kubectl exec -it kafka-client -n production -- \
  kafka-console-consumer.sh --bootstrap-server <msk-broker>:9092 \
  --topic order-events-dlq \
  --from-beginning | grep "<order-id>"

# Check event processing errors
kubectl logs deployment/inventory-service -n production | \
  grep "Event processing failed" | grep "<order-id>"
```

#### Resolution

**Immediate Actions**

1. **Replay Failed Events from DLQ**
```bash
# Consume from DLQ and republish to main topic
kubectl exec -it kafka-client -n production -- bash -c '
kafka-console-consumer.sh --bootstrap-server <msk-broker>:9092 \
  --topic order-events-dlq \
  --from-beginning \
  --max-messages 100 | \
kafka-console-producer.sh --bootstrap-server <msk-broker>:9092 \
  --topic order-events
'
```

2. **Force Synchronization**
```bash
# Trigger manual sync for specific order
curl -X POST http://inventory-service:8080/api/v1/sync/order/<order-id>

# Verify synchronization
curl http://inventory-service:8080/api/v1/inventory/order/<order-id>
```

3. **Implement Read-Your-Writes Consistency**
```java
// Return version/timestamp with write response
@PostMapping("/orders")
public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
    Order order = orderService.createOrder(request);
    
    // Include version for consistency checking
    return ResponseEntity.ok()
        .header("X-Resource-Version", order.getVersion().toString())
        .header("X-Event-Timestamp", order.getEventTimestamp().toString())
        .header("ETag", order.getVersion().toString())
        .body(OrderResponse.from(order));
}

// Client can poll with version until consistent
@GetMapping("/orders/{id}")
public ResponseEntity<OrderResponse> getOrder(
    @PathVariable String id,
    @RequestHeader(value = "If-None-Match", required = false) String version
) {
    Order order = orderService.findById(id);
    
    // Return 304 if version matches (not yet consistent)
    if (version != null && version.equals(order.getVersion().toString())) {
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
    }
    
    return ResponseEntity.ok()
        .eTag(order.getVersion().toString())
        .body(OrderResponse.from(order));
}
```

**Root Cause Fixes**
- Implement event versioning and ordering
- Add idempotency keys to prevent duplicate processing
- Implement event replay mechanism
- Add consistency checks and reconciliation jobs
- Use outbox pattern for reliable event publishing

#### Prevention
- Monitor event processing lag in CloudWatch
- Implement eventual consistency UI patterns (loading states, optimistic updates)
- Add version vectors for conflict detection
- Regular consistency audits (daily reconciliation jobs)
- Document consistency guarantees in API documentation
- Implement compensating reads for critical data

---

## Cross-Service Transaction Failures

### Distributed Transaction Rollback

#### Symptoms
- Transaction partially committed across services
- Some services updated, others not
- Data inconsistency across bounded contexts
- Transaction timeout errors
- Orphaned data in some services

#### Detection
```bash
# Check transaction logs
kubectl logs deployment/order-service -n production | \
  grep "Transaction" | grep "failed"

# Monitor transaction metrics
kubectl exec -it deployment/order-service -n production -- \
  curl localhost:8080/actuator/metrics/transaction.failures
```

#### Diagnosis

**Step 1: Identify Transaction Scope**
```bash
# Check which services were involved
kubectl logs deployment/order-service -n production | \
  grep "Transaction ID: <tx-id>"

# Verify transaction state in each service
for service in order payment inventory; do
  echo "Checking $service..."
  curl http://${service}-service:8080/api/v1/transactions/<tx-id>/status
done
```

**Step 2: Analyze Failure Cause**
Common causes:
- Network timeout between services
- Service unavailability during transaction
- Database deadlock
- Resource exhaustion (connection pool, memory)
- Timeout configuration mismatch

**Step 3: Check Service Health at Failure Time**
```bash
# Check service events around failure time
kubectl get events -n production --sort-by='.lastTimestamp' | \
  grep -E "order-service|payment-service|inventory-service"

# Check service logs for errors
kubectl logs deployment/payment-service -n production \
  --since-time="2025-10-25T10:00:00Z" | grep -i error
```

#### Resolution

**Immediate Actions**

1. **Use Saga Pattern Instead**
```java
// Replace distributed transactions with saga pattern
@Service
public class OrderSagaOrchestrator {
    
    public void processOrder(Order order) {
        // Step 1: Reserve inventory
        InventoryReservation reservation = inventoryService.reserve(order.getItems());
        
        try {
            // Step 2: Process payment
            Payment payment = paymentService.process(order.getPaymentInfo());
            
            try {
                // Step 3: Confirm order
                orderService.confirm(order.getId());
                
            } catch (Exception e) {
                // Compensate: Refund payment
                paymentService.refund(payment.getId());
                throw e;
            }
            
        } catch (Exception e) {
            // Compensate: Release inventory
            inventoryService.release(reservation.getId());
            throw e;
        }
    }
}
```

2. **Implement Idempotent Operations**
```java
@Service
public class PaymentService {
    
    @Transactional
    public Payment processPayment(PaymentRequest request) {
        // Check if already processed (idempotency)
        Optional<Payment> existing = paymentRepository
            .findByIdempotencyKey(request.getIdempotencyKey());
        
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // Process payment
        Payment payment = new Payment(request);
        return paymentRepository.save(payment);
    }
}
```

**Root Cause Fixes**
- Avoid distributed transactions when possible
- Use saga pattern for long-running business processes
- Implement compensating transactions
- Add proper timeout handling
- Use event-driven architecture for loose coupling

#### Prevention
- Design for eventual consistency
- Use saga pattern for cross-service workflows
- Implement idempotent operations
- Monitor transaction success rates
- Set appropriate timeouts (consider network latency)
- Regular testing of failure scenarios

---

## Circuit Breaker Issues

### Circuit Breaker Open State

#### Symptoms
- Service calls failing immediately without attempting
- "Circuit breaker is open" errors in logs
- Fallback responses returned to clients
- Service appears unavailable even when healthy
- Increased error rates in dependent services

#### Detection
```bash
# Check circuit breaker state via Actuator
kubectl exec -it deployment/order-service -n production -- \
  curl localhost:8080/actuator/health | jq '.components.circuitBreakers'

# Monitor circuit breaker metrics
kubectl exec -it deployment/order-service -n production -- \
  curl localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state
```

#### Diagnosis

**Step 1: Check Circuit Breaker Configuration**
```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        failure-rate-threshold: 50  # Open if 50% of calls fail
        wait-duration-in-open-state: 60s  # Wait before trying half-open
        permitted-number-of-calls-in-half-open-state: 3
        sliding-window-size: 10  # Number of calls to track
        sliding-window-type: COUNT_BASED
        minimum-number-of-calls: 5  # Min calls before calculating rate
        slow-call-rate-threshold: 100  # Threshold for slow calls
        slow-call-duration-threshold: 2s  # What constitutes a slow call
```

**Step 2: Verify Target Service Health**
```bash
# Check if target service is healthy
kubectl get pods -n production -l app=payment-service
kubectl logs deployment/payment-service -n production --tail=50

# Test service directly
curl http://payment-service:8080/actuator/health

# Check service metrics
kubectl exec -it deployment/payment-service -n production -- \
  curl localhost:8080/actuator/metrics/http.server.requests
```

**Step 3: Review Failure History**
```bash
# Check recent failures that triggered circuit breaker
kubectl logs deployment/order-service -n production | \
  grep "Circuit breaker" | tail -30

# Check failure rate
kubectl exec -it deployment/order-service -n production -- \
  curl localhost:8080/actuator/metrics/resilience4j.circuitbreaker.failure.rate
```

#### Resolution

**Immediate Actions**

1. **Fix Target Service**
```bash
# Restart unhealthy service
kubectl rollout restart deployment/payment-service -n production

# Verify service recovery
kubectl rollout status deployment/payment-service -n production

# Check service health
kubectl exec -it deployment/payment-service -n production -- \
  curl localhost:8080/actuator/health
```

2. **Monitor Circuit Breaker Recovery**
```bash
# Watch circuit breaker state transitions
watch -n 1 'kubectl exec -it deployment/order-service -n production -- \
  curl -s localhost:8080/actuator/health | \
  jq ".components.circuitBreakers.details.paymentService.state"'
```

3. **Implement Better Fallback Logic**
```java
@Service
public class OrderService {
    
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    @Retry(name = "paymentService")
    @Bulkhead(name = "paymentService")
    public PaymentResult processPayment(PaymentRequest request) {
        return paymentClient.process(request);
    }
    
    // Fallback method
    private PaymentResult paymentFallback(PaymentRequest request, Exception ex) {
        log.warn("Payment service unavailable, using fallback", ex);
        
        // Queue for later processing
        paymentQueue.enqueue(request);
        
        // Return pending status
        return PaymentResult.pending(request.getOrderId());
    }
}
```

**Root Cause Fixes**
- Fix underlying service issues
- Adjust circuit breaker thresholds based on actual behavior
- Implement meaningful fallback logic
- Add retry mechanisms with exponential backoff
- Improve service resilience

#### Prevention
- Monitor circuit breaker state changes in CloudWatch
- Set up alarms for open circuits
- Regular health checks on dependencies
- Test fallback mechanisms in staging
- Document circuit breaker behavior
- Implement gradual recovery patterns

---

## Rate Limiting and Throttling

### Application-Level Rate Limiting

#### Symptoms
- 429 Too Many Requests responses
- Clients receiving rate limit errors
- Legitimate traffic being blocked
- Uneven rate limit distribution across instances

#### Detection
```bash
# Check rate limit metrics via Actuator
kubectl exec -it deployment/api-gateway -n production -- \
  curl localhost:8080/actuator/metrics/http.server.requests | \
  jq '.measurements[] | select(.statistic=="COUNT") | select(.value > 0)' | \
  grep "status=429"

# Monitor rate limit violations
kubectl logs deployment/api-gateway -n production | \
  grep "Rate limit exceeded" | wc -l
```

#### Diagnosis

**Step 1: Identify Rate Limited Clients**
```bash
# Check rate limit logs
kubectl logs deployment/api-gateway -n production | \
  grep "Rate limit exceeded" | \
  awk '{print $5}' | sort | uniq -c | sort -rn | head -20

# Check client request patterns
kubectl logs deployment/api-gateway -n production | \
  grep "client-id" | \
  awk '{print $5, $8}' | sort | uniq -c
```

**Step 2: Review Rate Limit Configuration**
```java
// Rate limiting using Bucket4j with Redis
@Configuration
public class RateLimitConfiguration {
    
    @Bean
    public RateLimiter rateLimiter(RedisTemplate<String, String> redisTemplate) {
        return RateLimiter.builder()
            .withBandwidth(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
            .withRedisBackend(redisTemplate)
            .build();
    }
}

// Apply rate limiting
@RestController
public class OrderController {
    
    @GetMapping("/orders")
    @RateLimit(key = "#request.getHeader('X-Client-ID')", limit = 100, period = "1m")
    public List<Order> getOrders(HttpServletRequest request) {
        return orderService.findAll();
    }
}
```

**Step 3: Check Redis Rate Limiter State**
```bash
# Connect to Redis
kubectl exec -it redis-0 -n production -- redis-cli

# Check rate limit keys
KEYS ratelimit:*

# Check specific client rate limit
GET ratelimit:<client-id>:tokens
TTL ratelimit:<client-id>:tokens

# Check rate limit bucket state
HGETALL ratelimit:<client-id>:bucket
```

#### Resolution

**Immediate Actions**

1. **Increase Rate Limits** (if legitimate traffic)
```java
// Adjust rate limits in configuration
@Configuration
public class RateLimitConfiguration {
    
    @Bean
    public Map<String, RateLimitConfig> rateLimitConfigs() {
        return Map.of(
            "default", RateLimitConfig.builder()
                .limit(100)
                .period(Duration.ofMinutes(1))
                .build(),
            "premium", RateLimitConfig.builder()
                .limit(1000)  // Higher limit for premium clients
                .period(Duration.ofMinutes(1))
                .build()
        );
    }
}
```

2. **Implement Tiered Rate Limiting**
```java
@Component
public class TieredRateLimiter {
    
    public boolean allowRequest(String clientId, String tier) {
        RateLimitConfig config = getRateLimitConfig(tier);
        
        String key = "ratelimit:" + clientId + ":" + tier;
        Long current = redisTemplate.opsForValue().increment(key);
        
        if (current == 1) {
            redisTemplate.expire(key, config.getPeriod());
        }
        
        return current <= config.getLimit();
    }
    
    private RateLimitConfig getRateLimitConfig(String tier) {
        return switch (tier) {
            case "premium" -> new RateLimitConfig(1000, Duration.ofMinutes(1));
            case "standard" -> new RateLimitConfig(100, Duration.ofMinutes(1));
            default -> new RateLimitConfig(10, Duration.ofMinutes(1));
        };
    }
}
```

3. **Add Rate Limit Headers**
```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String clientId = request.getHeader("X-Client-ID");
        RateLimitResult result = rateLimiter.checkLimit(clientId);
        
        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.getResetTime()));
        
        if (!result.isAllowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(result.getRetryAfter()));
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}
```

**Root Cause Fixes**
- Implement proper client identification
- Add rate limit headers in responses
- Implement exponential backoff in clients
- Monitor rate limit usage patterns
- Implement distributed rate limiting with Redis

#### Prevention
- Monitor rate limit usage in CloudWatch
- Set up alarms for high violation rates
- Implement rate limit headers
- Document rate limits in API documentation
- Regular rate limit capacity planning
- Implement client SDK with built-in rate limiting

---

### Service Throttling Under Load

#### Symptoms
- Slow response times under high load
- Service rejecting requests
- Queue buildup
- Resource exhaustion (CPU, memory, connections)
- Increased error rates

#### Detection
```bash
# Check service metrics
kubectl exec -it deployment/order-service -n production -- \
  curl localhost:8080/actuator/metrics/http.server.requests.active

# Monitor thread pool usage
kubectl exec -it deployment/order-service -n production -- \
  curl localhost:8080/actuator/metrics/executor.active

# Check resource utilization
kubectl top pods -n production -l app=order-service
```

#### Diagnosis

**Step 1: Check Thread Pool Configuration**
```yaml
# application.yml
server:
  tomcat:
    threads:
      max: 200  # Maximum threads
      min-spare: 10  # Minimum idle threads
    max-connections: 10000  # Maximum connections
    accept-count: 100  # Queue size
    connection-timeout: 20s
```

**Step 2: Analyze Request Queue**
```bash
# Check queue depth
kubectl exec -it deployment/order-service -n production -- \
  curl localhost:8080/actuator/metrics/executor.queue.remaining

# Check rejected tasks
kubectl exec -it deployment/order-service -n production -- \
  curl localhost:8080/actuator/metrics/executor.rejected
```

**Step 3: Identify Bottlenecks**
Common causes:
- Database connection pool exhaustion
- External service timeouts
- Memory pressure causing GC pauses
- CPU saturation
- Network bandwidth limits

#### Resolution

**Immediate Actions**

1. **Scale Service Horizontally**
```bash
# Increase replicas
kubectl scale deployment order-service -n production --replicas=10

# Verify scaling
kubectl get pods -n production -l app=order-service -w
```

2. **Increase Thread Pool Size**
```yaml
server:
  tomcat:
    threads:
      max: 400  # Increase
      min-spare: 20
```

3. **Implement Bulkhead Pattern**
```java
@Configuration
public class BulkheadConfiguration {
    
    @Bean
    public ThreadPoolBulkhead orderProcessingBulkhead() {
        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(10)
            .coreThreadPoolSize(5)
            .queueCapacity(100)
            .build();
        
        return ThreadPoolBulkhead.of("orderProcessing", config);
    }
}

@Service
public class OrderService {
    
    @Bulkhead(name = "orderProcessing", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<Order> processOrder(OrderRequest request) {
        // Isolated thread pool for order processing
        return CompletableFuture.supplyAsync(() -> {
            return orderProcessor.process(request);
        });
    }
}
```

**Root Cause Fixes**
- Optimize slow operations
- Implement async processing
- Add caching for frequently accessed data
- Optimize database queries
- Implement load shedding for non-critical requests

#### Prevention
- Monitor service capacity in CloudWatch
- Implement auto-scaling based on metrics
- Regular load testing
- Capacity planning based on growth projections
- Implement graceful degradation
- Use HPA (Horizontal Pod Autoscaler) in Kubernetes

---

## Monitoring and Alerting

### Key Metrics to Monitor

#### Event-Driven Architecture
- **Consumer Lag**: Alert if > 1000 messages for > 5 minutes
- **Rebalance Frequency**: Alert if > 5 rebalances/hour
- **Event Processing Rate**: Monitor throughput
- **Dead Letter Queue Size**: Alert if > 100 messages

#### Distributed Tracing
- **Trace Latency**: P95, P99 response times
- **Trace Error Rate**: Alert if > 1%
- **Missing Spans**: Alert if > 5%
- **Service Dependency Health**: Monitor all dependencies

#### Saga Patterns
- **Saga Completion Rate**: Alert if < 95%
- **Compensation Execution Rate**: Monitor frequency
- **Stuck Saga Count**: Alert if > 10
- **Saga Duration**: P95, P99 durations

#### Eventual Consistency
- **Event Processing Lag**: Alert if > 5 seconds
- **Consistency Window Duration**: Monitor average time
- **Failed Event Count**: Alert if > 10/minute
- **DLQ Message Count**: Alert if increasing

#### Circuit Breakers
- **Circuit Breaker State**: Alert on state changes
- **State Transition Frequency**: Monitor oscillations
- **Fallback Execution Rate**: Monitor usage
- **Half-Open Test Success Rate**: Track recovery

#### Rate Limiting
- **Rate Limit Violations**: Alert if > 100/minute
- **Client Request Distribution**: Monitor patterns
- **Rate Limit Utilization**: Track usage per tier
- **Throttled Request Percentage**: Alert if > 5%

### CloudWatch Alarms Configuration

```bash
# Create alarm for high consumer lag
aws cloudwatch put-metric-alarm \
  --alarm-name high-consumer-lag \
  --alarm-description "Alert when consumer lag exceeds 1000" \
  --metric-name EstimatedMaxTimeLag \
  --namespace AWS/Kafka \
  --statistic Average \
  --period 300 \
  --evaluation-periods 2 \
  --threshold 1000 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=Consumer Group,Value=order-processing-group

# Create alarm for circuit breaker open
aws cloudwatch put-metric-alarm \
  --alarm-name circuit-breaker-open \
  --alarm-description "Alert when circuit breaker opens" \
  --metric-name CircuitBreakerState \
  --namespace CustomMetrics \
  --statistic Maximum \
  --period 60 \
  --evaluation-periods 1 \
  --threshold 1 \
  --comparison-operator GreaterThanOrEqualToThreshold

# Create alarm for saga failures
aws cloudwatch put-metric-alarm \
  --alarm-name high-saga-failure-rate \
  --alarm-description "Alert when saga failure rate is high" \
  --metric-name SagaFailures \
  --namespace CustomMetrics \
  --statistic Sum \
  --period 300 \
  --evaluation-periods 1 \
  --threshold 10 \
  --comparison-operator GreaterThanThreshold
```

---

## Related Documentation

- [Kubernetes Issues](kubernetes-issues.md)
- [Database Issues](database-issues.md)
- [Performance Issues](performance-issues.md)
- [Monitoring Guide](../monitoring/monitoring-alerting.md)
- [Deployment Viewpoint](../../viewpoints/deployment/overview.md)
- [Operational Viewpoint](../../viewpoints/operational/overview.md)

---

## Escalation Procedures

### Level 1: On-Call Engineer
- Initial triage and diagnosis
- Apply immediate fixes (scaling, restarts)
- Monitor recovery
- Document incident

### Level 2: Senior Engineer
- Complex distributed system issues
- Saga pattern failures requiring manual intervention
- Cross-service transaction issues
- Performance optimization

### Level 3: Architecture Team
- System-wide consistency issues
- Architecture changes needed
- Major incident coordination
- Post-mortem analysis

### External Support
- **AWS Support**: MSK, EKS, RDS infrastructure issues
- **Vendor Support**: Third-party service issues
- **Security Team**: Security-related incidents

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-25  
**Owner**: Operations Team  
**Review Frequency**: Quarterly
