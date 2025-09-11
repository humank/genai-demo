package solid.humank.genaidemo.infrastructure.observability.logging;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for log retention optimization
 */
@Configuration
@ConfigurationProperties(prefix = "genai-demo.observability.logging.retention")
public class LogRetentionProperties {

    private boolean optimizationEnabled = true;
    private int highVolumeThreshold = 1000; // logs per minute
    private Map<String, RetentionPolicy> retentionPolicies = Map.of(
            "application", new RetentionPolicy(Duration.ofDays(7), Duration.ofDays(30), Duration.ofDays(90)),
            "security", new RetentionPolicy(Duration.ofDays(30), Duration.ofDays(90), Duration.ofDays(365)),
            "audit", new RetentionPolicy(Duration.ofDays(90), Duration.ofDays(365), Duration.ofDays(2555)),
            "performance", new RetentionPolicy(Duration.ofDays(1), Duration.ofDays(7), Duration.ofDays(30)));

    // Getters and setters
    public boolean isOptimizationEnabled() {
        return optimizationEnabled;
    }

    public void setOptimizationEnabled(boolean optimizationEnabled) {
        this.optimizationEnabled = optimizationEnabled;
    }

    public int getHighVolumeThreshold() {
        return highVolumeThreshold;
    }

    public void setHighVolumeThreshold(int highVolumeThreshold) {
        this.highVolumeThreshold = highVolumeThreshold;
    }

    public Map<String, RetentionPolicy> getRetentionPolicies() {
        return retentionPolicies;
    }

    public void setRetentionPolicies(Map<String, RetentionPolicy> retentionPolicies) {
        this.retentionPolicies = retentionPolicies;
    }

    /**
     * Retention policy for different storage tiers
     */
    public static class RetentionPolicy {
        private Duration hotStorageDuration;
        private Duration warmStorageDuration;
        private Duration coldStorageDuration;

        public RetentionPolicy() {
        }

        public RetentionPolicy(Duration hotStorageDuration, Duration warmStorageDuration,
                Duration coldStorageDuration) {
            this.hotStorageDuration = hotStorageDuration;
            this.warmStorageDuration = warmStorageDuration;
            this.coldStorageDuration = coldStorageDuration;
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
    }
}