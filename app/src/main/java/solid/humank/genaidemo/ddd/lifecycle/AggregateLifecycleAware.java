package solid.humank.genaidemo.ddd.lifecycle;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.ddd.events.DomainEvent;
import solid.humank.genaidemo.utils.SpringContextHolder;

/**
 * 聚合生命週期感知接口
 * 為領域實體提供生命週期管理功能
 * 
 * 注意：這個類提供了靜態方法作為便利方法，但內部使用 SpringContextHolder 獲取實例
 * 這樣設計是為了讓領域實體可以方便地使用生命週期管理功能，而不需要注入 AggregateLifecycle
 */
@Component
public class AggregateLifecycleAware {
    
    /**
     * 提供給領域實體使用的靜態方法
     * 內部使用 SpringContextHolder 獲取 AggregateLifecycle 實例
     */
    public static void apply(DomainEvent event) {
        getLifecycle().apply(event);
    }

    public static void commit() {
        getLifecycle().commit();
    }

    public static void rollback() {
        getLifecycle().rollback();
    }

    public static void clear() {
        getLifecycle().clear();
    }
    
    /**
     * 獲取 AggregateLifecycle 實例
     * 直接從 Spring 上下文獲取實例
     */
    private static AggregateLifecycle getLifecycle() {
        // 從 Spring 上下文獲取實例
        AggregateLifecycle lifecycle = SpringContextHolder.getBean(AggregateLifecycle.class);
        if (lifecycle == null) {
            throw new IllegalStateException("AggregateLifecycle not initialized");
        }
        
        return lifecycle;
    }
    
    /**
     * 獲取當前線程的待處理事件
     */
    public static java.util.List<DomainEvent> getPendingEvents() {
        return AggregateLifecycle.getCurrentThreadPendingEvents();
    }
    
    /**
     * 清理當前線程的 ThreadLocal 資源
     */
    public static void clearThreadLocalEvents() {
        AggregateLifecycle.clearCurrentThreadEvents();
    }
}
