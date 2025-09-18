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
import solid.humank.genaidemo.config.TestObservabilityConfiguration;

/**
 * 配置測試 - 驗證追蹤組件的 Spring 配置
 * 
 * 優點：
 * - 專注於配置驗證
 * - 使用簡化的測試配置
 * - 避免複雜的依賴衝突
 */
@SpringBootTest(classes = {
        TestObservabilityConfiguration.class,
        TraceContextManager.class
})
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.main.lazy-initialization=true",
        "spring.jmx.enabled=false",
        "logging.level.root=ERROR",
        "management.health.defaults.enabled=false"
})
@DisplayName("追蹤配置測試")
class TracingConfigurationTest {

    @Autowired
    private OpenTelemetry openTelemetry;

    @Autowired
    private Tracer tracer;

    @Autowired
    private TraceContextManager traceContextManager;

    @Test
    @DisplayName("應該正確配置 OpenTelemetry 組件")
    void shouldHaveOpenTelemetryComponentsConfigured() {
        // Then
        assertThat(openTelemetry).isNotNull();
        assertThat(tracer).isNotNull();
        assertThat(traceContextManager).isNotNull();
    }

    @Test
    @DisplayName("應該能夠創建 Tracer")
    void shouldBeAbleToCreateTracer() {
        // When
        var span = tracer.spanBuilder("test-span").startSpan();

        // Then
        assertThat(span).isNotNull();
        assertThat(span.getSpanContext().isValid()).isTrue();

        // Cleanup
        span.end();
    }

    @Test
    @DisplayName("TraceContextManager 應該能夠處理基本操作")
    void shouldHandleBasicTraceContextOperations() {
        // Given
        String correlationId = "config-test-correlation";

        // When
        traceContextManager.setCorrelationId(correlationId);

        // Then
        var result = traceContextManager.getCurrentCorrelationId();
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(correlationId);

        // Cleanup
        traceContextManager.clearContext();
    }
}