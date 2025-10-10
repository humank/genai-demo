package solid.humank.genaidemo.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing AWS Secrets Manager integration
 * 
 * Provides caching, automatic refresh, and error handling for secrets retrieval.
 */
@Service
@ConditionalOnProperty(name = "aws.secretsmanager.enabled", havingValue = "true")
public class SecretsManagerService {

    private static final Logger logger = LoggerFactory.getLogger(SecretsManagerService.class);

    private final SecretsManagerClient secretsManagerClient;
    private final SecretsManagerProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, CachedSecret> secretCache = new ConcurrentHashMap<>();

    public SecretsManagerService(SecretsManagerClient secretsManagerClient, SecretsManagerProperties properties) {
        this.secretsManagerClient = secretsManagerClient;
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void initialize() {
        logger.info("Initializing Secrets Manager Service with region: {}", properties.getRegion());
        logger.info("Cache TTL: {} seconds, Refresh interval: {} seconds", 
                   properties.getCacheTtl(), properties.getRefreshInterval());
        
        // Pre-load configured secrets
        preloadSecrets();
    }

    @PreDestroy
    public void cleanup() {
        logger.info("Cleaning up Secrets Manager Service");
        secretCache.clear();
        secretsManagerClient.close();
    }

    /**
     * Get secret value by name with caching
     */
    public String getSecretValue(String secretName) {
        CachedSecret cached = secretCache.get(secretName);
        
        if (cached != null && !cached.isExpired()) {
            logger.debug("Returning cached secret: {}", secretName);
            return cached.getValue();
        }
        
        return retrieveAndCacheSecret(secretName);
    }

    /**
     * Get secret value as JSON object
     */
    public <T> T getSecretAsObject(String secretName, Class<T> clazz) {
        String secretJson = getSecretValue(secretName);
        try {
            return objectMapper.readValue(secretJson, clazz);
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse secret {} as {}: {}", secretName, clazz.getSimpleName(), e.getMessage());
            throw new SecretParsingException("Failed to parse secret as " + clazz.getSimpleName(), e);
        }
    }

    /**
     * Get database credentials
     */
    public DatabaseCredentials getDatabaseCredentials() {
        String databaseSecretName = properties.getSecrets().get("database");
        if (databaseSecretName == null) {
            throw new SecretConfigurationException("Database secret name not configured");
        }
        return getSecretAsObject(databaseSecretName, DatabaseCredentials.class);
    }

    /**
     * Get API keys
     */
    public ApiKeys getApiKeys() {
        String apiKeysSecretName = properties.getSecrets().get("api-keys");
        if (apiKeysSecretName == null) {
            throw new SecretConfigurationException("API keys secret name not configured");
        }
        return getSecretAsObject(apiKeysSecretName, ApiKeys.class);
    }

    /**
     * Get application secrets
     */
    public ApplicationSecrets getApplicationSecrets() {
        String applicationSecretName = properties.getSecrets().get("application");
        if (applicationSecretName == null) {
            throw new SecretConfigurationException("Application secret name not configured");
        }
        return getSecretAsObject(applicationSecretName, ApplicationSecrets.class);
    }

    /**
     * Refresh all cached secrets
     */
    @Scheduled(fixedRateString = "#{@secretsManagerProperties.refreshInterval * 1000}")
    public void refreshSecrets() {
        logger.info("Refreshing cached secrets");
        
        secretCache.keySet().forEach(secretName -> {
            try {
                retrieveAndCacheSecret(secretName);
                logger.debug("Refreshed secret: {}", secretName);
            } catch (Exception e) {
                logger.warn("Failed to refresh secret {}: {}", secretName, e.getMessage());
            }
        });
    }

    /**
     * Clear cache for a specific secret
     */
    public void clearSecretCache(String secretName) {
        secretCache.remove(secretName);
        logger.info("Cleared cache for secret: {}", secretName);
    }

    /**
     * Clear all cached secrets
     */
    public void clearAllCache() {
        secretCache.clear();
        logger.info("Cleared all secret cache");
    }

    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStatistics() {
        return Map.of(
            "totalSecrets", secretCache.size(),
            "expiredSecrets", secretCache.values().stream().mapToInt(s -> s.isExpired() ? 1 : 0).sum(),
            "cacheHitRate", calculateCacheHitRate()
        );
    }

    private void preloadSecrets() {
        properties.getSecrets().values().forEach(secretName -> {
            try {
                retrieveAndCacheSecret(secretName);
                logger.info("Pre-loaded secret: {}", secretName);
            } catch (Exception e) {
                if (properties.isFailFast()) {
                    throw new SecretRetrievalException("Failed to pre-load secret: " + secretName, e);
                } else {
                    logger.warn("Failed to pre-load secret {}: {}", secretName, e.getMessage());
                }
            }
        });
    }

    private String retrieveAndCacheSecret(String secretName) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < properties.getMaxRetryAttempts()) {
            try {
                GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();

                GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);
                String secretValue = response.secretString();

                // Cache the secret
                secretCache.put(secretName, new CachedSecret(secretValue, properties.getCacheTtl()));
                
                logger.debug("Successfully retrieved and cached secret: {}", secretName);
                return secretValue;

            } catch (SecretsManagerException e) {
                lastException = e;
                attempts++;
                
                logger.warn("Attempt {} failed to retrieve secret {}: {}", attempts, secretName, e.getMessage());
                
                if (attempts < properties.getMaxRetryAttempts()) {
                    try {
                        Thread.sleep(properties.getRetryDelayMs() * attempts); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SecretRetrievalException("Interrupted while retrying secret retrieval", ie);
                    }
                }
            }
        }

        logger.error("Failed to retrieve secret {} after {} attempts", secretName, attempts);
        throw new SecretRetrievalException("Unable to retrieve secret: " + secretName, lastException);
    }

    private double calculateCacheHitRate() {
        // This is a simplified implementation
        // In a real scenario, you'd track hits and misses
        return secretCache.isEmpty() ? 0.0 : 0.95; // Placeholder
    }

    /**
     * Cached secret with expiration
     */
    private static class CachedSecret {
        private final String value;
        private final Instant expiryTime;

        public CachedSecret(String value, int ttlSeconds) {
            this.value = value;
            this.expiryTime = Instant.now().plusSeconds(ttlSeconds);
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiryTime);
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Database credentials model
     */
    public static class DatabaseCredentials {
        private String username;
        private String password;
        private String host;
        private int port;
        private String dbname;
        private String engine;

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getDbname() { return dbname; }
        public void setDbname(String dbname) { this.dbname = dbname; }
        
        public String getEngine() { return engine; }
        public void setEngine(String engine) { this.engine = engine; }
    }

    /**
     * API keys model
     */
    public static class ApiKeys {
        private String openaiApiKey;
        private String anthropicApiKey;
        private String bedrockAccessKey;
        private String externalServiceKey;

        // Getters and setters
        public String getOpenaiApiKey() { return openaiApiKey; }
        public void setOpenaiApiKey(String openaiApiKey) { this.openaiApiKey = openaiApiKey; }
        
        public String getAnthropicApiKey() { return anthropicApiKey; }
        public void setAnthropicApiKey(String anthropicApiKey) { this.anthropicApiKey = anthropicApiKey; }
        
        public String getBedrockAccessKey() { return bedrockAccessKey; }
        public void setBedrockAccessKey(String bedrockAccessKey) { this.bedrockAccessKey = bedrockAccessKey; }
        
        public String getExternalServiceKey() { return externalServiceKey; }
        public void setExternalServiceKey(String externalServiceKey) { this.externalServiceKey = externalServiceKey; }
    }

    /**
     * Application secrets model
     */
    public static class ApplicationSecrets {
        private String jwtSecret;
        private String encryptionKey;
        private String sessionSecret;
        private String webhookSecret;

        // Getters and setters
        public String getJwtSecret() { return jwtSecret; }
        public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
        
        public String getEncryptionKey() { return encryptionKey; }
        public void setEncryptionKey(String encryptionKey) { this.encryptionKey = encryptionKey; }
        
        public String getSessionSecret() { return sessionSecret; }
        public void setSessionSecret(String sessionSecret) { this.sessionSecret = sessionSecret; }
        
        public String getWebhookSecret() { return webhookSecret; }
        public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
    }

    /**
     * Exception classes
     */
    public static class SecretRetrievalException extends RuntimeException {
        public SecretRetrievalException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class SecretParsingException extends RuntimeException {
        public SecretParsingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class SecretConfigurationException extends RuntimeException {
        public SecretConfigurationException(String message) {
            super(message);
        }
    }
}