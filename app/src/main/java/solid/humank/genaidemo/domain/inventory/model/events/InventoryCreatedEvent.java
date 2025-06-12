package solid.humank.genaidemo.domain.inventory.model.events;

import solid.humank.genaidemo.domain.common.event.AbstractDomainEvent;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryId;

/**
 * 庫存創建事件
 */
public class InventoryCreatedEvent extends AbstractDomainEvent {
    
    private final InventoryId inventoryId;
    private final String productId;
    private final String productName;
    private final int initialQuantity;
    
    public InventoryCreatedEvent(InventoryId inventoryId, String productId, String productName, int initialQuantity) {
        super("inventory-service");
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.productName = productName;
        this.initialQuantity = initialQuantity;
    }
    
    public InventoryId getInventoryId() {
        return inventoryId;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public int getInitialQuantity() {
        return initialQuantity;
    }
    
    @Override
    public String getEventType() {
        return "InventoryCreatedEvent";
    }
}