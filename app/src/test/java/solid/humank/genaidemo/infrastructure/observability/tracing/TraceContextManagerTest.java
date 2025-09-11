package solid.humank.genaidemo.infrastructure.observability.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/**
 * Unit tests for TraceContextManager
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "tracing.enabled=true"
})
class TraceContextManagerTest {

    @Autowired
    private TraceContextManager traceContextManager;

    @Autowired
    private Tracer tracer;

    private Span testSpan;
    private Scope testScope;

    @BeforeEach
    void setUp() {
        // Create a test span for each test
        testSpan = tracer.spanBuilder("test-span").startSpan();
        testScope = testSpan.makeCurrent();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        if (testScope != null) {
            testScope.close();
        }
        if (testSpan != null) {
            testSpan.end();
        }
        MDC.clear();
    }

    @Test
    @DisplayName("Should set correlation ID in MDC and span")
    void shouldSetCorrelationIdInMdcAndSpan() {
        // Given
        String correlationId = "test-correlation-123";

        // When
        traceContextManager.setCorrelationId(correlationId);

        // Then
        assertThat(MDC.get("correlationId")).isEqualTo(correlationId);
        // Note: We can't easily verify span attributes in unit tests without mocking
    }

    @Test
    @DisplayName("Should set business context in MDC and span")
    void shouldSetBusinessContextInMdcAndSpan() {
        // Given
        String userId = "user-123";
        String orderId = "order-456";

        // When
        traceContextManager.setBusinessContext(userId, orderId);

        // Then
        assertThat(MDC.get("userId")).isEqualTo(userId);
        assertThat(MDC.get("orderId")).isEqualTo(orderId);
    }

    @Test
    @DisplayName("Should set customer context in MDC and span")
    void shouldSetCustomerContextInMdcAndSpan() {
        // Given
        String customerId = "customer-789";

        // When
        traceContextManager.setCustomerContext(customerId);

        // Then
        assertThat(MDC.get("customerId")).isEqualTo(customerId);
    }

    @Test
    @DisplayName("Should update MDC with trace context")
    void shouldUpdateMdcWithTraceContext() {
        // When
        traceContextManager.updateMDCWithTraceContext();

        // Then
        assertThat(MDC.get("traceId")).isNotNull();
        assertThat(MDC.get("spanId")).isNotNull();
    }

    @Test
    @DisplayName("Should get current trace ID")
    void shouldGetCurrentTraceId() {
        // When
        traceContextManager.updateMDCWithTraceContext();
        var traceId = traceContextManager.getCurrentTraceId();

        // Then
        assertThat(traceId).isPresent();
        assertThat(traceId.get()).isNotEmpty();
    }

    @Test
    @DisplayName("Should get current span ID")
    void shouldGetCurrentSpanId() {
        // When
        traceContextManager.updateMDCWithTraceContext();
        var spanId = traceContextManager.getCurrentSpanId();

        // Then
        assertThat(spanId).isPresent();
        assertThat(spanId.get()).isNotEmpty();
    }

    @Test
    @DisplayName("Should get current correlation ID")
    void shouldGetCurrentCorrelationId() {
        // Given
        String correlationId = "test-correlation-456";
        traceContextManager.setCorrelationId(correlationId);

        // When
        var retrievedCorrelationId = traceContextManager.getCurrentCorrelationId();

        // Then
        assertThat(retrievedCorrelationId).isPresent();
        assertThat(retrievedCorrelationId.get()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Should record business operation")
    void shouldRecordBusinessOperation() {
        // Given
        String operationType = "order-processing";
        String operationName = "createOrder";
        String entityId = "order-123";

        // When
        traceContextManager.recordBusinessOperation(operationType, operationName, entityId);

        // Then
        // This test mainly verifies no exceptions are thrown
        // In a real scenario, we would verify span attributes
    }

    @Test
    @DisplayName("Should clear context")
    void shouldClearContext() {
        // Given
        traceContextManager.setCorrelationId("test-correlation");
        traceContextManager.setBusinessContext("user-123", "order-456");
        traceContextManager.updateMDCWithTraceContext();

        // When
        traceContextManager.clearContext();

        // Then
        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("userId")).isNull();
        assertThat(MDC.get("orderId")).isNull();
        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("spanId")).isNull();
    }

    @Test
    @DisplayName("Should initialize trace context")
    void shouldInitializeTraceContext() {
        // Given
        String correlationId = "init-correlation-789";

        // When
        traceContextManager.initializeTraceContext(correlationId);

        // Then
        assertThat(MDC.get("correlationId")).isEqualTo(correlationId);
        assertThat(MDC.get("traceId")).isNotNull();
        assertThat(MDC.get("spanId")).isNotNull();
    }

    @Test
    @DisplayName("Should handle null correlation ID gracefully")
    void shouldHandleNullCorrelationIdGracefully() {
        // When
        traceContextManager.setCorrelationId(null);

        // Then
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    @DisplayName("Should handle empty correlation ID gracefully")
    void shouldHandleEmptyCorrelationIdGracefully() {
        // When
        traceContextManager.setCorrelationId("   ");

        // Then
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    @DisplayName("Should record error with throwable")
    void shouldRecordErrorWithThrowable() {
        // Given
        Exception testException = new RuntimeException("Test error");
        String errorMessage = "Test error occurred";

        // When
        traceContextManager.recordError(testException, errorMessage);

        // Then
        // This test mainly verifies no exceptions are thrown
        // In a real scenario, we would verify span error attributes
    }
}