package solid.humank.genaidemo.domain.order.events;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.events.DomainEvent;
import solid.humank.genaidemo.domain.order.model.valueobject.Money;
import solid.humank.genaidemo.domain.order.model.valueobject.OrderId;

/**
 * 訂單創建事件
 * 注意：此事件與 domain.order.model.events 包中的事件重複
 * @deprecated 請使用 {@link solid.humank.genaidemo.domain.order.model.events.OrderCreatedEvent} 代替
 */
@Deprecated
public record OrderCreatedEvent(
    UUID eventId,
    Instant occurredOn,
    String eventType,
    OrderId orderId,
    String customerId,
    Money totalAmount,
    List<Map<String, Object>> orderItems
) implements DomainEvent {
    
    public OrderCreatedEvent(
        OrderId orderId,
        String customerId,
        Money totalAmount,
        List<Map<String, Object>> orderItems
    ) {
        this(
            UUID.randomUUID(),
            Instant.now(),
            "OrderCreated",
            orderId,
            customerId,
            totalAmount,
            orderItems
        );
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
}