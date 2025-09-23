package solid.humank.genaidemo.infrastructure.event.publisher;

import java.util.Map;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * Interface for Dead Letter Service implementations
 * 
 * This interface allows for different implementations of dead letter handling
 * based on the environment (in-memory for development, Kafka for production).
 */
public interface DeadLetterServiceInterface {

    /**
     * Send failed event to dead letter queue
     * 
     * @param event The failed domain event
     * @param cause The exception that caused the failure
     */
    void sendToDeadLetter(DomainEvent event, Throwable cause);

    /**
     * Get dead letter count for a specific event type
     * 
     * @param eventType The event type
     * @return The count of dead letter events for this type
     */
    long getDeadLetterCount(String eventType);

    /**
     * Get error count for a specific error type
     * 
     * @param errorType The error type
     * @return The count of errors for this type
     */
    long getErrorTypeCount(String errorType);

    /**
     * Get total dead letter events
     * 
     * @return The total count of dead letter events
     */
    long getTotalDeadLetterEvents();

    /**
     * Get all dead letter statistics
     * 
     * @return Map of event types to their dead letter counts
     */
    Map<String, Long> getDeadLetterStatistics();

    /**
     * Get all error type statistics
     * 
     * @return Map of error types to their counts
     */
    Map<String, Long> getErrorTypeStatistics();
}