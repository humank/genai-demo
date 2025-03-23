package solid.humank.genaidemo.ddd.events;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.exceptions.BusinessException;

/**
 * 領域事件發布器
 * 統一處理事件發布邏輯，提供錯誤處理和日誌記錄
 */
@Component
public class DomainEventPublisher {
    private final DomainEventBus eventBus;
    
    public DomainEventPublisher(DomainEventBus eventBus) {
        this.eventBus = eventBus;
    }
    
    /**
     * 發布領域事件
     * 
     * @param event 要發布的領域事件
     * @throws BusinessException 如果事件發布失敗
     */
    public void publish(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("事件不能為空");
        }
        
        try {
            eventBus.publish(event);
        } catch (Exception e) {
            throw new BusinessException("事件發布失敗: " + e.getMessage());
        }
    }
}
