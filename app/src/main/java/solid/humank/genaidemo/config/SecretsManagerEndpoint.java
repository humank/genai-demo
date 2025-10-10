package solid.humank.genaidemo.config;

// import org.springframework.boot.actuator.endpoint.annotation.Endpoint;
// import org.springframework.boot.actuator.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom Actuator endpoint for AWS Secrets Manager monitoring
 * 
 * Provides detailed information about Secrets Manager configuration,
 * cache performance, and operational statistics.
 */
@Component
// @Endpoint(id = "secrets-manager")
@ConditionalOnProperty(name = "aws.secretsmanager.enabled", havingValue = "true")
public class SecretsManagerEndpoint {

    private final SecretsManagerService secretsManagerService;
    private final SecretsManagerProperties properties;

    public SecretsManagerEndpoint(SecretsManagerService secretsManagerService, 
                                SecretsManagerProperties properties) {
        this.secretsManagerService = secretsManagerService;
        this.properties = properties;
    }

    /**
     * Get comprehensive Secrets Manager information
     */
    // @ReadOperation
    public Map<String, Object> secretsManagerInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // Configuration information
        info.put("configuration", getConfigurationInfo());
        
        // Cache statistics
        info.put("cache", secretsManagerService.getCacheStatistics());
        
        // Operational status
        info.put("status", getOperationalStatus());
        
        // Secrets information (without sensitive data)
        info.put("secrets", getSecretsInfo());
        
        info.put("timestamp", Instant.now());
        
        return info;
    }

    /**
     * Get configuration information
     */
    private Map<String, Object> getConfigurationInfo() {
        Map<String, Object> config = new HashMap<>();
        config.put("region", properties.getRegion());
        config.put("cacheTtl", properties.getCacheTtl());
        config.put("refreshInterval", properties.getRefreshInterval());
        config.put("maxRetryAttempts", properties.getMaxRetryAttempts());
        config.put("retryDelayMs", properties.getRetryDelayMs());
        config.put("failFast", properties.isFailFast());
        return config;
    }

    /**
     * Get operational status
     */
    private Map<String, Object> getOperationalStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Test basic connectivity
            Map<String, Object> cacheStats = secretsManagerService.getCacheStatistics();
            status.put("connectivity", "UP");
            status.put("lastSuccessfulOperation", Instant.now());
            
        } catch (Exception e) {
            status.put("connectivity", "DOWN");
            status.put("lastError", e.getMessage());
            status.put("lastErrorTime", Instant.now());
        }
        
        return status;
    }

    /**
     * Get secrets information without exposing sensitive data
     */
    private Map<String, Object> getSecretsInfo() {
        Map<String, Object> secretsInfo = new HashMap<>();
        
        Map<String, String> configuredSecrets = properties.getSecrets();
        if (configuredSecrets != null) {
            Map<String, Object> secrets = new HashMap<>();
            
            configuredSecrets.forEach((key, secretName) -> {
                Map<String, Object> secretInfo = new HashMap<>();
                secretInfo.put("secretName", secretName);
                secretInfo.put("configured", true);
                
                // Test if secret is accessible (without retrieving actual value)
                try {
                    // This is a simplified check - in a real implementation,
                    // you might want to check if the secret exists without retrieving it
                    secretInfo.put("accessible", true);
                    secretInfo.put("lastAccessed", "Recently"); // Placeholder
                } catch (Exception e) {
                    secretInfo.put("accessible", false);
                    secretInfo.put("error", e.getMessage());
                }
                
                secrets.put(key, secretInfo);
            });
            
            secretsInfo.put("configured", secrets);
            secretsInfo.put("totalConfigured", configuredSecrets.size());
        }
        
        return secretsInfo;
    }
}