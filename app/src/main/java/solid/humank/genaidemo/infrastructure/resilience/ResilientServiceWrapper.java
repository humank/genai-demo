package solid.humank.genaidemo.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Resilient Service Wrapper
 * 
 * Provides a unified interface for applying resilience patterns:
 * - Circuit Breaker: Prevents cascading failures
 * - Retry: Handles transient failures
 * - Time Limiter: Prevents long-running operations
 * - Fallback: Provides degraded functionality
 * 
 * Usage Example:
 * <pre>
 * {@code
 * String result = resilientWrapper.executeWithResilience(
 *     "externalApi",
 *     () -> externalApiClient.call(),
 *     () -> "fallback-value"
 * );
 * }
 * </pre>
 */
@Component
public class ResilientServiceWrapper {
    
    private static final Logger logger = LoggerFactory.getLogger(ResilientServiceWrapper.class);
    
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;
    private final MeterRegistry meterRegistry;
    
    public ResilientServiceWrapper(
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            TimeLimiterRegistry timeLimiterRegistry,
            MeterRegistry meterRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.timeLimiterRegistry = timeLimiterRegistry;
        this.meterRegistry = meterRegistry;
        
        logger.info("Resilient Service Wrapper initialized");
    }
    
    /**
     * Execute operation with full resilience patterns
     * 
     * @param serviceName Name of the service (for metrics and configuration)
     * @param operation The operation to execute
     * @param fallback Fallback operation if main operation fails
     * @return Result of operation or fallback
     */
    public <T> T executeWithResilience(
            String serviceName,
            Supplier<T> operation,
            Supplier<T> fallback) {
        
        Instant startTime = Instant.now();
        
        try {
            // Get or create circuit breaker
            CircuitBreaker circuitBreaker = circuitBreakerRegistry
                .circuitBreaker(serviceName);
            
            // Get or create retry
            Retry retry = retryRegistry.retry(serviceName);
            
            // Decorate operation with resilience patterns
            Supplier<T> decoratedOperation = CircuitBreaker
                .decorateSupplier(circuitBreaker, operation);
            
            decoratedOperation = Retry
                .decorateSupplier(retry, decoratedOperation);
            
            // Execute with resilience
            T result = decoratedOperation.get();
            
            // Record success metrics
            recordSuccess(serviceName, startTime);
            
            return result;
            
        } catch (Exception e) {
            logger.warn("Operation failed for service: {}, using fallback. Error: {}", 
                serviceName, e.getMessage());
            
            // Record failure metrics
            recordFailure(serviceName, startTime, e);
            
            // Execute fallback
            if (fallback != null) {
                try {
                    T fallbackResult = fallback.get();
                    recordFallbackSuccess(serviceName);
                    return fallbackResult;
                } catch (Exception fallbackError) {
                    logger.error("Fallback also failed for service: {}", serviceName, fallbackError);
                    recordFallbackFailure(serviceName);
                    throw new ResilientOperationException(
                        "Both operation and fallback failed for service: " + serviceName, 
                        fallbackError);
                }
            }
            
            throw new ResilientOperationException(
                "Operation failed and no fallback provided for service: " + serviceName, e);
        }
    }
    
    /**
     * Execute operation with circuit breaker only
     */
    public <T> T executeWithCircuitBreaker(
            String serviceName,
            Supplier<T> operation,
            Supplier<T> fallback) {
        
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
        
        try {
            return circuitBreaker.executeSupplier(operation);
        } catch (Exception e) {
            logger.warn("Circuit breaker triggered for service: {}, using fallback", serviceName);
            
            if (fallback != null) {
                return fallback.get();
            }
            
            throw e;
        }
    }
    
    /**
     * Execute operation with retry only
     */
    public <T> T executeWithRetry(
            String serviceName,
            Supplier<T> operation) {
        
        Retry retry = retryRegistry.retry(serviceName);
        
        return retry.executeSupplier(operation);
    }
    
