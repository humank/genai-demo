package solid.humank.genaidemo.infrastructure.observability.metrics;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

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
     */
    @Bean
    @Profile("production")
    public CloudWatchMeterRegistry cloudWatchMeterRegistry() {
        CloudWatchConfig cloudWatchConfig = new CloudWatchConfig() {
            @Override
            public String get(String key) {
                return null; // Use default values
            }

            @Override
            public String namespace() {
                return "GenAIDemo/Application";
            }

            @Override
            public Duration step() {
                return Duration.ofMinutes(1);
            }

            @Override
            public int batchSize() {
                return 20;
            }
        };

        CloudWatchAsyncClient cloudWatchAsyncClient = CloudWatchAsyncClient.builder()
                .region(Region.of(awsRegion))
                .build();

        return new CloudWatchMeterRegistry(cloudWatchConfig, Clock.SYSTEM, cloudWatchAsyncClient);
    }

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
                    // Filter out noisy metrics
                    return name.startsWith("jvm.gc.pause") ||
                            name.startsWith("process.files") ||
                            name.startsWith("system.load.average");
                }));
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