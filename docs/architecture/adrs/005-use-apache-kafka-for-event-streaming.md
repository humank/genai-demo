---
adr_number: 005
title: "Use Apache Kafka (MSK) for Event Streaming"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [003, 004, 008]
affected_viewpoints: ["deployment", "concurrency", "information"]
affected_perspectives: ["performance", "availability", "scalability"]
---

# ADR-005: Use Apache Kafka (MSK) for Event Streaming

## Status

**Accepted** - 2025-10-24

## Context

### Problem Statement

The Enterprise E-Commerce Platform requires a robust event streaming platform that:

- Handles high-throughput event streaming (10,000+ events/second)
- Provides reliable message delivery with durability guarantees
- Supports event replay for debugging and recovery
- Enables real-time event processing and analytics
- Scales horizontally to handle growing event volumes
- Provides event ordering guarantees within partitions
- Supports multiple consumers with different processing speeds
- Integrates with domain events architecture (ADR-003)
- Enables event-driven microservices communication
- Provides monitoring and operational visibility

### Business Context

**Business Drivers**:

- Need for real-time business event processing
- Requirement for event-driven architecture across bounded contexts
- Support for real-time analytics and reporting
- Enable event sourcing capabilities for audit trails
- Scale to handle Black Friday traffic (100K+ events/second peak)
- Support for future event-driven features (recommendations, fraud detection)

**Constraints**:

- AWS cloud infrastructure (ADR-007)
- Budget: $3,000/month for messaging infrastructure
- Team has limited Kafka experience
- Must integrate with Spring Boot applications
- Need for managed service to reduce operational overhead
- Compliance requirements for event audit trails

### Technical Context

**Current State**:

- Domain events implemented (ADR-003)
- Spring Boot 3.4.5 + Java 21
- Hexagonal Architecture (ADR-002)
- 13 bounded contexts requiring event communication
- Redis for caching (ADR-004)
- PostgreSQL for primary database (ADR-001)

**Requirements**:

- Event throughput: 10,000+ events/second (normal), 100,000+ events/second (peak)
- Event retention: 7 days minimum, 30 days for audit
- Message durability: No data loss
- Latency: < 100ms end-to-end (95th percentile)
- Ordering: Maintain order within partition
- Scalability: Horizontal scaling for producers and consumers
- Monitoring: Real-time metrics and alerting
- Integration: Spring Boot Kafka integration

## Decision Drivers

1. **Throughput**: Handle high event volumes
2. **Durability**: Guarantee no message loss
3. **Scalability**: Horizontal scaling capability
4. **Ordering**: Maintain event order where needed
5. **Replay**: Support event replay for recovery
6. **Managed Service**: Reduce operational overhead
7. **AWS Integration**: Native AWS service integration
8. **Cost**: Stay within budget constraints

## Considered Options

### Option 1: Amazon MSK (Managed Streaming for Apache Kafka)

**Description**: AWS managed Kafka service with full Kafka API compatibility

**Architecture**:

```mermaid
graph LR
    N1["Domain Events"]
    N2["Spring Kafka Producer"]
    N1 --> N2
    N3["MSK Cluster"]
    N2 --> N3
    N4["Spring Kafka Consumer"]
    N3 --> N4
    N5["Event Handlers"]
    N4 --> N5
```

**Pros**:

- ✅ Fully managed Kafka service (AWS handles operations)
- ✅ Full Kafka API compatibility
- ✅ High throughput (millions of messages/second)
- ✅ Durable message storage with replication
- ✅ Event replay capability
- ✅ Horizontal scaling (add brokers)
- ✅ AWS integration (CloudWatch, IAM, VPC)
- ✅ Multi-AZ deployment for high availability
- ✅ Automatic patching and upgrades
- ✅ Strong ordering guarantees within partitions
- ✅ Large ecosystem and community

**Cons**:

- ⚠️ More complex than SQS/SNS
- ⚠️ Requires Kafka knowledge
- ⚠️ Higher cost than SQS
- ⚠️ Need to manage topics and partitions

**Cost**:

- Development: $500/month (kafka.m5.large, 2 brokers)
- Production: $2,500/month (kafka.m5.xlarge, 3 brokers, Multi-AZ)
- Storage: $100/month (1TB retention)
- Total: ~$2,600/month

**Risk**: **Low** - Mature technology, AWS managed

### Option 2: Amazon SQS + SNS

**Description**: AWS managed message queue and pub/sub service

**Pros**:

- ✅ Fully managed (no operations)
- ✅ Simple to use
- ✅ Cost-effective
- ✅ Automatic scaling
- ✅ AWS native integration

