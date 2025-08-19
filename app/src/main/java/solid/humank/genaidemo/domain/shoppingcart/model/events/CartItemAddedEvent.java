package solid.humank.genaidemo.domain.shoppingcart.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartId;

/**
 * 購物車商品添加事件 當商品被添加到購物車時發布此事件
 * 
 * 使用 record 實作，自動獲得不可變性和基礎功能
 * totalPrice 通過計算屬性自動計算，避免狀態不一致
 */
public record CartItemAddedEvent(
        ShoppingCartId cartId,
        CustomerId customerId,
        ProductId productId,
        int quantity,
        Money unitPrice,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     * 
     * @param cartId     購物車ID
     * @param customerId 客戶ID
     * @param productId  產品ID
     * @param quantity   數量
     * @param unitPrice  單價
     * @return 購物車商品添加事件
     */
    public static CartItemAddedEvent create(
            ShoppingCartId cartId,
            CustomerId customerId,
            ProductId productId,
            int quantity,
            Money unitPrice) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new CartItemAddedEvent(cartId, customerId, productId, quantity, unitPrice,
                metadata.eventId(), metadata.occurredOn());
    }

    /**
     * 計算總價
     * 
     * @return 總價 (單價 × 數量)
     */
    public Money getTotalPrice() {
        return unitPrice.multiply(quantity);
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
