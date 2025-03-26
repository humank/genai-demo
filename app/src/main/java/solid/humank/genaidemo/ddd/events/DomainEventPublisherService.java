package solid.humank.genaidemo.ddd.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 領域事件發布服務
 * 提供統一的事件發布機制
 */
@Service
public class DomainEventPublisherService {
    private final DomainEventBus eventBus;

    @Autowired
    public DomainEventPublisherService(DomainEventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void publish(DomainEvent event) {
        eventBus.publish(event);
    }
}
