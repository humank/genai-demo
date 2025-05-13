package solid.humank.genaidemo.infrastructure.inventory.exception;

/**
 * 庫存異常基類
 */
public class InventoryException extends RuntimeException {
    
    public InventoryException(String message) {
        super(message);
    }
    
    public InventoryException(String message, Throwable cause) {
        super(message, cause);
    }
}