package solid.humank.genaidemo.domain.payment.model.events;

import java.time.Instant;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.events.DomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;

/**
 * 支付完成事件
 */
public class PaymentCompletedEvent implements DomainEvent {
    private final UUID eventId;
    private final Instant occurredOn;
    private final String eventType;
    private final UUID paymentId;
    private final UUID orderId;
    private final Money amount;
    private final String transactionId;
    
    public PaymentCompletedEvent(UUID paymentId, UUID orderId, Money amount, String transactionId) {
        this.eventId = UUID.randomUUID();
        this.occurredOn = Instant.now();
        this.eventType = "PaymentCompleted";
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.transactionId = transactionId;
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
    
    public Money getAmount() {
        return amount;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
}