package solid.humank.genaidemo.infrastructure.observability.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import solid.humank.genaidemo.domain.observability.events.PerformanceMetricReceivedEvent;
import solid.humank.genaidemo.domain.observability.events.UserBehaviorAnalyticsEvent;
import solid.humank.genaidemo.infrastructure.event.publisher.DeadLetterService;
import solid.humank.genaidemo.infrastructure.monitoring.AlertService;

/**
 * Profile-Aware 事件處理器，具備韌性機制
 * 
 * 功能特性：
 * - 環境差異化錯誤處理 (開發/測試 vs 生產)
 * - 斷路器模式支援
 * - 重試機制與指數退避
 * - 死信佇列整合
 * - 環境適應性警報
 * - 效能監控與指標收集
 * 
 * 設計原則：
 * - 根據環境配置調整韌性策略
 * - 整合現有 DeadLetterService
 * - 支援追蹤 ID 傳播
 * - 提供詳細的錯誤分類和處理
 * 
 * 需求: 2.2, 3.1, 3.2
 */
@Component
public class ProfileAwareEventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ProfileAwareEventProcessor.class);

    private final DeadLetterService deadLetterService;
    private final AlertService alertService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Value("${genai-demo.events.publisher:in-memory}")
    private String publisherType;

    @Value("${genai-demo.resilience.circuit-breaker.failure-rate-threshold:50}")
    private float failureRateThreshold;

    @Value("${genai-demo.resilience.circuit-breaker.wait-duration-in-open-state:30000}")
    private long waitDurationInOpenState;

    @Value("${genai-demo.resilience.circuit-breaker.sliding-window-size:10}")
    private int slidingWindowSize;

    // 韌性指標
    private final Map<String, AtomicLong> processingSuccessCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> processingFailureCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> circuitBreakerOpenCounters = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastFailureTimes = new ConcurrentHashMap<>();
    private final AtomicLong totalProcessingAttempts = new AtomicLong(0);

    // 斷路器實例
    private io.github.resilience4j.circuitbreaker.CircuitBreaker userBehaviorCircuitBreaker;
    private io.github.resilience4j.circuitbreaker.CircuitBreaker performanceMetricCircuitBreaker;

    public ProfileAwareEventProcessor(
            DeadLetterService deadLetterService,
            AlertService alertService,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.deadLetterService = deadLetterService;
        this.alertService = alertService;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    /**
     * 在所有依賴注入完成後初始化斷路器
     */
    @PostConstruct
    private void postConstruct() {
        initializeCircuitBreakers();
        logger.info("ProfileAwareEventProcessor initialized with publisher type: {} and resilience patterns enabled",
                publisherType);
    }

    /**
     * 初始化斷路器配置
     */
    private void initializeCircuitBreakers() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(java.time.Duration.ofMillis(waitDurationInOpenState))
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();

        userBehaviorCircuitBreaker = circuitBreakerRegistry.circuitBreaker("userBehaviorProcessor", config);
        performanceMetricCircuitBreaker = circuitBreakerRegistry.circuitBreaker("performanceMetricProcessor", config);

        // 註冊斷路器事件監聽器
        userBehaviorCircuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    String correlationId = MDC.get("correlationId");
                    logger.info("Circuit breaker state transition for userBehavior: {} -> {} [correlationId: {}]",
                            event.getStateTransition().getFromState(), event.getStateTransition().getToState(),
                            correlationId);

                    if (event.getStateTransition()
                            .getToState() == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN) {
                        circuitBreakerOpenCounters.computeIfAbsent("userBehavior", k -> new AtomicInteger(0))
                                .incrementAndGet();
                        triggerEnvironmentSpecificAlert(AlertType.CIRCUIT_BREAKER_OPENED,
                                Map.of("processor", "userBehavior", "environment", publisherType));
                    }
                });

        performanceMetricCircuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    String correlationId = MDC.get("correlationId");
                    logger.info("Circuit breaker state transition for performanceMetric: {} -> {} [correlationId: {}]",
                            event.getStateTransition().getFromState(), event.getStateTransition().getToState(),
                            correlationId);

                    if (event.getStateTransition()
                            .getToState() == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN) {
                        circuitBreakerOpenCounters.computeIfAbsent("performanceMetric", k -> new AtomicInteger(0))
                                .incrementAndGet();
                        triggerEnvironmentSpecificAlert(AlertType.CIRCUIT_BREAKER_OPENED,
                                Map.of("processor", "performanceMetric", "environment", publisherType));
                    }
                });

        logger.info(
                "Circuit breakers initialized with failure rate threshold: {}%, wait duration: {}ms, sliding window: {}",
                failureRateThreshold, waitDurationInOpenState, slidingWindowSize);
    }

    /**
     * 處理用戶行為分析事件 (具備韌性機制)
     * 
     * @param event 用戶行為分析事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(value = { TransientException.class,
            ProcessingException.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public void processUserBehaviorAnalyticsEvent(UserBehaviorAnalyticsEvent event) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = event.traceId();
            MDC.put("correlationId", correlationId);
        }

        totalProcessingAttempts.incrementAndGet();

        logger.debug("Processing UserBehaviorAnalyticsEvent with resilience: {} in {} mode [correlationId: {}]",
                event.getFrontendEventType(), publisherType, correlationId);

        try {
            // 使用斷路器保護處理邏輯
            userBehaviorCircuitBreaker.executeSupplier(() -> {
                if ("kafka".equals(publisherType)) {
                    // 生產環境：MSK 處理
                    return processUserBehaviorEventWithKafka(event);
                } else {
                    // 開發/測試環境：記憶體處理
                    return processUserBehaviorEventInMemory(event);
                }
            });

            // 記錄成功處理
            onProcessingSuccess("userBehavior", event.getEventType());

            logger.debug(
                    "UserBehaviorAnalyticsEvent processed successfully with resilience in {} mode [correlationId: {}]",
                    publisherType, correlationId);

        } catch (Exception e) {
            logger.error("Failed to process UserBehaviorAnalyticsEvent: {} in {} mode [correlationId: {}]",
                    event.getFrontendEventType(), publisherType, correlationId, e);

            // 記錄處理失敗
            onProcessingFailure("userBehavior", event.getEventType(), e);

            // 根據異常類型決定是否重試
            if (isRetryableException(e)) {
                throw new TransientException("Transient error processing user behavior event", e);
            } else {
                throw new ProcessingException("Non-retryable error processing user behavior event", e);
            }
        }
    }

    /**
     * 處理效能指標接收事件 (具備韌性機制)
     * 
     * @param event 效能指標接收事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(value = { TransientException.class,
            ProcessingException.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public void processPerformanceMetricReceivedEvent(PerformanceMetricReceivedEvent event) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = event.traceId();
            MDC.put("correlationId", correlationId);
        }

        totalProcessingAttempts.incrementAndGet();

        logger.debug("Processing PerformanceMetricReceivedEvent with resilience: {} in {} mode [correlationId: {}]",
                event.metricType(), publisherType, correlationId);

        try {
            // 使用斷路器保護處理邏輯
            performanceMetricCircuitBreaker.executeSupplier(() -> {
                if ("kafka".equals(publisherType)) {
                    // 生產環境：MSK 處理
                    return processPerformanceMetricEventWithKafka(event);
                } else {
                    // 開發/測試環境：記憶體處理
                    return processPerformanceMetricEventInMemory(event);
                }
            });

            // 記錄成功處理
            onProcessingSuccess("performanceMetric", event.getEventType());

            logger.debug(
                    "PerformanceMetricReceivedEvent processed successfully with resilience in {} mode [correlationId: {}]",
                    publisherType, correlationId);

        } catch (Exception e) {
            logger.error("Failed to process PerformanceMetricReceivedEvent: {} in {} mode [correlationId: {}]",
                    event.metricType(), publisherType, correlationId, e);

            // 記錄處理失敗
            onProcessingFailure("performanceMetric", event.getEventType(), e);

            // 根據異常類型決定是否重試
            if (isRetryableException(e)) {
                throw new TransientException("Transient error processing performance metric event", e);
            } else {
                throw new ProcessingException("Non-retryable error processing performance metric event", e);
            }
        }
    }

    /**
     * 批量處理可觀測性事件 (具備韌性機制)
     * 
     * @param events 事件列表
     */
    @Retryable(value = {
            TransientException.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 15000))
    public void processBatchObservabilityEvents(List<Object> events) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = "batch_" + System.currentTimeMillis();
            MDC.put("correlationId", correlationId);
        }

        logger.debug("Processing batch of {} observability events in {} mode [correlationId: {}]",
                events.size(), publisherType, correlationId);

        try {
            for (Object event : events) {
                if (event instanceof UserBehaviorAnalyticsEvent userEvent) {
                    processUserBehaviorAnalyticsEvent(userEvent);
                } else if (event instanceof PerformanceMetricReceivedEvent perfEvent) {
                    processPerformanceMetricReceivedEvent(perfEvent);
                }
            }

            logger.debug("Batch processing completed successfully for {} events [correlationId: {}]",
                    events.size(), correlationId);

        } catch (Exception e) {
            logger.error("Failed to process batch of {} events [correlationId: {}]", events.size(), correlationId, e);
            throw new TransientException("Batch processing failed", e);
        }
    }

    /**
     * 用戶行為事件恢復處理 (重試失敗後的處理)
     * 
     * @param ex    異常
     * @param event 用戶行為分析事件
     */
    @Recover
    public void recoverFromUserBehaviorEventProcessingFailure(Exception ex, UserBehaviorAnalyticsEvent event) {
        String correlationId = MDC.get("correlationId");

        logger.error("All retry attempts failed for UserBehaviorAnalyticsEvent: {} [correlationId: {}]",
                event.getFrontendEventType(), correlationId, ex);

        if ("kafka".equals(publisherType)) {
            // 生產環境：發送到 MSK DLQ
            deadLetterService.sendToDeadLetter(event, ex);
        } else {
            // 開發環境：記錄到檔案或記憶體
            handleDevelopmentEnvironmentFailure(event, ex);
        }

        // 觸發環境適當的警報
        triggerEnvironmentSpecificAlert(AlertType.USER_BEHAVIOR_EVENT_PROCESSING_FAILURE,
                Map.of("eventType", event.getFrontendEventType(), "error", ex.getMessage(), "environment",
                        publisherType));
    }

    /**
     * 效能指標事件恢復處理 (重試失敗後的處理)
     * 
     * @param ex    異常
     * @param event 效能指標接收事件
     */
    @Recover
    public void recoverFromPerformanceMetricEventProcessingFailure(Exception ex, PerformanceMetricReceivedEvent event) {
        String correlationId = MDC.get("correlationId");

        logger.error("All retry attempts failed for PerformanceMetricReceivedEvent: {} [correlationId: {}]",
                event.metricType(), correlationId, ex);

        if ("kafka".equals(publisherType)) {
            // 生產環境：發送到 MSK DLQ
            deadLetterService.sendToDeadLetter(event, ex);
        } else {
            // 開發環境：記錄到檔案或記憶體
            handleDevelopmentEnvironmentFailure(event, ex);
        }

        // 觸發環境適當的警報
        triggerEnvironmentSpecificAlert(AlertType.PERFORMANCE_METRIC_EVENT_PROCESSING_FAILURE,
                Map.of("eventType", event.metricType(), "error", ex.getMessage(), "environment", publisherType));
    }

    /**
     * 批量事件恢復處理
     * 
     * @param ex     異常
     * @param events 事件列表
     */
    @Recover
    public void recoverFromBatchEventProcessingFailure(Exception ex, List<Object> events) {
        String correlationId = MDC.get("correlationId");

        logger.error("All retry attempts failed for batch processing of {} events [correlationId: {}]",
                events.size(), correlationId, ex);

        // 逐個處理失敗的事件
        for (Object event : events) {
            try {
                if (event instanceof UserBehaviorAnalyticsEvent userEvent) {
                    recoverFromUserBehaviorEventProcessingFailure(ex, userEvent);
                } else if (event instanceof PerformanceMetricReceivedEvent perfEvent) {
                    recoverFromPerformanceMetricEventProcessingFailure(ex, perfEvent);
                }
            } catch (Exception recoveryEx) {
                logger.error("Failed to recover individual event from batch [correlationId: {}]", correlationId,
                        recoveryEx);
            }
        }

        // 觸發批量處理失敗警報
        triggerEnvironmentSpecificAlert(AlertType.BATCH_EVENT_PROCESSING_FAILURE,
                Map.of("batchSize", events.size(), "error", ex.getMessage(), "environment", publisherType));
    }

    // === 私有處理方法 ===

    /**
     * 使用 Kafka 處理用戶行為事件 (生產環境)
     */
    private Boolean processUserBehaviorEventWithKafka(UserBehaviorAnalyticsEvent event) {
        String correlationId = MDC.get("correlationId");
        logger.debug("Processing user behavior event with Kafka: {} [correlationId: {}]",
                event.getFrontendEventType(), correlationId);

        // 模擬 Kafka 處理邏輯
        // 在實際實現中，這裡會調用 Kafka 發布邏輯
        simulateKafkaProcessing(event);

        return true;
    }

    /**
     * 記憶體處理用戶行為事件 (開發/測試環境)
     */
    private Boolean processUserBehaviorEventInMemory(UserBehaviorAnalyticsEvent event) {
        String correlationId = MDC.get("correlationId");
        logger.debug("Processing user behavior event in memory: {} [correlationId: {}]",
                event.getFrontendEventType(), correlationId);

        // 開發環境的簡化處理邏輯
        simulateInMemoryProcessing(event);

        return true;
    }

    /**
     * 使用 Kafka 處理效能指標事件 (生產環境)
     */
    private Boolean processPerformanceMetricEventWithKafka(PerformanceMetricReceivedEvent event) {
        String correlationId = MDC.get("correlationId");
        logger.debug("Processing performance metric event with Kafka: {} [correlationId: {}]",
                event.metricType(), correlationId);

        // 模擬 Kafka 處理邏輯
        simulateKafkaProcessing(event);

        return true;
    }

    /**
     * 記憶體處理效能指標事件 (開發/測試環境)
     */
    private Boolean processPerformanceMetricEventInMemory(PerformanceMetricReceivedEvent event) {
        String correlationId = MDC.get("correlationId");
        logger.debug("Processing performance metric event in memory: {} [correlationId: {}]",
                event.metricType(), correlationId);

        // 開發環境的簡化處理邏輯
        simulateInMemoryProcessing(event);

        return true;
    }

    /**
     * 模擬 Kafka 處理 (生產環境)
     */
    private void simulateKafkaProcessing(Object event) {
        // 在實際實現中，這裡會包含：
        // 1. Kafka 消息發布
        // 2. 分散式追蹤
        // 3. 持久化處理
        // 4. 監控指標更新

        // 模擬處理時間
        try {
            Thread.sleep(10); // 模擬 Kafka 發布延遲
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessingException("Processing interrupted", e);
        }
    }

    /**
     * 模擬記憶體處理 (開發/測試環境)
     */
    private void simulateInMemoryProcessing(Object event) {
        // 在實際實現中，這裡會包含：
        // 1. 記憶體統計更新
        // 2. 開發環境通知
        // 3. 本地儀表板更新

        // 模擬處理時間
        try {
            Thread.sleep(5); // 模擬記憶體處理延遲
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessingException("Processing interrupted", e);
        }
    }

    // === 韌性機制輔助方法 ===

    /**
     * 判斷異常是否可重試
     */
    private boolean isRetryableException(Exception e) {
        // 網路相關異常通常可重試
        if (e instanceof java.net.ConnectException ||
                e instanceof java.net.SocketTimeoutException ||
                e instanceof org.springframework.dao.TransientDataAccessException) {
            return true;
        }

        // 包含特定關鍵字的異常可重試
        String message = e.getMessage();
        if (message != null) {
            return message.contains("timeout") ||
                    message.contains("connection") ||
                    message.contains("temporary") ||
                    message.contains("transient");
        }

        return false;
    }

    /**
     * 處理開發環境失敗
     */
    private void handleDevelopmentEnvironmentFailure(Object event, Exception ex) {
        String correlationId = MDC.get("correlationId");

        // 在開發環境中，我們可以：
        // 1. 記錄到本地檔案
        // 2. 儲存到記憶體佇列
        // 3. 顯示在開發控制台

        logger.warn("Development environment failure handling for event: {} [correlationId: {}]",
                event.getClass().getSimpleName(), correlationId, ex);

        // 簡化的開發環境錯誤處理
        // 在實際實現中可以擴展為更完整的本地錯誤處理機制
    }

    /**
     * 觸發環境特定警報
     */
    private void triggerEnvironmentSpecificAlert(AlertType alertType, Map<String, Object> context) {
        try {
            if ("kafka".equals(publisherType)) {
                // 生產環境：觸發正式警報系統
                alertService.triggerAlert(alertType, context);
            } else {
                // 開發環境：記錄警報到日誌
                logger.warn("Development alert triggered: {} with context: {}", alertType, context);
            }
        } catch (Exception e) {
            logger.error("Failed to trigger environment-specific alert: {}", alertType, e);
        }
    }

    /**
     * 記錄處理成功
     */
    private void onProcessingSuccess(String processorType, String eventType) {
        String key = processorType + "_" + eventType;
        processingSuccessCounters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();

        logger.trace("Processing success recorded for: {}", key);
    }

    /**
     * 記錄處理失敗
     */
    private void onProcessingFailure(String processorType, String eventType, Exception e) {
        String key = processorType + "_" + eventType;
        processingFailureCounters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        lastFailureTimes.put(key, LocalDateTime.now());

        logger.debug("Processing failure recorded for: {} - {}", key, e.getMessage());
    }

    // === 監控和診斷方法 ===

    /**
     * 獲取處理成功次數
     */
    public long getProcessingSuccessCount(String processorType, String eventType) {
        String key = processorType + "_" + eventType;
        return processingSuccessCounters.getOrDefault(key, new AtomicLong(0)).get();
    }

    /**
     * 獲取處理失敗次數
     */
    public long getProcessingFailureCount(String processorType, String eventType) {
        String key = processorType + "_" + eventType;
        return processingFailureCounters.getOrDefault(key, new AtomicLong(0)).get();
    }

    /**
     * 獲取斷路器開啟次數
     */
    public int getCircuitBreakerOpenCount(String processorName) {
        return circuitBreakerOpenCounters.getOrDefault(processorName, new AtomicInteger(0)).get();
    }

    /**
     * 獲取總處理嘗試次數
     */
    public long getTotalProcessingAttempts() {
        return totalProcessingAttempts.get();
    }

    /**
     * 獲取斷路器狀態
     */
    public io.github.resilience4j.circuitbreaker.CircuitBreaker.State getCircuitBreakerState(String processorName) {
        switch (processorName) {
            case "userBehavior":
                return userBehaviorCircuitBreaker.getState();
            case "performanceMetric":
                return performanceMetricCircuitBreaker.getState();
            default:
                throw new IllegalArgumentException("Unknown processor: " + processorName);
        }
    }

    /**
     * 重置斷路器
     */
    public void resetCircuitBreaker(String processorName) {
        switch (processorName) {
            case "userBehavior":
                userBehaviorCircuitBreaker.reset();
                break;
            case "performanceMetric":
                performanceMetricCircuitBreaker.reset();
                break;
            default:
                throw new IllegalArgumentException("Unknown processor: " + processorName);
        }

        logger.info("Circuit breaker reset for processor: {}", processorName);
    }

    /**
     * 清除所有統計數據 (主要用於測試)
     */
    public void clearStatistics() {
        processingSuccessCounters.clear();
        processingFailureCounters.clear();
        circuitBreakerOpenCounters.clear();
        lastFailureTimes.clear();
        totalProcessingAttempts.set(0);

        logger.debug("Cleared all processing statistics");
    }

    // === 異常類別定義 ===

    /**
     * 暫時性異常 (可重試)
     */
    public static class TransientException extends RuntimeException {
        public TransientException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 處理異常 (可能可重試，取決於具體情況)
     */
    public static class ProcessingException extends RuntimeException {
        public ProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 警報類型枚舉
     */
    public enum AlertType {
        USER_BEHAVIOR_EVENT_PROCESSING_FAILURE,
        PERFORMANCE_METRIC_EVENT_PROCESSING_FAILURE,
        BATCH_EVENT_PROCESSING_FAILURE,
        CIRCUIT_BREAKER_OPENED
    }
}