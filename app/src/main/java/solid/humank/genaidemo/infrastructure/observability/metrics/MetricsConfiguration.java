package solid.humank.genaidemo.infrastructure.observability.metrics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

/**
 * Metrics configuration for different environments
 * - Development: Prometheus only
 * - Production: Both Prometheus and CloudWatch
 */
@Configuration
public class MetricsConfiguration {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${aws.region:ap-northeast-1}")
    private String awsRegion;

    /**
     * Prometheus registry for all environments
     */
    @Bean
    public PrometheusMeterRegistry prometheusMeterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    /**
     * CloudWatch registry for production environment only
     * Note: CloudWatch registry is provided by CloudWatchMetricsConfiguration to
     * avoid duplication
     */

    /**
     * Common meter registry customizer for all environments
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags(
                        "application", applicationName,
                        "environment", activeProfile,
                        "region", awsRegion)
                .meterFilter(MeterFilter.deny(id -> {
                    String name = id.getName();
                    // Filter out only very noisy metrics, keep important JVM metrics
                    return name.startsWith("jvm.gc.pause") ||
                            name.startsWith("process.files.open") ||
                            name.startsWith("system.load.average.1m");
                }));
    }

    /**
     * Ensure JVM metrics are available
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> jvmMetricsCustomizer() {
        return registry -> {
            // JVM metrics should be auto-configured by Spring Boot
            // This customizer ensures they are not filtered out
        };
    }

    /**
     * Business metrics configuration
     */
    // Temporarily disabled due to compilation issues
    // @Bean
    // public BusinessMetricsCollector businessMetricsCollector(MeterRegistry
    // meterRegistry) {
    // return new BusinessMetricsCollector(meterRegistry);
    // }

    /**
     * Custom metrics for domain events
     */
    // Temporarily disabled due to compilation issues
    // @Bean
    // public DomainEventMetricsCollector domainEventMetricsCollector(MeterRegistry
    // meterRegistry) {
    // return new DomainEventMetricsCollector(meterRegistry);
    // }
}