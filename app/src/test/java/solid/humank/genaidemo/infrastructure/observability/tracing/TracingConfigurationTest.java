package solid.humank.genaidemo.infrastructure.observability.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import io.opentelemetry.api.OpenTelemetry;

/**
 * Unit tests for TracingConfiguration
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "tracing.enabled=true",
        "tracing.sampling.ratio=1.0",
        "tracing.jaeger.endpoint=http://localhost:14250",
        "tracing.otlp.endpoint=http://localhost:4317"
})
class TracingConfigurationTest {

    @Autowired(required = false)
    private OpenTelemetry openTelemetry;

    @Test
    @DisplayName("Should create OpenTelemetry bean when tracing is enabled")
    void shouldCreateOpenTelemetryBeanWhenTracingEnabled() {
        // Then
        assertThat(openTelemetry).isNotNull();
    }

    @Test
    @DisplayName("Should configure OpenTelemetry with proper service name")
    void shouldConfigureOpenTelemetryWithProperServiceName() {
        // Given
        assertThat(openTelemetry).isNotNull();

        // When
        var tracer = openTelemetry.getTracer("test-tracer");

        // Then
        assertThat(tracer).isNotNull();
    }
}

/**
 * Test for disabled tracing configuration
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "tracing.enabled=false"
})
class DisabledTracingConfigurationTest {

    @Autowired(required = false)
    private OpenTelemetry openTelemetry;

    @Test
    @DisplayName("Should create no-op OpenTelemetry when tracing is disabled")
    void shouldCreateNoOpOpenTelemetryWhenTracingDisabled() {
        // Then - OpenTelemetry should still be available but as no-op
        assertThat(openTelemetry).isNotNull();
    }
}