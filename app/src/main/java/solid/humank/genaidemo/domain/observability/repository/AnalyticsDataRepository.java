package solid.humank.genaidemo.domain.observability.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import solid.humank.genaidemo.domain.common.annotations.Repository;
import solid.humank.genaidemo.domain.observability.model.aggregate.AnalyticsSession;
import solid.humank.genaidemo.domain.observability.valueobject.SessionId;

/**
 * 分析數據儲存庫介面
 * 
 * 定義分析數據的持久化操作，僅在生產環境中使用。
 * 提供統計查詢和業務指標檢索功能。
 * 
 * 設計原則：
 * - 領域驅動設計，專注於業務概念
 * - 支援複雜的統計查詢
 * - 包含數據保留政策管理
 * - 效能優化的查詢介面
 * 
 * 需求: 2.3, 3.3
 */
@Repository(name = "AnalyticsDataRepository", description = "分析數據儲存庫")
public interface AnalyticsDataRepository {

    // 基本 CRUD 操作

    /**
     * 儲存分析會話
     */
    AnalyticsSession save(AnalyticsSession session);

    /**
     * 根據會話 ID 查找分析會話
     */
    Optional<AnalyticsSession> findBySessionId(SessionId sessionId);

    /**
     * 根據用戶 ID 查找分析會話
     */
    List<AnalyticsSession> findByUserId(String userId);

    /**
     * 刪除分析會話
     */
    void delete(SessionId sessionId);

    // 統計查詢介面

    /**
     * 獲取會話統計數據
     * 
     * @param startTime 開始時間
     * @param endTime   結束時間
     * @return 統計數據 Map
     */
    Map<String, Object> getSessionStats(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 獲取頁面瀏覽統計
     * 
     * @param startTime 開始時間
     * @param endTime   結束時間
     * @return 頁面瀏覽統計列表
     */
    List<Map<String, Object>> getPageViewStats(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 獲取用戶行為統計
     * 
     * @param startTime 開始時間
     * @param endTime   結束時間
     * @return 用戶行為統計列表
     */
    List<Map<String, Object>> getUserBehaviorStats(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 獲取效能指標統計
     * 
     * @param startTime 開始時間
     * @param endTime   結束時間
     * @return 效能指標統計列表
     */
    List<Map<String, Object>> getPerformanceMetricStats(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 獲取業務指標統計
     * 
     * @param startTime 開始時間
     * @param endTime   結束時間
     * @return 業務指標統計 Map
     */
    Map<String, Object> getBusinessMetrics(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 獲取用戶活躍度統計
     * 
     * @param startTime 開始時間
     * @param endTime   結束時間
     * @return 用戶活躍度統計列表
     */
    List<Map<String, Object>> getUserActivityStats(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 獲取每日統計數據
     * 
     * @param startDate 開始日期
     * @param endDate   結束日期
     * @return 每日統計數據列表
     */
    List<Map<String, Object>> getDailyStats(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 獲取即時統計數據
     * 
     * @return 即時統計數據 Map
     */
    Map<String, Object> getRealTimeStats();

    // 高級查詢介面

    /**
     * 查找熱門頁面
     * 
     * @param startTime 開始時間
     * @param limit     限制數量
     * @return 熱門頁面列表
     */
    List<Map<String, Object>> getPopularPages(LocalDateTime startTime, int limit);

    /**
     * 查找效能問題頁面
     * 
     * @param startTime 開始時間
     * @param threshold 效能閾值
     * @return 效能問題頁面列表
     */
    List<Map<String, Object>> getPerformanceIssuePages(LocalDateTime startTime, double threshold);

    /**
     * 查找最活躍用戶
     * 
     * @param startTime 開始時間
     * @param limit     限制數量
     * @return 最活躍用戶列表
     */
    List<Map<String, Object>> getMostActiveUsers(LocalDateTime startTime, int limit);

    /**
     * 獲取轉換漏斗數據
     * 
     * @param funnelSteps 漏斗步驟
     * @param startTime   開始時間
     * @param endTime     結束時間
     * @return 轉換漏斗數據
     */
    Map<String, Object> getConversionFunnelData(List<String> funnelSteps,
            LocalDateTime startTime,
            LocalDateTime endTime);

    // 數據保留政策介面

    /**
     * 清理過期數據
     * 
     * @return 清理的記錄數量
     */
    int cleanupExpiredData();

    /**
     * 獲取數據保留統計
     * 
     * @return 數據保留統計 Map
     */
    Map<String, Object> getDataRetentionStats();

    /**
     * 設定數據保留政策
     * 
     * @param retentionDays 保留天數
     */
    void setDataRetentionPolicy(int retentionDays);

    // 效能監控介面

    /**
     * 獲取儲存庫效能指標
     * 
     * @return 效能指標 Map
     */
    Map<String, Object> getRepositoryPerformanceMetrics();

    /**
     * 獲取查詢執行統計
     * 
     * @return 查詢執行統計 Map
     */
    Map<String, Object> getQueryExecutionStats();
}