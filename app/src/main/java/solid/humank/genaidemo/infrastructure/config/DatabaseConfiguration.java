package solid.humank.genaidemo.infrastructure.config;

import javax.sql.DataSource;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;

/**
 * Database Configuration Strategy Interface
 * Defines the contract for database configuration across different environments
 */
public interface DatabaseConfiguration {
    
    /**
     * Create and configure the DataSource for the specific environment
     * @return configured DataSource
     */
    DataSource createDataSource();
    
    /**
     * Get Flyway configuration for database migrations
     * @return Flyway configuration
     */
    FluentConfiguration getFlywayConfiguration();
    
    /**
     * Get JPA properties for the specific database
     * @return JPA properties
     */
    JpaProperties getJpaProperties();
    
    /**
     * Validate database connectivity and configuration
     * @throws DatabaseConfigurationException if validation fails
     */
    void validateConfiguration() throws DatabaseConfigurationException;
    
    /**
     * Get the database type identifier
     * @return database type (h2, postgresql, etc.)
     */
    String getDatabaseType();
    
    /**
     * Get the migration path for this database type
     * @return migration path
     */
    String getMigrationPath();
}