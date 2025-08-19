package solid.humank.genaidemo.domain.shoppingcart.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartId;

/**
 * 購物車商品數量更新事件 當購物車中商品數量被更新時發布此事件
 * 
 * 使用 record 實作，quantityDifference 通過計算屬性自動計算
 */
public record CartItemQuantityUpdatedEvent(
        ShoppingCartId cartId,
        CustomerId customerId,
        ProductId productId,
        int oldQuantity,
        int newQuantity,
        Money unitPrice,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     * 
     * @param cartId      購物車ID
     * @param customerId  客戶ID
     * @param productId   產品ID
     * @param oldQuantity 舊數量
     * @param newQuantity 新數量
     * @param unitPrice   單價
     * @return 購物車商品數量更新事件
     */
    public static CartItemQuantityUpdatedEvent create(
            ShoppingCartId cartId,
            CustomerId customerId,
            ProductId productId,
            int oldQuantity,
            int newQuantity,
            Money unitPrice) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new CartItemQuantityUpdatedEvent(cartId, customerId, productId, oldQuantity, newQuantity, unitPrice,
                metadata.eventId(), metadata.occurredOn());
    }

    /**
     * 計算數量差異
     * 
     * @return 數量差異 (新數量 - 舊數量)
     */
    public int getQuantityDifference() {
        return newQuantity - oldQuantity;
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
