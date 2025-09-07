package solid.humank.genaidemo.infrastructure.event.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.infrastructure.event.publisher.DomainEventPublisherAdapter;

/**
 * Transactional event handler demonstrating @TransactionalEventListener usage
 * 
 * Features:
 * - Processes events after transaction commit
 * - Handles rollback scenarios
 * - Supports correlation ID tracking
 * - Provides different processing phases
 * 
 * Requirements: 2.3, 2.4
 */
@Component
public class TransactionalEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(TransactionalEventHandler.class);

    /**
     * Handle events after successful transaction commit
     * This is the most common use case for domain event processing
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEventAfterCommit(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        DomainEvent event = wrapper.getSource();
        
        // Set correlation ID for tracing
        String correlationId = event.getEventId().toString();
        MDC.put("correlationId", correlationId);
        
        try {
            logger.info("Processing domain event after commit: {} with ID: {} from aggregate: {}", 
                event.getEventType(), event.getEventId(), event.getAggregateId());
            
            // Process the event (e.g., update read models, send notifications, etc.)
            processEventAfterCommit(event);
            
            logger.info("Successfully processed domain event after commit: {}", event.getEventType());
            
        } catch (Exception e) {
            logger.error("Failed to process domain event after commit: {} with ID: {}", 
                event.getEventType(), event.getEventId(), e);
            
            // In production, you might want to send this to a dead letter queue
            // or implement retry logic
            handleEventProcessingError(event, e);
            
        } finally {
            MDC.clear();
        }
    }

    /**
     * Handle events before transaction commit
     * Use this for validation or preparation tasks that should fail the transaction
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleEventBeforeCommit(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        DomainEvent event = wrapper.getSource();
        
        logger.debug("Processing domain event before commit: {} with ID: {}", 
            event.getEventType(), event.getEventId());
        
        // Perform validation or preparation tasks
        // If this method throws an exception, the transaction will be rolled back
        validateEventBeforeCommit(event);
    }

    /**
     * Handle events after transaction rollback
     * Use this for cleanup or compensation tasks
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleEventAfterRollback(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        DomainEvent event = wrapper.getSource();
        
        logger.warn("Processing domain event after rollback: {} with ID: {}", 
            event.getEventType(), event.getEventId());
        
        // Perform cleanup or compensation tasks
        handleEventAfterRollback(event);
    }

    /**
     * Handle events after transaction completion (commit or rollback)
     * Use this for tasks that should always run regardless of transaction outcome
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
    public void handleEventAfterCompletion(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        DomainEvent event = wrapper.getSource();
        
        logger.debug("Processing domain event after completion: {} with ID: {}", 
            event.getEventType(), event.getEventId());
        
        // Perform tasks that should always run (e.g., metrics, logging)
        updateEventMetrics(event);
    }

    /**
     * Process event after successful transaction commit
     * Override this method in subclasses for specific event processing logic
     * 
     * @param event The domain event to process
     */
    protected void processEventAfterCommit(DomainEvent event) {
        // Default implementation - can be overridden by specific handlers
        logger.debug("Default event processing for: {}", event.getEventType());
        
        // Example processing based on event type
        switch (event.getEventType()) {
            case "CustomerCreated" -> processCustomerCreatedEvent(event);
            case "OrderSubmitted" -> processOrderSubmittedEvent(event);
            case "PaymentProcessed" -> processPaymentProcessedEvent(event);
            default -> logger.debug("No specific processing for event type: {}", event.getEventType());
        }
    }

    /**
     * Validate event before transaction commit
     * 
     * @param event The domain event to validate
     */
    protected void validateEventBeforeCommit(DomainEvent event) {
        // Perform validation logic
        if (event.getAggregateId() == null || event.getAggregateId().trim().isEmpty()) {
            throw new IllegalStateException("Event must have a valid aggregate ID");
        }
        
        logger.debug("Event validation passed for: {}", event.getEventType());
    }

    /**
     * Handle event after transaction rollback
     * 
     * @param event The domain event from the rolled back transaction
     */
    protected void handleEventAfterRollback(DomainEvent event) {
        // Perform cleanup or compensation logic
        logger.debug("Handling rollback for event: {}", event.getEventType());
    }

    /**
     * Update event metrics for monitoring
     * 
     * @param event The domain event
     */
    protected void updateEventMetrics(DomainEvent event) {
        // Update metrics (in production, this would integrate with CloudWatch)
        logger.debug("Updating metrics for event: {} from aggregate: {}", 
            event.getEventType(), event.getAggregateId());
    }

    /**
     * Handle event processing errors
     * 
     * @param event The domain event that failed to process
     * @param error The error that occurred
     */
    protected void handleEventProcessingError(DomainEvent event, Exception error) {
        logger.error("Event processing error for: {} - {}", event.getEventType(), error.getMessage());
        
        // In production, implement retry logic or send to dead letter queue
        // For now, just log the error
    }

    /**
     * Process customer created events
     */
    private void processCustomerCreatedEvent(DomainEvent event) {
        logger.info("Processing customer created event: {}", event.getAggregateId());
        // Example: Send welcome email, update customer statistics, etc.
    }

    /**
     * Process order submitted events
     */
    private void processOrderSubmittedEvent(DomainEvent event) {
        logger.info("Processing order submitted event: {}", event.getAggregateId());
        // Example: Update inventory, send order confirmation, etc.
    }

    /**
     * Process payment processed events
     */
    private void processPaymentProcessedEvent(DomainEvent event) {
        logger.info("Processing payment processed event: {}", event.getAggregateId());
        // Example: Update order status, send receipt, etc.
    }
}