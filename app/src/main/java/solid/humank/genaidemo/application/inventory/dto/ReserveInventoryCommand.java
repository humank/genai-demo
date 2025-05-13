package solid.humank.genaidemo.application.inventory.dto;

import java.util.UUID;

/**
 * 預留庫存命令
 */
public class ReserveInventoryCommand {
    private final String productId;
    private final int quantity;
    private final UUID orderId;

    public ReserveInventoryCommand(String productId, int quantity, UUID orderId) {
        this.productId = productId;
        this.quantity = quantity;
        this.orderId = orderId;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public UUID getOrderId() {
        return orderId;
    }
}