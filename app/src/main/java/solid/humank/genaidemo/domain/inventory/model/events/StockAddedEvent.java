package solid.humank.genaidemo.domain.inventory.model.events;

import solid.humank.genaidemo.domain.common.event.AbstractDomainEvent;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryId;

/** 庫存增加事件 */
public class StockAddedEvent extends AbstractDomainEvent {

    private final InventoryId inventoryId;
    private final String productId;
    private final int addedQuantity;
    private final int newTotalQuantity;

    public StockAddedEvent(
            InventoryId inventoryId, String productId, int addedQuantity, int newTotalQuantity) {
        super("inventory-service");
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.addedQuantity = addedQuantity;
        this.newTotalQuantity = newTotalQuantity;
    }

    public InventoryId getInventoryId() {
        return inventoryId;
    }

    public String getProductId() {
        return productId;
    }

    public int getAddedQuantity() {
        return addedQuantity;
    }

    public int getNewTotalQuantity() {
        return newTotalQuantity;
    }

    @Override
    public String getEventType() {
        return "StockAddedEvent";
    }
}
