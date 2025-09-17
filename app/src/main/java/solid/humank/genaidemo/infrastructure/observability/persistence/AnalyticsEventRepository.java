package solid.humank.genaidemo.infrastructure.observability.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import solid.humank.genaidemo.application.observability.query.AnalyticsQueryService;

/**
 * 分析事件儲存庫介面
 * 
 * 提供統一的分析事件查詢介面，支援不同的實現方式。
 */
public interface AnalyticsEventRepository {

    List<AnalyticsEventEntity> findByUserIdAndTimestampBetween(String userId, LocalDateTime startTime, LocalDateTime endTime);
    
    List<AnalyticsEventEntity> findPerformanceEventsByPageAndTimeRange(String page, LocalDateTime startTime, LocalDateTime endTime);
    
    List<AnalyticsEventEntity> findBusinessEventsByTimeRange(LocalDateTime startTime, LocalDateTime endTime);
    
    List<AnalyticsEventEntity> findByFrontendEventTypeAndTimestampBetween(String eventType, LocalDateTime startTime, LocalDateTime endTime);
    
    Page<AnalyticsEventEntity> findAll(Pageable pageable);
    
    List<AnalyticsQueryService.PopularPage> findPopularPages(LocalDateTime startTime, LocalDateTime endTime, int limit);
    
    List<AnalyticsQueryService.ActivityTrend> findActivityTrends(LocalDateTime startTime, LocalDateTime endTime);
}