package solid.humank.genaidemo.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Development Configuration
 * 
 * Disables unnecessary auto-configurations for development environment
 * to improve startup time and avoid connection errors.
 */
@Configuration
@Profile("dev")
@EnableAutoConfiguration(exclude = {
        KafkaAutoConfiguration.class // Completely disable Kafka in development
})
public class DevelopmentConfiguration {
    // This class intentionally left empty
    // The @EnableAutoConfiguration exclusions are sufficient
}