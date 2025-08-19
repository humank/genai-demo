package solid.humank.genaidemo.domain.common.lifecycle;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * 聚合生命週期感知接口 為領域實體提供生命週期管理功能
 *
 * <p>
 * 注意：這個類提供了靜態方法作為便利方法，通過靜態實例提供服務
 * 這樣設計是為了讓領域實體可以方便地使用生命週期管理功能，而不需要注入 AggregateLifecycle
 */
public class AggregateLifecycleAware {
    private static final Logger LOGGER = Logger.getLogger(AggregateLifecycleAware.class.getName());

    private static AggregateLifecycle lifecycleInstance;

    private AggregateLifecycleAware() {
        // 私有構造函數，防止實例化
    }

    /**
     * 設置AggregateLifecycle實例
     * 這個方法應該在應用啟動時由基礎設施層調用
     */
    public static void setLifecycleInstance(AggregateLifecycle lifecycle) {
        lifecycleInstance = lifecycle;
    }

    /**
     * 提供給領域實體使用的靜態方法 內部使用 SpringContextHolder 獲取 AggregateLifecycle 實例
     *
     * @param event 要應用的領域事件
     * @throws IllegalArgumentException 如果事件為 null
     * @throws IllegalStateException    如果 AggregateLifecycle 未初始化
     */
    public static void apply(DomainEvent event) {
        Objects.requireNonNull(event, "Event cannot be null");

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    String.format(
                            "Applying event via static method: %s",
                            event.getClass().getSimpleName()));
        }

        getLifecycle().apply(event);
    }

    /**
     * 提交所有待處理的事件
     *
     * @throws IllegalStateException 如果 AggregateLifecycle 未初始化
     */
    public static void commit() {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Committing events via static method");
        }

        getLifecycle().commit();
    }

    /**
     * 取消所有待處理的事件
     *
     * @throws IllegalStateException 如果 AggregateLifecycle 未初始化
     */
    public static void rollback() {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Rolling back events via static method");
        }

        getLifecycle().rollback();
    }

    /**
     * 清理 ThreadLocal 資源
     *
     * @throws IllegalStateException 如果 AggregateLifecycle 未初始化
     */
    public static void clear() {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Clearing resources via static method");
        }

        getLifecycle().clear();
    }

    /**
     * 獲取 AggregateLifecycle 實例
     *
     * @return AggregateLifecycle 實例
     * @throws IllegalStateException 如果 AggregateLifecycle 未初始化
     */
    private static AggregateLifecycle getLifecycle() {
        if (lifecycleInstance == null) {
            LOGGER.severe("AggregateLifecycle instance is null");
            throw new IllegalStateException("AggregateLifecycle not initialized");
        }
        return lifecycleInstance;
    }

    /**
     * 獲取當前線程的待處理事件
     *
     * @return 當前線程的待處理事件列表
     */
    public static List<DomainEvent> getPendingEvents() {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Getting pending events");
        }

        return AggregateLifecycle.getCurrentThreadPendingEvents();
    }

    /** 清理當前線程的 ThreadLocal 資源 */
    public static void clearThreadLocalEvents() {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Clearing thread local events");
        }

        AggregateLifecycle.clearCurrentThreadEvents();
    }
}
