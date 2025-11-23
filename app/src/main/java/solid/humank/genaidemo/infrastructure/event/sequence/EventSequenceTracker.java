package solid.humank.genaidemo.infrastructure.event.sequence;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * 事件順序追蹤器
 * 需求 9.5: 實現事件順序錯亂的檢測和處理機制
 */
@Component
public class EventSequenceTracker {    private static final Logger logger = LoggerFactory.getLogger(EventSequenceTracker.class);

    // 每個聚合根的事件序列號追蹤
    private final ConcurrentHashMap<String, AtomicLong> aggregateSequences = new ConcurrentHashMap<>();

    // 事件順序驗證記錄
    private final ConcurrentHashMap<String, EventSequenceRecord> sequenceRecords = new ConcurrentHashMap<>();

    /**
     * 驗證事件順序
     */
    public EventSequenceValidation validateEventSequence(DomainEvent event) {
        String aggregateId = event.getAggregateId();

        // 獲取或創建聚合根的序列追蹤器
        AtomicLong sequenceTracker = aggregateSequences.computeIfAbsent(aggregateId, k -> new AtomicLong(0));

        // 獲取當前期望的序列號
        long expectedSequence = sequenceTracker.get() + 1;

        // 假設事件有序列號（這裡簡化處理，實際應該從事件中獲取）
        long eventSequence = getEventSequence(event);

        EventSequenceRecord record = new EventSequenceRecord(
                aggregateId, event.getEventType(), expectedSequence, eventSequence, LocalDateTime.now());

        sequenceRecords.put(generateRecordKey(event), record);

        if (eventSequence == expectedSequence) {
            // 順序正確，更新序列號
            sequenceTracker.set(eventSequence);

            logger.debug("Event sequence validated: {} for aggregate: {} (sequence: {})",
                    event.getEventType(), aggregateId, eventSequence);

            return EventSequenceValidation.valid(record);

        } else if (eventSequence < expectedSequence) {
            // 重複事件或過期事件
            logger.warn("Duplicate or outdated event detected: {} for aggregate: {} (expected: {}, actual: {})",
                    event.getEventType(), aggregateId, expectedSequence, eventSequence);

            return EventSequenceValidation.duplicate(record);

        } else {
            // 事件順序錯亂（跳過了某些事件）
            logger.error("Event sequence disorder detected: {} for aggregate: {} (expected: {}, actual: {})",
                    event.getEventType(), aggregateId, expectedSequence, eventSequence);

            return EventSequenceValidation.outOfOrder(record);
        }
    }

    /**
     * 強制更新序列號（用於處理順序錯亂後的恢復）
     */
    public void forceUpdateSequence(String aggregateId, long sequence) {
        aggregateSequences.computeIfAbsent(aggregateId, k -> new AtomicLong(0)).set(sequence);

        logger.info("Forced sequence update for aggregate: {} to sequence: {}", aggregateId, sequence);
    }

    /**
     * 獲取聚合根的當前序列號
     */
    public long getCurrentSequence(String aggregateId) {
        return aggregateSequences.getOrDefault(aggregateId, new AtomicLong(0)).get();
    }

    /**
     * 重置聚合根的序列追蹤
     */
    public void resetSequence(String aggregateId) {
        aggregateSequences.remove(aggregateId);

        // 清理相關的序列記錄
        sequenceRecords.entrySet().removeIf(entry -> entry.getValue().getAggregateId().equals(aggregateId));

        logger.info("Reset sequence tracking for aggregate: {}", aggregateId);
    }

