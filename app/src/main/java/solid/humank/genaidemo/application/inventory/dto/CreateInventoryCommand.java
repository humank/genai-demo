package solid.humank.genaidemo.application.inventory.dto;

/** 創建庫存命令 */
public class CreateInventoryCommand {
    private final String productId;
    private final String productName;
    private final int initialQuantity;
    private final int threshold;

    public CreateInventoryCommand(
            String productId, String productName, int initialQuantity, int threshold) {
        this.productId = productId;
        this.productName = productName;
        this.initialQuantity = initialQuantity;
        this.threshold = threshold;
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

    public int getThreshold() {
        return threshold;
    }
}
