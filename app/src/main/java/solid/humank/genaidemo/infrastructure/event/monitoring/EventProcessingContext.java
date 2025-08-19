package solid.humank.genaidemo.infrastructure.event.monitoring;

import java.time.Duration;
import java.time.LocalDateTime;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * 事件處理上下文
 * 記錄事件處理過程中的監控信息
 */
public class EventProcessingContext {

    private final String contextId;
    private final DomainEvent event;
    private final String handlerName;
    private final LocalDateTime startTime;

    private LocalDateTime endTime;
    private boolean completed = false;

    public EventProcessingContext(String contextId, DomainEvent event, String handlerName) {
        this.contextId = contextId;
        this.event = event;
        this.handlerName = handlerName;
        this.startTime = LocalDateTime.now();
    }

    /**
     * 標記處理完成
     */
    public void markCompleted() {
        this.endTime = LocalDateTime.now();
        this.completed = true;
    }

    /**
     * 獲取處理時間
     */
    public Duration getProcessingTime() {
        LocalDateTime end = completed ? endTime : LocalDateTime.now();
        return Duration.between(startTime, end);
    }

    /**
     * 檢查是否超時
     */
    public boolean isTimedOut(Duration timeout) {
        return getProcessingTime().compareTo(timeout) > 0;
    }

    // Getters
    public String getContextId() {
        return contextId;
    }

    public DomainEvent getEvent() {
        return event;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public boolean isCompleted() {
        return completed;
    }

    @Override
    public String toString() {
        return String.format("EventProcessingContext{contextId='%s', eventType='%s', handlerName='%s', " +
                "startTime=%s, processingTime=%s, completed=%s}",
                contextId, event.getEventType(), handlerName, startTime,
                getProcessingTime(), completed);
    }
}