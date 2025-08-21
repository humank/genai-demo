package solid.humank.genaidemo.domain.common.event;

/** 事件總線接口 用於發布和訂閱領域事件 */
public interface EventBus {

    /**
     * 發布事件
     *
     * @param event 領域事件
     */
    void publish(Object event);

    /**
     * 訂閱事件
     *
     * @param eventType 事件類型
     * @param handler   事件處理器
     * @param <T>       事件類型
     */
    <T> void subscribe(Class<T> eventType, EventHandler<T> handler);

    /**
     * 取消訂閱事件
     *
     * @param eventType 事件類型
     * @param handler   事件處理器
     * @param <T>       事件類型
     */
    <T> void unsubscribe(Class<T> eventType, EventHandler<T> handler);
}
