package solid.humank.genaidemo.application.inventory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import solid.humank.genaidemo.application.common.DistributedLockService;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;

import java.time.Duration;

/**
 * Example service demonstrating how to use distributed locking for inventory operations.
 * This service ensures that inventory updates are atomic across multiple application instances.
 */
@Slf4j
@Service
public class InventoryLockingService {
    
    private final DistributedLockService lockService;
    
    // Lock timeouts for inventory operations
    private static final Duration INVENTORY_LOCK_WAIT_TIME = Duration.ofSeconds(3);
    private static final Duration INVENTORY_LOCK_LEASE_TIME = Duration.ofSeconds(30);
    
    public InventoryLockingService(DistributedLockService lockService) {
        this.lockService = lockService;
        log.info("InventoryLockingService initialized with distributed locking support");
    }
    
    /**
     * Reserves inventory for a product with distributed locking.
     * This ensures that concurrent reservation requests are handled atomically.
     * 
     * @param productId the product to reserve inventory for
     * @param quantity the quantity to reserve
     * @return true if reservation was successful, false otherwise
     */
    public boolean reserveInventory(ProductId productId, int quantity) {
        String lockKey = DistributedLockService.LockKeys.inventoryOperation(productId.getId());
        
        log.debug("Attempting to reserve {} units of product {} with distributed lock", 
                 quantity, productId.getId());
        
        try {
            return lockService.executeWithLock(lockKey, () -> {
                // Simulate inventory reservation logic
                log.info("Reserving {} units of product {} (locked operation)", 
                        quantity, productId.getId());
                
                // In a real implementation, this would:
                // 1. Check current inventory levels
                // 2. Validate reservation request
                // 3. Update inventory records
                // 4. Create reservation record
                
                // Simulate some processing time
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Operation interrupted", e);
                }
                
                log.info("Successfully reserved {} units of product {}", 
                        quantity, productId.getId());
                return true;
                
            }, INVENTORY_LOCK_WAIT_TIME, INVENTORY_LOCK_LEASE_TIME);
            
        } catch (IllegalStateException e) {
            log.warn("Failed to acquire lock for inventory reservation of product {}: {}", 
                    productId.getId(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error during inventory reservation for product {}", 
                     productId.getId(), e);
            return false;
        }
    }
    
    /**
     * Releases reserved inventory with distributed locking.
     * 
     * @param productId the product to release inventory for
     * @param quantity the quantity to release
     * @return true if release was successful, false otherwise
     */
    public boolean releaseInventory(ProductId productId, int quantity) {
        String lockKey = DistributedLockService.LockKeys.inventoryOperation(productId.getId());
        
        log.debug("Attempting to release {} units of product {} with distributed lock", 
                 quantity, productId.getId());
        
        try {
            return lockService.executeWithLock(lockKey, () -> {
                log.info("Releasing {} units of product {} (locked operation)", 
                        quantity, productId.getId());
                
                // In a real implementation, this would:
                // 1. Validate release request
                // 2. Update inventory records
                // 3. Remove or update reservation record
                
                // Simulate some processing time
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Operation interrupted", e);
                }
                
                log.info("Successfully released {} units of product {}", 
                        quantity, productId.getId());
                return true;
                
            }, INVENTORY_LOCK_WAIT_TIME, INVENTORY_LOCK_LEASE_TIME);
            
        } catch (IllegalStateException e) {
            log.warn("Failed to acquire lock for inventory release of product {}: {}", 
                    productId.getId(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error during inventory release for product {}", 
                     productId.getId(), e);
            return false;
        }
    }
    
    /**
     * Attempts to reserve inventory without waiting if lock is not available.
     * 
     * @param productId the product to reserve inventory for
     * @param quantity the quantity to reserve
     * @return true if reservation was successful, null if lock could not be acquired, false on error
     */
    public Boolean tryReserveInventory(ProductId productId, int quantity) {
        String lockKey = DistributedLockService.LockKeys.inventoryOperation(productId.getId());
        
        log.debug("Attempting non-blocking inventory reservation for {} units of product {}", 
                 quantity, productId.getId());
        
        try {
            return lockService.tryExecuteWithLock(lockKey, () -> {
                log.info("Non-blocking reservation of {} units of product {}", 
                        quantity, productId.getId());
                
                // Simulate reservation logic (simplified)
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Operation interrupted", e);
                }
                
                return true;
                
            }, Duration.ofMillis(100), INVENTORY_LOCK_LEASE_TIME);
            
        } catch (Exception e) {
            log.error("Error during non-blocking inventory reservation for product {}", 
                     productId.getId(), e);
            return false;
        }
    }
    
    /**
     * Checks if inventory operations are currently locked for a product.
     * 
     * @param productId the product to check
     * @return true if locked, false otherwise
     */
    public boolean isInventoryLocked(ProductId productId) {
        String lockKey = DistributedLockService.LockKeys.inventoryOperation(productId.getId());
        return lockService.isLocked(lockKey);
    }
    
    /**
     * Forces the release of an inventory lock (use with extreme caution).
     * 
     * @param productId the product to force unlock
     */
    public void forceUnlockInventory(ProductId productId) {
        String lockKey = DistributedLockService.LockKeys.inventoryOperation(productId.getId());
        log.warn("Force unlocking inventory for product: {}", productId.getId());
        lockService.forceUnlock(lockKey);
    }
}