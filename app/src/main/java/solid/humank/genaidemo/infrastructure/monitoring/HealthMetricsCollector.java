package solid.humank.genaidemo.infrastructure.monitoring;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Collects and publishes health check metrics to monitoring systems.
 * 
 * Implements requirement 8.3: Update CloudWatch custom metrics when health
 * status changes
 * Implements requirement 8.2: Monitor database connectivity and external
 * service availability
 */
@Component
@Profile("!test")
public class HealthMetricsCollector {

    private static final Logger log = LoggerFactory.getLogger(HealthMetricsCollector.class);

    private final MeterRegistry meterRegistry;
    private final ApplicationContext applicationContext;

    // Metrics tracking
    private final Map<String, Counter> healthCheckCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> healthCheckTimers = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> healthStatusGauges = new ConcurrentHashMap<>();

    // Health status tracking
    private final Map<String, HealthStatus> lastHealthStatus = new ConcurrentHashMap<>();

    public HealthMetricsCollector(MeterRegistry meterRegistry, ApplicationContext applicationContext) {
        this.meterRegistry = meterRegistry;
        this.applicationContext = applicationContext;
        initializeMetrics();
    }

    /**
     * Initialize health check metrics
     */
    private void initializeMetrics() {
        // Get all health indicators from application context
        Map<String, HealthIndicator> healthIndicators = applicationContext.getBeansOfType(HealthIndicator.class);

        for (String indicatorName : healthIndicators.keySet()) {
            String metricName = sanitizeMetricName(indicatorName);

            // Create counter for health check executions
            healthCheckCounters.put(metricName, Counter.builder("health.check.executions")
                    .description("Number of health check executions")
                    .tag("indicator", metricName)
                    .register(meterRegistry));

            // Create timer for health check duration
            healthCheckTimers.put(metricName, Timer.builder("health.check.duration")
                    .description("Health check execution duration")
                    .tag("indicator", metricName)
                    .register(meterRegistry));

            // Create gauge for health status (1 = UP, 0 = DOWN, -1 = UNKNOWN)
            AtomicLong statusValue = new AtomicLong(0);
            healthStatusGauges.put(metricName, statusValue);

            Gauge.builder("health.check.status", statusValue, AtomicLong::get)
                    .description("Health check status (1=UP, 0=DOWN, -1=UNKNOWN)")
                    .tag("indicator", metricName)
                    .register(meterRegistry);
        }

        log.info("Initialized health metrics for {} indicators", healthIndicators.size());
    }

    /**
     * Collect health metrics from all health indicators
     * Runs every 30 seconds to update CloudWatch metrics
     */
    @Scheduled(fixedRate = 30000) // 30 seconds
    public void collectHealthMetrics() {
        log.debug("Collecting health metrics from all indicators");

        Map<String, HealthIndicator> healthIndicators = applicationContext.getBeansOfType(HealthIndicator.class);

        for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {
            String indicatorName = entry.getKey();
            HealthIndicator indicator = entry.getValue();

            collectHealthMetric(indicatorName, indicator);
        }
    }

    /**
     * Collect metrics from a specific health indicator
     */
    private void collectHealthMetric(String indicatorName, HealthIndicator indicator) {
        String metricName = sanitizeMetricName(indicatorName);

        // Ensure metrics are initialized for this indicator
        ensureMetricsInitialized(metricName);

        Timer.Sample sample = Timer.start(meterRegistry);
        Instant startTime = Instant.now();

        try {
            // Execute health check
            Health health = indicator.health();

            // Record execution (with null check)
            Counter counter = healthCheckCounters.get(metricName);
            if (counter != null) {
                counter.increment();
            }

            Timer timer = healthCheckTimers.get(metricName);
            if (timer != null) {
                sample.stop(timer);
            }

            // Update status gauge (with null check)
            long statusValue = convertStatusToValue(health.getStatus());
            AtomicLong gauge = healthStatusGauges.get(metricName);
            if (gauge != null) {
                gauge.set(statusValue);
            }

            // Check for status changes and log/alert if necessary
            HealthStatus currentStatus = new HealthStatus(health.getStatus(), startTime, health.getDetails());
            HealthStatus previousStatus = lastHealthStatus.get(metricName);

            if (previousStatus == null || !previousStatus.status.equals(currentStatus.status)) {
                handleHealthStatusChange(metricName, previousStatus, currentStatus);
            }

            lastHealthStatus.put(metricName, currentStatus);

            // Record additional metrics from health details
            recordHealthDetails(metricName, health.getDetails());

        } catch (Exception e) {
            log.error("Error collecting health metric for indicator: {}", indicatorName, e);

            // Record error
            Counter.builder("health.check.errors")
                    .description("Number of health check errors")
                    .tag("indicator", metricName)
                    .tag("error", e.getClass().getSimpleName())
                    .register(meterRegistry)
                    .increment();

            // Set status to unknown (with null check)
            AtomicLong gauge = healthStatusGauges.get(metricName);
            if (gauge != null) {
                gauge.set(-1);
            }
        }
    }

