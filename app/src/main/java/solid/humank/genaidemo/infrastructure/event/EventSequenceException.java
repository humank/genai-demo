package solid.humank.genaidemo.infrastructure.event;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * 事件順序錯亂異常
 * 需求 9.5: 實現事件順序錯亂的檢測和處理機制
 */
public class EventSequenceException extends EventProcessingException {

    private final long expectedSequence;
    private final long actualSequence;
    private final String aggregateId;

    public EventSequenceException(String message, DomainEvent event, String eventHandlerName,
            String aggregateId, long expectedSequence, long actualSequence) {
        super(message, event, eventHandlerName);
        this.expectedSequence = expectedSequence;
        this.actualSequence = actualSequence;
        this.aggregateId = aggregateId;
    }

    public EventSequenceException(String message, DomainEvent event, String eventHandlerName,
            String aggregateId, long expectedSequence, long actualSequence, Throwable cause) {
        super(message, event, eventHandlerName, cause);
        this.expectedSequence = expectedSequence;
        this.actualSequence = actualSequence;
        this.aggregateId = aggregateId;
    }

    public long getExpectedSequence() {
        return expectedSequence;
    }

    public long getActualSequence() {
        return actualSequence;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    @Override
    public String getMessage() {
        return String.format("%s (aggregate: %s, expected: %d, actual: %d)",
                super.getMessage(), aggregateId, expectedSequence, actualSequence);
    }
}