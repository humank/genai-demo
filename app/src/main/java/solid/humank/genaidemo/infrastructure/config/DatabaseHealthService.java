package solid.humank.genaidemo.infrastructure.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Database Health Service
 * Provides detailed health information about the database connection
 */
@Component
public class DatabaseHealthService {
    
    private static final Logger log = LoggerFactory.getLogger(DatabaseHealthService.class);
    
    private final DataSource dataSource;
    private final DatabaseConfigurationManager databaseConfigurationManager;
    
    public DatabaseHealthService(DataSource dataSource, 
                                 DatabaseConfigurationManager databaseConfigurationManager) {
        this.dataSource = dataSource;
        this.databaseConfigurationManager = databaseConfigurationManager;
    }
    
    public DatabaseHealthStatus checkHealth() {
        try {
            return checkDatabaseHealth();
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return new DatabaseHealthStatus(
                "DOWN",
                Map.of("error", e.getMessage()),
                System.currentTimeMillis()
            );
        }
    }
    
    private DatabaseHealthStatus checkDatabaseHealth() {
        Map<String, Object> details = new HashMap<>();
        
        try {
            // Get database configuration info
            DatabaseConfigurationManager.DatabaseHealthInfo healthInfo = 
                databaseConfigurationManager.getDatabaseHealthInfo();
            
            details.put("database", healthInfo.databaseType());
            details.put("migrationPath", healthInfo.migrationPath());
            details.put("status", healthInfo.status());
            
            // Test database connectivity
            long startTime = System.currentTimeMillis();
            try (Connection connection = dataSource.getConnection()) {
                long connectionTime = System.currentTimeMillis() - startTime;
                details.put("connectionTime", connectionTime + "ms");
                
                if (connection == null || connection.isClosed()) {
                    details.put("error", "Connection is null or closed");
                    return new DatabaseHealthStatus("DOWN", details, System.currentTimeMillis());
                }
                
                // Get database metadata
                var metaData = connection.getMetaData();
                details.put("databaseProductName", metaData.getDatabaseProductName());
                details.put("databaseProductVersion", metaData.getDatabaseProductVersion());
                details.put("driverName", metaData.getDriverName());
                details.put("driverVersion", metaData.getDriverVersion());
                details.put("url", metaData.getURL());
                
                // Test query execution
                long queryStartTime = System.currentTimeMillis();
                try (var statement = connection.createStatement()) {
                    var resultSet = statement.executeQuery("SELECT 1");
                    if (resultSet.next()) {
                        long queryTime = System.currentTimeMillis() - queryStartTime;
                        details.put("queryTime", queryTime + "ms");
                        details.put("queryResult", "SUCCESS");
                    }
                }
                
                // Add connection pool information if available
                addConnectionPoolInfo(details);
                
                return new DatabaseHealthStatus("UP", details, System.currentTimeMillis());
                
            }
        } catch (SQLException e) {
            log.error("Database connectivity test failed", e);
            details.put("error", e.getMessage());
            details.put("sqlState", e.getSQLState());
            details.put("errorCode", e.getErrorCode());
            
            return new DatabaseHealthStatus("DOWN", details, System.currentTimeMillis());
        }
    }
    
    private void addConnectionPoolInfo(Map<String, Object> details) {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            try {
                var poolMXBean = hikariDataSource.getHikariPoolMXBean();
                Map<String, Object> poolInfo = new HashMap<>();
                
                poolInfo.put("activeConnections", poolMXBean.getActiveConnections());
                poolInfo.put("idleConnections", poolMXBean.getIdleConnections());
                poolInfo.put("totalConnections", poolMXBean.getTotalConnections());
                poolInfo.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());
                
                var config = hikariDataSource.getHikariConfigMXBean();
                poolInfo.put("maximumPoolSize", config.getMaximumPoolSize());
                poolInfo.put("minimumIdle", config.getMinimumIdle());
                poolInfo.put("connectionTimeout", config.getConnectionTimeout());
                poolInfo.put("idleTimeout", config.getIdleTimeout());
                poolInfo.put("maxLifetime", config.getMaxLifetime());
                
                details.put("connectionPool", poolInfo);
                details.put("poolType", "HikariCP");
                
            } catch (Exception e) {
                log.warn("Could not retrieve connection pool information", e);
                details.put("connectionPool", "Information not available");
            }
        } else {
            details.put("connectionPool", "Not using HikariCP");
        }
    }
    
    /**
     * Database Health Status Record
     */
    public record DatabaseHealthStatus(
        String status,
        Map<String, Object> details,
        long timestamp
    ) {}
}