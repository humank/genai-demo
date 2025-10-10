package solid.humank.genaidemo.infrastructure.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Tags;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics collection for Cross-Region Cache Service
 * 
 * This component collects and exports comprehensive metrics for cache operations
 * to support monitoring, alerting, and performance optimization in cross-region deployments.
 * 
 * Metrics collected:
 * - Cache hit/miss rates
 * - Cache operation latencies
 * - Cache invalidation events
 * - Cache error rates
 * - Cache size and memory usage
 * 
 * Requirements: 4.1.4 - Cross-region cache synchronization monitoring
 * 
 * @author Development Team
 * @since 2.0.0
 */
@Component
public class CrossRegionCacheMetrics {

    // Counters for cache operations
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter cachePutCounter;
    private final Counter cacheInvalidationCounter;
    private final Counter cacheInvalidationHitCounter;
    private final Counter cachePatternInvalidationCounter;
    private final Counter cacheErrorCounter;
    private final Counter cacheSerializationErrorCounter;
    private final Counter cacheDeserializationErrorCounter;
    
    // Timers for operation latencies
    private final Timer cacheGetTimer;
    private final Timer cachePutTimer;
    private final Timer cacheInvalidationTimer;
    private final Timer cacheLoadAndStoreTimer;
    
    // Gauges for cache statistics
    private final AtomicLong cacheSize = new AtomicLong(0);
    private final AtomicLong cacheMemoryUsage = new AtomicLong(0);
    
    private final RedissonClient redissonClient;

    public CrossRegionCacheMetrics(MeterRegistry meterRegistry, RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        
        // Initialize counters
        this.cacheHitCounter = Counter.builder("cache.requests")
            .description("Number of cache requests")
            .tag("result", "hit")
            .register(meterRegistry);
            
        this.cacheMissCounter = Counter.builder("cache.requests")
            .description("Number of cache requests")
            .tag("result", "miss")
            .register(meterRegistry);
            
        this.cachePutCounter = Counter.builder("cache.puts")
            .description("Number of cache put operations")
            .register(meterRegistry);
            
        this.cacheInvalidationCounter = Counter.builder("cache.invalidations")
            .description("Number of cache invalidation operations")
            .register(meterRegistry);
            
        this.cacheInvalidationHitCounter = Counter.builder("cache.invalidation.hits")
            .description("Number of cache requests that hit invalidation markers")
            .register(meterRegistry);
            
        this.cachePatternInvalidationCounter = Counter.builder("cache.pattern.invalidations")
            .description("Number of cache pattern invalidation operations")
            .register(meterRegistry);
            
        this.cacheErrorCounter = Counter.builder("cache.errors")
            .description("Number of cache operation errors")
            .register(meterRegistry);
            
        this.cacheSerializationErrorCounter = Counter.builder("cache.serialization.errors")
            .description("Number of cache serialization errors")
            .register(meterRegistry);
            
        this.cacheDeserializationErrorCounter = Counter.builder("cache.deserialization.errors")
            .description("Number of cache deserialization errors")
            .register(meterRegistry);
        
        // Initialize timers
        this.cacheGetTimer = Timer.builder("cache.get.time")
            .description("Time taken for cache get operations")
            .register(meterRegistry);
            
        this.cachePutTimer = Timer.builder("cache.put.time")
            .description("Time taken for cache put operations")
            .register(meterRegistry);
            
        this.cacheInvalidationTimer = Timer.builder("cache.invalidation.time")
            .description("Time taken for cache invalidation operations")
            .register(meterRegistry);
            
        this.cacheLoadAndStoreTimer = Timer.builder("cache.load.store.time")
            .description("Time taken to load data and store in cache")
            .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("cache.size", this, CrossRegionCacheMetrics::getCacheSize)
            .description("Current number of entries in cache")
            .register(meterRegistry);
            
        Gauge.builder("cache.memory.usage", this, CrossRegionCacheMetrics::getCacheMemoryUsage)
            .description("Current memory usage of cache in bytes")
            .register(meterRegistry);
            
        Gauge.builder("cache.hit.ratio", this, CrossRegionCacheMetrics::getCacheHitRatio)
            .description("Cache hit ratio (hits / (hits + misses))")
            .register(meterRegistry);
    }

