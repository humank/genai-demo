package solid.humank.genaidemo.infrastructure.observability.analytics;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.observability.events.PerformanceMetricReceivedEvent;
import solid.humank.genaidemo.domain.observability.events.UserBehaviorAnalyticsEvent;
import solid.humank.genaidemo.infrastructure.event.publisher.DomainEventPublisherAdapter;

/**
 * Profile-Aware 即時分析引擎
 * 
 * 根據環境配置自動選擇事件消費策略：
 * - 開發/測試環境：記憶體事件處理，支援快速開發和測試
 * - 生產環境：Kafka 事件消費，支援分散式處理和擴展
 * 
 * 功能特性：
 * - 環境差異化事件消費
 * - 即時業務指標更新
 * - WebSocket 即時通知
 * - 效能指標分析和警報
 * - 用戶行為分析和統計
 * 
 * 需求: 2.3, 3.3
 */
@Component
public class ProfileAwareAnalyticsEngine {

    private static final Logger logger = LoggerFactory.getLogger(ProfileAwareAnalyticsEngine.class);

    private final SimpMessagingTemplate messagingTemplate;

    @Value("${genai-demo.events.publisher:in-memory}")
    private String publisherType;

    // 業務指標統計
    private final Map<String, AtomicLong> userBehaviorMetrics = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> performanceMetrics = new ConcurrentHashMap<>();
    private final Map<String, Double> performanceValues = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastUpdateTimes = new ConcurrentHashMap<>();
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);

    // 效能閾值配置
    private static final double LCP_THRESHOLD = 2500.0; // 2.5s
    private static final double FID_THRESHOLD = 100.0; // 100ms
    private static final double CLS_THRESHOLD = 0.1; // 0.1
    private static final double TTFB_THRESHOLD = 600.0; // 600ms
    private static final double PAGE_LOAD_THRESHOLD = 3000.0; // 3s

    public ProfileAwareAnalyticsEngine(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        logger.info("ProfileAwareAnalyticsEngine initialized with publisher type: {}", publisherType);
    }

    /**
     * 處理記憶體中的用戶行為分析事件 (開發/測試環境)
     * 
     * @param wrapper 領域事件包裝器
     */
    @EventListener
    @ConditionalOnProperty(name = "genai-demo.events.publisher", havingValue = "in-memory")
    public void handleInMemoryUserBehaviorEvent(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        if (wrapper.getSource() instanceof UserBehaviorAnalyticsEvent event) {
            String correlationId = MDC.get("correlationId");
            if (correlationId == null) {
                correlationId = event.traceId();
                MDC.put("correlationId", correlationId);
            }

            logger.debug("Processing UserBehaviorAnalyticsEvent in memory: {} [correlationId: {}]",
                    event.getFrontendEventType(), correlationId);

            try {
                processUserBehaviorEventInMemory(event);
                updateRealTimeMetrics(event);

                logger.debug("UserBehaviorAnalyticsEvent processed successfully in memory [correlationId: {}]",
                        correlationId);

            } catch (Exception e) {
                logger.error("Failed to process UserBehaviorAnalyticsEvent in memory [correlationId: {}]",
                        correlationId, e);
            }
        }
    }

    /**
     * 處理記憶體中的效能指標事件 (開發/測試環境)
     * 
     * @param wrapper 領域事件包裝器
     */
    @EventListener
    @ConditionalOnProperty(name = "genai-demo.events.publisher", havingValue = "in-memory")
    public void handleInMemoryPerformanceMetricEvent(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        if (wrapper.getSource() instanceof PerformanceMetricReceivedEvent event) {
            String correlationId = MDC.get("correlationId");
            if (correlationId == null) {
                correlationId = event.traceId();
                MDC.put("correlationId", correlationId);
            }

            logger.debug("Processing PerformanceMetricReceivedEvent in memory: {} = {} [correlationId: {}]",
                    event.metricType(), event.value(), correlationId);

            try {
                processPerformanceMetricEventInMemory(event);
                updateRealTimeMetrics(event);

                logger.debug("PerformanceMetricReceivedEvent processed successfully in memory [correlationId: {}]",
                        correlationId);

            } catch (Exception e) {
                logger.error("Failed to process PerformanceMetricReceivedEvent in memory [correlationId: {}]",
                        correlationId, e);
            }
        }
    }

    /**
     * 處理來自 Kafka 的用戶行為分析事件 (生產環境)
     * 
     * @param event 用戶行為分析事件
     */
    @KafkaListener(topics = "#{@kafkaTopicConfig.getUserBehaviorTopicName()}")
    @ConditionalOnProperty(name = "genai-demo.events.publisher", havingValue = "kafka")
    public void handleKafkaUserBehaviorEvent(UserBehaviorAnalyticsEvent event) {
        String correlationId = event.traceId();
        MDC.put("correlationId", correlationId);

        logger.debug("Processing UserBehaviorAnalyticsEvent from Kafka: {} [correlationId: {}]",
                event.getFrontendEventType(), correlationId);

        try {
            processUserBehaviorEventFromKafka(event);
            updateRealTimeMetrics(event);

            logger.debug("UserBehaviorAnalyticsEvent processed successfully from Kafka [correlationId: {}]",
                    correlationId);

        } catch (Exception e) {
            logger.error("Failed to process UserBehaviorAnalyticsEvent from Kafka [correlationId: {}]",
                    correlationId, e);
            // 在生產環境中，可以將失敗的事件發送到 DLQ
            throw e; // 讓 Kafka 重試機制處理
        } finally {
            MDC.clear();
        }
    }

    /**
     * 處理來自 Kafka 的效能指標事件 (生產環境)
     * 
     * @param event 效能指標接收事件
     */
    @KafkaListener(topics = "#{@kafkaTopicConfig.getPerformanceMetricTopicName()}")
    @ConditionalOnProperty(name = "genai-demo.events.publisher", havingValue = "kafka")
    public void handleKafkaPerformanceMetricEvent(PerformanceMetricReceivedEvent event) {
        String correlationId = event.traceId();
        MDC.put("correlationId", correlationId);

        logger.debug("Processing PerformanceMetricReceivedEvent from Kafka: {} = {} [correlationId: {}]",
                event.metricType(), event.value(), correlationId);

        try {
            processPerformanceMetricEventFromKafka(event);
            updateRealTimeMetrics(event);

            logger.debug("PerformanceMetricReceivedEvent processed successfully from Kafka [correlationId: {}]",
                    correlationId);

        } catch (Exception e) {
            logger.error("Failed to process PerformanceMetricReceivedEvent from Kafka [correlationId: {}]",
                    correlationId, e);
            // 在生產環境中，可以將失敗的事件發送到 DLQ
            throw e; // 讓 Kafka 重試機制處理
        } finally {
            MDC.clear();
        }
    }

    /**
     * 記憶體處理用戶行為事件
     * 
     * @param event 用戶行為分析事件
     */
    private void processUserBehaviorEventInMemory(UserBehaviorAnalyticsEvent event) {
        // 更新用戶行為統計
        String eventTypeKey = "user_behavior_" + event.getFrontendEventType();
        userBehaviorMetrics.computeIfAbsent(eventTypeKey, k -> new AtomicLong(0)).incrementAndGet();

        // 處理特定的業務邏輯
        switch (event.getFrontendEventType()) {
            case "page_view" -> processPageViewEvent(event);
            case "user_action" -> processUserActionEvent(event);
            case "business_event" -> processBusinessEvent(event);
            default -> logger.debug("Unknown user behavior event type: {}", event.getFrontendEventType());
        }

        logger.trace("User behavior event processed in memory: {} for session: {}",
                event.getFrontendEventType(), event.sessionId());
    }

    /**
     * 記憶體處理效能指標事件
     * 
     * @param event 效能指標接收事件
     */
    private void processPerformanceMetricEventInMemory(PerformanceMetricReceivedEvent event) {
        // 更新效能指標統計
        String metricKey = "performance_" + event.metricType();
        performanceMetrics.computeIfAbsent(metricKey, k -> new AtomicLong(0)).incrementAndGet();
        performanceValues.put(metricKey, event.value());

        // 檢查效能閾值
        checkPerformanceThreshold(event);

        logger.trace("Performance metric event processed in memory: {} = {} for page: {}",
                event.metricType(), event.value(), event.page());
    }

    /**
     * Kafka 處理用戶行為事件
     * 
     * @param event 用戶行為分析事件
     */
    private void processUserBehaviorEventFromKafka(UserBehaviorAnalyticsEvent event) {
        // 生產環境的用戶行為處理邏輯
        String eventTypeKey = "user_behavior_" + event.getFrontendEventType();
        userBehaviorMetrics.computeIfAbsent(eventTypeKey, k -> new AtomicLong(0)).incrementAndGet();

        // 在生產環境中，可以：
        // 1. 將數據寫入資料庫
        // 2. 更新搜尋索引
        // 3. 觸發推薦系統更新
        // 4. 發送到分析系統

        logger.trace("User behavior event processed from Kafka: {} for session: {}",
                event.getFrontendEventType(), event.sessionId());
    }

    /**
     * Kafka 處理效能指標事件
     * 
     * @param event 效能指標接收事件
     */
    private void processPerformanceMetricEventFromKafka(PerformanceMetricReceivedEvent event) {
        // 生產環境的效能指標處理邏輯
        String metricKey = "performance_" + event.metricType();
        performanceMetrics.computeIfAbsent(metricKey, k -> new AtomicLong(0)).incrementAndGet();
        performanceValues.put(metricKey, event.value());

        // 檢查效能閾值
        checkPerformanceThreshold(event);

        // 在生產環境中，可以：
        // 1. 將指標寫入時序資料庫
        // 2. 更新監控儀表板
        // 3. 觸發自動擴展
        // 4. 發送警報

        logger.trace("Performance metric event processed from Kafka: {} = {} for page: {}",
                event.metricType(), event.value(), event.page());
    }

    /**
     * 處理頁面瀏覽事件
     * 
     * @param event 用戶行為分析事件
     */
    private void processPageViewEvent(UserBehaviorAnalyticsEvent event) {
        event.getPage().ifPresent(page -> {
            String pageKey = "page_views_" + page.replaceAll("[^a-zA-Z0-9]", "_");
            userBehaviorMetrics.computeIfAbsent(pageKey, k -> new AtomicLong(0)).incrementAndGet();
            logger.trace("Page view recorded for: {}", page);
        });
    }

    /**
     * 處理用戶操作事件
     * 
     * @param event 用戶行為分析事件
     */
    private void processUserActionEvent(UserBehaviorAnalyticsEvent event) {
        event.getAction().ifPresent(action -> {
            String actionKey = "user_actions_" + action.replaceAll("[^a-zA-Z0-9]", "_");
            userBehaviorMetrics.computeIfAbsent(actionKey, k -> new AtomicLong(0)).incrementAndGet();
            logger.trace("User action recorded: {}", action);
        });
    }

    /**
     * 處理業務事件
     * 
     * @param event 用戶行為分析事件
     */
    private void processBusinessEvent(UserBehaviorAnalyticsEvent event) {
        // 處理特定的業務事件，如購買、加入購物車等
        String businessEventKey = "business_events_" + event.getFrontendEventType();
        userBehaviorMetrics.computeIfAbsent(businessEventKey, k -> new AtomicLong(0)).incrementAndGet();
        logger.trace("Business event recorded: {}", event.getFrontendEventType());
    }

    /**
     * 檢查效能閾值並觸發警報
     * 
     * @param event 效能指標接收事件
     */
    private void checkPerformanceThreshold(PerformanceMetricReceivedEvent event) {
        if (event.exceedsRecommendedThreshold()) {
            String alertMessage = String.format(
                    "Performance threshold exceeded: %s = %.2f for page %s (threshold: %.2f)",
                    event.metricType(), event.value(), event.page(), getThreshold(event.metricType()));

            logger.warn(alertMessage);

            // 發送即時警報通知
            sendPerformanceAlert(event, alertMessage);
        }
    }

    /**
     * 獲取效能指標閾值
     * 
     * @param metricType 指標類型
     * @return 閾值
     */
    private double getThreshold(String metricType) {
        return switch (metricType) {
            case "lcp" -> LCP_THRESHOLD;
            case "fid" -> FID_THRESHOLD;
            case "cls" -> CLS_THRESHOLD;
            case "ttfb" -> TTFB_THRESHOLD;
            case "page_load" -> PAGE_LOAD_THRESHOLD;
            default -> 0.0;
        };
    }

    /**
     * 發送效能警報
     * 
     * @param event        效能指標事件
     * @param alertMessage 警報訊息
     */
    private void sendPerformanceAlert(PerformanceMetricReceivedEvent event, String alertMessage) {
        try {
            Map<String, Object> alert = Map.of(
                    "type", "performance_alert",
                    "metricType", event.metricType(),
                    "value", event.value(),
                    "page", event.page(),
                    "threshold", getThreshold(event.metricType()),
                    "message", alertMessage,
                    "timestamp", LocalDateTime.now());

            // 透過 WebSocket 發送即時警報
            messagingTemplate.convertAndSend("/topic/performance-alerts", alert);

            logger.debug("Performance alert sent via WebSocket: {}", alertMessage);

        } catch (Exception e) {
            logger.error("Failed to send performance alert: {}", alertMessage, e);
        }
    }

    /**
     * 更新即時指標並廣播
     * 
     * @param event 領域事件
     */
    private void updateRealTimeMetrics(Object event) {
        totalEventsProcessed.incrementAndGet();
        lastUpdateTimes.put("last_update", LocalDateTime.now());

        // 準備即時指標數據
        Map<String, Object> metricsUpdate = Map.of(
                "totalEvents", totalEventsProcessed.get(),
                "userBehaviorMetrics", getUserBehaviorMetricsSnapshot(),
                "performanceMetrics", getPerformanceMetricsSnapshot(),
                "lastUpdate", LocalDateTime.now());

        try {
            // 透過 WebSocket 廣播即時指標更新
            messagingTemplate.convertAndSend("/topic/analytics-updates", metricsUpdate);

            logger.trace("Real-time metrics update sent via WebSocket");

        } catch (Exception e) {
            logger.error("Failed to send real-time metrics update", e);
        }
    }

    /**
     * 獲取用戶行為指標快照
     * 
     * @return 用戶行為指標 Map
     */
    private Map<String, Long> getUserBehaviorMetricsSnapshot() {
        return userBehaviorMetrics.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get()));
    }

    /**
     * 獲取效能指標快照
     * 
     * @return 效能指標 Map
     */
    private Map<String, Object> getPerformanceMetricsSnapshot() {
        Map<String, Object> snapshot = new ConcurrentHashMap<>();

        // 添加計數器
        performanceMetrics.entrySet().forEach(entry -> snapshot.put(entry.getKey() + "_count", entry.getValue().get()));

        // 添加最新值
        performanceValues.entrySet().forEach(entry -> snapshot.put(entry.getKey() + "_value", entry.getValue()));

        return snapshot;
    }

    // === 監控和診斷方法 ===

    /**
     * 檢查是否為 Kafka 模式
     * 
     * @return 如果是 Kafka 模式返回 true
     */
    public boolean isKafkaMode() {
        return "kafka".equals(publisherType);
    }

    /**
     * 檢查是否為記憶體模式
     * 
     * @return 如果是記憶體模式返回 true
     */
    public boolean isInMemoryMode() {
        return "in-memory".equals(publisherType);
    }

    /**
     * 獲取當前發布器類型
     * 
     * @return 發布器類型
     */
    public String getPublisherType() {
        return publisherType;
    }

    /**
     * 獲取處理的事件總數
     * 
     * @return 事件總數
     */
    public long getTotalEventsProcessed() {
        return totalEventsProcessed.get();
    }

    /**
     * 獲取用戶行為指標
     * 
     * @return 用戶行為指標 Map
     */
    public Map<String, Long> getUserBehaviorMetrics() {
        return getUserBehaviorMetricsSnapshot();
    }

    /**
     * 獲取效能指標
     * 
     * @return 效能指標 Map
     */
    public Map<String, Object> getPerformanceMetrics() {
        return getPerformanceMetricsSnapshot();
    }

    /**
     * 獲取最後更新時間
     * 
     * @return 最後更新時間
     */
    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTimes.get("last_update");
    }

    /**
     * 清除所有指標 (主要用於測試)
     */
    public void clearMetrics() {
        userBehaviorMetrics.clear();
        performanceMetrics.clear();
        performanceValues.clear();
        lastUpdateTimes.clear();
        totalEventsProcessed.set(0);
        logger.debug("All analytics metrics cleared");
    }
}