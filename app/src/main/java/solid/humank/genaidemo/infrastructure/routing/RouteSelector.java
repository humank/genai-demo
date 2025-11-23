package solid.humank.genaidemo.infrastructure.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * RouteSelector implements intelligent endpoint selection logic.
 * 
 * Selection strategy:
 * 1. Local-first: Prefer endpoints in the same region
 * 2. Health-based: Only select healthy endpoints
 * 3. Latency-aware: Choose endpoints with lower latency
 * 4. Automatic failover: Switch to backup region if local is unhealthy
 * 
 * This component is critical for Active-Active architecture, ensuring
 * optimal performance and automatic failover capabilities.
 */
@Component
public class RouteSelector {    private static final Logger logger = LoggerFactory.getLogger(RouteSelector.class);
    
    private final RegionDetector regionDetector;
    private final HealthChecker healthChecker;
    
    public RouteSelector(RegionDetector regionDetector, HealthChecker healthChecker) {
        this.regionDetector = regionDetector;
        this.healthChecker = healthChecker;
    }
    
    /**
     * Selects the optimal endpoint from available options.
     * 
     * @param endpoints list of available endpoint names
     * @return the selected endpoint name, or empty if none available
     */
    public Optional<String> selectEndpoint(List<String> endpoints) {
        if (endpoints == null || endpoints.isEmpty()) {
            logger.warn("No endpoints available for selection");
            return Optional.empty();
        }
        
        String currentRegion = regionDetector.detectRegion();
        logger.debug("Selecting endpoint for region: {}", currentRegion);
        
        // Strategy 1: Try local region first
        Optional<String> localEndpoint = selectLocalHealthyEndpoint(endpoints, currentRegion);
        if (localEndpoint.isPresent()) {
            logger.debug("Selected local endpoint: {}", localEndpoint.get());
            return localEndpoint;
        }
        
        // Strategy 2: Failover to any healthy endpoint
        Optional<String> fallbackEndpoint = selectAnyHealthyEndpoint(endpoints);
        if (fallbackEndpoint.isPresent()) {
            logger.warn("Local endpoint unavailable, failing over to: {}", fallbackEndpoint.get());
            return fallbackEndpoint;
        }
        
        String lastResort = endpoints.get(0);
        logger.error("No healthy endpoints available, using last resort: {}", lastResort);
        return Optional.of(lastResort);
    }
    
    /**
     * Selects the best database endpoint for the current region.
     * 
     * @return the selected database endpoint name
     */
    public String selectDatabaseEndpoint() {
        List<String> dbEndpoints = List.of("taiwan-db", "japan-db");
        return selectEndpoint(dbEndpoints).orElse("taiwan-db");
    }
    
    /**
     * Selects the best Redis endpoint for the current region.
     * 
     * @return the selected Redis endpoint name
     */
    public String selectRedisEndpoint() {
        List<String> redisEndpoints = List.of("taiwan-redis", "japan-redis");
        return selectEndpoint(redisEndpoints).orElse("taiwan-redis");
    }
    
    /**
     * Selects the best Kafka endpoint for the current region.
     * 
     * @return the selected Kafka endpoint name
     */
    public String selectKafkaEndpoint() {
        List<String> kafkaEndpoints = List.of("taiwan-kafka", "japan-kafka");
        return selectEndpoint(kafkaEndpoints).orElse("taiwan-kafka");
    }    private Optional<String> selectLocalHealthyEndpoint(List<String> endpoints, String currentRegion) {
        return endpoints.stream()
            .filter(endpoint -> endpoint.contains(getRegionShortName(currentRegion)))
            .filter(healthChecker::isHealthy)
            .findFirst();
    }
    
    private Optional<String> selectAnyHealthyEndpoint(List<String> endpoints) {
        return endpoints.stream()
            .filter(healthChecker::isHealthy)
            .min((e1, e2) -> Long.compare(
                healthChecker.getLatency(e1),
                healthChecker.getLatency(e2)
            ));
    }
    
    private String getRegionShortName(String region) {
        // Map AWS region codes to short names used in endpoint identifiers
        return switch (region) {
            case "ap-northeast-1" -> "taiwan";
            case "ap-northeast-2" -> "japan";
            default -> "taiwan";
        };
    }
}
