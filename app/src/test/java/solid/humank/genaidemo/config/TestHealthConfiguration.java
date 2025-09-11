package solid.humank.genaidemo.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for health indicators to ensure tests pass
 * Uses @Primary to override production health indicators in tests
 */
@TestConfiguration
@Profile("test")
public class TestHealthConfiguration {

    @Bean
    @Primary
    public HealthIndicator databaseHealthIndicator() {
        return () -> Health.up()
                .withDetail("database", "H2")
                .withDetail("status", "UP")
                .withDetail("test", true)
                .build();
    }

    @Bean
    @Primary
    public HealthIndicator applicationReadinessIndicator() {
        return () -> Health.up()
                .withDetail("readiness", "READY")
                .withDetail("status", "UP")
                .withDetail("test", true)
                .build();
    }

    @Bean
    @Primary
    public HealthIndicator systemResourcesIndicator() {
        return () -> Health.up()
                .withDetail("diskSpace", "Available")
                .withDetail("memory", "Available")
                .withDetail("status", "UP")
                .withDetail("test", true)
                .build();
    }
}