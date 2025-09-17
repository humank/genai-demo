package solid.humank.genaidemo.infrastructure.observability.websocket;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message format for WebSocket analytics updates.
 * Represents real-time analytics data sent to connected clients.
 */
public record AnalyticsUpdateMessage(
                @JsonProperty("type") String type,
                @JsonProperty("data") Map<String, Object> data,
                @JsonProperty("timestamp") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS") LocalDateTime timestamp) {

        @JsonCreator
        public AnalyticsUpdateMessage(
                        @JsonProperty("type") String type,
                        @JsonProperty("data") Map<String, Object> data) {
                this(type, data, LocalDateTime.now());
        }

        /**
         * Create a user behavior analytics update message
         */
        public static AnalyticsUpdateMessage userBehaviorUpdate(
                        String eventType,
                        String sessionId,
                        Map<String, Object> eventData) {
                return new AnalyticsUpdateMessage(
                                "user_behavior_update",
                                Map.of(
                                                "eventType", eventType,
                                                "sessionId", sessionId,
                                                "eventData", eventData));
        }

        /**
         * Create a performance metrics update message
         */
        public static AnalyticsUpdateMessage performanceMetricsUpdate(
                        String metricType,
                        double value,
                        String page) {
                return new AnalyticsUpdateMessage(
                                "performance_metrics_update",
                                Map.of(
                                                "metricType", metricType,
                                                "value", value,
                                                "page", page));
        }

        /**
         * Create a business metrics update message
         */
        public static AnalyticsUpdateMessage businessMetricsUpdate(
                        String metricName,
                        Object metricValue,
                        Map<String, Object> additionalData) {
                return new AnalyticsUpdateMessage(
                                "business_metrics_update",
                                Map.of(
                                                "metricName", metricName,
                                                "metricValue", metricValue,
                                                "additionalData", additionalData));
        }

        /**
         * Create a system status update message
         */
        public static AnalyticsUpdateMessage systemStatusUpdate(
                        String status,
                        Map<String, Object> statusData) {
                return new AnalyticsUpdateMessage(
                                "system_status_update",
                                Map.of(
                                                "status", status,
                                                "statusData", statusData));
        }

        /**
         * Create an error notification message
         */
        public static AnalyticsUpdateMessage errorNotification(
                        String errorType,
                        String errorMessage,
                        Map<String, Object> errorContext) {
                return new AnalyticsUpdateMessage(
                                "error_notification",
                                Map.of(
                                                "errorType", errorType,
                                                "errorMessage", errorMessage,
                                                "errorContext", errorContext));
        }

        public String getType() {
                return type;
        }

        public Map<String, Object> getData() {
                return data;
        }

        public LocalDateTime getTimestamp() {
                return timestamp;
        }
}