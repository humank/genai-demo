package solid.humank.genaidemo.infrastructure.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.EnableRetry;

import com.fasterxml.jackson.databind.ObjectMapper;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.event.DomainEventPublisher;
import solid.humank.genaidemo.infrastructure.event.publisher.DeadLetterService;
import solid.humank.genaidemo.infrastructure.event.publisher.DomainEventPublisherAdapter;
import solid.humank.genaidemo.infrastructure.event.publisher.InMemoryDomainEventPublisher;
import solid.humank.genaidemo.infrastructure.event.publisher.KafkaDomainEventPublisher;
import solid.humank.genaidemo.infrastructure.event.publisher.TransactionalDomainEventPublisher;

/**
 * Enhanced Domain Event Configuration
 * Configures profile-based event publishing strategy with comprehensive
 * observability
 * 
 * Features:
 * - Profile-based event publisher selection (dev: in-memory, production:
 * Kafka/MSK)
 * - Transactional event publishing with @TransactionalEventListener
 * - Retry mechanisms and dead letter queue handling
 * - Enhanced logging and tracing support
 * - Production-ready Kafka configuration for MSK integration
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6
 */
@Configuration
@EnableRetry
public class DomainEventConfiguration {

    // === Development Profile Configuration ===

    /**
     * Enhanced in-memory event publisher for development profile
     * Features: event tracking, correlation IDs, development metrics, debugging
     * support
     * 
     * Requirements: 2.1, 2.2, 2.3
     */
    @Bean("domainEventPublisher")
    @Primary
    @Profile("dev")
    public DomainEventPublisher inMemoryDomainEventPublisher(ApplicationEventPublisher eventPublisher) {
        return new InMemoryDomainEventPublisher(eventPublisher);
    }

    // === Production Profile Configuration ===

    /**
     * Enhanced Kafka event publisher for production profile with MSK integration
     * Features: retry mechanisms, dead letter queue, distributed tracing,
     * production metrics
     * 
     * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6
     */
    @Bean("domainEventPublisher")
    @Primary
    @Profile("production")
    public DomainEventPublisher kafkaDomainEventPublisher(
            KafkaTemplate<String, DomainEvent> kafkaTemplate,
            DeadLetterService deadLetterService) {
        return new KafkaDomainEventPublisher(kafkaTemplate, deadLetterService);
    }

    /**
     * Dead letter service for handling failed events in production
     * Provides comprehensive error tracking and retry management
     * 
     * Requirements: 2.4, 2.5, 2.6
     */
    @Bean
    @Profile("production")
    public DeadLetterService deadLetterService(
            KafkaTemplate<String, Object> deadLetterKafkaTemplate,
            ObjectMapper objectMapper) {
        return new DeadLetterService(deadLetterKafkaTemplate, objectMapper);
    }

    /**
     * Object mapper for JSON serialization in production
     */
    @Bean
    @Profile("production")
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }

    // === Test Profile Configuration ===

    /**
     * Transactional event publisher for test profile
     * Ensures events are processed after transaction commit with cleanup on
     * rollback
     * 
     * Requirements: 2.3, 2.4
     */
    @Bean("domainEventPublisher")
    @Primary
    @Profile("test")
    public DomainEventPublisher transactionalDomainEventPublisher(ApplicationEventPublisher eventPublisher) {
        return new TransactionalDomainEventPublisher(eventPublisher);
    }

    // === Shared Configuration Beans ===

    /**
     * Transactional domain event publisher (available for all profiles)
     * Backup bean for scenarios requiring transactional event handling
     */
    @Bean("transactionalDomainEventPublisher")
    public DomainEventPublisher transactionalDomainEventPublisherBean(ApplicationEventPublisher eventPublisher) {
        return new TransactionalDomainEventPublisher(eventPublisher);
    }

    /**
     * Basic domain event publisher adapter (available for all profiles)
     * Used for immediate event publishing scenarios
     */
    @Bean("domainEventPublisherAdapter")
    public DomainEventPublisher domainEventPublisherAdapter(ApplicationEventPublisher eventPublisher) {
        return new DomainEventPublisherAdapter(eventPublisher);
    }

    // === Kafka Configuration for Production ===
    // Note: deadLetterKafkaTemplate is provided by KafkaConfiguration to avoid
    // duplication
}