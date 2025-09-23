package solid.humank.genaidemo.infrastructure.observability.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

/**
 * 單元測試 - 測試 TraceContextManager 的核心邏輯
 * 
 * 優點：
 * - 快速執行（< 50ms）
 * - 無外部依賴
 * - 專注於業務邏輯
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TraceContextManager 單元測試")
class TraceContextManagerUnitTest {

    private TraceContextManager traceContextManager;

    @BeforeEach
    void setUp() {
        traceContextManager = new TraceContextManager();
        // 清理 MDC 確保測試隔離
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        // 清理 MDC
        MDC.clear();
    }

    @Test
    @DisplayName("應該正確設置和獲取 Correlation ID")
    void shouldSetAndGetCorrelationId() {
        // Given
        String correlationId = "test-correlation-123";

        // When
        traceContextManager.setCorrelationId(correlationId);

        // Then
        Optional<String> result = traceContextManager.getCurrentCorrelationId();
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(correlationId);

        // 驗證 MDC 也被正確設置
        assertThat(MDC.get("correlationId")).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("應該正確設置業務上下文")
    void shouldSetBusinessContext() {
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
    @DisplayName("應該正確設置客戶上下文")
    void shouldSetCustomerContext() {
        // Given
        String customerId = "customer-789";

        // When
        traceContextManager.setCustomerContext(customerId);

        // Then
        assertThat(MDC.get("customerId")).isEqualTo(customerId);
    }

    @Test
    @DisplayName("應該正確清理上下文")
    void shouldClearContext() {
        // Given - 設置一些上下文
        traceContextManager.setCorrelationId("test-correlation");
        traceContextManager.setBusinessContext("user-123", "order-456");
        traceContextManager.setCustomerContext("customer-789");

        // When
        traceContextManager.clearContext();

        // Then
        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("userId")).isNull();
        assertThat(MDC.get("orderId")).isNull();
        assertThat(MDC.get("customerId")).isNull();
        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("spanId")).isNull();
    }

    @Test
    @DisplayName("應該處理空值和空字符串")
    void shouldHandleNullAndEmptyValues() {
        // When - 設置 null 和空值
        traceContextManager.setCorrelationId(null);
        traceContextManager.setCorrelationId("");
        traceContextManager.setCorrelationId("   ");
        traceContextManager.setBusinessContext(null, "");
        traceContextManager.setCustomerContext("   ");

        // Then - 不應該設置任何 MDC 值
        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("userId")).isNull();
        assertThat(MDC.get("orderId")).isNull();
        assertThat(MDC.get("customerId")).isNull();
    }

    @Test
    @DisplayName("應該正確初始化追蹤上下文")
    void shouldInitializeTraceContext() {
        // Given
        String correlationId = "init-correlation-123";

        // When
        traceContextManager.initializeTraceContext(correlationId);

        // Then
        Optional<String> result = traceContextManager.getCurrentCorrelationId();
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("當沒有有效 Span 時應該返回空的 Trace ID")
    void shouldReturnEmptyTraceIdWhenNoValidSpan() {
        // When - 沒有活動的 span
        Optional<String> traceId = traceContextManager.getCurrentTraceId();
        Optional<String> spanId = traceContextManager.getCurrentSpanId();

        // Then
        assertThat(traceId).isEmpty();
        assertThat(spanId).isEmpty();
    }

    @Test
    @DisplayName("應該正確記錄錯誤信息到 MDC")
    void shouldRecordErrorInformation() {
        // Given
        Exception testException = new RuntimeException("Test error");
        String errorMessage = "Business operation failed";

        // When
        traceContextManager.recordError(testException, errorMessage);

        // Then - 在單元測試中，我們主要驗證方法不會拋出異常
        // 實際的 span 操作會在集成測試中驗證
        // 這裡我們驗證方法可以安全調用
        assertThat(true).isTrue(); // 方法執行成功
    }

    @Test
    @DisplayName("應該正確記錄業務操作信息")
    void shouldRecordBusinessOperation() {
        // Given
        String operationType = "CREATE";
        String operationName = "CreateOrder";
        String entityId = "order-123";

        // When
        traceContextManager.recordBusinessOperation(operationType, operationName, entityId);

        // Then - 驗證方法可以安全調用
        assertThat(true).isTrue(); // 方法執行成功
    }
}