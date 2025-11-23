package solid.humank.genaidemo.infrastructure.resilience;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.annotation.Timed;

/**
 * Example service demonstrating resilience patterns usage.
 *
 * This service shows how to apply circuit breaker, retry, fallback,
 * caching, and monitoring patterns to your services.
 *
 * Usage Examples:
 *
 * 1. Simple Circuit Breaker with Fallback:
 *
 * @CircuitBreaker(name = "exampleService", fallbackMethod = "findByIdFallback")
 *                      public Optional<Data> findById(String id) { ... }
 *
 *                      2. Retry with Exponential Backoff:
 * @Retry(name = "database")
 *             public Data save(Data data) { ... }
 *
 *             3. Combined Patterns:
 * @CircuitBreaker(name = "exampleService", fallbackMethod = "processFallback")
 * @Retry(name = "externalApi")
 * @TimeLimiter(name = "externalApi")
 *                   public Result process(Request request) { ... }
 *
 * @author Kiro AI Assistant
 * @since 1.0
 */
@Service
public class ExampleResilientService {
    private static final Logger logger = LoggerFactory.getLogger(ExampleResilientService.class);

    private final ResilientServiceWrapper resilientWrapper;

    public ExampleResilientService(ResilientServiceWrapper resilientWrapper) {
        this.resilientWrapper = resilientWrapper;
    }

    // ============================================================================
    // Example 1: Circuit Breaker with Fallback
    // ============================================================================

    /**
     * Example method demonstrating circuit breaker with fallback.
     *
     * Circuit breaker will open after 50% failure rate (default config).
     * When open, fallback method is called immediately without attempting the
     * operation.
     */
    @CircuitBreaker(name = "exampleService", fallbackMethod = "findByIdFallback")
    @Timed(value = "example.findById", description = "Time taken to find by ID")
    public Optional<String> findById(String id) {
        logger.info("Finding data by ID: {}", id);

        // Simulate potential failure
        if (id.startsWith("fail")) {
            throw new RuntimeException("Simulated failure for ID: " + id);
        }

        return Optional.of("Data for ID: " + id);
    }

    /**
     * Fallback method for findById.
     * Called when circuit breaker is open or operation fails.
     * Note: This method is invoked by Resilience4j framework via reflection
     */
    @SuppressWarnings("unused")
    private Optional<String> findByIdFallback(String id, Exception e) {
        logger.warn("Using fallback for ID: {} due to: {}", id, e.getMessage());
        return Optional.empty();
    }

    // ============================================================================
    // Example 2: Retry with Exponential Backoff
    // ============================================================================

    /**
     * Example method demonstrating retry with exponential backoff.
     *
     * Will retry 3 times with exponential backoff: 500ms, 1s, 2s
     */
    @Retry(name = "database")
    @Timed(value = "example.save", description = "Time taken to save data")
    public String save(String data) {
        logger.info("Saving data: {}", data);

        // Simulate transient failure
        if (Math.random() < 0.3) {
            throw new RuntimeException("Transient database error");
        }

        return "Saved: " + data;
    }

    // ============================================================================
    // Example 3: Combined Patterns (Circuit Breaker + Retry + Time Limiter)
    // ============================================================================

    /**
     * Example method demonstrating combined resilience patterns.
     *
     * - Circuit Breaker: Prevents cascading failures
     * - Retry: Handles transient failures
     * - Time Limiter: Prevents slow operations
     */
    @CircuitBreaker(name = "externalApi", fallbackMethod = "callExternalApiFallback")
    @Retry(name = "externalApi")
    @TimeLimiter(name = "externalApi")
    @Timed(value = "example.callExternalApi", description = "Time taken to call external API")
    public CompletableFuture<String> callExternalApi(String request) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Calling external API with request: {}", request);

            // Simulate API call
            try {
                Thread.sleep(100); // Simulate network delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted", e);
            }

