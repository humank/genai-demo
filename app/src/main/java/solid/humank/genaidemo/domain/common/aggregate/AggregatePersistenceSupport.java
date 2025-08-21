package solid.humank.genaidemo.domain.common.aggregate;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * 聚合根持久化支援
 * 
 * 確保聚合根持久化時事件正確收集和發布
 * 
 * 設計理念：
 * 1. 事件收集：在持久化前收集所有未提交事件
 * 2. 事務邊界：確保事件在事務邊界內正確處理
 * 3. 狀態一致性：確保聚合根狀態與事件的一致性
 * 4. 純領域：不依賴任何基礎設施框架
 * 
 * 使用方式：
 * 
 * // 在 Repository Adapter 中
 * public Customer save(Customer customer) {
 * AggregatePersistenceSupport.beforePersistence(customer, events -> {
 * // 驗證事件
 * validateEvents(events);
 * });
 * 
 * Customer savedCustomer = jpaRepository.save(customer);
 * 
 * AggregatePersistenceSupport.afterPersistence(savedCustomer, events -> {
 * // 發布事件
 * eventPublisher.publishAll(events);
 * });
 * 
 * return savedCustomer;
 * }
 */
public final class AggregatePersistenceSupport {

    private AggregatePersistenceSupport() {
        // 工具類，不允許實例化
    }

    /**
     * 持久化前處理
     * 
     * @param aggregateRoot 聚合根
     * @param eventHandler  事件處理器
     * @param <T>           聚合根類型
     * @return 收集到的事件列表
     */
    public static <T extends AggregateRootInterface> List<DomainEvent> beforePersistence(
            T aggregateRoot,
            Consumer<List<DomainEvent>> eventHandler) {

        Objects.requireNonNull(aggregateRoot, "聚合根不能為空");
        Objects.requireNonNull(eventHandler, "事件處理器不能為空");

        // 收集未提交的事件
        List<DomainEvent> events = aggregateRoot.getUncommittedEvents();

        // 驗證聚合根狀態
        validateAggregateState(aggregateRoot);

        // 驗證事件
        validateEvents(events);

        // 執行自定義事件處理
        eventHandler.accept(events);

        return events;
    }

    /**
     * 持久化後處理
     * 
     * @param aggregateRoot 聚合根
     * @param eventHandler  事件處理器
     * @param <T>           聚合根類型
     */
    public static <T extends AggregateRootInterface> void afterPersistence(
            T aggregateRoot,
            Consumer<List<DomainEvent>> eventHandler) {

        Objects.requireNonNull(aggregateRoot, "聚合根不能為空");
        Objects.requireNonNull(eventHandler, "事件處理器不能為空");

        // 獲取未提交的事件
        List<DomainEvent> events = aggregateRoot.getUncommittedEvents();

        if (!events.isEmpty()) {
            // 執行事件處理
            eventHandler.accept(events);

            // 標記事件為已提交
            aggregateRoot.markEventsAsCommitted();
        }
    }

    /**
     * 完整的持久化處理
     * 
     * @param aggregateRoot        聚合根
     * @param persistenceOperation 持久化操作
     * @param eventHandler         事件處理器
     * @param <T>                  聚合根類型
     * @return 持久化後的聚合根
     */
    public static <T extends AggregateRootInterface> T handlePersistence(
            T aggregateRoot,
            java.util.function.Function<T, T> persistenceOperation,
            Consumer<List<DomainEvent>> eventHandler) {

        // 持久化前處理
        List<DomainEvent> events = beforePersistence(aggregateRoot, evts -> {
            // 預設不做額外處理
        });

        // 執行持久化操作
        T persistedAggregate = persistenceOperation.apply(aggregateRoot);

        // 持久化後處理
        afterPersistence(persistedAggregate, eventHandler);

        return persistedAggregate;
    }

