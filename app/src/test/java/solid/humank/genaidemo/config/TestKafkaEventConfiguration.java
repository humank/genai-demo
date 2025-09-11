package solid.humank.genaidemo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.infrastructure.event.publisher.DeadLetterService;
import solid.humank.genaidemo.infrastructure.event.publisher.KafkaDomainEventPublisher;

/**
 * Test configuration for Kafka event publishing components
 * Provides mock implementations for testing Kafka-related functionality
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

    @Bean
    @Primary
    public DeadLetterService testDeadLetterService() {
        return new DeadLetterService(testDeadLetterKafkaTemplate(), new ObjectMapper());
    }

    @Bean
    @Primary
    public KafkaDomainEventPublisher testKafkaDomainEventPublisher() {
        return new KafkaDomainEventPublisher(testKafkaTemplate(), testDeadLetterService());
    }
}