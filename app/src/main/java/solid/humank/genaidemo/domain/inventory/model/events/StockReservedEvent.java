package solid.humank.genaidemo.domain.inventory.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryId;
import solid.humank.genaidemo.domain.inventory.model.valueobject.ReservationId;

/**
 * 庫存預留事件
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record StockReservedEvent(
        InventoryId inventoryId,
        String productId,
        ReservationId reservationId,
        UUID orderId,
        int quantity,
        int remainingQuantity,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static StockReservedEvent create(
            InventoryId inventoryId,
            String productId,
            ReservationId reservationId,
            UUID orderId,
            int quantity,
            int remainingQuantity) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new StockReservedEvent(inventoryId, productId, reservationId, orderId, quantity, remainingQuantity,
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
        return inventoryId.getId().toString();
    }
}