**Cons**:

- ❌ No event replay capability
- ❌ Limited ordering guarantees (FIFO queues have throughput limits)
- ❌ No event streaming semantics
- ❌ Not suitable for event sourcing
- ❌ Limited retention (14 days max)
- ❌ No partition-based scaling
- ❌ Higher latency than Kafka

**Cost**: $800/month (estimated)

**Risk**: **Medium** - Limited capabilities for event streaming

### Option 3: Amazon Kinesis Data Streams

**Description**: AWS managed real-time data streaming service

**Pros**:

- ✅ Fully managed
- ✅ Real-time streaming
- ✅ AWS native integration
- ✅ Automatic scaling (on-demand mode)
- ✅ Event replay capability

**Cons**:

- ❌ Proprietary API (not Kafka compatible)
- ❌ More expensive than MSK at scale
- ❌ Limited retention (365 days max)
- ❌ Smaller ecosystem than Kafka
- ❌ Shard management complexity
- ❌ Less flexible than Kafka

**Cost**: $3,500/month (estimated for similar throughput)

**Risk**: **Medium** - Vendor lock-in, higher cost

### Option 4: Self-Managed Kafka on EC2

**Description**: Run Kafka cluster on EC2 instances

**Pros**:

- ✅ Full control over configuration
- ✅ Potentially lower cost
- ✅ Full Kafka features

**Cons**:

- ❌ High operational overhead
- ❌ Need Kafka expertise
- ❌ Manual scaling and patching
- ❌ Complex monitoring setup
- ❌ High maintenance burden
- ❌ Team lacks Kafka operations experience

**Cost**: $1,500/month (infrastructure) + operational costs

**Risk**: **High** - Operational complexity, team expertise

### Option 5: RabbitMQ

**Description**: Open-source message broker

**Pros**:

- ✅ Mature message broker
- ✅ Good Spring integration
- ✅ Flexible routing

**Cons**:

- ❌ Not designed for event streaming
- ❌ Limited event replay
- ❌ Lower throughput than Kafka
- ❌ Not suitable for event sourcing
- ❌ Need to self-manage or use Amazon MQ

**Cost**: $1,000/month (Amazon MQ)

**Risk**: **Medium** - Not optimal for event streaming

## Decision Outcome

**Chosen Option**: **Amazon MSK (Managed Streaming for Apache Kafka)**

### Rationale

Amazon MSK was selected for the following reasons:

1. **Event Streaming Semantics**: Kafka is purpose-built for event streaming with replay, ordering, and durability
2. **High Throughput**: Handles millions of events/second, well beyond our requirements
3. **Event Replay**: Critical for debugging, recovery, and event sourcing
4. **Managed Service**: AWS handles operations, patching, and scaling
5. **Kafka Ecosystem**: Large ecosystem of tools, libraries, and community support
6. **Spring Boot Integration**: Excellent Spring Kafka integration
7. **Cost-Effective**: Within budget while providing enterprise features
8. **Ordering Guarantees**: Partition-based ordering for event sequences
9. **Durability**: Replication and persistence guarantee no data loss
10. **Scalability**: Horizontal scaling by adding brokers and partitions

**Implementation Strategy**:

**MSK Cluster Configuration**:

```text
Production Cluster:

- Broker Type: kafka.m5.xlarge (3 brokers)
- Multi-AZ: 3 availability zones
- Storage: 1TB per broker (EBS gp3)
- Replication Factor: 3
- Min In-Sync Replicas: 2
- Retention: 7 days (configurable per topic)

```

**Topic Design**:

```text
Topics by Bounded Context:

- customer-events (partitions: 10)
- order-events (partitions: 20)
- product-events (partitions: 10)
- payment-events (partitions: 15)
- inventory-events (partitions: 10)
- notification-events (partitions: 5)

```

**Producer Configuration**:

```java
@Configuration
public class KafkaProducerConfiguration {
    
    @Bean
    public ProducerFactory<String, DomainEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, mskBootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Durability settings
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        
        // Performance settings
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, DomainEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

**Consumer Configuration**:

```java
@Configuration
public class KafkaConsumerConfiguration {
    
