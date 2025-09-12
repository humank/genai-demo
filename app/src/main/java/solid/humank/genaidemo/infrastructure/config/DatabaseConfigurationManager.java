package solid.humank.genaidemo.infrastructure.config;

import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

/**
 * Database Configuration Manager
 * Manages database configuration selection and validation based on active
 * profiles
 */
@Configuration
public class DatabaseConfigurationManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfigurationManager.class);

    private final Environment environment;
    private final List<DatabaseConfiguration> databaseConfigurations;
    private DatabaseConfiguration activeDatabaseConfiguration;

    public DatabaseConfigurationManager(Environment environment,
            List<DatabaseConfiguration> databaseConfigurations) {
        this.environment = environment;
        this.databaseConfigurations = databaseConfigurations;
        this.activeDatabaseConfiguration = selectDatabaseConfiguration();
    }

    /**
     * Select the appropriate database configuration based on active profiles
     */
    private DatabaseConfiguration selectDatabaseConfiguration() {
        String[] activeProfiles = environment.getActiveProfiles();

        log.info("Selecting database configuration for active profiles: {}", java.util.Arrays.toString(activeProfiles));

        // If no profiles are active, default to development
        if (activeProfiles.length == 0) {
            log.info("No active profiles found, defaulting to development database configuration");
            return findConfigurationByType("h2");
        }

        // Check for production profile first
        for (String profile : activeProfiles) {
            if ("production".equals(profile)) {
                log.info("Production profile detected, using PostgreSQL database configuration");
                return findConfigurationByType("postgresql");
            }
        }

        // Check for development profile
        for (String profile : activeProfiles) {
            if ("dev".equals(profile)) {
                log.info("Development profile detected, using H2 database configuration");
                return findConfigurationByType("h2");
            }
        }

        // Check for test profile
        for (String profile : activeProfiles) {
            if ("test".equals(profile) || "test-minimal".equals(profile)) {
                log.info("Test profile detected ({}), using H2 database configuration", profile);
                return findConfigurationByType("h2");
            }
        }

        // Default to development if no recognized profile is found
        log.warn("No recognized database profile found in {}, defaulting to H2",
                java.util.Arrays.toString(activeProfiles));
        return findConfigurationByType("h2");
    }

    private DatabaseConfiguration findConfigurationByType(String databaseType) {
        return databaseConfigurations.stream()
                .filter(config -> databaseType.equals(config.getDatabaseType()))
                .findFirst()
                .orElseThrow(() -> new DatabaseConfigurationException(
                        "No database configuration found for type: " + databaseType));
    }

    /**
     * Get the active database configuration
     */
    public DatabaseConfiguration getDatabaseConfiguration() {
        if (activeDatabaseConfiguration == null) {
            throw new DatabaseConfigurationException("Database configuration not initialized");
        }
        return activeDatabaseConfiguration;
    }

    /**
     * Create the primary DataSource bean using the active database configuration
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        log.info("Creating primary DataSource using {} database configuration",
                activeDatabaseConfiguration.getDatabaseType());

        try {
            DataSource dataSource = activeDatabaseConfiguration.createDataSource();
            log.info("DataSource created successfully for database type: {}",
                    activeDatabaseConfiguration.getDatabaseType());
            return dataSource;
        } catch (Exception e) {
            log.error("Failed to create DataSource for database type: {}",
                    activeDatabaseConfiguration.getDatabaseType(), e);
            throw new DatabaseConfigurationException("Failed to create DataSource", e);
        }
    }

    /**
     * Validate database configuration on application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateDatabaseConfigurationOnStartup() {
        log.info("=== Database Configuration Validation ===");
        log.info("Active database type: {}", activeDatabaseConfiguration.getDatabaseType());
        log.info("Migration path: {}", activeDatabaseConfiguration.getMigrationPath());

        try {
            // Validate the database configuration
            activeDatabaseConfiguration.validateConfiguration();

            // Log configuration details
            logConfigurationDetails();

            log.info("Database configuration validation completed successfully");

        } catch (DatabaseConfigurationException e) {
            log.error("Database configuration validation failed: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during database configuration validation", e);
            throw new DatabaseConfigurationException("Database configuration validation failed", e);
        }

        log.info("==========================================");
    }

    private void logConfigurationDetails() {
        try {
            String databaseType = activeDatabaseConfiguration.getDatabaseType();
            String migrationPath = activeDatabaseConfiguration.getMigrationPath();

            log.info("Database Configuration Details:");
            log.info("  - Type: {}", databaseType);
            log.info("  - Migration Path: {}", migrationPath);
            log.info("  - JPA Platform: {}", activeDatabaseConfiguration.getJpaProperties().getDatabasePlatform());
            log.info("  - Show SQL: {}", activeDatabaseConfiguration.getJpaProperties().isShowSql());

            // Log environment-specific details
            if ("postgresql".equals(databaseType)) {
                log.info("  - Environment: Production");
                log.info("  - Connection Pooling: HikariCP");
                log.info("  - SSL: Recommended");
            } else if ("h2".equals(databaseType)) {
                log.info("  - Environment: Development/Test");
                log.info("  - Mode: In-Memory");
                log.info("  - H2 Console: Available at /h2-console");
            }

        } catch (Exception e) {
            log.warn("Could not log configuration details: {}", e.getMessage());
        }
    }

    /**
     * Get database health information
     */
    public DatabaseHealthInfo getDatabaseHealthInfo() {
        try {
            String databaseType = activeDatabaseConfiguration.getDatabaseType();
            String migrationPath = activeDatabaseConfiguration.getMigrationPath();

            // Test connectivity
            activeDatabaseConfiguration.validateConfiguration();

            return new DatabaseHealthInfo(
                    databaseType,
                    migrationPath,
                    "UP",
                    "Database connection successful",
                    System.currentTimeMillis());

        } catch (Exception e) {
            return new DatabaseHealthInfo(
                    activeDatabaseConfiguration.getDatabaseType(),
                    activeDatabaseConfiguration.getMigrationPath(),
                    "DOWN",
                    "Database connection failed: " + e.getMessage(),
                    System.currentTimeMillis());
        }
    }

    /**
     * Database Health Information Record
     */
    public record DatabaseHealthInfo(
            String databaseType,
            String migrationPath,
            String status,
            String message,
            long timestamp) {
    }
}