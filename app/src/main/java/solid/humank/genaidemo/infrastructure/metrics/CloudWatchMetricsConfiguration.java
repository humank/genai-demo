package solid.humank.genaidemo.infrastructure.metrics;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import java.time.Duration;
import java.util.Map;

/**
 * CloudWatch Metrics Configuration for Spring Boot Actuator
 * 
 * This configuration enables automatic export of Micrometer metrics to CloudWatch,
 * including JVM metrics, HTTP metrics, and custom business metrics.
 * 
 * Features:
 * - Automatic JVM metrics (memory, GC, threads)
 * - HTTP request metrics (latency, throughput, errors)
 * - Custom business metrics
 * - Thread pool metrics
 * - Database connection pool metrics
 * 
 * @author GenAI Demo Team
 * @since 1.0
 */
@Configuration
@Profile({"staging", "production"})
public class CloudWatchMetricsConfiguration {

    @Value("${spring.application.name:genai-demo}")
    private String applicationName;

    @Value("${spring.profiles.active:default}")
    private String environment;

    @Value("${aws.region:ap-northeast-1}")
    private String awsRegion;

    /**
     * Configure CloudWatch Meter Registry
     * 
     * This registry automatically exports metrics to CloudWatch at regular intervals.
     */
    @Bean
    public CloudWatchMeterRegistry cloudWatchMeterRegistry(CloudWatchConfig cloudWatchConfig,
                                                            Clock clock) {
        CloudWatchAsyncClient cloudWatchAsyncClient = CloudWatchAsyncClient
            .builder()
            .region(Region.of(awsRegion))
            .build();

        return new CloudWatchMeterRegistry(cloudWatchConfig, clock, cloudWatchAsyncClient);
    }

    /**
     * CloudWatch Configuration
     * 
     * Configures the export behavior including namespace, batch size, and frequency.
     */
    @Bean
    public CloudWatchConfig cloudWatchConfig() {
        return new CloudWatchConfig() {
            private final Map<String, String> configuration = Map.of(
                "cloudwatch.namespace", "GenAIDemo/Application",
                "cloudwatch.step", "PT1M"  // Export every 1 minute
            );

            @Override
            public String get(String key) {
                return configuration.get(key);
            }

            @Override
            public String namespace() {
                return "GenAIDemo/Application";
            }

            @Override
            public int batchSize() {
                return 20;  // Send up to 20 metrics per request
            }

            @Override
            public Duration step() {
                return Duration.ofMinutes(1);  // Export frequency
            }

            @Override
            public boolean enabled() {
                return true;
            }
        };
    }

    /**
     * Customize Meter Registry with common tags
     * 
     * Adds application name and environment as tags to all metrics.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags(
                "application", applicationName,
                "environment", environment,
                "region", awsRegion
            )
            .meterFilter(MeterFilter.deny(id -> {
                // Filter out noisy metrics
                String name = id.getName();
                return name.startsWith("jvm.classes") ||
                       name.startsWith("jvm.buffer") ||
                       name.startsWith("process.files") ||
                       name.startsWith("system.load");
            }));
    }

    /**
     * Clock bean for Micrometer
     */
    @Bean
    public Clock micrometerClock() {
        return Clock.SYSTEM;
    }
}
