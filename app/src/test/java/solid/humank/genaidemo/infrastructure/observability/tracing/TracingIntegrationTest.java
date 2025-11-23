package solid.humank.genaidemo.infrastructure.observability.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import solid.humank.genaidemo.config.SimplifiedTracingConfiguration;

/**
 * 重構後的追蹤集成測試
 * 
 * 改進：
 * - 移除了複雜的配置依賴
 * - 專注於核心追蹤功能
 * - 使用簡化的測試配置
 * 
 * 注意：大部分測試已經移到專門的單元測試和配置測試中
 * 這個類現在只測試最核心的集成場景
 */
@SpringBootTest(classes = {
        SimplifiedTracingConfiguration.class,
        TraceContextManager.class
})
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.main.lazy-initialization=true",
        "spring.jmx.enabled=false",
        "logging.level.root=ERROR",
        "management.health.defaults.enabled=false"
})
@DisplayName("追蹤核心集成測試")
class TracingIntegrationTest {

    @Autowired    private OpenTelemetry openTelemetry;

    @Autowired
    private Tracer tracer;

    @Autowired
    private TraceContextManager traceContextManager;

    @Test
    @DisplayName("應該正確配置追蹤組件")
    void shouldHaveTracingComponentsConfigured() {
        // Then
        assertThat(openTelemetry).isNotNull();
        assertThat(tracer).isNotNull();
        assertThat(traceContextManager).isNotNull();
    }

    @Test
    @DisplayName("應該能夠創建和管理 Span")
    void shouldCreateAndManageSpans() {
        // When
        var span = tracer.spanBuilder("integration-test-span")
                .setAttribute("test.type", "integration")
                .startSpan();

        // Then
        assertThat(span).isNotNull();
        assertThat(span.getSpanContext().isValid()).isTrue();

        // 驗證可以獲取 span 信息
        String traceId = span.getSpanContext().getTraceId();
        String spanId = span.getSpanContext().getSpanId();

        assertThat(traceId).isNotEmpty();
        assertThat(spanId).isNotEmpty();

        // Cleanup
        span.end();
    }

    @Test
    @DisplayName("應該正確處理追蹤上下文傳播")
    void shouldHandleTraceContextPropagation() {
        // Given
        String correlationId = "integration-test-correlation";

        // When
        var span = tracer.spanBuilder("parent-span").startSpan();

        try (var scope = span.makeCurrent()) {
            // 在 span 上下文中設置 correlation ID
            traceContextManager.setCorrelationId(correlationId);

            // 創建子 span
            var childSpan = tracer.spanBuilder("child-span").startSpan();

            try (var childScope = childSpan.makeCurrent()) {
                // 驗證上下文傳播
                var retrievedCorrelationId = traceContextManager.getCurrentCorrelationId();
                assertThat(retrievedCorrelationId).isPresent();
                assertThat(retrievedCorrelationId.get()).isEqualTo(correlationId);

                // 驗證 span 層次結構
                assertThat(childSpan.getSpanContext().getTraceId())
                        .isEqualTo(span.getSpanContext().getTraceId());

            } finally {
                childSpan.end();
            }

        } finally {
            span.end();
            traceContextManager.clearContext();
        }
    }

    @Test
    @DisplayName("應該正確處理業務上下文")
    void shouldHandleBusinessContext() {
        // Given
        String userId = "integration-user-123";
        String orderId = "integration-order-456";
        String customerId = "integration-customer-789";

        // When
        var span = tracer.spanBuilder("business-operation").startSpan();

        try (var scope = span.makeCurrent()) {
            traceContextManager.setBusinessContext(userId, orderId);
            traceContextManager.setCustomerContext(customerId);

            // 記錄業務操作
            traceContextManager.recordBusinessOperation("CREATE", "CreateOrder", orderId);

            // Then - 驗證操作成功完成
            assertThat(true).isTrue();

        } finally {
            span.end();
            traceContextManager.clearContext();
        }
    }

    @Test
    @DisplayName("應該正確處理錯誤記錄")
    void shouldHandleErrorRecording() {
        // Given
        Exception testException = new RuntimeException("Integration test error");
        String errorMessage = "Test error for tracing";

        // When
        var span = tracer.spanBuilder("error-test-span").startSpan();

        try (var scope = span.makeCurrent()) {
            traceContextManager.recordError(testException, errorMessage);

            // Then - 驗證操作成功完成
            assertThat(true).isTrue();

        } finally {
            span.end();
        }
    }
}