            return "API Response for: " + request;
        });
    }

    /**
     * Fallback method for callExternalApi.
     * Note: This method is invoked by Resilience4j framework via reflection
     */
    @SuppressWarnings("unused")
    private CompletableFuture<String> callExternalApiFallback(String request, Exception e) {
        logger.warn("Using fallback for external API call: {} due to: {}", request, e.getMessage());
        return CompletableFuture.completedFuture("Fallback response");
    }

    // ============================================================================
    // Example 4: Circuit Breaker with Caching
    // ============================================================================

    /**
     * Example method demonstrating circuit breaker with caching.
     *
     * Cache provides additional resilience by serving cached data
     * when the circuit breaker is open.
     */
    @CircuitBreaker(name = "exampleService", fallbackMethod = "findAllFallback")
    @Cacheable(value = "exampleData", unless = "#result.isEmpty()")
    @Timed(value = "example.findAll", description = "Time taken to find all")
    public List<String> findAll() {
        logger.info("Finding all data");

        // Simulate potential failure
        if (Math.random() < 0.2) {
            throw new RuntimeException("Simulated failure in findAll");
        }

        return List.of("Data1", "Data2", "Data3");
    }

    /**
     * Fallback method for findAll.
     * Note: This method is invoked by Resilience4j framework via reflection
     */
    @SuppressWarnings("unused")
    private List<String> findAllFallback(Exception e) {
        logger.warn("Using fallback for findAll due to: {}", e.getMessage());
        return List.of(); // Return empty list as fallback
    }

    // ============================================================================
    // Example 5: Using ResilientServiceWrapper Directly
    // ============================================================================

    /**
     * Example method demonstrating direct use of ResilientServiceWrapper.
     *
     * This approach gives you more control over resilience patterns
     * and allows dynamic configuration.
     */
    public String processWithWrapper(String data) {
        return resilientWrapper.executeWithResilience(
                "exampleService",
                () -> {
                    logger.info("Processing data with wrapper: {}", data);

                    // Simulate processing
                    if (data.startsWith("error")) {
                        throw new RuntimeException("Processing error");
                    }

                    return "Processed: " + data;
                },
                () -> {
                    logger.warn("Using fallback for data: {}", data);
                    return "Fallback: " + data;
                });
    }

    // ============================================================================
    // Example 6: Critical Service with Strict Circuit Breaker
    // ============================================================================

    /**
     * Example method for critical services with strict circuit breaker.
     *
     * Uses "critical" configuration:
     * - 30% failure rate threshold (vs 50% default)
     * - 60s wait duration (vs 30s default)
     * - 5 minimum calls (vs 10 default)
     */
    @CircuitBreaker(name = "critical", fallbackMethod = "processCriticalFallback")
    @Retry(name = "database")
    @Timed(value = "example.processCritical", description = "Time taken to process critical operation")
    public String processCritical(String data) {
        logger.info("Processing critical data: {}", data);

        // Critical operation that must succeed
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }

        return "Critical: " + data;
    }

    /**
     * Fallback method for processCritical.
     * Note: This method is invoked by Resilience4j framework via reflection
     */
    @SuppressWarnings("unused")
    private String processCriticalFallback(String data, Exception e) {
        logger.error("Critical operation failed for data: {} due to: {}", data, e.getMessage());
        // For critical operations, you might want to:
        // 1. Send alert
        // 2. Queue for manual processing
        // 3. Return error response
        return "CRITICAL_FAILURE";
    }

    // ============================================================================
    // Example 7: Lenient Service with Relaxed Circuit Breaker
    // ============================================================================

    /**
     * Example method for non-critical services with lenient circuit breaker.
     *
     * Uses "lenient" configuration:
     * - 70% failure rate threshold (vs 50% default)
     * - 15s wait duration (vs 30s default)
     * - 20 minimum calls (vs 10 default)
     */
    @CircuitBreaker(name = "lenient", fallbackMethod = "processNonCriticalFallback")
    @Timed(value = "example.processNonCritical", description = "Time taken to process non-critical operation")
    public String processNonCritical(String data) {
        logger.info("Processing non-critical data: {}", data);

        // Non-critical operation that can tolerate failures
        return "NonCritical: " + data;
    }

    /**
     * Fallback method for processNonCritical.
     * Note: This method is invoked by Resilience4j framework via reflection
     */
    @SuppressWarnings("unused")
    private String processNonCriticalFallback(String data, Exception e) {
        logger.debug("Non-critical operation failed for data: {} due to: {}", data, e.getMessage());
        return "SKIPPED";
    }
}
