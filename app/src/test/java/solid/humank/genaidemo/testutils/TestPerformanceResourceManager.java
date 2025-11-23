package solid.humank.genaidemo.testutils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestComponent;

/**
 * Test Performance Resource Manager for basic resource monitoring.
 * 
 * This component provides performance-focused resource monitoring
 * and is distinct from the TestProfileConfiguration.TestResourceManager
 * which handles general test resource allocation.
 * 
 * Requirements: 5.3, 5.5, 7.5
 */
@TestComponent
public class TestPerformanceResourceManager {    private static final Logger logger = LoggerFactory.getLogger(TestPerformanceResourceManager.class);
    private final MemoryMXBean memoryBean;

    public TestPerformanceResourceManager() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        logger.info("TestPerformanceResourceManager initialized");
    }

    /**
     * Get resource usage statistics.
     */
    public ResourceUsageStats getResourceUsageStats() {
        MemoryUsage currentMemory = memoryBean.getHeapMemoryUsage();

        return new ResourceUsageStats(
                0L, // totalTestsExecuted
                currentMemory.getUsed(),
                currentMemory.getMax(),
                0L, // totalMemoryAllocated
                0 // activeTestResources
        );
    }

    /**
     * Force cleanup of all test resources.
     */
    public void forceCleanup() {
        logger.info("Force cleanup of all test resources");
        System.gc();
        logger.info("Force cleanup completed");
    }

    /**
     * Resource usage statistics.
     */
    public static class ResourceUsageStats {
        private final long totalTestsExecuted;
        private final long currentMemoryUsed;
        private final long maxMemoryAvailable;
        private final long totalMemoryAllocated;
        private final int activeTestResources;

        public ResourceUsageStats(long totalTestsExecuted, long currentMemoryUsed,
                long maxMemoryAvailable, long totalMemoryAllocated,
                int activeTestResources) {
            this.totalTestsExecuted = totalTestsExecuted;
            this.currentMemoryUsed = currentMemoryUsed;
            this.maxMemoryAvailable = maxMemoryAvailable;
            this.totalMemoryAllocated = totalMemoryAllocated;
            this.activeTestResources = activeTestResources;
        }

        // Getters
        public long getTotalTestsExecuted() {
            return totalTestsExecuted;
        }

        public long getCurrentMemoryUsed() {
            return currentMemoryUsed;
        }

        public long getMaxMemoryAvailable() {
            return maxMemoryAvailable;
        }

        public long getTotalMemoryAllocated() {
            return totalMemoryAllocated;
        }

        public int getActiveTestResources() {
            return activeTestResources;
        }

        public double getMemoryUsagePercentage() {
            return maxMemoryAvailable > 0 ? (double) currentMemoryUsed / maxMemoryAvailable * 100 : 0;
        }

        @Override
        public String toString() {
            return String.format(
                    "ResourceUsageStats{tests=%d, memory=%.1f%% (%d/%d MB), allocated=%d MB, active=%d}",
                    totalTestsExecuted,
                    getMemoryUsagePercentage(),
                    currentMemoryUsed / (1024 * 1024),
                    maxMemoryAvailable / (1024 * 1024),
                    totalMemoryAllocated / (1024 * 1024),
                    activeTestResources);
        }
    }
}