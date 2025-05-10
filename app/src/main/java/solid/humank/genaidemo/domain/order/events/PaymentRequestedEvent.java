package solid.humank.genaidemo.domain.order.events;

import solid.humank.genaidemo.domain.common.events.DomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;

import java.util.UUID;

/**
 * 支付請求事件
 * 
 * 當訂單需要支付時觸發的領域事件。
 * 包含支付ID、訂單ID和支付金額等信息。
 */
public class PaymentRequestedEvent implements DomainEvent {
    private final UUID paymentId;
    private final UUID orderId;
    private final Money amount;

    private final UUID eventId;
    private final java.time.Instant occurredOn;
    private final String eventType;
    
    /**
     * 建立支付請求事件
     * 
     * @param paymentId 支付ID
     * @param orderId 訂單ID
     * @param amount 支付金額
     */
    public PaymentRequestedEvent(UUID paymentId, UUID orderId, Money amount) {
        this.eventId = UUID.randomUUID();
        this.occurredOn = java.time.Instant.now();
        this.eventType = "PaymentRequested";
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
    }

    /**
     * 獲取支付ID
     * 
     * @return 支付ID
     */
    public UUID getPaymentId() {
        return paymentId;
    }

    /**
     * 獲取訂單ID
     * 
     * @return 訂單ID
     */
    public UUID getOrderId() {
        return orderId;
    }

    /**
     * 獲取支付金額
     * 
     * @return 支付金額
     */
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