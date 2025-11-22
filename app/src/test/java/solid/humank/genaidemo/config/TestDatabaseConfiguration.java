package solid.humank.genaidemo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import solid.humank.genaidemo.infrastructure.config.DatabaseConfigurationManager;

import static org.mockito.Mockito.mock;

/**
 * Test Database configuration
 * 
 * Provides mock implementations for database-related beans in tests.
 */
@TestConfiguration
@Profile("test")
public class TestDatabaseConfiguration {

    /**
     * Provide a mock DatabaseConfigurationManager for tests
     */
    @Bean
    @Primary
    public DatabaseConfigurationManager testDatabaseConfigurationManager() {
        return mock(DatabaseConfigurationManager.class);
    }
}
