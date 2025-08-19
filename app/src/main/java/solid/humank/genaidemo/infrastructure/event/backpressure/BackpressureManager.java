package solid.humank.genaidemo.infrastructure.event.backpressure;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 背壓管理器
 * 需求 9.4: 建立系統負載過高時的背壓機制
 */
@Component
public class BackpressureManager {

    private static final Logger logger = LoggerFactory.getLogger(BackpressureManager.class);

    // 配置參數
    private final int maxConcurrentEvents;
    private final int maxQueueSize;
    private final Duration slidingWindowDuration;
    private final int maxEventsPerWindow;

    // 當前狀態
    private final AtomicInteger currentConcurrentEvents = new AtomicInteger(0);
    private final AtomicInteger currentQueueSize = new AtomicInteger(0);
    private final AtomicLong eventsInCurrentWindow = new AtomicLong(0);
    private volatile LocalDateTime windowStartTime = LocalDateTime.now();

    // 背壓狀態
    private volatile BackpressureLevel currentLevel = BackpressureLevel.NORMAL;
    private volatile LocalDateTime lastLevelChangeTime = LocalDateTime.now();

    public BackpressureManager() {
        // 默認配置
        this.maxConcurrentEvents = 100;
        this.maxQueueSize = 1000;
        this.slidingWindowDuration = Duration.ofMinutes(1);
        this.maxEventsPerWindow = 1000;
    }

    public BackpressureManager(int maxConcurrentEvents, int maxQueueSize,
            Duration slidingWindowDuration, int maxEventsPerWindow) {
        this.maxConcurrentEvents = maxConcurrentEvents;
        this.maxQueueSize = maxQueueSize;
        this.slidingWindowDuration = slidingWindowDuration;
        this.maxEventsPerWindow = maxEventsPerWindow;
    }

    /**
     * 檢查是否可以處理新事件
     */
    public BackpressureDecision shouldProcessEvent() {
        updateSlidingWindow();
        updateBackpressureLevel();

        switch (currentLevel) {
            case NORMAL:
                return BackpressureDecision.PROCEED;

            case MODERATE:
                // 中等負載：延遲處理
                return BackpressureDecision.DELAY;

            case HIGH:
                // 高負載：拒絕部分請求
                if (currentConcurrentEvents.get() < maxConcurrentEvents * 0.8) {
                    return BackpressureDecision.DELAY;
                } else {
                    return BackpressureDecision.REJECT;
                }

            case CRITICAL:
                // 臨界負載：拒絕新請求
                return BackpressureDecision.REJECT;

            default:
                return BackpressureDecision.PROCEED;
        }
    }

    /**
     * 開始處理事件
     */
    public void startProcessing() {
        currentConcurrentEvents.incrementAndGet();
        eventsInCurrentWindow.incrementAndGet();
    }

    /**
     * 完成事件處理
     */
    public void completeProcessing() {
        currentConcurrentEvents.decrementAndGet();
    }

    /**
     * 添加事件到隊列
     */
    public boolean addToQueue() {
        if (currentQueueSize.get() >= maxQueueSize) {
            logger.warn("Event queue is full, rejecting new event (queue size: {})", currentQueueSize.get());
            return false;
        }

        currentQueueSize.incrementAndGet();
        return true;
    }

    /**
     * 從隊列移除事件
     */
    public void removeFromQueue() {
        currentQueueSize.decrementAndGet();
    }

    /**
     * 獲取建議的延遲時間
     */
    public Duration getSuggestedDelay() {
        switch (currentLevel) {
            case MODERATE:
                return Duration.ofMillis(100);
            case HIGH:
                return Duration.ofMillis(500);
            case CRITICAL:
                return Duration.ofSeconds(2);
            default:
                return Duration.ZERO;
        }
    }

    /**
     * 獲取當前背壓狀態
     */
    public BackpressureStatus getStatus() {
        return new BackpressureStatus(
                currentLevel,
                currentConcurrentEvents.get(),
                currentQueueSize.get(),
                eventsInCurrentWindow.get(),
                lastLevelChangeTime);
    }

    private void updateSlidingWindow() {
        LocalDateTime now = LocalDateTime.now();
        if (Duration.between(windowStartTime, now).compareTo(slidingWindowDuration) >= 0) {
            // 重置滑動窗口
            windowStartTime = now;
            eventsInCurrentWindow.set(0);
        }
    }

    private void updateBackpressureLevel() {
        BackpressureLevel newLevel = calculateBackpressureLevel();

        if (newLevel != currentLevel) {
            logger.info("Backpressure level changed from {} to {} (concurrent: {}, queue: {}, window: {})",
                    currentLevel, newLevel, currentConcurrentEvents.get(),
                    currentQueueSize.get(), eventsInCurrentWindow.get());

            currentLevel = newLevel;
            lastLevelChangeTime = LocalDateTime.now();
        }
    }

    private BackpressureLevel calculateBackpressureLevel() {
        int concurrent = currentConcurrentEvents.get();
        int queue = currentQueueSize.get();
        long windowEvents = eventsInCurrentWindow.get();

        // 計算各項指標的負載百分比
        double concurrentLoad = (double) concurrent / maxConcurrentEvents;
        double queueLoad = (double) queue / maxQueueSize;
        double windowLoad = (double) windowEvents / maxEventsPerWindow;

        // 取最高負載作為整體負載
        double maxLoad = Math.max(Math.max(concurrentLoad, queueLoad), windowLoad);

        if (maxLoad >= 0.9) {
            return BackpressureLevel.CRITICAL;
        } else if (maxLoad >= 0.7) {
            return BackpressureLevel.HIGH;
        } else if (maxLoad >= 0.5) {
            return BackpressureLevel.MODERATE;
        } else {
            return BackpressureLevel.NORMAL;
        }
    }

    /**
     * 背壓等級
     */
    public enum BackpressureLevel {
        NORMAL, // 正常負載
        MODERATE, // 中等負載
        HIGH, // 高負載
        CRITICAL // 臨界負載
    }

    /**
     * 背壓決策
     */
    public enum BackpressureDecision {
        PROCEED, // 繼續處理
        DELAY, // 延遲處理
        REJECT // 拒絕處理
    }

    /**
     * 背壓狀態
     */
    public static class BackpressureStatus {
        private final BackpressureLevel level;
        private final int currentConcurrentEvents;
        private final int currentQueueSize;
        private final long eventsInCurrentWindow;
        private final LocalDateTime lastLevelChangeTime;

        public BackpressureStatus(BackpressureLevel level, int currentConcurrentEvents,
                int currentQueueSize, long eventsInCurrentWindow,
                LocalDateTime lastLevelChangeTime) {
            this.level = level;
            this.currentConcurrentEvents = currentConcurrentEvents;
            this.currentQueueSize = currentQueueSize;
            this.eventsInCurrentWindow = eventsInCurrentWindow;
            this.lastLevelChangeTime = lastLevelChangeTime;
        }

        // Getters
        public BackpressureLevel getLevel() {
            return level;
        }

        public int getCurrentConcurrentEvents() {
            return currentConcurrentEvents;
        }

        public int getCurrentQueueSize() {
            return currentQueueSize;
        }

        public long getEventsInCurrentWindow() {
            return eventsInCurrentWindow;
        }

        public LocalDateTime getLastLevelChangeTime() {
            return lastLevelChangeTime;
        }

        @Override
        public String toString() {
            return String.format("BackpressureStatus{level=%s, concurrent=%d, queue=%d, window=%d, lastChange=%s}",
                    level, currentConcurrentEvents, currentQueueSize, eventsInCurrentWindow, lastLevelChangeTime);
        }
    }
}