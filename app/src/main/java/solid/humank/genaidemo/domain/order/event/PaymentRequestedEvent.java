package solid.humank.genaidemo.domain.order.event;

import solid.humank.genaidemo.domain.common.events.DomainEvent;
import solid.humank.genaidemo.domain.order.model.valueobject.Money;

import java.util.UUID;

/**
 * 支付請求事件
 */
public class PaymentRequestedEvent implements DomainEvent {
    private final UUID paymentId;
    private final UUID orderId;
    private final Money amount;

    private final UUID eventId;
    private final java.time.Instant occurredOn;
    private final String eventType;
    
    public PaymentRequestedEvent(UUID paymentId, UUID orderId, Money amount) {
        this.eventId = UUID.randomUUID();
        this.occurredOn = java.time.Instant.now();
        this.eventType = "PaymentRequested";
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Money getAmount() {
        return amount;
    }
    
    @Override
    public UUID getEventId() {
        return eventId;
    }
    
    @Override
    public java.time.Instant getOccurredOn() {
        return occurredOn;
    }
    
    @Override
    public String getEventType() {
        return eventType;
    }
}