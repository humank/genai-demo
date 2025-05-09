package solid.humank.genaidemo.domain.payment.events;

import solid.humank.genaidemo.domain.common.events.DomainEvent;
import solid.humank.genaidemo.domain.order.model.valueobject.Money;

import java.util.UUID;

/**
 * 支付完成事件
 */
public class PaymentCompletedEvent implements DomainEvent {
    private final UUID paymentId;
    private final UUID orderId;
    private final Money amount;
    private final String transactionId;

    private final UUID eventId;
    private final java.time.Instant occurredOn;
    private final String eventType;
    
    public PaymentCompletedEvent(UUID paymentId, UUID orderId, Money amount, String transactionId) {
        this.eventId = UUID.randomUUID();
        this.occurredOn = java.time.Instant.now();
        this.eventType = "PaymentCompleted";
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.transactionId = transactionId;
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

    public String getTransactionId() {
        return transactionId;
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