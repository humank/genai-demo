package solid.humank.genaidemo.ddd.lifecycle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.ddd.events.DomainEvent;

/**
 * 聚合生命週期感知接口
 * 為領域實體提供生命週期管理功能
 */
@Component
public class AggregateLifecycleAware {
    
    private static AggregateLifecycle lifecycle;

    @Autowired
    public void setAggregateLifecycle(AggregateLifecycle lifecycle) {
        AggregateLifecycleAware.lifecycle = lifecycle;
    }

    /**
     * 提供給領域實體使用的靜態方法
     */
    public static void apply(DomainEvent event) {
        if (lifecycle == null) {
            throw new IllegalStateException("AggregateLifecycle not initialized");
        }
        lifecycle.apply(event);
    }

    public static void commit() {
        if (lifecycle == null) {
            throw new IllegalStateException("AggregateLifecycle not initialized");
        }
        lifecycle.commit();
    }

    public static void rollback() {
        if (lifecycle == null) {
            throw new IllegalStateException("AggregateLifecycle not initialized");
        }
        lifecycle.rollback();
    }

    public static void clear() {
        if (lifecycle == null) {
            throw new IllegalStateException("AggregateLifecycle not initialized");
        }
        lifecycle.clear();
    }
}
