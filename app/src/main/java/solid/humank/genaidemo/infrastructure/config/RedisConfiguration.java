package solid.humank.genaidemo.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Configuration for Active-Active Multi-Region Setup
 * 
 * Supports multiple Redis access patterns:
 * - Local-First: Connect to local region Redis (default, recommended)
 * - Multi-Region: Optional cross-region read capabilities
 * 
 * Global Datastore handles cross-region synchronization automatically.
 * Application can choose local-only or multi-region access patterns.
 * 
 * Requirements: 4.4 - Cross-region cache synchronization in Active-Active mode
 */
@Configuration
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisConfiguration {

    // Primary (local) Redis configuration
    @Value("${redis.primary.host:localhost}")
    private String primaryRedisHost;

    @Value("${redis.primary.port:6379}")
    private int primaryRedisPort;

    @Value("${redis.primary.password:}")
    private String primaryRedisPassword;

    @Value("${redis.primary.database:0}")
    private int primaryRedisDatabase;

    // Current region for context
    @Value("${aws.region:us-east-1}")
    private String currentRegion;

    // Multi-region access (optional)
    @Value("${redis.multiregion.enabled:false}")
    private boolean multiRegionEnabled;

    // Secondary regions (for read operations if needed)
    @Value("${redis.secondary.regions:}")
    private String secondaryRegions;

    /**
     * Primary Redis Connection Factory (Local Region)
     * This is the main connection for all Redis operations in Active-Active mode
     */
    @Bean
    @Primary
    public RedisConnectionFactory primaryRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(primaryRedisHost);
        config.setPort(primaryRedisPort);
        config.setDatabase(primaryRedisDatabase);
        
        if (primaryRedisPassword != null && !primaryRedisPassword.trim().isEmpty()) {
            config.setPassword(primaryRedisPassword);
        }

        return new LettuceConnectionFactory(config);
    }

    // Removed duplicate redisConnectionFactory bean to avoid conflicts with Redisson auto-configuration

    /**
     * Primary Redis Template for general object operations
     * Uses local region Redis for optimal performance
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        return createRedisTemplate(primaryRedisConnectionFactory(), Object.class);
    }

    /**
     * Custom String Redis Template for simple string operations
     * Uses local region Redis for optimal performance
     * Named differently to avoid conflict with Redisson's stringRedisTemplate
     */
    @Bean
    public RedisTemplate<String, String> customStringRedisTemplate() {
        return createRedisTemplate(primaryRedisConnectionFactory(), String.class);
    }

    /**
     * Generic method to create Redis templates with consistent configuration
     */
    private <T> RedisTemplate<String, T> createRedisTemplate(RedisConnectionFactory connectionFactory, Class<T> valueType) {
        RedisTemplate<String, T> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Value serializer based on type
        if (valueType == String.class) {
            StringRedisSerializer stringSerializer = new StringRedisSerializer();
            template.setValueSerializer(stringSerializer);
            template.setHashValueSerializer(stringSerializer);
        } else {
            GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
            template.setValueSerializer(jsonSerializer);
            template.setHashValueSerializer(jsonSerializer);
        }
        
        template.afterPropertiesSet();
        return template;
    }
}