package solid.humank.genaidemo.infrastructure.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

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
     * @param lockKey           the lock key
     * @param acquisitionTimeMs time taken to acquire lock in milliseconds
     */
    public void recordLockAcquisitionSuccess(String lockKey, long acquisitionTimeMs) {
        lockAcquisitionSuccessCounter.increment();
        lockAcquisitionTimer.record(acquisitionTimeMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record failed lock acquisition
     *
     * @param lockKey the lock key
     * @param reason  failure reason (e.g., TIMEOUT, INTERRUPTED)
     */
    public void recordLockAcquisitionFailure(String lockKey, String reason) {
        lockAcquisitionFailureCounter.increment();
    }

    /**
     * Record successful operation under lock
     *
     * @param lockKey         the lock key
     * @param operationTimeMs time taken to execute operation in milliseconds
     */
    public void recordLockOperationSuccess(String lockKey, long operationTimeMs) {
        lockOperationSuccessCounter.increment();
        lockOperationTimer.record(operationTimeMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record failed operation under lock
     *
     * @param lockKey   the lock key
     * @param errorType type of error that occurred
     */
    public void recordLockOperationFailure(String lockKey, String errorType) {
        lockOperationFailureCounter.increment();
    }

    /**
     * Record successful lock release
     *
     * @param lockKey    the lock key
     * @param holdTimeMs total time the lock was held in milliseconds
     */
    public void recordLockRelease(String lockKey, long holdTimeMs) {
        lockReleaseSuccessCounter.increment();
        lockHoldTimer.record(holdTimeMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record failed lock release
     *
     * @param lockKey   the lock key
     * @param errorType type of error that occurred
     */
    public void recordLockReleaseFailure(String lockKey, String errorType) {
        lockReleaseFailureCounter.increment();
    }
}
