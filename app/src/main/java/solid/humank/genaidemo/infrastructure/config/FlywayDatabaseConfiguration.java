package solid.humank.genaidemo.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway Database Configuration
 * Configures Flyway migrations based on the active database configuration
 */
@Configuration
public class FlywayDatabaseConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FlywayDatabaseConfiguration.class);

    /**
     * Customize Flyway configuration based on active database configuration
     * TEMPORARILY DISABLED to avoid circular dependency issues
     */
    // @Bean
    public FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
        return configuration -> {
            log.info(
                    "Using default Flyway configuration - database-specific configuration disabled to avoid circular dependency");

            // Apply basic H2 configuration for development
            configuration
                    .baselineOnMigrate(true)
                    .validateOnMigrate(true)
                    .cleanDisabled(false)
                    .outOfOrder(false)
                    .placeholderReplacement(true)
                    .locations("classpath:db/migration");
        };
    }

    private void configureForH2(org.flywaydb.core.api.configuration.FluentConfiguration configuration) {
        log.info("Applying H2-specific Flyway configuration");

        configuration
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .cleanDisabled(false) // Allow clean in development
                .outOfOrder(false)
                .placeholderReplacement(true)
                .placeholders(java.util.Map.of(
                        "database.type", "h2",
                        "environment", "development"));
    }

    private void configureForPostgreSQL(org.flywaydb.core.api.configuration.FluentConfiguration configuration) {
        log.info("Applying PostgreSQL-specific Flyway configuration");

        configuration
                .baselineOnMigrate(false) // Don't baseline in production
                .validateOnMigrate(true)
                .cleanDisabled(true) // Never allow clean in production
                .outOfOrder(false)
                .placeholderReplacement(true)
                .placeholders(java.util.Map.of(
                        "database.type", "postgresql",
                        "environment", "production"));
    }

    // Note: We don't create a custom Flyway bean to avoid circular dependency
    // Spring Boot's auto-configuration will handle Flyway creation
    // Our FlywayConfigurationCustomizer will customize the configuration
}