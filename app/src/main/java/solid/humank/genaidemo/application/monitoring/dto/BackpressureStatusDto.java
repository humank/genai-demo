package solid.humank.genaidemo.application.monitoring.dto;

import java.time.LocalDateTime;

/**
 * 背壓狀態DTO
 * 應用層的背壓狀態數據傳輸對象
 */
public class BackpressureStatusDto {
    private final String level;
    private final int currentConcurrentEvents;
    private final int currentQueueSize;
    private final long eventsInCurrentWindow;
    private final LocalDateTime lastLevelChangeTime;

    public BackpressureStatusDto(String level, int currentConcurrentEvents,
            int currentQueueSize, long eventsInCurrentWindow,
            LocalDateTime lastLevelChangeTime) {
        this.level = level;
        this.currentConcurrentEvents = currentConcurrentEvents;
        this.currentQueueSize = currentQueueSize;
        this.eventsInCurrentWindow = eventsInCurrentWindow;
        this.lastLevelChangeTime = lastLevelChangeTime;
    }

    public String getLevel() {
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
}