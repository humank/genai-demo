package solid.humank.genaidemo.infrastructure.config;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import java.time.Duration;
import java.util.Map;

/**
 * AWS Native Concurrency Monitoring System - CloudWatch Metrics Configuration
 * 
 * This configuration enables Spring Boot Actuator metrics export to CloudWatch
 * for comprehensive monitoring of thread pools, JVM metrics, and application performance.
 * 
 * Features:
 * - Thread pool metrics for KEDA autoscaling
 * - JVM memory and GC metrics
 * - HTTP request metrics
 * - Custom business metrics
 * - X-Ray distributed tracing integration
 * 
 * Created: 2025年9月24日 下午6:23 (台北時間)
 */
@Configuration
@Profile({"staging", "production"})
@ConditionalOnProperty(name = "management.metrics.export.cloudwatch.enabled", havingValue = "true")
public class CloudWatchMetricsConfig {

    @Value("${management.metrics.export.cloudwatch.namespace:GenAIDemo}")
    private String namespace;

    @Value("${spring.profiles.active:development}")
    private String environment;

    @Value("${aws.region:ap-east-2}")
    private String awsRegion;

    /**
     * Configure CloudWatch MeterRegistry for metrics export
     */
    @Bean
    public CloudWatchMeterRegistry cloudWatchMeterRegistry() {
        CloudWatchConfig cloudWatchConfig = new CloudWatchConfig() {
            @Override
            public String get(String key) {
                return null; // Use default values
            }

            @Override
            public String namespace() {
                return namespace + "/" + environment.substring(0, 1).toUpperCase() + environment.substring(1);
            }

            @Override
            public Duration step() {
                return Duration.ofMinutes(1); // Send metrics every minute
            }

            @Override
            public int batchSize() {
                return 20; // Send up to 20 metrics per batch
            }
        };

        CloudWatchAsyncClient cloudWatchAsyncClient = CloudWatchAsyncClient.builder()
                .region(Region.of(awsRegion))
                .build();

        return new CloudWatchMeterRegistry(cloudWatchConfig, Clock.SYSTEM, cloudWatchAsyncClient);
    }

    /**
     * Customize MeterRegistry with common tags and filters
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            // Add common tags to all metrics
            registry.config()
                    .commonTags(
                            "application", "genai-demo",
                            "environment", environment,
                            "region", awsRegion,
                            "service", "genai-demo-backend"
                    )
                    // Filter out noisy metrics
                    .meterFilter(MeterFilter.deny(id -> {
                        String name = id.getName();
                        // Filter out some noisy JVM metrics
                        return name.startsWith("jvm.gc.overhead") ||
                               name.startsWith("jvm.buffer.count") ||
                               name.startsWith("process.files");
                    }))
                    // Rename metrics for better organization
                    .meterFilter(MeterFilter.renameTag("executor", "name", "thread_pool"))
                    // Add percentile histograms for important metrics
                    .meterFilter(MeterFilter.accept(id -> id.getName().startsWith("http.server.requests")))
                    .meterFilter(MeterFilter.accept(id -> id.getName().startsWith("executor")));
        };
    }

    /**
     * Thread Pool Metrics Configuration for KEDA Integration
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> threadPoolMetricsCustomizer() {
        return registry -> {
            // Configure thread pool metrics specifically for KEDA monitoring
            registry.config()
                    .meterFilter(MeterFilter.accept(id -> {
                        String name = id.getName();
                        // Accept all executor metrics for KEDA
                        return name.startsWith("executor.") ||
                               name.startsWith("thread_pool.") ||
                               name.startsWith("event_processing_executor.") ||
                               name.startsWith("retry_executor.");
                    }));
        };
    }

    /**
     * JVM Metrics Configuration
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> jvmMetricsCustomizer() {
        return registry -> {
            // Configure JVM metrics for monitoring
            registry.config()
                    .meterFilter(MeterFilter.accept(id -> {
                        String name = id.getName();
                        // Accept important JVM metrics
                        return name.startsWith("jvm.memory.") ||
                               name.startsWith("jvm.gc.") ||
                               name.startsWith("jvm.threads.") ||
                               name.startsWith("jvm.classes.");
                    }));
        };
    }

    /**
     * HTTP Metrics Configuration
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> httpMetricsCustomizer() {
        return registry -> {
            // Configure HTTP metrics for monitoring
            registry.config()
                    .meterFilter(MeterFilter.accept(id -> {
                        String name = id.getName();
                        // Accept HTTP server metrics
                        return name.startsWith("http.server.requests") ||
                               name.startsWith("http.client.requests");
                    }))
                    // Add custom tags for HTTP metrics
                    .meterFilter(MeterFilter.commonTags(Tags.of("component", "web")));
        };
    }

    /**
     * Custom Business Metrics Configuration
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> businessMetricsCustomizer() {
        return registry -> {
            // Configure business metrics
            registry.config()
                    .meterFilter(MeterFilter.accept(id -> {
                        String name = id.getName();
                        // Accept business metrics
                        return name.startsWith("genai.demo.") ||
                               name.startsWith("customer.") ||
                               name.startsWith("order.") ||
                               name.startsWith("event.") ||
                               name.startsWith("health.") ||
                               name.startsWith("redis.") ||
                               name.startsWith("database.");
                    }))
                    // Add custom tags for business metrics
                    .meterFilter(MeterFilter.commonTags(Tags.of("component", "business")));
        };
    }
}