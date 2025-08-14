package solid.humank.genaidemo.application.inventory.dto;

/** 庫存檢查結果 */
public class InventoryCheckResult {
    private final String productId;
    private final int requestedQuantity;
    private final int availableQuantity;
    private final boolean sufficient;
    private final String message;

    private InventoryCheckResult(
            String productId,
            int requestedQuantity,
            int availableQuantity,
            boolean sufficient,
            String message) {
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
        this.sufficient = sufficient;
        this.message = message;
    }

    public static InventoryCheckResult sufficient(
            String productId, int requestedQuantity, int availableQuantity) {
        return new InventoryCheckResult(
                productId, requestedQuantity, availableQuantity, true, "庫存充足");
    }

    public static InventoryCheckResult insufficient(
            String productId, int requestedQuantity, int availableQuantity) {
        return new InventoryCheckResult(
                productId,
                requestedQuantity,
                availableQuantity,
                false,
                String.format("庫存不足，需要 %d，但只有 %d", requestedQuantity, availableQuantity));
    }

    public static InventoryCheckResult notFound(String productId) {
        return new InventoryCheckResult(productId, 0, 0, false, "找不到產品庫存");
    }

    public String getProductId() {
        return productId;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public boolean isSufficient() {
        return sufficient;
    }

    public String getMessage() {
        return message;
    }
}
