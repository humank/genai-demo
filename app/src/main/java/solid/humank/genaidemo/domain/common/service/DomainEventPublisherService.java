package solid.humank.genaidemo.domain.common.service;

import java.util.logging.Logger;

import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.event.DomainEventBus;

/** 領域事件發布服務 用於發布領域事件 */
@DomainService
public class DomainEventPublisherService {

    private static final DomainEventBus eventBus;

    static {
        // Initialize with a default implementation or null
        eventBus = null;
    }

    private final DomainEventBus instanceEventBus;

    public DomainEventPublisherService(DomainEventBus eventBus) {
        this.instanceEventBus = eventBus;
    }

    /**
     * 發布領域事件
     *
     * @param event 領域事件
     */
    public void publish(DomainEvent event) {
        if (instanceEventBus != null) {
            instanceEventBus.publish(event);
        }
    }

    /**
     * 靜態方法發布領域事件
     *
     * @param event 領域事件
     */
    public static void publishEvent(DomainEvent event) {
        if (eventBus != null) {
            eventBus.publish(event);
        } else {
            Logger.getLogger(DomainEventPublisherService.class.getName())
                    .warning(
                            "EventBus is null, event not published: "
                                    + (event != null ? event.getClass().getSimpleName() : "null"));
        }
    }
}
