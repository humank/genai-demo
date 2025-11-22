package solid.humank.genaidemo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * Main test configuration that imports all test-specific configurations
 * 
 * This configuration is automatically loaded when running tests with 'test' profile
 * and provides all necessary beans that are missing in test environment.
 */
@TestConfiguration
@Profile("test")
@Import({
    TestSecretsManagerConfiguration.class,
    TestDataSourceConfiguration.class,
    TestMetricsConfiguration.class,
    TestFlywayConfiguration.class,
    TestDatabaseConfiguration.class,
    TestDatabaseValidatorConfiguration.class
})
public class TestApplicationConfiguration {
    // This class serves as an aggregator for all test configurations
}
