package solid.humank.genaidemo.infrastructure.event.handler.customer;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import solid.humank.genaidemo.domain.customer.model.events.CustomerCreatedEvent;
import solid.humank.genaidemo.infrastructure.event.handler.AbstractDomainEventHandler;

/**
 * 客戶創建事件處理器
 * 處理客戶創建事件，執行歡迎流程和初始化設定
 * 
 * 需求 6.2: 建立事件處理器接收和處理事件的機制
 * 需求 6.5: 建立跨聚合根事件通信的業務邏輯執行
 */
@Component
public class CustomerCreatedEventHandler extends AbstractDomainEventHandler<CustomerCreatedEvent> {

    @Override
    @Transactional
    public void handle(CustomerCreatedEvent event) {
        // 跨聚合根業務邏輯：新客戶歡迎流程

        // 1. 記錄新客戶創建日誌
        System.out.println(String.format(
                "New customer created: %s (%s) with membership level: %s",
                event.customerName().getName(),
                event.email().getEmail(),
                event.membershipLevel()));

        // 2. 觸發歡迎郵件發送（模擬）
        sendWelcomeEmail(event);

        // 3. 創建客戶統計記錄（模擬）
        createCustomerStats(event);

        // 4. 初始化客戶偏好設定（模擬）
        initializeCustomerPreferences(event);
    }

    @Override
    public Class<CustomerCreatedEvent> getSupportedEventType() {
        return CustomerCreatedEvent.class;
    }

    /**
     * 發送歡迎郵件（模擬實現）
     */
    private void sendWelcomeEmail(CustomerCreatedEvent event) {
        // 在實際實現中，這裡會調用郵件服務或消息隊列
        System.out.println(String.format(
                "Sending welcome email to %s (%s)",
                event.customerName().getName(),
                event.email().getEmail()));
    }

    /**
     * 創建客戶統計記錄（模擬實現）
     */
    private void createCustomerStats(CustomerCreatedEvent event) {
        // 在實際實現中，這裡會調用統計服務創建客戶統計記錄
        System.out.println(String.format(
                "Creating customer stats record for customer: %s",
                event.customerId().getValue()));
    }

    /**
     * 初始化客戶偏好設定（模擬實現）
     */
    private void initializeCustomerPreferences(CustomerCreatedEvent event) {
        // 在實際實現中，這裡會設定客戶的預設偏好
        System.out.println(String.format(
                "Initializing default preferences for customer: %s",
                event.customerId().getValue()));
    }
}