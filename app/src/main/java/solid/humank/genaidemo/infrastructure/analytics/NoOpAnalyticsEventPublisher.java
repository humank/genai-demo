package solid.humank.genaidemo.infrastructure.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * No-operation implementation of AnalyticsEventPublisher for development
 * environments.
 * 
 * This implementation logs events but doesn't actually send them anywhere,
 * making it suitable for development and testing environments where
 * analytics infrastructure is not available.
 */
public class NoOpAnalyticsEventPublisher implements AnalyticsEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NoOpAnalyticsEventPublisher.class);

    @Override
    public void publish(DomainEvent event) {
        logger.debug("Analytics event (no-op): {} - {} ({})",
                event.getEventType(),
                event.getAggregateId(),
                event.getEventId());
    }

    @Override
    public void publishBatch(Iterable<DomainEvent> events) {
        int count = 0;
        for (DomainEvent event : events) {
            count++;
            logger.debug("Analytics event (no-op): {} - {} ({})",
                    event.getEventType(),
                    event.getAggregateId(),
                    event.getEventId());
        }
        logger.debug("Processed {} analytics events (no-op)", count);
    }

    @Override
    public void flush() {
        logger.debug("Analytics flush (no-op)");
    }

    @Override
    public boolean isHealthy() {
        return true; // Always healthy in no-op mode
    }
}