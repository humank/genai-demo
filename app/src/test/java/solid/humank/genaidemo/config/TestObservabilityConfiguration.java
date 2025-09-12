package solid.humank.genaidemo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

/**
 * Simplified test configuration for observability components
 * Only includes beans that actually exist to avoid conflicts
 */
@TestConfiguration
public class TestObservabilityConfiguration {

    @Bean
    @Primary
    public MeterRegistry testMeterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @Primary
    public OpenTelemetry testOpenTelemetry() {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(SdkTracerProvider.builder().build())
                .build();
    }

    @Bean
    @Primary
    public Tracer testTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("test-tracer");
    }
}