    /**
     * 獲取序列追蹤統計
     */
    public SequenceTrackingStatistics getStatistics() {
        int totalAggregates = aggregateSequences.size();
        int totalRecords = sequenceRecords.size();

        long validEvents = sequenceRecords.values().stream()
                .mapToLong(record -> record.isValid() ? 1 : 0)
                .sum();

        long duplicateEvents = sequenceRecords.values().stream()
                .mapToLong(record -> record.isDuplicate() ? 1 : 0)
                .sum();

        long outOfOrderEvents = sequenceRecords.values().stream()
                .mapToLong(record -> record.isOutOfOrder() ? 1 : 0)
                .sum();

        return new SequenceTrackingStatistics(
                totalAggregates, totalRecords, validEvents, duplicateEvents, outOfOrderEvents);
    }

    /**
     * 清理過期的序列記錄
     */
    public void cleanupExpiredRecords() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);

        sequenceRecords.entrySet().removeIf(entry -> entry.getValue().getTimestamp().isBefore(cutoffTime));

        logger.debug("Cleaned up expired sequence records");
    }

    private long getEventSequence(DomainEvent event) {
        // 這裡簡化處理，實際應該從事件中獲取序列號
        // 可以通過事件的時間戳或其他方式來模擬序列號
        return event.getOccurredOn().toEpochSecond(java.time.ZoneOffset.UTC);
    }

    private String generateRecordKey(DomainEvent event) {
        return String.format("%s-%s-%s",
                event.getAggregateId(),
                event.getEventType(),
                event.getEventId());
    }

    /**
     * 事件序列驗證結果
     */
    public static class EventSequenceValidation {
        private final ValidationResult result;
        private final EventSequenceRecord record;

        private EventSequenceValidation(ValidationResult result, EventSequenceRecord record) {
            this.result = result;
            this.record = record;
        }

        public static EventSequenceValidation valid(EventSequenceRecord record) {
            return new EventSequenceValidation(ValidationResult.VALID, record);
        }

        public static EventSequenceValidation duplicate(EventSequenceRecord record) {
            return new EventSequenceValidation(ValidationResult.DUPLICATE, record);
        }

        public static EventSequenceValidation outOfOrder(EventSequenceRecord record) {
            return new EventSequenceValidation(ValidationResult.OUT_OF_ORDER, record);
        }

        public boolean isValid() {
            return result == ValidationResult.VALID;
        }

        public boolean isDuplicate() {
            return result == ValidationResult.DUPLICATE;
        }

        public boolean isOutOfOrder() {
            return result == ValidationResult.OUT_OF_ORDER;
        }

        public ValidationResult getResult() {
            return result;
        }

        public EventSequenceRecord getRecord() {
            return record;
        }

        public enum ValidationResult {
            VALID, // 順序正確
            DUPLICATE, // 重複事件
            OUT_OF_ORDER // 順序錯亂
        }
    }

    /**
     * 序列追蹤統計
     */
    public static class SequenceTrackingStatistics {
        private final int totalAggregates;
        private final int totalRecords;
        private final long validEvents;
        private final long duplicateEvents;
        private final long outOfOrderEvents;

        public SequenceTrackingStatistics(int totalAggregates, int totalRecords,
                long validEvents, long duplicateEvents, long outOfOrderEvents) {
            this.totalAggregates = totalAggregates;
            this.totalRecords = totalRecords;
            this.validEvents = validEvents;
            this.duplicateEvents = duplicateEvents;
            this.outOfOrderEvents = outOfOrderEvents;
        }

        public int getTotalAggregates() {
            return totalAggregates;
        }

        public int getTotalRecords() {
            return totalRecords;
        }

        public long getValidEvents() {
            return validEvents;
        }

        public long getDuplicateEvents() {
            return duplicateEvents;
        }

        public long getOutOfOrderEvents() {
            return outOfOrderEvents;
        }

        public double getValidRate() {
            return totalRecords > 0 ? (double) validEvents / totalRecords : 0.0;
        }

        public double getDuplicateRate() {
            return totalRecords > 0 ? (double) duplicateEvents / totalRecords : 0.0;
        }

        public double getOutOfOrderRate() {
            return totalRecords > 0 ? (double) outOfOrderEvents / totalRecords : 0.0;
        }
    }
}