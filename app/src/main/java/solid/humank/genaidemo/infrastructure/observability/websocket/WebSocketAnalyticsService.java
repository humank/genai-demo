package solid.humank.genaidemo.infrastructure.observability.websocket;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import solid.humank.genaidemo.domain.observability.events.PerformanceMetricReceivedEvent;
import solid.humank.genaidemo.domain.observability.events.UserBehaviorAnalyticsEvent;

/**
 * Service for broadcasting analytics updates via WebSocket.
 * Listens to domain events and broadcasts real-time updates to connected
 * clients.
 */
@Service
public class WebSocketAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAnalyticsService.class);

    private final AnalyticsWebSocketHandler webSocketHandler;

    public WebSocketAnalyticsService(AnalyticsWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * Handle user behavior analytics events and broadcast to WebSocket clients
     */
    @EventListener
    public void handleUserBehaviorEvent(UserBehaviorAnalyticsEvent event) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = "ws-behavior-" + System.currentTimeMillis();
            MDC.put("correlationId", correlationId);
        }

        try {
            logger.debug("Broadcasting user behavior event [correlationId: {}, eventType: {}, sessionId: {}]",
                    correlationId, event.eventType(), event.sessionId());

            AnalyticsUpdateMessage message = AnalyticsUpdateMessage.userBehaviorUpdate(
                    event.eventType(),
                    event.sessionId(),
                    event.eventData());

            webSocketHandler.broadcastAnalyticsUpdate(message);

            logger.debug("Successfully broadcasted user behavior event [correlationId: {}, connectedClients: {}]",
                    correlationId, webSocketHandler.getConnectedSessionCount());

        } catch (Exception e) {
            logger.error("Failed to broadcast user behavior event [correlationId: {}, eventId: {}]",
                    correlationId, event.eventId(), e);
        } finally {
            if (correlationId.startsWith("ws-behavior-")) {
                MDC.remove("correlationId");
            }
        }
    }

    /**
     * Handle performance metric events and broadcast to WebSocket clients
     */
    @EventListener
    public void handlePerformanceMetricEvent(PerformanceMetricReceivedEvent event) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = "ws-perf-" + System.currentTimeMillis();
            MDC.put("correlationId", correlationId);
        }

        try {
            logger.debug(
                    "Broadcasting performance metric event [correlationId: {}, metricType: {}, value: {}, page: {}]",
                    correlationId, event.metricType(), event.value(), event.page());

            AnalyticsUpdateMessage message = AnalyticsUpdateMessage.performanceMetricsUpdate(
                    event.metricType(),
                    event.value(),
                    event.page());

            webSocketHandler.broadcastAnalyticsUpdate(message);

            logger.debug("Successfully broadcasted performance metric event [correlationId: {}, connectedClients: {}]",
                    correlationId, webSocketHandler.getConnectedSessionCount());

        } catch (Exception e) {
            logger.error("Failed to broadcast performance metric event [correlationId: {}, metricId: {}]",
                    correlationId, event.metricId(), e);
        } finally {
            if (correlationId.startsWith("ws-perf-")) {
                MDC.remove("correlationId");
            }
        }
    }

    /**
     * Broadcast business metrics update
     */
    public void broadcastBusinessMetricsUpdate(String metricName, Object metricValue,
            Map<String, Object> additionalData) {
        String correlationId = "ws-business-" + System.currentTimeMillis();
        MDC.put("correlationId", correlationId);

        try {
            logger.debug("Broadcasting business metrics update [correlationId: {}, metricName: {}, metricValue: {}]",
                    correlationId, metricName, metricValue);

            AnalyticsUpdateMessage message = AnalyticsUpdateMessage.businessMetricsUpdate(
                    metricName,
                    metricValue,
                    additionalData);

            webSocketHandler.broadcastAnalyticsUpdate(message);

            logger.debug("Successfully broadcasted business metrics update [correlationId: {}, connectedClients: {}]",
                    correlationId, webSocketHandler.getConnectedSessionCount());

        } catch (Exception e) {
            logger.error("Failed to broadcast business metrics update [correlationId: {}, metricName: {}]",
                    correlationId, metricName, e);
        } finally {
            MDC.remove("correlationId");
        }
    }

    /**
     * Broadcast system status update
     */
    public void broadcastSystemStatusUpdate(String status, Map<String, Object> statusData) {
        String correlationId = "ws-status-" + System.currentTimeMillis();
        MDC.put("correlationId", correlationId);

        try {
            logger.debug("Broadcasting system status update [correlationId: {}, status: {}]",
                    correlationId, status);

            AnalyticsUpdateMessage message = AnalyticsUpdateMessage.systemStatusUpdate(status, statusData);
            webSocketHandler.broadcastAnalyticsUpdate(message);

            logger.debug("Successfully broadcasted system status update [correlationId: {}, connectedClients: {}]",
                    correlationId, webSocketHandler.getConnectedSessionCount());

        } catch (Exception e) {
            logger.error("Failed to broadcast system status update [correlationId: {}, status: {}]",
                    correlationId, status, e);
        } finally {
            MDC.remove("correlationId");
        }
    }

    /**
     * Broadcast error notification
     */
    public void broadcastErrorNotification(String errorType, String errorMessage, Map<String, Object> errorContext) {
        String correlationId = "ws-error-" + System.currentTimeMillis();
        MDC.put("correlationId", correlationId);

        try {
            logger.debug("Broadcasting error notification [correlationId: {}, errorType: {}]",
                    correlationId, errorType);

            AnalyticsUpdateMessage message = AnalyticsUpdateMessage.errorNotification(
                    errorType, errorMessage, errorContext);

            webSocketHandler.broadcastAnalyticsUpdate(message);

            logger.debug("Successfully broadcasted error notification [correlationId: {}, connectedClients: {}]",
                    correlationId, webSocketHandler.getConnectedSessionCount());

        } catch (Exception e) {
            logger.error("Failed to broadcast error notification [correlationId: {}, errorType: {}]",
                    correlationId, errorType, e);
        } finally {
            MDC.remove("correlationId");
        }
    }

    /**
     * Get current connection statistics
     */
    public Map<String, Object> getConnectionStats() {
        return Map.of(
                "connectedSessions", webSocketHandler.getConnectedSessionCount(),
                "timestamp", System.currentTimeMillis());
    }
}