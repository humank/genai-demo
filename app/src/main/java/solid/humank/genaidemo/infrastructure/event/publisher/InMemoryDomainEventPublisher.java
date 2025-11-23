package solid.humank.genaidemo.infrastructure.event.publisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.event.DomainEventPublisher;

/**
 * Enhanced In-memory domain event publisher for development profile
 * 
 * Features:
 * - Publishes events as Spring ApplicationEvents for local processing
 * - Thread-safe event collection for testing and debugging
 * - Transactional event publishing with @TransactionalEventListener
 * - Enhanced logging with correlation IDs and tracing
 * - Event metrics and statistics for development monitoring
 * - Event replay capability for debugging
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6
 */
@Component
@Profile("local")
public class InMemoryDomainEventPublisher implements DomainEventPublisher {    private static final Logger logger = LoggerFactory.getLogger(InMemoryDomainEventPublisher.class);

    private final ApplicationEventPublisher springEventPublisher;
    private final List<DomainEvent> publishedEvents = new CopyOnWriteArrayList<>();
    private final Map<String, AtomicLong> eventCounters = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastEventTimes = new ConcurrentHashMap<>();
    private final AtomicLong totalEventsPublished = new AtomicLong(0);

    public InMemoryDomainEventPublisher(ApplicationEventPublisher springEventPublisher) {
        this.springEventPublisher = springEventPublisher;
        logger.info("Enhanced InMemoryDomainEventPublisher initialized for development profile");
        logger.info("Features enabled: event tracking, correlation IDs, development metrics");
    }

    @Override
    public void publish(DomainEvent event) {
        if (event == null) {
            logger.warn("Attempted to publish null domain event");
            return;
        }

        // Set correlation ID for tracing if not already set
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = event.getEventId().toString();
            MDC.put("correlationId", correlationId);
        }

        logger.debug("Publishing domain event: {} with ID: {} from aggregate: {} [correlationId: {}]",
                event.getEventType(), event.getEventId(), event.getAggregateId(), correlationId);

        // Update development metrics
        updateEventMetrics(event);

        // Publish as Spring ApplicationEvent for local processing
        DomainEventPublisherAdapter.DomainEventWrapper wrapper = new DomainEventPublisherAdapter.DomainEventWrapper(
                event);
        springEventPublisher.publishEvent(wrapper);

        // Store for testing and debugging purposes
        publishedEvents.add(event);

        logger.debug("Domain event published successfully: {} [total events: {}]",
                event.getEventType(), totalEventsPublished.get());
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            logger.debug("No events to publish");
            return;
        }

        logger.debug("Publishing {} domain events in batch", events.size());

        for (DomainEvent event : events) {
            publish(event);
        }

        logger.debug("All {} domain events published successfully [total events: {}]",
                events.size(), totalEventsPublished.get());
    }

    /**
     * Handle transactional event processing
     * This method demonstrates how events can be processed after transaction commit
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransactionalEvent(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        DomainEvent event = wrapper.getSource();
        String correlationId = MDC.get("correlationId");

        logger.debug("Processing transactional event after commit: {} with ID: {} [correlationId: {}]",
                event.getEventType(), event.getEventId(), correlationId);

        // In development, we can add additional processing here
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

        // Example: Trigger development-specific side effects
        triggerDevelopmentSideEffects(event);
    }

    /**
     * Update event metrics for development monitoring
     * 
     * @param event The domain event
     */
    private void updateEventMetrics(DomainEvent event) {
        String eventType = event.getEventType();

        // Update event counters
        eventCounters.computeIfAbsent(eventType, k -> new AtomicLong(0)).incrementAndGet();
        totalEventsPublished.incrementAndGet();

        // Update last event times
        lastEventTimes.put(eventType, LocalDateTime.now());

        logger.trace("Event metrics updated - Type: {}, Count: {}, Total: {}",
                eventType, eventCounters.get(eventType).get(), totalEventsPublished.get());
    }

    /**
     * Update development metrics for monitoring
     * 
     * @param event The domain event
     */
    private void updateDevelopmentMetrics(DomainEvent event) {
        logger.debug("Development metrics updated for event: {} from aggregate: {}",
                event.getEventType(), event.getAggregateId());
    }

    /**
     * Trigger development-specific side effects
     * 
     * @param event The domain event
     */
    private void triggerDevelopmentSideEffects(DomainEvent event) {
        // In development, we might want to:
        // - Update search indexes
        // - Send test notifications
        // - Update development dashboards
        logger.debug("Development side effects triggered for event: {}", event.getEventType());
    }

    // === Testing and Debugging Methods ===

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
     * Get published events by aggregate ID for testing
     * 
     * @param aggregateId The aggregate ID to filter by
     * @return List of events from the specified aggregate
     */
    public List<DomainEvent> getPublishedEventsByAggregateId(String aggregateId) {
        return publishedEvents.stream()
                .filter(event -> aggregateId.equals(event.getAggregateId()))
                .toList();
    }

    /**
     * Clear all published events (useful for testing)
     */
    public void clearPublishedEvents() {
        publishedEvents.clear();
        eventCounters.clear();
        lastEventTimes.clear();
        totalEventsPublished.set(0);
        logger.debug("Cleared all published events and metrics");
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
     * Get count of published events by type
     * 
     * @param eventType The event type
     * @return Number of published events of the specified type
     */
    public long getPublishedEventCountByType(String eventType) {
        return eventCounters.getOrDefault(eventType, new AtomicLong(0)).get();
    }

    /**
     * Get total events published across all types
     * 
     * @return Total number of events published
     */
    public long getTotalEventsPublished() {
        return totalEventsPublished.get();
    }

    /**
     * Get last event time for a specific event type
     * 
     * @param eventType The event type
     * @return Last time an event of this type was published
     */
    public LocalDateTime getLastEventTime(String eventType) {
        return lastEventTimes.get(eventType);
    }

    /**
     * Get event statistics for development monitoring
     * 
     * @return Map of event types to their counts
     */
    public Map<String, Long> getEventStatistics() {
        return eventCounters.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get()));
    }

    /**
     * Replay events for debugging (re-publish all stored events)
     * Useful for testing event handlers
     */
    public void replayAllEvents() {
        logger.info("Replaying {} stored events for debugging", publishedEvents.size());

        List<DomainEvent> eventsToReplay = List.copyOf(publishedEvents);
        clearPublishedEvents();

        for (DomainEvent event : eventsToReplay) {
            publish(event);
        }

        logger.info("Replayed {} events successfully", eventsToReplay.size());
    }

    /**
     * Check if any events have been published
     * 
     * @return true if events have been published, false otherwise
     */
    public boolean hasPublishedEvents() {
        return !publishedEvents.isEmpty();
    }

    /**
     * Wait for a specific number of events to be published (useful for testing)
     * 
     * @param expectedCount The expected number of events
     * @param timeoutMs     Timeout in milliseconds
     * @return true if the expected count was reached, false if timeout
     */
    public boolean waitForEvents(int expectedCount, long timeoutMs) {
        long startTime = System.currentTimeMillis();

        while (publishedEvents.size() < expectedCount) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                logger.warn("Timeout waiting for {} events, only {} published",
                        expectedCount, publishedEvents.size());
                return false;
            }

            try {
                Thread.sleep(10); // Small delay to avoid busy waiting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        logger.debug("Successfully waited for {} events", expectedCount);
        return true;
    }
}