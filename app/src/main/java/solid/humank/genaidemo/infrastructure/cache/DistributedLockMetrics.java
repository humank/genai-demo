package solid.humank.genaidemo.infrastructure.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Metrics collection for Distributed Lock Service
 * 
 * This component collects and exports metrics for distributed lock operations
 * to support monitoring and alerting in cross-region deployments.
 * 
 * Metrics collected:
 * - Lock acquisition success/failure rates
 * - Lock acquisition time
 * - Lock operation duration
 * - Lock release success/failure rates
 * - Lock hold time
 * 
 * Requirements: 4.1.4 - Cross-region cache synchronization monitoring
 * 
 * @author Development Team
 * @since 2.0.0
 */
@Component
public class DistributedLockMetrics {

    private final Counter lockAcquisitionSuccessCounter;
    private final Counter lockAcquisitionFailureCounter;
    private final Counter lockOperationSuccessCounter;
    private final Counter lockOperationFailureCounter;
    private final Counter lockReleaseSuccessCounter;
    private final Counter lockReleaseFailureCounter;
    
    private final Timer lockAcquisitionTimer;
    private final Timer lockOperationTimer;
    private final Timer lockHoldTimer;

    public DistributedLockMetrics(MeterRegistry meterRegistry) {
        // Success counters
        this.lockAcquisitionSuccessCounter = Counter.builder("distributed.lock.acquisition.success")
            .description("Number of successful lock acquisitions")
            .register(meterRegistry);
            
        this.lockOperationSuccessCounter = Counter.builder("distributed.lock.operation.success")
            .description("Number of successful operations under lock")
            .register(meterRegistry);
            
        this.lockReleaseSuccessCounter = Counter.builder("distributed.lock.release.success")
            .description("Number of successful lock releases")
            .register(meterRegistry);

        // Failure counters
        this.lockAcquisitionFailureCounter = Counter.builder("distributed.lock.acquisition.failure")
            .description("Number of failed lock acquisitions")
            .register(meterRegistry);
            
        this.lockOperationFailureCounter = Counter.builder("distributed.lock.operation.failure")
            .description("Number of failed operations under lock")
            .register(meterRegistry);
            
        this.lockReleaseFailureCounter = Counter.builder("distributed.lock.release.failure")
            .description("Number of failed lock releases")
            .register(meterRegistry);

        // Timers
        this.lockAcquisitionTimer = Timer.builder("distributed.lock.acquisition.time")
            .description("Time taken to acquire a distributed lock")
            .register(meterRegistry);
            
        this.lockOperationTimer = Timer.builder("distributed.lock.operation.time")
            .description("Time taken to execute operation under lock")
            .register(meterRegistry);
            
        this.lockHoldTimer = Timer.builder("distributed.lock.hold.time")
            .description("Total time a lock was held")
            .register(meterRegistry);
    }

    /**
     * Record successful lock acquisition
     * 
     * @param lockKey the lock key
     * @param acquisitionTimeMs time taken to acquire lock in milliseconds
     */
    public void recordLockAcquisitionSuccess(String lockKey, long acquisitionTimeMs) {
        Tags tags = Tags.of(
            "lock_key", sanitizeLockKey(lockKey),
            "result", "success"
        );
        
        lockAcquisitionSuccessCounter.increment();
        lockAcquisitionTimer.record(acquisitionTimeMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record failed lock acquisition
     * 
     * @param lockKey the lock key
     * @param reason failure reason (e.g., TIMEOUT, INTERRUPTED)
     */
    public void recordLockAcquisitionFailure(String lockKey, String reason) {
        Tags tags = Tags.of(
            "lock_key", sanitizeLockKey(lockKey),
            "result", "failure",
            "reason", reason
        );
        
        lockAcquisitionFailureCounter.increment();
    }

    /**
     * Record successful operation under lock
     * 
     * @param lockKey the lock key
     * @param operationTimeMs time taken to execute operation in milliseconds
     */
    public void recordLockOperationSuccess(String lockKey, long operationTimeMs) {
        Tags tags = Tags.of(
            "lock_key", sanitizeLockKey(lockKey),
            "result", "success"
        );
        
        lockOperationSuccessCounter.increment();
        lockOperationTimer.record(operationTimeMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record failed operation under lock
     * 
     * @param lockKey the lock key
     * @param errorType type of error that occurred
     */
    public void recordLockOperationFailure(String lockKey, String errorType) {
        Tags tags = Tags.of(
            "lock_key", sanitizeLockKey(lockKey),
            "result", "failure",
            "error_type", errorType
        );
        
        lockOperationFailureCounter.increment();
    }

    /**
     * Record successful lock release
     * 
     * @param lockKey the lock key
     * @param holdTimeMs total time the lock was held in milliseconds
     */
    public void recordLockRelease(String lockKey, long holdTimeMs) {
        Tags tags = Tags.of(
            "lock_key", sanitizeLockKey(lockKey),
            "result", "success"
        );
        
        lockReleaseSuccessCounter.increment();
        lockHoldTimer.record(holdTimeMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record failed lock release
     * 
     * @param lockKey the lock key
     * @param errorType type of error that occurred
     */
    public void recordLockReleaseFailure(String lockKey, String errorType) {
        Tags tags = Tags.of(
            "lock_key", sanitizeLockKey(lockKey),
            "result", "failure",
            "error_type", errorType
        );
        
        lockReleaseFailureCounter.increment();
    }

    /**
     * Sanitize lock key for use in metrics tags
     * Removes sensitive information and limits length
     * 
     * @param lockKey original lock key
     * @return sanitized lock key
     */
    private String sanitizeLockKey(String lockKey) {
        if (lockKey == null) {
            return "unknown";
        }
        
        // Remove the distributed-lock prefix if present
        String sanitized = lockKey.startsWith("distributed-lock:") 
            ? lockKey.substring("distributed-lock:".length()) 
            : lockKey;
        
        // Extract the main category/type from the key
        // e.g., "customer:123:update" -> "customer:update"
        String[] parts = sanitized.split(":");
        if (parts.length >= 3) {
            // Keep first and last parts, replace middle with placeholder
            sanitized = parts[0] + ":*:" + parts[parts.length - 1];
        } else if (parts.length == 2) {
            // Keep first part, replace second with placeholder if it looks like an ID
            if (parts[1].matches("\\d+") || parts[1].length() > 10) {
                sanitized = parts[0] + ":*";
            }
        }
        
        // Limit length to avoid high cardinality
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 47) + "...";
        }
        
        return sanitized;
    }
}