    /**
     * Execute async operation with time limiter
     * 
     * Note: For async operations with timeout, it's recommended to use
     * @TimeLimiter annotation directly on your service methods.
     * 
     * Example:
     * @TimeLimiter(name = "serviceName")
     * public CompletableFuture<T> asyncOperation() { ... }
     */
    public <T> CompletableFuture<T> executeWithTimeout(
            String serviceName,
            Supplier<CompletableFuture<T>> operation,
            Duration timeout) {
        
        logger.info("Executing async operation with timeout for service: {}", serviceName);
        
        try {
            // Execute the operation
            CompletableFuture<T> future = operation.get();
            
            // Add timeout handling
            CompletableFuture<T> timeoutFuture = future.orTimeout(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            
            // Record metrics on completion
            Instant startTime = Instant.now();
            timeoutFuture.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.error("Async operation failed for service: {}", serviceName, throwable);
                    if (throwable instanceof Exception) {
                        recordFailure(serviceName, startTime, (Exception) throwable);
                    }
                } else {
                    recordSuccess(serviceName, startTime);
                }
            });
            
            return timeoutFuture;
        } catch (Exception e) {
            logger.error("Failed to start async operation for service: {}", serviceName, e);
            recordFailure(serviceName, Instant.now(), e);
            throw new ResilientOperationException("Failed to start async operation", e);
        }
    }
    
    /**
     * Execute callable with all resilience patterns
     */
    public <T> T executeCallable(
            String serviceName,
            Callable<T> operation,
            Supplier<T> fallback) {
        
        return executeWithResilience(
            serviceName,
            () -> {
                try {
                    return operation.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            },
            fallback
        );
    }
    
    /**
     * Get circuit breaker state for monitoring
     */
    public String getCircuitBreakerState(String serviceName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
        return circuitBreaker.getState().name();
    }
    
    /**
     * Get circuit breaker metrics
     */
    public CircuitBreakerMetrics getCircuitBreakerMetrics(String serviceName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        
        return new CircuitBreakerMetrics(
            serviceName,
            circuitBreaker.getState().name(),
            metrics.getFailureRate(),
            metrics.getSlowCallRate(),
            metrics.getNumberOfSuccessfulCalls(),
            metrics.getNumberOfFailedCalls(),
            metrics.getNumberOfSlowCalls()
        );
    }
    
    // Private helper methods for metrics recording
    
    private void recordSuccess(String serviceName, Instant startTime) {
        Duration duration = Duration.between(startTime, Instant.now());
        
        meterRegistry.counter("resilience.operation.success",
            Tags.of("service", serviceName))
            .increment();
        
        meterRegistry.timer("resilience.operation.duration",
            Tags.of("service", serviceName, "status", "success"))
            .record(duration);
    }
    
    private void recordFailure(String serviceName, Instant startTime, Exception e) {
        Duration duration = Duration.between(startTime, Instant.now());
        
        meterRegistry.counter("resilience.operation.failure",
            Tags.of("service", serviceName, "exception", e.getClass().getSimpleName()))
            .increment();
        
        meterRegistry.timer("resilience.operation.duration",
            Tags.of("service", serviceName, "status", "failure"))
            .record(duration);
    }
    
    private void recordFallbackSuccess(String serviceName) {
        meterRegistry.counter("resilience.fallback.success",
            Tags.of("service", serviceName))
            .increment();
    }
    
    private void recordFallbackFailure(String serviceName) {
        meterRegistry.counter("resilience.fallback.failure",
            Tags.of("service", serviceName))
            .increment();
    }
    
    /**
     * Circuit Breaker Metrics DTO
     */
    public record CircuitBreakerMetrics(
        String serviceName,
        String state,
        float failureRate,
        float slowCallRate,
        long successfulCalls,
        long failedCalls,
        long slowCalls
    ) {}
    
    /**
     * Custom exception for resilient operations
     */
    public static class ResilientOperationException extends RuntimeException {
        public ResilientOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
