package solid.humank.genaidemo.infrastructure.event.retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.infrastructure.event.EventProcessingException;

/**
 * 事件重試管理器
 * 需求 9.1: 實現事件處理失敗的錯誤記錄和重試機制
 */
@Component
public class EventRetryManager {    private static final Logger logger = LoggerFactory.getLogger(EventRetryManager.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private final ConcurrentHashMap<String, RetryContext> retryContexts = new ConcurrentHashMap<>();

    /**
     * 執行帶重試的事件處理
     */
    @Async
    public CompletableFuture<Void> executeWithRetry(DomainEvent event, String handlerName,
            Consumer<DomainEvent> eventHandler, RetryPolicy retryPolicy) {
        return CompletableFuture.runAsync(() -> {
            String contextKey = generateContextKey(event, handlerName);
            RetryContext context = retryContexts.computeIfAbsent(contextKey,
                    k -> new RetryContext(event, handlerName, retryPolicy));

            executeWithRetryInternal(context, eventHandler);
        });
    }

    private void executeWithRetryInternal(RetryContext context, Consumer<DomainEvent> eventHandler) {
        try {
            logger.debug("Executing event handler: {} for event: {} (attempt: {})",
                    context.getHandlerName(), context.getEvent().getEventType(), context.getAttemptCount());

            eventHandler.accept(context.getEvent());

            // 成功處理，移除重試上下文
            retryContexts.remove(context.getContextKey());

            logger.debug("Successfully processed event: {} with handler: {} after {} attempts",
                    context.getEvent().getEventType(), context.getHandlerName(), context.getAttemptCount());

        } catch (Exception e) {
            handleEventProcessingFailure(context, e, eventHandler);
        }
    }

    private void handleEventProcessingFailure(RetryContext context, Exception e, Consumer<DomainEvent> eventHandler) {
        context.incrementAttempt();

        logger.warn("Event processing failed: {} with handler: {} (attempt: {}/{}), error: {}",
                context.getEvent().getEventType(), context.getHandlerName(),
                context.getAttemptCount(), context.getRetryPolicy().getMaxAttempts(), e.getMessage());

        if (context.getRetryPolicy().shouldRetry(e, context.getAttemptCount())) {
            scheduleRetry(context, e, eventHandler);
        } else {
            handleFinalFailure(context, e);
        }
    }

    private void scheduleRetry(RetryContext context, Exception e, Consumer<DomainEvent> eventHandler) {
        Duration delay = context.getRetryPolicy().calculateDelay(context.getAttemptCount());
        context.setNextRetryTime(LocalDateTime.now().plus(delay));

        logger.info("Scheduling retry for event: {} with handler: {} in {} ms (attempt: {}/{})",
                context.getEvent().getEventType(), context.getHandlerName(), delay.toMillis(),
                context.getAttemptCount(), context.getRetryPolicy().getMaxAttempts());

        scheduler.schedule(() -> executeWithRetryInternal(context, eventHandler), delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void handleFinalFailure(RetryContext context, Exception e) {
        // 移除重試上下文
        retryContexts.remove(context.getContextKey());

        logger.error("Event processing finally failed after {} attempts: {} with handler: {}, error: {}",
                context.getAttemptCount(), context.getEvent().getEventType(),
                context.getHandlerName(), e.getMessage(), e);

        // 創建最終失敗的異常
        EventProcessingException finalException = EventProcessingException.nonRetryable(
                "Event processing failed after " + context.getAttemptCount() + " attempts",
                context.getEvent(), context.getHandlerName(), e);

        // 可以在這裡添加死信隊列或其他失敗處理邏輯
        handleDeadLetter(context, finalException);
    }

    private void handleDeadLetter(RetryContext context, EventProcessingException exception) {
        // 記錄到死信隊列或發送警報
        logger.error("Event sent to dead letter queue: {} with handler: {}, final error: {}",
                context.getEvent().getEventType(), context.getHandlerName(), exception.getMessage());

        // 這裡可以實現死信隊列邏輯，例如：
        // - 保存到數據庫
        // - 發送到消息隊列
        // - 發送警報通知
    }

    private String generateContextKey(DomainEvent event, String handlerName) {
        return String.format("%s-%s-%s", event.getEventType(), event.getAggregateId(), handlerName);
    }

    /**
     * 獲取當前重試統計信息
     */
    public RetryStatistics getRetryStatistics() {
        int activeRetries = retryContexts.size();
        long totalRetryAttempts = retryContexts.values().stream()
                .mapToLong(RetryContext::getAttemptCount)
                .sum();

        return new RetryStatistics(activeRetries, totalRetryAttempts);
    }

    /**
     * 清理過期的重試上下文
     */
    public void cleanupExpiredContexts() {
        LocalDateTime now = LocalDateTime.now();
        retryContexts.entrySet().removeIf(entry -> {
            RetryContext context = entry.getValue();
            // 清理超過1小時沒有重試的上下文
            return context.getNextRetryTime() != null &&
                    context.getNextRetryTime().plusHours(1).isBefore(now);
        });
    }

    /**
     * 重試統計信息
     */
    public static class RetryStatistics {
        private final int activeRetries;
        private final long totalRetryAttempts;

        public RetryStatistics(int activeRetries, long totalRetryAttempts) {
            this.activeRetries = activeRetries;
            this.totalRetryAttempts = totalRetryAttempts;
        }

        public int getActiveRetries() {
            return activeRetries;
        }

        public long getTotalRetryAttempts() {
            return totalRetryAttempts;
        }
    }
}