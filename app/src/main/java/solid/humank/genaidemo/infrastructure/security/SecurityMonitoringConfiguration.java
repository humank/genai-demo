package solid.humank.genaidemo.infrastructure.security;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Security monitoring configuration and metrics collection
 * Implements requirement 11.5: IF unauthorized access is attempted THEN the
 * system SHALL log security events and alert administrators
 */
@Configuration
@ConfigurationProperties(prefix = "genai-demo.security.monitoring")
// @EnableScheduling // 禁用定時任務以避免記憶體問題
public class SecurityMonitoringConfiguration {

    private boolean enabled = true;
    private Duration alertThreshold = Duration.ofMinutes(5);
    private int maxFailedAttempts = 5;
    private Duration lockoutDuration = Duration.ofMinutes(15);
    private boolean realTimeAlerting = true;

    @Bean
    public SecurityMetricsCollector securityMetricsCollector(MeterRegistry meterRegistry) {
        return new SecurityMetricsCollector(meterRegistry);
    }

    @Bean
    public SecurityHealthIndicator securityHealthIndicator(SecurityMetricsCollector metricsCollector) {
        return new SecurityHealthIndicator(metricsCollector);
    }

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getAlertThreshold() {
        return alertThreshold;
    }

    public void setAlertThreshold(Duration alertThreshold) {
        this.alertThreshold = alertThreshold;
    }

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public void setMaxFailedAttempts(int maxFailedAttempts) {
        this.maxFailedAttempts = maxFailedAttempts;
    }

    public Duration getLockoutDuration() {
        return lockoutDuration;
    }

    public void setLockoutDuration(Duration lockoutDuration) {
        this.lockoutDuration = lockoutDuration;
    }

    public boolean isRealTimeAlerting() {
        return realTimeAlerting;
    }

    public void setRealTimeAlerting(boolean realTimeAlerting) {
        this.realTimeAlerting = realTimeAlerting;
    }
}

/**
 * Security metrics collector for monitoring security events
 */
@Component
class SecurityMetricsCollector {

    private final MeterRegistry meterRegistry;

    // Counters for different security events
    private final Counter authenticationSuccessCounter;
    private final Counter authenticationFailureCounter;
    private final Counter authorizationDeniedCounter;
    private final Counter suspiciousActivityCounter;
    private final Counter piiAccessCounter;
    private final Counter configurationChangeCounter;

    // Timers for security operations
    private final Timer authenticationTimer;
    private final Timer authorizationTimer;

    // In-memory tracking for alerting
    private final Map<String, AtomicInteger> failedAttemptsByIp = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastFailureByIp = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lockoutByIp = new ConcurrentHashMap<>();

    // Security health metrics
    private final AtomicLong totalSecurityEvents = new AtomicLong(0);
    private final AtomicLong highSeverityEvents = new AtomicLong(0);
    private final AtomicLong criticalSeverityEvents = new AtomicLong(0);

    public SecurityMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize counters
        this.authenticationSuccessCounter = Counter.builder("security.authentication.success")
                .description("Number of successful authentication attempts")
                .register(meterRegistry);

        this.authenticationFailureCounter = Counter.builder("security.authentication.failure")
                .description("Number of failed authentication attempts")
                .register(meterRegistry);

        this.authorizationDeniedCounter = Counter.builder("security.authorization.denied")
                .description("Number of authorization denied events")
                .register(meterRegistry);

        this.suspiciousActivityCounter = Counter.builder("security.suspicious.activity")
                .description("Number of suspicious activity events")
                .register(meterRegistry);

        this.piiAccessCounter = Counter.builder("security.pii.access")
                .description("Number of PII access events")
                .register(meterRegistry);

        this.configurationChangeCounter = Counter.builder("security.configuration.change")
                .description("Number of configuration change events")
                .register(meterRegistry);

        // Initialize timers
        this.authenticationTimer = Timer.builder("security.authentication.duration")
                .description("Time taken for authentication operations")
                .register(meterRegistry);

        this.authorizationTimer = Timer.builder("security.authorization.duration")
                .description("Time taken for authorization operations")
                .register(meterRegistry);

