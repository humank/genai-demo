package solid.humank.genaidemo.testutils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test Performance Report Generator for creating detailed HTML reports.
 * 
 * This generator creates:
 * - HTML performance reports with charts
 * - CSV data exports for analysis
 * - Performance trend analysis
 * - Memory usage visualization
 * 
 * Requirements: 5.1, 5.4
 */
public class TestPerformanceReportGenerator {

    private static final Logger logger = LoggerFactory.getLogger(TestPerformanceReportGenerator.class);

    private static final String REPORT_DIR = "build/reports/test-performance";
    private static final String HTML_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Test Performance Report</title>
                <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }
                    .container { max-width: 1200px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { text-align: center; margin-bottom: 30px; }
                    .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 30px; }
                    .stat-card { background: #f8f9fa; padding: 15px; border-radius: 6px; text-align: center; }
                    .stat-value { font-size: 2em; font-weight: bold; color: #007bff; }
                    .stat-label { color: #6c757d; margin-top: 5px; }
                    .chart-container { margin: 30px 0; }
                    .chart-wrapper { position: relative; height: 400px; }
                    .table-container { margin: 30px 0; }
                    table { width: 100%; border-collapse: collapse; }
                    th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
                    th { background-color: #f8f9fa; font-weight: bold; }
                    .slow-test { background-color: #fff3cd; }
                    .failed-test { background-color: #f8d7da; }
                    .passed-test { background-color: #d4edda; }
                    .footer { text-align: center; margin-top: 30px; color: #6c757d; font-size: 0.9em; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Test Performance Report</h1>
                        <p>Generated on: %s</p>
                    </div>

                    <div class="stats-grid">
                        <div class="stat-card">
                            <div class="stat-value">%d</div>
                            <div class="stat-label">Total Tests</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-value">%.1f%%</div>
                            <div class="stat-label">Success Rate</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-value">%d ms</div>
                            <div class="stat-label">Avg Execution Time</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-value">%d</div>
                            <div class="stat-label">Slow Tests</div>
                        </div>
                    </div>

                    <div class="chart-container">
                        <h2>Test Execution Times</h2>
                        <div class="chart-wrapper">
                            <canvas id="executionTimeChart"></canvas>
                        </div>
                    </div>

                    <div class="chart-container">
                        <h2>Memory Usage</h2>
                        <div class="chart-wrapper">
                            <canvas id="memoryUsageChart"></canvas>
                        </div>
                    </div>

                    <div class="table-container">
                        <h2>Test Results</h2>
                        <table>
                            <thead>
                                <tr>
                                    <th>Test Name</th>
                                    <th>Status</th>
                                    <th>Execution Time (ms)</th>
                                    <th>Memory Delta (MB)</th>
                                    <th>Notes</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                        </table>
                    </div>

                    <div class="footer">
                        <p>Report generated by Test Performance Monitor</p>
                    </div>
                </div>

                <script>
                    // Execution Time Chart
                    const executionTimeCtx = document.getElementById('executionTimeChart').getContext('2d');
                    new Chart(executionTimeCtx, {
                        type: 'bar',
                        data: {
                            labels: %s,
                            datasets: [{
                                label: 'Execution Time (ms)',
                                data: %s,
                                backgroundColor: 'rgba(54, 162, 235, 0.6)',
                                borderColor: 'rgba(54, 162, 235, 1)',
                                borderWidth: 1
                            }]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            scales: {
                                y: {
                                    beginAtZero: true,
                                    title: {
                                        display: true,
                                        text: 'Time (ms)'
                                    }
                                }
                            }
                        }
                    });

                    // Memory Usage Chart
                    const memoryUsageCtx = document.getElementById('memoryUsageChart').getContext('2d');
                    new Chart(memoryUsageCtx, {
                        type: 'line',
                        data: {
                            labels: %s,
                            datasets: [{
                                label: 'Memory Delta (MB)',
                                data: %s,
                                backgroundColor: 'rgba(255, 99, 132, 0.2)',
                                borderColor: 'rgba(255, 99, 132, 1)',
                                borderWidth: 2,
                                fill: false
                            }]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            scales: {
                                y: {
                                    title: {
                                        display: true,
                                        text: 'Memory (MB)'
                                    }
                                }
                            }
                        }
                    });
                </script>
            </body>
            </html>
            """;

    /**
     * Generate HTML performance report.
     */
    public static void generateHtmlReport(TestPerformanceData data) {
        try {
            Path reportDir = Paths.get(REPORT_DIR);
            Files.createDirectories(reportDir);

            String reportFileName = reportDir.resolve("performance-report.html").toString();

            try (FileWriter writer = new FileWriter(reportFileName)) {
                String htmlContent = String.format(HTML_TEMPLATE,
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        data.getTotalTests(),
                        data.getSuccessRate(),
                        data.getAverageExecutionTime(),
                        data.getSlowTestCount(),
                        generateTestResultsTable(data.getTestResults()),
                        generateTestNamesJson(data.getTestResults()),
                        generateExecutionTimesJson(data.getTestResults()),
                        generateTestNamesJson(data.getTestResults()),
                        generateMemoryUsageJson(data.getTestResults()));

                writer.write(htmlContent);
            }

            logger.info("HTML performance report generated: {}", reportFileName);

        } catch (IOException e) {
            logger.error("Failed to generate HTML performance report", e);
        }
    }

    /**
     * Generate CSV data export.
     */
    public static void generateCsvReport(TestPerformanceData data) {
        try {
            Path reportDir = Paths.get(REPORT_DIR);
            Files.createDirectories(reportDir);

            String csvFileName = reportDir.resolve("performance-data.csv").toString();

            try (FileWriter writer = new FileWriter(csvFileName)) {
                writer.write(
                        "Test Name,Class Name,Method Name,Status,Execution Time (ms),Memory Delta (MB),Start Time,End Time\n");

                for (TestResult result : data.getTestResults()) {
                    writer.write(String.format("%s,%s,%s,%s,%d,%d,%s,%s\n",
                            escapeCSV(result.getTestName()),
                            escapeCSV(result.getClassName()),
                            escapeCSV(result.getMethodName()),
                            result.getStatus(),
                            result.getExecutionTimeMs(),
                            result.getMemoryDeltaMB(),
                            result.getStartTime(),
                            result.getEndTime()));
                }
            }

            logger.info("CSV performance report generated: {}", csvFileName);

        } catch (IOException e) {
            logger.error("Failed to generate CSV performance report", e);
        }
    }

    /**
     * Generate test results table HTML.
     */
    private static String generateTestResultsTable(List<TestResult> results) {
        StringBuilder sb = new StringBuilder();

        for (TestResult result : results) {
            String rowClass = getRowClass(result);
            String notes = generateNotes(result);

            sb.append(String.format(
                    "<tr class=\"%s\"><td>%s</td><td>%s</td><td>%d</td><td>%d</td><td>%s</td></tr>\n",
                    rowClass,
                    escapeHtml(result.getTestName()),
                    result.getStatus(),
                    result.getExecutionTimeMs(),
                    result.getMemoryDeltaMB(),
                    notes));
        }

        return sb.toString();
    }

    /**
     * Generate test names JSON array.
     */
    private static String generateTestNamesJson(List<TestResult> results) {
        List<String> names = results.stream()
                .map(TestResult::getMethodName)
                .collect(Collectors.toList());

        return "[\"" + String.join("\", \"", names) + "\"]";
    }

    /**
     * Generate execution times JSON array.
     */
    private static String generateExecutionTimesJson(List<TestResult> results) {
        List<String> times = results.stream()
                .map(result -> String.valueOf(result.getExecutionTimeMs()))
                .collect(Collectors.toList());

        return "[" + String.join(", ", times) + "]";
    }

    /**
     * Generate memory usage JSON array.
     */
    private static String generateMemoryUsageJson(List<TestResult> results) {
        List<String> memory = results.stream()
                .map(result -> String.valueOf(result.getMemoryDeltaMB()))
                .collect(Collectors.toList());

        return "[" + String.join(", ", memory) + "]";
    }

    /**
     * Get CSS row class based on test result.
     */
    private static String getRowClass(TestResult result) {
        if ("FAILED".equals(result.getStatus())) {
            return "failed-test";
        } else if ("PASSED".equals(result.getStatus())) {
            if (result.getExecutionTimeMs() > 5000) {
                return "slow-test";
            }
            return "passed-test";
        }
        return "";
    }

    /**
     * Generate notes for test result.
     */
    private static String generateNotes(TestResult result) {
        StringBuilder notes = new StringBuilder();

        if (result.getExecutionTimeMs() > 30000) {
            notes.append("Very Slow Test; ");
        } else if (result.getExecutionTimeMs() > 5000) {
            notes.append("Slow Test; ");
        }

        if (Math.abs(result.getMemoryDeltaMB()) > 50) {
            notes.append("High Memory Usage; ");
        }

        if (result.getFailureCause() != null) {
            notes.append("Failed: ").append(escapeHtml(result.getFailureCause()));
        }

        return notes.toString();
    }

    /**
     * Escape HTML special characters.
     */
    private static String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Escape CSV special characters.
     */
    private static String escapeCSV(String text) {
        if (text == null)
            return "";
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    /**
     * Test performance data holder.
     */
    public static class TestPerformanceData {
        private final List<TestResult> testResults;
        private final int totalTests;
        private final int failedTests;
        private final long totalExecutionTime;

        public TestPerformanceData(List<TestResult> testResults, int totalTests,
                int failedTests, long totalExecutionTime) {
            this.testResults = testResults;
            this.totalTests = totalTests;
            this.failedTests = failedTests;
            this.totalExecutionTime = totalExecutionTime;
        }

        public List<TestResult> getTestResults() {
            return testResults;
        }

        public int getTotalTests() {
            return totalTests;
        }

        public int getFailedTests() {
            return failedTests;
        }

        public long getTotalExecutionTime() {
            return totalExecutionTime;
        }

        public double getSuccessRate() {
            return totalTests > 0 ? (double) (totalTests - failedTests) / totalTests * 100 : 0;
        }

        public long getAverageExecutionTime() {
            return totalTests > 0 ? totalExecutionTime / totalTests : 0;
        }

        public long getSlowTestCount() {
            return testResults.stream()
                    .mapToLong(TestResult::getExecutionTimeMs)
                    .filter(time -> time > 5000)
                    .count();
        }
    }

    /**
     * Test result data holder.
     */
    public static class TestResult {
        private final String testName;
        private final String className;
        private final String methodName;
        private final String status;
        private final long executionTimeMs;
        private final long memoryDeltaMB;
        private final String startTime;
        private final String endTime;
        private final String failureCause;

        public TestResult(String testName, String className, String methodName,
                String status, long executionTimeMs, long memoryDeltaMB,
                String startTime, String endTime, String failureCause) {
            this.testName = testName;
            this.className = className;
            this.methodName = methodName;
            this.status = status;
            this.executionTimeMs = executionTimeMs;
            this.memoryDeltaMB = memoryDeltaMB;
            this.startTime = startTime;
            this.endTime = endTime;
            this.failureCause = failureCause;
        }

        // Getters
        public String getTestName() {
            return testName;
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getStatus() {
            return status;
        }

        public long getExecutionTimeMs() {
            return executionTimeMs;
        }

        public long getMemoryDeltaMB() {
            return memoryDeltaMB;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public String getFailureCause() {
            return failureCause;
        }
    }
}