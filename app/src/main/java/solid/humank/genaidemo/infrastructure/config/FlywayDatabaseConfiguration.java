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

    // Spring Boot's auto-configuration will handle Flyway creation
    // Database-specific configuration can be added via application.yml:
    // spring.flyway.baseline-on-migrate=true (for H2)
    // spring.flyway.clean-disabled=true (for PostgreSQL)
}
