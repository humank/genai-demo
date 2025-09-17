package solid.humank.genaidemo.infrastructure.observability.websocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Manages WebSocket connections and provides connection health monitoring.
 * Handles connection lifecycle, health checks, and connection statistics.
 */
@Component
public class WebSocketConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConnectionManager.class);

    private final Map<String, ConnectionInfo> connectionInfo = new ConcurrentHashMap<>();
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong totalDisconnections = new AtomicLong(0);

    /**
     * Register a new WebSocket connection
     */
    public void registerConnection(WebSocketSession session) {
        String sessionId = session.getId();
        String correlationId = "conn-mgr-" + sessionId;
        MDC.put("correlationId", correlationId);
        MDC.put("sessionId", sessionId);

        try {
            ConnectionInfo info = new ConnectionInfo(
                    sessionId,
                    System.currentTimeMillis(),
                    session.getRemoteAddress() != null ? session.getRemoteAddress().toString() : "unknown");

            connectionInfo.put(sessionId, info);
            totalConnections.incrementAndGet();

            logger.info("Registered WebSocket connection [sessionId: {}, correlationId: {}, remoteAddress: {}]",
                    sessionId, correlationId, info.remoteAddress());

        } finally {
            MDC.clear();
        }
    }

    /**
     * Unregister a WebSocket connection
     */
    public void unregisterConnection(String sessionId) {
        String correlationId = "conn-mgr-" + sessionId;
        MDC.put("correlationId", correlationId);
        MDC.put("sessionId", sessionId);

        try {
            ConnectionInfo info = connectionInfo.remove(sessionId);
            if (info != null) {
                totalDisconnections.incrementAndGet();
                long connectionDuration = System.currentTimeMillis() - info.connectedAt();

                logger.info("Unregistered WebSocket connection [sessionId: {}, correlationId: {}, duration: {}ms]",
                        sessionId, correlationId, connectionDuration);
            }

        } finally {
            MDC.clear();
        }
    }

    /**
     * Update last activity timestamp for a connection
     */
    public void updateLastActivity(String sessionId) {
        ConnectionInfo info = connectionInfo.get(sessionId);
        if (info != null) {
            ConnectionInfo updatedInfo = new ConnectionInfo(
                    info.sessionId(),
                    info.connectedAt(),
                    info.remoteAddress(),
                    System.currentTimeMillis());
            connectionInfo.put(sessionId, updatedInfo);
        }
    }

    /**
     * Get connection statistics
     */
    public ConnectionStats getConnectionStats() {
        return new ConnectionStats(
                connectionInfo.size(),
                totalConnections.get(),
                totalDisconnections.get(),
                System.currentTimeMillis());
    }

    /**
     * Get detailed connection information
     */
    public Map<String, ConnectionInfo> getConnectionDetails() {
        return Map.copyOf(connectionInfo);
    }

    /**
     * Scheduled task to clean up stale connections and log statistics
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void performHealthCheck() {
        String correlationId = "conn-health-" + System.currentTimeMillis();
        MDC.put("correlationId", correlationId);

        try {
            long currentTime = System.currentTimeMillis();
            long staleThreshold = 5 * 60 * 1000; // 5 minutes

            // Find stale connections
            connectionInfo.entrySet().removeIf(entry -> {
                ConnectionInfo info = entry.getValue();
                boolean isStale = (currentTime - info.lastActivity()) > staleThreshold;

                if (isStale) {
                    logger.warn(
                            "Removing stale WebSocket connection [sessionId: {}, correlationId: {}, lastActivity: {}]",
                            info.sessionId(), correlationId, info.lastActivity());
                }

                return isStale;
            });

            // Log connection statistics
            ConnectionStats stats = getConnectionStats();
            logger.debug(
                    "WebSocket connection health check [correlationId: {}, activeConnections: {}, totalConnections: {}, totalDisconnections: {}]",
                    correlationId, stats.activeConnections(), stats.totalConnections(), stats.totalDisconnections());

        } finally {
            MDC.clear();
        }
    }

    /**
     * Connection information record
     */
    public record ConnectionInfo(
            String sessionId,
            long connectedAt,
            String remoteAddress,
            long lastActivity) {
        public ConnectionInfo(String sessionId, long connectedAt, String remoteAddress) {
            this(sessionId, connectedAt, remoteAddress, connectedAt);
        }
    }

    /**
     * Connection statistics record
     */
    public record ConnectionStats(
            int activeConnections,
            long totalConnections,
            long totalDisconnections,
            long timestamp) {
    }
}