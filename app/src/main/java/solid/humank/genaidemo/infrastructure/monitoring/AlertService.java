package solid.humank.genaidemo.infrastructure.monitoring;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import solid.humank.genaidemo.infrastructure.observability.event.ProfileAwareEventProcessor.AlertType;

/**
 * 環境適應性警報服務
 * 
 * 功能特性：
 * - 根據環境配置調整警報策略
 * - 支援多種警報類型和嚴重程度
 * - 警報頻率控制和去重
 * - 詳細的警報統計和監控
 * - 整合追蹤 ID 和上下文資訊
 * 
 * 環境差異：
 * - 開發環境：日誌記錄，控制台輸出
 * - 測試環境：日誌記錄，簡化通知
 * - 生產環境：完整警報系統，外部通知
 * 
 * 需求: 2.2, 3.1, 3.2
 */
@Service
public class AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);

    @Value("${genai-demo.events.publisher:in-memory}")
    private String publisherType;

    @Value("${genai-demo.alerts.enabled:true}")
    private boolean alertsEnabled;

    @Value("${genai-demo.alerts.rate-limit.window-minutes:5}")
    private int rateLimitWindowMinutes;

    @Value("${genai-demo.alerts.rate-limit.max-per-window:10}")
    private int maxAlertsPerWindow;

    // 警報統計
    private final Map<AlertType, AtomicLong> alertCounters = new ConcurrentHashMap<>();
    private final Map<AlertType, LocalDateTime> lastAlertTimes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> alertRateLimitCounters = new ConcurrentHashMap<>();
    private final AtomicLong totalAlertsTriggered = new AtomicLong(0);

    /**
     * 觸發警報
     * 
     * @param alertType 警報類型
     * @param context   警報上下文資訊
     */
    public void triggerAlert(AlertType alertType, Map<String, Object> context) {
        if (!alertsEnabled) {
            logger.debug("Alerts are disabled, skipping alert: {}", alertType);
            return;
        }

        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = "alert_" + System.currentTimeMillis();
            MDC.put("correlationId", correlationId);
        }

        // 檢查警報頻率限制
        if (isRateLimited(alertType)) {
            logger.debug("Alert rate limited: {} [correlationId: {}]", alertType, correlationId);
            return;
        }

        try {
            // 更新警報統計
            updateAlertStatistics(alertType);

            // 根據環境選擇警報策略
            if ("kafka".equals(publisherType)) {
                // 生產環境：完整警報處理
                handleProductionAlert(alertType, context, correlationId);
            } else {
                // 開發/測試環境：簡化警報處理
                handleDevelopmentAlert(alertType, context, correlationId);
            }

            logger.info("Alert triggered successfully: {} [correlationId: {}]", alertType, correlationId);

        } catch (Exception e) {
            logger.error("Failed to trigger alert: {} [correlationId: {}]", alertType, correlationId, e);
        }
    }

    /**
     * 觸發高優先級警報 (繞過頻率限制)
     * 
     * @param alertType 警報類型
     * @param context   警報上下文資訊
     */
    public void triggerCriticalAlert(AlertType alertType, Map<String, Object> context) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = "critical_alert_" + System.currentTimeMillis();
            MDC.put("correlationId", correlationId);
        }

        logger.warn("CRITICAL ALERT triggered: {} [correlationId: {}]", alertType, correlationId);

        try {
            // 更新警報統計
            updateAlertStatistics(alertType);

            // 強制處理關鍵警報，不受環境限制
            handleCriticalAlert(alertType, context, correlationId);

            logger.warn("Critical alert processed successfully: {} [correlationId: {}]", alertType, correlationId);

        } catch (Exception e) {
            logger.error("Failed to process critical alert: {} [correlationId: {}]", alertType, correlationId, e);
        }
    }

    /**
     * 批量觸發警報
     * 
     * @param alerts 警報資訊列表
     */
    public void triggerBatchAlerts(Map<AlertType, Map<String, Object>> alerts) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = "batch_alert_" + System.currentTimeMillis();
            MDC.put("correlationId", correlationId);
        }

        logger.debug("Processing batch of {} alerts [correlationId: {}]", alerts.size(), correlationId);

        for (Map.Entry<AlertType, Map<String, Object>> entry : alerts.entrySet()) {
            try {
                triggerAlert(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                logger.error("Failed to process alert in batch: {} [correlationId: {}]",
                        entry.getKey(), correlationId, e);
            }
        }

        logger.debug("Batch alert processing completed [correlationId: {}]", correlationId);
    }

    // === 私有處理方法 ===

    /**
     * 處理生產環境警報
     */
    private void handleProductionAlert(AlertType alertType, Map<String, Object> context, String correlationId) {
        logger.info("Processing production alert: {} with context: {} [correlationId: {}]",
                alertType, context, correlationId);

        // 生產環境警報處理邏輯：
        // 1. 發送到外部監控系統 (如 CloudWatch, Datadog)
        // 2. 發送通知到 Slack/Teams
        // 3. 創建事件票據
        // 4. 更新監控儀表板

        switch (alertType) {
            case USER_BEHAVIOR_EVENT_PROCESSING_FAILURE:
                handleUserBehaviorProcessingFailureAlert(context, correlationId);
                break;
            case PERFORMANCE_METRIC_EVENT_PROCESSING_FAILURE:
                handlePerformanceMetricProcessingFailureAlert(context, correlationId);
                break;
            case BATCH_EVENT_PROCESSING_FAILURE:
                handleBatchProcessingFailureAlert(context, correlationId);
                break;
            case CIRCUIT_BREAKER_OPENED:
                handleCircuitBreakerOpenedAlert(context, correlationId);
                break;
            default:
                handleGenericProductionAlert(alertType, context, correlationId);
        }
    }

    /**
     * 處理開發環境警報
     */
    private void handleDevelopmentAlert(AlertType alertType, Map<String, Object> context, String correlationId) {
        logger.warn("Development alert: {} with context: {} [correlationId: {}]",
                alertType, context, correlationId);

        // 開發環境警報處理邏輯：
        // 1. 詳細日誌記錄
        // 2. 控制台輸出
        // 3. 本地通知 (如果支援)
        // 4. 開發儀表板更新

        String alertMessage = formatDevelopmentAlertMessage(alertType, context);

        // 輸出到控制台 (開發環境)
        System.err.println("🚨 DEVELOPMENT ALERT: " + alertMessage);

        // 記錄詳細資訊
        logger.warn("Development Alert Details - Type: {}, Context: {}, CorrelationId: {}",
                alertType, context, correlationId);
    }

    /**
     * 處理關鍵警報
     */
    private void handleCriticalAlert(AlertType alertType, Map<String, Object> context, String correlationId) {
        logger.error("CRITICAL ALERT: {} with context: {} [correlationId: {}]",
                alertType, context, correlationId);

        // 關鍵警報處理邏輯 (不分環境)：
        // 1. 立即通知
        // 2. 創建高優先級事件
        // 3. 觸發自動恢復機制
        // 4. 記錄到安全日誌

        String criticalMessage = formatCriticalAlertMessage(alertType, context);

        // 強制輸出到控制台
        System.err.println("🔥 CRITICAL ALERT: " + criticalMessage);

        // 記錄到安全日誌
        logger.error("SECURITY/CRITICAL - Alert: {}, Context: {}, CorrelationId: {}",
                alertType, context, correlationId);

        // 在生產環境中，這裡會觸發更多緊急處理邏輯
        if ("kafka".equals(publisherType)) {
            handleProductionCriticalAlert(alertType, context, correlationId);
        }
    }

    // === 特定警報類型處理方法 ===

    /**
     * 處理用戶行為事件處理失敗警報
     */
    private void handleUserBehaviorProcessingFailureAlert(Map<String, Object> context, String correlationId) {
        logger.warn("User behavior event processing failure detected [correlationId: {}]: {}",
                correlationId, context);

        // 特定處理邏輯：
        // 1. 檢查失敗率
        // 2. 分析失敗模式
        // 3. 觸發自動恢復
    }

    /**
     * 處理效能指標事件處理失敗警報
     */
    private void handlePerformanceMetricProcessingFailureAlert(Map<String, Object> context, String correlationId) {
        logger.warn("Performance metric event processing failure detected [correlationId: {}]: {}",
                correlationId, context);

        // 特定處理邏輯：
        // 1. 檢查系統效能
        // 2. 分析資源使用
        // 3. 調整處理策略
    }

    /**
     * 處理批量事件處理失敗警報
     */
    private void handleBatchProcessingFailureAlert(Map<String, Object> context, String correlationId) {
        logger.warn("Batch event processing failure detected [correlationId: {}]: {}",
                correlationId, context);

        // 特定處理邏輯：
        // 1. 分析批量大小
        // 2. 檢查系統負載
        // 3. 調整批量策略
    }

    /**
     * 處理斷路器開啟警報
     */
    private void handleCircuitBreakerOpenedAlert(Map<String, Object> context, String correlationId) {
        logger.error("Circuit breaker opened [correlationId: {}]: {}", correlationId, context);

        // 特定處理邏輯：
        // 1. 分析失敗模式
        // 2. 檢查下游服務
        // 3. 觸發降級策略
    }

    /**
     * 處理通用生產環境警報
     */
    private void handleGenericProductionAlert(AlertType alertType, Map<String, Object> context, String correlationId) {
        logger.info("Generic production alert: {} [correlationId: {}]: {}",
                alertType, correlationId, context);
    }

    /**
     * 處理生產環境關鍵警報
     */
    private void handleProductionCriticalAlert(AlertType alertType, Map<String, Object> context, String correlationId) {
        logger.error("Production critical alert: {} [correlationId: {}]: {}",
                alertType, correlationId, context);

        // 生產環境關鍵警報處理：
        // 1. 立即通知 on-call 工程師
        // 2. 創建 P0 事件
        // 3. 觸發自動恢復程序
        // 4. 記錄到審計日誌
    }

    // === 輔助方法 ===

    /**
     * 檢查警報是否受頻率限制
     */
    private boolean isRateLimited(AlertType alertType) {
        String rateLimitKey = alertType.name() + "_" + getCurrentWindowKey();
        AtomicLong counter = alertRateLimitCounters.computeIfAbsent(rateLimitKey, k -> new AtomicLong(0));

        long currentCount = counter.incrementAndGet();

        if (currentCount > maxAlertsPerWindow) {
            logger.debug("Alert rate limit exceeded for {}: {} > {}",
                    alertType, currentCount, maxAlertsPerWindow);
            return true;
        }

        return false;
    }

    /**
     * 獲取當前時間窗口鍵
     */
    private String getCurrentWindowKey() {
        long currentMinutes = System.currentTimeMillis() / (1000 * 60);

        // 防止除零錯誤
        if (rateLimitWindowMinutes <= 0) {
            return String.valueOf(currentMinutes);
        }

        long windowStart = (currentMinutes / rateLimitWindowMinutes) * rateLimitWindowMinutes;
        return String.valueOf(windowStart);
    }

    /**
     * 更新警報統計
     */
    private void updateAlertStatistics(AlertType alertType) {
        alertCounters.computeIfAbsent(alertType, k -> new AtomicLong(0)).incrementAndGet();
        lastAlertTimes.put(alertType, LocalDateTime.now());
        totalAlertsTriggered.incrementAndGet();

        logger.trace("Alert statistics updated - Type: {}, Count: {}, Total: {}",
                alertType, alertCounters.get(alertType).get(), totalAlertsTriggered.get());
    }

    /**
     * 格式化開發環境警報訊息
     */
    private String formatDevelopmentAlertMessage(AlertType alertType, Map<String, Object> context) {
        StringBuilder message = new StringBuilder();
        message.append(alertType.name()).append(" - ");

        context.forEach((key, value) -> {
            message.append(key).append(": ").append(value).append(", ");
        });

        // 移除最後的逗號和空格
        if (message.length() > 2) {
            message.setLength(message.length() - 2);
        }

        return message.toString();
    }

    /**
     * 格式化關鍵警報訊息
     */
    private String formatCriticalAlertMessage(AlertType alertType, Map<String, Object> context) {
        return String.format("CRITICAL: %s - Environment: %s - Context: %s",
                alertType.name(), publisherType, context);
    }

    // === 監控和診斷方法 ===

    /**
     * 獲取警報統計
     */
    public Map<AlertType, Long> getAlertStatistics() {
        return alertCounters.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get()));
    }

    /**
     * 獲取特定警報類型的觸發次數
     */
    public long getAlertCount(AlertType alertType) {
        return alertCounters.getOrDefault(alertType, new AtomicLong(0)).get();
    }

    /**
     * 獲取特定警報類型的最後觸發時間
     */
    public LocalDateTime getLastAlertTime(AlertType alertType) {
        return lastAlertTimes.get(alertType);
    }

    /**
     * 獲取總警報觸發次數
     */
    public long getTotalAlertsTriggered() {
        return totalAlertsTriggered.get();
    }

    /**
     * 檢查警報是否啟用
     */
    public boolean isAlertsEnabled() {
        return alertsEnabled;
    }

    /**
     * 啟用/停用警報
     */
    public void setAlertsEnabled(boolean enabled) {
        this.alertsEnabled = enabled;
        logger.info("Alerts {}", enabled ? "enabled" : "disabled");
    }

    /**
     * 清除警報統計 (主要用於測試)
     */
    public void clearStatistics() {
        alertCounters.clear();
        lastAlertTimes.clear();
        alertRateLimitCounters.clear();
        totalAlertsTriggered.set(0);

        logger.debug("Cleared all alert statistics");
    }

    /**
     * 獲取當前環境類型
     */
    public String getEnvironmentType() {
        return publisherType;
    }
}