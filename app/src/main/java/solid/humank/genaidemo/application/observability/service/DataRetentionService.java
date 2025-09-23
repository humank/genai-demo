package solid.humank.genaidemo.application.observability.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import solid.humank.genaidemo.domain.observability.repository.AnalyticsDataRepository;

/**
 * 數據保留政策服務
 * 
 * 負責管理分析數據的生命週期，包含自動清理過期數據、監控存儲使用情況等功能。
 * 僅在生產環境中啟用，確保數據存儲的可持續性。
 * 
 * 設計原則：
 * - 自動化數據清理，減少人工干預
 * - 可配置的保留政策
 * - 完整的清理日誌和監控
 * - 安全的批次處理機制
 * 
 * 需求: 2.3, 3.3
 */
@Service
@ConditionalOnProperty(name = "genai-demo.observability.analytics.storage", havingValue = "database", matchIfMissing = false)
public class DataRetentionService {

    private static final Logger logger = LoggerFactory.getLogger(DataRetentionService.class);

    private final AnalyticsDataRepository analyticsDataRepository;

    public DataRetentionService(AnalyticsDataRepository analyticsDataRepository) {
        this.analyticsDataRepository = analyticsDataRepository;
    }

    /**
     * 手動執行數據清理 (原定期任務已移除)
     * 可通過 API 或管理界面手動觸發
     */
    @Transactional
    public void performManualCleanup() {
        logger.info("Starting manual data retention cleanup");

        try {
            Map<String, Object> beforeStats = getDataRetentionStats();
            logger.info("Data retention stats before cleanup: {}", beforeStats);

            int cleanedRecords = performDataCleanup();

            Map<String, Object> afterStats = getDataRetentionStats();
            logger.info("Data retention cleanup completed. Cleaned {} records. Stats after cleanup: {}",
                    cleanedRecords, afterStats);

            // 記錄清理效果
            logCleanupResults(beforeStats, afterStats, cleanedRecords);

        } catch (Exception e) {
            logger.error("Error during manual data retention cleanup", e);
            throw e;
        }
    }

    /**
     * 手動執行數據清理
     * 
     * @return 清理的記錄數量
     */
    @Transactional
    public int performDataCleanup() {
        logger.info("Performing manual data cleanup");

        try {
            int cleanedRecords = analyticsDataRepository.cleanupExpiredData();
            logger.info("Manual data cleanup completed. Cleaned {} records", cleanedRecords);
            return cleanedRecords;

        } catch (Exception e) {
            logger.error("Error during manual data cleanup", e);
            throw e;
        }
    }

    /**
     * 獲取數據保留統計
     * 
     * @return 數據保留統計
     */
    public Map<String, Object> getDataRetentionStats() {
        try {
            return analyticsDataRepository.getDataRetentionStats();
        } catch (Exception e) {
            logger.error("Error getting data retention stats", e);
            throw e;
        }
    }

    /**
     * 設定數據保留政策
     * 
     * @param retentionDays 保留天數
     */
    @Transactional
    public void setDataRetentionPolicy(int retentionDays) {
        logger.info("Setting data retention policy to {} days", retentionDays);

        if (retentionDays < 1) {
            throw new IllegalArgumentException("Retention days must be at least 1");
        }

        if (retentionDays > 3650) { // 10 年
            throw new IllegalArgumentException("Retention days cannot exceed 3650 (10 years)");
        }

        try {
            analyticsDataRepository.setDataRetentionPolicy(retentionDays);
            logger.info("Data retention policy set to {} days successfully", retentionDays);

        } catch (Exception e) {
            logger.error("Error setting data retention policy to {} days", retentionDays, e);
            throw e;
        }
    }

    /**
     * 檢查存儲健康狀況
     * 
     * @return 存儲健康狀況報告
     */
    public Map<String, Object> checkStorageHealth() {
        logger.debug("Checking storage health");

        try {
            Map<String, Object> retentionStats = getDataRetentionStats();
            Map<String, Object> performanceMetrics = analyticsDataRepository.getRepositoryPerformanceMetrics();

            // 計算健康指標
            Long totalSessions = (Long) retentionStats.get("totalSessions");
            Long totalEvents = (Long) retentionStats.get("totalEvents");
            Long expiredSessions = (Long) retentionStats.get("expiredSessions");
            Long expiredEvents = (Long) retentionStats.get("expiredEvents");

            boolean isHealthy = calculateStorageHealth(totalSessions, totalEvents, expiredSessions, expiredEvents);

            Map<String, Object> healthReport = Map.of(
                    "isHealthy", isHealthy,
                    "timestamp", LocalDateTime.now(),
                    "retentionStats", retentionStats,
                    "performanceMetrics", performanceMetrics,
                    "recommendations", generateRecommendations(retentionStats));

            logger.debug("Storage health check completed: {}", healthReport);
            return healthReport;

        } catch (Exception e) {
            logger.error("Error checking storage health", e);
            throw e;
        }
    }

