package solid.humank.genaidemo.infrastructure.event.handler;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.infrastructure.event.EventProcessingException;
import solid.humank.genaidemo.infrastructure.event.EventProcessingTimeoutException;
import solid.humank.genaidemo.infrastructure.event.EventSequenceException;
import solid.humank.genaidemo.infrastructure.event.backpressure.BackpressureManager;
import solid.humank.genaidemo.infrastructure.event.monitoring.EventProcessingContext;
import solid.humank.genaidemo.infrastructure.event.monitoring.EventProcessingMonitor;
import solid.humank.genaidemo.infrastructure.event.retry.EventRetryManager;
import solid.humank.genaidemo.infrastructure.event.retry.RetryPolicy;
import solid.humank.genaidemo.infrastructure.event.sequence.EventSequenceTracker;

/**
 * 彈性事件處理器
 * 整合所有錯誤處理和監控機制的統一事件處理入口
 * 
 * 需求 9.1: 實現事件處理失敗的錯誤記錄和重試機制
 * 需求 9.2: 建立系統異常的適當錯誤回應和日誌記錄
 * 需求 9.3: 實現事件處理超時的檢測和處理機制
 * 需求 9.4: 建立系統負載過高時的背壓機制
 * 需求 9.5: 實現事件順序錯亂的檢測和處理機制
 */
@Component
public class ResilientEventHandler {    private static final Logger logger = LoggerFactory.getLogger(ResilientEventHandler.class);

    private final EventProcessingMonitor monitor;
    private final EventRetryManager retryManager;
    private final BackpressureManager backpressureManager;
    private final EventSequenceTracker sequenceTracker;

    // 默認配置
    private final Duration defaultTimeout = Duration.ofSeconds(30);
    private final RetryPolicy defaultRetryPolicy = RetryPolicy.defaultPolicy();

    public ResilientEventHandler(EventProcessingMonitor monitor,
            EventRetryManager retryManager,
            BackpressureManager backpressureManager,
            EventSequenceTracker sequenceTracker) {
        this.monitor = monitor;
        this.retryManager = retryManager;
        this.backpressureManager = backpressureManager;
        this.sequenceTracker = sequenceTracker;
    }

    /**
     * 處理事件（使用默認配置）
     */
    public CompletableFuture<Void> handleEvent(DomainEvent event, String handlerName,
            Consumer<DomainEvent> eventHandler) {
        return handleEvent(event, handlerName, eventHandler, defaultTimeout, defaultRetryPolicy);
    }

