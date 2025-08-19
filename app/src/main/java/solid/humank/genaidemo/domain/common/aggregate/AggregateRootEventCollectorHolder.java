package solid.humank.genaidemo.domain.common.aggregate;

import java.util.Map;
import java.util.WeakHashMap;
import solid.humank.genaidemo.domain.common.event.DomainEventCollector;
import solid.humank.genaidemo.domain.common.event.SimpleDomainEventCollector;

/**
 * 聚合根事件收集器持有者
 * 使用 WeakHashMap 確保聚合根實例被 GC 時，對應的事件收集器也會被清理
 * 線程安全的實作，支援多線程環境
 */
public final class AggregateRootEventCollectorHolder {
    
    // 使用 WeakHashMap 避免記憶體洩漏
    private static final Map<Object, DomainEventCollector> EVENT_COLLECTORS = 
        new WeakHashMap<>();
    
    private AggregateRootEventCollectorHolder() {
        // 工具類別，禁止實例化
    }
    
    /**
     * 獲取聚合根對應的事件收集器
     * 如果不存在則創建新的收集器
     *
     * @param aggregateRoot 聚合根實例
     * @return 事件收集器
     */
    public static synchronized DomainEventCollector getEventCollector(Object aggregateRoot) {
        return EVENT_COLLECTORS.computeIfAbsent(aggregateRoot, 
            key -> new SimpleDomainEventCollector());
    }
    
    /**
     * 清除指定聚合根的事件收集器
     * 主要用於測試環境的清理
     *
     * @param aggregateRoot 聚合根實例
     */
    public static synchronized void clearEventCollector(Object aggregateRoot) {
        EVENT_COLLECTORS.remove(aggregateRoot);
    }
    
    /**
     * 清除所有事件收集器
     * 主要用於測試環境的全局清理
     */
    public static synchronized void clearAllEventCollectors() {
        EVENT_COLLECTORS.clear();
    }
}