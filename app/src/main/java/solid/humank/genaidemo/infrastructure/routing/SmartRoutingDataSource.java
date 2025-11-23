package solid.humank.genaidemo.infrastructure.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * SmartRoutingDataSource provides dynamic DataSource routing based on region and health.
 * 
 * This component extends Spring's AbstractRoutingDataSource to enable intelligent
 * database connection routing in Active-Active multi-region architecture.
 * 
 * Key features:
 * - Automatic region detection
 * - Health-based routing
 * - Transparent failover
 * - Zero application code changes required
 * 
 * The routing decision is made per database operation, allowing real-time
 * adaptation to changing health conditions.
 */
public class SmartRoutingDataSource extends AbstractRoutingDataSource {    private static final Logger logger = LoggerFactory.getLogger(SmartRoutingDataSource.class);
    
    private final RouteSelector routeSelector;
    
    public SmartRoutingDataSource(RouteSelector routeSelector) {
        this.routeSelector = routeSelector;
    }
    
    /**
     * Determines the current lookup key for DataSource selection.
     * 
     * This method is called by Spring for each database operation,
     * enabling dynamic routing based on current conditions.
     * 
     * @return the DataSource lookup key (e.g., "taiwan-db", "japan-db")
     */
    @Override
    protected Object determineCurrentLookupKey() {
        String selectedEndpoint = routeSelector.selectDatabaseEndpoint();
        logger.debug("Routing database connection to: {}", selectedEndpoint);
        return selectedEndpoint;
    }
}
