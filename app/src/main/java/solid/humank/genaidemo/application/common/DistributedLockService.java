package solid.humank.genaidemo.application.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import solid.humank.genaidemo.domain.common.lock.DistributedLockManager;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Application service that provides high-level distributed locking operations.
 * This service wraps the DistributedLockManager with business logic and error handling.
 */
@Slf4j
@Service
public class DistributedLockService {
    
    private final DistributedLockManager lockManager;
    
    // Default lock settings
    private static final Duration DEFAULT_LOCK_WAIT_TIME = Duration.ofSeconds(5);
    private static final Duration DEFAULT_LOCK_LEASE_TIME = Duration.ofSeconds(30);
    
    public DistributedLockService(DistributedLockManager lockManager) {
        this.lockManager = lockManager;
        log.info("DistributedLockService initialized with lock manager: {}", 
                lockManager.getClass().getSimpleName());
    }
    
    /**
     * Executes a task with distributed locking using default timeouts.
     * 
     * @param lockKey the unique key for the lock
     * @param task the task to execute while holding the lock
     * @param <T> the return type of the task
     * @return the result of the task execution
     * @throws IllegalStateException if the lock cannot be acquired
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> task) {
        return executeWithLock(lockKey, task, DEFAULT_LOCK_WAIT_TIME, DEFAULT_LOCK_LEASE_TIME);
    }
    
    /**
     * Executes a task with distributed locking using custom timeouts.
     * 
     * @param lockKey the unique key for the lock
     * @param task the task to execute while holding the lock
     * @param waitTime maximum time to wait for the lock
     * @param leaseTime how long to hold the lock
     * @param <T> the return type of the task
     * @return the result of the task execution
     * @throws IllegalStateException if the lock cannot be acquired
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> task, Duration waitTime, Duration leaseTime) {
        String lockValue = generateLockValue();
        
        log.debug("Attempting to acquire lock: {} with waitTime: {} and leaseTime: {}", 
                 lockKey, waitTime, leaseTime);
        
        boolean acquired = lockManager.tryLock(lockKey, leaseTime, waitTime);
        
        if (!acquired) {
            String message = String.format("Failed to acquire lock '%s' within %s", lockKey, waitTime);
            log.warn(message);
            throw new IllegalStateException(message);
        }
        
        try {
            log.debug("Lock acquired: {}, executing task", lockKey);
            T result = task.get();
            log.debug("Task completed successfully for lock: {}", lockKey);
            return result;
        } catch (Exception e) {
            log.error("Task execution failed for lock: {}", lockKey, e);
            throw e;
        } finally {
            try {
                lockManager.unlock(lockKey);
                log.debug("Lock released: {}", lockKey);
            } catch (Exception e) {
                log.error("Failed to release lock: {}", lockKey, e);
                // Don't throw here to avoid masking the original exception
            }
        }
    }
    
    /**
     * Executes a task with distributed locking, returning null if lock cannot be acquired.
     * 
     * @param lockKey the unique key for the lock
     * @param task the task to execute while holding the lock
     * @param waitTime maximum time to wait for the lock
     * @param leaseTime how long to hold the lock
     * @param <T> the return type of the task
     * @return the result of the task execution, or null if lock cannot be acquired
     */
    public <T> T tryExecuteWithLock(String lockKey, Supplier<T> task, Duration waitTime, Duration leaseTime) {
        try {
            return executeWithLock(lockKey, task, waitTime, leaseTime);
        } catch (IllegalStateException e) {
            log.debug("Could not acquire lock: {}, returning null", lockKey);
            return null;
        }
    }
    
    /**
     * Checks if a lock is currently held.
     * 
     * @param lockKey the unique key for the lock
     * @return true if the lock is held, false otherwise
     */
    public boolean isLocked(String lockKey) {
        return lockManager.isLocked(lockKey);
    }
    
    /**
     * Gets information about a lock.
     * 
     * @param lockKey the unique key for the lock
     * @return lock information, or null if lock doesn't exist
     */
    public String getLockInfo(String lockKey) {
        return lockManager.getLockInfo(lockKey);
    }
    
    /**
     * Forces the release of a lock. Use with extreme caution.
     * 
     * @param lockKey the unique key for the lock
     */
    public void forceUnlock(String lockKey) {
        log.warn("Force unlocking: {}", lockKey);
        lockManager.forceUnlock(lockKey);
    }
    
    /**
     * Creates lock keys for common business operations.
     */
    public static class LockKeys {
        private static final String PREFIX = "business-lock:";
        
        public static String customerOperation(String customerId) {
            return PREFIX + "customer:" + customerId;
        }
        
        public static String orderOperation(String orderId) {
            return PREFIX + "order:" + orderId;
        }
        
        public static String inventoryOperation(String productId) {
            return PREFIX + "inventory:" + productId;
        }
        
        public static String paymentOperation(String paymentId) {
            return PREFIX + "payment:" + paymentId;
        }
        
        public static String globalOperation(String operationName) {
            return PREFIX + "global:" + operationName;
        }
    }
    
    private String generateLockValue() {
        return "lock-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }
}