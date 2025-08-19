package solid.humank.genaidemo.testutils.isolation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 測試執行監控器
 * 監控測試隔離的執行狀況，提供統計和診斷資訊
 */
public class TestExecutionMonitor {

    private static final TestExecutionMonitor INSTANCE = new TestExecutionMonitor();

    private final AtomicInteger totalTests = new AtomicInteger(0);
    private final AtomicInteger successfulTests = new AtomicInteger(0);
    private final AtomicInteger failedTests = new AtomicInteger(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    private final AtomicLong totalCleanupTime = new AtomicLong(0);

    private final Map<String, TestExecutionRecord> executionRecords = new ConcurrentHashMap<>();
    private final Map<String, Integer> resourceUsageStats = new ConcurrentHashMap<>();

    private TestExecutionMonitor() {
        // Private constructor for singleton
    }

    public static TestExecutionMonitor getInstance() {
        return INSTANCE;
    }

    /**
     * 記錄測試開始
     */
    public void recordTestStart(String testId, String testName) {
        totalTests.incrementAndGet();
        TestExecutionRecord record = new TestExecutionRecord(testId, testName);
        executionRecords.put(testId, record);
    }

    /**
     * 記錄測試成功
     */
    public void recordTestSuccess(String testId, Duration executionTime, Duration cleanupTime) {
        successfulTests.incrementAndGet();
        totalExecutionTime.addAndGet(executionTime.toMillis());
        totalCleanupTime.addAndGet(cleanupTime.toMillis());

        TestExecutionRecord record = executionRecords.get(testId);
        if (record != null) {
            record.markSuccess(executionTime, cleanupTime);
        }
    }

    /**
     * 記錄測試失敗
     */
    public void recordTestFailure(String testId, Duration executionTime, String errorMessage) {
        failedTests.incrementAndGet();
        totalExecutionTime.addAndGet(executionTime.toMillis());

        TestExecutionRecord record = executionRecords.get(testId);
        if (record != null) {
            record.markFailure(executionTime, errorMessage);
        }
    }

    /**
     * 記錄資源使用
     */
    public void recordResourceUsage(String resourceType) {
        resourceUsageStats.merge(resourceType, 1, Integer::sum);
    }

    /**
     * 獲取執行統計
     */
    public TestExecutionStats getExecutionStats() {
        return new TestExecutionStats(
                totalTests.get(),
                successfulTests.get(),
                failedTests.get(),
                totalExecutionTime.get(),
                totalCleanupTime.get(),
                new ConcurrentHashMap<>(resourceUsageStats));
    }

    /**
     * 獲取特定測試的執行記錄
     */
    public TestExecutionRecord getExecutionRecord(String testId) {
        return executionRecords.get(testId);
    }

    /**
     * 清理監控資料
     */
    public void reset() {
        totalTests.set(0);
        successfulTests.set(0);
        failedTests.set(0);
        totalExecutionTime.set(0);
        totalCleanupTime.set(0);
        executionRecords.clear();
        resourceUsageStats.clear();
    }

    /**
     * 測試執行記錄
     */
    public static class TestExecutionRecord {
        private final String testId;
        private final String testName;
        private final LocalDateTime startTime;
        private LocalDateTime endTime;
        private Duration executionTime;
        private Duration cleanupTime;
        private boolean successful;
        private String errorMessage;

        public TestExecutionRecord(String testId, String testName) {
            this.testId = testId;
            this.testName = testName;
            this.startTime = LocalDateTime.now();
            this.successful = false;
        }

        public void markSuccess(Duration executionTime, Duration cleanupTime) {
            this.endTime = LocalDateTime.now();
            this.executionTime = executionTime;
            this.cleanupTime = cleanupTime;
            this.successful = true;
        }

        public void markFailure(Duration executionTime, String errorMessage) {
            this.endTime = LocalDateTime.now();
            this.executionTime = executionTime;
            this.errorMessage = errorMessage;
            this.successful = false;
        }

        // Getters
        public String getTestId() {
            return testId;
        }

        public String getTestName() {
            return testName;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public Duration getExecutionTime() {
            return executionTime;
        }

        public Duration getCleanupTime() {
            return cleanupTime;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * 測試執行統計
     */
    public static class TestExecutionStats {
        private final int totalTests;
        private final int successfulTests;
        private final int failedTests;
        private final long totalExecutionTimeMs;
        private final long totalCleanupTimeMs;
        private final Map<String, Integer> resourceUsage;

        public TestExecutionStats(int totalTests, int successfulTests, int failedTests,
                long totalExecutionTimeMs, long totalCleanupTimeMs,
                Map<String, Integer> resourceUsage) {
            this.totalTests = totalTests;
            this.successfulTests = successfulTests;
            this.failedTests = failedTests;
            this.totalExecutionTimeMs = totalExecutionTimeMs;
            this.totalCleanupTimeMs = totalCleanupTimeMs;
            this.resourceUsage = resourceUsage;
        }

        public double getSuccessRate() {
            return totalTests > 0 ? (double) successfulTests / totalTests : 0.0;
        }

        public double getAverageExecutionTimeMs() {
            return totalTests > 0 ? (double) totalExecutionTimeMs / totalTests : 0.0;
        }

        public double getAverageCleanupTimeMs() {
            return successfulTests > 0 ? (double) totalCleanupTimeMs / successfulTests : 0.0;
        }

        // Getters
        public int getTotalTests() {
            return totalTests;
        }

        public int getSuccessfulTests() {
            return successfulTests;
        }

        public int getFailedTests() {
            return failedTests;
        }

        public long getTotalExecutionTimeMs() {
            return totalExecutionTimeMs;
        }

        public long getTotalCleanupTimeMs() {
            return totalCleanupTimeMs;
        }

        public Map<String, Integer> getResourceUsage() {
            return resourceUsage;
        }

        @Override
        public String toString() {
            return String.format(
                    "TestExecutionStats{totalTests=%d, successfulTests=%d, failedTests=%d, " +
                            "successRate=%.2f%%, avgExecutionTime=%.2fms, avgCleanupTime=%.2fms}",
                    totalTests, successfulTests, failedTests, getSuccessRate() * 100,
                    getAverageExecutionTimeMs(), getAverageCleanupTimeMs());
        }
    }
}