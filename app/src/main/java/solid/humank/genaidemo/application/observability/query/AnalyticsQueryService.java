package solid.humank.genaidemo.application.observability.query;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import solid.humank.genaidemo.infrastructure.observability.persistence.AnalyticsEventEntity;
import solid.humank.genaidemo.infrastructure.observability.persistence.AnalyticsEventRepository;
import solid.humank.genaidemo.infrastructure.observability.persistence.AnalyticsSessionEntity;
import solid.humank.genaidemo.infrastructure.observability.persistence.AnalyticsSessionRepository;

/**
 * 分析數據查詢服務
 * 
 * 提供可觀測性數據的查詢和統計功能，支援業務分析和監控需求。
 * 
 * 需求: 2.3, 3.3
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsQueryService {

    private final AnalyticsEventRepository eventRepository;
    private final AnalyticsSessionRepository sessionRepository;

    public AnalyticsQueryService(AnalyticsEventRepository eventRepository,
            AnalyticsSessionRepository sessionRepository) {
        this.eventRepository = eventRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * 查詢用戶行為事件統計
     */
    public UserBehaviorStats getUserBehaviorStats(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        List<AnalyticsEventEntity> events = eventRepository.findByUserIdAndTimestampBetween(
                userId, startTime, endTime);

        long pageViews = events.stream()
                .filter(e -> "page_view".equals(e.getFrontendEventType()))
                .count();

        long userActions = events.stream()
                .filter(e -> "user_action".equals(e.getFrontendEventType()))
                .count();

        long businessEvents = events.stream()
                .filter(e -> "business_event".equals(e.getFrontendEventType()))
                .count();

        return new UserBehaviorStats(userId, pageViews, userActions, businessEvents, startTime, endTime);
    }

    /**
     * 查詢頁面效能統計
     */
    public PagePerformanceStats getPagePerformanceStats(String page, LocalDateTime startTime, LocalDateTime endTime) {
        List<AnalyticsEventEntity> performanceEvents = eventRepository.findPerformanceEventsByPageAndTimeRange(
                page, startTime, endTime);

        double avgLcp = performanceEvents.stream()
                .filter(e -> "lcp".equals(e.getMetricType()))
                .mapToDouble(e -> Double.parseDouble(e.getMetricValue()))
                .average()
                .orElse(0.0);

        double avgFid = performanceEvents.stream()
                .filter(e -> "fid".equals(e.getMetricType()))
                .mapToDouble(e -> Double.parseDouble(e.getMetricValue()))
                .average()
                .orElse(0.0);

        double avgCls = performanceEvents.stream()
                .filter(e -> "cls".equals(e.getMetricType()))
                .mapToDouble(e -> Double.parseDouble(e.getMetricValue()))
                .average()
                .orElse(0.0);

        return new PagePerformanceStats(page, avgLcp, avgFid, avgCls, startTime, endTime);
    }

    /**
     * 查詢業務指標統計
     */
    public BusinessMetricsStats getBusinessMetricsStats(LocalDateTime startTime, LocalDateTime endTime) {
        List<AnalyticsEventEntity> businessEvents = eventRepository.findBusinessEventsByTimeRange(startTime, endTime);

        long productViews = businessEvents.stream()
                .filter(e -> "product_view".equals(e.getFrontendEventType()))
                .count();

        long addToCarts = businessEvents.stream()
                .filter(e -> "add_to_cart".equals(e.getFrontendEventType()))
                .count();

        long searches = businessEvents.stream()
                .filter(e -> "search".equals(e.getFrontendEventType()))
                .count();

        double conversionRate = productViews > 0 ? (double) addToCarts / productViews * 100 : 0.0;

        return new BusinessMetricsStats(productViews, addToCarts, searches, conversionRate, startTime, endTime);
    }

    /**
     * 查詢會話統計
     */
    public SessionStats getSessionStats(LocalDateTime startTime, LocalDateTime endTime) {
        List<AnalyticsSessionEntity> sessions = sessionRepository.findByCreatedAtBetween(startTime, endTime);

        long totalSessions = sessions.size();
        long activeSessions = sessions.stream()
                .filter(s -> s.getEndedAt() == null)
                .count();

        double avgDuration = sessions.stream()
                .filter(s -> s.getEndedAt() != null)
                .mapToLong(s -> java.time.Duration.between(s.getCreatedAt(), s.getEndedAt()).toMinutes())
                .average()
                .orElse(0.0);

        return new SessionStats(totalSessions, activeSessions, avgDuration, startTime, endTime);
    }

    /**
     * 分頁查詢分析事件
     */
    public Page<AnalyticsEventEntity> getAnalyticsEvents(Pageable pageable) {
        return eventRepository.findAll(pageable);
    }

    /**
     * 根據事件類型查詢事件
     */
    public List<AnalyticsEventEntity> getEventsByType(String eventType, LocalDateTime startTime, LocalDateTime endTime) {
        return eventRepository.findByFrontendEventTypeAndTimestampBetween(eventType, startTime, endTime);
    }

    /**
     * 查詢熱門頁面
     */
    public List<PopularPage> getPopularPages(LocalDateTime startTime, LocalDateTime endTime, int limit) {
        return eventRepository.findPopularPages(startTime, endTime, limit);
    }

    /**
     * 查詢用戶活動趨勢
     */
    public List<ActivityTrend> getActivityTrends(LocalDateTime startTime, LocalDateTime endTime) {
        return eventRepository.findActivityTrends(startTime, endTime);
    }

    // 內部統計類別
    public record UserBehaviorStats(
            String userId,
            long pageViews,
            long userActions,
            long businessEvents,
            LocalDateTime startTime,
            LocalDateTime endTime) {
    }

    public record PagePerformanceStats(
            String page,
            double avgLcp,
            double avgFid,
            double avgCls,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        public String getPerformanceGrade() {
            if (avgLcp <= 2500 && avgFid <= 100 && avgCls <= 0.1) {
                return "good";
            } else if (avgLcp <= 4000 && avgFid <= 300 && avgCls <= 0.25) {
                return "needs-improvement";
            } else {
                return "poor";
            }
        }
    }

    public record BusinessMetricsStats(
            long productViews,
            long addToCarts,
            long searches,
            double conversionRate,
            LocalDateTime startTime,
            LocalDateTime endTime) {
    }

    public record SessionStats(
            long totalSessions,
            long activeSessions,
            double avgDurationMinutes,
            LocalDateTime startTime,
            LocalDateTime endTime) {
    }

    public record PopularPage(
            String page,
            long viewCount) {
    }

    public record ActivityTrend(
            LocalDateTime hour,
            long eventCount) {
    }
}