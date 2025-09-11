package solid.humank.genaidemo.infrastructure.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * Unit tests for NoOpAnalyticsEventPublisher.
 * 
 * These tests verify that the no-op implementation behaves correctly
 * and doesn't cause any side effects in development environments.
 */
@ExtendWith(MockitoExtension.class)
class NoOpAnalyticsEventPublisherTest {

    private NoOpAnalyticsEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new NoOpAnalyticsEventPublisher();
    }

    @Test
    void shouldPublishSingleEventWithoutError() {
        // Given
        DomainEvent event = createTestEvent();

        // When & Then - should not throw any exception
        publisher.publish(event);

        // Verify publisher is always healthy
        assertThat(publisher.isHealthy()).isTrue();
    }

    @Test
    void shouldPublishBatchEventsWithoutError() {
        // Given
        List<DomainEvent> events = List.of(
                createTestEvent(),
                createTestEvent(),
                createTestEvent());

        // When & Then - should not throw any exception
        publisher.publishBatch(events);

        // Verify publisher is always healthy
        assertThat(publisher.isHealthy()).isTrue();
    }

    @Test
    void shouldFlushWithoutError() {
        // When & Then - should not throw any exception
        publisher.flush();

        // Verify publisher is always healthy
        assertThat(publisher.isHealthy()).isTrue();
    }

    @Test
    void shouldAlwaysBeHealthy() {
        // When & Then
        assertThat(publisher.isHealthy()).isTrue();

        // Even after operations
        publisher.publish(createTestEvent());
        publisher.flush();

        assertThat(publisher.isHealthy()).isTrue();
    }

    private DomainEvent createTestEvent() {
        return new TestDomainEvent(
                UUID.randomUUID(),
                "TestEvent",
                "TEST-001",
                LocalDateTime.now());
    }

    /**
     * Test implementation of DomainEvent for testing purposes.
     */
    private record TestDomainEvent(
            UUID eventId,
            String eventType,
            String aggregateId,
            LocalDateTime occurredOn) implements DomainEvent {

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
            return eventType;
        }

        @Override
        public String getAggregateId() {
            return aggregateId;
        }
    }
}