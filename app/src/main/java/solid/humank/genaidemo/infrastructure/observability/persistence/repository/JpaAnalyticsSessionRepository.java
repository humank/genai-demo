package solid.humank.genaidemo.infrastructure.observability.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import solid.humank.genaidemo.infrastructure.observability.persistence.entity.JpaAnalyticsSessionEntity;

/**
 * 分析會話 JPA 儲存庫介面
 * 
 * 提供分析會話實體的資料庫操作方法，支援統計查詢和數據保留政策。
 * 僅在生產環境中使用，用於持久化分析數據。
 * 
 * 設計原則：
 * - 高效的查詢方法設計
 * - 支援分頁和排序
 * - 包含統計和聚合查詢
 * - 支援數據清理和保留政策
 * 
 * 需求: 2.3, 3.3
 */
@Repository
public interface JpaAnalyticsSessionRepository extends JpaRepository<JpaAnalyticsSessionEntity, String> {

    /**
     * 根據會話 ID 查找分析會話
     */
    Optional<JpaAnalyticsSessionEntity> findBySessionId(String sessionId);

    /**
     * 根據用戶 ID 查找分析會話
     */
    List<JpaAnalyticsSessionEntity> findByUserId(String userId);

    /**
     * 根據用戶 ID 和時間範圍查找分析會話
     */
    @Query("SELECT s FROM JpaAnalyticsSessionEntity s WHERE s.userId = :userId " +
            "AND s.startTime >= :startTime AND s.startTime <= :endTime " +
            "ORDER BY s.startTime DESC")
    List<JpaAnalyticsSessionEntity> findByUserIdAndTimeRange(
            @Param("userId") String userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查找匿名用戶會話
     */
    @Query("SELECT s FROM JpaAnalyticsSessionEntity s WHERE s.isAnonymous = true " +
            "AND s.startTime >= :startTime ORDER BY s.startTime DESC")
    List<JpaAnalyticsSessionEntity> findAnonymousSessionsSince(@Param("startTime") LocalDateTime startTime);

    /**
     * 查找活躍會話（在指定時間內有活動）
     */
    @Query("SELECT s FROM JpaAnalyticsSessionEntity s WHERE s.lastActivityAt >= :since " +
            "AND s.endTime IS NULL ORDER BY s.lastActivityAt DESC")
    List<JpaAnalyticsSessionEntity> findActiveSessionsSince(@Param("since") LocalDateTime since);

    /**
     * 分頁查詢會話
     */
    @Query("SELECT s FROM JpaAnalyticsSessionEntity s WHERE s.startTime >= :startTime " +
            "ORDER BY s.startTime DESC")
    Page<JpaAnalyticsSessionEntity> findSessionsPageable(
            @Param("startTime") LocalDateTime startTime,
            Pageable pageable);

    /**
     * 統計查詢 - 會話總數
     */
    @Query("SELECT COUNT(s) FROM JpaAnalyticsSessionEntity s WHERE s.startTime >= :startTime")
    long countSessionsSince(@Param("startTime") LocalDateTime startTime);

    /**
     * 統計查詢 - 用戶會話總數
     */
    @Query("SELECT COUNT(s) FROM JpaAnalyticsSessionEntity s WHERE s.userId = :userId " +
            "AND s.startTime >= :startTime")
    long countUserSessionsSince(@Param("userId") String userId, @Param("startTime") LocalDateTime startTime);

    /**
     * 統計查詢 - 匿名會話總數
     */
    @Query("SELECT COUNT(s) FROM JpaAnalyticsSessionEntity s WHERE s.isAnonymous = true " +
            "AND s.startTime >= :startTime")
    long countAnonymousSessionsSince(@Param("startTime") LocalDateTime startTime);

    /**
     * 統計查詢 - 平均會話時長
     */
    @Query("SELECT AVG(s.durationSeconds) FROM JpaAnalyticsSessionEntity s " +
            "WHERE s.durationSeconds IS NOT NULL AND s.startTime >= :startTime")
    Double getAverageSessionDurationSince(@Param("startTime") LocalDateTime startTime);

    /**
     * 統計查詢 - 總頁面瀏覽量
     */
    @Query("SELECT SUM(s.pageViewsCount) FROM JpaAnalyticsSessionEntity s WHERE s.startTime >= :startTime")
    Long getTotalPageViewsSince(@Param("startTime") LocalDateTime startTime);

    /**
     * 統計查詢 - 總用戶操作數
     */
    @Query("SELECT SUM(s.userActionsCount) FROM JpaAnalyticsSessionEntity s WHERE s.startTime >= :startTime")
    Long getTotalUserActionsSince(@Param("startTime") LocalDateTime startTime);

    /**
     * 統計查詢 - 總業務事件數
     */
    @Query("SELECT SUM(s.businessEventsCount) FROM JpaAnalyticsSessionEntity s WHERE s.startTime >= :startTime")
    Long getTotalBusinessEventsSince(@Param("startTime") LocalDateTime startTime);

    /**
     * 統計查詢 - 每日會話統計
     */
    @Query("SELECT DATE(s.startTime) as date, COUNT(s) as sessionCount, " +
            "SUM(s.pageViewsCount) as totalPageViews, " +
            "SUM(s.userActionsCount) as totalUserActions, " +
            "AVG(s.durationSeconds) as avgDuration " +
            "FROM JpaAnalyticsSessionEntity s " +
            "WHERE s.startTime >= :startTime AND s.startTime <= :endTime " +
            "GROUP BY DATE(s.startTime) ORDER BY DATE(s.startTime)")
    List<Object[]> getDailySessionStats(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 統計查詢 - 用戶活躍度統計
     */
    @Query("SELECT s.userId, COUNT(s) as sessionCount, " +
            "SUM(s.pageViewsCount) as totalPageViews, " +
            "SUM(s.userActionsCount) as totalUserActions, " +
            "MAX(s.lastActivityAt) as lastActivity " +
            "FROM JpaAnalyticsSessionEntity s " +
            "WHERE s.userId IS NOT NULL AND s.startTime >= :startTime " +
            "GROUP BY s.userId ORDER BY sessionCount DESC")
    List<Object[]> getUserActivityStats(@Param("startTime") LocalDateTime startTime);

    /**
     * 數據保留政策 - 查找過期會話
     */
    @Query("SELECT s FROM JpaAnalyticsSessionEntity s WHERE s.retentionDate <= :currentTime")
    List<JpaAnalyticsSessionEntity> findExpiredSessions(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 數據保留政策 - 刪除過期會話
     */
    @Modifying
    @Query("DELETE FROM JpaAnalyticsSessionEntity s WHERE s.retentionDate <= :currentTime")
    int deleteExpiredSessions(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 數據保留政策 - 計算過期會話數量
     */
    @Query("SELECT COUNT(s) FROM JpaAnalyticsSessionEntity s WHERE s.retentionDate <= :currentTime")
    long countExpiredSessions(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 效能查詢 - 查找最活躍的會話
     */
    @Query("SELECT s FROM JpaAnalyticsSessionEntity s WHERE s.startTime >= :startTime " +
            "ORDER BY (s.pageViewsCount + s.userActionsCount + s.businessEventsCount) DESC")
    List<JpaAnalyticsSessionEntity> findMostActiveSessions(
            @Param("startTime") LocalDateTime startTime,
            Pageable pageable);

    /**
     * 效能查詢 - 查找長時間會話
     */
    @Query("SELECT s FROM JpaAnalyticsSessionEntity s WHERE s.durationSeconds >= :minDurationSeconds " +
            "AND s.startTime >= :startTime ORDER BY s.durationSeconds DESC")
    List<JpaAnalyticsSessionEntity> findLongSessions(
            @Param("minDurationSeconds") long minDurationSeconds,
            @Param("startTime") LocalDateTime startTime,
            Pageable pageable);
}