package solid.humank.genaidemo.infrastructure.event.sequence;

import java.time.LocalDateTime;

/**
 * 事件序列記錄
 * 記錄事件序列驗證的詳細信息
 */
public class EventSequenceRecord {

    private final String aggregateId;
    private final String eventType;
    private final long expectedSequence;
    private final long actualSequence;
    private final LocalDateTime timestamp;

    public EventSequenceRecord(String aggregateId, String eventType,
            long expectedSequence, long actualSequence, LocalDateTime timestamp) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.expectedSequence = expectedSequence;
        this.actualSequence = actualSequence;
        this.timestamp = timestamp;
    }

    /**
     * 檢查序列是否有效
     */
    public boolean isValid() {
        return expectedSequence == actualSequence;
    }

    /**
     * 檢查是否為重複事件
     */
    public boolean isDuplicate() {
        return actualSequence < expectedSequence;
    }

    /**
     * 檢查是否順序錯亂
     */
    public boolean isOutOfOrder() {
        return actualSequence > expectedSequence;
    }

    /**
     * 獲取序列差異
     */
    public long getSequenceDifference() {
        return actualSequence - expectedSequence;
    }

    // Getters
    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public long getExpectedSequence() {
        return expectedSequence;
    }

    public long getActualSequence() {
        return actualSequence;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("EventSequenceRecord{aggregateId='%s', eventType='%s', " +
                "expectedSequence=%d, actualSequence=%d, timestamp=%s, valid=%s}",
                aggregateId, eventType, expectedSequence, actualSequence, timestamp, isValid());
    }
}