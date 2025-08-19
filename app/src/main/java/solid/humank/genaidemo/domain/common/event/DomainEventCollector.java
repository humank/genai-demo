package solid.humank.genaidemo.domain.common.event;

import java.util.List;

/** 領域事件收集器介面 純 Domain Layer 介面，不依賴任何框架 用於聚合根收集和管理領域事件 */
public interface DomainEventCollector {

    /**
     * 收集領域事件
     *
     * @param event 要收集的領域事件
     */
    void collectEvent(DomainEvent event);

    /**
     * 獲取所有未提交的事件
     *
     * @return 未提交事件的副本列表
     */
    List<DomainEvent> getUncommittedEvents();

    /** 標記事件為已提交 清除所有未提交的事件 */
    void markEventsAsCommitted();

    /**
     * 檢查是否有未提交的事件
     *
     * @return 如果有未提交事件返回 true
     */
    boolean hasUncommittedEvents();
}
