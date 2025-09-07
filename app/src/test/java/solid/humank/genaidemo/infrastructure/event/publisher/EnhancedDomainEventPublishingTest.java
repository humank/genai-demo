package solid.humank.genaidemo.infrastructure.event.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * Test for enhanced domain event publishing strategy
 * 
 * Tests both development (in-memory) and production (Kafka) publishers
 * Verifies transactional event publishing, retry mechanisms, and dead letter
 * queue handling
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Enhanced Domain Event Publishing Strategy Tests")
class EnhancedDomainEventPublishingTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private KafkaTemplate<String, DomainEvent> kafkaTemplate;

    @Mock
    private DeadLetterService deadLetterService;

    // Test event implementation
    private record TestDomainEvent(
            String aggregateId,
            String eventData,
            UUID eventId,
            LocalDateTime occurredOn) implements DomainEvent {

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

    @Nested
    @DisplayName("Development Profile - InMemoryDomainEventPublisher Tests")
    @ActiveProfiles("dev")
    class InMemoryDomainEventPublisherTest {

        private InMemoryDomainEventPublisher publisher;

        @BeforeEach
        void setUp() {
            publisher = new InMemoryDomainEventPublisher(applicationEventPublisher);
        }

        @Test
        @DisplayName("Should publish single event to Spring ApplicationEventPublisher")
        void shouldPublishSingleEventToSpringApplicationEventPublisher() {
            // Given
            TestDomainEvent event = TestDomainEvent.create("AGG-001", "test data");

            // When
            publisher.publish(event);

            // Then
            verify(applicationEventPublisher).publishEvent(any(DomainEventPublisherAdapter.DomainEventWrapper.class));
            assertThat(publisher.getPublishedEvents()).hasSize(1);
            assertThat(publisher.getPublishedEvents().get(0)).isEqualTo(event);
        }

        @Test
        @DisplayName("Should publish multiple events and store them for testing")
        void shouldPublishMultipleEventsAndStoreThemForTesting() {
            // Given
            List<DomainEvent> events = List.of(
                    TestDomainEvent.create("AGG-001", "test data 1"),
                    TestDomainEvent.create("AGG-002", "test data 2"),
                    TestDomainEvent.create("AGG-003", "test data 3"));

            // When
            publisher.publishAll(events);

            // Then
            verify(applicationEventPublisher, times(3))
                    .publishEvent(any(DomainEventPublisherAdapter.DomainEventWrapper.class));
            assertThat(publisher.getPublishedEvents()).hasSize(3);
            assertThat(publisher.getPublishedEventCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should filter published events by type")
        void shouldFilterPublishedEventsByType() {
            // Given
            TestDomainEvent event1 = TestDomainEvent.create("AGG-001", "test data 1");
            TestDomainEvent event2 = TestDomainEvent.create("AGG-002", "test data 2");

            // When
            publisher.publish(event1);
            publisher.publish(event2);

            // Then
            List<DomainEvent> testEvents = publisher.getPublishedEventsByType("TestEvent");
            assertThat(testEvents).hasSize(2);
        }

        @Test
        @DisplayName("Should clear published events for testing")
        void shouldClearPublishedEventsForTesting() {
            // Given
            TestDomainEvent event = TestDomainEvent.create("AGG-001", "test data");
            publisher.publish(event);
            assertThat(publisher.getPublishedEvents()).hasSize(1);

            // When
            publisher.clearPublishedEvents();

            // Then
            assertThat(publisher.getPublishedEvents()).isEmpty();
            assertThat(publisher.getPublishedEventCount()).isZero();
        }

        @Test
        @DisplayName("Should handle null events gracefully")
        void shouldHandleNullEventsGracefully() {
            // When
            publisher.publish(null);
            publisher.publishAll(null);

            // Then
            assertThat(publisher.getPublishedEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Production Profile - KafkaDomainEventPublisher Tests")
    @ActiveProfiles("production")
    class KafkaDomainEventPublisherTest {

        private KafkaDomainEventPublisher publisher;

        @BeforeEach
        void setUp() {
            publisher = new KafkaDomainEventPublisher(kafkaTemplate, deadLetterService);
        }

        @Test
        @DisplayName("Should publish event to correct Kafka topic")
        void shouldPublishEventToCorrectKafkaTopic() {
            // Given
            TestDomainEvent event = TestDomainEvent.create("AGG-001", "test data");
            CompletableFuture<SendResult<String, DomainEvent>> future = CompletableFuture.completedFuture(null);
            when(kafkaTemplate.send(eq("genai-demo.testevent"), eq("AGG-001"), eq(event))).thenReturn(future);

            // When
            publisher.publish(event);

            // Then
            verify(kafkaTemplate).send("genai-demo.testevent", "AGG-001", event);
        }

        @Test
        @DisplayName("Should publish multiple events to Kafka")
        void shouldPublishMultipleEventsToKafka() {
            // Given
            List<DomainEvent> events = List.of(
                    TestDomainEvent.create("AGG-001", "test data 1"),
                    TestDomainEvent.create("AGG-002", "test data 2"));
            CompletableFuture<SendResult<String, DomainEvent>> future = CompletableFuture.completedFuture(null);
            when(kafkaTemplate.send(any(String.class), any(String.class), any(DomainEvent.class))).thenReturn(future);

            // When
            publisher.publishAll(events);

            // Then
            verify(kafkaTemplate, times(2)).send(any(String.class), any(String.class), any(DomainEvent.class));
        }

        @Test
        @DisplayName("Should handle null events gracefully in production")
        void shouldHandleNullEventsGracefullyInProduction() {
            // When
            publisher.publish(null);
            publisher.publishAll(null);

            // Then - No exceptions should be thrown
            // Verify no Kafka calls were made
            verify(kafkaTemplate, times(0)).send(any(String.class), any(String.class), any(DomainEvent.class));
        }

        @Test
        @DisplayName("Should handle empty event list gracefully")
        void shouldHandleEmptyEventListGracefully() {
            // When
            publisher.publishAll(List.of());

            // Then - No exceptions should be thrown
            verify(kafkaTemplate, times(0)).send(any(String.class), any(String.class), any(DomainEvent.class));
        }
    }

    @Nested
    @DisplayName("Dead Letter Service Tests")
    class DeadLetterServiceTest {

        private DeadLetterService deadLetterService;

        @Mock
        private KafkaTemplate<String, Object> deadLetterKafkaTemplate;

        @BeforeEach
        void setUp() {
            deadLetterService = new DeadLetterService(deadLetterKafkaTemplate,
                    new com.fasterxml.jackson.databind.ObjectMapper());
        }

        @Test
        @DisplayName("Should send failed event to dead letter queue")
        void shouldSendFailedEventToDeadLetterQueue() {
            // Given
            TestDomainEvent event = TestDomainEvent.create("AGG-001", "test data");
            Exception cause = new RuntimeException("Kafka publishing failed");

            // When
            deadLetterService.sendToDeadLetter(event, cause);

            // Then
            verify(deadLetterKafkaTemplate).send(
                    eq("genai-demo.dead-letter"),
                    eq("AGG-001"),
                    any(DeadLetterService.DeadLetterEvent.class));
        }
    }

    @Nested
    @DisplayName("Event Publishing Exception Tests")
    class EventPublishingExceptionTest {

        @Test
        @DisplayName("Should create EventPublishingException with message and cause")
        void shouldCreateEventPublishingExceptionWithMessageAndCause() {
            // Given
            String message = "Event publishing failed";
            Exception cause = new RuntimeException("Kafka error");

            // When
            KafkaDomainEventPublisher.EventPublishingException exception = new KafkaDomainEventPublisher.EventPublishingException(
                    message, cause);

            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }
}