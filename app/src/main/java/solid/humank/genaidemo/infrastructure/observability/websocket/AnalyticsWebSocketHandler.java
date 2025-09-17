package solid.humank.genaidemo.infrastructure.observability.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * WebSocket handler for real-time analytics updates.
 * Manages WebSocket connections and broadcasts analytics events to connected
 * clients.
 */
@Component
public class AnalyticsWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final WebSocketConnectionManager connectionManager;
    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    @Autowired
    public AnalyticsWebSocketHandler(ObjectMapper objectMapper, WebSocketConnectionManager connectionManager) {
        this.objectMapper = objectMapper;
        this.connectionManager = connectionManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        String correlationId = generateCorrelationId(sessionId);

        // Set MDC context for logging
        MDC.put("correlationId", correlationId);
        MDC.put("sessionId", sessionId);

        try {
            sessions.add(session);
            sessionMap.put(sessionId, session);
            connectionManager.registerConnection(session);

            logger.info("WebSocket connection established [sessionId: {}, correlationId: {}]",
                    sessionId, correlationId);

            // Send welcome message
            sendWelcomeMessage(session);

        } finally {
            MDC.clear();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        String correlationId = generateCorrelationId(sessionId);

        // Set MDC context for logging
        MDC.put("correlationId", correlationId);
        MDC.put("sessionId", sessionId);

        try {
            sessions.remove(session);
            sessionMap.remove(sessionId);
            connectionManager.unregisterConnection(sessionId);

            logger.info("WebSocket connection closed [sessionId: {}, correlationId: {}, status: {}]",
                    sessionId, correlationId, status);

        } finally {
            MDC.clear();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String correlationId = generateCorrelationId(sessionId);

        // Set MDC context for logging
        MDC.put("correlationId", correlationId);
        MDC.put("sessionId", sessionId);

        try {
            String payload = message.getPayload();
            connectionManager.updateLastActivity(sessionId);

            logger.debug("Received WebSocket message [sessionId: {}, correlationId: {}, payload: {}]",
                    sessionId, correlationId, payload);

            // Handle subscription/unsubscription requests
            handleClientMessage(session, payload);

        } finally {
            MDC.clear();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        String correlationId = generateCorrelationId(sessionId);

        // Set MDC context for logging
        MDC.put("correlationId", correlationId);
        MDC.put("sessionId", sessionId);

        try {
            logger.error("WebSocket transport error [sessionId: {}, correlationId: {}]",
                    sessionId, correlationId, exception);

            // Remove session on error
            sessions.remove(session);
            sessionMap.remove(sessionId);

        } finally {
            MDC.clear();
        }
    }

    /**
     * Broadcast analytics update to all connected clients
     */
    public void broadcastAnalyticsUpdate(AnalyticsUpdateMessage message) {
        String correlationId = generateCorrelationId("broadcast");
        MDC.put("correlationId", correlationId);

        try {
            String jsonMessage = objectMapper.writeValueAsString(message);

            logger.debug("Broadcasting analytics update [correlationId: {}, connectedSessions: {}, messageType: {}]",
                    correlationId, sessions.size(), message.getType());

            // Send to all connected sessions
            sessions.parallelStream().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(jsonMessage));
                    } else {
                        // Remove closed sessions
                        sessions.remove(session);
                        sessionMap.remove(session.getId());
                    }
                } catch (IOException e) {
                    logger.warn("Failed to send message to session [sessionId: {}, correlationId: {}]",
                            session.getId(), correlationId, e);
                    // Remove problematic session
                    sessions.remove(session);
                    sessionMap.remove(session.getId());
                }
            });

        } catch (Exception e) {
            logger.error("Failed to broadcast analytics update [correlationId: {}]", correlationId, e);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Send message to specific session
     */
    public void sendToSession(String sessionId, AnalyticsUpdateMessage message) {
        String correlationId = generateCorrelationId(sessionId);
        MDC.put("correlationId", correlationId);
        MDC.put("sessionId", sessionId);

        try {
            WebSocketSession session = sessionMap.get(sessionId);
            if (session != null && session.isOpen()) {
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));

                logger.debug("Sent message to specific session [sessionId: {}, correlationId: {}, messageType: {}]",
                        sessionId, correlationId, message.getType());
            } else {
                logger.warn("Session not found or closed [sessionId: {}, correlationId: {}]",
                        sessionId, correlationId);
            }
        } catch (Exception e) {
            logger.error("Failed to send message to session [sessionId: {}, correlationId: {}]",
                    sessionId, correlationId, e);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Get number of connected sessions
     */
    public int getConnectedSessionCount() {
        return sessions.size();
    }

    private void sendWelcomeMessage(WebSocketSession session) throws IOException {
        AnalyticsUpdateMessage welcomeMessage = new AnalyticsUpdateMessage(
                "connection_established",
                Map.of(
                        "sessionId", session.getId(),
                        "timestamp", System.currentTimeMillis(),
                        "message", "Connected to analytics WebSocket"));

        String jsonMessage = objectMapper.writeValueAsString(welcomeMessage);
        session.sendMessage(new TextMessage(jsonMessage));
    }

    private void handleClientMessage(WebSocketSession session, String payload) {
        try {
            // Parse client message for subscription management
            @SuppressWarnings("unchecked")
            Map<String, Object> clientMessage = objectMapper.readValue(payload, Map.class);

            String action = (String) clientMessage.get("action");
            if ("subscribe".equals(action)) {
                handleSubscription(session, clientMessage);
            } else if ("unsubscribe".equals(action)) {
                handleUnsubscription(session, clientMessage);
            } else if ("ping".equals(action)) {
                handlePing(session);
            }

        } catch (Exception e) {
            logger.warn("Failed to handle client message [sessionId: {}]", session.getId(), e);
        }
    }

    private void handleSubscription(WebSocketSession session, Map<String, Object> message) throws IOException {
        String channel = (String) message.get("channel");
        logger.debug("Client subscribed to channel [sessionId: {}, channel: {}]", session.getId(), channel);

        // Store subscription info in session attributes
        session.getAttributes().put("subscribedChannel", channel);

        // Send subscription confirmation
        AnalyticsUpdateMessage confirmMessage = new AnalyticsUpdateMessage(
                "subscription_confirmed",
                Map.of("channel", channel, "timestamp", System.currentTimeMillis()));

        String jsonMessage = objectMapper.writeValueAsString(confirmMessage);
        session.sendMessage(new TextMessage(jsonMessage));
    }

    private void handleUnsubscription(WebSocketSession session, Map<String, Object> message) throws IOException {
        String channel = (String) message.get("channel");
        logger.debug("Client unsubscribed from channel [sessionId: {}, channel: {}]", session.getId(), channel);

        // Remove subscription info from session attributes
        session.getAttributes().remove("subscribedChannel");

        // Send unsubscription confirmation
        AnalyticsUpdateMessage confirmMessage = new AnalyticsUpdateMessage(
                "unsubscription_confirmed",
                Map.of("channel", channel, "timestamp", System.currentTimeMillis()));

        String jsonMessage = objectMapper.writeValueAsString(confirmMessage);
        session.sendMessage(new TextMessage(jsonMessage));
    }

    private void handlePing(WebSocketSession session) throws IOException {
        // Respond to ping with pong
        AnalyticsUpdateMessage pongMessage = new AnalyticsUpdateMessage(
                "pong",
                Map.of("timestamp", System.currentTimeMillis()));

        String jsonMessage = objectMapper.writeValueAsString(pongMessage);
        session.sendMessage(new TextMessage(jsonMessage));
    }

    private String generateCorrelationId(String sessionId) {
        return "ws-" + sessionId + "-" + System.currentTimeMillis();
    }
}