    /**
     * Record cache hit
     * 
     * @param cacheKey the cache key
     * @param durationMs operation duration in milliseconds
     */
    public void recordCacheHit(String cacheKey, long durationMs) {
        Tags tags = Tags.of(
            "cache_key", sanitizeCacheKey(cacheKey),
            "operation", "get",
            "result", "hit"
        );
        
        cacheHitCounter.increment();
        cacheGetTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record cache miss
     * 
     * @param cacheKey the cache key
     * @param durationMs operation duration in milliseconds
     */
    public void recordCacheMiss(String cacheKey, long durationMs) {
        Tags tags = Tags.of(
            "cache_key", sanitizeCacheKey(cacheKey),
            "operation", "get",
            "result", "miss"
        );
        
        cacheMissCounter.increment();
        cacheGetTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record cache put operation
     * 
     * @param cacheKey the cache key
     * @param durationMs operation duration in milliseconds
     */
    public void recordCachePut(String cacheKey, long durationMs) {
        Tags tags = Tags.of(
            "cache_key", sanitizeCacheKey(cacheKey),
            "operation", "put"
        );
        
        cachePutCounter.increment();
        cachePutTimer.record(durationMs, TimeUnit.MILLISECONDS);
        
        // Update cache size estimate
        cacheSize.incrementAndGet();
    }

    /**
     * Record cache invalidation
     * 
     * @param cacheKey the cache key
     * @param durationMs operation duration in milliseconds
     */
    public void recordCacheInvalidation(String cacheKey, long durationMs) {
        Tags tags = Tags.of(
            "cache_key", sanitizeCacheKey(cacheKey),
            "operation", "invalidate"
        );
        
        cacheInvalidationCounter.increment();
        cacheInvalidationTimer.record(durationMs, TimeUnit.MILLISECONDS);
        
        // Update cache size estimate
        cacheSize.decrementAndGet();
    }

    /**
     * Record cache invalidation hit (request hit invalidation marker)
     * 
     * @param cacheKey the cache key
     */
    public void recordCacheInvalidationHit(String cacheKey) {
        Tags tags = Tags.of(
            "cache_key", sanitizeCacheKey(cacheKey)
        );
        
        cacheInvalidationHitCounter.increment();
    }

    /**
     * Record cache pattern invalidation
     * 
     * @param pattern the cache key pattern
     * @param invalidatedCount number of keys invalidated
     * @param durationMs operation duration in milliseconds
     */
    public void recordCachePatternInvalidation(String pattern, int invalidatedCount, long durationMs) {
        Tags tags = Tags.of(
            "pattern", sanitizeCacheKey(pattern),
            "operation", "pattern_invalidate"
        );
        
        cachePatternInvalidationCounter.increment();
        cacheInvalidationTimer.record(durationMs, TimeUnit.MILLISECONDS);
        
        // Update cache size estimate
        cacheSize.addAndGet(-invalidatedCount);
    }

    /**
     * Record cache load and store operation
     * 
     * @param cacheKey the cache key
     * @param durationMs operation duration in milliseconds
     */
    public void recordCacheLoadAndStore(String cacheKey, long durationMs) {
        Tags tags = Tags.of(
            "cache_key", sanitizeCacheKey(cacheKey),
            "operation", "load_and_store"
        );
        
        cacheLoadAndStoreTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record cache operation error
     * 
     * @param cacheKey the cache key
     * @param errorType type of error
     * @param durationMs operation duration in milliseconds
     */
    public void recordCacheError(String cacheKey, String errorType, long durationMs) {
        Tags tags = Tags.of(
            "cache_key", sanitizeCacheKey(cacheKey),
            "error_type", errorType
        );
        
        cacheErrorCounter.increment();
    }

    /**
     * Record cache serialization error
     * 
     * @param cacheKey the cache key
     */
    public void recordCacheSerializationError(String cacheKey) {
        Tags tags = Tags.of(
            "cache_key", sanitizeCacheKey(cacheKey)
        );
        
        cacheSerializationErrorCounter.increment();
    }

    /**
     * Record cache deserialization error
     * 
     * @param cacheKey the cache key
     */
    public void recordCacheDeserializationError(String cacheKey) {
        Tags tags = Tags.of(
            "cache_key", sanitizeCacheKey(cacheKey)
        );
        
        cacheDeserializationErrorCounter.increment();
    }

    /**
     * Get current cache size estimate
     * 
     * @return estimated number of cache entries
     */
    public double getCacheSize() {
        try {
            // Try to get actual size from Redis if possible
            long actualSize = redissonClient.getKeys().count();
            cacheSize.set(actualSize);
            return actualSize;
        } catch (Exception e) {
            // Fall back to estimate
            return Math.max(0, cacheSize.get());
        }
    }

    /**
     * Get current cache memory usage estimate
     * 
     * @return estimated memory usage in bytes
     */
    public double getCacheMemoryUsage() {
        try {
            // Try to get actual memory usage from Redis if possible
            // This is an approximation as Redis doesn't provide exact per-key memory usage
            long memoryUsage = redissonClient.getKeys().count() * 1024; // Rough estimate: 1KB per key
            cacheMemoryUsage.set(memoryUsage);
            return memoryUsage;
        } catch (Exception e) {
            // Fall back to estimate
            return Math.max(0, cacheMemoryUsage.get());
        }
    }

    /**
     * Calculate cache hit ratio
     * 
     * @return cache hit ratio (0.0 to 1.0)
     */
    public double getCacheHitRatio() {
        double hits = cacheHitCounter.count();
        double misses = cacheMissCounter.count();
        double total = hits + misses;
        
        if (total == 0) {
            return 0.0;
        }
        
        return hits / total;
    }

    /**
     * Sanitize cache key for use in metrics tags
     * Removes sensitive information and limits length
     * 
     * @param cacheKey original cache key
     * @return sanitized cache key
     */
    private String sanitizeCacheKey(String cacheKey) {
        if (cacheKey == null) {
            return "unknown";
        }
        
        // Remove the cache prefix if present
        String sanitized = cacheKey.startsWith("cross-region-cache:") 
            ? cacheKey.substring("cross-region-cache:".length()) 
            : cacheKey;
        
        // Extract the main category/type from the key
        // e.g., "customer:123:profile" -> "customer:profile"
        String[] parts = sanitized.split(":");
        if (parts.length >= 3) {
            // Keep first and last parts, replace middle with placeholder
            sanitized = parts[0] + ":*:" + parts[parts.length - 1];
        } else if (parts.length == 2) {
            // Keep first part, replace second with placeholder if it looks like an ID
            if (parts[1].matches("\\d+") || parts[1].length() > 10) {
                sanitized = parts[0] + ":*";
            }
        }
        
        // Limit length to avoid high cardinality
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 47) + "...";
        }
        
        return sanitized;
    }
}