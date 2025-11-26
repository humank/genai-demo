package solid.humank.genaidemo.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import solid.humank.genaidemo.infrastructure.config.DatabaseConfigurationManager;
import solid.humank.genaidemo.infrastructure.config.DatabaseConfigurationValidator;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test configuration for DatabaseConfigurationValidator
 * 
 * Provides a no-op validator that doesn't perform any validation in tests.
 */
@TestConfiguration
@Profile("test")
public class TestDatabaseValidatorConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TestDatabaseValidatorConfiguration.class);

    /**
     * Provide a no-op DatabaseConfigurationValidator for tests
     * This completely disables database validation in test environment
     */
    @Bean
    @Primary
    public DatabaseConfigurationValidator testDatabaseConfigurationValidator(
            DataSource dataSource,
            DatabaseConfigurationManager databaseConfigurationManager,
            ObjectProvider<Flyway> flywayProvider) {

        log.info("Using test DatabaseConfigurationValidator - validation disabled");

        return new DatabaseConfigurationValidator(dataSource, databaseConfigurationManager, flywayProvider) {
            @Override
            public void validateDatabaseOnStartup() {
                log.debug("Database validation skipped in test environment");
                // No-op - do nothing in tests
            }
        };
    }
}
