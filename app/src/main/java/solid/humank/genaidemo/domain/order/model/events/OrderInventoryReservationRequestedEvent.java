package solid.humank.genaidemo.domain.order.model.events;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.OrderItem;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/**
 * 訂單庫存預留請求事件
 * 
 * 當訂單提交時發布此事件，請求庫存系統預留商品
 * 
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record OrderInventoryReservationRequestedEvent(
        OrderId orderId,
        CustomerId customerId,
        List<OrderItem> items,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static OrderInventoryReservationRequestedEvent create(OrderId orderId,
            CustomerId customerId,
            List<OrderItem> items) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new OrderInventoryReservationRequestedEvent(orderId, customerId, items,
                metadata.eventId(), metadata.occurredOn());
    }

    // 向後兼容的構造函數
    public OrderInventoryReservationRequestedEvent(
            OrderId orderId,
            CustomerId customerId,
            List<OrderItem> items) {
        this(orderId, customerId, items, UUID.randomUUID(), LocalDateTime.now());
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