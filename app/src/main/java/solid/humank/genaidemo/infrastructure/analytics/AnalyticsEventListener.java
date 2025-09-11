package solid.humank.genaidemo.infrastructure.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.infrastructure.event.publisher.DomainEventPublisherAdapter;

/**
 * Event listener that forwards domain events to the analytics pipeline.
 * 
 * This listener subscribes to all domain events published through the
 * DomainEventPublisherAdapter and forwards them to the analytics pipeline
 * for real-time business intelligence processing.
 * 
 * Requirements addressed:
 * - 9.1: Automatically update QuickSight datasets for real-time analytics
 * - 9.2: Display customer lifecycle metrics (registration, activation, churn)
 * - 9.3: Show order processing funnel, conversion rates, and revenue trends
 * - 9.4: Display stock levels, reorder alerts, and demand forecasting
 * - 9.5: Provide payment success rates, processing times, and failure analysis
 */
@Component
public class AnalyticsEventListener {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsEventListener.class);

    private final AnalyticsEventPublisher analyticsPublisher;

    public AnalyticsEventListener(AnalyticsEventPublisher analyticsPublisher) {
        this.analyticsPublisher = analyticsPublisher;
    }

    /**
     * Handles domain events after transaction commit and forwards them to analytics
     * pipeline.
     * 
     * This method is called asynchronously after the transaction commits to ensure
     * that events are only sent to analytics if the business operation succeeded.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleDomainEvent(DomainEventPublisherAdapter.DomainEventWrapper eventWrapper) {
        DomainEvent event = eventWrapper.getSource();

        try {
            // Forward event to analytics pipeline
            analyticsPublisher.publish(event);

            logger.debug("Forwarded domain event to analytics: {} ({})",
                    event.getEventType(), event.getEventId());

        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to forward domain event to analytics: {} ({})",
                    event.getEventType(), event.getEventId(), e);
        }
    }

    /**
     * Handles customer-related events for customer lifecycle analytics.
     * 
     * These events are used to track customer registration, activation,
     * profile updates, and churn metrics in the business intelligence dashboards.
     */
    @EventListener
    @Async
    public void handleCustomerEvent(DomainEvent event) {
        if (isCustomerEvent(event)) {
            logger.debug("Processing customer analytics event: {} for customer {}",
                    event.getEventType(), event.getAggregateId());

            // Customer events are already handled by the general domain event listener
            // This method could be used for customer-specific analytics processing
        }
    }

    /**
     * Handles order-related events for order processing analytics.
     * 
     * These events are used to track order funnel, conversion rates,
     * revenue trends, and processing times in the business intelligence dashboards.
     */
    @EventListener
    @Async
    public void handleOrderEvent(DomainEvent event) {
        if (isOrderEvent(event)) {
            logger.debug("Processing order analytics event: {} for order {}",
                    event.getEventType(), event.getAggregateId());

            // Order events are already handled by the general domain event listener
            // This method could be used for order-specific analytics processing
        }
    }

    /**
     * Handles inventory-related events for inventory analytics.
     * 
     * These events are used to track stock levels, reorder alerts,
     * and demand forecasting in the business intelligence dashboards.
     */
    @EventListener
    @Async
    public void handleInventoryEvent(DomainEvent event) {
        if (isInventoryEvent(event)) {
            logger.debug("Processing inventory analytics event: {} for product {}",
                    event.getEventType(), event.getAggregateId());

            // Inventory events are already handled by the general domain event listener
            // This method could be used for inventory-specific analytics processing
        }
    }

    /**
     * Handles payment-related events for payment analytics.
     * 
     * These events are used to track payment success rates, processing times,
     * and failure analysis in the business intelligence dashboards.
     */
    @EventListener
    @Async
    public void handlePaymentEvent(DomainEvent event) {
        if (isPaymentEvent(event)) {
            logger.debug("Processing payment analytics event: {} for payment {}",
                    event.getEventType(), event.getAggregateId());

            // Payment events are already handled by the general domain event listener
            // This method could be used for payment-specific analytics processing
        }
    }

    private boolean isCustomerEvent(DomainEvent event) {
        return event.getEventType().toLowerCase().contains("customer");
    }

    private boolean isOrderEvent(DomainEvent event) {
        return event.getEventType().toLowerCase().contains("order");
    }

    private boolean isInventoryEvent(DomainEvent event) {
        String eventType = event.getEventType().toLowerCase();
        return eventType.contains("inventory") || eventType.contains("product");
    }

    private boolean isPaymentEvent(DomainEvent event) {
        return event.getEventType().toLowerCase().contains("payment");
    }
}