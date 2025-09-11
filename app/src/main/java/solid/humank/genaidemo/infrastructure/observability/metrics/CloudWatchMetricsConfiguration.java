package solid.humank.genaidemo.infrastructure.observability.metrics;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

/**
 * CloudWatch metrics configuration for production environment
 * Sends application metrics to AWS CloudWatch for monitoring and alerting
 */
@Configuration
@Profile("production")
@ConditionalOnProperty(name = "management.metrics.export.cloudwatch.enabled", havingValue = "true")
public class CloudWatchMetricsConfiguration {

    @Value("${aws.region:ap-northeast-1}")
    private String awsRegion;

    @Value("${management.metrics.export.cloudwatch.namespace:GenAIDemo/Application}")
    private String namespace;

    @Value("${management.metrics.export.cloudwatch.step:PT1M}")
    private String step;

    @Value("${management.metrics.export.cloudwatch.batch-size:20}")
    private int batchSize;

    /**
     * CloudWatch async client for metrics publishing
     */
    @Bean
    public CloudWatchAsyncClient cloudWatchAsyncClient() {
        return CloudWatchAsyncClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }

    /**
     * CloudWatch meter registry configuration
     */
    @Bean
    public CloudWatchMeterRegistry cloudWatchMeterRegistry(CloudWatchAsyncClient cloudWatchAsyncClient) {
        CloudWatchConfig cloudWatchConfig = new CloudWatchConfig() {
            @Override
            public String get(String key) {
                return null; // Use default values
            }

            @Override
            public String namespace() {
                return namespace;
            }

            @Override
            public Duration step() {
                return Duration.parse(step);
            }

            @Override
            public int batchSize() {
                return batchSize;
            }

            @Override
            public boolean enabled() {
                return true;
            }
        };

        return new CloudWatchMeterRegistry(cloudWatchConfig, Clock.SYSTEM, cloudWatchAsyncClient);
    }

    /**
     * Custom CloudWatch metrics publisher for business metrics
     */
    @Bean
    public CloudWatchBusinessMetricsPublisher cloudWatchBusinessMetricsPublisher(
            CloudWatchAsyncClient cloudWatchAsyncClient,
            @Value("${spring.application.name}") String applicationName,
            @Value("${spring.profiles.active}") String environment) {
        return new CloudWatchBusinessMetricsPublisher(cloudWatchAsyncClient, applicationName, environment);
    }
}