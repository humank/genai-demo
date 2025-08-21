package solid.humank.genaidemo.domain.order.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/**
 * 訂單支付請求事件
 * 
 * 當訂單確認後發布此事件，請求支付系統處理支付
 * 
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record OrderPaymentRequestedEvent(
        OrderId orderId,
        CustomerId customerId,
        Money paymentAmount,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static OrderPaymentRequestedEvent create(OrderId orderId,
            CustomerId customerId,
            Money paymentAmount) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new OrderPaymentRequestedEvent(orderId, customerId, paymentAmount,
                metadata.eventId(), metadata.occurredOn());
    }

    // 向後兼容的構造函數
    public OrderPaymentRequestedEvent(
            OrderId orderId,
            CustomerId customerId,
            Money paymentAmount) {
        this(orderId, customerId, paymentAmount, UUID.randomUUID(), LocalDateTime.now());
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String getEventType() {
        return DomainEvent.getEventTypeFromClass(this.getClass());
    }

    @Override
    public String getAggregateId() {
        return orderId.getValue();
    }
}