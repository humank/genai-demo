package solid.humank.genaidemo.infrastructure.event;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * 事件處理異常
 * 需求 9.1: 實現事件處理失敗的錯誤記錄和重試機制
 */
public class EventProcessingException extends RuntimeException {

    private final DomainEvent event;
    private final String eventHandlerName;
    private final int attemptCount;
    private final boolean retryable;

    public EventProcessingException(String message, DomainEvent event, String eventHandlerName) {
        this(message, event, eventHandlerName, 1, true, null);
    }

    public EventProcessingException(String message, DomainEvent event, String eventHandlerName, Throwable cause) {
        this(message, event, eventHandlerName, 1, true, cause);
    }

    public EventProcessingException(String message, DomainEvent event, String eventHandlerName,
            int attemptCount, boolean retryable, Throwable cause) {
        super(message, cause);
        this.event = event;
        this.eventHandlerName = eventHandlerName;
        this.attemptCount = attemptCount;
        this.retryable = retryable;
    }

    public DomainEvent getEvent() {
        return event;
    }

    public String getEventHandlerName() {
        return eventHandlerName;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public boolean isRetryable() {
        return retryable;
    }

    /**
     * 創建不可重試的異常
     */
    public static EventProcessingException nonRetryable(String message, DomainEvent event,
            String eventHandlerName, Throwable cause) {
        return new EventProcessingException(message, event, eventHandlerName, 1, false, cause);
    }

    /**
     * 創建重試異常
     */
    public EventProcessingException withRetryAttempt(int newAttemptCount) {
        return new EventProcessingException(getMessage(), event, eventHandlerName,
                newAttemptCount, retryable, getCause());
    }
}