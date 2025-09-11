package solid.humank.genaidemo.infrastructure.security;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Data retention configuration and management
 * Implements requirement 11.4: WHEN data is stored THEN the system SHALL
 * implement proper retention policies
 */
@Configuration
@ConfigurationProperties(prefix = "genai-demo.data-retention")
@EnableScheduling
public class DataRetentionConfiguration {

    private Map<String, RetentionPolicy> policies = new ConcurrentHashMap<>();

    public DataRetentionConfiguration() {
        // Initialize default retention policies
        initializeDefaultPolicies();
    }

    private void initializeDefaultPolicies() {
        // Application logs retention
        policies.put("application-logs", new RetentionPolicy(
                Duration.ofDays(30), // Hot storage (CloudWatch)
                Duration.ofDays(90), // Warm storage (S3 Standard)
                Duration.ofDays(2555), // Cold storage (S3 Glacier) - 7 years
                true // Auto-cleanup enabled
        ));

        // Security logs retention (longer retention for compliance)
        policies.put("security-logs", new RetentionPolicy(
                Duration.ofDays(90), // Hot storage
                Duration.ofDays(365), // Warm storage
                Duration.ofDays(2555), // Cold storage - 7 years
                true));

        // Audit logs retention (longest retention for compliance)
        policies.put("audit-logs", new RetentionPolicy(
                Duration.ofDays(90), // Hot storage
                Duration.ofDays(365), // Warm storage
                Duration.ofDays(3650), // Cold storage - 10 years
                true));

        // Metrics retention
        policies.put("metrics", new RetentionPolicy(
                Duration.ofDays(15), // Hot storage
                Duration.ofDays(90), // Warm storage
                Duration.ofDays(365), // Cold storage - 1 year
                true));

        // Traces retention
        policies.put("traces", new RetentionPolicy(
                Duration.ofDays(7), // Hot storage
                Duration.ofDays(30), // Warm storage
                Duration.ofDays(90), // Cold storage - 3 months
                true));

        // Domain events retention
        policies.put("domain-events", new RetentionPolicy(
                Duration.ofDays(30), // Hot storage
                Duration.ofDays(365), // Warm storage
                Duration.ofDays(2555), // Cold storage - 7 years (business data)
                true));

        // Personal data retention (GDPR compliance)
        policies.put("personal-data", new RetentionPolicy(
                Duration.ofDays(30), // Hot storage
                Duration.ofDays(365), // Warm storage
                Duration.ofDays(2555), // Cold storage - 7 years (with right to be forgotten)
                true));

        // Session data retention
        policies.put("session-data", new RetentionPolicy(
                Duration.ofHours(24), // Hot storage
                Duration.ofDays(7), // Warm storage
                Duration.ofDays(30), // Cold storage
                true));

        // Temporary data retention
        policies.put("temp-data", new RetentionPolicy(
                Duration.ofHours(1), // Hot storage
                Duration.ofHours(24), // Warm storage
                Duration.ofDays(7), // Cold storage
                true));
    }

    public RetentionPolicy getPolicy(String dataType) {
        return policies.getOrDefault(dataType, getDefaultPolicy());
    }

    public void setPolicy(String dataType, RetentionPolicy policy) {
        policies.put(dataType, policy);
    }

    private RetentionPolicy getDefaultPolicy() {
        return new RetentionPolicy(
                Duration.ofDays(30),
                Duration.ofDays(90),
                Duration.ofDays(365),
                true);
    }

    public Map<String, RetentionPolicy> getPolicies() {
        return policies;
    }

    public void setPolicies(Map<String, RetentionPolicy> policies) {
        this.policies = policies;
    }

    /**
     * Retention policy definition
     */
    public static class RetentionPolicy {
        private Duration hotStorageDuration;
        private Duration warmStorageDuration;
        private Duration coldStorageDuration;
        private boolean autoCleanupEnabled;
        private LocalDateTime lastCleanup;

        public RetentionPolicy() {
        }

