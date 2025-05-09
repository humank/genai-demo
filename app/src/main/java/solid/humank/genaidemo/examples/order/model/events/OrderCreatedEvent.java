package solid.humank.genaidemo.examples.order.model.events;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import solid.humank.genaidemo.ddd.events.DomainEvent;
import solid.humank.genaidemo.examples.order.model.valueobject.Money;
import solid.humank.genaidemo.examples.order.model.valueobject.OrderId;

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