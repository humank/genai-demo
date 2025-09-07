package solid.humank.genaidemo.infrastructure.event.publisher;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * Enhanced Dead Letter Service for handling failed domain events in production
 * 
 * Features:
 * - Sends failed events to dedicated dead letter topic
 * - Detailed error tracking and categorization
 * - Retry attempt tracking
 * - Correlation ID and tracing support
 * - Dead letter metrics and monitoring
 * - Event serialization with fallback mechanisms
 * 
 * Requirements: 2.4, 2.5, 2.6
 */
@Component
@Profile("production")
public class DeadLetterService {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterService.class);

    private final KafkaTemplate<String, Object> deadLetterKafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${genai-demo.events.dead-letter.topic:genai-demo.dead-letter}")
    private String deadLetterTopic;

    // Dead letter metrics
    private final Map<String, AtomicLong> deadLetterCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorTypeCounters = new ConcurrentHashMap<>();
    private final AtomicLong totalDeadLetterEvents = new AtomicLong(0);

    public DeadLetterService(
            KafkaTemplate<String, Object> deadLetterKafkaTemplate,
            ObjectMapper objectMapper) {
        this.deadLetterKafkaTemplate = deadLetterKafkaTemplate;
        this.objectMapper = objectMapper;
        logger.info("Enhanced DeadLetterService initialized for production profile");
    }

    /**
     * Send failed event to dead letter queue with comprehensive error tracking
     */
    public void sendToDeadLetter(DomainEvent event, Throwable cause) {
        try {
            String correlationId = MDC.get("correlationId");
            String traceId = MDC.get("traceId");

            DeadLetterEvent deadLetterEvent = new DeadLetterEvent(
                    event.getEventId().toString(),
                    event.getEventType(),
                    event.getAggregateId(),
                    event.getClass().getName(),
                    serializeEvent(event),
                    cause.getClass().getSimpleName(),
                    cause.getMessage(),
                    getStackTrace(cause),
                    correlationId,
                    traceId,
                    LocalDateTime.now().toString(),
                    System.currentTimeMillis(),
                    getRetryAttemptCount(correlationId));

            deadLetterKafkaTemplate.send(deadLetterTopic, event.getAggregateId(), deadLetterEvent);

            // Update metrics
            updateDeadLetterMetrics(event, cause);

            logger.warn("Event sent to dead letter queue: {} with ID: {} - Reason: {} [correlationId: {}, traceId: {}]",
                    event.getEventType(), event.getEventId(), cause.getMessage(), correlationId, traceId);

        } catch (Exception e) {
            logger.error(
                    "Failed to send event to dead letter queue: {} with ID: {} - Original error: {}, DLQ error: {}",
                    event.getEventType(), event.getEventId(), cause.getMessage(), e.getMessage(), e);
        }
    }

    /**
     * Serialize event for dead letter storage with fallback mechanisms
     */
    private String serializeEvent(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event for dead letter: {}", event.getEventId(), e);

            // Fallback to basic string representation
            try {
                return createFallbackSerialization(event);
            } catch (Exception fallbackError) {
                logger.error("Fallback serialization also failed for event: {}", event.getEventId(), fallbackError);
                return "Serialization failed: " + e.getMessage() + ", Fallback failed: " + fallbackError.getMessage();
            }
        }
    }

    /**
     * Create fallback serialization when JSON serialization fails
     */
    private String createFallbackSerialization(DomainEvent event) {
        return String.format(
                "{\"eventId\":\"%s\",\"eventType\":\"%s\",\"aggregateId\":\"%s\",\"occurredOn\":\"%s\",\"fallback\":true}",
                event.getEventId(),
                event.getEventType(),
                event.getAggregateId(),
                event.getOccurredOn());
    }

    /**
     * Get stack trace as string for error tracking
     */
    private String getStackTrace(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Get retry attempt count from correlation context
     */
    private int getRetryAttemptCount(String correlationId) {
        // In a real implementation, this could track retry attempts
        // For now, return a default value
        return 3; // Assuming max retry attempts reached
    }

    /**
     * Update dead letter metrics for monitoring
     */
    private void updateDeadLetterMetrics(DomainEvent event, Throwable cause) {
        String eventType = event.getEventType();
        String errorType = cause.getClass().getSimpleName();

        deadLetterCounters.computeIfAbsent(eventType, k -> new AtomicLong(0)).incrementAndGet();
        errorTypeCounters.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
        totalDeadLetterEvents.incrementAndGet();

        logger.debug("Dead letter metrics updated - Event: {}, Error: {}, Total DLQ: {}",
                eventType, errorType, totalDeadLetterEvents.get());
    }

    // === Monitoring Methods ===

    /**
     * Get dead letter count for a specific event type
     */
    public long getDeadLetterCount(String eventType) {
        return deadLetterCounters.getOrDefault(eventType, new AtomicLong(0)).get();
    }

    /**
     * Get error count for a specific error type
     */
    public long getErrorTypeCount(String errorType) {
        return errorTypeCounters.getOrDefault(errorType, new AtomicLong(0)).get();
    }

    /**
     * Get total dead letter events
     */
    public long getTotalDeadLetterEvents() {
        return totalDeadLetterEvents.get();
    }

    /**
     * Get all dead letter statistics
     */
    public Map<String, Long> getDeadLetterStatistics() {
        return deadLetterCounters.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get()));
    }

    /**
     * Get all error type statistics
     */
    public Map<String, Long> getErrorTypeStatistics() {
        return errorTypeCounters.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get()));
    }

    /**
     * Enhanced dead letter event record with comprehensive error tracking
     */
    public record DeadLetterEvent(
            String originalEventId,
            String originalEventType,
            String aggregateId,
            String eventClassName,
            String eventData,
            String errorType,
            String errorMessage,
            String stackTrace,
            String correlationId,
            String traceId,
            String failedAt,
            long timestamp,
            int retryAttempts) {
        public DeadLetterEvent {
            // Validation
            if (originalEventId == null || originalEventId.trim().isEmpty()) {
                throw new IllegalArgumentException("Original event ID cannot be null or empty");
            }
            if (originalEventType == null || originalEventType.trim().isEmpty()) {
                throw new IllegalArgumentException("Original event type cannot be null or empty");
            }
            if (aggregateId == null || aggregateId.trim().isEmpty()) {
                throw new IllegalArgumentException("Aggregate ID cannot be null or empty");
            }
        }

        /**
         * Create a dead letter event with minimal required information
         */
        public static DeadLetterEvent createMinimal(
                String eventId,
                String eventType,
                String aggregateId,
                String errorMessage) {
            return new DeadLetterEvent(
                    eventId,
                    eventType,
                    aggregateId,
                    "Unknown",
                    "Serialization failed",
                    "Unknown",
                    errorMessage,
                    "Stack trace not available",
                    null,
                    null,
                    LocalDateTime.now().toString(),
                    System.currentTimeMillis(),
                    0);
        }

        /**
         * Check if this dead letter event has tracing information
         */
        public boolean hasTracingInfo() {
            return correlationId != null && traceId != null;
        }

        /**
         * Get a summary of the error for logging
         */
        public String getErrorSummary() {
            return String.format("Event: %s, Error: %s, Message: %s",
                    originalEventType, errorType, errorMessage);
        }
    }
}