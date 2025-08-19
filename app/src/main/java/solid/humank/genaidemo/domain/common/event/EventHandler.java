package solid.humank.genaidemo.domain.common.event;

/**
 * 通用事件處理器接口
 * 用於 EventBus 的通用事件處理機制
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