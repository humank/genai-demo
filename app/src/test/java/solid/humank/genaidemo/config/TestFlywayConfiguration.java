package solid.humank.genaidemo.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

/**
 * Test Flyway configuration
 * 
 * Provides a mock Flyway bean for tests to avoid database migration issues.
 * Tests should not depend on Flyway migrations.
 */
@TestConfiguration
@Profile("test")
public class TestFlywayConfiguration {

    /**
     * Provide a mock Flyway bean for tests
     * This prevents tests from failing due to missing Flyway bean
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    public Flyway testFlyway() {
        return mock(Flyway.class);
    }
}
