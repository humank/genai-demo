package solid.humank.genaidemo.infrastructure.config;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * H2 Database Configuration
 * Configures H2 in-memory database for development and test environments
 */
@Component
@Profile({ "local", "test" })
public class DevelopmentDatabaseConfiguration implements DatabaseConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DevelopmentDatabaseConfiguration.class);

    private static final String H2_URL = "jdbc:h2:mem:genaidemo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    private static final String H2_DRIVER = "org.h2.Driver";
    private static final String H2_USERNAME = "sa";
    private static final String H2_PASSWORD = "";
    private static final String MIGRATION_PATH = "classpath:db/migration/h2";

    @Override
    public DataSource createDataSource() {
        log.info("Creating H2 in-memory DataSource for development/test environment");

        return DataSourceBuilder.create()
                .driverClassName(H2_DRIVER)
                .url(H2_URL)
                .username(H2_USERNAME)
                .password(H2_PASSWORD)
                .build();
    }

    @Override
    public FluentConfiguration getFlywayConfiguration() {
        log.info("Configuring Flyway for H2 database with migration path: {}", MIGRATION_PATH);

        return new FluentConfiguration()
                .dataSource(createDataSource())
                .locations(MIGRATION_PATH)
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .cleanDisabled(false) // Allow clean in development
                .outOfOrder(false);
    }

    @Override
    public JpaProperties getJpaProperties() {
        JpaProperties properties = new JpaProperties();
        properties.setDatabasePlatform("org.hibernate.dialect.H2Dialect");
        properties.setShowSql(true);

        // Hibernate properties for development
        properties.getProperties().put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.getProperties().put("hibernate.format_sql", "true");
        properties.getProperties().put("hibernate.use_sql_comments", "true");
        properties.getProperties().put("hibernate.hbm2ddl.auto", "validate"); // Use Flyway for schema management

        return properties;
    }

    @Override
    public void validateConfiguration() throws DatabaseConfigurationException {
        log.info("Validating H2 database configuration for development/test environment");

        try {
            DataSource dataSource = createDataSource();

            // Test database connectivity
            try (Connection connection = dataSource.getConnection()) {
                if (connection == null || connection.isClosed()) {
                    throw new DatabaseConfigurationException("Failed to establish H2 database connection");
                }

                // Verify H2 driver is available
                String driverVersion = connection.getMetaData().getDriverVersion();
                log.info("H2 database connection successful. Driver version: {}", driverVersion);

                // Test basic query execution
                try (var statement = connection.createStatement()) {
                    statement.execute("SELECT 1");
                    log.info("H2 database query test successful");
                }

            }
        } catch (SQLException e) {
            throw new DatabaseConfigurationException("H2 database validation failed", e);
        } catch (Exception e) {
            throw new DatabaseConfigurationException("Unexpected error during H2 database validation", e);
        }

        log.info("H2 database configuration validation completed successfully");
    }

    @Override
    public String getDatabaseType() {
        return "h2";
    }

    @Override
    public String getMigrationPath() {
        return MIGRATION_PATH;
    }
}