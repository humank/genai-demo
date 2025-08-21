package solid.humank.genaidemo.infrastructure.inventory.exception;

/** 庫存不足異常 */
public class InsufficientInventoryException extends InventoryException {

    private final String productId;
    private final int requestedQuantity;
    private final int availableQuantity;

    public InsufficientInventoryException(
            String productId, int requestedQuantity, int availableQuantity) {
        super(
                String.format(
                        "庫存不足，產品 %s 需要 %d，但只有 %d",
                        productId, requestedQuantity, availableQuantity));
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
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
}
