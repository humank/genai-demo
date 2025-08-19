package solid.humank.genaidemo.domain.order.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;

/**
 * 訂單項添加事件
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record OrderItemAddedEvent(
        OrderId orderId,
        String productId,
        int quantity,
        Money price,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static OrderItemAddedEvent create(OrderId orderId, String productId, int quantity, Money price) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new OrderItemAddedEvent(orderId, productId, quantity, price,
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
