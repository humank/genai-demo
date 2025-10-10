package solid.humank.genaidemo.infrastructure.routing;

import java.time.Instant;

/**
 * Represents the health status of a service endpoint.
 * 
 * This record captures health check results including availability,
 * latency, and timestamp information for intelligent routing decisions.
 */
public record EndpointHealth(
    String endpoint,
    boolean isHealthy,
    long latencyMs,
    Instant lastChecked,
    String errorMessage
) {
    
    /**
     * Creates a healthy endpoint status.
     */
    public static EndpointHealth healthy(String endpoint, long latencyMs) {
        return new EndpointHealth(endpoint, true, latencyMs, Instant.now(), null);
    }
    
    /**
     * Creates an unhealthy endpoint status.
     */
    public static EndpointHealth unhealthy(String endpoint, String errorMessage) {
        return new EndpointHealth(endpoint, false, -1, Instant.now(), errorMessage);
    }
    
    /**
     * Checks if this endpoint is considered stale (not checked recently).
     * 
     * @param maxAgeSeconds maximum age in seconds before considering stale
     * @return true if the health check is stale
     */
    public boolean isStale(long maxAgeSeconds) {
        return Instant.now().getEpochSecond() - lastChecked.getEpochSecond() > maxAgeSeconds;
    }
}
