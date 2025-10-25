---
adr_number: 029
title: "Background Job Processing Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [005, 022, 028]
affected_viewpoints: ["concurrency", "deployment", "operational"]
affected_perspectives: ["performance", "scalability", "availability"]
---

# ADR-029: Background Job Processing Strategy

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform requires background job processing for:

- **Asynchronous Operations**: Email sending, notifications, report generation
- **Scheduled Tasks**: Daily inventory sync, order status updates, abandoned cart reminders
- **Long-Running Operations**: Bulk imports, data exports, image processing
- **Retry Logic**: Failed payment retries, external API call retries
- **Batch Processing**: End-of-day reconciliation, analytics aggregation
- **Event Processing**: Domain event handlers that don't need immediate execution
- **Resource-Intensive Tasks**: PDF generation, video transcoding, data analysis

### Business Context

**Business Drivers**:
- Improve user experience (don't block on slow operations)
- Handle peak loads (queue jobs during traffic spikes)
- Ensure reliability (retry failed operations automatically)
- Support scheduled operations (daily reports, reminders)
- Enable scalability (process jobs independently)
- Maintain system responsiveness (offload heavy tasks)

**Business Constraints**:
- Budget: $300/month for job processing infrastructure
- Job processing latency < 5 minutes for high-priority jobs
- Support for job priorities (critical, high, normal, low)
- Job retry with exponential backoff
- Job monitoring and alerting
- Dead letter queue for failed jobs

### Technical Context

**Current State**:
- Spring Boot 3.4.5 application
- Kafka for event streaming (ADR-005)
- Redis for distributed caching (ADR-022)
- AWS cloud infrastructure
- Multiple application instances (horizontal scaling)

**Requirements**:
- Distributed job processing (multiple workers)
- Job scheduling (cron-like)
- Job priorities and queues
- Retry mechanism with exponential backoff
- Dead letter queue for failed jobs
- Job monitoring and metrics
- Horizontal scalability
- At-least-once delivery guarantee
- Job persistence (survive restarts)

## Decision Drivers

1. **Reliability**: At-least-once delivery, automatic retries
2. **Scalability**: Handle 10,000+ jobs/hour
3. **Performance**: Low latency for high-priority jobs
4. **Features**: Scheduling, priorities, retries, DLQ
5. **Integration**: Spring Boot, AWS ecosystem
6. **Cost**: Within $300/month budget
7. **Operations**: Easy to monitor and troubleshoot
8. **Flexibility**: Support various job types

## Considered Options

### Option 1: Spring Boot @Async with Database Queue

**Description**: Use Spring's @Async with database-backed job queue

**Pros**:
- ✅ Simple to implement
- ✅ No additional infrastructure
- ✅ ACID transactions with jobs
- ✅ Built into Spring Boot
- ✅ Easy to debug

**Cons**:
- ❌ Poor scalability (database bottleneck)
- ❌ No built-in scheduling
- ❌ Limited retry logic
- ❌ No priority queues
- ❌ Polling overhead
- ❌ Doesn't survive application restarts well
- ❌ No distributed coordination

**Cost**: $0 (included in database)

**Risk**: **High** - Insufficient for requirements

**Performance**: 100-500 jobs/minute

### Option 2: AWS SQS + Spring Cloud AWS

**Description**: Use AWS SQS for job queues with Spring Cloud AWS integration

**Pros**:
- ✅ Fully managed service
- ✅ Highly scalable (unlimited throughput)
- ✅ At-least-once delivery
- ✅ Dead letter queue support
- ✅ Message visibility timeout (retry)
- ✅ FIFO queues available
- ✅ Excellent Spring Boot integration
- ✅ Pay-as-you-go pricing
- ✅ AWS native integration
- ✅ No operational overhead

**Cons**:
- ⚠️ No built-in scheduling (need EventBridge)
- ⚠️ Limited message size (256KB)
- ⚠️ Eventual consistency
- ⚠️ No priority within queue (need multiple queues)

**Cost**: 
- SQS: $0.40/million requests (first 1M free)
- EventBridge: $1.00/million events
- Total: $50-100/month (10M jobs/month)

**Risk**: **Low** - AWS managed service

**Performance**: 10,000+ jobs/second

### Option 3: Redis-Based Queue (Spring Data Redis)

**Description**: Use Redis as job queue with Spring Data Redis

**Pros**:
- ✅ Uses existing Redis infrastructure
- ✅ Fast (in-memory)
- ✅ Simple to implement
- ✅ Good Spring Boot integration
- ✅ Supports priorities (sorted sets)
- ✅ Pub/sub for notifications

**Cons**:
- ❌ No built-in scheduling
- ❌ Limited persistence (AOF/RDB)
- ❌ Manual retry logic
- ❌ No dead letter queue (need to implement)
- ❌ Memory constraints
- ❌ Complex distributed coordination

**Cost**: $0 (uses existing Redis)

**Risk**: **Medium** - Need to implement features

**Performance**: 5,000-10,000 jobs/second

### Option 4: Quartz Scheduler + Database

**Description**: Use Quartz Scheduler for job scheduling and execution

**Pros**:
- ✅ Mature scheduling library
- ✅ Cron-like scheduling
- ✅ Clustered mode (distributed)
- ✅ Job persistence
- ✅ Spring Boot integration
- ✅ Misfire handling

**Cons**:
- ❌ Database-backed (scalability limits)
- ❌ Complex configuration
- ❌ Not designed for high-throughput queues
- ❌ Limited retry logic
- ❌ No dead letter queue
- ❌ Polling overhead

**Cost**: $0 (uses existing database)

**Risk**: **Medium** - Scalability concerns

**Performance**: 100-1,000 jobs/minute

### Option 5: Apache Kafka (Existing Infrastructure)

**Description**: Use existing Kafka infrastructure for job processing

**Pros**:
- ✅ Already deployed (ADR-005)
- ✅ High throughput
- ✅ Durable (replicated)
- ✅ Ordered processing
- ✅ Exactly-once semantics
- ✅ Good for event-driven jobs

**Cons**:
- ❌ No built-in scheduling
- ❌ Complex retry logic
- ❌ No priority queues
- ❌ Overkill for simple jobs
- ❌ Higher operational complexity

**Cost**: $0 (existing infrastructure)

**Risk**: **Medium** - Complex for simple jobs

**Performance**: 100,000+ messages/second

## Decision Outcome

**Chosen Option**: **AWS SQS + Spring Cloud AWS + EventBridge Scheduler**

### Rationale

AWS SQS with EventBridge was selected for the following reasons:

1. **Fully Managed**: No operational overhead, AWS handles scaling
2. **Reliability**: At-least-once delivery, automatic retries
3. **Scalability**: Unlimited throughput, handles 10,000+ jobs/second
4. **Cost-Effective**: Pay-as-you-go, within budget ($50-100/month)
5. **Features**: Dead letter queue, visibility timeout, FIFO queues
6. **Integration**: Excellent Spring Boot support via Spring Cloud AWS
7. **Scheduling**: EventBridge for cron-like scheduling
8. **AWS Native**: Seamless integration with other AWS services
9. **Proven**: Used by millions of applications worldwide

**Job Processing Architecture**:

**Queue Structure**:
- `ecommerce-jobs-critical`: Critical jobs (payment processing, order confirmation)
- `ecommerce-jobs-high`: High-priority jobs (email sending, notifications)
- `ecommerce-jobs-normal`: Normal jobs (report generation, data sync)
- `ecommerce-jobs-low`: Low-priority jobs (analytics, cleanup)
- `ecommerce-jobs-dlq`: Dead letter queue for failed jobs

**Job Types**:
- **Immediate Jobs**: Processed as soon as possible (email, notifications)
- **Scheduled Jobs**: Triggered by EventBridge (daily reports, reminders)
- **Delayed Jobs**: Delayed execution (abandoned cart after 1 hour)
- **Recurring Jobs**: Periodic execution (inventory sync every 15 minutes)

**Retry Strategy**:
- **Automatic Retries**: SQS visibility timeout (exponential backoff)
- **Max Retries**: 3 attempts before moving to DLQ
- **Backoff**: 1 minute, 5 minutes, 15 minutes
- **DLQ Processing**: Manual review and reprocessing

**EventBridge Scheduling**:
- Daily reports: 8:00 AM UTC
- Inventory sync: Every 15 minutes
- Abandoned cart reminders: Every hour
- Order status updates: Every 5 minutes
- Analytics aggregation: Midnight UTC

**Why Not @Async**: Poor scalability, limited features, doesn't survive restarts.

**Why Not Redis Queue**: Need to implement many features, memory constraints.

**Why Not Quartz**: Database bottleneck, not designed for high-throughput queues.

**Why Not Kafka**: Overkill for simple jobs, complex retry logic.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Learn SQS and EventBridge | Training, code examples, documentation |
| Operations Team | Low | Monitor SQS and EventBridge | AWS managed service, CloudWatch dashboards |
| End Users | Positive | Faster response times, better UX | N/A |
| Business | Positive | Reliable job processing, scalability | N/A |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:
- All bounded contexts (job processing)
- Application services (job submission)
- Infrastructure layer (SQS client, EventBridge)
- Notification service (email, SMS)
- Report generation service
- Data synchronization services

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| SQS unavailability | Very Low | High | Fallback to database queue, circuit breaker |
| Job processing delays | Low | Medium | Monitor queue depth, scale workers |
| DLQ overflow | Low | Medium | Alerting, automated reprocessing |
| Cost overrun | Low | Low | Monitor usage, set alarms |
| Message loss | Very Low | High | At-least-once delivery, DLQ |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Setup (Week 1)

- [x] Create SQS queues (critical, high, normal, low, DLQ)
- [x] Configure queue policies and IAM roles
- [x] Set up EventBridge scheduler
- [x] Configure CloudWatch alarms
- [x] Create monitoring dashboards

### Phase 2: Integration (Week 2-3)

- [x] Integrate Spring Cloud AWS
- [x] Implement job submission service
- [x] Implement job processing workers
- [x] Add retry logic and error handling
- [x] Implement DLQ processing

### Phase 3: Job Types (Week 4-5)

- [x] Implement email sending jobs
- [x] Implement notification jobs
- [x] Implement report generation jobs
- [x] Implement data sync jobs
- [x] Implement scheduled jobs (EventBridge)

### Phase 4: Testing & Optimization (Week 6)

- [x] Load testing (10,000+ jobs/hour)
- [x] Failure testing (retry, DLQ)
- [x] Performance optimization
- [x] Documentation and training

### Rollback Strategy

**Trigger Conditions**:
- Job processing failure rate > 5%
- Queue depth > 10,000 messages
- SQS errors > 1%
- Cost exceeds budget by > 50%

**Rollback Steps**:
1. Disable job submission to SQS
2. Fall back to synchronous processing
3. Investigate and fix issues
4. Re-enable SQS gradually
5. Monitor performance

**Rollback Time**: < 1 hour

## Monitoring and Success Criteria

### Success Metrics

- ✅ Job processing success rate > 99%
- ✅ Job processing latency < 5 minutes (high-priority)
- ✅ Queue depth < 1,000 messages
- ✅ DLQ messages < 1% of total
- ✅ Cost within budget ($100/month)
- ✅ Zero job loss incidents

### Monitoring Plan

**CloudWatch Metrics**:
- SQS Queue Depth (per queue)
- SQS Messages Sent/Received
- SQS Message Age
- SQS DLQ Messages
- EventBridge Invocations
- EventBridge Failed Invocations
- Lambda Duration (for EventBridge targets)

**Application Metrics**:
```java
@Component
public class JobProcessingMetrics {
    private final Timer jobProcessingTime;
    private final Counter jobsProcessed;
    private final Counter jobsFailed;
    private final Gauge queueDepth;
    
    // Track job processing performance
}
```

**Alerts**:
- Queue depth > 5,000 messages
- Message age > 30 minutes
- DLQ messages > 100
- Job failure rate > 5%
- EventBridge failures > 1%
- Cost > $150/month

**Review Schedule**:
- Daily: Check job processing metrics
- Weekly: Review failed jobs and DLQ
- Monthly: Optimize job processing
- Quarterly: Job processing strategy review

## Consequences

### Positive Consequences

- ✅ **Reliability**: At-least-once delivery, automatic retries
- ✅ **Scalability**: Unlimited throughput, handles peak loads
- ✅ **Performance**: Fast job processing, low latency
- ✅ **Cost-Effective**: Pay-as-you-go, within budget
- ✅ **Managed Service**: No operational overhead
- ✅ **Flexibility**: Support various job types and priorities
- ✅ **Monitoring**: Comprehensive metrics and alerting

### Negative Consequences

- ⚠️ **Complexity**: Additional AWS services to manage
- ⚠️ **Eventual Consistency**: Jobs processed asynchronously
- ⚠️ **Debugging**: Harder to trace job execution
- ⚠️ **Cost**: Additional AWS service costs
- ⚠️ **Learning Curve**: Team needs to learn SQS and EventBridge

### Technical Debt

**Identified Debt**:
1. Manual DLQ processing (can automate with Lambda)
2. Simple retry logic (can add exponential backoff with jitter)
3. No job prioritization within queue (future enhancement)

**Debt Repayment Plan**:
- **Q2 2026**: Implement automated DLQ processing
- **Q3 2026**: Add advanced retry strategies
- **Q4 2026**: Implement job prioritization and scheduling optimization

## Related Decisions

- [ADR-005: Use Apache Kafka for Event Streaming](005-use-kafka-for-event-streaming.md) - Kafka for event-driven jobs
- [ADR-022: Distributed Locking with Redis](022-distributed-locking-with-redis.md) - Locking for job coordination
- [ADR-028: File Storage Strategy with S3](028-file-storage-strategy-with-s3.md) - S3 for job artifacts

## Notes

### SQS Queue Configuration

```yaml
# SQS Queue: ecommerce-jobs-critical
Visibility Timeout: 30 seconds
Message Retention: 4 days
Receive Wait Time: 20 seconds (long polling)
Dead Letter Queue: ecommerce-jobs-dlq
Max Receive Count: 3
Encryption: SSE-SQS

# SQS Queue: ecommerce-jobs-high
Visibility Timeout: 60 seconds
Message Retention: 4 days
Receive Wait Time: 20 seconds
Dead Letter Queue: ecommerce-jobs-dlq
Max Receive Count: 3

# SQS Queue: ecommerce-jobs-normal
Visibility Timeout: 300 seconds (5 minutes)
Message Retention: 4 days
Receive Wait Time: 20 seconds
Dead Letter Queue: ecommerce-jobs-dlq
Max Receive Count: 3

# SQS Queue: ecommerce-jobs-low
Visibility Timeout: 600 seconds (10 minutes)
Message Retention: 4 days
Receive Wait Time: 20 seconds
Dead Letter Queue: ecommerce-jobs-dlq
Max Receive Count: 3

# SQS Queue: ecommerce-jobs-dlq
Message Retention: 14 days
Alarm: CloudWatch alarm when messages > 100
```

### Spring Boot SQS Integration

```java
@Configuration
public class SqsConfiguration {
    
    @Value("${aws.region}")
    private String region;
    
    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
            .region(Region.of(region))
            .build();
    }
    
    @Bean
    public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
        return SqsTemplate.builder()
            .sqsAsyncClient(sqsAsyncClient)
            .build();
    }
}

@Service
public class JobSubmissionService {
    
    @Autowired
    private SqsTemplate sqsTemplate;
    
    @Value("${aws.sqs.queue.critical}")
    private String criticalQueue;
    
    @Value("${aws.sqs.queue.high}")
    private String highQueue;
    
    @Value("${aws.sqs.queue.normal}")
    private String normalQueue;
    
    public void submitJob(Job job, JobPriority priority) {
        String queueUrl = getQueueUrl(priority);
        
        SendMessageRequest request = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(serializeJob(job))
            .messageAttributes(Map.of(
                "JobType", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(job.getType())
                    .build(),
                "Priority", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(priority.name())
                    .build()
            ))
            .build();
        
        sqsTemplate.send(request);
    }
    
    public void submitDelayedJob(Job job, Duration delay) {
        String queueUrl = getQueueUrl(job.getPriority());
        
        SendMessageRequest request = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(serializeJob(job))
            .delaySeconds((int) delay.getSeconds())
            .build();
        
        sqsTemplate.send(request);
    }
    
    private String getQueueUrl(JobPriority priority) {
        return switch (priority) {
            case CRITICAL -> criticalQueue;
            case HIGH -> highQueue;
            case NORMAL -> normalQueue;
            case LOW -> normalQueue;
        };
    }
}
```

### Job Processing Worker

```java
@Component
public class JobProcessingWorker {
    
    @Autowired
    private JobExecutor jobExecutor;
    
    @SqsListener(value = "${aws.sqs.queue.critical}", deletionPolicy = ON_SUCCESS)
    public void processCriticalJob(String message, Acknowledgment acknowledgment) {
        processJob(message, JobPriority.CRITICAL, acknowledgment);
    }
    
    @SqsListener(value = "${aws.sqs.queue.high}", deletionPolicy = ON_SUCCESS)
    public void processHighPriorityJob(String message, Acknowledgment acknowledgment) {
        processJob(message, JobPriority.HIGH, acknowledgment);
    }
    
    @SqsListener(value = "${aws.sqs.queue.normal}", deletionPolicy = ON_SUCCESS)
    public void processNormalJob(String message, Acknowledgment acknowledgment) {
        processJob(message, JobPriority.NORMAL, acknowledgment);
    }
    
    private void processJob(String message, JobPriority priority, Acknowledgment acknowledgment) {
        try {
            Job job = deserializeJob(message);
            
            // Execute job
            jobExecutor.execute(job);
            
            // Acknowledge successful processing
            acknowledgment.acknowledge();
            
            // Record metrics
            recordJobSuccess(job, priority);
            
        } catch (Exception e) {
            // Log error
            logger.error("Job processing failed", e);
            
            // Record metrics
            recordJobFailure(job, priority, e);
            
            // Don't acknowledge - message will be retried
            // After max retries, will move to DLQ
        }
    }
}

@Service
public class JobExecutor {
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private ReportService reportService;
    
    public void execute(Job job) {
        switch (job.getType()) {
            case "SEND_EMAIL" -> executeSendEmail(job);
            case "SEND_NOTIFICATION" -> executeSendNotification(job);
            case "GENERATE_REPORT" -> executeGenerateReport(job);
            case "SYNC_INVENTORY" -> executeSyncInventory(job);
            default -> throw new UnsupportedJobTypeException(job.getType());
        }
    }
    
    private void executeSendEmail(Job job) {
        EmailJobData data = job.getData(EmailJobData.class);
        emailService.sendEmail(data.getTo(), data.getSubject(), data.getBody());
    }
    
    private void executeSendNotification(Job job) {
        NotificationJobData data = job.getData(NotificationJobData.class);
        notificationService.sendNotification(data.getUserId(), data.getMessage());
    }
    
    private void executeGenerateReport(Job job) {
        ReportJobData data = job.getData(ReportJobData.class);
        reportService.generateReport(data.getReportType(), data.getParameters());
    }
}
```

### EventBridge Scheduler Configuration

```yaml
# EventBridge Rule: Daily Reports
Name: ecommerce-daily-reports
Schedule: cron(0 8 * * ? *)  # 8:00 AM UTC daily
Target: Lambda function to submit SQS job
Input:
  jobType: GENERATE_REPORT
  reportType: DAILY_SALES
  priority: NORMAL

# EventBridge Rule: Inventory Sync
Name: ecommerce-inventory-sync
Schedule: rate(15 minutes)
Target: Lambda function to submit SQS job
Input:
  jobType: SYNC_INVENTORY
  priority: HIGH

# EventBridge Rule: Abandoned Cart Reminders
Name: ecommerce-abandoned-cart
Schedule: rate(1 hour)
Target: Lambda function to submit SQS job
Input:
  jobType: SEND_ABANDONED_CART_REMINDER
  priority: NORMAL

# EventBridge Rule: Order Status Updates
Name: ecommerce-order-status
Schedule: rate(5 minutes)
Target: Lambda function to submit SQS job
Input:
  jobType: UPDATE_ORDER_STATUS
  priority: HIGH
```

### Lambda Function for EventBridge

```javascript
// Lambda function to submit jobs to SQS from EventBridge
const AWS = require('aws-sdk');
const sqs = new AWS.SQS();

exports.handler = async (event) => {
    const queueUrl = process.env.SQS_QUEUE_URL;
    
    const params = {
        QueueUrl: queueUrl,
        MessageBody: JSON.stringify(event),
        MessageAttributes: {
            'JobType': {
                DataType: 'String',
                StringValue: event.jobType
            },
            'Priority': {
                DataType: 'String',
                StringValue: event.priority
            },
            'Source': {
                DataType: 'String',
                StringValue: 'EventBridge'
            }
        }
    };
    
    try {
        await sqs.sendMessage(params).promise();
        return { statusCode: 200, body: 'Job submitted successfully' };
    } catch (error) {
        console.error('Error submitting job:', error);
        throw error;
    }
};
```

### DLQ Processing

```java
@Component
public class DlqProcessor {
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void processDlq() {
        // Receive messages from DLQ
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
            .queueUrl(dlqUrl)
            .maxNumberOfMessages(10)
            .build();
        
        ReceiveMessageResponse response = sqsClient.receiveMessage(request);
        
        for (Message message : response.messages()) {
            try {
                // Log failed job
                logFailedJob(message);
                
                // Optionally: Attempt reprocessing with manual intervention
                // Or: Send alert to operations team
                
                // Delete from DLQ after logging
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(dlqUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());
                    
            } catch (Exception e) {
                logger.error("Error processing DLQ message", e);
            }
        }
    }
}
```

### Job Model

```java
@Data
public class Job {
    private String id;
    private String type;
    private JobPriority priority;
    private Map<String, Object> data;
    private Instant createdAt;
    private Instant scheduledAt;
    private int retryCount;
    private String errorMessage;
    
    public <T> T getData(Class<T> clazz) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(data, clazz);
    }
}

public enum JobPriority {
    CRITICAL,  // Process immediately
    HIGH,      // Process within 1 minute
    NORMAL,    // Process within 5 minutes
    LOW        // Process within 15 minutes
}

@Data
public class EmailJobData {
    private String to;
    private String subject;
    private String body;
    private List<String> attachments;
}

@Data
public class NotificationJobData {
    private String userId;
    private String message;
    private NotificationType type;
}

@Data
public class ReportJobData {
    private String reportType;
    private Map<String, Object> parameters;
    private String outputFormat;
}
```

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
