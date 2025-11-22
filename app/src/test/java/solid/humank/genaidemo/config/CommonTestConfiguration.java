package solid.humank.genaidemo.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import solid.humank.genaidemo.infrastructure.config.DatabaseConfigurationManager;

import static org.mockito.Mockito.mock;

/**
 * Common test configuration for all test profiles
 * 
 * This configuration provides beans that are commonly needed across all test profiles
 * (test, local, etc.) without profile restrictions.
 */
@TestConfiguration
public class CommonTestConfiguration {

    /**
     * Provide a mock Flyway bean for all tests
     * This prevents tests from failing due to missing Flyway bean
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    public Flyway commonTestFlyway() {
        return mock(Flyway.class);
    }

    /**
     * Provide a mock DatabaseConfigurationManager for all tests
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    public DatabaseConfigurationManager commonTestDatabaseConfigurationManager() {
        return mock(DatabaseConfigurationManager.class);
    }
}
