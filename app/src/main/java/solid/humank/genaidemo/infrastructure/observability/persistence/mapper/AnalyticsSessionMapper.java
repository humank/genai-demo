package solid.humank.genaidemo.infrastructure.observability.persistence.mapper;

import java.util.Optional;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.observability.model.aggregate.AnalyticsSession;
import solid.humank.genaidemo.domain.observability.valueobject.SessionId;
import solid.humank.genaidemo.domain.observability.valueobject.UserId;
import solid.humank.genaidemo.infrastructure.observability.persistence.entity.JpaAnalyticsSessionEntity;

/**
 * 分析會話映射器
 * 
 * 負責在領域模型和持久化實體之間進行轉換。
 * 確保領域邏輯與基礎設施細節的分離。
 * 
 * 設計原則：
 * - 雙向轉換支援
 * - 完整的數據映射
 * - 錯誤處理和驗證
 * - 效能優化的轉換邏輯
 * 
 * 需求: 2.3, 3.3
 */
@Component
public class AnalyticsSessionMapper {

    /**
     * 將領域模型轉換為持久化實體
     * 
     * @param session 領域模型
     * @return 持久化實體
     */
    public JpaAnalyticsSessionEntity toEntity(AnalyticsSession session) {
        if (session == null) {
            return null;
        }

        JpaAnalyticsSessionEntity entity = new JpaAnalyticsSessionEntity();

        // 基本屬性映射
        entity.setSessionId(session.getSessionId().value());
        entity.setUserId(session.getUserId().map(UserId::value).orElse(null));
        entity.setTraceId(session.getTraceId());
        entity.setStartTime(session.getStartTime());
        entity.setLastActivityAt(session.getLastActivityAt());
        entity.setEndTime(session.getEndTime().orElse(null));
        entity.setDurationSeconds(session.getDurationSeconds().orElse(null));

        // 統計數據映射
        entity.setPageViewsCount(session.getPageViewsCount());
        entity.setUserActionsCount(session.getUserActionsCount());
        entity.setBusinessEventsCount(session.getBusinessEventsCount());
        entity.setPerformanceMetricsCount(session.getPerformanceMetricsCount());

        // 其他屬性
        entity.setIsAnonymous(session.isAnonymous());
        entity.setSessionMetadata(session.getSessionMetadata());
        entity.setRetentionDate(session.getRetentionDate());

        return entity;
    }

    /**
     * 將持久化實體轉換為領域模型
     * 
     * @param entity 持久化實體
     * @return 領域模型
     */
    public AnalyticsSession toDomain(JpaAnalyticsSessionEntity entity) {
        if (entity == null) {
            return null;
        }

        SessionId sessionId = SessionId.of(entity.getSessionId());
        Optional<UserId> userId = entity.getUserId() != null ? Optional.of(UserId.of(entity.getUserId()))
                : Optional.empty();

        // 使用建構子創建領域模型
        AnalyticsSession session = new AnalyticsSession(
                sessionId,
                userId,
                entity.getTraceId(),
                entity.getStartTime(),
                entity.getSessionMetadata());

        // 設定其他屬性
        session.setLastActivityAt(entity.getLastActivityAt());
        if (entity.getEndTime() != null) {
            session.setEndTime(entity.getEndTime());
        }

        // 設定統計數據
        session.setPageViewsCount(entity.getPageViewsCount());
        session.setUserActionsCount(entity.getUserActionsCount());
        session.setBusinessEventsCount(entity.getBusinessEventsCount());
        session.setPerformanceMetricsCount(entity.getPerformanceMetricsCount());

        // 設定保留日期
        session.setRetentionDate(entity.getRetentionDate());

        return session;
    }

    /**
     * 更新現有實體的屬性
     * 
     * @param entity  要更新的實體
     * @param session 領域模型
     */
    public void updateEntity(JpaAnalyticsSessionEntity entity, AnalyticsSession session) {
        if (entity == null || session == null) {
            return;
        }

        // 更新可變屬性
        entity.setLastActivityAt(session.getLastActivityAt());
        entity.setEndTime(session.getEndTime().orElse(null));
        entity.setDurationSeconds(session.getDurationSeconds().orElse(null));

        // 更新統計數據
        entity.setPageViewsCount(session.getPageViewsCount());
        entity.setUserActionsCount(session.getUserActionsCount());
        entity.setBusinessEventsCount(session.getBusinessEventsCount());
        entity.setPerformanceMetricsCount(session.getPerformanceMetricsCount());

        // 更新元數據
        entity.setSessionMetadata(session.getSessionMetadata());
        entity.setRetentionDate(session.getRetentionDate());
    }
}