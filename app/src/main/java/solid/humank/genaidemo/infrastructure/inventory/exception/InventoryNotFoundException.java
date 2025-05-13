package solid.humank.genaidemo.infrastructure.inventory.exception;

/**
 * 庫存不存在異常
 */
public class InventoryNotFoundException extends InventoryException {
    
    private final String productId;
    
    public InventoryNotFoundException(String productId) {
        super(String.format("找不到產品 %s 的庫存", productId));
        this.productId = productId;
    }
    
    public String getProductId() {
        return productId;
    }
}