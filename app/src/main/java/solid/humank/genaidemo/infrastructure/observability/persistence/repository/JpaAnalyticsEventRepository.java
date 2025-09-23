package solid.humank.genaidemo.infrastructure.observability.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import solid.humank.genaidemo.infrastructure.observability.persistence.entity.JpaAnalyticsEventEntity;

/**
 * 分析事件 JPA 儲存庫介面
 * 
 * 提供分析事件實體的資料庫操作方法，支援詳細的事件查詢和統計分析。
 * 僅在生產環境中使用，用於持久化事件數據。
 * 
 * 設計原則：
 * - 高效的查詢方法設計
 * - 支援多維度事件分析
 * - 包含效能指標查詢
 * - 支援數據清理和保留政策
 * 
 * 需求: 2.3, 3.3
 */
@Repository
public interface JpaAnalyticsEventRepository extends JpaRepository<JpaAnalyticsEventEntity, String> {

        /**
         * 根據會話 ID 查找事件
         */
        List<JpaAnalyticsEventEntity> findBySessionIdOrderByOccurredAtDesc(String sessionId);

        /**
         * 根據用戶 ID 查找事件
         */
        List<JpaAnalyticsEventEntity> findByUserIdOrderByOccurredAtDesc(String userId);

        /**
         * 根據事件類型查找事件
         */
        List<JpaAnalyticsEventEntity> findByFrontendEventTypeOrderByOccurredAtDesc(String frontendEventType);

        /**
         * 根據會話 ID 和事件類型查找事件
         */
        @Query("SELECT e FROM JpaAnalyticsEventEntity e WHERE e.sessionId = :sessionId " +
                        "AND e.frontendEventType = :eventType ORDER BY e.occurredAt DESC")
        List<JpaAnalyticsEventEntity> findBySessionIdAndEventType(
                        @Param("sessionId") String sessionId,
                        @Param("eventType") String eventType);

