package solid.humank.genaidemo.infrastructure.event.publisher;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.event.DomainEventPublisher;

/**
 * In-memory domain event publisher for development profile
 * 
 * Features:
 * - Publishes events as Spring ApplicationEvents for local processing
 * - Thread-safe event collection for testing and debugging
 * - Transactional event publishing with @TransactionalEventListener
 * - Simple logging for development debugging
 * 
 * Requirements: 2.1, 2.2, 2.3
 */
public class InMemoryDomainEventPublisher implements DomainEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryDomainEventPublisher.class);
    
    private final ApplicationEventPublisher springEventPublisher;
    private final List<DomainEvent> publishedEvents = new CopyOnWriteArrayList<>();

    public InMemoryDomainEventPublisher(ApplicationEventPublisher springEventPublisher) {
        this.springEventPublisher = springEventPublisher;
        logger.info("InMemoryDomainEventPublisher initialized for development profile");
    }

    @Override
    public void publish(DomainEvent event) {
        if (event == null) {
            logger.warn("Attempted to publish null domain event");
            return;
        }

        logger.debug("Publishing domain event: {} with ID: {}", 
            event.getEventType(), event.getEventId());

        // Publish as Spring ApplicationEvent for local processing
        DomainEventPublisherAdapter.DomainEventWrapper wrapper = 
            new DomainEventPublisherAdapter.DomainEventWrapper(event);
        springEventPublisher.publishEvent(wrapper);

        // Store for testing and debugging purposes
        publishedEvents.add(event);
        
        logger.debug("Domain event published successfully: {}", event.getEventType());
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            logger.debug("No events to publish");
            return;
        }

        logger.debug("Publishing {} domain events", events.size());
        
        for (DomainEvent event : events) {
            publish(event);
        }
        
        logger.debug("All {} domain events published successfully", events.size());
    }

    /**
     * Get all published events for testing and debugging
     * Thread-safe access to published events
     * 
     * @return List of all published events
     */
    public List<DomainEvent> getPublishedEvents() {
        return List.copyOf(publishedEvents);
    }

    /**
     * Get published events by type for testing
     * 
     * @param eventType The event type to filter by
     * @return List of events matching the type
     */
    public List<DomainEvent> getPublishedEventsByType(String eventType) {
        return publishedEvents.stream()
            .filter(event -> eventType.equals(event.getEventType()))
            .toList();
    }

    /**
     * Clear all published events (useful for testing)
     */
    public void clearPublishedEvents() {
        publishedEvents.clear();
        logger.debug("Cleared all published events");
    }

    /**
     * Get count of published events
     * 
     * @return Number of published events
     */
    public int getPublishedEventCount() {
        return publishedEvents.size();
    }

    /**
     * Handle transactional event processing
     * This method demonstrates how events can be processed after transaction commit
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransactionalEvent(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        DomainEvent event = wrapper.getSource();
        logger.debug("Processing transactional event after commit: {} with ID: {}", 
            event.getEventType(), event.getEventId());
        
        // In development, we can add additional processing here
        // For example, updating search indexes, sending notifications, etc.
        processEventAfterCommit(event);
    }

    /**
     * Process event after transaction commit
     * Override this method in tests or extend this class for custom processing
     * 
     * @param event The domain event to process
     */
    protected void processEventAfterCommit(DomainEvent event) {
        logger.debug("Event processed after transaction commit: {}", event.getEventType());
        
        // Example: Update development metrics
        updateDevelopmentMetrics(event);
    }

    /**
     * Update development metrics for monitoring
     * 
     * @param event The domain event
     */
    private void updateDevelopmentMetrics(DomainEvent event) {
        // In development, we can track event metrics for debugging
        logger.debug("Development metrics updated for event: {} from aggregate: {}", 
            event.getEventType(), event.getAggregateId());
    }
}