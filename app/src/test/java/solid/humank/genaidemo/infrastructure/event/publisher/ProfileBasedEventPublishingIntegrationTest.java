package solid.humank.genaidemo.infrastructure.event.publisher;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.event.DomainEventPublisher;

/**
 * Integration test for profile-based domain event publishing strategy
 * 
 * Tests the actual Spring configuration and profile-based bean selection
 * 
 * Requirements: 2.1, 2.2, 2.3
 */
@SpringBootTest(classes = {
    solid.humank.genaidemo.infrastructure.config.DomainEventConfiguration.class
})
@ActiveProfiles("local")
@DisplayName("Profile-Based Event Publishing Integration Tests")
class ProfileBasedEventPublishingIntegrationTest {

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    // Test event implementation
    private record TestDomainEvent(
        String aggregateId,
        String eventData,
        UUID eventId,
        LocalDateTime occurredOn
    ) implements DomainEvent {
        
        public static TestDomainEvent create(String aggregateId, String eventData) {
            DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
            return new TestDomainEvent(aggregateId, eventData, metadata.eventId(), metadata.occurredOn());
        }

        @Override
        public UUID getEventId() {
            return eventId;
        }

        @Override
        public LocalDateTime getOccurredOn() {
            return occurredOn;
        }

        @Override
        public String getEventType() {
            return "TestEvent";
        }

        @Override
        public String getAggregateId() {
            return aggregateId;
        }
    }

    @Test
    @DisplayName("Should use InMemoryDomainEventPublisher in dev profile")
    void shouldUseInMemoryDomainEventPublisherInDevProfile() {
        // Verify that the correct publisher is injected
        assertThat(domainEventPublisher).isInstanceOf(InMemoryDomainEventPublisher.class);
    }

    @Test
    @DisplayName("Should publish events successfully in dev profile")
    void shouldPublishEventsSuccessfullyInDevProfile() {
        // Given
        TestDomainEvent event = TestDomainEvent.create("AGG-001", "test data");
        InMemoryDomainEventPublisher inMemoryPublisher = (InMemoryDomainEventPublisher) domainEventPublisher;
        
        // Clear any existing events
        inMemoryPublisher.clearPublishedEvents();

        // When
        domainEventPublisher.publish(event);

        // Then
        assertThat(inMemoryPublisher.getPublishedEvents()).hasSize(1);
        assertThat(inMemoryPublisher.getPublishedEvents().get(0)).isEqualTo(event);
    }

    @Test
    @DisplayName("Should handle null events gracefully in dev profile")
    void shouldHandleNullEventsGracefullyInDevProfile() {
        // Given
        InMemoryDomainEventPublisher inMemoryPublisher = (InMemoryDomainEventPublisher) domainEventPublisher;
        int initialEventCount = inMemoryPublisher.getPublishedEventCount();

        // When
        domainEventPublisher.publish(null);

        // Then - No exception should be thrown and no events should be added
        assertThat(inMemoryPublisher.getPublishedEventCount()).isEqualTo(initialEventCount);
    }
}