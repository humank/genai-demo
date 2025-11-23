package solid.humank.genaidemo.testutils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestComponent;

/**
 * Test Resource Manager for monitoring and optimizing memory usage during test
 * execution.
 * 
 * This component provides:
 * - Memory usage monitoring and optimization
 * - Automatic cleanup mechanisms for test resources
 * - Resource usage statistics and reporting
 * - Memory threshold validation
 * 
 * Requirements: 5.1, 5.2, 5.4
 */
@TestComponent
public class TestResourceManager {    private static final Logger logger = LoggerFactory.getLogger(TestResourceManager.class);

    private final MemoryMXBean memoryBean;
    private final AtomicInteger activeTestResources;
    private final AtomicLong totalMemoryAllocated;
    private final AtomicLong peakMemoryUsage;

    // Memory thresholds
    private static final double MEMORY_WARNING_THRESHOLD = 0.8; // 80%
    private static final double MEMORY_CRITICAL_THRESHOLD = 0.9; // 90%
    private static final long MEMORY_CLEANUP_THRESHOLD_MB = 100; // 100MB increase

    // Cleanup tracking
    private Instant lastCleanupTime;
    private long cleanupCount = 0;

    public TestResourceManager() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.activeTestResources = new AtomicInteger(0);
        this.totalMemoryAllocated = new AtomicLong(0);
        this.peakMemoryUsage = new AtomicLong(0);
        this.lastCleanupTime = Instant.now();

