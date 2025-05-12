package solid.humank.genaidemo.domain.common.event;

/**
 * 事件處理器接口
 * 
 * @param <T> 事件類型
 */
@FunctionalInterface
public interface EventHandler<T> {
    
    /**
     * 處理事件
     * 
     * @param event 事件
     */
    void handle(T event);
}