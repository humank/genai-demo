package solid.humank.genaidemo.application.observability.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import solid.humank.genaidemo.application.observability.dto.AnalyticsStatsDto;
import solid.humank.genaidemo.application.observability.dto.BusinessMetricsDto;
import solid.humank.genaidemo.application.observability.dto.PerformanceStatsDto;
import solid.humank.genaidemo.application.observability.dto.UserActivityStatsDto;
import solid.humank.genaidemo.domain.observability.repository.AnalyticsDataRepository;

/**
 * 分析查詢服務
 * 
 * 提供統計資料檢索和業務指標查詢功能，僅在生產環境中啟用。
 * 支援多維度的數據分析和即時統計查詢。
 * 
 * 設計原則：
 * - 應用服務層，協調領域邏輯
 * - 僅在生產環境啟用，避免開發環境複雜性
 * - 高效的查詢聚合和數據轉換
 * - 完整的錯誤處理和日誌記錄
 * 
 * 需求: 2.3, 3.3
 */
@Service
@ConditionalOnProperty(name = "genai-demo.observability.analytics.storage", havingValue = "database", matchIfMissing = false)
@Transactional(readOnly = true)
public class AnalyticsQueryService {

    private final AnalyticsDataRepository analyticsDataRepository;

    public AnalyticsQueryService(AnalyticsDataRepository analyticsDataRepository) {
        this.analyticsDataRepository = analyticsDataRepository;
    }

    /**
     * 獲取綜合分析統計
     * 
     * @param startTime 開始時間
     * @param endTime   結束時間
     * @return 分析統計 DTO
     */
    public AnalyticsStatsDto getAnalyticsStats(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> sessionStats = analyticsDataRepository.getSessionStats(startTime, endTime);
        Map<String, Object> businessMetrics = analyticsDataRepository.getBusinessMetrics(startTime, endTime);
        List<Map<String, Object>> pageViewStats = analyticsDataRepository.getPageViewStats(startTime, endTime);
        List<Map<String, Object>> performanceStats = analyticsDataRepository.getPerformanceMetricStats(startTime,
                endTime);

        return AnalyticsStatsDto.builder()
                .startTime(startTime)
                .endTime(endTime)
                .totalSessions((Long) sessionStats.get("totalSessions"))
                .anonymousSessions((Long) sessionStats.get("anonymousSessions"))
                .registeredUserSessions((Long) sessionStats.get("registeredUserSessions"))
                .averageSessionDuration((Double) sessionStats.get("averageSessionDuration"))
                .totalPageViews((Long) sessionStats.get("totalPageViews"))
                .totalUserActions((Long) sessionStats.get("totalUserActions"))
                .totalBusinessEvents((Long) sessionStats.get("totalBusinessEvents"))
                .totalEvents((Long) businessMetrics.get("totalEvents"))
                .actionConversionRate((Double) businessMetrics.get("actionConversionRate"))
                .businessConversionRate((Double) businessMetrics.get("businessConversionRate"))
                .pageViewStats(pageViewStats)
                .performanceStats(performanceStats)
                .build();
    }

    /**
     * 獲取業務指標統計
     * 
     * @param startTime 開始時間
     * @param endTime   結束時間
     * @return 業務指標 DTO
     */
    public BusinessMetricsDto getBusinessMetrics(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> metrics = analyticsDataRepository.getBusinessMetrics(startTime, endTime);
        List<Map<String, Object>> funnelData = getFunnelAnalysis(startTime, endTime);
        List<Map<String, Object>> popularPages = analyticsDataRepository.getPopularPages(startTime, 10);

        return BusinessMetricsDto.builder()
                .startTime(startTime)
                .endTime(endTime)
                .totalEvents((Long) metrics.get("totalEvents"))
                .pageViews((Long) metrics.get("pageViews"))
                .userActions((Long) metrics.get("userActions"))
                .businessEvents((Long) metrics.get("businessEvents"))
                .actionConversionRate((Double) metrics.get("actionConversionRate"))
                .businessConversionRate((Double) metrics.get("businessConversionRate"))
                .funnelData(funnelData)
                .popularPages(popularPages)
                .build();
    }