    /**
     * 批量持久化處理
     * 
     * @param aggregateRoots       聚合根列表
     * @param persistenceOperation 批量持久化操作
     * @param eventHandler         事件處理器
     * @param <T>                  聚合根類型
     * @return 持久化後的聚合根列表
     */
    public static <T extends AggregateRootInterface> List<T> handleBatchPersistence(
            List<T> aggregateRoots,
            java.util.function.Function<List<T>, List<T>> persistenceOperation,
            Consumer<List<DomainEvent>> eventHandler) {

        Objects.requireNonNull(aggregateRoots, "聚合根列表不能為空");
        Objects.requireNonNull(persistenceOperation, "持久化操作不能為空");
        Objects.requireNonNull(eventHandler, "事件處理器不能為空");

        // 收集所有事件
        List<DomainEvent> allEvents = new java.util.ArrayList<>();
        for (T aggregateRoot : aggregateRoots) {
            List<DomainEvent> events = beforePersistence(aggregateRoot, evts -> {
                // 預設不做額外處理
            });
            allEvents.addAll(events);
        }

        // 執行批量持久化操作
        List<T> persistedAggregates = persistenceOperation.apply(aggregateRoots);

        // 處理所有事件
        if (!allEvents.isEmpty()) {
            eventHandler.accept(allEvents);

            // 標記所有聚合根的事件為已提交
            for (T aggregateRoot : persistedAggregates) {
                aggregateRoot.markEventsAsCommitted();
            }
        }

        return persistedAggregates;
    }

    /**
     * 驗證聚合根狀態
     * 
     * @param aggregateRoot 聚合根
     */
    private static void validateAggregateState(AggregateRootInterface aggregateRoot) {
        // 檢查聚合根是否有必要的註解
        if (!aggregateRoot.getClass().isAnnotationPresent(
                solid.humank.genaidemo.domain.common.annotations.AggregateRoot.class)) {
            throw new IllegalStateException(
                    "聚合根缺少 @AggregateRoot 註解: " + aggregateRoot.getClass().getName());
        }

        // 可以在這裡添加更多的狀態驗證邏輯
    }

    /**
     * 驗證事件
     * 
     * @param events 事件列表
     */
    private static void validateEvents(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            // 檢查事件是否有必要的屬性
            if (event.getEventId() == null) {
                throw new IllegalStateException(
                        "事件缺少事件ID: " + event.getClass().getName());
            }

            if (event.getOccurredOn() == null) {
                throw new IllegalStateException(
                        "事件缺少發生時間: " + event.getClass().getName());
            }

            if (event.getEventType() == null || event.getEventType().trim().isEmpty()) {
                throw new IllegalStateException(
                        "事件缺少事件類型: " + event.getClass().getName());
            }

            if (event.getAggregateId() == null || event.getAggregateId().trim().isEmpty()) {
                throw new IllegalStateException(
                        "事件缺少聚合根ID: " + event.getClass().getName());
            }
        }
    }

    /**
     * 持久化上下文
     */
    public static class PersistenceContext {
        private final String aggregateType;
        private final String aggregateId;
        private final java.time.LocalDateTime timestamp;
        private final int eventCount;

        public PersistenceContext(String aggregateType, String aggregateId, int eventCount) {
            this.aggregateType = Objects.requireNonNull(aggregateType, "聚合根類型不能為空");
            this.aggregateId = Objects.requireNonNull(aggregateId, "聚合根ID不能為空");
            this.eventCount = eventCount;
            this.timestamp = java.time.LocalDateTime.now();
        }

        public String getAggregateType() {
            return aggregateType;
        }

        public String getAggregateId() {
            return aggregateId;
        }

        public java.time.LocalDateTime getTimestamp() {
            return timestamp;
        }

        public int getEventCount() {
            return eventCount;
        }

        @Override
        public String toString() {
            return String.format("PersistenceContext{%s[%s], %d events @ %s}",
                    aggregateType, aggregateId, eventCount, timestamp);
        }
    }

    /**
     * 創建持久化上下文
     * 
     * @param aggregateRoot 聚合根
     * @return 持久化上下文
     */
    public static PersistenceContext createContext(AggregateRootInterface aggregateRoot) {
        return new PersistenceContext(
                aggregateRoot.getAggregateRootName(),
                "unknown", // 這裡需要聚合根提供ID
                aggregateRoot.getUncommittedEvents().size());
    }
}