    @Bean
    public ConsumerFactory<String, DomainEvent> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, mskBootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // Consumer group settings
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "order-service-group");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        // Performance settings
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
        
        return new DefaultKafkaConsumerFactory<>(config);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DomainEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DomainEvent> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(AckMode.MANUAL);
        return factory;
    }
}
```

**Why Not SQS/SNS**: Lacks event replay, streaming semantics, and event sourcing capabilities needed for our architecture.

**Why Not Kinesis**: More expensive at scale, proprietary API creates vendor lock-in, smaller ecosystem.

**Why Not Self-Managed**: Team lacks Kafka operations expertise, high operational overhead not justified.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | Need to learn Kafka concepts | Training, documentation, examples |
| Operations Team | Medium | Monitor Kafka metrics | CloudWatch dashboards, runbooks |
| Architects | Positive | Event-driven architecture enabled | Architecture guidelines |
| Business | Positive | Real-time event processing | Business metrics dashboards |
| DevOps Team | Medium | Deploy and manage MSK | AWS CDK automation, monitoring |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:

- All bounded contexts (event publishing and consuming)
- Event-driven architecture implementation
- Infrastructure deployment (ADR-007)
- Monitoring and observability (ADR-008)
- Application services (event publishing)
- Event handlers (event consuming)

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Kafka learning curve | High | Medium | Training, examples, pair programming |
| Message ordering issues | Medium | High | Proper partition key design, testing |
| Consumer lag | Medium | High | Monitoring, auto-scaling consumers |
| Topic design mistakes | Medium | Medium | Architecture reviews, best practices |
| Cost overruns | Low | Medium | Monitor usage, optimize retention |
| Data loss | Low | Critical | Proper configuration (acks=all, replication) |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: MSK Cluster Setup (Week 1-2)

- [ ] Provision MSK cluster using AWS CDK

  ```typescript
  import * as msk from 'aws-cdk-lib/aws-msk';
  
  export class MessagingStack extends Stack {
    public readonly cluster: msk.CfnCluster;
    
    constructor(scope: Construct, id: string, props: MessagingStackProps) {
      super(scope, id, props);
      
      this.cluster = new msk.CfnCluster(this, 'MSKCluster', {
        clusterName: 'ecommerce-events',
        kafkaVersion: '3.5.1',
        numberOfBrokerNodes: 3,
        brokerNodeGroupInfo: {
          instanceType: 'kafka.m5.xlarge',
          clientSubnets: props.vpc.privateSubnets.map(s => s.subnetId),
          securityGroups: [props.securityGroup.securityGroupId],
          storageInfo: {
            ebsStorageInfo: {
              volumeSize: 1000,
              provisionedThroughput: {
                enabled: true,
                volumeThroughput: 250,
              },
            },
          },
        },
        encryptionInfo: {
          encryptionInTransit: {
            clientBroker: 'TLS',
            inCluster: true,
          },
          encryptionAtRest: {
            dataVolumeKmsKeyId: props.kmsKey.keyId,
          },
        },
        enhancedMonitoring: 'PER_TOPIC_PER_PARTITION',
        loggingInfo: {
          brokerLogs: {
            cloudWatchLogs: {
              enabled: true,
              logGroup: '/aws/msk/ecommerce-events',
            },
          },
        },
      });
    }
  }
  ```

- [ ] Configure security groups
- [ ] Set up VPC endpoints
- [ ] Enable CloudWatch monitoring
- [ ] Create initial topics

### Phase 2: Spring Kafka Integration (Week 2-3)

- [ ] Add Spring Kafka dependencies

  ```xml
  <dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
  </dependency>
  ```

- [ ] Configure application properties

  ```yaml
  spring:
    kafka:
      bootstrap-servers: ${MSK_BOOTSTRAP_SERVERS}
      producer:
        key-serializer: org.apache.kafka.common.serialization.StringSerializer
        value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
        acks: all
        retries: 3
        properties:
          max.in.flight.requests.per.connection: 1
          compression.type: snappy
      consumer:
        key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
        value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
        group-id: ${spring.application.name}
        auto-offset-reset: earliest
        enable-auto-commit: false
        properties:
          spring.json.trusted.packages: solid.humank.genaidemo.domain
  ```

- [ ] Create Kafka event publisher

  ```java
  @Component
  public class KafkaEventPublisher {
      
      private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
      private final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);
      
      public KafkaEventPublisher(KafkaTemplate<String, DomainEvent> kafkaTemplate) {
          this.kafkaTemplate = kafkaTemplate;
      }
      
      public void publish(DomainEvent event) {
          String topic = determineTopicForEvent(event);
          String key = event.getAggregateId();
          
          ListenableFuture<SendResult<String, DomainEvent>> future = 
              kafkaTemplate.send(topic, key, event);
          
          future.addCallback(
              result -> logger.info("Published event {} to topic {}", 
                  event.getEventType(), topic),
              ex -> logger.error("Failed to publish event {} to topic {}", 
                  event.getEventType(), topic, ex)
          );
      }
      
      private String determineTopicForEvent(DomainEvent event) {
          String eventType = event.getEventType();
          
          // Route events to appropriate topics based on bounded context
          if (eventType.startsWith("Customer")) {
              return "customer-events";
          } else if (eventType.startsWith("Order")) {
              return "order-events";
          } else if (eventType.startsWith("Product")) {
              return "product-events";
          } else if (eventType.startsWith("Payment")) {
              return "payment-events";
          } else {
              return "general-events";
          }
      }
  }
  ```

### Phase 3: Event Consumers (Week 3-4)

- [ ] Create Kafka event listeners

  ```java
  @Component
  public class OrderEventConsumer {
      
      private final Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);
      private final InventoryService inventoryService;
      private final NotificationService notificationService;
      
      @KafkaListener(
          topics = "order-events",
          groupId = "inventory-service-group",
          containerFactory = "kafkaListenerContainerFactory"
      )
      public void handleOrderEvent(
          @Payload DomainEvent event,
          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
          @Header(KafkaHeaders.OFFSET) long offset,
          Acknowledgment acknowledgment
      ) {
          try {
              logger.info("Received event {} from partition {} at offset {}", 
                  event.getEventType(), partition, offset);
              
              processEvent(event);
              
              // Manual commit after successful processing
              acknowledgment.acknowledge();
              
          } catch (Exception e) {
              logger.error("Failed to process event {}", event.getEventType(), e);
              // Don't acknowledge - will retry
              throw e;
          }
      }
      
      private void processEvent(DomainEvent event) {
          switch (event) {
              case OrderSubmittedEvent e -> handleOrderSubmitted(e);
              case OrderCancelledEvent e -> handleOrderCancelled(e);
              default -> logger.warn("Unknown event type: {}", event.getEventType());
          }
      }
      
      private void handleOrderSubmitted(OrderSubmittedEvent event) {
          // Reserve inventory
          inventoryService.reserveItems(event.orderId(), event.items());
          
          // Send notification
          notificationService.sendOrderConfirmation(event.customerId(), event.orderId());
      }
      
      private void handleOrderCancelled(OrderCancelledEvent event) {
          // Release inventory
          inventoryService.releaseReservation(event.orderId());
      }
  }
  ```

- [ ] Implement error handling and retry

  ```java
  @Configuration
  public class KafkaErrorHandlingConfiguration {
      
      @Bean
      public DefaultErrorHandler errorHandler() {
          // Exponential backoff: 1s, 2s, 4s
          BackOff backOff = new ExponentialBackOff(1000, 2.0);
          
          DefaultErrorHandler errorHandler = new DefaultErrorHandler(
              (record, exception) -> {
                  // Send to dead letter topic
                  logger.error("Failed to process record after retries: {}", record, exception);
              },
              backOff
          );
          
          // Don't retry for certain exceptions
          errorHandler.addNotRetryableExceptions(
              DeserializationException.class,
              MessageConversionException.class
          );
          
          return errorHandler;
      }
  }
  ```

- [ ] Set up dead letter topics

  ```java
  @Component
  public class DeadLetterPublisher {
      
      private final KafkaTemplate<String, FailedEvent> kafkaTemplate;
      
      public void sendToDeadLetter(ConsumerRecord<String, DomainEvent> record, Exception exception) {
          FailedEvent failedEvent = new FailedEvent(
              record.topic(),
              record.partition(),
              record.offset(),
              record.key(),
              record.value(),
              exception.getMessage(),
              Instant.now()
          );
          
          kafkaTemplate.send("dead-letter-topic", failedEvent);
      }
  }
  ```

### Phase 4: Monitoring and Alerting (Week 4-5)

- [ ] Configure CloudWatch metrics

  ```typescript
  // MSK metrics to monitor
  const metrics = [
    'BytesInPerSec',
    'BytesOutPerSec',
    'MessagesInPerSec',
    'FetchConsumerTotalTimeMs',
    'ProduceLocalTimeMs',
    'UnderReplicatedPartitions',
    'OfflinePartitionsCount',
  ];
  
  metrics.forEach(metricName => {
    new cloudwatch.Alarm(this, `MSK-${metricName}`, {
      metric: new cloudwatch.Metric({
        namespace: 'AWS/Kafka',
        metricName: metricName,
        dimensionsMap: {
          'Cluster Name': cluster.clusterName,
        },
      }),
      threshold: getThresholdForMetric(metricName),
      evaluationPeriods: 2,
    });
  });
  ```

- [ ] Create Grafana dashboards
  - Kafka cluster health
  - Topic throughput and lag
  - Consumer group lag
  - Producer performance
  - Error rates

- [ ] Set up alerts
  - Consumer lag > 10,000 messages
  - Under-replicated partitions > 0
  - Offline partitions > 0
  - High producer latency > 100ms
  - High consumer latency > 500ms

### Phase 5: Testing (Week 5-6)

- [ ] Set up embedded Kafka for tests

  ```java
  @SpringBootTest
  @EmbeddedKafka(
      partitions = 1,
      topics = {"customer-events", "order-events"},
      brokerProperties = {
          "listeners=PLAINTEXT://localhost:9092",
          "port=9092"
      }
  )
  class KafkaIntegrationTest {
      
      @Autowired
      private KafkaTemplate<String, DomainEvent> kafkaTemplate;
      
      @Autowired
      private EmbeddedKafkaBroker embeddedKafka;
      
      @Test
      void should_publish_and_consume_customer_created_event() throws Exception {
          // Given
          CustomerCreatedEvent event = CustomerCreatedEvent.create(
              CustomerId.of("CUST-001"),
              new CustomerName("John Doe"),
              new Email("john@example.com"),
              MembershipLevel.STANDARD
          );
          
          CountDownLatch latch = new CountDownLatch(1);
          AtomicReference<DomainEvent> receivedEvent = new AtomicReference<>();
          
          // Set up consumer
          Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
              "test-group", "true", embeddedKafka);
          ConsumerFactory<String, DomainEvent> cf = 
              new DefaultKafkaConsumerFactory<>(consumerProps);
          Consumer<String, DomainEvent> consumer = cf.createConsumer();
          consumer.subscribe(Collections.singletonList("customer-events"));
          
          // When
          kafkaTemplate.send("customer-events", event.getAggregateId(), event);
          
          // Then
          ConsumerRecords<String, DomainEvent> records = 
              consumer.poll(Duration.ofSeconds(10));
          
          assertThat(records.count()).isEqualTo(1);
          ConsumerRecord<String, DomainEvent> record = records.iterator().next();
          assertThat(record.value()).isInstanceOf(CustomerCreatedEvent.class);
          assertThat(record.key()).isEqualTo("CUST-001");
      }
  }
  ```

- [ ] Test event ordering

  ```java
  @Test
  void should_maintain_event_order_within_partition() {
      // Given
      String aggregateId = "ORDER-001";
      List<DomainEvent> events = List.of(
          OrderCreatedEvent.create(OrderId.of(aggregateId)),
          OrderItemAddedEvent.create(OrderId.of(aggregateId)),
          OrderSubmittedEvent.create(OrderId.of(aggregateId))
      );
      
      // When - publish all events with same key (same partition)
      events.forEach(event -> 
          kafkaTemplate.send("order-events", aggregateId, event)
      );
      
      // Then - verify order is maintained
      List<DomainEvent> receivedEvents = consumeEvents("order-events", 3);
      assertThat(receivedEvents).containsExactlyElementsOf(events);
  }
  ```

- [ ] Test error handling and retry
- [ ] Test dead letter queue
- [ ] Load testing with high event volumes

### Phase 6: Production Deployment (Week 6-7)

- [ ] Deploy MSK cluster to production
- [ ] Create production topics with proper configuration

  ```bash
  # Create topics with replication and retention
  kafka-topics.sh --create \
    --bootstrap-server $MSK_BOOTSTRAP \
    --topic customer-events \
    --partitions 10 \
    --replication-factor 3 \
    --config retention.ms=604800000 \
    --config min.insync.replicas=2
  ```

- [ ] Configure monitoring and alerting
- [ ] Deploy event publishers
- [ ] Deploy event consumers
- [ ] Verify end-to-end event flow
- [ ] Monitor for 48 hours

### Phase 7: Documentation and Training (Week 7-8)

- [ ] Create Kafka operations runbook
  - Topic management procedures
  - Consumer group management
  - Troubleshooting guide
  - Performance tuning guide

- [ ] Conduct team training
  - Kafka concepts and architecture
  - Producer best practices
  - Consumer best practices
  - Monitoring and troubleshooting

- [ ] Document event schemas
  - Create schema registry (future)
  - Document event formats
  - Version management strategy

### Rollback Strategy

**Trigger Conditions**:

- Message loss > 0.01%
- Consumer lag consistently > 1 hour
- Cost exceeds $4,000/month
- Team unable to manage Kafka
- Performance issues affecting SLA

**Rollback Steps**:

1. Switch to SQS/SNS for critical events
2. Keep Kafka for non-critical events
3. Simplify event architecture
4. Provide additional training
5. Re-evaluate after addressing issues

**Rollback Time**: 1 week for critical paths, 4 weeks for complete rollback
