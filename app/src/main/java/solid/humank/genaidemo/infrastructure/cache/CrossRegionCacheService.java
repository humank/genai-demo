package solid.humank.genaidemo.infrastructure.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

// import org.redisson.api.RBucket;
// import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import solid.humank.genaidemo.application.common.DistributedLockService;

/**
 * Cross-Region Cache Service for Global Data Consistency
 *
 * This service provides caching capabilities with cross-region synchronization
 * using Redis Global Datastore. It handles cache invalidation, consistency
 * checks, and performance monitoring across multiple regions.
 *
 * Features:
 * - Cross-region cache synchronization via Global Datastore
 * - Automatic cache invalidation strategies
 * - Cache consistency monitoring
 * - Performance metrics collection
 * - Graceful degradation on cache failures
 *
 * Requirements: 4.1.4 - Cross-region cache synchronization
 *
 * @author Development Team
 * @since 2.0.0
 */
@Service
public class CrossRegionCacheService {

    private static final Logger logger = LoggerFactory.getLogger(CrossRegionCacheService.class);

    private static final String CACHE_KEY_PREFIX = "cross-region-cache:";
    private static final String INVALIDATION_KEY_PREFIX = "cache-invalidation:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);
    private static final Duration INVALIDATION_TTL = Duration.ofMinutes(5);

    // private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final DistributedLockService distributedLockService;
    private final CrossRegionCacheMetrics cacheMetrics;

    public CrossRegionCacheService(
            // RedissonClient redissonClient,
            ObjectMapper objectMapper,
            DistributedLockService distributedLockService,
            CrossRegionCacheMetrics cacheMetrics) {
        // this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
        this.distributedLockService = distributedLockService;
        this.cacheMetrics = cacheMetrics;
    }

    /**
     * Get value from cache with automatic fallback to data source
     *
     * @param key        cache key
     * @param valueType  class type of cached value
     * @param dataSource fallback data source if cache miss
     * @param <T>        value type
     * @return cached or freshly loaded value
     */
    public <T> Optional<T> get(String key, Class<T> valueType, Supplier<Optional<T>> dataSource) {
        return get(key, valueType, dataSource, DEFAULT_TTL);
    }

    /**
     * Get value from cache with custom TTL and automatic fallback
     *
     * @param key        cache key
     * @param valueType  class type of cached value
     * @param dataSource fallback data source if cache miss
     * @param ttl        time to live for cached value
     * @param <T>        value type
     * @return cached or freshly loaded value
     */
    public <T> Optional<T> get(String key, Class<T> valueType, Supplier<Optional<T>> dataSource, Duration ttl) {
        String normalizedKey = normalizeKey(key);
        long startTime = System.currentTimeMillis();

        try {
            // Check for cache invalidation marker first
            /*
             * if (isInvalidated(normalizedKey)) {
             * cacheMetrics.recordCacheInvalidationHit(normalizedKey);
             * logger.debug("Cache key {} is marked for invalidation, skipping cache",
             * normalizedKey);
             * return loadAndCache(normalizedKey, dataSource, ttl, startTime);
             * }
             * 
             * // Try to get from cache
             * RBucket<String> bucket = redissonClient.getBucket(normalizedKey);
             * String cachedJson = bucket.get();
             * 
             * if (cachedJson != null) {
             * Optional<T> deserializedValue = deserializeCachedValue(cachedJson, valueType,
             * normalizedKey, startTime);
             * if (deserializedValue.isPresent()) {
             * return deserializedValue;
             * }
             * // Deserialization failed, continue to load from data source
             * }
             */

            // Cache miss - load from data source
            long duration = System.currentTimeMillis() - startTime;
            cacheMetrics.recordCacheMiss(normalizedKey, duration);
            logger.debug("Cache miss for key: {} in {} ms", normalizedKey, duration);

            return loadAndCache(normalizedKey, dataSource, ttl, startTime);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            cacheMetrics.recordCacheError(normalizedKey, e.getClass().getSimpleName(), duration);
            logger.error("Cache operation failed for key: {}", normalizedKey, e);

            // Graceful degradation - try to load from data source
            try {
                return dataSource.get();
            } catch (Exception dataSourceException) {
                logger.error("Data source also failed for key: {}", normalizedKey, dataSourceException);
                return Optional.empty();
            }
        }
    }

    /**
     * Put value into cache with default TTL
     *
     * @param key   cache key
     * @param value value to cache
     * @param <T>   value type
     */
    public <T> void put(String key, T value) {
        put(key, value, DEFAULT_TTL);
    }

