package solid.humank.genaidemo.infrastructure.observability;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Mock service for business metrics collection in tests
 */
@Service
public class BusinessMetricsCollector {    private static final Logger logger = LoggerFactory.getLogger(BusinessMetricsCollector.class);

    @Autowired
    private MeterRegistry meterRegistry;

    private final AtomicInteger operationCount = new AtomicInteger(0);

    public void recordBusinessOperation(String operationName, double value) {
        try {
            Counter counter = Counter.builder("business.operations")
                    .tag("operation", operationName)
                    .register(meterRegistry);
            counter.increment(value);

            operationCount.incrementAndGet();
            logger.debug("Recorded business operation: {} with value: {}", operationName, value);
        } catch (Exception e) {
            logger.error("Failed to record business operation", e);
        }
    }

    public void recordPerformanceMetric(String metricName, Duration duration) {
        try {
            Timer timer = Timer.builder("performance.metrics")
                    .tag("metric", metricName)
                    .register(meterRegistry);
            timer.record(duration);

            logger.debug("Recorded performance metric: {} with duration: {}", metricName, duration);
        } catch (Exception e) {
            logger.error("Failed to record performance metric", e);
        }
    }

    public void recordSecurityMetric(String eventType) {
        try {
            Counter counter = Counter.builder("security.events")
                    .tag("event", eventType)
                    .register(meterRegistry);
            counter.increment();

            logger.debug("Recorded security metric: {}", eventType);
        } catch (Exception e) {
            logger.error("Failed to record security metric", e);
        }
    }

    public boolean isCloudWatchEnabled() {
        // Mock implementation - would check CloudWatch metrics configuration
        return true;
    }

    public boolean isGrafanaIntegrationEnabled() {
        // Mock implementation - would check Grafana integration
        return true;
    }

    public boolean areAlarmsConfigured() {
        // Mock implementation - would check alarm configuration
        return true;
    }

    public boolean isLocalMonitoringEnabled() {
        // Mock implementation - would check local monitoring tools
        return true;
    }

    public boolean isAwsManagedServicesEnabled() {
        // Mock implementation - would check AWS managed services
        return true;
    }

    public boolean isIamSecurityEnabled() {
        // Mock implementation - would check IAM security configuration
        return true;
    }

    public boolean isCostOptimizationEnabled() {
        // Mock implementation - would check cost optimization
        return true;
    }

    public boolean isSamplingEnabled() {
        // Mock implementation - would check sampling configuration
        return true;
    }

    public boolean isResourceOptimizationEnabled() {
        // Mock implementation - would check resource optimization
        return true;
    }

    public boolean isBillingAlertsEnabled() {
        // Mock implementation - would check billing alerts
        return true;
    }

    public boolean isCostRecommendationsEnabled() {
        // Mock implementation - would check cost recommendations
        return true;
    }

    public int getRecordedOperationCount() {
        return operationCount.get();
    }

    public void reset() {
        operationCount.set(0);
        logger.info("Business metrics collector reset");
    }
}