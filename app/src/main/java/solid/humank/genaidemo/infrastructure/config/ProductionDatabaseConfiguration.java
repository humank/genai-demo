package solid.humank.genaidemo.infrastructure.config;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Production Database Configuration
 * Configures PostgreSQL database for production environment with connection pooling
 */
@Component
@Profile("production")
public class ProductionDatabaseConfiguration implements DatabaseConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(ProductionDatabaseConfiguration.class);
    
    private static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";
    private static final String MIGRATION_PATH = "classpath:db/migration/postgresql";
    
    private final String dbHost;
    private final String dbPort;
    private final String dbName;
    private final String dbUsername;
    private final String dbPassword;
    
    // HikariCP configuration
    private final int maximumPoolSize;
    private final int minimumIdle;
    private final long connectionTimeout;
    private final long idleTimeout;
    private final long maxLifetime;
    private final long leakDetectionThreshold;
    
    public ProductionDatabaseConfiguration(
            @Value("${DB_HOST:localhost}") String dbHost,
            @Value("${DB_PORT:5432}") String dbPort,
            @Value("${DB_NAME:genaidemo}") String dbName,
            @Value("${DB_USERNAME:genaidemo}") String dbUsername,
            @Value("${DB_PASSWORD:}") String dbPassword,
            @Value("${spring.datasource.hikari.maximum-pool-size:20}") int maximumPoolSize,
            @Value("${spring.datasource.hikari.minimum-idle:5}") int minimumIdle,
            @Value("${spring.datasource.hikari.connection-timeout:30000}") long connectionTimeout,
            @Value("${spring.datasource.hikari.idle-timeout:600000}") long idleTimeout,
            @Value("${spring.datasource.hikari.max-lifetime:1800000}") long maxLifetime,
            @Value("${spring.datasource.hikari.leak-detection-threshold:60000}") long leakDetectionThreshold) {
        
        this.dbHost = dbHost;
        this.dbPort = dbPort;
        this.dbName = dbName;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.maximumPoolSize = maximumPoolSize;
        this.minimumIdle = minimumIdle;
        this.connectionTimeout = connectionTimeout;
        this.idleTimeout = idleTimeout;
        this.maxLifetime = maxLifetime;
        this.leakDetectionThreshold = leakDetectionThreshold;
    }
    
    @Override
    public DataSource createDataSource() {
        log.info("Creating PostgreSQL DataSource for production environment");
        log.info("Database connection: {}:{}/{}", dbHost, dbPort, dbName);
        
        String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName);
        
        // Use HikariCP for production connection pooling
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(POSTGRESQL_DRIVER);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        
        // Connection pool configuration
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setLeakDetectionThreshold(leakDetectionThreshold);
        
        // PostgreSQL specific optimizations
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
        
        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        log.info("HikariCP configuration: maxPoolSize={}, minIdle={}, connectionTimeout={}ms", 
                maximumPoolSize, minimumIdle, connectionTimeout);
        
        return new HikariDataSource(config);
    }
    
    @Override
    public FluentConfiguration getFlywayConfiguration() {
        log.info("Configuring Flyway for PostgreSQL database with migration path: {}", MIGRATION_PATH);
        
        return new FluentConfiguration()
                .dataSource(createDataSource())
                .locations(MIGRATION_PATH)
                .baselineOnMigrate(false) // Don't baseline in production
                .validateOnMigrate(true)
                .cleanDisabled(true) // Never allow clean in production
                .outOfOrder(false)
                .placeholderReplacement(false);
    }
    
    @Override
    public JpaProperties getJpaProperties() {
        JpaProperties properties = new JpaProperties();
        properties.setDatabasePlatform("org.hibernate.dialect.PostgreSQLDialect");
        properties.setShowSql(false); // Disable SQL logging in production
        
        // Hibernate properties for production
        properties.getProperties().put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.getProperties().put("hibernate.format_sql", "false");
        properties.getProperties().put("hibernate.hbm2ddl.auto", "validate"); // Use Flyway for schema management
        
        // Performance optimizations
        properties.getProperties().put("hibernate.jdbc.batch_size", "20");
        properties.getProperties().put("hibernate.order_inserts", "true");
        properties.getProperties().put("hibernate.order_updates", "true");
        properties.getProperties().put("hibernate.jdbc.batch_versioned_data", "true");
        
        // Connection handling
        properties.getProperties().put("hibernate.connection.provider_disables_autocommit", "true");
        
        return properties;
    }
    
    @Override
    public void validateConfiguration() throws DatabaseConfigurationException {
        log.info("Validating PostgreSQL database configuration for production environment");
        
        // Validate required environment variables
        validateEnvironmentVariables();
        
        try {
            DataSource dataSource = createDataSource();
            
            // Test database connectivity
            try (Connection connection = dataSource.getConnection()) {
                if (connection == null || connection.isClosed()) {
                    throw new DatabaseConfigurationException("Failed to establish PostgreSQL database connection");
                }
                
                // Verify PostgreSQL version and configuration
                String databaseProductName = connection.getMetaData().getDatabaseProductName();
                String databaseProductVersion = connection.getMetaData().getDatabaseProductVersion();
                String driverVersion = connection.getMetaData().getDriverVersion();
                
                log.info("PostgreSQL connection successful. Database: {} {}, Driver: {}", 
                        databaseProductName, databaseProductVersion, driverVersion);
                
                // Test basic query execution
                try (var statement = connection.createStatement()) {
                    var resultSet = statement.executeQuery("SELECT version()");
                    if (resultSet.next()) {
                        String version = resultSet.getString(1);
                        log.info("PostgreSQL version: {}", version);
                    }
                }
                
                // Validate database schema exists
                try (var statement = connection.createStatement()) {
                    var resultSet = statement.executeQuery(
                        "SELECT schema_name FROM information_schema.schemata WHERE schema_name = 'public'"
                    );
                    if (!resultSet.next()) {
                        throw new DatabaseConfigurationException("Public schema not found in PostgreSQL database");
                    }
                }
                
                // Check connection pool if using HikariCP
                if (dataSource instanceof HikariDataSource hikariDataSource) {
                    log.info("HikariCP pool status - Active: {}, Idle: {}, Total: {}, Waiting: {}", 
                            hikariDataSource.getHikariPoolMXBean().getActiveConnections(),
                            hikariDataSource.getHikariPoolMXBean().getIdleConnections(),
                            hikariDataSource.getHikariPoolMXBean().getTotalConnections(),
                            hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
                }
                
            }
        } catch (SQLException e) {
            throw new DatabaseConfigurationException("PostgreSQL database validation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new DatabaseConfigurationException("PostgreSQL database validation failed: " + e.getMessage(), e);
        }
        
        log.info("PostgreSQL database configuration validation completed successfully");
    }
    
    private void validateEnvironmentVariables() throws DatabaseConfigurationException {
        if (dbHost == null || dbHost.trim().isEmpty()) {
            throw new DatabaseConfigurationException("DB_HOST environment variable is required for production");
        }
        
        if (dbName == null || dbName.trim().isEmpty()) {
            throw new DatabaseConfigurationException("DB_NAME environment variable is required for production");
        }
        
        if (dbUsername == null || dbUsername.trim().isEmpty()) {
            throw new DatabaseConfigurationException("DB_USERNAME environment variable is required for production");
        }
        
        if (dbPassword == null || dbPassword.trim().isEmpty()) {
            log.warn("DB_PASSWORD is empty - this may cause authentication issues in production");
        }
        
        // Validate port is numeric
        try {
            int port = Integer.parseInt(dbPort);
            if (port < 1 || port > 65535) {
                throw new DatabaseConfigurationException("DB_PORT must be between 1 and 65535, got: " + port);
            }
        } catch (NumberFormatException e) {
            throw new DatabaseConfigurationException("DB_PORT must be a valid number, got: " + dbPort);
        }
        
        log.info("Environment variables validation completed successfully");
    }
    
    @Override
    public String getDatabaseType() {
        return "postgresql";
    }
    
    @Override
    public String getMigrationPath() {
        return MIGRATION_PATH;
    }
}