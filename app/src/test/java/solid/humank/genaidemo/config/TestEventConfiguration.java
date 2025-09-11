package solid.humank.genaidemo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for event publishing to prevent Kafka-related failures
 */
@TestConfiguration
@Profile("test")
public class TestEventConfiguration {

    @Bean
    @Primary
    public TestDeadLetterService testDeadLetterService() {
        return new TestDeadLetterService();
    }

    @Bean
    @Primary
    public TestKafkaEventPublisher testKafkaEventPublisher() {
        return new TestKafkaEventPublisher();
    }

    public static class TestDeadLetterService {
        public void sendToDeadLetterQueue(Object event, Exception cause) {
            // No-op implementation for tests
            System.out.println("Test: Event sent to dead letter queue: " + event.getClass().getSimpleName());
        }
    }

    public static class TestKafkaEventPublisher {
        public void publishEvent(Object event, String topic) {
            // No-op implementation for tests
            System.out.println("Test: Event published to topic " + topic + ": " + event.getClass().getSimpleName());
        }
    }
}