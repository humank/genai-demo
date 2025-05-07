package solid.humank.genaidemo.ddd.events;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.exceptions.BusinessException;

/**
 * 領域事件發布器
 * 統一處理事件發布邏輯，提供錯誤處理和日誌記錄
 */
@Component
public class DomainEventPublisher {
    private static final Logger LOGGER = Logger.getLogger(DomainEventPublisher.class.getName());
    
    private final DomainEventBus eventBus;
    
    /**
     * 構造函數
     * 
     * @param eventBus 領域事件匯流排
     */
    @Autowired
    public DomainEventPublisher(DomainEventBus eventBus) {
        this.eventBus = eventBus;
    }
    
    /**
     * 發布領域事件
     * 
     * @param event 要發布的領域事件
     * @throws IllegalArgumentException 如果事件為 null
     * @throws BusinessException 如果事件發布失敗
     */
    public void publish(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("事件不能為空");
        }
        
        try {
            LOGGER.fine("Publishing event: " + event.getClass().getSimpleName());
            eventBus.publish(event);
            LOGGER.fine("Event published successfully: " + event.getClass().getSimpleName());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to publish event: " + event.getClass().getSimpleName(), e);
            throw new BusinessException("事件發布失敗: " + e.getMessage());
        }
    }
}