    /**
     * 預測存儲使用趨勢
     * 
     * @return 存儲使用趨勢預測
     */
    public Map<String, Object> predictStorageUsage() {
        logger.debug("Predicting storage usage trends");

        try {
            Map<String, Object> currentStats = getDataRetentionStats();

            Long totalRecords = (Long) currentStats.get("totalSessions") + (Long) currentStats.get("totalEvents");

            // 簡化的趨勢預測（實際實現可能需要更複雜的算法）
            Map<String, Object> prediction = Map.of(
                    "currentRecords", totalRecords,
                    "predictedGrowthRate", 0.05, // 5% 每月增長
                    "estimatedRecordsIn30Days", Math.round(totalRecords * 1.05),
                    "estimatedRecordsIn90Days", Math.round(totalRecords * 1.16),
                    "recommendedCleanupFrequency", "daily",
                    "timestamp", LocalDateTime.now());

            logger.debug("Storage usage prediction completed: {}", prediction);
            return prediction;

        } catch (Exception e) {
            logger.error("Error predicting storage usage", e);
            throw e;
        }
    }

    private boolean calculateStorageHealth(Long totalSessions, Long totalEvents,
            Long expiredSessions, Long expiredEvents) {
        if (totalSessions == null || totalEvents == null ||
                expiredSessions == null || expiredEvents == null) {
            return false;
        }

        long totalRecords = totalSessions + totalEvents;
        long expiredRecords = expiredSessions + expiredEvents;

        if (totalRecords == 0) {
            return true; // 沒有數據時視為健康
        }

        double expiredRatio = (double) expiredRecords / totalRecords;

        // 如果過期數據比例超過 20%，視為不健康
        return expiredRatio <= 0.2;
    }

    private Map<String, String> generateRecommendations(Map<String, Object> retentionStats) {
        Long totalSessions = (Long) retentionStats.get("totalSessions");
        Long totalEvents = (Long) retentionStats.get("totalEvents");
        Long expiredSessions = (Long) retentionStats.get("expiredSessions");
        Long expiredEvents = (Long) retentionStats.get("expiredEvents");

        if (totalSessions == null || totalEvents == null ||
                expiredSessions == null || expiredEvents == null) {
            return Map.of("general", "Unable to generate recommendations due to missing data");
        }

        long totalRecords = totalSessions + totalEvents;
        long expiredRecords = expiredSessions + expiredEvents;

        if (totalRecords == 0) {
            return Map.of("general", "No data available for analysis");
        }

        double expiredRatio = (double) expiredRecords / totalRecords;

        if (expiredRatio > 0.3) {
            return Map.of(
                    "cleanup", "High amount of expired data detected. Consider running cleanup immediately.",
                    "policy", "Consider reducing data retention period to optimize storage.");
        } else if (expiredRatio > 0.1) {
            return Map.of(
                    "cleanup", "Moderate amount of expired data. Schedule cleanup within 24 hours.",
                    "monitoring", "Monitor data growth trends closely.");
        } else {
            return Map.of(
                    "status", "Data retention is healthy. Continue current practices.",
                    "optimization", "Consider implementing automated cleanup if not already enabled.");
        }
    }

    private void logCleanupResults(Map<String, Object> beforeStats,
            Map<String, Object> afterStats,
            int cleanedRecords) {
        Long beforeTotal = (Long) beforeStats.get("totalSessions") + (Long) beforeStats.get("totalEvents");
        Long afterTotal = (Long) afterStats.get("totalSessions") + (Long) afterStats.get("totalEvents");

        double reductionPercentage = beforeTotal > 0 ? ((double) cleanedRecords / beforeTotal) * 100 : 0.0;

        logger.info("Cleanup results summary:");
        logger.info("  - Records before cleanup: {}", beforeTotal);
        logger.info("  - Records after cleanup: {}", afterTotal);
        logger.info("  - Records cleaned: {}", cleanedRecords);
        logger.info("  - Reduction percentage: {:.2f}%", reductionPercentage);

        Double beforeRetentionRate = (Double) beforeStats.get("sessionRetentionRate");
        Double afterRetentionRate = (Double) afterStats.get("sessionRetentionRate");

        if (beforeRetentionRate != null && afterRetentionRate != null) {
            logger.info("  - Session retention rate improved from {:.2f}% to {:.2f}%",
                    beforeRetentionRate * 100, afterRetentionRate * 100);
        }
    }
}