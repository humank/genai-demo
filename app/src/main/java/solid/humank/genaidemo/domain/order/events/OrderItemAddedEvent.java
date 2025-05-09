package solid.humank.genaidemo.domain.order.events;

import java.time.Instant;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.events.DomainEvent;
import solid.humank.genaidemo.domain.order.model.valueobject.Money;
import solid.humank.genaidemo.domain.order.model.valueobject.OrderId;

public record OrderItemAddedEvent(
    UUID eventId,
    Instant occurredOn,
    String eventType,
    OrderId orderId,
    String productId,
    int quantity,
    Money unitPrice
) implements DomainEvent {

    public OrderItemAddedEvent(
        OrderId orderId,
        String productId,
        int quantity,
        Money unitPrice
    ) {
        this(
            UUID.randomUUID(),
            Instant.now(),
            "OrderItemAdded",
            orderId,
            productId,
            quantity,
            unitPrice
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