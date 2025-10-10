package solid.humank.genaidemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Map;

/**
 * Configuration properties for AWS Secrets Manager
 */
@ConfigurationProperties(prefix = "aws.secretsmanager")
@Validated
public class SecretsManagerProperties {

    /**
     * Whether Secrets Manager is enabled
     */
    private boolean enabled = false;

    /**
     * AWS region for Secrets Manager
     */
    @NotBlank
    private String region = "ap-east-2";

    /**
     * Cache TTL in seconds
     */
    @Positive
    private int cacheTtl = 300; // 5 minutes

    /**
     * Refresh interval in seconds
     */
    @Positive
    private int refreshInterval = 3600; // 1 hour

    /**
     * Map of secret names for different types
     */
    @NotNull
    private Map<String, String> secrets = Map.of();

    /**
     * Whether to fail fast on secret retrieval errors
     */
    private boolean failFast = true;

    /**
     * Maximum retry attempts for secret retrieval
     */
    @Positive
    private int maxRetryAttempts = 3;

    /**
     * Retry delay in milliseconds
     */
    @Positive
    private long retryDelayMs = 1000;

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public int getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(int cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public int getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(int refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public Map<String, String> getSecrets() {
        return secrets;
    }

    public void setSecrets(Map<String, String> secrets) {
        this.secrets = secrets;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }
}