package solid.humank.genaidemo.infrastructure.observability.persistence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import solid.humank.genaidemo.application.observability.query.AnalyticsQueryService;

/**
 * 記憶體分析事件儲存庫實現
 * 
 * 用於開發和測試環境的記憶體實現，提供基本的查詢功能。
 */
@Repository
@ConditionalOnProperty(name = "genai-demo.events.publisher", havingValue = "in-memory")
public class InMemoryAnalyticsEventRepository implements AnalyticsEventRepository {

    private final Map<String, InMemoryAnalyticsEvent> events = new ConcurrentHashMap<>();

    @Override
    public List<AnalyticsEventEntity> findByUserIdAndTimestampBetween(String userId, LocalDateTime startTime,
            LocalDateTime endTime) {
        return events.values().stream()
                .filter(e -> userId.equals(e.getUserId()))
                .filter(e -> e.getTimestamp().isAfter(startTime) && e.getTimestamp().isBefore(endTime))
                .collect(Collectors.toList());
    }

    @Override
    public List<AnalyticsEventEntity> findPerformanceEventsByPageAndTimeRange(String page, LocalDateTime startTime,
            LocalDateTime endTime) {
        return events.values().stream()
                .filter(e -> "performance_metric".equals(e.getFrontendEventType()))
                .filter(e -> page.equals(e.getPagePath()))
                .filter(e -> e.getTimestamp().isAfter(startTime) && e.getTimestamp().isBefore(endTime))
                .collect(Collectors.toList());
    }

    @Override
    public List<AnalyticsEventEntity> findBusinessEventsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return events.values().stream()
                .filter(e -> "business_event".equals(e.getFrontendEventType()))
                .filter(e -> e.getTimestamp().isAfter(startTime) && e.getTimestamp().isBefore(endTime))
                .collect(Collectors.toList());
    }

    @Override
    public List<AnalyticsEventEntity> findByFrontendEventTypeAndTimestampBetween(String eventType,
            LocalDateTime startTime, LocalDateTime endTime) {
        return events.values().stream()
                .filter(e -> eventType.equals(e.getFrontendEventType()))
                .filter(e -> e.getTimestamp().isAfter(startTime) && e.getTimestamp().isBefore(endTime))
                .collect(Collectors.toList());
    }

    @Override
    public Page<AnalyticsEventEntity> findAll(Pageable pageable) {
        List<AnalyticsEventEntity> allEvents = new ArrayList<>(events.values());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allEvents.size());

        if (start > allEvents.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, allEvents.size());
        }

        List<AnalyticsEventEntity> pageContent = allEvents.subList(start, end);
        return new PageImpl<>(java.util.Objects.requireNonNull((List<AnalyticsEventEntity>) pageContent), pageable,
                allEvents.size());
    }

    @Override
    public List<AnalyticsQueryService.PopularPage> findPopularPages(LocalDateTime startTime, LocalDateTime endTime,
            int limit) {
        Map<String, Long> pageViews = events.values().stream()
                .filter(e -> "page_view".equals(e.getFrontendEventType()))
                .filter(e -> e.getTimestamp().isAfter(startTime) && e.getTimestamp().isBefore(endTime))
                .collect(Collectors.groupingBy(
                        AnalyticsEventEntity::getPagePath,
                        Collectors.counting()));

        return pageViews.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> new AnalyticsQueryService.PopularPage(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AnalyticsQueryService.ActivityTrend> findActivityTrends(LocalDateTime startTime,
            LocalDateTime endTime) {
        Map<LocalDateTime, Long> hourlyActivity = events.values().stream()
                .filter(e -> e.getTimestamp().isAfter(startTime) && e.getTimestamp().isBefore(endTime))
                .collect(Collectors.groupingBy(
                        e -> e.getTimestamp().withMinute(0).withSecond(0).withNano(0),
                        Collectors.counting()));

        return hourlyActivity.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new AnalyticsQueryService.ActivityTrend(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    // 用於添加事件的方法
    public void addEvent(String eventId, String userId, String sessionId, String eventType,
            String metricType, String metricValue, String pagePath, String actionType) {
        InMemoryAnalyticsEvent event = new InMemoryAnalyticsEvent(
                eventId, userId, sessionId, eventType, metricType, metricValue,
                pagePath, actionType, LocalDateTime.now());
        events.put(eventId, event);
    }

    // 內部事件實現
    private static class InMemoryAnalyticsEvent implements AnalyticsEventEntity {
        private final String id;
        private final String userId;
        private final String sessionId;
        private final String frontendEventType;
        private final String metricType;
        private final String metricValue;
        private final String pagePath;
        private final String actionType;
        private final LocalDateTime timestamp;

        public InMemoryAnalyticsEvent(String id, String userId, String sessionId, String frontendEventType,
                String metricType, String metricValue, String pagePath, String actionType,
                LocalDateTime timestamp) {
            this.id = id;
            this.userId = userId;
            this.sessionId = sessionId;
            this.frontendEventType = frontendEventType;
            this.metricType = metricType;
            this.metricValue = metricValue;
            this.pagePath = pagePath;
            this.actionType = actionType;
            this.timestamp = timestamp;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getUserId() {
            return userId;
        }

        @Override
        public String getSessionId() {
            return sessionId;
        }

        @Override
        public String getFrontendEventType() {
            return frontendEventType;
        }

        @Override
        public String getMetricType() {
            return metricType;
        }

        @Override
        public String getMetricValue() {
            return metricValue;
        }

        @Override
        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        @Override
        public String getPagePath() {
            return pagePath;
        }

        @Override
        public String getActionType() {
            return actionType;
        }
    }
}