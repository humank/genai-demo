package solid.humank.genaidemo.application.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知應用服務
 * 
 * 負責處理各種通知相關的業務邏輯
 * 協調領域服務和基礎設施服務
 * 
 * 需求 8.3: 支援 CustomerCreatedEvent 到通知 bounded context 的事件流轉
 */
@Service
public class NotificationApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationApplicationService.class);

    /**
     * 發送歡迎郵件
     * 
     * @param customerId      客戶ID
     * @param customerName    客戶姓名
     * @param email           電子郵件
     * @param membershipLevel 會員等級
     */
    @Transactional
    public void sendWelcomeEmail(String customerId, String customerName, String email, String membershipLevel) {
        LOGGER.info("發送歡迎郵件 - 客戶ID: {}, 姓名: {}, 電子郵件: {}, 會員等級: {}",
                customerId, customerName, email, membershipLevel);

        // 實際實作中，這裡會調用通知領域服務或基礎設施服務
        // 例如：notificationDomainService.createWelcomeNotification(...)
        // 或：emailService.sendWelcomeEmail(...)

        // 模擬發送歡迎郵件
        String welcomeMessage = String.format(
                "親愛的 %s，歡迎加入我們！您的會員等級是 %s。",
                customerName, membershipLevel);

        LOGGER.info("歡迎郵件內容: {}", welcomeMessage);
        LOGGER.info("歡迎郵件已發送至: {}", email);
    }

    /**
     * 發送訂單通知
     * 
     * @param customerId  客戶ID
     * @param orderId     訂單ID
     * @param orderStatus 訂單狀態
     */
    @Transactional
    public void sendOrderNotification(String customerId, String orderId, String orderStatus) {
        LOGGER.info("發送訂單通知 - 客戶ID: {}, 訂單ID: {}, 狀態: {}",
                customerId, orderId, orderStatus);

        // 實際實作中，這裡會根據訂單狀態發送不同的通知
        String notificationMessage = String.format(
                "您的訂單 %s 狀態已更新為: %s",
                orderId, orderStatus);

        LOGGER.info("訂單通知內容: {}", notificationMessage);
    }

    /**
     * 發送促銷通知
     * 
     * @param customerId       客戶ID
     * @param promotionName    促銷名稱
     * @param promotionDetails 促銷詳情
     */
    @Transactional
    public void sendPromotionNotification(String customerId, String promotionName, String promotionDetails) {
        LOGGER.info("發送促銷通知 - 客戶ID: {}, 促銷名稱: {}", customerId, promotionName);

        String notificationMessage = String.format(
                "新促銷活動: %s - %s",
                promotionName, promotionDetails);

        LOGGER.info("促銷通知內容: {}", notificationMessage);
    }
}