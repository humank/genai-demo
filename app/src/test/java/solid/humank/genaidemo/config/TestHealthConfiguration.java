package solid.humank.genaidemo.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration for health check components
 */
@TestConfiguration
public class TestHealthConfiguration {

    @Bean
    @Primary
    public HealthIndicator databaseHealthIndicator() {
        return () -> Health.up()
                .withDetail("database", "H2 test database")
                .withDetail("status", "UP")
                .build();
    }

    @Bean
    public HealthIndicator applicationReadinessIndicator() {
        return () -> Health.up()
                .withDetail("readiness", "Application is ready")
                .withDetail("status", "UP")
                .build();
    }

    @Bean
    public HealthIndicator systemResourcesIndicator() {
        return () -> Health.up()
                .withDetail("diskSpace", "Available")
                .withDetail("ping", "UP")
                .withDetail("status", "UP")
                .build();
    }
}