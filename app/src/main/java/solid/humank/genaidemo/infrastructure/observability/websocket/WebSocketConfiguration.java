package solid.humank.genaidemo.infrastructure.observability.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for real-time analytics updates.
 * Provides WebSocket endpoints for broadcasting analytics events and
 * performance metrics.
 */
@Configuration
@EnableWebSocket
@org.springframework.scheduling.annotation.EnableScheduling
public class WebSocketConfiguration implements WebSocketConfigurer {

    private final AnalyticsWebSocketHandler analyticsWebSocketHandler;

    public WebSocketConfiguration(@Lazy AnalyticsWebSocketHandler analyticsWebSocketHandler) {
        this.analyticsWebSocketHandler = analyticsWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Register analytics WebSocket endpoint with CORS support
        registry.addHandler(analyticsWebSocketHandler, "/ws/analytics")
                .setAllowedOrigins("*") // In production, specify actual origins
                .withSockJS(); // Enable SockJS fallback for browsers that don't support WebSocket
    }
}