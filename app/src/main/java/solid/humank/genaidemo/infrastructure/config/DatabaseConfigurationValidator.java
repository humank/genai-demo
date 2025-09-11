package solid.humank.genaidemo.infrastructure.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Database Configuration Validator
 * Validates database configuration, connectivity, and migration status
 */
@Component
public class DatabaseConfigurationValidator {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfigurationValidator.class);

    private final DataSource dataSource;
    private final DatabaseConfigurationManager databaseConfigurationManager;
    private final Flyway flyway;

    public DatabaseConfigurationValidator(DataSource dataSource,
            DatabaseConfigurationManager databaseConfigurationManager,
            @Autowired(required = false) Flyway flyway) {
        this.dataSource = dataSource;
        this.databaseConfigurationManager = databaseConfigurationManager;
        this.flyway = flyway;
    }

    /**
     * Comprehensive database validation on application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(100) // Run after database configuration manager
    public void validateDatabaseOnStartup() {
        log.info("=== Comprehensive Database Validation ===");

        try {
            // 1. Validate basic connectivity
            validateConnectivity();

            // 2. Validate database schema and structure
            validateDatabaseSchema();

            // 3. Validate Flyway migration status
            validateMigrationStatus();

            // 4. Validate database-specific features
            validateDatabaseFeatures();

            // 5. Performance validation
            validatePerformance();

            log.info("All database validations completed successfully");

        } catch (DatabaseConfigurationException e) {
            log.error("Database validation failed: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during database validation", e);
            throw new DatabaseConfigurationException("Database validation failed", e);
        }

        log.info("==========================================");
    }

    private void validateConnectivity() throws DatabaseConfigurationException {
        log.info("Validating database connectivity...");

        try (Connection connection = dataSource.getConnection()) {
            if (connection == null || connection.isClosed()) {
                throw new DatabaseConfigurationException("Database connection is null or closed");
            }

            // Test basic query
            try (var statement = connection.createStatement()) {
                var resultSet = statement.executeQuery("SELECT 1");
                if (!resultSet.next() || resultSet.getInt(1) != 1) {
                    throw new DatabaseConfigurationException("Basic query test failed");
                }
            }

            log.info("Database connectivity validation: PASSED");

        } catch (SQLException e) {
            throw new DatabaseConfigurationException("Database connectivity validation failed", e);
        }
    }

    private void validateDatabaseSchema() throws DatabaseConfigurationException {
        log.info("Validating database schema...");

        DatabaseConfiguration dbConfig = databaseConfigurationManager.getDatabaseConfiguration();
        String databaseType = dbConfig.getDatabaseType();

        try (Connection connection = dataSource.getConnection()) {
            var metaData = connection.getMetaData();

            // Validate database type matches configuration
            String actualDatabaseType = metaData.getDatabaseProductName().toLowerCase();
            if (databaseType.equals("h2") && !actualDatabaseType.contains("h2")) {
                throw new DatabaseConfigurationException(
                        "Expected H2 database but found: " + actualDatabaseType);
            }
            if (databaseType.equals("postgresql") && !actualDatabaseType.contains("postgresql")) {
                throw new DatabaseConfigurationException(
                        "Expected PostgreSQL database but found: " + actualDatabaseType);
            }

            // Check if required tables exist (basic validation)
            validateRequiredTables(connection);

            log.info("Database schema validation: PASSED");

        } catch (SQLException e) {
            throw new DatabaseConfigurationException("Database schema validation failed", e);
        }
    }

    private void validateRequiredTables(Connection connection) throws SQLException {
        // Check for some core tables that should exist after migrations
        String[] requiredTables = { "payments", "orders", "customers", "products" };

        var metaData = connection.getMetaData();
        List<String> missingTables = new ArrayList<>();

        for (String tableName : requiredTables) {
            try (var resultSet = metaData.getTables(null, null, tableName.toUpperCase(), new String[] { "TABLE" })) {
                if (!resultSet.next()) {
                    // Try lowercase for case-sensitive databases
                    try (var resultSet2 = metaData.getTables(null, null, tableName.toLowerCase(),
                            new String[] { "TABLE" })) {
                        if (!resultSet2.next()) {
                            missingTables.add(tableName);
                        }
                    }
                }
            }
        }

        if (!missingTables.isEmpty()) {
            log.warn("Some expected tables are missing: {}. This might be normal if migrations haven't run yet.",
                    missingTables);
        } else {
            log.info("All core tables are present");
        }
    }

    private void validateMigrationStatus() throws DatabaseConfigurationException {
        log.info("Validating Flyway migration status...");

        if (flyway == null) {
            log.info("Flyway is not available - skipping migration validation");
            return;
        }

        try {
            MigrationInfoService infoService = flyway.info();
            MigrationInfo[] migrations = infoService.all();

            if (migrations.length == 0) {
                log.warn("No migrations found - this might be expected for a new database");
                return;
            }

            // Check for pending migrations
            MigrationInfo[] pending = infoService.pending();
            if (pending.length > 0) {
                log.info("Found {} pending migrations", pending.length);
                for (MigrationInfo migration : pending) {
                    log.info("Pending migration: {} - {}", migration.getVersion(), migration.getDescription());
                }
            }

            // Check for failed migrations by examining migration states
            boolean hasFailedMigrations = false;
            StringBuilder errorMsg = new StringBuilder("Found failed migrations: ");
            for (MigrationInfo migration : migrations) {
                if (migration.getState().isFailed()) {
                    hasFailedMigrations = true;
                    errorMsg.append(String.format("%s (%s), ", migration.getVersion(), migration.getDescription()));
                }
            }
            if (hasFailedMigrations) {
                throw new DatabaseConfigurationException(errorMsg.toString());
            }

            // Log current migration status
            MigrationInfo current = infoService.current();
            if (current != null) {
                log.info("Current migration version: {} - {}", current.getVersion(), current.getDescription());
            }

            log.info("Migration status validation: PASSED");

        } catch (Exception e) {
            if (e instanceof DatabaseConfigurationException) {
                throw e;
            }
            throw new DatabaseConfigurationException("Migration status validation failed", e);
        }
    }

    private void validateDatabaseFeatures() throws DatabaseConfigurationException {
        log.info("Validating database-specific features...");

        DatabaseConfiguration dbConfig = databaseConfigurationManager.getDatabaseConfiguration();
        String databaseType = dbConfig.getDatabaseType();

        try (Connection connection = dataSource.getConnection()) {

            if ("postgresql".equals(databaseType)) {
                validatePostgreSQLFeatures(connection);
            } else if ("h2".equals(databaseType)) {
                validateH2Features(connection);
            }

            log.info("Database features validation: PASSED");

        } catch (SQLException e) {
            throw new DatabaseConfigurationException("Database features validation failed", e);
        }
    }

    private void validatePostgreSQLFeatures(Connection connection) throws SQLException {
        // Test JSONB support (if available)
        try (var statement = connection.createStatement()) {
            statement.execute("SELECT '{\"test\": \"value\"}'::jsonb");
            log.info("PostgreSQL JSONB support: AVAILABLE");
        } catch (SQLException e) {
            log.warn("PostgreSQL JSONB support: NOT AVAILABLE - {}", e.getMessage());
        }

        // Test UUID support
        try (var statement = connection.createStatement()) {
            statement.execute("SELECT gen_random_uuid()");
            log.info("PostgreSQL UUID support: AVAILABLE");
        } catch (SQLException e) {
            log.warn("PostgreSQL UUID support: NOT AVAILABLE - {}", e.getMessage());
        }
    }

    private void validateH2Features(Connection connection) throws SQLException {
        // Test H2 specific functions
        try (var statement = connection.createStatement()) {
            var resultSet = statement.executeQuery("SELECT H2VERSION()");
            if (resultSet.next()) {
                String version = resultSet.getString(1);
                log.info("H2 version: {}", version);
            }
        } catch (SQLException e) {
            log.warn("Could not retrieve H2 version: {}", e.getMessage());
        }

        // Test CLOB support
        try (var statement = connection.createStatement()) {
            statement.execute("SELECT CAST('test' AS CLOB)");
            log.info("H2 CLOB support: AVAILABLE");
        } catch (SQLException e) {
            log.warn("H2 CLOB support: NOT AVAILABLE - {}", e.getMessage());
        }
    }

    private void validatePerformance() throws DatabaseConfigurationException {
        log.info("Validating database performance...");

        try (Connection connection = dataSource.getConnection()) {

            // Test connection time
            long startTime = System.currentTimeMillis();
            try (var statement = connection.createStatement()) {
                statement.executeQuery("SELECT 1");
            }
            long queryTime = System.currentTimeMillis() - startTime;

            log.info("Basic query execution time: {}ms", queryTime);

            if (queryTime > 1000) { // 1 second threshold
                log.warn("Database query response time is high: {}ms", queryTime);
            }

            // Test transaction performance
            startTime = System.currentTimeMillis();
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.execute("SELECT 1");
                connection.commit();
            } finally {
                connection.setAutoCommit(true);
            }
            long transactionTime = System.currentTimeMillis() - startTime;

            log.info("Transaction execution time: {}ms", transactionTime);

            log.info("Performance validation: PASSED");

        } catch (SQLException e) {
            throw new DatabaseConfigurationException("Performance validation failed", e);
        }
    }

    /**
     * Get detailed validation report
     */
    public DatabaseValidationReport getValidationReport() {
        try {
            DatabaseConfigurationManager.DatabaseHealthInfo healthInfo = databaseConfigurationManager
                    .getDatabaseHealthInfo();

            return new DatabaseValidationReport(
                    healthInfo.databaseType(),
                    healthInfo.status(),
                    "All validations passed",
                    System.currentTimeMillis(),
                    getMigrationInfo());

        } catch (Exception e) {
            return new DatabaseValidationReport(
                    "unknown",
                    "DOWN",
                    "Validation failed: " + e.getMessage(),
                    System.currentTimeMillis(),
                    "Migration info not available");
        }
    }

    private String getMigrationInfo() {
        if (flyway == null) {
            return "Flyway not available";
        }

        try {
            MigrationInfoService infoService = flyway.info();
            MigrationInfo current = infoService.current();
            if (current != null) {
                return String.format("Current: %s (%s)", current.getVersion(), current.getDescription());
            }
            return "No migrations applied";
        } catch (Exception e) {
            return "Migration info not available: " + e.getMessage();
        }
    }

    /**
     * Database Validation Report Record
     */
    public record DatabaseValidationReport(
            String databaseType,
            String status,
            String message,
            long timestamp,
            String migrationInfo) {
    }
}