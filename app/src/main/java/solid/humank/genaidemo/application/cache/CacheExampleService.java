package solid.humank.genaidemo.application.cache;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import solid.humank.genaidemo.application.common.DistributedLockService;
import solid.humank.genaidemo.infrastructure.cache.CrossRegionCacheService;

/**
 * Example service demonstrating cross-region cache and distributed lock usage
 *
 * This service provides examples of how to use the cross-region cache
 * and distributed locking services in application code.
 *
 * Requirements: 4.1.4 - Cross-region cache synchronization
 *
 * @author Development Team
 * @since 2.0.0
 */
@Service
public class CacheExampleService {
    private static final Logger logger = LoggerFactory.getLogger(CacheExampleService.class);

    // String constants
    private static final String CACHE_KEY_PREFIX = "user:";

    private final CrossRegionCacheService cacheService;
    private final DistributedLockService lockService;

    public CacheExampleService(
            CrossRegionCacheService cacheService,
            DistributedLockService lockService) {
        this.cacheService = cacheService;
        this.lockService = lockService;
    }

    /**
     * Example: Cache customer data with automatic fallback to database
     */
    public Optional<CustomerData> getCustomerData(String customerId) {
        String cacheKey = CACHE_KEY_PREFIX + customerId;

        return cacheService.get(
                cacheKey,
                CustomerData.class,
                () -> loadCustomerFromDatabase(customerId),
                Duration.ofMinutes(30));
    }

    /**
     * Example: Update customer data with distributed lock to prevent race
     * conditions
     */
    public void updateCustomerData(String customerId, CustomerData newData) {
        String lockKey = "customer:update:" + customerId;

        lockService.executeWithLock(lockKey, () -> {
            // Critical section - only one thread/instance can execute this at a time
            logger.info("Updating customer data for: {}", customerId);

            // Update in database
            saveCustomerToDatabase(customerId, newData);

            // Invalidate cache across all regions
            String cacheKey = CACHE_KEY_PREFIX + customerId;
            cacheService.invalidate(cacheKey);

            logger.info("Customer data updated and cache invalidated for: {}", customerId);
            return null; // Return null for Supplier<T>
        });
    }

    /**
     * Example: Bulk cache invalidation with pattern
     */
    public void invalidateCustomerCaches(String customerIdPattern) {
        String lockKey = "customer:bulk-invalidation";

        lockService.executeWithLock(lockKey, () -> {
            String cachePattern = CACHE_KEY_PREFIX + customerIdPattern;
            cacheService.invalidatePattern(cachePattern);
            logger.info("Invalidated customer caches matching pattern: {}", cachePattern);
            return null; // Return null for Supplier<T>
        }, Duration.ofSeconds(10), Duration.ofSeconds(60)); // Custom timeouts: 10s wait, 60s lease
    }

    /**
     * Example: Manual lock management for complex operations
     */
    public void performComplexOperation(String operationId) {
        String lockKey = "complex-operation:" + operationId;

        try {
            lockService.executeWithLock(lockKey, () -> {
                logger.info("Acquired lock for complex operation: {}", operationId);

                // Perform complex multi-step operation
                performStep1(operationId);
                performStep2(operationId);
                performStep3(operationId);

                logger.info("Complex operation completed: {}", operationId);
                return null;
            }, Duration.ofSeconds(5), Duration.ofSeconds(30));
        } catch (IllegalStateException e) {
            logger.warn("Could not acquire lock for operation: {}", operationId);
            throw new IllegalStateException("Operation already in progress: " + operationId, e);
        }
    }

    /**
     * Example: Check if operation is already running
     */
    public boolean isOperationRunning(String operationId) {
        String lockKey = "complex-operation:" + operationId;
        return lockService.isLocked(lockKey);
    }

    // Mock database operations for examples
    private Optional<CustomerData> loadCustomerFromDatabase(String customerId) {
        logger.debug("Loading customer from database: {}", customerId);
        // Simulate database call
        return Optional
                .of(new CustomerData(customerId, "Customer " + customerId, "customer" + customerId + "@example.com"));
    }

    private void saveCustomerToDatabase(String customerId, CustomerData data) {
        logger.debug("Saving customer to database: {}", customerId);
        // Simulate database save
    }

    private void performStep1(String operationId) {
        logger.debug("Performing step 1 for operation: {}", operationId);
        // Simulate work
    }

    private void performStep2(String operationId) {
        logger.debug("Performing step 2 for operation: {}", operationId);
        // Simulate work
    }

    private void performStep3(String operationId) {
        logger.debug("Performing step 3 for operation: {}", operationId);
        // Simulate work
    }

    /**
     * Simple data class for cache examples
     */
    public static class CustomerData {
        private String id;
        private String name;
        private String email;

        public CustomerData() {
        }

        public CustomerData(String id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public String toString() {
            return "CustomerData{id='" + id + "', name='" + name + "', email='" + email + "'}";
        }
    }
}