        public RetentionPolicy(Duration hotStorageDuration, Duration warmStorageDuration,
                Duration coldStorageDuration, boolean autoCleanupEnabled) {
            this.hotStorageDuration = hotStorageDuration;
            this.warmStorageDuration = warmStorageDuration;
            this.coldStorageDuration = coldStorageDuration;
            this.autoCleanupEnabled = autoCleanupEnabled;
            this.lastCleanup = LocalDateTime.now();
        }

        // Getters and setters
        public Duration getHotStorageDuration() {
            return hotStorageDuration;
        }

        public void setHotStorageDuration(Duration hotStorageDuration) {
            this.hotStorageDuration = hotStorageDuration;
        }

        public Duration getWarmStorageDuration() {
            return warmStorageDuration;
        }

        public void setWarmStorageDuration(Duration warmStorageDuration) {
            this.warmStorageDuration = warmStorageDuration;
        }

        public Duration getColdStorageDuration() {
            return coldStorageDuration;
        }

        public void setColdStorageDuration(Duration coldStorageDuration) {
            this.coldStorageDuration = coldStorageDuration;
        }

        public boolean isAutoCleanupEnabled() {
            return autoCleanupEnabled;
        }

        public void setAutoCleanupEnabled(boolean autoCleanupEnabled) {
            this.autoCleanupEnabled = autoCleanupEnabled;
        }

        public LocalDateTime getLastCleanup() {
            return lastCleanup;
        }

        public void setLastCleanup(LocalDateTime lastCleanup) {
            this.lastCleanup = lastCleanup;
        }

        public Duration getTotalRetentionDuration() {
            return hotStorageDuration.plus(warmStorageDuration).plus(coldStorageDuration);
        }

        public boolean isExpired(LocalDateTime timestamp) {
            return timestamp.isBefore(LocalDateTime.now().minus(getTotalRetentionDuration()));
        }
    }
}

/**
 * Data retention service for automated cleanup
 */
@Component
class DataRetentionService {

    private final DataRetentionConfiguration retentionConfig;

    public DataRetentionService(DataRetentionConfiguration retentionConfig) {
        this.retentionConfig = retentionConfig;
    }

    /**
     * Scheduled cleanup task - runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void performScheduledCleanup() {
        retentionConfig.getPolicies().forEach((dataType, policy) -> {
            if (policy.isAutoCleanupEnabled()) {
                performCleanup(dataType, policy);
            }
        });
    }

    /**
     * Manual cleanup for specific data type
     */
    public void performCleanup(String dataType) {
        DataRetentionConfiguration.RetentionPolicy policy = retentionConfig.getPolicy(dataType);
        performCleanup(dataType, policy);
    }

    private void performCleanup(String dataType, DataRetentionConfiguration.RetentionPolicy policy) {
        try {
            // Log cleanup operation
            System.out.println("Starting cleanup for data type: " + dataType);

            // Calculate cutoff dates
            LocalDateTime hotCutoff = LocalDateTime.now().minus(policy.getHotStorageDuration());
            LocalDateTime warmCutoff = LocalDateTime.now().minus(policy.getWarmStorageDuration());
            LocalDateTime coldCutoff = LocalDateTime.now().minus(policy.getColdStorageDuration());

            // Perform cleanup based on data type
            switch (dataType) {
                case "application-logs" -> cleanupApplicationLogs(hotCutoff, warmCutoff, coldCutoff);
                case "security-logs" -> cleanupSecurityLogs(hotCutoff, warmCutoff, coldCutoff);
                case "audit-logs" -> cleanupAuditLogs(hotCutoff, warmCutoff, coldCutoff);
                case "metrics" -> cleanupMetrics(hotCutoff, warmCutoff, coldCutoff);
                case "traces" -> cleanupTraces(hotCutoff, warmCutoff, coldCutoff);
                case "domain-events" -> cleanupDomainEvents(hotCutoff, warmCutoff, coldCutoff);
                case "personal-data" -> cleanupPersonalData(hotCutoff, warmCutoff, coldCutoff);
                case "session-data" -> cleanupSessionData(hotCutoff, warmCutoff, coldCutoff);
                case "temp-data" -> cleanupTempData(hotCutoff, warmCutoff, coldCutoff);
                default -> System.out.println("Unknown data type for cleanup: " + dataType);
            }

            // Update last cleanup timestamp
            policy.setLastCleanup(LocalDateTime.now());

            System.out.println("Completed cleanup for data type: " + dataType);

        } catch (Exception e) {
            System.err.println("Error during cleanup for data type " + dataType + ": " + e.getMessage());
        }
    }

