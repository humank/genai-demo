package solid.humank.genaidemo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * Simplified test configuration for Kafka event publishing components
 * Provides mock implementations for testing without actual Kafka dependencies
 */
@TestConfiguration
@Profile("test")
public class TestKafkaEventConfiguration {

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public KafkaTemplate<String, DomainEvent> testKafkaTemplate() {
        return org.mockito.Mockito.mock(KafkaTemplate.class);
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public KafkaTemplate<String, Object> testDeadLetterKafkaTemplate() {
        return org.mockito.Mockito.mock(KafkaTemplate.class);
    }
}