    /**
     * 獲取效能統計
     * 
     * @param startTime 開始時間
     * @param endTime   結束時間
     * @return 效能統計 DTO
     */
    public PerformanceStatsDto getPerformanceStats(LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> performanceMetrics = analyticsDataRepository.getPerformanceMetricStats(startTime,
                endTime);
        List<Map<String, Object>> issuePages = analyticsDataRepository.getPerformanceIssuePages(startTime, 2000.0); // 2秒閾值

        return PerformanceStatsDto.builder()
                .startTime(startTime)
                .endTime(endTime)
                .performanceMetrics(performanceMetrics)
                .performanceIssuePages(issuePages)
                .build();
    }

    /**
     * 獲取用戶活躍度統計
     * 
     * @param startTime 開始時間
     * @param endTime   結束時間
     * @return 用戶活躍度統計 DTO
     */
    public UserActivityStatsDto getUserActivityStats(LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> userActivity = analyticsDataRepository.getUserActivityStats(startTime, endTime);
        List<Map<String, Object>> mostActiveUsers = analyticsDataRepository.getMostActiveUsers(startTime, 20);

        return UserActivityStatsDto.builder()
                .startTime(startTime)
                .endTime(endTime)
                .userActivityStats(userActivity)
                .mostActiveUsers(mostActiveUsers)
                .build();
    }

    /**
     * 獲取每日統計趨勢
     * 
     * @param startDate 開始日期
     * @param endDate   結束日期
     * @return 每日統計數據列表
     */
    public List<Map<String, Object>> getDailyTrends(LocalDateTime startDate, LocalDateTime endDate) {
        return analyticsDataRepository.getDailyStats(startDate, endDate);
    }

    /**
     * 獲取即時統計數據
     * 
     * @return 即時統計數據
     */
    public Map<String, Object> getRealTimeStats() {
        return analyticsDataRepository.getRealTimeStats();
    }

    /**
     * 獲取熱門頁面統計
     * 
     * @param startTime 開始時間
     * @param limit     限制數量
     * @return 熱門頁面列表
     */
    public List<Map<String, Object>> getPopularPages(LocalDateTime startTime, int limit) {
        return analyticsDataRepository.getPopularPages(startTime, limit);
    }

    /**
     * 獲取效能問題頁面
     * 
     * @param startTime 開始時間
     * @param threshold 效能閾值（毫秒）
     * @return 效能問題頁面列表
     */
    public List<Map<String, Object>> getPerformanceIssuePages(LocalDateTime startTime, double threshold) {
        return analyticsDataRepository.getPerformanceIssuePages(startTime, threshold);
    }

    /**
     * 獲取轉換漏斗分析
     * 
     * @param startTime 開始時間
     * @param endTime   結束時間
     * @return 漏斗分析數據
     */
    public List<Map<String, Object>> getFunnelAnalysis(LocalDateTime startTime, LocalDateTime endTime) {
        // 定義標準的轉換漏斗步驟
        List<String> funnelSteps = List.of(
                "page_view", // 頁面瀏覽
                "user_action", // 用戶操作
                "business_event" // 業務事件
        );

        Map<String, Object> funnelData = analyticsDataRepository.getConversionFunnelData(
                funnelSteps, startTime, endTime);

        return List.of(funnelData);
    }

    /**
     * 獲取數據保留統計
     * 
     * @return 數據保留統計
     */
    public Map<String, Object> getDataRetentionStats() {
        return analyticsDataRepository.getDataRetentionStats();
    }

    /**
     * 獲取儲存庫效能指標
     * 
     * @return 效能指標
     */
    public Map<String, Object> getRepositoryPerformanceMetrics() {
        return analyticsDataRepository.getRepositoryPerformanceMetrics();
    }

    /**
     * 執行數據清理
     * 
     * @return 清理的記錄數量
     */
    @Transactional
    public int performDataCleanup() {
        return analyticsDataRepository.cleanupExpiredData();
    }

    /**
     * 設定數據保留政策
     * 
     * @param retentionDays 保留天數
     */
    @Transactional
    public void setDataRetentionPolicy(int retentionDays) {
        analyticsDataRepository.setDataRetentionPolicy(retentionDays);
    }
}