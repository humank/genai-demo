package solid.humank.genaidemo.infrastructure.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * Kafka Configuration for Production Event Publishing
 * 
 * Features:
 * - Amazon MSK integration with optimized producer settings
 * - Separate producer factories for domain events and dead letter queue
 * - Production-ready serialization and error handling
 * - Idempotent producers for exactly-once semantics
 * - Optimized batching and compression settings
 * 
 * Requirements: 2.1, 2.2, 2.4, 2.5, 2.6
 */
@Configuration
@Profile("production")
public class KafkaEventConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.acks:all}")
    private String acks;

    @Value("${spring.kafka.producer.retries:3}")
    private int retries;

    @Value("${spring.kafka.producer.batch-size:16384}")
    private int batchSize;

    @Value("${spring.kafka.producer.linger-ms:5}")
    private int lingerMs;

    @Value("${spring.kafka.producer.buffer-memory:33554432}")
    private long bufferMemory;

    /**
     * Producer factory for domain events with optimized settings for MSK
     */
    @Bean
    public ProducerFactory<String, DomainEvent> domainEventProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Basic configuration
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Reliability and durability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, acks);
        configProps.put(ProducerConfig.RETRIES_CONFIG, retries);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Performance optimization settings
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        // Ordering and delivery settings
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);

        // Serialization settings
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        configProps.put(JsonSerializer.TYPE_MAPPINGS,
                "DomainEvent:solid.humank.genaidemo.domain.common.event.DomainEvent");

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Kafka template for domain events
     */
    @Bean
    public KafkaTemplate<String, DomainEvent> domainEventKafkaTemplate() {
        KafkaTemplate<String, DomainEvent> template = new KafkaTemplate<>(domainEventProducerFactory());

        // Set default topic if needed
        // template.setDefaultTopic("genai-demo.default");

        return template;
    }

    /**
     * Producer factory for dead letter queue with generic object support
     */
    @Bean
    public ProducerFactory<String, Object> deadLetterProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Basic configuration
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Reliability settings (less strict for dead letter queue)
        configProps.put(ProducerConfig.ACKS_CONFIG, "1");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 1);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);

        // Performance settings (optimized for error handling)
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 8192);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 16777216);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");

        // Timeout settings (shorter for error scenarios)
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 60000);
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 15000);

        // Serialization settings for dead letter events
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Kafka template for dead letter queue
     */
    @Bean("deadLetterKafkaTemplate")
    public KafkaTemplate<String, Object> deadLetterKafkaTemplate() {
        return new KafkaTemplate<>(deadLetterProducerFactory());
    }
}