package solid.humank.genaidemo.domain.shoppingcart.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartId;

/**
 * 購物車商品移除事件 當商品從購物車中移除時發布此事件
 * 
 * 使用 record 實作，removedAmount 通過計算屬性自動計算
 */
public record CartItemRemovedEvent(
        ShoppingCartId cartId,
        CustomerId customerId,
        ProductId productId,
        int removedQuantity,
        Money unitPrice,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     * 
     * @param cartId          購物車ID
     * @param customerId      客戶ID
     * @param productId       產品ID
     * @param removedQuantity 移除的數量
     * @param unitPrice       單價
     * @return 購物車商品移除事件
     */
    public static CartItemRemovedEvent create(
            ShoppingCartId cartId,
            CustomerId customerId,
            ProductId productId,
            int removedQuantity,
            Money unitPrice) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new CartItemRemovedEvent(cartId, customerId, productId, removedQuantity, unitPrice,
                metadata.eventId(), metadata.occurredOn());
    }

    /**
     * 計算移除的總金額
     * 
     * @return 移除的總金額 (單價 × 移除數量)
     */
    public Money getRemovedAmount() {
        return unitPrice.multiply(removedQuantity);
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String getEventType() {
        return DomainEvent.getEventTypeFromClass(this.getClass());
    }

    @Override
    public String getAggregateId() {
        return cartId.value();
    }
}
