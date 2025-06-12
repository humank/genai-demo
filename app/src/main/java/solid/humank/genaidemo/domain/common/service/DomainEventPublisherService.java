package solid.humank.genaidemo.domain.common.service;

import org.springframework.stereotype.Service;
import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.event.DomainEventBus;

import java.util.logging.Logger;

/**
 * 領域事件發布服務
 * 用於發布領域事件
 */
@Service
public class DomainEventPublisherService {
    
    private static DomainEventBus eventBus;
    
    public DomainEventPublisherService(DomainEventBus eventBus) {
        DomainEventPublisherService.eventBus = eventBus;
    }
    
    /**
     * 發布領域事件
     * 
     * @param event 領域事件
     */
    public void publish(DomainEvent event) {
        if (eventBus != null) {
            eventBus.publish(event);
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
                  .warning("EventBus is null, event not published: " + 
                          (event != null ? event.getClass().getSimpleName() : "null"));
        }
    }
}