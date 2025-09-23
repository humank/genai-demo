package solid.humank.genaidemo.infrastructure.observability.event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.observability.events.PerformanceMetricReceivedEvent;
import solid.humank.genaidemo.domain.observability.events.UserBehaviorAnalyticsEvent;
import solid.humank.genaidemo.infrastructure.event.publisher.DomainEventPublisherAdapter;

/**
 * Profile-Aware 可觀測性事件發布器
 * 
 * 根據環境配置自動選擇事件處理策略：
 * - 開發/測試環境：記憶體處理，支援快速開發和測試
 * - 生產環境：MSK 處理，支援分散式處理和持久化
 * 
 * 設計原則：
 * - 環境差異化處理，無需修改業務代碼
 * - 整合現有 DDD 事件系統
 * - 支援追蹤 ID 傳播和 MDC 整合
 * - 提供開發和生產環境的指標收集
 * 
 * 需求: 2.2, 3.1, 3.2
 */
@Component
public class ObservabilityEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ObservabilityEventPublisher.class);

    private final ApplicationEventPublisher springEventPublisher;

    @Value("${genai-demo.events.publisher:in-memory}")
    private String publisherType;

    // 指標收集
    private final Map<String, AtomicLong> eventCounters = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastEventTimes = new ConcurrentHashMap<>();
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);

    public ObservabilityEventPublisher(ApplicationEventPublisher springEventPublisher) {
        this.springEventPublisher = springEventPublisher;
        logger.info("ObservabilityEventPublisher initialized with publisher type: {}", publisherType);
    }

    /**
     * 處理用戶行為分析事件
     * 根據環境配置選擇處理策略
     * 
     * @param event 用戶行為分析事件
     */
    @EventListener
    public void handleUserBehaviorAnalyticsEvent(UserBehaviorAnalyticsEvent event) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = event.traceId();
            MDC.put("correlationId", correlationId);
        }

        logger.debug("Processing UserBehaviorAnalyticsEvent: {} with trace: {} in {} mode [correlationId: {}]",
                event.getFrontendEventType(), event.traceId(), publisherType, correlationId);

        try {
            // 更新指標
            updateEventMetrics(event.getEventType());

            if ("kafka".equals(publisherType)) {
                // 生產環境：發布到 MSK
                publishToKafka(event);
            } else {
                // 開發/測試環境：記憶體處理
                processInMemory(event);
            }

            logger.debug("UserBehaviorAnalyticsEvent processed successfully in {} mode [correlationId: {}]",
                    publisherType, correlationId);

        } catch (Exception e) {
            logger.error("Failed to process UserBehaviorAnalyticsEvent: {} in {} mode [correlationId: {}]",
                    event.getFrontendEventType(), publisherType, correlationId, e);
            throw new ObservabilityEventProcessingException("Failed to process user behavior analytics event", e);
        }
    }

    /**
     * 處理效能指標接收事件
     * 根據環境配置選擇處理策略
     * 
     * @param event 效能指標接收事件
     */
    @EventListener
    public void handlePerformanceMetricReceivedEvent(PerformanceMetricReceivedEvent event) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = event.traceId();
            MDC.put("correlationId", correlationId);
        }

        logger.debug("Processing PerformanceMetricReceivedEvent: {} with value: {} in {} mode [correlationId: {}]",
                event.metricType(), event.value(), publisherType, correlationId);

        try {
            // 更新指標
            updateEventMetrics(event.getEventType());

            if ("kafka".equals(publisherType)) {
                // 生產環境：發布到 MSK
                publishToKafka(event);
            } else {
                // 開發/測試環境：記憶體處理
                processInMemory(event);
            }

            logger.debug("PerformanceMetricReceivedEvent processed successfully in {} mode [correlationId: {}]",
                    publisherType, correlationId);

        } catch (Exception e) {
            logger.error("Failed to process PerformanceMetricReceivedEvent: {} in {} mode [correlationId: {}]",
                    event.metricType(), publisherType, correlationId, e);
            throw new ObservabilityEventProcessingException("Failed to process performance metric event", e);
        }
    }

    /**
     * 發布事件到 MSK (生產環境)
     * 
     * @param event 領域事件
     */
    private void publishToKafka(Object event) {
        String correlationId = MDC.get("correlationId");
        logger.debug("Publishing event to MSK: {} [correlationId: {}]", event.getClass().getSimpleName(),
                correlationId);

        // 包裝事件並透過 Spring 事件系統發布到 MSK
        // 現有的 KafkaDomainEventPublisher 會處理實際的 Kafka 發布
        if (event instanceof UserBehaviorAnalyticsEvent userEvent) {
            DomainEventPublisherAdapter.DomainEventWrapper wrapper = new DomainEventPublisherAdapter.DomainEventWrapper(
                    userEvent);
            springEventPublisher.publishEvent(wrapper);
        } else if (event instanceof PerformanceMetricReceivedEvent perfEvent) {
            DomainEventPublisherAdapter.DomainEventWrapper wrapper = new DomainEventPublisherAdapter.DomainEventWrapper(
                    perfEvent);
            springEventPublisher.publishEvent(wrapper);
        }

        logger.debug("Event published to MSK successfully [correlationId: {}]", correlationId);
    }

    /**
     * 記憶體處理事件 (開發/測試環境)
     * 
     * @param event 領域事件
     */
    private void processInMemory(Object event) {
        String correlationId = MDC.get("correlationId");
        logger.debug("Processing event in memory: {} [correlationId: {}]", event.getClass().getSimpleName(),
                correlationId);

        // 在記憶體中處理事件，適合開發和測試
        if (event instanceof UserBehaviorAnalyticsEvent userEvent) {
            processUserBehaviorEventInMemory(userEvent);
        } else if (event instanceof PerformanceMetricReceivedEvent perfEvent) {
            processPerformanceMetricEventInMemory(perfEvent);
        }

        logger.debug("Event processed in memory successfully [correlationId: {}]", correlationId);
    }

    /**
     * 記憶體處理用戶行為事件
     * 
     * @param event 用戶行為分析事件
     */
    private void processUserBehaviorEventInMemory(UserBehaviorAnalyticsEvent event) {
        String correlationId = MDC.get("correlationId");

        // 開發環境的簡化處理邏輯
        logger.debug("Processing user behavior event in memory: type={}, session={}, user={} [correlationId: {}]",
                event.getFrontendEventType(), event.sessionId(),
                event.userId().orElse("anonymous"), correlationId);

        // 在開發環境中，我們可以：
        // 1. 更新記憶體中的統計數據
        // 2. 觸發開發環境的通知
        // 3. 更新開發儀表板
        updateInMemoryUserBehaviorStats(event);
    }

    /**
     * 記憶體處理效能指標事件
     * 
     * @param event 效能指標接收事件
     */
    private void processPerformanceMetricEventInMemory(PerformanceMetricReceivedEvent event) {
        String correlationId = MDC.get("correlationId");

        // 開發環境的簡化處理邏輯
        logger.debug("Processing performance metric event in memory: type={}, value={}, page={} [correlationId: {}]",
                event.metricType(), event.value(), event.page(), correlationId);

        // 在開發環境中，我們可以：
        // 1. 檢查效能閾值
        // 2. 更新記憶體中的效能統計
        // 3. 觸發開發環境的警報
        updateInMemoryPerformanceStats(event);

        // 檢查是否超過建議閾值
        if (event.exceedsRecommendedThreshold()) {
            logger.warn("Performance metric exceeds recommended threshold: {} = {} for page {} [correlationId: {}]",
                    event.metricType(), event.value(), event.page(), correlationId);
        }
    }

    /**
     * 更新記憶體中的用戶行為統計
     * 
     * @param event 用戶行為分析事件
     */
    private void updateInMemoryUserBehaviorStats(UserBehaviorAnalyticsEvent event) {
        // 在開發環境中更新簡單的統計數據
        String statsKey = "user_behavior_" + event.getFrontendEventType();
        eventCounters.computeIfAbsent(statsKey, k -> new AtomicLong(0)).incrementAndGet();

        logger.trace("Updated user behavior stats: {} = {}",
                statsKey, eventCounters.get(statsKey).get());
    }

    /**
     * 更新記憶體中的效能統計
     * 
     * @param event 效能指標接收事件
     */
    private void updateInMemoryPerformanceStats(PerformanceMetricReceivedEvent event) {
        // 在開發環境中更新簡單的效能統計
        String statsKey = "performance_" + event.metricType();
        eventCounters.computeIfAbsent(statsKey, k -> new AtomicLong(0)).incrementAndGet();

        logger.trace("Updated performance stats: {} = {}",
                statsKey, eventCounters.get(statsKey).get());
    }

    /**
     * 更新事件指標
     * 
     * @param eventType 事件類型
     */
    private void updateEventMetrics(String eventType) {
        eventCounters.computeIfAbsent(eventType, k -> new AtomicLong(0)).incrementAndGet();
        lastEventTimes.put(eventType, LocalDateTime.now());
        totalEventsProcessed.incrementAndGet();

        logger.trace("Event metrics updated - Type: {}, Count: {}, Total: {}",
                eventType, eventCounters.get(eventType).get(), totalEventsProcessed.get());
    }

    // === 監控和診斷方法 ===

    /**
     * 獲取當前發布器類型
     * 
     * @return 發布器類型 (in-memory 或 kafka)
     */
    public String getPublisherType() {
        return publisherType;
    }

    /**
     * 獲取事件處理總數
     * 
     * @return 處理的事件總數
     */
    public long getTotalEventsProcessed() {
        return totalEventsProcessed.get();
    }

    /**
     * 獲取特定事件類型的處理次數
     * 
     * @param eventType 事件類型
     * @return 處理次數
     */
    public long getEventCount(String eventType) {
        return eventCounters.getOrDefault(eventType, new AtomicLong(0)).get();
    }

    /**
     * 獲取特定事件類型的最後處理時間
     * 
     * @param eventType 事件類型
     * @return 最後處理時間
     */
    public LocalDateTime getLastEventTime(String eventType) {
        return lastEventTimes.get(eventType);
    }

    /**
     * 獲取所有事件統計
     * 
     * @return 事件統計 Map
     */
    public Map<String, Long> getEventStatistics() {
        return eventCounters.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get()));
    }

    /**
     * 清除所有統計數據 (主要用於測試)
     */
    public void clearStatistics() {
        eventCounters.clear();
        lastEventTimes.clear();
        totalEventsProcessed.set(0);
        logger.debug("Cleared all event statistics");
    }

    /**
     * 檢查發布器是否為記憶體模式
     * 
     * @return 如果是記憶體模式返回 true
     */
    public boolean isInMemoryMode() {
        return "in-memory".equals(publisherType);
    }

    /**
     * 檢查發布器是否為 Kafka 模式
     * 
     * @return 如果是 Kafka 模式返回 true
     */
    public boolean isKafkaMode() {
        return "kafka".equals(publisherType);
    }

    /**
     * 可觀測性事件處理異常
     */
    public static class ObservabilityEventProcessingException extends RuntimeException {
        public ObservabilityEventProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}