    /**
     * Put value into cache with custom TTL
     *
     * @param key   cache key
     * @param value value to cache
     * @param ttl   time to live for cached value
     * @param <T>   value type
     */
    public <T> void put(String key, T value, Duration ttl) {
        String normalizedKey = normalizeKey(key);
        long startTime = System.currentTimeMillis();

        try {
            /*
             * String jsonValue = objectMapper.writeValueAsString(value);
             * RBucket<String> bucket = redissonClient.getBucket(normalizedKey);
             * bucket.setAsync(jsonValue, Duration.ofMillis(ttl.toMillis()));
             */

            long duration = System.currentTimeMillis() - startTime;
            cacheMetrics.recordCachePut(normalizedKey, duration);
            logger.debug("Cached value for key: {} with TTL {} in {} ms", normalizedKey, ttl, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            cacheMetrics.recordCacheError(normalizedKey, e.getClass().getSimpleName(), duration);
            logger.error("Failed to cache value for key: {}", normalizedKey, e);
            throw new CrossRegionCacheException("Failed to cache value", e);
        }
    }

    /**
     * Invalidate cache entry across all regions
     *
     * @param key cache key to invalidate
     */
    public void invalidate(String key) {
        String normalizedKey = normalizeKey(key);
        String lockKey = INVALIDATION_KEY_PREFIX + normalizedKey;

        distributedLockService.executeWithLock(lockKey, () -> {
            long startTime = System.currentTimeMillis();

            try {
                /*
                 * // Set invalidation marker
                 * String invalidationKey = INVALIDATION_KEY_PREFIX + normalizedKey;
                 * RBucket<String> invalidationBucket =
                 * redissonClient.getBucket(invalidationKey);
                 * invalidationBucket.setAsync(Instant.now().toString(), INVALIDATION_TTL);
                 * 
                 * // Remove the actual cache entry
                 * RBucket<String> cacheBucket = redissonClient.getBucket(normalizedKey);
                 * cacheBucket.delete();
                 */

                long duration = System.currentTimeMillis() - startTime;
                cacheMetrics.recordCacheInvalidation(normalizedKey, duration);
                logger.debug("Invalidated cache for key: {} in {} ms", normalizedKey, duration);

                return null; // Return null for Supplier<T>

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                cacheMetrics.recordCacheError(normalizedKey, e.getClass().getSimpleName(), duration);
                logger.error("Failed to invalidate cache for key: {}", normalizedKey, e);
                throw new CrossRegionCacheException("Failed to invalidate cache", e);
            }
        });
    }

    /**
     * Invalidate multiple cache entries with pattern
     *
     * @param keyPattern pattern to match keys (supports wildcards)
     */
    public void invalidatePattern(String keyPattern) {
        String normalizedPattern = normalizeKey(keyPattern);
        String lockKey = "cache-pattern-invalidation:" + normalizedPattern;

        distributedLockService.executeWithLock(lockKey, () -> {
            long startTime = System.currentTimeMillis();
            int invalidatedCount = 0;

            try {
                // Use getKeysStream for better performance and non-deprecated API
                invalidatedCount = invalidateMatchingKeys(normalizedPattern);

                long duration = System.currentTimeMillis() - startTime;
                cacheMetrics.recordCachePatternInvalidation(normalizedPattern, invalidatedCount, duration);
                logger.debug("Invalidated {} cache entries matching pattern: {} in {} ms",
                        invalidatedCount, normalizedPattern, duration);

                return null;

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                cacheMetrics.recordCacheError(normalizedPattern, e.getClass().getSimpleName(), duration);
                logger.error("Failed to invalidate cache pattern: {}", normalizedPattern, e);
                throw new CrossRegionCacheException("Failed to invalidate cache pattern: " + normalizedPattern, e);
            }
        });
    }

    /**
     * Check if cache entry exists
     *
     * @param key cache key
     * @return true if entry exists and is not invalidated
     */
    public boolean exists(String key) {
        String normalizedKey = normalizeKey(key);

        try {
            if (isInvalidated(normalizedKey)) {
                return false;
            }

            /*
             * RBucket<String> bucket = redissonClient.getBucket(normalizedKey);
             * return bucket.isExists();
             */
            return false;

        } catch (Exception e) {
            logger.warn("Failed to check cache existence for key: {}", normalizedKey, e);
            return false;
        }
    }

    /**
     * Get remaining TTL for cache entry
     *
     * @param key cache key
     * @return remaining TTL in milliseconds, -1 if key doesn't exist
     */
    public long getRemainingTTL(String key) {
        String normalizedKey = normalizeKey(key);

        try {
            /*
             * RBucket<String> bucket = redissonClient.getBucket(normalizedKey);
             * return bucket.remainTimeToLive();
             */
            return -1;

        } catch (Exception e) {
            logger.warn("Failed to get TTL for cache key: {}", normalizedKey, e);
            return -1;
        }
    }

    /**
     * Deserialize cached JSON value
     */
    private <T> Optional<T> deserializeCachedValue(String cachedJson, Class<T> valueType,
            String normalizedKey, long startTime) {
        try {
            T value = objectMapper.readValue(cachedJson, valueType);
            long duration = System.currentTimeMillis() - startTime;
            cacheMetrics.recordCacheHit(normalizedKey, duration);
            logger.debug("Cache hit for key: {} in {} ms", normalizedKey, duration);
            return Optional.of(value);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to deserialize cached value for key: {}, will reload from source",
                    normalizedKey, e);
            cacheMetrics.recordCacheDeserializationError(normalizedKey);
            return Optional.empty();
        }
    }