    /**
     * Handle health status changes and trigger alerts if necessary
     */
    private void handleHealthStatusChange(String indicatorName, HealthStatus previous, HealthStatus current) {
        String previousStatusStr = previous != null ? previous.status.getCode() : "UNKNOWN";
        String currentStatusStr = current.status.getCode();

        log.info("Health status changed for {}: {} -> {}",
                indicatorName, previousStatusStr, currentStatusStr);

        // Record status change metric
        Counter.builder("health.check.status.changes")
                .description("Number of health status changes")
                .tag("indicator", indicatorName)
                .tag("from", previousStatusStr)
                .tag("to", currentStatusStr)
                .register(meterRegistry)
                .increment();

        // Trigger alert for critical status changes
        if (current.status.equals(Status.DOWN) || current.status.equals(Status.OUT_OF_SERVICE)) {
            triggerHealthAlert(indicatorName, current);
        }
    }

    /**
     * Trigger health alert for critical status changes
     */
    private void triggerHealthAlert(String indicatorName, HealthStatus status) {
        log.warn("HEALTH ALERT: {} is {}", indicatorName, status.status.getCode());

        // Record alert metric
        Counter.builder("health.alerts.triggered")
                .description("Number of health alerts triggered")
                .tag("indicator", indicatorName)
                .tag("status", status.status.getCode())
                .register(meterRegistry)
                .increment();

        // In a real implementation, this would send alerts via SNS, email, etc.
        // For now, we'll just log and record metrics
    }

    /**
     * Ensure metrics are initialized for the given indicator
     */
    private void ensureMetricsInitialized(String metricName) {
        // Check if metrics already exist
        if (healthCheckCounters.containsKey(metricName)) {
            return;
        }

        // Initialize metrics for this indicator
        synchronized (this) {
            // Double-check locking pattern
            if (healthCheckCounters.containsKey(metricName)) {
                return;
            }

            // Create counter for health check executions
            healthCheckCounters.put(metricName, Counter.builder("health.check.executions")
                    .description("Number of health check executions")
                    .tag("indicator", metricName)
                    .register(meterRegistry));

            // Create timer for health check duration
            healthCheckTimers.put(metricName, Timer.builder("health.check.duration")
                    .description("Health check execution duration")
                    .tag("indicator", metricName)
                    .register(meterRegistry));

            // Create gauge for health status (1 = UP, 0 = DOWN, -1 = UNKNOWN)
            AtomicLong statusValue = new AtomicLong(0);
            healthStatusGauges.put(metricName, statusValue);

            Gauge.builder("health.check.status", statusValue, AtomicLong::get)
                    .description("Health check status (1=UP, 0=DOWN, -1=UNKNOWN)")
                    .tag("indicator", metricName)
                    .register(meterRegistry);

            log.debug("Initialized metrics for health indicator: {}", metricName);
        }
    }

    /**
     * Record additional metrics from health check details
     */
    private void recordHealthDetails(String indicatorName, Map<String, Object> details) {
        if (details == null)
            return;

        // Record response time if available
        Object responseTime = details.get("responseTime");
        if (responseTime instanceof String) {
            try {
                String responseTimeStr = (String) responseTime;
                if (responseTimeStr.endsWith("ms")) {
                    double responseTimeMs = Double.parseDouble(responseTimeStr.replace("ms", ""));

                    Timer.builder("health.check.response.time")
                            .description("Health check response time")
                            .tag("indicator", indicatorName)
                            .register(meterRegistry)
                            .record(Duration.ofMillis((long) responseTimeMs));
                }
            } catch (NumberFormatException e) {
                log.debug("Could not parse response time: {}", responseTime);
            }
        }

        // Record memory usage if available
        Object memory = details.get("memory");
        if (memory instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> memoryDetails = (Map<String, Object>) memory;

            Object usageRatio = memoryDetails.get("usageRatio");
            if (usageRatio instanceof String) {
                try {
                    String usageStr = (String) usageRatio;
                    if (usageStr.endsWith("%")) {
                        double usage = Double.parseDouble(usageStr.replace("%", ""));

                        Gauge.builder("health.system.memory.usage", () -> usage)
                                .description("System memory usage percentage")
                                .tag("indicator", indicatorName)
                                .register(meterRegistry);
                    }
                } catch (NumberFormatException e) {
                    log.debug("Could not parse memory usage: {}", usageRatio);
                }
            }
        }
    }

    /**
     * Convert health status to numeric value for metrics
     */
    private long convertStatusToValue(Status status) {
        if (Status.UP.equals(status)) {
            return 1;
        } else if (Status.DOWN.equals(status) || Status.OUT_OF_SERVICE.equals(status)) {
            return 0;
        } else {
            return -1; // UNKNOWN or other status
        }
    }

    /**
     * Sanitize metric name for use in monitoring systems
     */
    private String sanitizeMetricName(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    /**
     * Internal class to track health status changes
     */
    private static class HealthStatus {
        final Status status;
        final Instant timestamp;
        final Map<String, Object> details;

        HealthStatus(Status status, Instant timestamp, Map<String, Object> details) {
            this.status = status;
            this.timestamp = timestamp;
            this.details = details;
        }
    }
}