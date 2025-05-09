package solid.humank.genaidemo.domain.payment.events;

import solid.humank.genaidemo.domain.common.events.DomainEvent;

import java.util.UUID;

/**
 * 支付失敗事件
 */
public class PaymentFailedEvent implements DomainEvent {
    private final UUID paymentId;
    private final UUID orderId;
    private final String reason;

    private final UUID eventId;
    private final java.time.Instant occurredOn;
    private final String eventType;
    
    public PaymentFailedEvent(UUID paymentId, UUID orderId, String reason) {
        this.eventId = UUID.randomUUID();
        this.occurredOn = java.time.Instant.now();
        this.eventType = "PaymentFailed";
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.reason = reason;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getReason() {
        return reason;
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