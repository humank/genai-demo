package solid.humank.genaidemo.infrastructure.event.monitoring;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * 事件處理監控器
 * 需求 9.2: 建立系統異常的適當錯誤回應和日誌記錄
 * 需求 9.3: 實現事件處理超時的檢測和處理機制
 */
@Component
public class EventProcessingMonitor {

    private static final Logger logger = LoggerFactory.getLogger(EventProcessingMonitor.class);

    // 事件處理統計
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final AtomicLong totalEventsSucceeded = new AtomicLong(0);
    private final AtomicLong totalEventsFailed = new AtomicLong(0);
    private final AtomicLong totalEventsTimedOut = new AtomicLong(0);

    // 正在處理的事件
    private final ConcurrentHashMap<String, EventProcessingContext> activeProcessing = new ConcurrentHashMap<>();

    // 事件處理時間統計
    private final ConcurrentHashMap<String, EventTypeStatistics> eventTypeStats = new ConcurrentHashMap<>();

    /**
     * 開始監控事件處理
     */
    public EventProcessingContext startMonitoring(DomainEvent event, String handlerName) {
        String contextId = generateContextId(event, handlerName);
        EventProcessingContext context = new EventProcessingContext(contextId, event, handlerName);

        activeProcessing.put(contextId, context);
        totalEventsProcessed.incrementAndGet();

        logger.debug("Started monitoring event processing: {} with handler: {} (contextId: {})",
                event.getEventType(), handlerName, contextId);

        return context;
    }

    /**
     * 完成事件處理監控
     */
    public void completeMonitoring(EventProcessingContext context, boolean success, Throwable error) {
        activeProcessing.remove(context.getContextId());

        Duration processingTime = context.getProcessingTime();
        String eventType = context.getEvent().getEventType();

        // 更新統計
        if (success) {
            totalEventsSucceeded.incrementAndGet();
            logger.debug("Event processing completed successfully: {} with handler: {} in {}ms",
                    eventType, context.getHandlerName(), processingTime.toMillis());
        } else {
            totalEventsFailed.incrementAndGet();
            logger.warn("Event processing failed: {} with handler: {} after {}ms, error: {}",
                    eventType, context.getHandlerName(), processingTime.toMillis(),
                    error != null ? error.getMessage() : "Unknown error");
        }

        // 更新事件類型統計
        updateEventTypeStatistics(eventType, processingTime, success);
    }

    /**
     * 記錄事件處理超時
     */
    public void recordTimeout(EventProcessingContext context, Duration timeout) {
        activeProcessing.remove(context.getContextId());
        totalEventsTimedOut.incrementAndGet();

        Duration actualTime = context.getProcessingTime();

        logger.error("Event processing timed out: {} with handler: {} (timeout: {}ms, actual: {}ms)",
                context.getEvent().getEventType(), context.getHandlerName(),
                timeout.toMillis(), actualTime.toMillis());

        // 更新事件類型統計
        updateEventTypeStatistics(context.getEvent().getEventType(), actualTime, false);
    }

    /**
     * 檢查超時的事件處理
     */
    public void checkForTimeouts(Duration timeout) {
        LocalDateTime cutoffTime = LocalDateTime.now().minus(timeout);

        activeProcessing.values().stream()
                .filter(context -> context.getStartTime().isBefore(cutoffTime))
                .forEach(context -> {
                    logger.warn("Detected timeout for event processing: {} with handler: {} (started: {})",
                            context.getEvent().getEventType(), context.getHandlerName(), context.getStartTime());
                    recordTimeout(context, timeout);
                });
    }

    /**
     * 獲取處理統計信息
     */
    public ProcessingStatistics getStatistics() {
        return new ProcessingStatistics(
                totalEventsProcessed.get(),
                totalEventsSucceeded.get(),
                totalEventsFailed.get(),
                totalEventsTimedOut.get(),
                activeProcessing.size(),
                new ConcurrentHashMap<>(eventTypeStats));
    }

    /**
     * 獲取當前活躍的處理上下文
     */
    public ConcurrentHashMap<String, EventProcessingContext> getActiveProcessing() {
        return new ConcurrentHashMap<>(activeProcessing);
    }

    /**
     * 重置統計信息
     */
    public void resetStatistics() {
        totalEventsProcessed.set(0);
        totalEventsSucceeded.set(0);
        totalEventsFailed.set(0);
        totalEventsTimedOut.set(0);
        eventTypeStats.clear();

        logger.info("Event processing statistics reset");
    }

    private void updateEventTypeStatistics(String eventType, Duration processingTime, boolean success) {
        eventTypeStats.compute(eventType, (key, stats) -> {
            if (stats == null) {
                stats = new EventTypeStatistics(eventType);
            }
            stats.addProcessingTime(processingTime, success);
            return stats;
        });
    }

    private String generateContextId(DomainEvent event, String handlerName) {
        return String.format("%s-%s-%s-%d",
                event.getEventType(),
                event.getAggregateId(),
                handlerName,
                System.nanoTime());
    }

    /**
     * 事件處理統計信息
     */
    public static class ProcessingStatistics {
        private final long totalProcessed;
        private final long totalSucceeded;
        private final long totalFailed;
        private final long totalTimedOut;
        private final int activeProcessing;
        private final ConcurrentHashMap<String, EventTypeStatistics> eventTypeStats;

        public ProcessingStatistics(long totalProcessed, long totalSucceeded, long totalFailed,
                long totalTimedOut, int activeProcessing,
                ConcurrentHashMap<String, EventTypeStatistics> eventTypeStats) {
            this.totalProcessed = totalProcessed;
            this.totalSucceeded = totalSucceeded;
            this.totalFailed = totalFailed;
            this.totalTimedOut = totalTimedOut;
            this.activeProcessing = activeProcessing;
            this.eventTypeStats = eventTypeStats;
        }

        public long getTotalProcessed() {
            return totalProcessed;
        }

        public long getTotalSucceeded() {
            return totalSucceeded;
        }

        public long getTotalFailed() {
            return totalFailed;
        }

        public long getTotalTimedOut() {
            return totalTimedOut;
        }

        public int getActiveProcessing() {
            return activeProcessing;
        }

        public ConcurrentHashMap<String, EventTypeStatistics> getEventTypeStats() {
            return eventTypeStats;
        }

        public double getSuccessRate() {
            return totalProcessed > 0 ? (double) totalSucceeded / totalProcessed : 0.0;
        }

        public double getFailureRate() {
            return totalProcessed > 0 ? (double) totalFailed / totalProcessed : 0.0;
        }

        public double getTimeoutRate() {
            return totalProcessed > 0 ? (double) totalTimedOut / totalProcessed : 0.0;
        }
    }
}