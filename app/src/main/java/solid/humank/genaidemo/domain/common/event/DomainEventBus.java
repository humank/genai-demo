package solid.humank.genaidemo.domain.common.event;

import org.springframework.stereotype.Component;
import solid.humank.genaidemo.infrastructure.event.DomainEventPublisherAdapter;

/** 領域事件總線 用於發布領域事件 */
@Component
public class DomainEventBus {

    private final DomainEventPublisherAdapter eventPublisherAdapter;

    public DomainEventBus(DomainEventPublisherAdapter eventPublisherAdapter) {
        this.eventPublisherAdapter = eventPublisherAdapter;
    }

    /**
     * 發布領域事件
     *
     * @param event 領域事件
     */
    public void publish(DomainEvent event) {
        if (event != null) {
            eventPublisherAdapter.publish(event);
        }
    }
}