        logger.info("TestResourceManager initialized - Initial memory usage: {} MB",
                getCurrentMemoryUsageMB());
    }

    /**
     * Get current resource usage statistics.
     * 
     * @return ResourceUsageStats containing current usage information
     */
    public ResourceUsageStats getResourceUsageStats() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long currentMemoryMB = heapUsage.getUsed() / (1024 * 1024);
        long maxMemoryMB = heapUsage.getMax() / (1024 * 1024);
        double memoryUsagePercentage = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;

        // Update peak memory usage
        peakMemoryUsage.updateAndGet(current -> Math.max(current, currentMemoryMB));

        return new ResourceUsageStats(
                activeTestResources.get(),
                currentMemoryMB,
                maxMemoryMB,
                memoryUsagePercentage,
                totalMemoryAllocated.get(),
                peakMemoryUsage.get(),
                cleanupCount,
                lastCleanupTime);
    }

    /**
     * Monitor memory usage during test execution.
     * 
     * @param testName Name of the test being monitored
     * @return MemoryMonitoringSession for tracking memory usage
     */
    public MemoryMonitoringSession monitorMemoryUsage(String testName) {
        activeTestResources.incrementAndGet();
        MemoryUsage initialMemory = memoryBean.getHeapMemoryUsage();

        logger.debug("Starting memory monitoring for test: {} - Initial memory: {} MB",
                testName, initialMemory.getUsed() / (1024 * 1024));

        return new MemoryMonitoringSession(testName, initialMemory, Instant.now());
    }

    /**
     * Check if sufficient memory is available for test execution.
     * 
     * @param requiredMemoryMB Required memory in MB
     * @return true if sufficient memory is available
     */
    public boolean isMemoryAvailable(long requiredMemoryMB) {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long availableMemoryMB = (heapUsage.getMax() - heapUsage.getUsed()) / (1024 * 1024);

        boolean isAvailable = availableMemoryMB >= requiredMemoryMB;

        if (!isAvailable) {
            logger.warn("Insufficient memory available. Required: {} MB, Available: {} MB",
                    requiredMemoryMB, availableMemoryMB);
        }

        return isAvailable;
    }

    /**
     * Check if current memory usage is within acceptable limits.
     * 
     * @return true if memory usage is acceptable
     */
    public boolean isMemoryUsageAcceptable() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        double usagePercentage = (double) heapUsage.getUsed() / heapUsage.getMax();

        if (usagePercentage > MEMORY_CRITICAL_THRESHOLD) {
            logger.error("CRITICAL: Memory usage at {:.1f}% - Immediate cleanup required",
                    usagePercentage * 100);
            return false;
        } else if (usagePercentage > MEMORY_WARNING_THRESHOLD) {
            logger.warn("WARNING: Memory usage at {:.1f}% - Consider cleanup",
                    usagePercentage * 100);
            return false;
        }

        return true;
    }

    /**
     * Force cleanup of all test resources and trigger garbage collection.
     */
    public void forceCleanup() {
        logger.info("Forcing cleanup of test resources - Current memory usage: {} MB",
                getCurrentMemoryUsageMB());

        long memoryBeforeCleanup = getCurrentMemoryUsageMB();

        // Reset resource counters
        activeTestResources.set(0);

        // Force garbage collection
        System.gc();

        // Wait a bit for GC to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long memoryAfterCleanup = getCurrentMemoryUsageMB();
        long memoryFreed = memoryBeforeCleanup - memoryAfterCleanup;

        cleanupCount++;
        lastCleanupTime = Instant.now();

        logger.info("Cleanup completed - Memory freed: {} MB, Current usage: {} MB",
                memoryFreed, memoryAfterCleanup);
    }

    /**
     * Optimize memory usage for test execution.
     * 
     * @param testContext Context information about the test
     */
    public void optimizeForTest(TestContext testContext) {
        logger.debug("Optimizing memory for test: {}", testContext.getTestName());

        // Check if cleanup is needed based on memory usage
        if (!isMemoryUsageAcceptable()) {
            logger.info("Memory usage high, performing cleanup before test: {}",
                    testContext.getTestName());
            forceCleanup();
        }

        // Check if cleanup is needed based on memory increase
        long currentMemory = getCurrentMemoryUsageMB();
        long memoryIncrease = currentMemory - (totalMemoryAllocated.get() / (1024 * 1024));

        if (memoryIncrease > MEMORY_CLEANUP_THRESHOLD_MB) {
            logger.info("Memory increase of {} MB detected, performing cleanup", memoryIncrease);
            forceCleanup();
        }

        // Update total memory allocated
        totalMemoryAllocated.set(currentMemory * 1024 * 1024);
    }

    /**
     * Get current memory usage in MB.
     */
    private long getCurrentMemoryUsageMB() {
        return memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
    }

    /**
     * Memory monitoring session for tracking memory usage during test execution.
     */
    public class MemoryMonitoringSession implements AutoCloseable {
        private final String testName;
        private final MemoryUsage initialMemory;
        private final Instant startTime;

        public MemoryMonitoringSession(String testName, MemoryUsage initialMemory, Instant startTime) {
            this.testName = testName;
            this.initialMemory = initialMemory;
            this.startTime = startTime;
        }

        @Override
        public void close() {
            activeTestResources.decrementAndGet();

            MemoryUsage finalMemory = memoryBean.getHeapMemoryUsage();
            long memoryDeltaMB = (finalMemory.getUsed() - initialMemory.getUsed()) / (1024 * 1024);
            Duration executionTime = Duration.between(startTime, Instant.now());

            logger.debug("Memory monitoring completed for test: {} - Memory delta: {} MB, Duration: {} ms",
                    testName, memoryDeltaMB, executionTime.toMillis());

            // Check if memory usage is concerning
            if (Math.abs(memoryDeltaMB) > MEMORY_CLEANUP_THRESHOLD_MB) {
                logger.warn("High memory usage detected in test: {} - Memory delta: {} MB",
                        testName, memoryDeltaMB);
            }
        }

        public long getMemoryDeltaMB() {
            MemoryUsage currentMemory = memoryBean.getHeapMemoryUsage();
            return (currentMemory.getUsed() - initialMemory.getUsed()) / (1024 * 1024);
        }

        public Duration getExecutionTime() {
            return Duration.between(startTime, Instant.now());
        }
    }

    /**
     * Resource usage statistics holder.
     */
    public static class ResourceUsageStats {
        private final int activeTestResources;
        private final long currentMemoryMB;
        private final long maxMemoryMB;
        private final double memoryUsagePercentage;
        private final long totalMemoryAllocated;
        private final long peakMemoryUsage;
        private final long cleanupCount;
        private final Instant lastCleanupTime;

        public ResourceUsageStats(int activeTestResources, long currentMemoryMB, long maxMemoryMB,
                double memoryUsagePercentage, long totalMemoryAllocated, long peakMemoryUsage,
                long cleanupCount, Instant lastCleanupTime) {
            this.activeTestResources = activeTestResources;
            this.currentMemoryMB = currentMemoryMB;
            this.maxMemoryMB = maxMemoryMB;
            this.memoryUsagePercentage = memoryUsagePercentage;
            this.totalMemoryAllocated = totalMemoryAllocated;
            this.peakMemoryUsage = peakMemoryUsage;
            this.cleanupCount = cleanupCount;
            this.lastCleanupTime = lastCleanupTime;
        }

        // Getters
        public int getActiveTestResources() {
            return activeTestResources;
        }

        public long getCurrentMemoryMB() {
            return currentMemoryMB;
        }

        public long getMaxMemoryMB() {
            return maxMemoryMB;
        }

        public double getMemoryUsagePercentage() {
            return memoryUsagePercentage;
        }

        public long getTotalMemoryAllocated() {
            return totalMemoryAllocated;
        }

        public long getPeakMemoryUsage() {
            return peakMemoryUsage;
        }

        public long getCleanupCount() {
            return cleanupCount;
        }

        public Instant getLastCleanupTime() {
            return lastCleanupTime;
        }

        @Override
        public String toString() {
            return String.format(
                    "ResourceUsageStats{activeResources=%d, memory=%d/%d MB (%.1f%%), peak=%d MB, cleanups=%d}",
                    activeTestResources, currentMemoryMB, maxMemoryMB, memoryUsagePercentage,
                    peakMemoryUsage, cleanupCount);
        }
    }

    /**
     * Test context information for optimization.
     */
    public static class TestContext {
        private final String testName;
        private final String testClass;
        private final boolean isMemoryIntensive;
        private final long estimatedMemoryRequirementMB;

        public TestContext(String testName, String testClass, boolean isMemoryIntensive,
                long estimatedMemoryRequirementMB) {
            this.testName = testName;
            this.testClass = testClass;
            this.isMemoryIntensive = isMemoryIntensive;
            this.estimatedMemoryRequirementMB = estimatedMemoryRequirementMB;
        }

        // Getters
        public String getTestName() {
            return testName;
        }

        public String getTestClass() {
            return testClass;
        }

        public boolean isMemoryIntensive() {
            return isMemoryIntensive;
        }

        public long getEstimatedMemoryRequirementMB() {
            return estimatedMemoryRequirementMB;
        }
    }
}