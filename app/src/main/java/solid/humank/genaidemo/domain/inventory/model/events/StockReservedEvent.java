package solid.humank.genaidemo.domain.inventory.model.events;

import java.util.UUID;
import solid.humank.genaidemo.domain.common.event.AbstractDomainEvent;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryId;
import solid.humank.genaidemo.domain.inventory.model.valueobject.ReservationId;

/** 庫存預留事件 */
public class StockReservedEvent extends AbstractDomainEvent {

    private final InventoryId inventoryId;
    private final String productId;
    private final ReservationId reservationId;
    private final UUID orderId;
    private final int quantity;
    private final int remainingQuantity;

    public StockReservedEvent(
            InventoryId inventoryId,
            String productId,
            ReservationId reservationId,
            UUID orderId,
            int quantity,
            int remainingQuantity) {
        super("inventory-service");
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.quantity = quantity;
        this.remainingQuantity = remainingQuantity;
    }

    public InventoryId getInventoryId() {
        return inventoryId;
    }

    public String getProductId() {
        return productId;
    }

    public ReservationId getReservationId() {
        return reservationId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    @Override
    public String getEventType() {
        return "StockReservedEvent";
    }
}
