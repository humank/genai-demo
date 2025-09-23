package solid.humank.genaidemo.domain.observability.model.aggregate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import solid.humank.genaidemo.domain.common.aggregate.AggregateRootInterface;
import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.observability.events.PerformanceMetricReceivedEvent;
import solid.humank.genaidemo.domain.observability.events.UserBehaviorAnalyticsEvent;
import solid.humank.genaidemo.domain.observability.valueobject.SessionId;
import solid.humank.genaidemo.domain.observability.valueobject.UserId;

/**
 * 可觀測性會話聚合根
 * 
 * 管理用戶會話期間的可觀測性數據收集，包括用戶行為事件和效能指標。
 * 整合現有 DDD 架構，支援事件收集和發布。
 * 
 * 職責：
 * - 記錄用戶行為事件
 * - 收集效能指標
 * - 維護會話狀態
 * - 發布領域事件
 * 
 * 需求: 1.1, 1.2, 1.3, 2.2
 */
@AggregateRoot(name = "ObservabilitySession", description = "可觀測性會話聚合根", boundedContext = "Observability", version = "1.0")
public class ObservabilitySession implements AggregateRootInterface {

    private final SessionId sessionId;
    private final Optional<UserId> userId;
    private final LocalDateTime startTime;
    private LocalDateTime lastActivity;
    private final List<UserBehaviorEvent> behaviorEvents;
    private final List<PerformanceMetric> performanceMetrics;
    private boolean active;

    /**
     * 創建新的可觀測性會話
     * 
     * @param sessionId 會話 ID
     * @param userId    用戶 ID (可選)
     */
    public ObservabilitySession(SessionId sessionId, Optional<UserId> userId) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.startTime = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.behaviorEvents = new ArrayList<>();
        this.performanceMetrics = new ArrayList<>();
        this.active = true;
    }

    /**
     * 記錄用戶行為事件
     * 
     * @param event 用戶行為事件
     */
    public void recordUserBehavior(UserBehaviorEvent event) {
        validateEvent(event);
        validateSessionActive();

        this.behaviorEvents.add(event);
        this.lastActivity = LocalDateTime.now();

        // 發布用戶行為分析事件
        collectEvent(UserBehaviorAnalyticsEvent.create(
                event.getEventId(),
                event.getEventType(),
                sessionId.value(),
                userId.map(UserId::value),
                event.getTraceId(),
                event.getData()));
    }

    /**
     * 記錄效能指標
     * 
     * @param metric 效能指標
     */
    public void recordPerformanceMetric(PerformanceMetric metric) {
        validatePerformanceMetric(metric);
        validateSessionActive();

        this.performanceMetrics.add(metric);
        this.lastActivity = LocalDateTime.now();

        // 發布效能指標事件
        collectEvent(PerformanceMetricReceivedEvent.create(
                metric.getMetricId(),
                metric.getMetricType(),
                metric.getValue(),
                metric.getPage(),
                sessionId.value(),
                metric.getTraceId()));
    }

    /**
     * 結束會話
     */
    public void endSession() {
        this.active = false;
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * 檢查會話是否過期
     * 
     * @param timeoutMinutes 超時分鐘數
     * @return 如果會話過期返回 true
     */
    public boolean isExpired(int timeoutMinutes) {
        return lastActivity.isBefore(LocalDateTime.now().minusMinutes(timeoutMinutes));
    }

    /**
     * 獲取會話統計資訊
     * 
     * @return 會話統計
     */
    public SessionStatistics getStatistics() {
        return new SessionStatistics(
                behaviorEvents.size(),
                performanceMetrics.size(),
                startTime,
                lastActivity,
                active);
    }

    // === Getters ===

    public SessionId getSessionId() {
        return sessionId;
    }

    public Optional<UserId> getUserId() {
        return userId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public boolean isActive() {
        return active;
    }

    public List<UserBehaviorEvent> getBehaviorEvents() {
        return List.copyOf(behaviorEvents);
    }

    public List<PerformanceMetric> getPerformanceMetrics() {
        return List.copyOf(performanceMetrics);
    }

    // === Private Methods ===

    private void validateEvent(UserBehaviorEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("User behavior event cannot be null");
        }
        if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
            throw new IllegalArgumentException("Event ID cannot be null or empty");
        }
        if (event.getEventType() == null || event.getEventType().trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
    }

    private void validatePerformanceMetric(PerformanceMetric metric) {
        if (metric == null) {
            throw new IllegalArgumentException("Performance metric cannot be null");
        }
        if (metric.getMetricId() == null || metric.getMetricId().trim().isEmpty()) {
            throw new IllegalArgumentException("Metric ID cannot be null or empty");
        }
        if (metric.getValue() < 0) {
            throw new IllegalArgumentException("Metric value cannot be negative");
        }
    }

    private void validateSessionActive() {
        if (!active) {
            throw new IllegalStateException("Cannot record events on inactive session");
        }
    }

    // === Inner Classes ===

    /**
     * 用戶行為事件值對象
     */
    public static class UserBehaviorEvent {
        private final String eventId;
        private final String eventType;
        private final String traceId;
        private final Map<String, Object> data;
        private final LocalDateTime timestamp;

        public UserBehaviorEvent(String eventId, String eventType, String traceId,
                Map<String, Object> data) {
            this.eventId = eventId;
            this.eventType = eventType;
            this.traceId = traceId;
            this.data = data;
            this.timestamp = LocalDateTime.now();
        }

        // Getters
        public String getEventId() {
            return eventId;
        }

        public String getEventType() {
            return eventType;
        }

        public String getTraceId() {
            return traceId;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    /**
     * 效能指標值對象
     */
    public static class PerformanceMetric {
        private final String metricId;
        private final String metricType;
        private final double value;
        private final String page;
        private final String traceId;
        private final LocalDateTime timestamp;

        public PerformanceMetric(String metricId, String metricType, double value,
                String page, String traceId) {
            this.metricId = metricId;
            this.metricType = metricType;
            this.value = value;
            this.page = page;
            this.traceId = traceId;
            this.timestamp = LocalDateTime.now();
        }

        // Getters
        public String getMetricId() {
            return metricId;
        }

        public String getMetricType() {
            return metricType;
        }

        public double getValue() {
            return value;
        }

        public String getPage() {
            return page;
        }

        public String getTraceId() {
            return traceId;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    /**
     * 會話統計資訊
     */
    public record SessionStatistics(
            int behaviorEventCount,
            int performanceMetricCount,
            LocalDateTime startTime,
            LocalDateTime lastActivity,
            boolean active) {
    }
}