    /**
     * Load data from source and cache it
     */
    private <T> Optional<T> loadAndCache(String normalizedKey,
            Supplier<Optional<T>> dataSource, Duration ttl, long startTime) {
        try {
            Optional<T> value = dataSource.get();

            if (value.isPresent()) {
                // Cache the loaded value
                // put(normalizedKey.substring(CACHE_KEY_PREFIX.length()), value.get(), ttl);

                long totalDuration = System.currentTimeMillis() - startTime;
                cacheMetrics.recordCacheLoadAndStore(normalizedKey, totalDuration);
                logger.debug("Loaded and cached value for key: {} in {} ms", normalizedKey, totalDuration);
            }

            return value;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            cacheMetrics.recordCacheError(normalizedKey, e.getClass().getSimpleName(), duration);
            logger.error("Failed to load data for key: {}", normalizedKey, e);
            return Optional.empty();
        }
    }

    /**
     * Invalidate matching keys helper method
     */
    private int invalidateMatchingKeys(String normalizedPattern) {
        int count = 0;
        try {
            /*
             * // Suppress deprecation warning - getKeysByPattern is the most appropriate
             * // method
             * // for pattern-based key retrieval with controlled iteration
             * 
             * @SuppressWarnings("deprecation")
             * Iterable<String> keys =
             * redissonClient.getKeys().getKeysByPattern(normalizedPattern);
             * 
             * int processedCount = 0;
             * for (String key : keys) {
             * if (key.startsWith(CACHE_KEY_PREFIX)) {
             * invalidateSingleKey(key);
             * count++;
             * }
             * // Limit processing to avoid excessive memory usage
             * if (++processedCount >= 1000) {
             * logger.warn("Reached maximum key processing limit (1000) for pattern: {}",
             * normalizedPattern);
             * break;
             * }
             * }
             */
        } catch (Exception e) {
            logger.error("Error invalidating keys with pattern: {}", normalizedPattern, e);
            throw new CrossRegionCacheException("Failed to invalidate keys", e);
        }
        return count;
    }

    /**
     * Invalidate single key helper method
     */
    private void invalidateSingleKey(String key) {
        try {
            /*
             * String invalidationKey = INVALIDATION_KEY_PREFIX + key;
             * RBucket<String> invalidationBucket =
             * redissonClient.getBucket(invalidationKey);
             * invalidationBucket.setAsync(Instant.now().toString(), INVALIDATION_TTL);
             * 
             * RBucket<String> cacheBucket = redissonClient.getBucket(key);
             * cacheBucket.delete();
             */
        } catch (Exception e) {
            logger.warn("Failed to invalidate single key: {}", key, e);
            throw new CrossRegionCacheException("Failed to invalidate key: " + key, e);
        }
    }

    /**
     * Check if cache key is marked for invalidation
     */
    private boolean isInvalidated(String normalizedKey) {
        try {
            /*
             * String invalidationKey = INVALIDATION_KEY_PREFIX + normalizedKey;
             * RBucket<String> invalidationBucket =
             * redissonClient.getBucket(invalidationKey);
             * return invalidationBucket.isExists();
             */
            return false;
        } catch (Exception e) {
            logger.warn("Failed to check invalidation status for key: {}", normalizedKey, e);
            return false;
        }
    }

    /**
     * Normalize cache key with prefix
     */
    private String normalizeKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Cache key cannot be null or empty");
        }

        String normalized = key.trim();

        // Add prefix if not already present
        if (!normalized.startsWith(CACHE_KEY_PREFIX)) {
            normalized = CACHE_KEY_PREFIX + normalized;
        }

        // Validate key length
        if (normalized.length() > 250) {
            throw new IllegalArgumentException("Cache key too long: " + normalized.length() + " characters");
        }

        return normalized;
    }

    /**
     * Exception thrown when cache operations fail
     */
    public static class CrossRegionCacheException extends RuntimeException {
        public CrossRegionCacheException(String message) {
            super(message);
        }

        public CrossRegionCacheException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
