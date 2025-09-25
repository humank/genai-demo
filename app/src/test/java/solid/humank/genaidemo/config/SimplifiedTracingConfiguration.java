package solid.humank.genaidemo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import solid.humank.genaidemo.infrastructure.observability.tracing.TraceContextManager;

/**
 * 簡化的追蹤配置 - 專門為測試設計
 * 
 * 特點：
 * - 使用 @Primary 避免 Bean 衝突
 * - 最小化配置，只包含必要組件
 * - 使用 NoOp 實現避免複雜依賴
 */
@TestConfiguration
@Profile("test")
public class SimplifiedTracingConfiguration {

    /**
     * 測試用的 OpenTelemetry 實例
     * 使用最簡單的配置，避免外部依賴
     */
    @Bean
    @Primary
    public OpenTelemetry testOpenTelemetry() {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(
                        SdkTracerProvider.builder()
                                .build())
                .build();
    }

    /**
     * 測試用的 Tracer
     */
    @Bean
    @Primary
    public Tracer testTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("test-tracer", "1.0.0");
    }

    /**
     * TraceContextManager - 使用真實實現進行測試
     */
    @Bean
    @Primary
    public TraceContextManager testTraceContextManager() {
        return new TraceContextManager();
    }
}