package solid.humank.genaidemo.domain.common.event;

import java.util.List;

/** 領域事件發布器接口 定義在領域層，由基礎設施層實現 */
public interface DomainEventPublisher {

    /**
     * 發布領域事件
     *
     * @param event 領域事件
     */
    void publish(DomainEvent event);

    /**
     * 批量發布領域事件
     *
     * @param events 領域事件列表
     */
    void publishAll(List<DomainEvent> events);
}
