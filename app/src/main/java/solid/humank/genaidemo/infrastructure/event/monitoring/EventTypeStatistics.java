package solid.humank.genaidemo.infrastructure.event.monitoring;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事件類型統計信息
 * 記錄特定事件類型的處理統計
 */
public class EventTypeStatistics {

    private final String eventType;
    private final AtomicLong totalCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicLong totalProcessingTimeMillis = new AtomicLong(0);
    private volatile long minProcessingTimeMillis = Long.MAX_VALUE;
    private volatile long maxProcessingTimeMillis = 0;

    public EventTypeStatistics(String eventType) {
        this.eventType = eventType;
    }

    /**
     * 添加處理時間記錄
     */
    public synchronized void addProcessingTime(Duration processingTime, boolean success) {
        long millis = processingTime.toMillis();

        totalCount.incrementAndGet();
        totalProcessingTimeMillis.addAndGet(millis);

        if (success) {
            successCount.incrementAndGet();
        } else {
            failureCount.incrementAndGet();
        }

        // 更新最小和最大處理時間
        if (millis < minProcessingTimeMillis) {
            minProcessingTimeMillis = millis;
        }
        if (millis > maxProcessingTimeMillis) {
            maxProcessingTimeMillis = millis;
        }
    }

    /**
     * 獲取平均處理時間
     */
    public Duration getAverageProcessingTime() {
        long total = totalCount.get();
        if (total == 0) {
            return Duration.ZERO;
        }
        return Duration.ofMillis(totalProcessingTimeMillis.get() / total);
    }

    /**
     * 獲取最小處理時間
     */
    public Duration getMinProcessingTime() {
        return minProcessingTimeMillis == Long.MAX_VALUE ? Duration.ZERO : Duration.ofMillis(minProcessingTimeMillis);
    }

    /**
     * 獲取最大處理時間
     */
    public Duration getMaxProcessingTime() {
        return Duration.ofMillis(maxProcessingTimeMillis);
    }

    /**
     * 獲取成功率
     */
    public double getSuccessRate() {
        long total = totalCount.get();
        return total > 0 ? (double) successCount.get() / total : 0.0;
    }

    /**
     * 獲取失敗率
     */
    public double getFailureRate() {
        long total = totalCount.get();
        return total > 0 ? (double) failureCount.get() / total : 0.0;
    }

    // Getters
    public String getEventType() {
        return eventType;
    }

    public long getTotalCount() {
        return totalCount.get();
    }

    public long getSuccessCount() {
        return successCount.get();
    }

    public long getFailureCount() {
        return failureCount.get();
    }

    public Duration getTotalProcessingTime() {
        return Duration.ofMillis(totalProcessingTimeMillis.get());
    }

    @Override
    public String toString() {
        return String.format("EventTypeStatistics{eventType='%s', totalCount=%d, successCount=%d, " +
                "failureCount=%d, avgProcessingTime=%s, successRate=%.2f%%}",
                eventType, getTotalCount(), getSuccessCount(), getFailureCount(),
                getAverageProcessingTime(), getSuccessRate() * 100);
    }
}