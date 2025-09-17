package solid.humank.genaidemo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import solid.humank.genaidemo.testutils.TestPerformanceExtension;
import solid.humank.genaidemo.testutils.annotations.IntegrationTest;
import solid.humank.genaidemo.testutils.base.BaseIntegrationTest;

/**
 * Performance and Reliability Testing for Disabled Tests Reactivation
 * 
 * This test class validates:
 * - Test execution consistency across multiple runs
 * - Performance requirements compliance
 * - Concurrent test execution scenarios
 * - Resource cleanup verification
 * - Memory usage patterns
 * - Test reliability under load
 */
@IntegrationTest
@TestPerformanceExtension(maxExecutionTimeMs = 60000, maxMemoryIncreaseMB = 300)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:performance-test",
        "logging.level.solid.humank.genaidemo=INFO",
        "management.endpoints.web.exposure.include=*",
        "test.performance.monitoring.enabled=true"
})
@TestMethodOrder(OrderAnnotation.class)
public class PerformanceReliabilityTest extends BaseIntegrationTest {

    private static final int CONSISTENCY_TEST_ITERATIONS = 5;
    private static final int CONCURRENT_REQUESTS = 20;
    private static final int LOAD_TEST_DURATION_SECONDS = 30;
    private static final long MAX_RESPONSE_TIME_MS = 2000; // 2 seconds max response time
    private static final double MAX_MEMORY_INCREASE_MB = 200.0; // 200MB max memory increase

    private final AtomicInteger successfulRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final List<Long> responseTimes = new ArrayList<>();

    @BeforeEach
    void setUp() {
        logTestStart("PerformanceReliabilityTest setup");

        // Reset counters
        successfulRequests.set(0);
        failedRequests.set(0);
        totalResponseTime.set(0);
        responseTimes.clear();

        // Validate test environment
        validateTestEnvironment();
        waitForApplicationReady();

        // Force cleanup before each test
        forceResourceCleanup();

        logTestEnd("PerformanceReliabilityTest setup");
    }

