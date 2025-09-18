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
 * Simplified tracing configuration specifically for TracingWebIntegrationTest
 * 
 * This configuration:
 * - Uses @Primary to avoid Bean conflicts
 * - Provides minimal tracing setup for web integration testing
 * - Avoids complex dependencies that cause configuration issues
 * 
 * Requirements addressed:
 * - 4.2: Resolve Bean conflicts in tracing configuration
 * - 4.3: Simplify test configuration to avoid complex Bean dependencies
 */
@TestConfiguration
@Profile("test")
public class SimplifiedTracingWebTestConfiguration {

    /**
     * Simplified OpenTelemetry instance for web integration tests
     * Uses @Primary to override any conflicting beans
     */
    @Bean
    @Primary
    public OpenTelemetry webTestOpenTelemetry() {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(
                        SdkTracerProvider.builder()
                                .build())
                .build();
    }

    /**
     * Simplified Tracer for web integration tests
     */
    @Bean
    @Primary
    public Tracer webTestTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("web-integration-test", "1.0.0");
    }

    /**
     * TraceContextManager for web integration tests
     * Uses the real implementation to test actual functionality
     */
    @Bean
    @Primary
    public TraceContextManager webTestTraceContextManager() {
        return new TraceContextManager();
    }
}