package solid.humank.genaidemo.domain.payment.model.events;

import java.time.Instant;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.events.DomainEvent;

/**
 * 支付失敗事件
 */
public class PaymentFailedEvent implements DomainEvent {
    private final UUID eventId;
    private final Instant occurredOn;
    private final String eventType;
    private final UUID paymentId;
    private final UUID orderId;
    private final String errorMessage;
    
    public PaymentFailedEvent(UUID paymentId, UUID orderId, String errorMessage) {
        this.eventId = UUID.randomUUID();
        this.occurredOn = Instant.now();
        this.eventType = "PaymentFailed";
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.errorMessage = errorMessage;
    }
    
    @Override
    public UUID getEventId() {
        return eventId;
    }
    
    @Override
    public Instant getOccurredOn() {
        return occurredOn;
    }
    
    @Override
    public String getEventType() {
        return eventType;
    }
    
    public UUID getPaymentId() {
        return paymentId;
    }
    
    public UUID getOrderId() {
        return orderId;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
}