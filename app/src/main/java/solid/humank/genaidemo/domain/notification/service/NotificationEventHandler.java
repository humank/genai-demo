package solid.humank.genaidemo.domain.notification.service;

import org.springframework.stereotype.Service;
import solid.humank.genaidemo.domain.common.event.EventSubscriber;
import solid.humank.genaidemo.domain.notification.model.events.NotificationCreatedEvent;
import solid.humank.genaidemo.domain.notification.model.events.NotificationStatusChangedEvent;

import java.util.logging.Logger;

/**
 * 通知事件處理服務
 * 處理通知相關的領域事件
 */
@Service
public class NotificationEventHandler {
    private static final Logger LOGGER = Logger.getLogger(NotificationEventHandler.class.getName());
    
    /**
     * 處理通知創建事件
     * 
     * @param event 通知創建事件
     */
    @EventSubscriber(NotificationCreatedEvent.class)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        LOGGER.info(() -> String.format(
            "處理通知創建事件: 通知ID=%s, 客戶ID=%s, 類型=%s, 主題=%s, 渠道=%s",
            event.getNotificationId(), event.getCustomerId(), 
            event.getType(), event.getSubject(), event.getChannels()));
            
        // 這裡可以添加通知創建後的業務邏輯
        // 例如：記錄統計數據、觸發外部系統等
    }
    
    /**
     * 處理通知狀態變更事件
     * 
     * @param event 通知狀態變更事件
     */
    @EventSubscriber(NotificationStatusChangedEvent.class)
    public void handleNotificationStatusChanged(NotificationStatusChangedEvent event) {
        LOGGER.info(() -> String.format(
            "處理通知狀態變更事件: 通知ID=%s, 客戶ID=%s, 狀態變更=%s->%s, 原因=%s",
            event.getNotificationId(), event.getCustomerId(), 
            event.getOldStatus(), event.getNewStatus(), event.getReason()));
            
        // 這裡可以添加通知狀態變更後的業務邏輯
        // 例如：更新統計數據、觸發後續流程等
    }
}