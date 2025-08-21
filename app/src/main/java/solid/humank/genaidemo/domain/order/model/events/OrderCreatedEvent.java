package solid.humank.genaidemo.domain.order.model.events;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;

/**
 * 訂單創建事件
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record OrderCreatedEvent(
        OrderId orderId,
        String customerId,
        Money totalAmount,
        List<String> items,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static OrderCreatedEvent create(
            OrderId orderId, String customerId, Money totalAmount, List<String> items) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new OrderCreatedEvent(orderId, customerId, totalAmount, List.copyOf(items),
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
