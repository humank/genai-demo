package solid.humank.genaidemo.infrastructure.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * 領域事件發布適配器
 * 將領域事件轉換為Spring應用事件
 */
@Component
public class DomainEventPublisherAdapter {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public DomainEventPublisherAdapter(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * 發布領域事件
     * 將領域事件包裝為Spring應用事件
     * 
     * @param event 領域事件
     */
    public void publish(DomainEvent event) {
        if (event != null) {
            eventPublisher.publishEvent(new DomainEventWrapper(event));
        }
    }
    
    /**
     * 領域事件包裝器
     * 將領域事件包裝為Spring應用事件
     */
    public static class DomainEventWrapper extends org.springframework.context.ApplicationEvent {
        private static final long serialVersionUID = 1L;
        
        public DomainEventWrapper(DomainEvent event) {
            super(event);
        }
        
        @Override
        public DomainEvent getSource() {
            return (DomainEvent) super.getSource();
        }
    }
}