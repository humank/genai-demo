package solid.humank.genaidemo.infrastructure.event.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import solid.humank.genaidemo.application.notification.service.NotificationApplicationService;
import solid.humank.genaidemo.domain.common.event.EventSubscriber;
import solid.humank.genaidemo.domain.customer.model.events.CustomerCreatedEvent;
import solid.humank.genaidemo.domain.notification.model.events.NotificationCreatedEvent;
import solid.humank.genaidemo.infrastructure.event.publisher.DomainEventPublisherAdapter;

/**
 * 通知事件處理器
 * 
 * 位於基礎設施層，處理來自其他 bounded context 的事件
 * 只調用應用服務，不直接操作領域物件
 * 
 * 需求 8.3: 實現 CustomerCreatedEvent 到通知 bounded context 的事件流轉驗證
 * 需求 8.7: 確保所有事件處理器位於基礎設施層並只調用應用服務或領域服務
 */
@Component
public class NotificationEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationEventHandler.class);

    private final NotificationApplicationService notificationApplicationService;

    public NotificationEventHandler(NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    /**
     * 處理通知創建事件
     *
     * @param event 通知創建事件
     */
    @EventSubscriber(NotificationCreatedEvent.class)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        LOGGER.info("處理通知創建事件: 通知ID={}, 客戶ID={}, 類型={}, 主題={}",
                event.notificationId(), event.customerId(), event.type(), event.subject());

        // 這裡可以添加通知創建後的業務邏輯
        // 例如：記錄通知歷史、更新統計等
    }

    /**
     * 處理客戶創建事件
     * 在事務提交後發送歡迎通知
     * 
     * 需求 8.3: CustomerCreatedEvent 到通知 bounded context 的事件流轉
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCustomerCreated(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        if (wrapper.getSource() instanceof CustomerCreatedEvent) {
            CustomerCreatedEvent event = (CustomerCreatedEvent) wrapper.getSource();

            LOGGER.info("處理客戶創建事件，發送歡迎通知 - 客戶ID: {}, 客戶姓名: {}, 電子郵件: {}",
                    event.getAggregateId(), event.customerName().getName(), event.email().getEmail());

            try {
                // 調用應用服務發送歡迎通知
                sendWelcomeNotification(
                        event.customerId().getId(),
                        event.customerName().getName(),
                        event.email().getEmail(),
                        event.membershipLevel().name());

                LOGGER.info("歡迎通知發送完成 - 客戶ID: {}", event.getAggregateId());

            } catch (Exception e) {
                LOGGER.error("處理客戶創建事件時發生錯誤 - 客戶ID: {}, 錯誤: {}",
                        event.getAggregateId(), e.getMessage(), e);
                // 這裡可以實作補償機制或重試邏輯
            }
        }
    }

    /**
     * 發送歡迎通知
     * 
     * @param customerId      客戶ID
     * @param customerName    客戶姓名
     * @param email           電子郵件
     * @param membershipLevel 會員等級
     */
    private void sendWelcomeNotification(String customerId, String customerName, String email, String membershipLevel) {
        LOGGER.info("發送歡迎通知 - 客戶ID: {}, 姓名: {}, 電子郵件: {}, 會員等級: {}",
                customerId, customerName, email, membershipLevel);

        // 實際調用通知應用服務
        notificationApplicationService.sendWelcomeEmail(customerId, customerName, email, membershipLevel);
    }
}