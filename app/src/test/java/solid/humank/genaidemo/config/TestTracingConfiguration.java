package solid.humank.genaidemo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import solid.humank.genaidemo.infrastructure.observability.tracing.TraceContextManager;

/**
 * Test configuration for tracing components
 */
@TestConfiguration
public class TestTracingConfiguration {

    @Bean
    @Primary
    public OpenTelemetry openTelemetry() {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(SdkTracerProvider.builder().build())
                .build();
    }

    @Bean
    @Primary
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("test-tracer");
    }

    @Bean
    @Primary
    public TraceContextManager traceContextManager() {
        return new TraceContextManager();
    }
}