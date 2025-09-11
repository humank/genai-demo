package solid.humank.genaidemo.infrastructure.observability.metrics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

/**
 * Custom CloudWatch metrics publisher for business-specific metrics
 * Publishes high-level business KPIs to CloudWatch for executive dashboards
 */
@Component
@Profile("production")
@ConditionalOnProperty(name = "management.metrics.export.cloudwatch.enabled", havingValue = "true")
public class CloudWatchBusinessMetricsPublisher {

    private static final Logger logger = LoggerFactory.getLogger(CloudWatchBusinessMetricsPublisher.class);

    private final CloudWatchAsyncClient cloudWatchClient;
    private final String applicationName;
    private final String environment;
    private final String namespace;

    public CloudWatchBusinessMetricsPublisher(CloudWatchAsyncClient cloudWatchClient,
            String applicationName,
            String environment) {
        this.cloudWatchClient = cloudWatchClient;
        this.applicationName = applicationName;
        this.environment = environment;
        this.namespace = "GenAIDemo/Business";
    }

    /**
     * Publish business metrics to CloudWatch
     */
    @Async
    public CompletableFuture<Void> publishBusinessMetrics(String metricName, double value, String unit) {
        return publishBusinessMetrics(metricName, value, unit, List.of());
    }

    /**
     * Publish business metrics with custom dimensions
     */
    @Async
    public CompletableFuture<Void> publishBusinessMetrics(String metricName, double value, String unit,
            List<Dimension> customDimensions) {
        try {
            List<Dimension> dimensions = new ArrayList<>();
            dimensions.add(Dimension.builder()
                    .name("Application")
                    .value(applicationName)
                    .build());
            dimensions.add(Dimension.builder()
                    .name("Environment")
                    .value(environment)
                    .build());
            dimensions.addAll(customDimensions);

            MetricDatum metricDatum = MetricDatum.builder()
                    .metricName(metricName)
                    .value(value)
                    .unit(StandardUnit.fromValue(unit))
                    .timestamp(Instant.now())
                    .dimensions(dimensions)
                    .build();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace(namespace)
                    .metricData(metricDatum)
                    .build();

            return cloudWatchClient.putMetricData(request)
                    .thenRun(() -> logger.debug("Successfully published metric {} with value {} to CloudWatch",
                            metricName, value))
                    .exceptionally(throwable -> {
                        logger.error("Failed to publish metric {} to CloudWatch", metricName, throwable);
                        return null;
                    });

        } catch (Exception e) {
            logger.error("Error publishing business metric {} to CloudWatch", metricName, e);
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Publish customer metrics
     */
    public CompletableFuture<Void> publishCustomerMetrics(String metricName, double value) {
        List<Dimension> dimensions = List.of(
                Dimension.builder().name("Domain").value("Customer").build());
        return publishBusinessMetrics(metricName, value, "Count", dimensions);
    }

    /**
     * Publish order metrics
     */
    public CompletableFuture<Void> publishOrderMetrics(String metricName, double value, String unit) {
        List<Dimension> dimensions = List.of(
                Dimension.builder().name("Domain").value("Order").build());
        return publishBusinessMetrics(metricName, value, unit, dimensions);
    }

    /**
     * Publish payment metrics
     */
    public CompletableFuture<Void> publishPaymentMetrics(String metricName, double value, String status) {
        List<Dimension> dimensions = List.of(
                Dimension.builder().name("Domain").value("Payment").build(),
                Dimension.builder().name("Status").value(status).build());
        return publishBusinessMetrics(metricName, value, "Count", dimensions);
    }

    /**
     * Publish inventory metrics
     */
    public CompletableFuture<Void> publishInventoryMetrics(String metricName, double value) {
        List<Dimension> dimensions = List.of(
                Dimension.builder().name("Domain").value("Inventory").build());
        return publishBusinessMetrics(metricName, value, "Count", dimensions);
    }

    /**
     * Publish performance metrics
     */
    public CompletableFuture<Void> publishPerformanceMetrics(String metricName, double value, String operation) {
        List<Dimension> dimensions = List.of(
                Dimension.builder().name("Operation").value(operation).build());
        return publishBusinessMetrics(metricName, value, "Milliseconds", dimensions);
    }
}