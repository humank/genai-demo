package solid.humank.genaidemo.domain.common.event;

import solid.humank.genaidemo.domain.common.annotations.DomainService;

/** 領域事件總線 用於發布領域事件 */
@DomainService(name = "DomainEventBus", description = "領域事件總線，用於發布領域事件", boundedContext = "Common")
public class DomainEventBus {

    private final DomainEventPublisher eventPublisher;

    public DomainEventBus(DomainEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 發布領域事件
     *
     * @param event 領域事件
     */
    public void publish(DomainEvent event) {
        if (event != null) {
            eventPublisher.publish(event);
        }
    }
}