    /**
     * 處理事件（完整配置）
     */
    public CompletableFuture<Void> handleEvent(DomainEvent event, String handlerName,
            Consumer<DomainEvent> eventHandler,
            Duration timeout, RetryPolicy retryPolicy) {

        // 1. 檢查背壓狀態
        BackpressureManager.BackpressureDecision decision = backpressureManager.shouldProcessEvent();

        switch (decision) {
            case REJECT:
                logger.warn("Event rejected due to backpressure: {} with handler: {}",
                        event.getEventType(), handlerName);
                return CompletableFuture.failedFuture(
                        new EventProcessingException("Event rejected due to system overload",
                                event, handlerName));

            case DELAY:
                Duration delay = backpressureManager.getSuggestedDelay();
                logger.info("Event delayed due to backpressure: {} with handler: {} (delay: {}ms)",
                        event.getEventType(), handlerName, delay.toMillis());

                return CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(delay.toMillis());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Event processing interrupted", e);
                    }
                }).thenCompose(v -> processEventInternal(event, handlerName, eventHandler, timeout, retryPolicy));

            case PROCEED:
            default:
                return processEventInternal(event, handlerName, eventHandler, timeout, retryPolicy);
        }
    }

    private CompletableFuture<Void> processEventInternal(DomainEvent event, String handlerName,
            Consumer<DomainEvent> eventHandler,
            Duration timeout, RetryPolicy retryPolicy) {

        // 2. 驗證事件順序
        EventSequenceTracker.EventSequenceValidation sequenceValidation = sequenceTracker.validateEventSequence(event);

        if (sequenceValidation.isDuplicate()) {
            logger.info("Duplicate event ignored: {} with handler: {}",
                    event.getEventType(), handlerName);
            return CompletableFuture.completedFuture(null);
        }

        if (sequenceValidation.isOutOfOrder()) {
            logger.error("Out-of-order event detected: {} with handler: {}",
                    event.getEventType(), handlerName);

            EventSequenceException sequenceException = new EventSequenceException(
                    "Event sequence disorder detected",
                    event, handlerName,
                    event.getAggregateId(),
                    sequenceValidation.getRecord().getExpectedSequence(),
                    sequenceValidation.getRecord().getActualSequence());

            return CompletableFuture.failedFuture(sequenceException);
        }

        // 3. 開始監控
        EventProcessingContext context = monitor.startMonitoring(event, handlerName);
        backpressureManager.startProcessing();

        // 4. 執行帶重試和超時的事件處理
        return executeWithTimeoutAndRetry(event, handlerName, eventHandler, timeout, retryPolicy, context);
    }

    private CompletableFuture<Void> executeWithTimeoutAndRetry(DomainEvent event, String handlerName,
            Consumer<DomainEvent> eventHandler,
            Duration timeout, RetryPolicy retryPolicy,
            EventProcessingContext context) {

        // 創建帶超時的處理任務
        CompletableFuture<Void> processingFuture = retryManager.executeWithRetry(
                event, handlerName, eventHandler, retryPolicy);

        // 添加超時處理
        CompletableFuture<Void> timeoutFuture = processingFuture
                .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .handle((result, throwable) -> {
                    context.markCompleted();
                    backpressureManager.completeProcessing();

                    if (throwable == null) {
                        // 成功處理
                        monitor.completeMonitoring(context, true, null);
                        logger.debug("Event processed successfully: {} with handler: {} in {}ms",
                                event.getEventType(), handlerName, context.getProcessingTime().toMillis());
                        return null;

                    } else if (throwable instanceof TimeoutException) {
                        // 超時處理
                        EventProcessingTimeoutException timeoutException = new EventProcessingTimeoutException(
                                "Event processing timed out",
                                event, handlerName,
                                timeout.toMillis(),
                                context.getProcessingTime().toMillis(),
                                throwable);

                        monitor.recordTimeout(context, timeout);
                        logger.error("Event processing timed out: {} with handler: {} after {}ms",
                                event.getEventType(), handlerName, context.getProcessingTime().toMillis());

                        throw new RuntimeException(timeoutException);

                    } else {
                        // 其他錯誤
                        monitor.completeMonitoring(context, false, throwable);
                        logger.error("Event processing failed: {} with handler: {} after {}ms, error: {}",
                                event.getEventType(), handlerName, context.getProcessingTime().toMillis(),
                                throwable.getMessage());

                        if (throwable instanceof RuntimeException) {
                            throw (RuntimeException) throwable;
                        } else {
                            throw new RuntimeException(throwable);
                        }
                    }
                });

        return timeoutFuture;
    }

    /**
     * 獲取系統健康狀態
     */
    public SystemHealthStatus getSystemHealth() {
        EventProcessingMonitor.ProcessingStatistics processingStats = monitor.getStatistics();
        EventRetryManager.RetryStatistics retryStats = retryManager.getRetryStatistics();
        BackpressureManager.BackpressureStatus backpressureStatus = backpressureManager.getStatus();
        EventSequenceTracker.SequenceTrackingStatistics sequenceStats = sequenceTracker.getStatistics();

        return new SystemHealthStatus(processingStats, retryStats, backpressureStatus, sequenceStats);
    }

    /**
     * 系統健康狀態
     */
    public static class SystemHealthStatus {
        private final EventProcessingMonitor.ProcessingStatistics processingStats;
        private final EventRetryManager.RetryStatistics retryStats;
        private final BackpressureManager.BackpressureStatus backpressureStatus;
        private final EventSequenceTracker.SequenceTrackingStatistics sequenceStats;

        public SystemHealthStatus(EventProcessingMonitor.ProcessingStatistics processingStats,
                EventRetryManager.RetryStatistics retryStats,
                BackpressureManager.BackpressureStatus backpressureStatus,
                EventSequenceTracker.SequenceTrackingStatistics sequenceStats) {
            this.processingStats = processingStats;
            this.retryStats = retryStats;
            this.backpressureStatus = backpressureStatus;
            this.sequenceStats = sequenceStats;
        }

        public EventProcessingMonitor.ProcessingStatistics getProcessingStats() {
            return processingStats;
        }

        public EventRetryManager.RetryStatistics getRetryStats() {
            return retryStats;
        }

        public BackpressureManager.BackpressureStatus getBackpressureStatus() {
            return backpressureStatus;
        }

        public EventSequenceTracker.SequenceTrackingStatistics getSequenceStats() {
            return sequenceStats;
        }

        /**
         * 計算整體健康分數 (0-100)
         */
        public int getHealthScore() {
            double successRate = processingStats.getSuccessRate();
            double backpressureScore = getBackpressureScore();
            double sequenceScore = sequenceStats.getValidRate();

            return (int) ((successRate * 0.5 + backpressureScore * 0.3 + sequenceScore * 0.2) * 100);
        }

        private double getBackpressureScore() {
            switch (backpressureStatus.getLevel()) {
                case NORMAL:
                    return 1.0;
                case MODERATE:
                    return 0.7;
                case HIGH:
                    return 0.4;
                case CRITICAL:
                    return 0.1;
                default:
                    return 1.0;
            }
        }

        /**
         * 判斷系統是否健康
         */
        public boolean isHealthy() {
            return getHealthScore() >= 80;
        }
    }
}