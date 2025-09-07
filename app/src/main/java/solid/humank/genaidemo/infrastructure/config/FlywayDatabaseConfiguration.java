package solid.humank.genaidemo.infrastructure.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Flyway Database Configuration
 * Configures Flyway migrations based on the active database configuration
 */
@Configuration
@DependsOn("databaseConfigurationManager")
public class FlywayDatabaseConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(FlywayDatabaseConfiguration.class);
    
    private final DatabaseConfigurationManager databaseConfigurationManager;
    
    public FlywayDatabaseConfiguration(DatabaseConfigurationManager databaseConfigurationManager) {
        this.databaseConfigurationManager = databaseConfigurationManager;
    }
    
    /**
     * Customize Flyway configuration based on active database configuration
     */
    @Bean
    public FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
        return configuration -> {
            DatabaseConfiguration dbConfig = databaseConfigurationManager.getDatabaseConfiguration();
            String migrationPath = dbConfig.getMigrationPath();
            String databaseType = dbConfig.getDatabaseType();
            
            log.info("Configuring Flyway for database type: {} with migration path: {}", databaseType, migrationPath);
            
            // Set migration locations based on database type
            configuration.locations(migrationPath);
            
            // Configure based on database type
            switch (databaseType) {
                case "h2" -> configureForH2(configuration);
                case "postgresql" -> configureForPostgreSQL(configuration);
                default -> throw new DatabaseConfigurationException("Unsupported database type: " + databaseType);
            }
            
            log.info("Flyway configuration completed for database type: {}", databaseType);
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
                "environment", "development"
            ));
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
                "environment", "production"
            ));
    }
    
    /**
     * Create Flyway instance with custom configuration
     */
    @Bean
    public Flyway flyway() {
        DatabaseConfiguration dbConfig = databaseConfigurationManager.getDatabaseConfiguration();
        
        log.info("Creating Flyway instance for database type: {}", dbConfig.getDatabaseType());
        
        return dbConfig.getFlywayConfiguration()
            .load();
    }
}