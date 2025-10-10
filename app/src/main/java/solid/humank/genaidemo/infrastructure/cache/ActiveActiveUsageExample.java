package solid.humank.genaidemo.infrastructure.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Active-Active Redis Usage Example
 * 
 * Demonstrates how to use Redis in Active-Active multi-region setup:
 * 
 * Key Principles:
 * 1. Write to local region for best performance
 * 2. Read from local region (Global Datastore ensures data availability)
 * 3. Let Global Datastore handle cross-region synchronization automatically
 * 4. Application code is identical across all regions
 * 
 * This example shows typical usage patterns for:
 * - Session management
 * - Caching business data
 * - Distributed counters
 * - Feature flags
 */
@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class ActiveActiveUsageExample {

    private final ActiveActiveRedisService redisService;

    @Autowired
    public ActiveActiveUsageExample(ActiveActiveRedisService redisService) {
        this.redisService = redisService;
    }

    /**
     * Example: Session Management in Active-Active Mode
     * 
     * Sessions are stored in local region and automatically replicated
     * to other regions via Global Datastore
     */
    public void manageUserSession(String userId, Object sessionData) {
        String sessionKey = "session:" + userId;

        // Store session in local region (will be replicated automatically)
        redisService.put(sessionKey, sessionData, Duration.ofHours(24));

        // Read session from local region (available due to Global Datastore)
        Object retrievedSession = redisService.get(sessionKey, Object.class);

        // Session is available in all regions without additional code
    }

    /**
     * Example: Business Data Caching
     * 
     * Cache business data locally, Global Datastore ensures consistency
     */
    public void cacheBusinessData(String customerId, CustomerData customerData) {
        String cacheKey = "customer:" + customerId;

        // Cache in local region with TTL
        redisService.put(cacheKey, customerData, Duration.ofMinutes(30));

        // Data is automatically available in other regions
        // No need for manual cross-region synchronization
    }

    /**
     * Example: Distributed Counters
     * 
     * Counters work across regions with eventual consistency
     */
    public void trackPageViews(String pageId) {
        String counterKey = "pageviews:" + pageId;

        // Increment in local region
        long currentCount = redisService.increment(counterKey);

        // Set expiration for daily counters
        redisService.expire(counterKey, Duration.ofDays(1));

        // Count is eventually consistent across all regions
    }

    /**
     * Example: Feature Flags
     * 
     * Feature flags are stored locally and replicated globally
     */
    public void setFeatureFlag(String flagName, boolean enabled) {
        String flagKey = "feature:" + flagName;

        // Set feature flag in local region
        redisService.putString(flagKey, String.valueOf(enabled));

        // Flag is automatically available in all regions
    }

    public boolean isFeatureEnabled(String flagName) {
        String flagKey = "feature:" + flagName;

        // Read from local region (Global Datastore ensures availability)
        String flagValue = redisService.getString(flagKey);
        return Boolean.parseBoolean(flagValue);
    }

    /**
     * Example: Health Check Across Regions
     * 
     * Check local Redis health - each region checks its own Redis
     */
    public HealthStatus checkHealth() {
        boolean localHealthy = redisService.isHealthy();
        String currentRegion = redisService.getCurrentRegion();

        return new HealthStatus(currentRegion, localHealthy, redisService.isMultiRegionEnabled());
    }

    /**
     * Example: Cache Invalidation
     * 
     * Invalidate cache in local region, Global Datastore propagates deletion
     */
    public void invalidateCache(String cacheKey) {
        // Delete from local region
        boolean deleted = redisService.delete(cacheKey);

        // Deletion is automatically propagated to other regions
        // No need for manual cross-region invalidation
    }

    /**
     * Health Status Data Class
     */
    public static class HealthStatus {
        private final String region;
        private final boolean healthy;
        private final boolean multiRegionEnabled;

        public HealthStatus(String region, boolean healthy, boolean multiRegionEnabled) {
            this.region = region;
            this.healthy = healthy;
            this.multiRegionEnabled = multiRegionEnabled;
        }

        // Getters
        public String getRegion() {
            return region;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public boolean isMultiRegionEnabled() {
            return multiRegionEnabled;
        }
    }

    /**
     * Customer Data Example Class
     */
    public static class CustomerData {
        private String customerId;
        private String name;
        private String email;

        // Constructors, getters, setters
        public CustomerData() {
        }

        public CustomerData(String customerId, String name, String email) {
            this.customerId = customerId;
            this.name = name;
            this.email = email;
        }

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}