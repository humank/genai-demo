package solid.humank.genaidemo.domain.common.aggregate;

import java.util.Objects;
import java.util.function.Function;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * 跨聚合根操作支援
 * 
 * 確保跨聚合根操作通過事件而非直接引用進行通信
 * 
 * 設計理念：
 * 1. 事件驅動：強制使用事件進行跨聚合根通信
 * 2. 類型安全：使用泛型確保類型安全
 * 3. 解耦合：避免聚合根間的直接依賴
 * 4. 純領域：不依賴任何基礎設施框架
 * 
 * 使用方式：
 * 
 * // 在聚合根中
 * public void processOrder() {
 * // 業務邏輯
 * this.status = OrderStatus.PROCESSING;
 * 
 * // 跨聚合根操作：通知庫存系統預留商品
 * CrossAggregateOperation.publishEvent(this,
 * new OrderProcessingStartedEvent(this.id, this.items));
 * }
 */
public final class CrossAggregateOperation {

    private CrossAggregateOperation() {
        // 工具類，不允許實例化
    }

    /**
     * 發布跨聚合根事件
     * 
     * @param sourceAggregate 源聚合根
     * @param event           要發布的事件
     * @param <T>             聚合根類型
     */
    public static <T extends AggregateRootInterface> void publishEvent(T sourceAggregate, DomainEvent event) {
        Objects.requireNonNull(sourceAggregate, "源聚合根不能為空");
        Objects.requireNonNull(event, "事件不能為空");

        // 驗證事件的聚合根ID是否與源聚合根匹配
        validateEventSource(sourceAggregate, event);

        // 收集事件
        sourceAggregate.collectEvent(event);
    }

    /**
     * 條件性發布跨聚合根事件
     * 
     * @param sourceAggregate 源聚合根
     * @param condition       發布條件
     * @param eventSupplier   事件供應器
     * @param <T>             聚合根類型
     */
    public static <T extends AggregateRootInterface> void publishEventIf(
            T sourceAggregate,
            boolean condition,
            java.util.function.Supplier<DomainEvent> eventSupplier) {

        if (condition) {
            DomainEvent event = eventSupplier.get();
            publishEvent(sourceAggregate, event);
        }
    }

    /**
     * 發布多個跨聚合根事件
     * 
     * @param sourceAggregate 源聚合根
     * @param events          要發布的事件列表
     * @param <T>             聚合根類型
     */
    public static <T extends AggregateRootInterface> void publishEvents(
            T sourceAggregate,
            java.util.List<DomainEvent> events) {

        Objects.requireNonNull(events, "事件列表不能為空");

        for (DomainEvent event : events) {
            publishEvent(sourceAggregate, event);
        }
    }

    /**
     * 執行跨聚合根操作並發布事件
     * 
     * @param sourceAggregate 源聚合根
     * @param operation       要執行的操作
     * @param eventGenerator  事件產生器
     * @param <T>             聚合根類型
     * @param <R>             操作結果類型
     * @return 操作結果
     */
    public static <T extends AggregateRootInterface, R> R executeAndPublish(
            T sourceAggregate,
            java.util.function.Supplier<R> operation,
            Function<R, DomainEvent> eventGenerator) {

        // 執行操作
        R result = operation.get();

        // 產生並發布事件
        DomainEvent event = eventGenerator.apply(result);
        publishEvent(sourceAggregate, event);

        return result;
    }

    /**
     * 執行跨聚合根操作並條件性發布事件
     * 
     * @param sourceAggregate 源聚合根
     * @param operation       要執行的操作
     * @param eventGenerator  事件產生器
     * @param condition       發布條件
     * @param <T>             聚合根類型
     * @param <R>             操作結果類型
     * @return 操作結果
     */
    public static <T extends AggregateRootInterface, R> R executeAndPublishIf(
            T sourceAggregate,
            java.util.function.Supplier<R> operation,
            Function<R, DomainEvent> eventGenerator,
            java.util.function.Predicate<R> condition) {

        // 執行操作
        R result = operation.get();

        // 條件性發布事件
        if (condition.test(result)) {
            DomainEvent event = eventGenerator.apply(result);
            publishEvent(sourceAggregate, event);
        }

        return result;
    }

    /**
     * 驗證事件源
     * 
     * @param sourceAggregate 源聚合根
     * @param event           事件
     */
    private static void validateEventSource(AggregateRootInterface sourceAggregate, DomainEvent event) {
        // 這裡可以添加更嚴格的驗證邏輯
        // 例如檢查事件的聚合根ID是否與源聚合根的ID匹配

        // 基本驗證：確保事件有聚合根ID
        String eventAggregateId = event.getAggregateId();
        if (eventAggregateId == null || eventAggregateId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "跨聚合根事件必須包含有效的聚合根ID: " + event.getClass().getSimpleName());
        }
    }

    /**
     * 跨聚合根操作上下文
     * 
     * 用於追蹤和記錄跨聚合根操作
     */
    public static class OperationContext {
        private final String sourceAggregateType;
        private final String sourceAggregateId;
        private final String operationName;
        private final java.time.LocalDateTime timestamp;

        public OperationContext(String sourceAggregateType, String sourceAggregateId, String operationName) {
            this.sourceAggregateType = Objects.requireNonNull(sourceAggregateType, "源聚合根類型不能為空");
            this.sourceAggregateId = Objects.requireNonNull(sourceAggregateId, "源聚合根ID不能為空");
            this.operationName = Objects.requireNonNull(operationName, "操作名稱不能為空");
            this.timestamp = java.time.LocalDateTime.now();
        }

        public String getSourceAggregateType() {
            return sourceAggregateType;
        }

        public String getSourceAggregateId() {
            return sourceAggregateId;
        }

        public String getOperationName() {
            return operationName;
        }

        public java.time.LocalDateTime getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return String.format("CrossAggregateOperation{%s[%s].%s @ %s}",
                    sourceAggregateType, sourceAggregateId, operationName, timestamp);
        }
    }

    /**
     * 跨聚合根操作建構器
     */
    public static class Builder<T extends AggregateRootInterface> {
        private final T sourceAggregate;
        private final java.util.List<DomainEvent> events = new java.util.ArrayList<>();

        public Builder(T sourceAggregate) {
            this.sourceAggregate = Objects.requireNonNull(sourceAggregate, "源聚合根不能為空");
        }

        public Builder<T> addEvent(DomainEvent event) {
            events.add(Objects.requireNonNull(event, "事件不能為空"));
            return this;
        }

        public Builder<T> addEventIf(boolean condition, java.util.function.Supplier<DomainEvent> eventSupplier) {
            if (condition) {
                events.add(eventSupplier.get());
            }
            return this;
        }

        public void execute() {
            publishEvents(sourceAggregate, events);
        }

        public int getEventCount() {
            return events.size();
        }

        public boolean hasEvents() {
            return !events.isEmpty();
        }
    }

    /**
     * 創建跨聚合根操作建構器
     * 
     * @param sourceAggregate 源聚合根
     * @param <T>             聚合根類型
     * @return 操作建構器
     */
    public static <T extends AggregateRootInterface> Builder<T> from(T sourceAggregate) {
        return new Builder<>(sourceAggregate);
    }
}