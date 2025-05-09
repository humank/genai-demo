package solid.humank.genaidemo.domain.common.lifecycle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import solid.humank.genaidemo.domain.common.events.DomainEvent;
import solid.humank.genaidemo.domain.common.events.DomainEventBus;

/**
 * 聚合生命週期管理器
 * 處理聚合的事件發布和狀態變更
 */
@Service
public class AggregateLifecycle {
    private static final Logger LOGGER = Logger.getLogger(AggregateLifecycle.class.getName());
    private static final ThreadLocal<List<DomainEvent>> PENDING_EVENTS = ThreadLocal.withInitial(ArrayList::new);
    
    private final DomainEventBus eventBus;

    /**
     * 構造函數
     * 
     * @param eventBus 領域事件匯流排
     */
    public AggregateLifecycle(DomainEventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * 套用聚合狀態變更並發布事件
     * 
     * @param event 要應用的領域事件
     * @throws IllegalArgumentException 如果事件為 null
     */
    public void apply(DomainEvent event) {
        Objects.requireNonNull(event, "Event cannot be null");
        
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(() -> String.format("Applying event: %s", event.getClass().getSimpleName()));
        }
        
        // 將事件加入待處理列表
        PENDING_EVENTS.get().add(event);
    }

    /**
     * 提交所有待處理的事件
     */
    public void commit() {
        List<DomainEvent> events = PENDING_EVENTS.get();
        
        if (events.isEmpty()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("No pending events to commit");
            }
            return;
        }
        
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(() -> String.format("Committing %d pending events", events.size()));
        }
        
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
        List<DomainEvent> events = PENDING_EVENTS.get();
        
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(() -> String.format("Rolling back %d pending events", events.size()));
        }
        
        events.clear();
    }

    /**
     * 取得目前待處理的事件
     * 
     * @return 待處理事件的副本列表
     */
    public List<DomainEvent> getPendingEvents() {
        return new ArrayList<>(PENDING_EVENTS.get());
    }

    /**
     * 清理 ThreadLocal 資源
     */
    public void clear() {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Clearing ThreadLocal resources");
        }
        
        PENDING_EVENTS.remove();
    }
    
    /**
     * 獲取 ThreadLocal 中的待處理事件列表
     * 這是唯一需要保持靜態的方法，因為 ThreadLocal 本身是靜態的
     * 
     * @return 當前線程的待處理事件列表
     */
    public static List<DomainEvent> getCurrentThreadPendingEvents() {
        return PENDING_EVENTS.get();
    }
    
    /**
     * 清理當前線程的 ThreadLocal 資源
     * 這是唯一需要保持靜態的方法，因為 ThreadLocal 本身是靜態的
     */
    public static void clearCurrentThreadEvents() {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Clearing current thread events");
        }
        
        PENDING_EVENTS.remove();
    }

    /**
     * 用於標記聚合根要使用生命週期管理的註解
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ManagedLifecycle {}

    /**
     * 用於標記聚合根中會產生事件的方法
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface EventSourcing {}
}
