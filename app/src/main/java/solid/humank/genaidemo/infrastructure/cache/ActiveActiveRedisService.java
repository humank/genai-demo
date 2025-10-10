package solid.humank.genaidemo.infrastructure.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Active-Active Redis Service
 * 
 * Provides Redis operations optimized for Active-Active multi-region setup:
 * - Local-first strategy for optimal performance
 * - Global Datastore handles cross-region synchronization automatically
 * - Simple interface that abstracts regional complexity
 * 
 * Key Principles:
 * 1. Always write to local region for best performance
 * 2. Read from local region (Global Datastore ensures consistency)
 * 3. Let infrastructure handle cross-region synchronization
 * 
 * Requirements: 4.4 - Cross-region cache synchronization in Active-Active mode
 */
@Service
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class ActiveActiveRedisService {

    private static final Logger logger = LoggerFactory.getLogger(ActiveActiveRedisService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, String> customStringRedisTemplate;

    @Value("${aws.region:local}")
    private String currentRegion;

    @Value("${redis.multiregion.enabled:false}")
    private boolean multiRegionEnabled;

    @Autowired
    public ActiveActiveRedisService(RedisTemplate<String, Object> redisTemplate,
                                   @Qualifier("customStringRedisTemplate") RedisTemplate<String, String> customStringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.customStringRedisTemplate = customStringRedisTemplate;
    }

    /**
     * Store object in cache with TTL
     * Always writes to local region for optimal performance
     */
    public void put(String key, Object value, Duration ttl) {
        logger.debug("Storing key '{}' in region '{}' with TTL {}", key, currentRegion, ttl);
        redisTemplate.opsForValue().set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Store object in cache without TTL
     * Always writes to local region for optimal performance
     */
    public void put(String key, Object value) {
        logger.debug("Storing key '{}' in region '{}'", key, currentRegion);
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * Get object from cache
     * Reads from local region (Global Datastore ensures data availability)
     */
    public <T> T get(String key, Class<T> type) {
        logger.debug("Reading key '{}' from region '{}'", key, currentRegion);
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    /**
     * Store string in cache with TTL
     */
    public void putString(String key, String value, Duration ttl) {
        logger.debug("Storing string key '{}' in region '{}' with TTL {}", key, currentRegion, ttl);
        customStringRedisTemplate.opsForValue().set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Store string in cache without TTL
     */
    public void putString(String key, String value) {
        logger.debug("Storing string key '{}' in region '{}'", key, currentRegion);
        customStringRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * Get string from cache
     */
    public String getString(String key) {
        logger.debug("Reading string key '{}' from region '{}'", key, currentRegion);
        return customStringRedisTemplate.opsForValue().get(key);
    }

    /**
     * Check if key exists
     */
    public boolean exists(String key) {
        Boolean exists = redisTemplate.hasKey(key);
        logger.debug("Key '{}' exists in region '{}': {}", key, currentRegion, exists);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Delete key from cache
     * Deletes from local region, Global Datastore will sync deletion
     */
    public boolean delete(String key) {
        logger.debug("Deleting key '{}' from region '{}'", key, currentRegion);
        Boolean deleted = redisTemplate.delete(key);
        return Boolean.TRUE.equals(deleted);
    }

    /**
     * Set TTL for existing key
     */
    public boolean expire(String key, Duration ttl) {
        logger.debug("Setting TTL for key '{}' in region '{}': {}", key, currentRegion, ttl);
        Boolean result = redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * Increment counter
     * Uses local region for atomic operations
     */
    public long increment(String key) {
        logger.debug("Incrementing counter '{}' in region '{}'", key, currentRegion);
        Long result = customStringRedisTemplate.opsForValue().increment(key);
        return result != null ? result : 0;
    }

    /**
     * Increment counter by delta
     */
    public long increment(String key, long delta) {
        logger.debug("Incrementing counter '{}' by {} in region '{}'", key, delta, currentRegion);
        Long result = customStringRedisTemplate.opsForValue().increment(key, delta);
        return result != null ? result : 0;
    }

    /**
     * Health check for local Redis connection
     */
    public boolean isHealthy() {
        try {
            customStringRedisTemplate.opsForValue().set("health-check-" + currentRegion, "ok", 1, TimeUnit.SECONDS);
            String result = customStringRedisTemplate.opsForValue().get("health-check-" + currentRegion);
            boolean healthy = "ok".equals(result);
            logger.debug("Health check for region '{}': {}", currentRegion, healthy);
            return healthy;
        } catch (Exception e) {
            logger.warn("Health check failed for region '{}': {}", currentRegion, e.getMessage());
            return false;
        }
    }

    /**
     * Get current region information
     */
    public String getCurrentRegion() {
        return currentRegion;
    }

    /**
     * Check if multi-region mode is enabled
     */
    public boolean isMultiRegionEnabled() {
        return multiRegionEnabled;
    }

    /**
     * Get cache statistics for current region
     */
    public CacheStats getStats() {
        try {
            // Basic stats - could be enhanced with more metrics
            boolean healthy = isHealthy();
            
            return CacheStats.builder()
                    .region(currentRegion)
                    .healthy(healthy)
                    .multiRegionEnabled(multiRegionEnabled)
                    .build();
        } catch (Exception e) {
            logger.error("Failed to get cache stats for region '{}': {}", currentRegion, e.getMessage());
            return CacheStats.builder()
                    .region(currentRegion)
                    .healthy(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Cache Statistics Data Class
     */
    public static class CacheStats {
        private final String region;
        private final boolean healthy;
        private final boolean multiRegionEnabled;
        private final String error;

        private CacheStats(Builder builder) {
            this.region = builder.region;
            this.healthy = builder.healthy;
            this.multiRegionEnabled = builder.multiRegionEnabled;
            this.error = builder.error;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getRegion() { return region; }
        public boolean isHealthy() { return healthy; }
        public boolean isMultiRegionEnabled() { return multiRegionEnabled; }
        public String getError() { return error; }

        public static class Builder {
            private String region;
            private boolean healthy;
            private boolean multiRegionEnabled;
            private String error;

            public Builder region(String region) {
                this.region = region;
                return this;
            }

            public Builder healthy(boolean healthy) {
                this.healthy = healthy;
                return this;
            }

            public Builder multiRegionEnabled(boolean multiRegionEnabled) {
                this.multiRegionEnabled = multiRegionEnabled;
                return this;
            }

            public Builder error(String error) {
                this.error = error;
                return this;
            }

            public CacheStats build() {
                return new CacheStats(this);
            }
        }
    }
}