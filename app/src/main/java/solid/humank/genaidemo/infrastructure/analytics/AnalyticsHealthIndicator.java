package solid.humank.genaidemo.infrastructure.analytics;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Health indicator for the analytics pipeline.
 * 
 * This health indicator checks the status of the analytics event publisher
 * and reports whether the analytics pipeline is healthy and able to process
 * events.
 * 
 * Requirements addressed:
 * - 8.2: Verify external service availability (analytics pipeline)
 * - 9.6: Show clear indicators when data sources are unavailable
 */
@Component
@ConditionalOnProperty(name = "analytics.enabled", havingValue = "true", matchIfMissing = false)
public class AnalyticsHealthIndicator implements HealthIndicator {

    private final AnalyticsEventPublisher analyticsPublisher;

    public AnalyticsHealthIndicator(AnalyticsEventPublisher analyticsPublisher) {
        this.analyticsPublisher = analyticsPublisher;
    }

    @Override
    public Health health() {
        try {
            boolean isHealthy = analyticsPublisher.isHealthy();

            if (isHealthy) {
                return Health.up()
                        .withDetail("status", "Analytics pipeline is operational")
                        .withDetail("publisher", analyticsPublisher.getClass().getSimpleName())
                        .withDetail("description", "Events are being successfully sent to analytics pipeline")
                        .build();
            } else {
                return Health.down()
                        .withDetail("status", "Analytics pipeline is experiencing issues")
                        .withDetail("publisher", analyticsPublisher.getClass().getSimpleName())
                        .withDetail("description", "Events may not be reaching the analytics pipeline")
                        .withDetail("recommendation", "Check AWS Kinesis Firehose service status and permissions")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("status", "Analytics health check failed")
                    .withDetail("error", e.getMessage())
                    .withDetail("description", "Unable to determine analytics pipeline status")
                    .withException(e)
                    .build();
        }
    }
}