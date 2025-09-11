package solid.humank.genaidemo.infrastructure.observability.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

/**
 * 輕量級單元測試 - TraceContextManager
 * 
 * 記憶體使用：~5MB (vs @SpringBootTest ~500MB)
 * 執行時間：~100ms (vs @SpringBootTest ~2s)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Trace Context Manager Unit Tests")
class TraceContextManagerUnitTest {

    private TraceContextManager traceContextManager;

    @BeforeEach
    void setUp() {
        traceContextManager = new TraceContextManager();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Should set correlation ID in MDC")
    void shouldSetCorrelationIdInMdc() {
        // Given
        String correlationId = "test-correlation-123";

        // When
        traceContextManager.setCorrelationId(correlationId);

        // Then
        assertThat(MDC.get("correlationId")).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Should set business context in MDC")
    void shouldSetBusinessContextInMdc() {
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
    @DisplayName("Should set customer context in MDC")
    void shouldSetCustomerContextInMdc() {
        // Given
        String customerId = "customer-789";

        // When
        traceContextManager.setCustomerContext(customerId);

        // Then
        assertThat(MDC.get("customerId")).isEqualTo(customerId);
    }

    @Test
    @DisplayName("Should get current correlation ID from MDC")
    void shouldGetCurrentCorrelationIdFromMdc() {
        // Given
        String correlationId = "test-correlation-456";
        MDC.put("correlationId", correlationId);

        // When
        var retrievedCorrelationId = traceContextManager.getCurrentCorrelationId();

        // Then
        assertThat(retrievedCorrelationId).isPresent();
        assertThat(retrievedCorrelationId.get()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Should return empty when no correlation ID in MDC")
    void shouldReturnEmptyWhenNoCorrelationIdInMdc() {
        // When
        var retrievedCorrelationId = traceContextManager.getCurrentCorrelationId();

        // Then
        assertThat(retrievedCorrelationId).isEmpty();
    }

    @Test
    @DisplayName("Should clear all context from MDC")
    void shouldClearAllContextFromMdc() {
        // Given
        MDC.put("correlationId", "test-correlation");
        MDC.put("userId", "user-123");
        MDC.put("orderId", "order-456");
        MDC.put("traceId", "trace-123");
        MDC.put("spanId", "span-456");

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
    @DisplayName("Should handle null business context gracefully")
    void shouldHandleNullBusinessContextGracefully() {
        // When
        traceContextManager.setBusinessContext(null, null);

        // Then
        assertThat(MDC.get("userId")).isNull();
        assertThat(MDC.get("orderId")).isNull();
    }

    @Test
    @DisplayName("Should handle empty business context gracefully")
    void shouldHandleEmptyBusinessContextGracefully() {
        // When
        traceContextManager.setBusinessContext("", "   ");

        // Then
        assertThat(MDC.get("userId")).isNull();
        assertThat(MDC.get("orderId")).isNull();
    }

    @Test
    @DisplayName("Should initialize trace context with correlation ID")
    void shouldInitializeTraceContextWithCorrelationId() {
        // Given
        String correlationId = "init-correlation-789";

        // When
        traceContextManager.initializeTraceContext(correlationId);

        // Then
        assertThat(MDC.get("correlationId")).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Should validate correlation ID format")
    void shouldValidateCorrelationIdFormat() {
        // Given
        String validCorrelationId = "valid-correlation-123";
        String invalidCorrelationId = "";

        // When & Then - 測試有效的 correlation ID
        traceContextManager.setCorrelationId(validCorrelationId);
        assertThat(MDC.get("correlationId")).isEqualTo(validCorrelationId);

        // 清理 MDC 後測試無效的 correlation ID
        MDC.clear();
        traceContextManager.setCorrelationId(invalidCorrelationId);
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    @DisplayName("Should handle business operation recording")
    void shouldHandleBusinessOperationRecording() {
        // Given
        String operationType = "order-processing";
        String operationName = "createOrder";
        String entityId = "order-123";

        // When
        traceContextManager.recordBusinessOperation(operationType, operationName, entityId);

        // Then - 這個方法主要是記錄到 span，我們測試它不會拋出異常
        // 在實際實現中，這個方法可能會設置一些 MDC 值
    }

    @Test
    @DisplayName("Should handle error recording")
    void shouldHandleErrorRecording() {
        // Given
        Exception testException = new RuntimeException("Test error");
        String errorMessage = "Test error occurred";

        // When
        traceContextManager.recordError(testException, errorMessage);

        // Then - 這個方法主要是記錄到 span，我們測試它不會拋出異常
        // 在實際實現中，這個方法可能會設置一些 MDC 值
    }

    @Test
    @DisplayName("Should update MDC with trace context")
    void shouldUpdateMdcWithTraceContext() {
        // When
        traceContextManager.updateMDCWithTraceContext();

        // Then - 在沒有活動 span 的情況下，這個方法應該不會拋出異常
        // 實際的 trace ID 和 span ID 會在有活動 span 時設置
    }

    @Test
    @DisplayName("Should get current trace ID")
    void shouldGetCurrentTraceId() {
        // When
        var traceId = traceContextManager.getCurrentTraceId();

        // Then - 在沒有活動 span 的情況下，應該返回 empty
        assertThat(traceId).isEmpty();
    }

    @Test
    @DisplayName("Should get current span ID")
    void shouldGetCurrentSpanId() {
        // When
        var spanId = traceContextManager.getCurrentSpanId();

        // Then - 在沒有活動 span 的情況下，應該返回 empty
        assertThat(spanId).isEmpty();
    }
}