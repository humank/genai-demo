package solid.humank.genaidemo.domain.payment.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.PaymentId;

/**
 * 支付完成事件
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record PaymentCompletedEvent(
        PaymentId paymentId,
        OrderId orderId,
        Money amount,
        String transactionId,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static PaymentCompletedEvent create(
            PaymentId paymentId, OrderId orderId, Money amount, String transactionId) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new PaymentCompletedEvent(paymentId, orderId, amount, transactionId,
                metadata.eventId(), metadata.occurredOn());
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
        return paymentId.getId().toString();
    }
}
