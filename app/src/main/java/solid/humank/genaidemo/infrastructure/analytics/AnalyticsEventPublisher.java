package solid.humank.genaidemo.infrastructure.analytics;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * Interface for publishing domain events to analytics pipeline.
 * 
 * This interface abstracts the analytics event publishing mechanism,
 * allowing for different implementations based on the environment
 * (e.g., Kinesis Firehose for production, no-op for development).
 */
public interface AnalyticsEventPublisher {

    /**
     * Publishes a single domain event to the analytics pipeline.
     * 
     * @param event The domain event to publish
     */
    void publish(DomainEvent event);

    /**
     * Publishes multiple domain events to the analytics pipeline in a batch.
     * 
     * @param events The domain events to publish
     */
    void publishBatch(Iterable<DomainEvent> events);

    /**
     * Flushes any pending events to ensure they are sent to the analytics pipeline.
     * This is useful for ensuring events are sent before application shutdown.
     */
    void flush();

    /**
     * Returns whether the analytics publisher is currently healthy and able to
     * publish events.
     * 
     * @return true if the publisher is healthy, false otherwise
     */
    boolean isHealthy();
}