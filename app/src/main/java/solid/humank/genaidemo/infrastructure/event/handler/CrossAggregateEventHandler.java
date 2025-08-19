package solid.humank.genaidemo.infrastructure.event.handler;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import solid.humank.genaidemo.domain.customer.model.events.CustomerCreatedEvent;
import solid.humank.genaidemo.domain.customer.model.events.RewardPointsEarnedEvent;
import solid.humank.genaidemo.domain.shoppingcart.model.events.CartItemAddedEvent;
import solid.humank.genaidemo.infrastructure.event.publisher.DomainEventPublisherAdapter;

/** 跨聚合根事件處理器 處理需要跨聚合根協調的業務邏輯 */
@Component
public class CrossAggregateEventHandler {

    /** 處理客戶創建事件 在事務提交後發送歡迎通知 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCustomerCreated(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        if (wrapper.getSource() instanceof CustomerCreatedEvent) {
            CustomerCreatedEvent event = (CustomerCreatedEvent) wrapper.getSource();

            // 發送歡迎通知
            sendWelcomeNotification(event.customerId().getId());

            // 創建初始購物車
            createInitialShoppingCart(event.customerId().getId());
        }
    }

    /** 處理購物車商品添加事件 檢查庫存並發送提醒 */
    @EventListener
    public void handleCartItemAdded(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        if (wrapper.getSource() instanceof CartItemAddedEvent) {
            CartItemAddedEvent event = (CartItemAddedEvent) wrapper.getSource();

            // 檢查庫存
            checkInventoryAvailability(event.productId().getId(), event.quantity());

            // 檢查是否有相關促銷
            checkApplicablePromotions(event.cartId().value(), event.productId().getId());
        }
    }

    /** 處理紅利點數獲得事件 檢查是否達到會員升級條件 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRewardPointsEarned(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        if (wrapper.getSource() instanceof RewardPointsEarnedEvent) {
            RewardPointsEarnedEvent event = (RewardPointsEarnedEvent) wrapper.getSource();

            // 檢查會員升級條件
            checkMembershipUpgrade(event.customerId().getId(), event.totalPoints());
        }
    }

    // 私有輔助方法

    private void sendWelcomeNotification(String customerId) {
        // 實作發送歡迎通知的邏輯
        System.out.println("發送歡迎通知給客戶: " + customerId);
    }

    private void createInitialShoppingCart(String customerId) {
        // 實作創建初始購物車的邏輯
        System.out.println("為客戶創建初始購物車: " + customerId);
    }

    private void checkInventoryAvailability(String productId, int quantity) {
        // 實作檢查庫存可用性的邏輯
        System.out.println("檢查商品 " + productId + " 的庫存，需求數量: " + quantity);
    }

    private void checkApplicablePromotions(String cartId, String productId) {
        // 實作檢查適用促銷的邏輯
        System.out.println("檢查購物車 " + cartId + " 中商品 " + productId + " 的適用促銷");
    }

    private void checkMembershipUpgrade(String customerId, int totalPoints) {
        // 實作檢查會員升級條件的邏輯
        System.out.println("檢查客戶 " + customerId + " 的會員升級條件，總點數: " + totalPoints);
    }
}