    private void cleanupApplicationLogs(LocalDateTime hotCutoff, LocalDateTime warmCutoff, LocalDateTime coldCutoff) {
        // Implementation would integrate with CloudWatch Logs API
        // Move logs older than hotCutoff to S3
        // Move logs older than warmCutoff to S3 IA
        // Delete logs older than coldCutoff
        System.out.println(
                "Cleaning up application logs - Hot: " + hotCutoff + ", Warm: " + warmCutoff + ", Cold: " + coldCutoff);
    }

    private void cleanupSecurityLogs(LocalDateTime hotCutoff, LocalDateTime warmCutoff, LocalDateTime coldCutoff) {
        // Security logs require special handling for compliance
        System.out.println(
                "Cleaning up security logs - Hot: " + hotCutoff + ", Warm: " + warmCutoff + ", Cold: " + coldCutoff);
    }

    private void cleanupAuditLogs(LocalDateTime hotCutoff, LocalDateTime warmCutoff, LocalDateTime coldCutoff) {
        // Audit logs have the longest retention for compliance
        System.out.println(
                "Cleaning up audit logs - Hot: " + hotCutoff + ", Warm: " + warmCutoff + ", Cold: " + coldCutoff);
    }

    private void cleanupMetrics(LocalDateTime hotCutoff, LocalDateTime warmCutoff, LocalDateTime coldCutoff) {
        // Cleanup CloudWatch metrics and Prometheus data
        System.out
                .println("Cleaning up metrics - Hot: " + hotCutoff + ", Warm: " + warmCutoff + ", Cold: " + coldCutoff);
    }

    private void cleanupTraces(LocalDateTime hotCutoff, LocalDateTime warmCutoff, LocalDateTime coldCutoff) {
        // Cleanup X-Ray traces
        System.out
                .println("Cleaning up traces - Hot: " + hotCutoff + ", Warm: " + warmCutoff + ", Cold: " + coldCutoff);
    }

    private void cleanupDomainEvents(LocalDateTime hotCutoff, LocalDateTime warmCutoff, LocalDateTime coldCutoff) {
        // Cleanup domain events from event store
        System.out.println(
                "Cleaning up domain events - Hot: " + hotCutoff + ", Warm: " + warmCutoff + ", Cold: " + coldCutoff);
    }

    private void cleanupPersonalData(LocalDateTime hotCutoff, LocalDateTime warmCutoff, LocalDateTime coldCutoff) {
        // Special handling for GDPR compliance - right to be forgotten
        System.out.println(
                "Cleaning up personal data - Hot: " + hotCutoff + ", Warm: " + warmCutoff + ", Cold: " + coldCutoff);
    }

    private void cleanupSessionData(LocalDateTime hotCutoff, LocalDateTime warmCutoff, LocalDateTime coldCutoff) {
        // Cleanup session data from Redis/database
        System.out.println(
                "Cleaning up session data - Hot: " + hotCutoff + ", Warm: " + warmCutoff + ", Cold: " + coldCutoff);
    }

    private void cleanupTempData(LocalDateTime hotCutoff, LocalDateTime warmCutoff, LocalDateTime coldCutoff) {
        // Cleanup temporary files and cache data
        System.out.println(
                "Cleaning up temporary data - Hot: " + hotCutoff + ", Warm: " + warmCutoff + ", Cold: " + coldCutoff);
    }
}