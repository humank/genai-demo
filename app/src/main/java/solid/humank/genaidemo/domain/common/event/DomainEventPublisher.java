package solid.humank.genaidemo.domain.common.event;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    /**
     * 發布領域事件
     * 
     * @param event 要發布的領域事件
     * @throws IllegalArgumentException 如果事件為 null
     * @throws BusinessException 如果事件發布失敗
     */
    public void publish(DomainEvent event) {
        Objects.requireNonNull(event, "事件不能為空");
        
        String eventName = event.getClass().getSimpleName();
        
        try {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(() -> String.format("Publishing event: %s", eventName));
            }
            
            eventBus.publish(event);
            
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(() -> String.format("Event published successfully: %s", eventName));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Failed to publish event: %s", eventName), e);
            throw new BusinessException("事件發布失敗: " + e.getMessage(), e);
        }
    }
    
    /**
     * 批量發布領域事件
     * 
     * @param events 要發布的領域事件列表
     * @throws IllegalArgumentException 如果事件列表為 null
     * @throws BusinessException 如果任何事件發布失敗
     */
    public void publishAll(java.util.List<DomainEvent> events) {
        Objects.requireNonNull(events, "事件列表不能為空");
        
        // 使用 Java 21 的 SequencedCollection 接口方法
        if (events.isEmpty()) {
            return;
        }
        
        // 使用 Java 21 的增強 Stream API
        events.stream()
              .forEach(this::publish);
    }
}
