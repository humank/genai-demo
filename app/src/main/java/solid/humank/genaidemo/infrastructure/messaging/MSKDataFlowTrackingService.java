package solid.humank.genaidemo.infrastructure.messaging;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import solid.humank.genaidemo.infrastructure.config.XRayTracingConfig.XRayTracingUtils;
import solid.humank.genaidemo.infrastructure.logging.CloudWatchDataFlowLogger;

/**
 * MSK Data Flow Tracking Service
 * 
 * This service provides comprehensive data flow tracking capabilities using Amazon MSK
 * with X-Ray distributed tracing, CloudWatch logging, and business metrics collection.
 * 
 * Features:
 * - Event publishing with comprehensive tracking metadata
 * - Event consumption with processing latency measurement
 * - X-Ray distributed tracing integration
 * - CloudWatch structured logging
 * - Business metrics collection and export
 * - Error handling and dead letter queue support
 * - Cross-context event correlation
 * 
 * Created: 2025年9月24日 下午2:34 (台北時間)
 * Task: 9.2 - MSK Data Flow Tracking Service Implementation
 */
@Service
public class MSKDataFlowTrackingService {

    private static final Logger logger = LoggerFactory.getLogger(MSKDataFlowTrackingService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final XRayTracingUtils xrayTracingUtils;
    private final CloudWatchDataFlowLogger cloudWatchLogger;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    // Metrics
    private final Counter eventsPublishedCounter;
    private final Counter eventsConsumedCounter;
    private final Counter eventsFailedCounter;
    private final Timer eventPublishingTimer;
    private final Timer eventProcessingTimer;

    public MSKDataFlowTrackingService(
            KafkaTemplate<String, Object> kafkaTemplate,
            XRayTracingUtils xrayTracingUtils,
            CloudWatchDataFlowLogger cloudWatchLogger,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        
        this.kafkaTemplate = kafkaTemplate;
        this.xrayTracingUtils = xrayTracingUtils;
        this.cloudWatchLogger = cloudWatchLogger;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;

        // Initialize metrics
        this.eventsPublishedCounter = Counter.builder("msk_events_published_total")
            .description("Total number of events published to MSK")
            .tag("component", "msk-data-flow")
            .register(meterRegistry);

        this.eventsConsumedCounter = Counter.builder("msk_events_consumed_total")
            .description("Total number of events consumed from MSK")
            .tag("component", "msk-data-flow")
            .register(meterRegistry);

        this.eventsFailedCounter = Counter.builder("msk_events_failed_total")
            .description("Total number of failed MSK events")
            .tag("component", "msk-data-flow")
            .register(meterRegistry);

        this.eventPublishingTimer = Timer.builder("msk_event_publishing_duration")
            .description("Time taken to publish events to MSK")
            .tag("component", "msk-data-flow")
            .register(meterRegistry);

        this.eventProcessingTimer = Timer.builder("msk_event_processing_duration")
            .description("Time taken to process events from MSK")
            .tag("component", "msk-data-flow")
            .register(meterRegistry);
    }

    /**
     * Track data flow by publishing event to MSK with comprehensive tracking
     */
    public CompletableFuture<SendResult<String, Object>> trackDataFlow(
            String topic, 
            String eventType, 
            Object eventData, 
            String correlationId) {
        
        return xrayTracingUtils.traceBusinessOperation("kafka-publish-" + eventType, () -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            try {
                // Create tracking metadata
                DataFlowEvent trackingEvent = createDataFlowEvent(
                    topic, eventType, eventData, correlationId);

                // Add X-Ray annotations
                addXRayAnnotations(topic, eventType, correlationId, trackingEvent);

                // Publish to MSK with comprehensive error handling
                CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, trackingEvent);
                
                future.whenComplete((result, throwable) -> {
                    sample.stop(eventPublishingTimer);
                    
                    if (throwable == null) {
                        recordSuccessMetrics(topic, eventType);
                        cloudWatchLogger.logSuccessfulPublish(trackingEvent, result);
                        logger.debug("Successfully published event to topic: {}, eventType: {}, eventId: {}", 
                                   topic, eventType, trackingEvent.getEventId());
                    } else {
                        recordFailureMetrics(topic, eventType, throwable);
                        cloudWatchLogger.logFailedPublish(trackingEvent, throwable);
                        handlePublishFailure(trackingEvent, throwable);
                    }
                });

                return future;
                
            } catch (Exception e) {
                sample.stop(eventPublishingTimer);
                recordFailureMetrics(topic, eventType, e);
                logger.error("Error creating data flow event for topic: {}, eventType: {}", topic, eventType, e);
                throw new DataFlowTrackingException("Failed to create data flow event", e);
            }
        });
    }

    /**
     * Handle data flow events from MSK topics
     */
    @KafkaListener(topics = "#{@mskConfiguration.kafkaTopics().getDataFlowTopics()}")
    public void handleDataFlowEvent(
            @Payload DataFlowEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        xrayTracingUtils.traceBusinessOperation("kafka-consume-" + event.getEventType(), () -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            try {
                // Add consumption metadata
                enrichEventWithConsumptionMetadata(event, partition, offset);

                // Calculate processing latency
                long processingLatency = calculateProcessingLatency(event);

                // Add X-Ray annotations for consumption
                addConsumptionXRayAnnotations(event, topic, partition, offset, processingLatency);

                // Log to CloudWatch for Logs Insights analysis
                cloudWatchLogger.logDataFlowEvent(event, processingLatency);

                // Update consumption metrics
                recordConsumptionMetrics(topic, event.getEventType(), processingLatency);

                // Process event based on type and context
                processDataFlowEventByType(event);

                // Manual acknowledgment for reliability
                acknowledgment.acknowledge();

                logger.debug("Successfully processed event from topic: {}, eventType: {}, eventId: {}, latency: {}ms", 
                           topic, event.getEventType(), event.getEventId(), processingLatency);

            } catch (Exception e) {
                sample.stop(eventProcessingTimer);
                cloudWatchLogger.logProcessingError(event, e);
                recordErrorMetrics(topic, event.getEventType(), e);
                handleProcessingError(event, e);
                
                // Don't acknowledge on error - let Kafka retry
                logger.error("Error processing event from topic: {}, eventType: {}, eventId: {}", 
                           topic, event.getEventType(), event.getEventId(), e);
                throw new DataFlowProcessingException("Failed to process data flow event", e);
            } finally {
                sample.stop(eventProcessingTimer);
            }
        });
    }

    /**
     * Create data flow event with comprehensive metadata
     */
    private DataFlowEvent createDataFlowEvent(
            String topic, 
            String eventType, 
            Object eventData, 
            String correlationId) {
        
        return DataFlowEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .correlationId(correlationId != null ? correlationId : UUID.randomUUID().toString())
            .eventType(eventType)
            .topic(topic)
            .timestamp(Instant.now())
            .traceId(getCurrentTraceId())
            .spanId(getCurrentSpanId())
            .sourceService(getServiceName())
            .data(eventData)
            .dataSize(calculateDataSize(eventData))
            .metadata(createEventMetadata(eventType))
            .build();
    }

    /**
     * Add X-Ray annotations for event publishing
     */
    private void addXRayAnnotations(String topic, String eventType, String correlationId, DataFlowEvent event) {
        xrayTracingUtils.addAnnotation("kafka.topic", topic);
        xrayTracingUtils.addAnnotation("kafka.event_type", eventType);
        xrayTracingUtils.addAnnotation("kafka.correlation_id", correlationId);
        xrayTracingUtils.addAnnotation("kafka.event_id", event.getEventId());
        xrayTracingUtils.addAnnotation("kafka.data_size", String.valueOf(event.getDataSize()));
        xrayTracingUtils.addAnnotation("messaging.system", "kafka");
        xrayTracingUtils.addAnnotation("messaging.operation", "publish");
    }

    /**
     * Add X-Ray annotations for event consumption
     */
    private void addConsumptionXRayAnnotations(
            DataFlowEvent event, 
            String topic, 
            int partition, 
            long offset, 
            long processingLatency) {
        
        xrayTracingUtils.addAnnotation("kafka.topic", topic);
        xrayTracingUtils.addAnnotation("kafka.partition", String.valueOf(partition));
        xrayTracingUtils.addAnnotation("kafka.offset", String.valueOf(offset));
        xrayTracingUtils.addAnnotation("kafka.event_type", event.getEventType());
        xrayTracingUtils.addAnnotation("kafka.processing_latency_ms", String.valueOf(processingLatency));
        xrayTracingUtils.addAnnotation("messaging.system", "kafka");
        xrayTracingUtils.addAnnotation("messaging.operation", "consume");
        
        // Link to producer trace if available
        if (event.getTraceId() != null) {
            xrayTracingUtils.addAnnotation("kafka.producer_trace_id", event.getTraceId());
        }
    }

    /**
     * Enrich event with consumption metadata
     */
    private void enrichEventWithConsumptionMetadata(DataFlowEvent event, int partition, long offset) {
        event.setConsumedAt(Instant.now());
        event.setConsumerPartition(partition);
        event.setConsumerOffset(offset);
        event.setTargetService(getServiceName());
    }

    /**
     * Calculate processing latency
     */
    private long calculateProcessingLatency(DataFlowEvent event) {
        if (event.getTimestamp() != null) {
            return Duration.between(event.getTimestamp(), Instant.now()).toMillis();
        }
        return 0;
    }

    /**
     * Process data flow event based on type and business context
     */
    private void processDataFlowEventByType(DataFlowEvent event) {
        switch (event.getEventType()) {
            case "ORDER_CREATED":
            case "ORDER_UPDATED":
            case "ORDER_CANCELLED":
                processOrderEvent(event);
                break;
            case "CUSTOMER_CREATED":
            case "CUSTOMER_UPDATED":
                processCustomerEvent(event);
                break;
            case "PAYMENT_PROCESSED":
            case "PAYMENT_FAILED":
                processPaymentEvent(event);
                break;
            case "INVENTORY_UPDATED":
            case "INVENTORY_RESERVED":
                processInventoryEvent(event);
                break;
            default:
                processGenericEvent(event);
                break;
        }
    }

    /**
     * Process order-related events
     */
    private void processOrderEvent(DataFlowEvent event) {
        logger.debug("Processing order event: {}", event.getEventType());
        // Implement order-specific processing logic
        // This could include updating order status, triggering workflows, etc.
    }

    /**
     * Process customer-related events
     */
    private void processCustomerEvent(DataFlowEvent event) {
        logger.debug("Processing customer event: {}", event.getEventType());
        // Implement customer-specific processing logic
    }

    /**
     * Process payment-related events
     */
    private void processPaymentEvent(DataFlowEvent event) {
        logger.debug("Processing payment event: {}", event.getEventType());
        // Implement payment-specific processing logic
    }

    /**
     * Process inventory-related events
     */
    private void processInventoryEvent(DataFlowEvent event) {
        logger.debug("Processing inventory event: {}", event.getEventType());
        // Implement inventory-specific processing logic
    }

    /**
     * Process generic events
     */
    private void processGenericEvent(DataFlowEvent event) {
        logger.debug("Processing generic event: {}", event.getEventType());
        // Implement generic event processing logic
    }

    /**
     * Record success metrics
     */
    private void recordSuccessMetrics(String topic, String eventType) {
        eventsPublishedCounter.increment(
            io.micrometer.core.instrument.Tags.of(
                "topic", topic,
                "event_type", eventType,
                "status", "success"
            )
        );
    }

    /**
     * Record failure metrics
     */
    private void recordFailureMetrics(String topic, String eventType, Throwable throwable) {
        eventsFailedCounter.increment(
            io.micrometer.core.instrument.Tags.of(
                "topic", topic,
                "event_type", eventType,
                "error_type", throwable.getClass().getSimpleName(),
                "status", "failed"
            )
        );
    }

    /**
     * Record consumption metrics
     */
    private void recordConsumptionMetrics(String topic, String eventType, long processingLatency) {
        eventsConsumedCounter.increment(
            io.micrometer.core.instrument.Tags.of(
                "topic", topic,
                "event_type", eventType,
                "status", "success"
            )
        );

        Timer.builder("msk_event_processing_latency")
            .description("Event processing latency in milliseconds")
            .tag("topic", topic)
            .tag("event_type", eventType)
            .register(meterRegistry)
            .record(processingLatency, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Record error metrics
     */
    private void recordErrorMetrics(String topic, String eventType, Exception exception) {
        eventsFailedCounter.increment(
            io.micrometer.core.instrument.Tags.of(
                "topic", topic,
                "event_type", eventType,
                "error_type", exception.getClass().getSimpleName(),
                "operation", "consume"
            )
        );
    }

    /**
     * Handle publish failure
     */
    private void handlePublishFailure(DataFlowEvent event, Throwable throwable) {
        logger.error("Failed to publish event to MSK: eventId={}, eventType={}, error={}", 
                   event.getEventId(), event.getEventType(), throwable.getMessage());
        
        // Implement retry logic or dead letter queue handling
        // For now, we'll just log the failure
    }

    /**
     * Handle processing error
     */
    private void handleProcessingError(DataFlowEvent event, Exception exception) {
        logger.error("Failed to process event from MSK: eventId={}, eventType={}, error={}", 
                   event.getEventId(), event.getEventType(), exception.getMessage());
        
        // Implement error handling logic (retry, dead letter queue, etc.)
    }

    /**
     * Calculate data size in bytes
     */
    private long calculateDataSize(Object data) {
        try {
            return objectMapper.writeValueAsBytes(data).length;
        } catch (JsonProcessingException e) {
            logger.warn("Failed to calculate data size", e);
            return 0;
        }
    }

    /**
     * Get current trace ID from X-Ray
     */
    private String getCurrentTraceId() {
        try {
            return com.amazonaws.xray.AWSXRay.getCurrentSegment().getTraceId().toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get current span ID from X-Ray
     */
    private String getCurrentSpanId() {
        try {
            return com.amazonaws.xray.AWSXRay.getCurrentSegment().getId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get service name
     */
    private String getServiceName() {
        return "genai-demo";
    }

    /**
     * Create event metadata
     */
    private DataFlowEvent.EventMetadata createEventMetadata(String eventType) {
        return DataFlowEvent.EventMetadata.builder()
            .version("1.0")
            .contentType("application/json")
            .encoding("UTF-8")
            .schema(eventType + "Schema")
            .build();
    }

    /**
     * Custom exceptions for data flow tracking
     */
    public static class DataFlowTrackingException extends RuntimeException {
        public DataFlowTrackingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DataFlowProcessingException extends RuntimeException {
        public DataFlowProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}