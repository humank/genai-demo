package solid.humank.genaidemo.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j Configuration for Application Resilience Patterns
 * 
 * Provides circuit breaker, retry, and time limiter configurations
 * for disaster recovery and business continuity.
 * 
 * @see <a href="https://resilience4j.readme.io/">Resilience4j Documentation</a>
 */
@Configuration
public class ResilienceConfiguration {    private static final Logger logger = LoggerFactory.getLogger(ResilienceConfiguration.class);
    
    /**
     * Circuit Breaker Registry with custom configurations
     * 
     * Circuit Breaker Pattern prevents cascading failures by:
     * - Opening circuit after failure threshold
     * - Half-open state for recovery testing
     * - Automatic recovery when service is healthy
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        logger.info("Initializing Circuit Breaker Registry");
        
        // Default configuration for most services
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50) // Open circuit if 50% of calls fail
            .slowCallRateThreshold(50) // Open circuit if 50% of calls are slow
            .slowCallDurationThreshold(Duration.ofSeconds(2)) // Call is slow if > 2s
            .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before half-open
            .permittedNumberOfCallsInHalfOpenState(5) // Test with 5 calls in half-open
            .minimumNumberOfCalls(10) // Need 10 calls before calculating failure rate
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(20) // Use last 20 calls for calculation
            .recordExceptions(Exception.class) // Record all exceptions
            .build();
        
        // Critical service configuration - more aggressive
        CircuitBreakerConfig criticalConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(30) // More sensitive - open at 30% failure
            .slowCallRateThreshold(40)
            .slowCallDurationThreshold(Duration.ofSeconds(1)) // Stricter timeout
            .waitDurationInOpenState(Duration.ofSeconds(60)) // Longer recovery time
            .permittedNumberOfCallsInHalfOpenState(3)
            .minimumNumberOfCalls(5)
            .slidingWindowSize(15)
            .build();
        
        // Non-critical service configuration - more lenient
        CircuitBreakerConfig lenientConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(70) // More tolerant
            .slowCallRateThreshold(60)
            .slowCallDurationThreshold(Duration.ofSeconds(5))
            .waitDurationInOpenState(Duration.ofSeconds(15)) // Faster recovery
            .permittedNumberOfCallsInHalfOpenState(10)
            .minimumNumberOfCalls(20)
            .slidingWindowSize(30)
            .build();
        
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);
        
        // Register named configurations
        registry.addConfiguration("critical", criticalConfig);
        registry.addConfiguration("lenient", lenientConfig);
        
        logger.info("Circuit Breaker Registry initialized with 3 configurations: default, critical, lenient");
        
        return registry;
    }
    
    /**
     * Retry Registry with exponential backoff configurations
     * 
     * Retry Pattern handles transient failures by:
     * - Automatic retry with exponential backoff
     * - Maximum retry attempts
     * - Configurable retry conditions
     */
    @Bean
    public RetryRegistry retryRegistry() {
        logger.info("Initializing Retry Registry");
        
        // Default retry configuration
        RetryConfig defaultConfig = RetryConfig.custom()
            .maxAttempts(3) // Maximum 3 attempts
            .waitDuration(Duration.ofMillis(500)) // Initial wait 500ms
            .intervalFunction(io.github.resilience4j.core.IntervalFunction
                .ofExponentialBackoff(500, 2)) // Exponential backoff: 500ms, 1s, 2s
            .retryExceptions(Exception.class) // Retry on all exceptions
            .ignoreExceptions(IllegalArgumentException.class) // Don't retry on validation errors
            .build();
        
        // Database retry configuration - more aggressive
        RetryConfig databaseConfig = RetryConfig.custom()
            .maxAttempts(5) // More retries for database
            .waitDuration(Duration.ofMillis(1000))
            .intervalFunction(io.github.resilience4j.core.IntervalFunction
                .ofExponentialBackoff(1000, 2)) // 1s, 2s, 4s, 8s, 16s
            .build();
        
        // External API retry configuration - less aggressive
        RetryConfig externalApiConfig = RetryConfig.custom()
            .maxAttempts(2) // Only 2 retries for external APIs
            .waitDuration(Duration.ofMillis(200))
            .intervalFunction(io.github.resilience4j.core.IntervalFunction
                .ofExponentialBackoff(200, 1.5)) // 200ms, 300ms
            .build();
        
        RetryRegistry registry = RetryRegistry.of(defaultConfig);
        
        // Register named configurations
        registry.addConfiguration("database", databaseConfig);
        registry.addConfiguration("externalApi", externalApiConfig);
        
        logger.info("Retry Registry initialized with 3 configurations: default, database, externalApi");
        
        return registry;
    }
    
    /**
     * Time Limiter Registry for timeout management
     * 
     * Time Limiter Pattern prevents long-running operations from blocking:
     * - Configurable timeout durations
     * - Automatic cancellation of slow operations
     * - Integration with circuit breaker
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        logger.info("Initializing Time Limiter Registry");
        
        // Default time limiter configuration
        TimeLimiterConfig defaultConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(3)) // 3 second timeout
            .cancelRunningFuture(true) // Cancel the operation if timeout
            .build();
        
        // Fast operation configuration
        TimeLimiterConfig fastConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(1))
            .cancelRunningFuture(true)
            .build();
        
        // Slow operation configuration
        TimeLimiterConfig slowConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(10))
            .cancelRunningFuture(true)
            .build();
        
        TimeLimiterRegistry registry = TimeLimiterRegistry.of(defaultConfig);
        
        // Register named configurations
        registry.addConfiguration("fast", fastConfig);
        registry.addConfiguration("slow", slowConfig);
        
        logger.info("Time Limiter Registry initialized with 3 configurations: default, fast, slow");
        
        return registry;
    }
}
