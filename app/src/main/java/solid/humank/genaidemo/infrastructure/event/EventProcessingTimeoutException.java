package solid.humank.genaidemo.infrastructure.event;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * 事件處理超時異常
 * 需求 9.3: 實現事件處理超時的檢測和處理機制
 */
public class EventProcessingTimeoutException extends EventProcessingException {

    private final long timeoutMillis;
    private final long actualProcessingTime;

    public EventProcessingTimeoutException(String message, DomainEvent event, String eventHandlerName,
            long timeoutMillis, long actualProcessingTime) {
        super(message, event, eventHandlerName);
        this.timeoutMillis = timeoutMillis;
        this.actualProcessingTime = actualProcessingTime;
    }

    public EventProcessingTimeoutException(String message, DomainEvent event, String eventHandlerName,
            long timeoutMillis, long actualProcessingTime, Throwable cause) {
        super(message, event, eventHandlerName, cause);
        this.timeoutMillis = timeoutMillis;
        this.actualProcessingTime = actualProcessingTime;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public long getActualProcessingTime() {
        return actualProcessingTime;
    }

    @Override
    public String getMessage() {
        return String.format("%s (timeout: %dms, actual: %dms)",
                super.getMessage(), timeoutMillis, actualProcessingTime);
    }
}