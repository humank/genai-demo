package solid.humank.genaidemo.domain.order.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;

/**
 * 訂單提交事件
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record OrderSubmittedEvent(
        OrderId orderId,
        String customerId,
        Money totalAmount,
        int itemCount,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static OrderSubmittedEvent create(
            OrderId orderId, String customerId, Money totalAmount, int itemCount) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new OrderSubmittedEvent(orderId, customerId, totalAmount, itemCount,
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
        return orderId.getValue();
    }
}
