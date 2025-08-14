package solid.humank.genaidemo.application.inventory.dto.command;

/**
 * 調整庫存命令
 */
public class AdjustInventoryCommand {
    private final String productId;
    private final int quantity;
    private final String reason;
    private final AdjustmentType type;

    public AdjustInventoryCommand(String productId, int quantity, String reason, AdjustmentType type) {
        this.productId = productId;
        this.quantity = quantity;
        this.reason = reason;
        this.type = type;
    }

    public static AdjustInventoryCommand of(String productId, int quantity, String reason, AdjustmentType type) {
        return new AdjustInventoryCommand(productId, quantity, reason, type);
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getReason() {
        return reason;
    }

    public AdjustmentType getType() {
        return type;
    }

    public enum AdjustmentType {
        INCREASE,  // 增加庫存
        DECREASE,  // 減少庫存
        SET        // 設定庫存
    }
}