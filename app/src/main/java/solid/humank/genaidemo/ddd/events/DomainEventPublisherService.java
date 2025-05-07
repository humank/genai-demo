package solid.humank.genaidemo.ddd.events;

import org.springframework.stereotype.Service;

import solid.humank.genaidemo.utils.SpringContextHolder;

/**
 * 領域事件發布服務
 * 提供統一的事件發布機制
 */
@Service
public class DomainEventPublisherService {
    private final DomainEventBus eventBus;

    
    public DomainEventPublisherService(DomainEventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * 發布領域事件
     * 
     * @param event 要發布的領域事件
     */
    public void publish(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("事件不能為空");
        }
        eventBus.publish(event);
    }
    
    /**
     * 提供靜態訪問方法
     * 
     * @param event 要發布的領域事件
     */
    public static void publishEvent(DomainEvent event) {
        SpringContextHolder.getBean(DomainEventPublisherService.class).publish(event);
    }
}
