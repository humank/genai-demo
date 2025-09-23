package solid.humank.genaidemo.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Test Profile Configuration
 * 
 * Disables unnecessary auto-configurations for test environment
 * to improve test startup time and avoid connection errors.
 */
@Configuration
@Profile("test")
@EnableAutoConfiguration(exclude = {
        KafkaAutoConfiguration.class // Completely disable Kafka in tests
})
public class TestProfileConfiguration {
    // This class intentionally left empty
    // The @EnableAutoConfiguration exclusions are sufficient
}