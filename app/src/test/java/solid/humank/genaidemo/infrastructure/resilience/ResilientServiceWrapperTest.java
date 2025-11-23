package solid.humank.genaidemo.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Resilient Service Wrapper
 * 
 * Tests resilience patterns:
 * - Circuit Breaker behavior
 * - Retry mechanism
 * - Fallback execution
 * - Metrics recording
 */
@ExtendWith(MockitoExtension.class)
class ResilientServiceWrapperTest {
    
    private ResilientServiceWrapper resilientWrapper;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private RetryRegistry retryRegistry;    private TimeLimiterRegistry timeLimiterRegistry;
    private MeterRegistry meterRegistry;
    
    @BeforeEach
    void setUp() {
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        retryRegistry = RetryRegistry.ofDefaults();
        timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        meterRegistry = new SimpleMeterRegistry();
        
        resilientWrapper = new ResilientServiceWrapper(
            circuitBreakerRegistry,
            retryRegistry,
            timeLimiterRegistry,
            meterRegistry
        );
    }
    
    @Test
    void should_execute_operation_successfully() {
        // Given
        String serviceName = "testService";
        String expectedResult = "success";
        
        // When
        String result = resilientWrapper.executeWithResilience(
            serviceName,
            () -> expectedResult,
            () -> "fallback"
        );
        
        // Then
        assertThat(result).isEqualTo(expectedResult);
        
        // Verify metrics
        assertThat(meterRegistry.counter("resilience.operation.success", 
            "service", serviceName).count()).isEqualTo(1);
    }
    
    @Test
    void should_use_fallback_when_operation_fails() {
        // Given
        String serviceName = "testService";
        String fallbackResult = "fallback-value";
        
        // When
        String result = resilientWrapper.executeWithResilience(
            serviceName,
            () -> {
                throw new RuntimeException("Operation failed");
            },
            () -> fallbackResult
        );
        
        // Then
        assertThat(result).isEqualTo(fallbackResult);
        
        // Verify metrics
        assertThat(meterRegistry.counter("resilience.operation.failure", 
            "service", serviceName, "exception", "RuntimeException").count()).isGreaterThan(0);
        assertThat(meterRegistry.counter("resilience.fallback.success", 
            "service", serviceName).count()).isEqualTo(1);
    }
    
    @Test
    void should_retry_on_failure() {
        // Given
        String serviceName = "testService";
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        // When
        String result = resilientWrapper.executeWithResilience(
            serviceName,
            () -> {
                int attempt = attemptCount.incrementAndGet();
                if (attempt < 3) {
                    throw new RuntimeException("Transient failure");
                }
                return "success-after-retry";
            },
            () -> "fallback"
        );
        
        // Then
        assertThat(result).isEqualTo("success-after-retry");
        assertThat(attemptCount.get()).isEqualTo(3);
    }
    
    @Test
    void should_throw_exception_when_no_fallback_provided() {
        // Given
        String serviceName = "testService";
        
        // When & Then
        assertThatThrownBy(() -> 
            resilientWrapper.executeWithResilience(
                serviceName,
                () -> {
                    throw new RuntimeException("Operation failed");
                },
                null
            )
        ).isInstanceOf(ResilientServiceWrapper.ResilientOperationException.class)
         .hasMessageContaining("Operation failed and no fallback provided");
    }
    
    @Test
    void should_execute_with_circuit_breaker_only() {
        // Given
        String serviceName = "testService";
        String expectedResult = "success";
        
        // When
        String result = resilientWrapper.executeWithCircuitBreaker(
            serviceName,
            () -> expectedResult,
            () -> "fallback"
        );
        
        // Then
        assertThat(result).isEqualTo(expectedResult);
    }
    
    @Test
    void should_execute_with_retry_only() {
        // Given
        String serviceName = "testService";
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        // When
        String result = resilientWrapper.executeWithRetry(
            serviceName,
            () -> {
                int attempt = attemptCount.incrementAndGet();
                if (attempt < 2) {
                    throw new RuntimeException("Transient failure");
                }
                return "success-after-retry";
            }
        );
        
        // Then
        assertThat(result).isEqualTo("success-after-retry");
        assertThat(attemptCount.get()).isEqualTo(2);
    }
    
    @Test
    void should_get_circuit_breaker_state() {
        // Given
        String serviceName = "testService";
        
        // Execute some operations to initialize circuit breaker
        resilientWrapper.executeWithCircuitBreaker(
            serviceName,
            () -> "success",
            () -> "fallback"
        );
        
        // When
        String state = resilientWrapper.getCircuitBreakerState(serviceName);
        
        // Then
        assertThat(state).isIn("CLOSED", "OPEN", "HALF_OPEN");
    }
    
    @Test
    void should_get_circuit_breaker_metrics() {
        // Given
        String serviceName = "testService";
        
        // Execute some operations
        for (int i = 0; i < 5; i++) {
            final int index = i; // Make it effectively final
            try {
                resilientWrapper.executeWithCircuitBreaker(
                    serviceName,
                    () -> {
                        if (index % 2 == 0) {
                            throw new RuntimeException("Failure");
                        }
                        return "success";
                    },
                    () -> "fallback"
                );
            } catch (Exception e) {
                // Ignore
            }
        }
        
        // When
        ResilientServiceWrapper.CircuitBreakerMetrics metrics = 
            resilientWrapper.getCircuitBreakerMetrics(serviceName);
        
        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics.serviceName()).isEqualTo(serviceName);
        assertThat(metrics.successfulCalls() + metrics.failedCalls()).isGreaterThan(0);
    }
    
    @Test
    void should_execute_callable_with_resilience() {
        // Given
        String serviceName = "testService";
        String expectedResult = "callable-result";
        
        // When
        String result = resilientWrapper.executeCallable(
            serviceName,
            () -> expectedResult,
            () -> "fallback"
        );
        
        // Then
        assertThat(result).isEqualTo(expectedResult);
    }
    
    @Test
    void should_use_fallback_when_both_operation_and_fallback_fail() {
        // Given
        String serviceName = "testService";
        
        // When & Then
        assertThatThrownBy(() -> 
            resilientWrapper.executeWithResilience(
                serviceName,
                () -> {
                    throw new RuntimeException("Operation failed");
                },
                () -> {
                    throw new RuntimeException("Fallback also failed");
                }
            )
        ).isInstanceOf(ResilientServiceWrapper.ResilientOperationException.class)
         .hasMessageContaining("Both operation and fallback failed");
        
        // Verify fallback failure metric
        assertThat(meterRegistry.counter("resilience.fallback.failure", 
            "service", serviceName).count()).isEqualTo(1);
    }
}
