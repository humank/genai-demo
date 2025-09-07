package solid.humank.genaidemo.application.common.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import solid.humank.genaidemo.domain.common.aggregate.AggregateRootInterface;
import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.event.DomainEventPublisher;

/**
 * Domain Event Application Service
 * 
 * Provides centralized domain event publishing functionality for application
 * services.
 * Handles event collection from aggregates and coordinates with the appropriate
 * event publisher based on the active profile.
 * 
 * Features:
 * - Centralized event publishing coordination
 * - Aggregate event collection and management
 * - Correlation ID and tracing support
 * - Transactional event publishing
 * - Profile-aware event handling
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6
 */
@Service
@Transactional
public class DomainEventApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(DomainEventApplicationService.class);

    private final DomainEventPublisher domainEventPublisher;

    public DomainEventApplicationService(DomainEventPublisher domainEventPublisher) {
        this.domainEventPublisher = domainEventPublisher;
        logger.info("DomainEventApplicationService initialized with publisher: {}",
                domainEventPublisher.getClass().getSimpleName());
    }

    /**
     * Publish events from an aggregate root
     * Collects uncommitted events from the aggregate and publishes them
     * 
     * @param aggregateRoot The aggregate root containing events to publish
     */
    public void publishEventsFromAggregate(AggregateRootInterface aggregateRoot) {
        if (aggregateRoot == null) {
            logger.warn("Attempted to publish events from null aggregate root");
            return;
        }

        if (!aggregateRoot.hasUncommittedEvents()) {
            logger.debug("No uncommitted events found in aggregate");
            return;
        }

        List<DomainEvent> events = aggregateRoot.getUncommittedEvents();
        String correlationId = MDC.get("correlationId");

        logger.debug("Publishing {} events from aggregate [correlationId: {}]",
                events.size(), correlationId);

        try {
            // Publish all events
            domainEventPublisher.publishAll(events);

            // Mark events as committed after successful publishing
            aggregateRoot.markEventsAsCommitted();

            logger.debug("Successfully published {} events from aggregate [correlationId: {}]",
                    events.size(), correlationId);

        } catch (Exception e) {
            logger.error("Failed to publish events from aggregate [correlationId: {}]",
                    correlationId, e);

            // Don't mark events as committed if publishing failed
            // This allows for retry in the next transaction
            throw new DomainEventPublishingException("Failed to publish domain events", e);
        }
    }

    /**
     * Publish a single domain event
     * 
     * @param event The domain event to publish
     */
    public void publishEvent(DomainEvent event) {
        if (event == null) {
            logger.warn("Attempted to publish null domain event");
            return;
        }

        String correlationId = MDC.get("correlationId");
        logger.debug("Publishing single event: {} [correlationId: {}]",
                event.getEventType(), correlationId);

        try {
            domainEventPublisher.publish(event);

            logger.debug("Successfully published event: {} [correlationId: {}]",
                    event.getEventType(), correlationId);

        } catch (Exception e) {
            logger.error("Failed to publish event: {} [correlationId: {}]",
                    event.getEventType(), correlationId, e);
            throw new DomainEventPublishingException("Failed to publish domain event", e);
        }
    }

    /**
     * Publish multiple domain events
     * 
     * @param events The list of domain events to publish
     */
    public void publishEvents(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            logger.debug("No events to publish");
            return;
        }

        String correlationId = MDC.get("correlationId");
        logger.debug("Publishing {} events [correlationId: {}]",
                events.size(), correlationId);

        try {
            domainEventPublisher.publishAll(events);

            logger.debug("Successfully published {} events [correlationId: {}]",
                    events.size(), correlationId);

        } catch (Exception e) {
            logger.error("Failed to publish {} events [correlationId: {}]",
                    events.size(), correlationId, e);
            throw new DomainEventPublishingException("Failed to publish domain events", e);
        }
    }

    /**
     * Publish events from multiple aggregates
     * Useful for complex business operations involving multiple aggregates
     * 
     * @param aggregateRoots The list of aggregate roots containing events to
     *                       publish
     */
    public void publishEventsFromAggregates(List<AggregateRootInterface> aggregateRoots) {
        if (aggregateRoots == null || aggregateRoots.isEmpty()) {
            logger.debug("No aggregates provided for event publishing");
            return;
        }

        String correlationId = MDC.get("correlationId");
        int totalEvents = 0;

        // Count total events
        for (AggregateRootInterface aggregate : aggregateRoots) {
            if (aggregate != null && aggregate.hasUncommittedEvents()) {
                totalEvents += aggregate.getUncommittedEvents().size();
            }
        }

        if (totalEvents == 0) {
            logger.debug("No uncommitted events found in any aggregates");
            return;
        }

        logger.debug("Publishing events from {} aggregates (total {} events) [correlationId: {}]",
                aggregateRoots.size(), totalEvents, correlationId);

        try {
            // Publish events from each aggregate
            for (AggregateRootInterface aggregate : aggregateRoots) {
                if (aggregate != null && aggregate.hasUncommittedEvents()) {
                    publishEventsFromAggregate(aggregate);
                }
            }

            logger.debug("Successfully published events from {} aggregates [correlationId: {}]",
                    aggregateRoots.size(), correlationId);

        } catch (Exception e) {
            logger.error("Failed to publish events from aggregates [correlationId: {}]",
                    correlationId, e);
            throw new DomainEventPublishingException("Failed to publish events from aggregates", e);
        }
    }

    /**
     * Set correlation ID for event tracing
     * Useful for maintaining trace context across service boundaries
     * 
     * @param correlationId The correlation ID to set
     */
    public void setCorrelationId(String correlationId) {
        if (correlationId != null && !correlationId.trim().isEmpty()) {
            MDC.put("correlationId", correlationId);
            logger.debug("Correlation ID set: {}", correlationId);
        }
    }

    /**
     * Clear correlation ID from MDC
     */
    public void clearCorrelationId() {
        MDC.remove("correlationId");
        logger.debug("Correlation ID cleared");
    }

    /**
     * Get the current correlation ID
     * 
     * @return The current correlation ID or null if not set
     */
    public String getCorrelationId() {
        return MDC.get("correlationId");
    }

    /**
     * Publish events from an aggregate root synchronously (for testing)
     * Similar to publishEventsFromAggregate but without transaction handling
     * 
     * @param aggregateRoot The aggregate root containing events to publish
     */
    public void publishEventsFromAggregateSync(
            solid.humank.genaidemo.domain.common.aggregate.AggregateRootInterface aggregateRoot) {
        if (aggregateRoot == null) {
            logger.warn("Attempted to publish events from null aggregate root");
            return;
        }

        if (!aggregateRoot.hasUncommittedEvents()) {
            logger.debug("No uncommitted events found in aggregate");
            return;
        }

        List<DomainEvent> events = aggregateRoot.getUncommittedEvents();
        String correlationId = MDC.get("correlationId");

        logger.debug("Publishing {} events from aggregate synchronously [correlationId: {}]",
                events.size(), correlationId);

        try {
            // Publish all events synchronously
            domainEventPublisher.publishAll(events);

            // Mark events as committed after successful publishing
            aggregateRoot.markEventsAsCommitted();

            logger.debug("Successfully published {} events from aggregate synchronously [correlationId: {}]",
                    events.size(), correlationId);

        } catch (Exception e) {
            logger.error("Failed to publish events from aggregate synchronously [correlationId: {}]",
                    correlationId, e);
            throw new DomainEventPublishingException("Failed to publish domain events synchronously", e);
        }
    }

    /**
     * Check if events can be published (publisher is available)
     * 
     * @return true if events can be published, false otherwise
     */
    public boolean canPublishEvents() {
        return domainEventPublisher != null;
    }

    /**
     * Get information about the current event publisher
     * 
     * @return Publisher class name for debugging
     */
    public String getPublisherInfo() {
        return domainEventPublisher != null ? domainEventPublisher.getClass().getSimpleName()
                : "No publisher available";
    }

    /**
     * Exception thrown when domain event publishing fails
     */
    public static class DomainEventPublishingException extends RuntimeException {
        public DomainEventPublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}