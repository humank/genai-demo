package solid.humank.genaidemo.domain.common.events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 領域事件總線
 * 用於發布領域事件
 */
@Component
public class DomainEventBus {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public DomainEventBus(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * 發布領域事件
     * 
     * @param event 領域事件
     */
    public void publish(DomainEvent event) {
        if (event != null) {
            eventPublisher.publishEvent(event);
        }
    }
}