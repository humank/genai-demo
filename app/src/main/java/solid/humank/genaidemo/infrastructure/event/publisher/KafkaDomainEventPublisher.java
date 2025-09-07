package solid.humank.genaidemo.infrastructure.event.publisher;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.event.DomainEventPublisher;

/**
 * Kafka domain event publisher for production profile with MSK integration
 * 
 * Features:
 * - Publishes events to Amazon MSK (Managed Streaming for Apache Kafka)
 * - Transactional event publishing with @TransactionalEventListener
 * - Retry mechanisms with exponential backoff
 * - Dead letter queue handling for failed events
 * - Correlation ID and tracing support
 * - Async publishing with callback handling
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6
 */
@Component
@Profile("production")
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaDomainEventPublisher.class);
    
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final DeadLetterService deadLetterService;
    
    // Topic naming convention: genai-demo.{eventType}
    private static final String TOPIC_PREFIX = "genai-demo.";
    private static final String DEAD_LETTER_TOPIC = "genai-demo.dead-letter";

    public KafkaDomainEventPublisher(
            KafkaTemplate<String, DomainEvent> kafkaTemplate,
            DeadLetterService deadLetterService) {
        this.kafkaTemplate = kafkaTemplate;
        this.deadLetterService = deadLetterService;
        logger.info("KafkaDomainEventPublisher initialized for production profile with MSK integration");
    }

    @Override
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void publish(DomainEvent event) {
        if (event == null) {
            logger.warn("Attempted to publish null domain event");
            return;
        }

        String topic = getTopicForEvent(event);
        String key = event.getAggregateId();
        
        // Set correlation ID for tracing
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = event.getEventId().toString();
            MDC.put("correlationId", correlationId);
        }

        logger.info("Publishing domain event to MSK: {} with ID: {} to topic: {}", 
            event.getEventType(), event.getEventId(), topic);

        try {
            // Async publish with callback
            CompletableFuture<SendResult<String, DomainEvent>> future = 
                kafkaTemplate.send(topic, key, event);
                
            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    handlePublishFailure(event, throwable);
                } else {
                    handlePublishSuccess(event, result);
                }
            });
            
        } catch (Exception e) {
            logger.error("Failed to publish event: {} with ID: {}", 
                event.getEventType(), event.getEventId(), e);
            throw new EventPublishingException("Event publishing failed", e);
        }
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            logger.debug("No events to publish");
            return;
        }

        logger.info("Publishing {} domain events to MSK", events.size());
        
        for (DomainEvent event : events) {
            publish(event);
        }
        
        logger.info("All {} domain events submitted for publishing", events.size());
    }

    /**
     * Handle transactional event publishing
     * Events are published after transaction commit to ensure consistency
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransactionalEvent(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        DomainEvent event = wrapper.getSource();
        logger.debug("Processing transactional event after commit: {} with ID: {}", 
            event.getEventType(), event.getEventId());
        
        // Publish to MSK after transaction commit
        publish(event);
    }

    /**
     * Recover method for retry mechanism
     * Called when all retry attempts are exhausted
     */
    @Recover
    public void recover(EventPublishingException ex, DomainEvent event) {
        logger.error("All retry attempts failed for event: {} with ID: {}", 
            event.getEventType(), event.getEventId(), ex);
        
        // Send to dead letter queue
        deadLetterService.sendToDeadLetter(event, ex);
    }

    /**
     * Handle successful event publishing
     */
    private void handlePublishSuccess(DomainEvent event, SendResult<String, DomainEvent> result) {
        logger.info("Event published successfully: {} with ID: {} to partition: {} at offset: {}", 
            event.getEventType(), 
            event.getEventId(),
            result.getRecordMetadata().partition(),
            result.getRecordMetadata().offset());
            
        // Update metrics
        updateSuccessMetrics(event);
    }

    /**
     * Handle failed event publishing
     */
    private void handlePublishFailure(DomainEvent event, Throwable failure) {
        logger.error("Async event publishing failed: {} with ID: {}", 
            event.getEventType(), event.getEventId(), failure);
            
        // Send to dead letter queue
        deadLetterService.sendToDeadLetter(event, failure);
        
        // Update error metrics
        updateErrorMetrics(event, failure);
    }

    /**
     * Get Kafka topic name for the event
     * Convention: genai-demo.{eventType}
     */
    private String getTopicForEvent(DomainEvent event) {
        return TOPIC_PREFIX + event.getEventType().toLowerCase();
    }

    /**
     * Update success metrics for monitoring
     */
    private void updateSuccessMetrics(DomainEvent event) {
        // In production, this would integrate with CloudWatch Metrics
        logger.debug("Success metrics updated for event: {} from aggregate: {}", 
            event.getEventType(), event.getAggregateId());
    }

    /**
     * Update error metrics for monitoring
     */
    private void updateErrorMetrics(DomainEvent event, Throwable error) {
        // In production, this would integrate with CloudWatch Metrics
        logger.debug("Error metrics updated for event: {} from aggregate: {} - Error: {}", 
            event.getEventType(), event.getAggregateId(), error.getMessage());
    }

    /**
     * Custom exception for event publishing failures
     */
    public static class EventPublishingException extends RuntimeException {
        public EventPublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Dead letter service for handling failed events
     */
    @Component
    @Profile("production")
    public static class DeadLetterService {
        
        private static final Logger logger = LoggerFactory.getLogger(DeadLetterService.class);
        
        private final KafkaTemplate<String, Object> deadLetterKafkaTemplate;
        
        public DeadLetterService(KafkaTemplate<String, Object> deadLetterKafkaTemplate) {
            this.deadLetterKafkaTemplate = deadLetterKafkaTemplate;
        }

        /**
         * Send failed event to dead letter queue
         */
        public void sendToDeadLetter(DomainEvent event, Throwable cause) {
            try {
                DeadLetterEvent deadLetterEvent = new DeadLetterEvent(
                    event.getEventId().toString(),
                    event.getEventType(),
                    event.getAggregateId(),
                    event.getClass().getName(),
                    serializeEvent(event),
                    cause.getMessage(),
                    System.currentTimeMillis()
                );
                
                deadLetterKafkaTemplate.send(DEAD_LETTER_TOPIC, event.getAggregateId(), deadLetterEvent);
                
                logger.warn("Event sent to dead letter queue: {} with ID: {} - Reason: {}", 
                    event.getEventType(), event.getEventId(), cause.getMessage());
                    
            } catch (Exception e) {
                logger.error("Failed to send event to dead letter queue: {} with ID: {}", 
                    event.getEventType(), event.getEventId(), e);
            }
        }

        /**
         * Serialize event for dead letter storage
         */
        private String serializeEvent(DomainEvent event) {
            try {
                // In production, use proper JSON serialization
                return event.toString();
            } catch (Exception e) {
                logger.error("Failed to serialize event for dead letter: {}", event.getEventId(), e);
                return "Serialization failed: " + e.getMessage();
            }
        }

        /**
         * Dead letter event record
         */
        public record DeadLetterEvent(
            String originalEventId,
            String originalEventType,
            String aggregateId,
            String eventClassName,
            String eventData,
            String errorMessage,
            long timestamp
        ) {}
    }
}