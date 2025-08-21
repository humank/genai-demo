package solid.humank.genaidemo.domain.common.aggregate;

import java.util.List;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.event.DomainEventCollector;

/**
 * 聚合根介面 - 混搭方案：Annotation + Interface Default Methods
 * 
 * 優點：
 * 1. 編譯時約束：必須實作此介面，IDE 會提示
 * 2. 零 override：所有方法都有 default 實作
 * 3. 註解驅動：通過 @AggregateRoot 提供元數據
 * 4. 自動驗證：在 default 方法中自動檢查註解
 * 
 * 使用方式：
 * 
 * @AggregateRoot(name = "Customer", boundedContext = "Customer")
 *                     public class Customer implements AggregateRootInterface {
 *                     // 無需 override 任何方法！
 * 
 *                     public void updateProfile(String name) {
 *                     // 業務邏輯
 *                     this.name = name;
 * 
 *                     // 直接調用，有編譯時檢查
 *                     collectEvent(new CustomerProfileUpdatedEvent(id, name));
 *                     }
 *                     }
 */
public interface AggregateRootInterface {

    /**
     * 獲取事件收集器
     * 自動驗證 @AggregateRoot 註解，確保正確使用
     *
     * @return 事件收集器實例
     */
    default DomainEventCollector getEventCollector() {
        // 自動驗證註解
        validateAggregateRootAnnotation();

        // 檢查是否啟用事件收集
        if (!isEventCollectionEnabled()) {
            return new NoOpDomainEventCollector();
        }

        return AggregateRootEventCollectorHolder.getEventCollector(this);
    }

    /**
     * 收集領域事件
     * 在聚合根狀態變更時調用此方法收集事件
     * 自動進行註解驗證和配置檢查
     *
     * @param event 要收集的領域事件
     */
    default void collectEvent(DomainEvent event) {
        getEventCollector().collectEvent(event);
    }

    /**
     * 獲取所有未提交的事件
     * Application Layer 使用此方法獲取事件進行發布
     *
     * @return 未提交事件的副本列表
     */
    default List<DomainEvent> getUncommittedEvents() {
        return getEventCollector().getUncommittedEvents();
    }

    /**
     * 標記事件為已提交
     * Application Layer 在成功發布事件後調用此方法
     */
    default void markEventsAsCommitted() {
        getEventCollector().markEventsAsCommitted();
    }

    /**
     * 檢查是否有未提交的事件
     *
     * @return 如果有未提交事件返回 true
     */
    default boolean hasUncommittedEvents() {
        return getEventCollector().hasUncommittedEvents();
    }

    /**
     * 清除所有未提交的事件
     * 主要用於測試中的事件隔離
     */
    default void clearEvents() {
        getEventCollector().markEventsAsCommitted();
    }

    // === 註解驗證和元數據方法 ===

    /**
     * 獲取聚合根名稱
     * 從 @AggregateRoot 註解中讀取
     *
     * @return 聚合根名稱
     */
    default String getAggregateRootName() {
        AggregateRoot annotation = getAggregateRootAnnotation();
        return annotation.name().isEmpty() ? this.getClass().getSimpleName() : annotation.name();
    }

    /**
     * 獲取 Bounded Context
     * 從 @AggregateRoot 註解中讀取
     *
     * @return Bounded Context 名稱
     */
    default String getBoundedContext() {
        return getAggregateRootAnnotation().boundedContext();
    }

    /**
     * 獲取聚合根版本
     * 從 @AggregateRoot 註解中讀取
     *
     * @return 版本號
     */
    default String getVersion() {
        return getAggregateRootAnnotation().version();
    }

    // === 私有輔助方法 ===

    /**
     * 驗證 @AggregateRoot 註解
     * 確保實作類別正確標記了註解
     */
    private void validateAggregateRootAnnotation() {
        if (!this.getClass().isAnnotationPresent(AggregateRoot.class)) {
            throw new IllegalStateException(
                    "Class " + this.getClass().getName() +
                            " implements AggregateRootInterface but is not annotated with @AggregateRoot. " +
                            "Please add @AggregateRoot annotation to the class.");
        }
    }

    /**
     * 獲取 @AggregateRoot 註解
     *
     * @return 註解實例
     */
    private AggregateRoot getAggregateRootAnnotation() {
        return this.getClass().getAnnotation(AggregateRoot.class);
    }

    /**
     * 檢查是否啟用事件收集
     *
     * @return 如果啟用返回 true
     */
    private boolean isEventCollectionEnabled() {
        return getAggregateRootAnnotation().enableEventCollection();
    }

    /**
     * 空實作的事件收集器
     * 當事件收集被禁用時使用
     */
    class NoOpDomainEventCollector implements DomainEventCollector {
        @Override
        public void collectEvent(DomainEvent event) {
            // 不做任何事情
        }

        @Override
        public List<DomainEvent> getUncommittedEvents() {
            return List.of();
        }

        @Override
        public void markEventsAsCommitted() {
            // 不做任何事情
        }

        @Override
        public boolean hasUncommittedEvents() {
            return false;
        }
    }
}