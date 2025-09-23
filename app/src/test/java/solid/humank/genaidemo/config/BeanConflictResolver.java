package solid.humank.genaidemo.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import solid.humank.genaidemo.infrastructure.observability.tracing.TraceContextManager;

/**
 * BeanConflictResolver handles overlapping Bean definitions between test and
 * production configurations.
 * 
 * This configuration uses @AutoConfigureBefore to ensure test beans are created
 * before production beans,
 * and @ConditionalOnProperty annotations to prevent conflicts based on test
 * properties.
 * 
 * Requirements addressed:
 * - 4.1: Handle overlapping Bean definitions
 * - 4.2: Prevent conflicts with @ConditionalOnProperty
 * - 4.3: Control Bean creation order with @AutoConfigureBefore
 */
@TestConfiguration
@Profile("test")
@AutoConfigureBefore({
        HealthCheckConfig.class,
        solid.humank.genaidemo.infrastructure.observability.ObservabilityConfiguration.class,
        solid.humank.genaidemo.infrastructure.observability.tracing.TracingConfiguration.class
})
public class BeanConflictResolver {

    // ========== Health Indicator Beans ==========

    @Bean
    @Primary
    @ConditionalOnProperty(name = "test.health.database.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "databaseHealthIndicator")
    public HealthIndicator testDatabaseHealthIndicator() {
        return () -> Health.up()
                .withDetail("database", "H2 test database")
                .withDetail("status", "UP")
                .withDetail("test", "true")
                .build();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "test.health.readiness.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "applicationReadinessIndicator")
    public HealthIndicator testApplicationReadinessIndicator() {
        return () -> Health.up()
                .withDetail("readiness", "Application is ready for testing")
                .withDetail("status", "UP")
                .withDetail("test", "true")
                .build();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "test.health.resources.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "systemResourcesIndicator")
    public HealthIndicator testSystemResourcesIndicator() {
        return () -> Health.up()
                .withDetail("diskSpace", "Available")
                .withDetail("ping", "UP")
                .withDetail("status", "UP")
                .withDetail("test", "true")
                .build();
    }

    // ========== Observability Beans ==========

    @Bean
    @Primary
    @ConditionalOnProperty(name = "test.observability.metrics.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public MeterRegistry testMeterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "test.observability.tracing.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public OpenTelemetry testOpenTelemetry() {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(SdkTracerProvider.builder().build())
                .build();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "test.observability.tracing.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public Tracer testTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("test-tracer");
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "test.observability.tracing.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public TraceContextManager testTraceContextManager() {
        return new TraceContextManager();
    }

    // ========== Kafka Health Indicator (Disabled in Test) ==========

    /**
     * Kafka health indicator is disabled in test environment by default.
     * This bean will only be created if explicitly enabled via property.
     */
    @Bean
    @ConditionalOnProperty(name = "test.health.kafka.enabled", havingValue = "true", matchIfMissing = false)
    @ConditionalOnMissingBean(name = "kafkaHealthIndicator")
    public HealthIndicator testKafkaHealthIndicator() {
        return () -> Health.up()
                .withDetail("kafka", "Mock Kafka for testing")
                .withDetail("status", "UP")
                .withDetail("test", "true")
                .build();
    }

    // ========== Analytics Health Indicator (Disabled in Test) ==========

    /**
     * Analytics health indicator is disabled in test environment by default.
     * This prevents conflicts with production analytics configuration.
     */
    @Bean
    @ConditionalOnProperty(name = "test.health.analytics.enabled", havingValue = "true", matchIfMissing = false)
    @ConditionalOnMissingBean(name = "analyticsHealthIndicator")
    public HealthIndicator testAnalyticsHealthIndicator() {
        return () -> Health.up()
                .withDetail("analytics", "Mock analytics for testing")
                .withDetail("status", "UP")
                .withDetail("test", "true")
                .build();
    }

    // ========== Security Health Indicator (Disabled in Test) ==========

    /**
     * Security health indicator is disabled in test environment by default.
     * This prevents conflicts with production security monitoring.
     */
    @Bean
    @ConditionalOnProperty(name = "test.health.security.enabled", havingValue = "true", matchIfMissing = false)
    @ConditionalOnMissingBean(name = "securityHealthIndicator")
    public HealthIndicator testSecurityHealthIndicator() {
        return () -> Health.up()
                .withDetail("security", "Mock security monitoring for testing")
                .withDetail("status", "UP")
                .withDetail("test", "true")
                .build();
    }
}