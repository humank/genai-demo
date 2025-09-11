package solid.humank.genaidemo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

/**
 * Test configuration for tracing to prevent memory issues and provide
 * lightweight implementations
 */
@TestConfiguration
@Profile("test")
public class TestTracingConfiguration {

    @Bean
    @Primary
    public OpenTelemetry testOpenTelemetry() {
        // Create a minimal OpenTelemetry instance for tests
        return OpenTelemetrySdk.builder()
                .setTracerProvider(SdkTracerProvider.builder()
                        .build())
                .build();
    }

    @Bean
    @Primary
    public Tracer testTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("test-tracer", "1.0.0");
    }
}