        // Register gauges for health metrics
        meterRegistry.gauge("security.events.total", totalSecurityEvents);
        meterRegistry.gauge("security.events.high_severity", highSeverityEvents);
        meterRegistry.gauge("security.events.critical_severity", criticalSeverityEvents);
        meterRegistry.gauge("security.failed_attempts.active_ips", failedAttemptsByIp, Map::size);
        meterRegistry.gauge("security.lockouts.active", lockoutByIp, Map::size);
    }

    public void recordAuthenticationSuccess(String sourceIp) {
        authenticationSuccessCounter.increment();
        totalSecurityEvents.incrementAndGet();

        // Clear failed attempts on successful authentication
        failedAttemptsByIp.remove(sourceIp);
        lastFailureByIp.remove(sourceIp);
        lockoutByIp.remove(sourceIp);
    }

    public void recordAuthenticationFailure(String sourceIp) {
        authenticationFailureCounter.increment();
        totalSecurityEvents.incrementAndGet();

        // Track failed attempts by IP
        if (sourceIp != null && !sourceIp.equals("unknown")) {
            AtomicInteger attempts = failedAttemptsByIp.computeIfAbsent(sourceIp, k -> new AtomicInteger(0));
            int currentAttempts = attempts.incrementAndGet();
            lastFailureByIp.put(sourceIp, LocalDateTime.now());

            // Check for brute force attack
            if (currentAttempts >= 5) { // Configurable threshold
                lockoutByIp.put(sourceIp, LocalDateTime.now());
                recordSuspiciousActivity("BRUTE_FORCE_ATTACK", sourceIp);
            }
        }
    }

    public void recordAuthorizationDenied(String sourceIp) {
        authorizationDeniedCounter.increment();
        totalSecurityEvents.incrementAndGet();
    }

    public void recordSuspiciousActivity(String activityType, String sourceIp) {
        suspiciousActivityCounter.increment();
        totalSecurityEvents.incrementAndGet();
        highSeverityEvents.incrementAndGet();

        // Tag with activity type for better monitoring
        Counter.builder("security.suspicious.activity.by_type")
                .tag("activity_type", activityType)
                .description("Suspicious activity events by type")
                .register(meterRegistry)
                .increment();
    }

    public void recordPiiAccess(String piiType, String operation) {
        piiAccessCounter.increment();
        totalSecurityEvents.incrementAndGet();
        highSeverityEvents.incrementAndGet();

        // Tag with PII type and operation
        Counter.builder("security.pii.access.by_type")
                .tag("pii_type", piiType)
                .tag("operation", operation)
                .description("PII access events by type and operation")
                .register(meterRegistry)
                .increment();
    }

    public void recordConfigurationChange(String configType) {
        configurationChangeCounter.increment();
        totalSecurityEvents.incrementAndGet();

        // Tag with configuration type
        Counter.builder("security.configuration.change.by_type")
                .tag("config_type", configType)
                .description("Configuration change events by type")
                .register(meterRegistry)
                .increment();
    }

    public void recordCriticalSecurityEvent() {
        criticalSeverityEvents.incrementAndGet();
        totalSecurityEvents.incrementAndGet();
    }

    public Timer.Sample startAuthenticationTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopAuthenticationTimer(Timer.Sample sample) {
        sample.stop(authenticationTimer);
    }

    public Timer.Sample startAuthorizationTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopAuthorizationTimer(Timer.Sample sample) {
        sample.stop(authorizationTimer);
    }

    public boolean isIpLockedOut(String sourceIp) {
        LocalDateTime lockoutTime = lockoutByIp.get(sourceIp);
        if (lockoutTime == null) {
            return false;
        }

        // Check if lockout has expired (15 minutes default)
        if (lockoutTime.isBefore(LocalDateTime.now().minusMinutes(15))) {
            lockoutByIp.remove(sourceIp);
            failedAttemptsByIp.remove(sourceIp);
            return false;
        }

        return true;
    }

    public int getFailedAttempts(String sourceIp) {
        AtomicInteger attempts = failedAttemptsByIp.get(sourceIp);
        return attempts != null ? attempts.get() : 0;
    }

    // Manual cleanup of old entries (原定期任務已移除)
    public void cleanupOldEntries() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);

        // Clean up old failed attempts
        lastFailureByIp.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));

        // Clean up expired lockouts
        LocalDateTime lockoutCutoff = LocalDateTime.now().minusMinutes(15);
        lockoutByIp.entrySet().removeIf(entry -> entry.getValue().isBefore(lockoutCutoff));

        // Clean up failed attempts for IPs that are no longer tracked
        failedAttemptsByIp.keySet().removeIf(ip -> !lastFailureByIp.containsKey(ip));
    }

    // Getters for health check
    public long getTotalSecurityEvents() {
        return totalSecurityEvents.get();
    }

    public long getHighSeverityEvents() {
        return highSeverityEvents.get();
    }

    public long getCriticalSeverityEvents() {
        return criticalSeverityEvents.get();
    }

    public int getActiveFailedAttempts() {
        return failedAttemptsByIp.size();
    }

    public int getActiveLockouts() {
        return lockoutByIp.size();
    }
}

/**
 * Security health indicator for monitoring security system health
 */
@Component
class SecurityHealthIndicator implements HealthIndicator {

    private final SecurityMetricsCollector metricsCollector;

    public SecurityHealthIndicator(SecurityMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();

        // Check critical security events
        long criticalEvents = metricsCollector.getCriticalSeverityEvents();
        long highSeverityEvents = metricsCollector.getHighSeverityEvents();
        int activeLockouts = metricsCollector.getActiveLockouts();
        int activeFailedAttempts = metricsCollector.getActiveFailedAttempts();

        // Determine health status
        if (criticalEvents > 10) { // More than 10 critical events
            builder.down()
                    .withDetail("status", "CRITICAL")
                    .withDetail("reason", "Too many critical security events");
        } else if (highSeverityEvents > 50 || activeLockouts > 20) {
            builder.down()
                    .withDetail("status", "WARNING")
                    .withDetail("reason", "High number of security events or lockouts");
        } else if (activeFailedAttempts > 100) {
            builder.outOfService()
                    .withDetail("status", "DEGRADED")
                    .withDetail("reason", "High number of failed authentication attempts");
        } else {
            builder.up()
                    .withDetail("status", "HEALTHY");
        }

        // Add security metrics to health details
        builder.withDetail("totalSecurityEvents", metricsCollector.getTotalSecurityEvents())
                .withDetail("criticalEvents", criticalEvents)
                .withDetail("highSeverityEvents", highSeverityEvents)
                .withDetail("activeLockouts", activeLockouts)
                .withDetail("activeFailedAttempts", activeFailedAttempts)
                .withDetail("lastCheck", LocalDateTime.now().toString());

        return builder.build();
    }
}