    @Test
    @Order(1)
    void shouldValidateTestExecutionConsistency() {
        logTestStart("shouldValidateTestExecutionConsistency");

        List<TestResult> results = new ArrayList<>();

        // Execute the same test multiple times to validate consistency
        for (int i = 0; i < CONSISTENCY_TEST_ITERATIONS; i++) {
            logger.info("Consistency test iteration: {}/{}", i + 1, CONSISTENCY_TEST_ITERATIONS);

            Instant startTime = Instant.now();

            // Execute health endpoint test
            ResponseEntity<String> response = performGet("/actuator/health", String.class);

            Instant endTime = Instant.now();
            Duration executionTime = Duration.between(startTime, endTime);

            TestResult result = new TestResult(
                    i + 1,
                    response.getStatusCode() == HttpStatus.OK,
                    executionTime.toMillis(),
                    getCurrentMemoryUsage());

            results.add(result);

            // Validate individual result
            assertThat(result.success())
                    .as("Iteration %d should be successful", i + 1)
                    .isTrue();

            assertThat(result.responseTimeMs())
                    .as("Iteration %d response time should be within limits", i + 1)
                    .isLessThan(MAX_RESPONSE_TIME_MS);

            // Small delay between iterations
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Analyze consistency across all iterations
        analyzeConsistencyResults(results);

        logTestEnd("shouldValidateTestExecutionConsistency");
    }

    @RepeatedTest(3)
    @Order(2)
    void shouldValidateRepeatedTestExecution() {
        logTestStart("shouldValidateRepeatedTestExecution");

        // Test that repeated execution produces consistent results
        Instant startTime = Instant.now();

        ResponseEntity<String> response = performGet("/actuator/metrics", String.class);

        Duration executionTime = Duration.between(startTime, Instant.now());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(executionTime.toMillis()).isLessThan(MAX_RESPONSE_TIME_MS);

        // Validate memory usage is acceptable
        assertThat(isMemoryUsageAcceptable())
                .as("Memory usage should be acceptable after repeated execution")
                .isTrue();

        logTestEnd("shouldValidateRepeatedTestExecution");
    }

    @Test
    @Order(3)
    void shouldValidateConcurrentTestExecution() {
        logTestStart("shouldValidateConcurrentTestExecution");

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_REQUESTS);
        List<CompletableFuture<TestResult>> futures = new ArrayList<>();

        try {
            // Launch concurrent requests
            for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
                final int requestId = i;

                CompletableFuture<TestResult> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        Instant startTime = Instant.now();

                        ResponseEntity<String> response = performGet("/actuator/health", String.class);

                        Duration executionTime = Duration.between(startTime, Instant.now());

                        return new TestResult(
                                requestId,
                                response.getStatusCode() == HttpStatus.OK,
                                executionTime.toMillis(),
                                getCurrentMemoryUsage());
                    } catch (Exception e) {
                        logger.error("Concurrent request {} failed", requestId, e);
                        return new TestResult(requestId, false, -1, 0);
                    } finally {
                        latch.countDown();
                    }
                }, executor);

                futures.add(future);
            }

            // Wait for all requests to complete
            boolean completed;
            try {
                completed = latch.await(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Concurrent test interrupted", e);
            }
            assertThat(completed)
                    .as("All concurrent requests should complete within timeout")
                    .isTrue();

            // Analyze concurrent execution results
            analyzeConcurrentResults(futures);

        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        logTestEnd("shouldValidateConcurrentTestExecution");
    }

    @Test
    @Order(4)
    void shouldValidateLoadTestPerformance() {
        logTestStart("shouldValidateLoadTestPerformance");

        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger requestCount = new AtomicInteger(0);
        Instant testStartTime = Instant.now();

        try {
            // Run load test for specified duration
            while (Duration.between(testStartTime, Instant.now()).getSeconds() < LOAD_TEST_DURATION_SECONDS) {
                executor.submit(() -> {
                    try {
                        Instant requestStart = Instant.now();

                        ResponseEntity<String> response = performGet("/actuator/info", String.class);

                        long responseTime = Duration.between(requestStart, Instant.now()).toMillis();

                        synchronized (responseTimes) {
                            responseTimes.add(responseTime);
                        }

                        if (response.getStatusCode().is2xxSuccessful()) {
                            successfulRequests.incrementAndGet();
                            totalResponseTime.addAndGet(responseTime);
                        } else {
                            failedRequests.incrementAndGet();
                        }

                        requestCount.incrementAndGet();

                    } catch (Exception e) {
                        failedRequests.incrementAndGet();
                        logger.warn("Load test request failed", e);
                    }
                });

                // Small delay to control request rate
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Wait for remaining requests to complete
            executor.shutdown();
            boolean terminated;
            try {
                terminated = executor.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Load test interrupted", e);
            }
            assertThat(terminated)
                    .as("Load test should complete within timeout")
                    .isTrue();

            // Analyze load test results
            analyzeLoadTestResults();

        } finally {
            if (!executor.isShutdown()) {
                executor.shutdownNow();
            }
        }

        logTestEnd("shouldValidateLoadTestPerformance");
    }

    @Test
    @Order(5)
    void shouldValidateResourceCleanupAfterLoad() {
        logTestStart("shouldValidateResourceCleanupAfterLoad");

        // Record initial memory usage
        long initialMemory = getCurrentMemoryUsage();

        // Generate some load to create resources
        for (int i = 0; i < 50; i++) {
            performGet("/actuator/health", String.class);
        }

        // Record memory after load
        long memoryAfterLoad = getCurrentMemoryUsage();

        // Force cleanup
        forceResourceCleanup();

        // Wait for cleanup to complete
        await("Resource cleanup should complete")
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    long currentMemory = getCurrentMemoryUsage();
                    double memoryIncrease = (currentMemory - initialMemory) / (1024.0 * 1024.0);
                    return memoryIncrease < MAX_MEMORY_INCREASE_MB;
                });

        // Validate final memory usage
        long finalMemory = getCurrentMemoryUsage();
        double memoryIncrease = (finalMemory - initialMemory) / (1024.0 * 1024.0);

        assertThat(memoryIncrease)
                .as("Memory increase after cleanup should be within limits")
                .isLessThan(MAX_MEMORY_INCREASE_MB);

        logger.info("Memory usage - Initial: {}MB, After Load: {}MB, Final: {}MB, Increase: {:.2f}MB",
                initialMemory / (1024 * 1024),
                memoryAfterLoad / (1024 * 1024),
                finalMemory / (1024 * 1024),
                memoryIncrease);

        logTestEnd("shouldValidateResourceCleanupAfterLoad");
    }

    @Test
    @Order(6)
    void shouldValidateTestExecutionTimeRequirements() {
        logTestStart("shouldValidateTestExecutionTimeRequirements");

        List<TestExecutionTime> executionTimes = new ArrayList<>();

        // Test different endpoint types and measure execution times
        String[] endpoints = {
                "/actuator/health",
                "/actuator/info",
                "/actuator/metrics"
        };

        for (String endpoint : endpoints) {
            for (int i = 0; i < 5; i++) {
                Instant startTime = Instant.now();

                ResponseEntity<String> response = performGet(endpoint, String.class);

                Duration executionTime = Duration.between(startTime, Instant.now());

                executionTimes.add(new TestExecutionTime(
                        endpoint,
                        executionTime.toMillis(),
                        response.getStatusCode().is2xxSuccessful()));

                // Validate individual execution time
                assertThat(executionTime.toMillis())
                        .as("Execution time for %s should be within limits", endpoint)
                        .isLessThan(MAX_RESPONSE_TIME_MS);
            }
        }

        // Analyze execution time patterns
        analyzeExecutionTimes(executionTimes);

        logTestEnd("shouldValidateTestExecutionTimeRequirements");
    }

    @Test
    @Order(7)
    void shouldValidateMemoryUsagePatterns() {
        logTestStart("shouldValidateMemoryUsagePatterns");

        List<MemorySnapshot> memorySnapshots = new ArrayList<>();

        // Take initial memory snapshot
        memorySnapshots.add(new MemorySnapshot("initial", getCurrentMemoryUsage()));

        // Execute various operations and monitor memory
        for (int batch = 1; batch <= 5; batch++) {
            // Execute a batch of requests
            for (int i = 0; i < 10; i++) {
                performGet("/actuator/health", String.class);
            }

            // Take memory snapshot after batch
            memorySnapshots.add(new MemorySnapshot(
                    "batch_" + batch,
                    getCurrentMemoryUsage()));

            // Small delay between batches
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Force cleanup and take final snapshot
        forceResourceCleanup();

        // Wait a bit for cleanup to take effect
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        memorySnapshots.add(new MemorySnapshot("after_cleanup", getCurrentMemoryUsage()));

        // Analyze memory usage patterns
        analyzeMemoryPatterns(memorySnapshots);

        logTestEnd("shouldValidateMemoryUsagePatterns");
    }

    // Helper methods for analysis

    private void analyzeConsistencyResults(List<TestResult> results) {
        logger.info("=== Consistency Analysis ===");

        long totalSuccessful = results.stream().mapToLong(r -> r.success() ? 1 : 0).sum();
        double successRate = (double) totalSuccessful / results.size() * 100;

        double avgResponseTime = results.stream()
                .mapToLong(TestResult::responseTimeMs)
                .average()
                .orElse(0.0);

        long maxResponseTime = results.stream()
                .mapToLong(TestResult::responseTimeMs)
                .max()
                .orElse(0);

        long minResponseTime = results.stream()
                .mapToLong(TestResult::responseTimeMs)
                .min()
                .orElse(0);

        logger.info("Success Rate: {:.1f}% ({}/{})", successRate, totalSuccessful, results.size());
        logger.info("Response Time - Avg: {:.1f}ms, Min: {}ms, Max: {}ms",
                avgResponseTime, minResponseTime, maxResponseTime);

        // Validate consistency requirements
        assertThat(successRate)
                .as("Success rate should be 100%")
                .isEqualTo(100.0);

        assertThat(avgResponseTime)
                .as("Average response time should be within limits")
                .isLessThan(MAX_RESPONSE_TIME_MS);

        // Check for response time variance (should be relatively consistent)
        double variance = calculateVariance(results.stream()
                .mapToLong(TestResult::responseTimeMs)
                .boxed()
                .toList());

        logger.info("Response Time Variance: {:.2f}", variance);

        // Variance should not be too high (indicating inconsistent performance)
        assertThat(variance)
                .as("Response time variance should indicate consistent performance")
                .isLessThan(1000000.0); // 1000ms^2 variance threshold
    }

    private void analyzeConcurrentResults(List<CompletableFuture<TestResult>> futures) {
        logger.info("=== Concurrent Execution Analysis ===");

        List<TestResult> results = futures.stream()
                .map(future -> {
                    try {
                        return future.get(5, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        logger.error("Failed to get concurrent result", e);
                        return new TestResult(-1, false, -1, 0);
                    }
                })
                .toList();

        long successfulCount = results.stream().mapToLong(r -> r.success() ? 1 : 0).sum();
        double concurrentSuccessRate = (double) successfulCount / results.size() * 100;

        double avgConcurrentResponseTime = results.stream()
                .filter(TestResult::success)
                .mapToLong(TestResult::responseTimeMs)
                .average()
                .orElse(0.0);

        logger.info("Concurrent Success Rate: {:.1f}% ({}/{})",
                concurrentSuccessRate, successfulCount, results.size());
        logger.info("Average Concurrent Response Time: {:.1f}ms", avgConcurrentResponseTime);

        // Validate concurrent execution requirements
        assertThat(concurrentSuccessRate)
                .as("Concurrent success rate should be at least 95%")
                .isGreaterThanOrEqualTo(95.0);

        assertThat(avgConcurrentResponseTime)
                .as("Average concurrent response time should be reasonable")
                .isLessThan(MAX_RESPONSE_TIME_MS * 2); // Allow 2x normal time for concurrent execution
    }

    private void analyzeLoadTestResults() {
        logger.info("=== Load Test Analysis ===");

        int totalRequests = successfulRequests.get() + failedRequests.get();
        double successRate = (double) successfulRequests.get() / totalRequests * 100;
        double avgResponseTime = successfulRequests.get() > 0
                ? (double) totalResponseTime.get() / successfulRequests.get()
                : 0;

        // Calculate percentiles
        List<Long> sortedTimes = new ArrayList<>(responseTimes);
        sortedTimes.sort(Long::compareTo);

        long p50 = getPercentile(sortedTimes, 50);
        long p95 = getPercentile(sortedTimes, 95);
        long p99 = getPercentile(sortedTimes, 99);

        double requestsPerSecond = (double) totalRequests / LOAD_TEST_DURATION_SECONDS;

        logger.info("Total Requests: {}, Success Rate: {:.1f}%", totalRequests, successRate);
        logger.info("Requests/Second: {:.1f}", requestsPerSecond);
        logger.info("Response Time - Avg: {:.1f}ms, P50: {}ms, P95: {}ms, P99: {}ms",
                avgResponseTime, p50, p95, p99);

        // Validate load test requirements
        assertThat(successRate)
                .as("Load test success rate should be at least 95%")
                .isGreaterThanOrEqualTo(95.0);

        assertThat(p95)
                .as("95th percentile response time should be within limits")
                .isLessThan(MAX_RESPONSE_TIME_MS);

        assertThat(requestsPerSecond)
                .as("Should handle reasonable request rate")
                .isGreaterThan(1.0); // At least 1 request per second
    }

    private void analyzeExecutionTimes(List<TestExecutionTime> executionTimes) {
        logger.info("=== Execution Time Analysis ===");

        executionTimes.stream()
                .collect(java.util.stream.Collectors.groupingBy(TestExecutionTime::endpoint))
                .forEach((endpoint, times) -> {
                    double avgTime = times.stream()
                            .mapToLong(TestExecutionTime::executionTimeMs)
                            .average()
                            .orElse(0.0);

                    long maxTime = times.stream()
                            .mapToLong(TestExecutionTime::executionTimeMs)
                            .max()
                            .orElse(0);

                    logger.info("Endpoint {}: Avg={:.1f}ms, Max={}ms", endpoint, avgTime, maxTime);
                });
    }

    private void analyzeMemoryPatterns(List<MemorySnapshot> snapshots) {
        logger.info("=== Memory Usage Pattern Analysis ===");

        long initialMemory = snapshots.get(0).memoryUsage();
        long finalMemory = snapshots.get(snapshots.size() - 1).memoryUsage();
        long maxMemory = snapshots.stream()
                .mapToLong(MemorySnapshot::memoryUsage)
                .max()
                .orElse(0);

        double maxIncrease = (maxMemory - initialMemory) / (1024.0 * 1024.0);
        double finalIncrease = (finalMemory - initialMemory) / (1024.0 * 1024.0);

        logger.info("Memory Usage - Initial: {}MB, Max: {}MB, Final: {}MB",
                initialMemory / (1024 * 1024),
                maxMemory / (1024 * 1024),
                finalMemory / (1024 * 1024));
        logger.info("Memory Increase - Max: {:.2f}MB, Final: {:.2f}MB", maxIncrease, finalIncrease);

        // Validate memory usage patterns
        assertThat(maxIncrease)
                .as("Maximum memory increase should be within limits")
                .isLessThan(MAX_MEMORY_INCREASE_MB);

        assertThat(finalIncrease)
                .as("Final memory increase should be minimal after cleanup")
                .isLessThan(MAX_MEMORY_INCREASE_MB / 2); // Should be less than half the limit after cleanup
    }

    // Utility methods

    private long getCurrentMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private double calculateVariance(List<Long> values) {
        double mean = values.stream().mapToLong(Long::longValue).average().orElse(0.0);
        return values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0.0);
    }

    private long getPercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty())
            return 0;
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1)));
    }

    // Data classes for test results

    private record TestResult(
            int iteration,
            boolean success,
            long responseTimeMs,
            long memoryUsage) {
    }

    private record TestExecutionTime(
            String endpoint,
            long executionTimeMs,
            boolean success) {
    }

    private record MemorySnapshot(
            String phase,
            long memoryUsage) {
    }
}