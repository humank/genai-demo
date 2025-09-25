package solid.humank.genaidemo.infrastructure.common.lock;

/**
 * Exception thrown when distributed lock operations fail.
 */
public class DistributedLockException extends RuntimeException {
    
    private final String lockKey;
    private final String operation;
    
    public DistributedLockException(String lockKey, String operation, String message) {
        super(String.format("Lock operation '%s' failed for key '%s': %s", operation, lockKey, message));
        this.lockKey = lockKey;
        this.operation = operation;
    }
    
    public DistributedLockException(String lockKey, String operation, String message, Throwable cause) {
        super(String.format("Lock operation '%s' failed for key '%s': %s", operation, lockKey, message), cause);
        this.lockKey = lockKey;
        this.operation = operation;
    }
    
    public String getLockKey() {
        return lockKey;
    }
    
    public String getOperation() {
        return operation;
    }
}