        /**
         * 根據時間範圍查找事件
         */
        @Query("SELECT e FROM JpaAnalyticsEventEntity e WHERE e.occurredAt >= :startTime " +
                        "AND e.occurredAt <= :endTime ORDER BY e.occurredAt DESC")
        List<JpaAnalyticsEventEntity> findByTimeRange(
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        /**
         * 分頁查詢事件
         */
        @Query("SELECT e FROM JpaAnalyticsEventEntity e WHERE e.occurredAt >= :startTime " +
                        "ORDER BY e.occurredAt DESC")
        Page<JpaAnalyticsEventEntity> findEventsPageable(
                        @Param("startTime") LocalDateTime startTime,
                        Pageable pageable);

        /**
         * 查找頁面瀏覽事件
         */
        @Query("SELECT e FROM JpaAnalyticsEventEntity e WHERE e.frontendEventType = 'page_view' " +
                        "AND e.occurredAt >= :startTime ORDER BY e.occurredAt DESC")
        List<JpaAnalyticsEventEntity> findPageViewEvents(@Param("startTime") LocalDateTime startTime);

        /**
         * 查找用戶操作事件
         */
        @Query("SELECT e FROM JpaAnalyticsEventEntity e WHERE e.frontendEventType = 'user_action' " +
                        "AND e.occurredAt >= :startTime ORDER BY e.occurredAt DESC")
        List<JpaAnalyticsEventEntity> findUserActionEvents(@Param("startTime") LocalDateTime startTime);

        /**
         * 查找業務事件
         */
        @Query("SELECT e FROM JpaAnalyticsEventEntity e WHERE e.frontendEventType = 'business_event' " +
                        "AND e.occurredAt >= :startTime ORDER BY e.occurredAt DESC")
        List<JpaAnalyticsEventEntity> findBusinessEvents(@Param("startTime") LocalDateTime startTime);

        /**
         * 查找效能指標事件
         */
        @Query("SELECT e FROM JpaAnalyticsEventEntity e WHERE e.frontendEventType = 'performance_metric' " +
                        "AND e.occurredAt >= :startTime ORDER BY e.occurredAt DESC")
        List<JpaAnalyticsEventEntity> findPerformanceMetricEvents(@Param("startTime") LocalDateTime startTime);

        /**
         * 根據頁面路徑查找事件
         */
        @Query("SELECT e FROM JpaAnalyticsEventEntity e WHERE e.pagePath = :pagePath " +
                        "AND e.occurredAt >= :startTime ORDER BY e.occurredAt DESC")
        List<JpaAnalyticsEventEntity> findByPagePath(
                        @Param("pagePath") String pagePath,
                        @Param("startTime") LocalDateTime startTime);

        /**
         * 根據操作類型查找事件
         */
        @Query("SELECT e FROM JpaAnalyticsEventEntity e WHERE e.actionType = :actionType " +
                        "AND e.occurredAt >= :startTime ORDER BY e.occurredAt DESC")
        List<JpaAnalyticsEventEntity> findByActionType(
                        @Param("actionType") String actionType,
                        @Param("startTime") LocalDateTime startTime);

        /**
         * 根據指標類型查找效能事件
         */
        @Query("SELECT e FROM JpaAnalyticsEventEntity e WHERE e.metricType = :metricType " +
                        "AND e.occurredAt >= :startTime ORDER BY e.occurredAt DESC")
        List<JpaAnalyticsEventEntity> findByMetricType(
                        @Param("metricType") String metricType,
                        @Param("startTime") LocalDateTime startTime);

        // 統計查詢方法

        /**
         * 統計事件總數
         */
        @Query("SELECT COUNT(e) FROM JpaAnalyticsEventEntity e WHERE e.occurredAt >= :startTime")
        long countEventsSince(@Param("startTime") LocalDateTime startTime);

        /**
         * 統計特定類型事件數量
         */
        @Query("SELECT COUNT(e) FROM JpaAnalyticsEventEntity e WHERE e.frontendEventType = :eventType " +
                        "AND e.occurredAt >= :startTime")
        long countEventsByTypeSince(
                        @Param("eventType") String eventType,
                        @Param("startTime") LocalDateTime startTime);

        // 數據保留政策方法

        /**
         * 查找過期事件
         */
        @Query("SELECT e FROM JpaAnalyticsEventEntity e WHERE e.retentionDate <= :currentTime")
        List<JpaAnalyticsEventEntity> findExpiredEvents(@Param("currentTime") LocalDateTime currentTime);

        /**
         * 刪除過期事件
         */
        @Modifying
        @Query("DELETE FROM JpaAnalyticsEventEntity e WHERE e.retentionDate <= :currentTime")
        int deleteExpiredEvents(@Param("currentTime") LocalDateTime currentTime);

        /**
         * 計算過期事件數量
         */
        @Query("SELECT COUNT(e) FROM JpaAnalyticsEventEntity e WHERE e.retentionDate <= :currentTime")
        long countExpiredEvents(@Param("currentTime") LocalDateTime currentTime);

        /**
         * 批量刪除會話相關事件
         */
        @Modifying
        @Query("DELETE FROM JpaAnalyticsEventEntity e WHERE e.sessionId = :sessionId")
        int deleteBySessionId(@Param("sessionId") String sessionId);

        /**
         * 批量刪除用戶相關事件
         */
        @Modifying
        @Query("DELETE FROM JpaAnalyticsEventEntity e WHERE e.userId = :userId")
        int deleteByUserId(@Param("userId") String userId);

        /**
         * 統計頁面瀏覽量
         */
        @Query("SELECT e.pagePath, COUNT(e) as viewCount FROM JpaAnalyticsEventEntity e " +
                        "WHERE e.frontendEventType = 'page_view' AND e.occurredAt >= :startTime " +
                        "GROUP BY e.pagePath ORDER BY viewCount DESC")
        List<Object[]> getPageViewStats(@Param("startTime") LocalDateTime startTime);

        /**
         * 統計用戶操作
         */
        @Query("SELECT e.actionType, COUNT(e) as actionCount FROM JpaAnalyticsEventEntity e " +
                        "WHERE e.frontendEventType = 'user_action' AND e.occurredAt >= :startTime " +
                        "GROUP BY e.actionType ORDER BY actionCount DESC")
        List<Object[]> getUserActionStats(@Param("startTime") LocalDateTime startTime);

        /**
         * 統計效能指標
         */
        @Query("SELECT e.metricType, COUNT(e) as metricCount, AVG(e.metricValue) as avgValue, " +
                        "MIN(e.metricValue) as minValue, MAX(e.metricValue) as maxValue " +
                        "FROM JpaAnalyticsEventEntity e " +
                        "WHERE e.frontendEventType = 'performance_metric' AND e.occurredAt >= :startTime " +
                        "GROUP BY e.metricType ORDER BY metricCount DESC")
        List<Object[]> getPerformanceMetricStats(@Param("startTime") LocalDateTime startTime);

        /**
         * 統計每日事件數量 (簡化版本，適用於 H2)
         */
        @Query("SELECT CAST(e.occurredAt AS date) as date, e.frontendEventType, COUNT(e) as eventCount " +
                        "FROM JpaAnalyticsEventEntity e " +
                        "WHERE e.occurredAt >= :startTime AND e.occurredAt <= :endTime " +
                        "GROUP BY CAST(e.occurredAt AS date), e.frontendEventType " +
                        "ORDER BY CAST(e.occurredAt AS date), e.frontendEventType")
        List<Object[]> getDailyEventStats(
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        /**
         * 查找熱門頁面
         */
        @Query("SELECT e.pagePath, COUNT(e) as viewCount, COUNT(DISTINCT e.sessionId) as uniqueVisitors " +
                        "FROM JpaAnalyticsEventEntity e " +
                        "WHERE e.frontendEventType = 'page_view' AND e.occurredAt >= :startTime " +
                        "GROUP BY e.pagePath ORDER BY viewCount DESC")
        List<Object[]> getPopularPages(@Param("startTime") LocalDateTime startTime, Pageable pageable);

        /**
         * 查找效能問題頁面
         */
        @Query("SELECT e.pagePath, e.metricType, AVG(e.metricValue) as avgValue, COUNT(e) as sampleCount " +
                        "FROM JpaAnalyticsEventEntity e " +
                        "WHERE e.frontendEventType = 'performance_metric' AND e.occurredAt >= :startTime " +
                        "GROUP BY e.pagePath, e.metricType " +
                        "HAVING AVG(e.metricValue) > :threshold ORDER BY avgValue DESC")
        List<Object[]> getPerformanceIssuePages(
                        @Param("startTime") LocalDateTime startTime,
                        @Param("threshold") double threshold);
}