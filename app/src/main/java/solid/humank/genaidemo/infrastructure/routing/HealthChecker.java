package solid.humank.genaidemo.infrastructure.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HealthChecker performs periodic health checks on service endpoints.
 * 
 * This component continuously monitors the health of database, cache, and
 * messaging endpoints across multiple regions, providing real-time health
 * status for intelligent routing decisions.
 * 
 * Health checks run every 5 seconds by default and track:
 * - Endpoint availability
 * - Response latency
 * - Error conditions
 */
@Component
public class HealthChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthChecker.class);
    
    private final Map<String, EndpointHealth> healthCache = new ConcurrentHashMap<>();
    private final Map<String, DataSource> dataSourceRegistry = new ConcurrentHashMap<>();
    
    /**
     * Registers a DataSource for health monitoring.
     * 
     * @param name unique identifier for the data source (e.g., "taiwan-db", "japan-db")
     * @param dataSource the DataSource to monitor
     */
    public void registerDataSource(String name, DataSource dataSource) {
        dataSourceRegistry.put(name, dataSource);
        logger.info("Registered DataSource for health monitoring: {}", name);
    }
    
    /**
     * Performs health check on all registered endpoints.
     * Runs every 5 seconds.
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 1000)
    public void performHealthChecks() {
        logger.debug("Starting health checks for {} endpoints", dataSourceRegistry.size());
        
        dataSourceRegistry.forEach((name, dataSource) -> {
            EndpointHealth health = checkDataSourceHealth(name, dataSource);
            healthCache.put(name, health);
            
            if (!health.isHealthy()) {
                logger.warn("Endpoint {} is unhealthy: {}", name, health.errorMessage());
            } else {
                logger.debug("Endpoint {} is healthy (latency: {}ms)", name, health.latencyMs());
            }
        });
    }
    
    /**
     * Gets the current health status of an endpoint.
     * 
     * @param endpointName the endpoint identifier
     * @return the health status, or unhealthy if not found
     */
    public EndpointHealth getHealth(String endpointName) {
        EndpointHealth health = healthCache.get(endpointName);
        if (health == null) {
            return EndpointHealth.unhealthy(endpointName, "No health data available");
        }
        
        // Check if health data is stale (older than 30 seconds)
        if (health.isStale(30)) {
            return EndpointHealth.unhealthy(endpointName, "Health data is stale");
        }
        
        return health;
    }
    
    /**
     * Checks if an endpoint is currently healthy.
     * 
     * @param endpointName the endpoint identifier
     * @return true if the endpoint is healthy
     */
    public boolean isHealthy(String endpointName) {
        return getHealth(endpointName).isHealthy();
    }
    
    /**
     * Gets the latency of the last health check for an endpoint.
     * 
     * @param endpointName the endpoint identifier
     * @return latency in milliseconds, or -1 if unavailable
     */
    public long getLatency(String endpointName) {
        EndpointHealth health = healthCache.get(endpointName);
        return health != null ? health.latencyMs() : -1;
    }
    
    /**
     * Gets all current health statuses.
     * 
     * @return map of endpoint names to their health status
     */
    public Map<String, EndpointHealth> getAllHealth() {
        return Map.copyOf(healthCache);
    }
    
    private EndpointHealth checkDataSourceHealth(String name, DataSource dataSource) {
        long startTime = System.currentTimeMillis();
        
        try (Connection connection = dataSource.getConnection()) {
            // Simple validation query
            if (connection.isValid(2)) {
                long latency = System.currentTimeMillis() - startTime;
                return EndpointHealth.healthy(name, latency);
            } else {
                return EndpointHealth.unhealthy(name, "Connection validation failed");
            }
        } catch (SQLException e) {
            logger.error("Health check failed for {}: {}", name, e.getMessage());
            return EndpointHealth.unhealthy(name, e.getMessage());
        }
    }
}
