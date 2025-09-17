package solid.humank.genaidemo.testutils;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test Performance Monitor for tracking test performance and resource usage.
 * 
 * This monitor provides:
 * - Test execution time monitoring
 * - Memory usage tracking during tests
 * - Detailed test execution reports
 * - Performance regression detection
 * 
 * This is distinct from the TestExecutionMonitor in the isolation package
 * which focuses on test isolation monitoring.
 * 
 * Requirements: 5.1, 5.4
 */
public class TestPerformanceMonitor implements BeforeAllCallback, AfterAllCallback,
        BeforeEachCallback, AfterEachCallback, TestWatcher {

    private static final Logger logger = LoggerFactory.getLogger(TestPerformanceMonitor.class);

    private final MemoryMXBean memoryBean;
    private final ConcurrentMap<String, TestExecutionInfo> testExecutions;
    private final ConcurrentMap<String, ClassExecutionInfo> classExecutions;
    private final AtomicLong totalTestsExecuted;
    private final AtomicLong totalFailedTests;
    private final AtomicLong totalExecutionTime;

    // Performance thresholds
    private static final long SLOW_TEST_THRESHOLD_MS = 5000; // 5 seconds
    private static final long VERY_SLOW_TEST_THRESHOLD_MS = 30000; // 30 seconds
    private static final double MEMORY_INCREASE_THRESHOLD = 50.0; // 50MB

    public TestPerformanceMonitor() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.testExecutions = new ConcurrentHashMap<>();
        this.classExecutions = new ConcurrentHashMap<>();
        this.totalTestsExecuted = new AtomicLong(0);
        this.totalFailedTests = new AtomicLong(0);
        this.totalExecutionTime = new AtomicLong(0);

        logger.info("TestPerformanceMonitor initialized");
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        String className = context.getRequiredTestClass().getSimpleName();
        logger.info("Starting test class: {}", className);

        ClassExecutionInfo classInfo = new ClassExecutionInfo(
                className,
                Instant.now(),
                getCurrentMemoryUsage());

        classExecutions.put(className, classInfo);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        String className = context.getRequiredTestClass().getSimpleName();
        logger.info("Completed test class: {}", className);

        ClassExecutionInfo classInfo = classExecutions.get(className);
        if (classInfo != null) {
            classInfo.setEndTime(Instant.now());
            classInfo.setEndMemoryUsage(getCurrentMemoryUsage());

            logClassExecutionSummary(classInfo);
            generateClassReport(classInfo);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        String testName = getFullTestName(context);
        logger.debug("Starting test: {}", testName);

        TestExecutionInfo testInfo = new TestExecutionInfo(
                testName,
                context.getRequiredTestClass().getSimpleName(),
                context.getRequiredTestMethod().getName(),
                Instant.now(),
                getCurrentMemoryUsage());

        testExecutions.put(testName, testInfo);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        String testName = getFullTestName(context);
        logger.debug("Completed test: {}", testName);

        TestExecutionInfo testInfo = testExecutions.get(testName);
        if (testInfo != null) {
            testInfo.setEndTime(Instant.now());
            testInfo.setEndMemoryUsage(getCurrentMemoryUsage());

            long executionTimeMs = testInfo.getExecutionTimeMs();
            totalTestsExecuted.incrementAndGet();
            totalExecutionTime.addAndGet(executionTimeMs);

            logTestExecutionSummary(testInfo);
            checkPerformanceRegression(testInfo);
        }
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        String testName = getFullTestName(context);
        TestExecutionInfo testInfo = testExecutions.get(testName);
        if (testInfo != null) {
            testInfo.setStatus(TestStatus.PASSED);
            logger.debug("Test passed: {}", testName);
        }
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String testName = getFullTestName(context);
        TestExecutionInfo testInfo = testExecutions.get(testName);
        if (testInfo != null) {
            testInfo.setStatus(TestStatus.FAILED);
            testInfo.setFailureCause(cause.getMessage());
            totalFailedTests.incrementAndGet();
            logger.error("Test failed: {} - Cause: {}", testName, cause.getMessage());
        }
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        String testName = getFullTestName(context);
        TestExecutionInfo testInfo = testExecutions.get(testName);
        if (testInfo != null) {
            testInfo.setStatus(TestStatus.ABORTED);
            testInfo.setFailureCause(cause.getMessage());
            logger.warn("Test aborted: {} - Cause: {}", testName, cause.getMessage());
        }
    }

    @Override
    public void testDisabled(ExtensionContext context, java.util.Optional<String> reason) {
        String testName = getFullTestName(context);
        logger.info("Test disabled: {} - Reason: {}", testName, reason.orElse("No reason provided"));
    }

    /**
     * Get current memory usage.
     */
    private MemoryUsage getCurrentMemoryUsage() {
        return memoryBean.getHeapMemoryUsage();
    }

    /**
     * Get full test name including class and method.
     */
    private String getFullTestName(ExtensionContext context) {
        return context.getRequiredTestClass().getSimpleName() + "." +
                context.getRequiredTestMethod().getName();
    }

    /**
     * Log test execution summary.
     */
    private void logTestExecutionSummary(TestExecutionInfo testInfo) {
        long executionTimeMs = testInfo.getExecutionTimeMs();
        long memoryDeltaMB = testInfo.getMemoryDeltaMB();

        if (executionTimeMs > VERY_SLOW_TEST_THRESHOLD_MS) {
            logger.error("VERY SLOW TEST: {} took {} ms (Memory: {} MB)",
                    testInfo.getTestName(), executionTimeMs, memoryDeltaMB);
        } else if (executionTimeMs > SLOW_TEST_THRESHOLD_MS) {
            logger.warn("SLOW TEST: {} took {} ms (Memory: {} MB)",
                    testInfo.getTestName(), executionTimeMs, memoryDeltaMB);
        } else {
            logger.debug("Test completed: {} - {} ms (Memory: {} MB)",
                    testInfo.getTestName(), executionTimeMs, memoryDeltaMB);
        }

        if (Math.abs(memoryDeltaMB) > MEMORY_INCREASE_THRESHOLD) {
            logger.warn("HIGH MEMORY USAGE: {} used {} MB memory",
                    testInfo.getTestName(), memoryDeltaMB);
        }
    }

    /**
     * Log class execution summary.
     */
    private void logClassExecutionSummary(ClassExecutionInfo classInfo) {
        Duration executionTime = Duration.between(classInfo.getStartTime(), classInfo.getEndTime());
        long memoryDeltaMB = classInfo.getMemoryDeltaMB();

        logger.info("Test class '{}' completed - Total time: {} ms, Memory delta: {} MB",
                classInfo.getClassName(),
                executionTime.toMillis(),
                memoryDeltaMB);
    }

    /**
     * Check for performance regression.
     */
    private void checkPerformanceRegression(TestExecutionInfo testInfo) {
        long executionTimeMs = testInfo.getExecutionTimeMs();

        // Simple regression detection - could be enhanced with historical data
        if (executionTimeMs > SLOW_TEST_THRESHOLD_MS) {
            logger.warn("PERFORMANCE REGRESSION DETECTED: {} is slower than expected ({} ms)",
                    testInfo.getTestName(), executionTimeMs);
        }
    }

    /**
     * Generate detailed report for test class.
     */
    private void generateClassReport(ClassExecutionInfo classInfo) {
        try {
            String reportFileName = "build/reports/test-performance/" +
                    classInfo.getClassName() + "-performance-report.txt";

            // Create directory if it doesn't exist
            java.nio.file.Path reportPath = java.nio.file.Paths.get(reportFileName);
            java.nio.file.Files.createDirectories(reportPath.getParent());

            try (FileWriter writer = new FileWriter(reportFileName)) {
                writer.write("Test Performance Report\n");
                writer.write("=======================\n\n");
                writer.write("Test Class: " + classInfo.getClassName() + "\n");
                writer.write("Start Time: " + classInfo.getStartTime() + "\n");
                writer.write("End Time: " + classInfo.getEndTime() + "\n");
                writer.write("Total Duration: "
                        + Duration.between(classInfo.getStartTime(), classInfo.getEndTime()).toMillis() + " ms\n");
                writer.write("Memory Delta: " + classInfo.getMemoryDeltaMB() + " MB\n\n");

                writer.write("Individual Test Results:\n");
                writer.write("========================\n");

                testExecutions.values().stream()
                        .filter(test -> test.getClassName().equals(classInfo.getClassName()))
                        .forEach(test -> {
                            try {
                                writer.write(String.format("- %s: %s (%d ms, %d MB)\n",
                                        test.getMethodName(),
                                        test.getStatus(),
                                        test.getExecutionTimeMs(),
                                        test.getMemoryDeltaMB()));

                                if (test.getFailureCause() != null) {
                                    writer.write("  Failure: " + test.getFailureCause() + "\n");
                                }
                            } catch (IOException e) {
                                logger.error("Failed to write test result to report", e);
                            }
                        });

                writer.write("\nSummary Statistics:\n");
                writer.write("==================\n");
                writer.write("Total Tests Executed: " + totalTestsExecuted.get() + "\n");
                writer.write("Total Failed Tests: " + totalFailedTests.get() + "\n");
                writer.write("Total Execution Time: " + totalExecutionTime.get() + " ms\n");
                writer.write("Average Test Time: "
                        + (totalTestsExecuted.get() > 0 ? totalExecutionTime.get() / totalTestsExecuted.get() : 0)
                        + " ms\n");
            }

            logger.info("Test performance report generated: {}", reportFileName);

        } catch (IOException e) {
            logger.error("Failed to generate test performance report", e);
        }
    }

    /**
     * Generate overall execution summary.
     */
    public void generateOverallSummary() {
        try {
            String summaryFileName = "build/reports/test-performance/overall-performance-summary.txt";

            // Create directory if it doesn't exist
            java.nio.file.Path summaryPath = java.nio.file.Paths.get(summaryFileName);
            java.nio.file.Files.createDirectories(summaryPath.getParent());

            try (FileWriter writer = new FileWriter(summaryFileName)) {
                writer.write("Overall Test Performance Summary\n");
                writer.write("================================\n\n");
                writer.write(
                        "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n\n");

                writer.write("Statistics:\n");
                writer.write("-----------\n");
                writer.write("Total Tests Executed: " + totalTestsExecuted.get() + "\n");
                writer.write("Total Failed Tests: " + totalFailedTests.get() + "\n");
                writer.write("Success Rate: " + String.format("%.2f%%",
                        totalTestsExecuted.get() > 0 ? (double) (totalTestsExecuted.get() - totalFailedTests.get())
                                / totalTestsExecuted.get() * 100 : 0)
                        + "\n");
                writer.write("Total Execution Time: " + totalExecutionTime.get() + " ms\n");
                writer.write("Average Test Time: " +
                        (totalTestsExecuted.get() > 0 ? totalExecutionTime.get() / totalTestsExecuted.get() : 0)
                        + " ms\n\n");

                writer.write("Performance Analysis:\n");
                writer.write("--------------------\n");

                long slowTests = testExecutions.values().stream()
                        .mapToLong(TestExecutionInfo::getExecutionTimeMs)
                        .filter(time -> time > SLOW_TEST_THRESHOLD_MS)
                        .count();

                writer.write("Slow Tests (>" + SLOW_TEST_THRESHOLD_MS + "ms): " + slowTests + "\n");

                long verySlowTests = testExecutions.values().stream()
                        .mapToLong(TestExecutionInfo::getExecutionTimeMs)
                        .filter(time -> time > VERY_SLOW_TEST_THRESHOLD_MS)
                        .count();

                writer.write("Very Slow Tests (>" + VERY_SLOW_TEST_THRESHOLD_MS + "ms): " + verySlowTests + "\n");

                // Top 5 slowest tests
                writer.write("\nTop 5 Slowest Tests:\n");
                testExecutions.values().stream()
                        .sorted((a, b) -> Long.compare(b.getExecutionTimeMs(), a.getExecutionTimeMs()))
                        .limit(5)
                        .forEach(test -> {
                            try {
                                writer.write(String.format("- %s: %d ms\n",
                                        test.getTestName(), test.getExecutionTimeMs()));
                            } catch (IOException e) {
                                logger.error("Failed to write slow test to summary", e);
                            }
                        });
            }

            logger.info("Overall test performance summary generated: {}", summaryFileName);

        } catch (IOException e) {
            logger.error("Failed to generate overall test performance summary", e);
        }
    }

    /**
     * Test execution information holder.
     */
    private static class TestExecutionInfo {
        private final String testName;
        private final String className;
        private final String methodName;
        private final Instant startTime;
        private final MemoryUsage startMemoryUsage;
        private Instant endTime;
        private MemoryUsage endMemoryUsage;
        private TestStatus status = TestStatus.RUNNING;
        private String failureCause;

        public TestExecutionInfo(String testName, String className, String methodName,
                Instant startTime, MemoryUsage startMemoryUsage) {
            this.testName = testName;
            this.className = className;
            this.methodName = methodName;
            this.startTime = startTime;
            this.startMemoryUsage = startMemoryUsage;
        }

        public long getExecutionTimeMs() {
            return endTime != null ? Duration.between(startTime, endTime).toMillis() : 0;
        }

        public long getMemoryDeltaMB() {
            return endMemoryUsage != null ? (endMemoryUsage.getUsed() - startMemoryUsage.getUsed()) / (1024 * 1024) : 0;
        }

        // Getters and setters
        public String getTestName() {
            return testName;
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }

        public Instant getStartTime() {
            return startTime;
        }

        public MemoryUsage getStartMemoryUsage() {
            return startMemoryUsage;
        }

        public Instant getEndTime() {
            return endTime;
        }

        public void setEndTime(Instant endTime) {
            this.endTime = endTime;
        }

        public MemoryUsage getEndMemoryUsage() {
            return endMemoryUsage;
        }

        public void setEndMemoryUsage(MemoryUsage endMemoryUsage) {
            this.endMemoryUsage = endMemoryUsage;
        }

        public TestStatus getStatus() {
            return status;
        }

        public void setStatus(TestStatus status) {
            this.status = status;
        }

        public String getFailureCause() {
            return failureCause;
        }

        public void setFailureCause(String failureCause) {
            this.failureCause = failureCause;
        }
    }

    /**
     * Class execution information holder.
     */
    private static class ClassExecutionInfo {
        private final String className;
        private final Instant startTime;
        private final MemoryUsage startMemoryUsage;
        private Instant endTime;
        private MemoryUsage endMemoryUsage;

        public ClassExecutionInfo(String className, Instant startTime, MemoryUsage startMemoryUsage) {
            this.className = className;
            this.startTime = startTime;
            this.startMemoryUsage = startMemoryUsage;
        }

        public long getMemoryDeltaMB() {
            return endMemoryUsage != null ? (endMemoryUsage.getUsed() - startMemoryUsage.getUsed()) / (1024 * 1024) : 0;
        }

        // Getters and setters
        public String getClassName() {
            return className;
        }

        public Instant getStartTime() {
            return startTime;
        }

        public MemoryUsage getStartMemoryUsage() {
            return startMemoryUsage;
        }

        public Instant getEndTime() {
            return endTime;
        }

        public void setEndTime(Instant endTime) {
            this.endTime = endTime;
        }

        public MemoryUsage getEndMemoryUsage() {
            return endMemoryUsage;
        }

        public void setEndMemoryUsage(MemoryUsage endMemoryUsage) {
            this.endMemoryUsage = endMemoryUsage;
        }
    }

    /**
     * Test status enumeration.
     */
    private enum TestStatus {
        RUNNING, PASSED, FAILED, ABORTED, DISABLED
    }
}