package solid.humank.genaidemo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * Unified DataSource Configuration
 * 
 * This configuration consolidates all database configuration logic into a single class,
 * handling different environments (local, test, staging, production) and credential sources
 * (local configuration, AWS Secrets Manager) based on active profiles and properties.
 */
@Configuration
public class UnifiedDataSourceConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedDataSourceConfiguration.class);

    private final Environment environment;
    private final SecretsManagerService secretsManagerService;

    public UnifiedDataSourceConfiguration(Environment environment,
                                        @Autowired(required = false) SecretsManagerService secretsManagerService) {
        this.environment = environment;
        this.secretsManagerService = secretsManagerService;
    }

    /**
     * Create the primary DataSource bean based on active profiles and configuration
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean secretsManagerEnabled = environment.getProperty("aws.secretsmanager.enabled", Boolean.class, false);
        
        logger.info("Configuring DataSource for profiles: {} with Secrets Manager: {}", 
                   java.util.Arrays.toString(activeProfiles), secretsManagerEnabled);

        if (secretsManagerEnabled && secretsManagerService != null) {
            return createSecretsManagerDataSource();
        } else {
            return createLocalDataSource();
        }
    }

    /**
     * Create DataSource using AWS Secrets Manager for credentials
     * Used for staging and production environments
     */
    private DataSource createSecretsManagerDataSource() {
        logger.info("Creating DataSource with AWS Secrets Manager credentials");
        
        try {
            SecretsManagerService.DatabaseCredentials credentials = secretsManagerService.getDatabaseCredentials();
            
            HikariConfig config = new HikariConfig();
            
            // Build JDBC URL for PostgreSQL
            String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s?sslmode=require&serverTimezone=UTC",
                                         credentials.getHost(),
                                         credentials.getPort(),
                                         credentials.getDbname());
            
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(credentials.getUsername());
            config.setPassword(credentials.getPassword());
            config.setDriverClassName("org.postgresql.Driver");
            
            // Production connection pool configuration
            config.setMaximumPoolSize(20);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(30000); // 30 seconds
            config.setIdleTimeout(600000); // 10 minutes
            config.setMaxLifetime(1800000); // 30 minutes
            config.setLeakDetectionThreshold(60000); // 1 minute
            
            // Pool name for monitoring
            config.setPoolName("ProductionHikariPool");
            
            // Connection test query
            config.setConnectionTestQuery("SELECT 1");
            
            // PostgreSQL optimization properties
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            
            logger.info("Production DataSource configured successfully with host: {}, database: {}", 
                       credentials.getHost(), credentials.getDbname());
            
            return new HikariDataSource(config);
            
        } catch (Exception e) {
            logger.error("Failed to configure DataSource with Secrets Manager: {}", e.getMessage(), e);
            throw new DatabaseConfigurationException("Failed to configure database with Secrets Manager", e);
        }
    }

    /**
     * Create DataSource for local development and testing using H2 database
     * Used for local, test, and development environments
     */
    private DataSource createLocalDataSource() {
        logger.info("Creating local H2 DataSource for development/testing");
        
        HikariConfig config = new HikariConfig();
        
        // H2 in-memory database configuration with PostgreSQL compatibility mode
        config.setJdbcUrl("jdbc:h2:mem:genaidemo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL");
        config.setUsername("sa");
        config.setPassword(""); // Empty password to match DevelopmentDatabaseConfiguration
        config.setDriverClassName("org.h2.Driver");
        
        // Development connection pool configuration (smaller pool)
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(20000); // 20 seconds
        config.setIdleTimeout(300000); // 5 minutes
        config.setMaxLifetime(900000); // 15 minutes
        
        // Pool name for monitoring
        config.setPoolName("DevelopmentHikariPool");
        
        // Connection test query for H2
        config.setConnectionTestQuery("SELECT 1");
        
        // H2 optimization properties
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "100");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "1024");
        
        logger.info("Local H2 DataSource configured successfully");
        
        return new HikariDataSource(config);
    }

    /**
     * Exception for database configuration errors
     */
    public static class DatabaseConfigurationException extends RuntimeException {
        public DatabaseConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}