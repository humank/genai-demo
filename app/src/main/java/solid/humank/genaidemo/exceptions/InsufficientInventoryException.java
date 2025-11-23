package solid.humank.genaidemo.exceptions;

/**
 * Exception thrown when there is insufficient inventory to fulfill a request
 *
 * This exception is used when attempting to reserve or allocate inventory
 * that exceeds the available quantity.
 */
public class InsufficientInventoryException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor with message
     *
     * @param message error message
     */
    public InsufficientInventoryException(String message) {
        super(message);
    }

    /**
     * Constructor with message and cause
     *
     * @param message error message
     * @param cause   the cause
     */
    public InsufficientInventoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
