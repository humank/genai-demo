package solid.humank.genaidemo.ddd.lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import solid.humank.genaidemo.ddd.events.DomainEvent;
import solid.humank.genaidemo.ddd.events.DomainEventBus;
import solid.humank.genaidemo.utils.SpringContextHolder;

/**
 * 聚合生命週期管理器
 * 處理聚合的事件發布和狀態變更
 */
@Service
public class AggregateLifecycle {
    private static final ThreadLocal<List<DomainEvent>> PENDING_EVENTS = ThreadLocal.withInitial(ArrayList::new);
    
    @Autowired
    private DomainEventBus eventBus;

    /**
     * 套用聚合狀態變更並發布事件
     */
    public void apply(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        // 將事件加入待處理列表
        PENDING_EVENTS.get().add(event);
    }

    /**
     * 提交所有待處理的事件
     */
    public void commit() {
        List<DomainEvent> events = PENDING_EVENTS.get();
        try {
            // 發布所有待處理的事件
            events.forEach(eventBus::publish);
        } finally {
            // 清除待處理事件
            events.clear();
        }
    }

    /**
     * 取消所有待處理的事件
     */
    public void rollback() {
        PENDING_EVENTS.get().clear();
    }

    /**
     * 取得目前待處理的事件
     */
    public List<DomainEvent> getPendingEvents() {
        return new ArrayList<>(PENDING_EVENTS.get());
    }

    /**
     * 清理 ThreadLocal 資源
     */
    public void clear() {
        PENDING_EVENTS.remove();
    }
    
    /**
     * 獲取 ThreadLocal 中的待處理事件列表
     * 這是唯一需要保持靜態的方法，因為 ThreadLocal 本身是靜態的
     */
    public static List<DomainEvent> getCurrentThreadPendingEvents() {
        return PENDING_EVENTS.get();
    }
    
    /**
     * 清理當前線程的 ThreadLocal 資源
     * 這是唯一需要保持靜態的方法，因為 ThreadLocal 本身是靜態的
     */
    public static void clearCurrentThreadEvents() {
        PENDING_EVENTS.remove();
    }

    /**
     * 用於標記聚合根要使用生命週期管理的註解
     */
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
    public @interface ManagedLifecycle {}

    /**
     * 用於標記聚合根中會產生事件的方法
     */
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
    public @interface EventSourcing {}
}
