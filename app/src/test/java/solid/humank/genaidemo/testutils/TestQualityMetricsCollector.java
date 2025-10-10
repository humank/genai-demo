package solid.humank.genaidemo.testutils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Collects and reports test quality metrics including execution time, success rate,
 * and coverage statistics.
 * 
 * <p>Usage:
 * <pre>{@code
 * @ExtendWith(TestQualityMetricsCollector.class)
 * class MyTest {
 *     // Tests are automatically monitored
 * }
 * }</pre>
 * 
 * <p>Metrics collected:
 * <ul>
 *   <li>Test execution time</li>
 *   <li>Test success/failure rate</li>
 *   <li>Slow test identification</li>
 *   <li>Test reliability metrics</li>
 * </ul>
 * 
 * @since 1.0
 */
public class TestQualityMetricsCollector implements 
        BeforeTestExecutionCallback, 
        AfterTestExecutionCallback,
        AfterAllCallback {
    
    private static final String START_TIME_KEY = "startTime";
    private static final long SLOW_TEST_THRESHOLD_MS = 50;
    
    private static final Map<String, TestMetrics> metricsMap = new ConcurrentHashMap<>();
    
    @Override
    public void beforeTestExecution(ExtensionContext context) {
        context.getStore(ExtensionContext.Namespace.GLOBAL)
            .put(START_TIME_KEY, Instant.now());
    }
    
    @Override
    public void afterTestExecution(ExtensionContext context) {
        Instant startTime = context.getStore(ExtensionContext.Namespace.GLOBAL)
            .get(START_TIME_KEY, Instant.class);
        
        if (startTime != null) {
            long executionTimeMs = Duration.between(startTime, Instant.now()).toMillis();
            boolean success = !context.getExecutionException().isPresent();
            
            String testName = context.getDisplayName();
            String className = context.getRequiredTestClass().getSimpleName();
            
            TestMetrics metrics = new TestMetrics(
                className,
                testName,
                executionTimeMs,
                success
            );
            
            metricsMap.put(className + "." + testName, metrics);
        }
    }
    
    @Override
    public void afterAll(ExtensionContext context) {
        if (context.getParent().isEmpty()) {
            // This is the root context, generate report
            generateQualityReport();
        }
    }
    
    private void generateQualityReport() {
        if (metricsMap.isEmpty()) {
            return;
        }
        
        List<TestMetrics> allMetrics = new ArrayList<>(metricsMap.values());
        
        // Calculate statistics
        long totalTests = allMetrics.size();
        long successfulTests = allMetrics.stream().filter(TestMetrics::success).count();
        long failedTests = totalTests - successfulTests;
        double successRate = (double) successfulTests / totalTests * 100;
        
        double avgExecutionTime = allMetrics.stream()
            .mapToLong(TestMetrics::executionTimeMs)
            .average()
            .orElse(0.0);
        
        List<TestMetrics> slowTests = allMetrics.stream()
            .filter(m -> m.executionTimeMs() > SLOW_TEST_THRESHOLD_MS)
            .sorted(Comparator.comparingLong(TestMetrics::executionTimeMs).reversed())
            .limit(10)
            .toList();
        
        // Generate report
        StringBuilder report = new StringBuilder();
        report.append("=".repeat(80)).append("\n");
        report.append("TEST QUALITY METRICS REPORT\n");
        report.append("=".repeat(80)).append("\n\n");
        
        report.append("Overall Statistics:\n");
        report.append(String.format("  Total Tests: %d\n", totalTests));
        report.append(String.format("  Successful: %d\n", successfulTests));
        report.append(String.format("  Failed: %d\n", failedTests));
        report.append(String.format("  Success Rate: %.2f%%\n", successRate));
        report.append(String.format("  Average Execution Time: %.2f ms\n\n", avgExecutionTime));
        
        if (!slowTests.isEmpty()) {
            report.append("Slow Tests (> ").append(SLOW_TEST_THRESHOLD_MS).append("ms):\n");
            for (int i = 0; i < slowTests.size(); i++) {
                TestMetrics metrics = slowTests.get(i);
                report.append(String.format("  %d. %s.%s - %d ms\n",
                    i + 1,
                    metrics.className(),
                    metrics.testName(),
                    metrics.executionTimeMs()));
            }
            report.append("\n");
        }
        
        // Group by class
        Map<String, List<TestMetrics>> byClass = allMetrics.stream()
            .collect(Collectors.groupingBy(TestMetrics::className));
        
        report.append("Test Metrics by Class:\n");
        byClass.forEach((className, metrics) -> {
            long classTotal = metrics.size();
            long classSuccess = metrics.stream().filter(TestMetrics::success).count();
            double classAvgTime = metrics.stream()
                .mapToLong(TestMetrics::executionTimeMs)
                .average()
                .orElse(0.0);
            
            report.append(String.format("  %s: %d tests, %.2f%% success, %.2f ms avg\n",
                className, classTotal, (double) classSuccess / classTotal * 100, classAvgTime));
        });
        
        report.append("\n").append("=".repeat(80)).append("\n");
        
        // Write report to file
        try {
            Path reportDir = Paths.get("build/reports/test-quality");
            Files.createDirectories(reportDir);
            
            Path reportFile = reportDir.resolve("quality-metrics.txt");
            Files.writeString(reportFile, report.toString());
            
            System.out.println("\nTest Quality Metrics Report generated: " + reportFile);
        } catch (IOException e) {
            System.err.println("Failed to write quality metrics report: " + e.getMessage());
        }
    }
    
    /**
     * Test metrics data record.
     */
    private record TestMetrics(
        String className,
        String testName,
        long executionTimeMs,
        boolean success
    ) {}
}
