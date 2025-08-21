package solid.humank.genaidemo.domain.shoppingcart.model.events;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.CartItem;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartId;

/**
 * 購物車清空事件 當購物車被清空時發布此事件
 * 
 * 使用 record 實作，自動獲得不可變性
 * clearedItems 使用 List.copyOf 確保不可變性
 */
public record CartClearedEvent(
        ShoppingCartId cartId,
        CustomerId customerId,
        List<CartItem> clearedItems,
        Money clearedAmount,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 確保 clearedItems 的不可變性
     */
    public CartClearedEvent {
        clearedItems = List.copyOf(clearedItems);
    }

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     * 
     * @param cartId        購物車ID
     * @param customerId    客戶ID
     * @param clearedItems  被清空的商品項目
     * @param clearedAmount 被清空的總金額
     * @return 購物車清空事件
     */
    public static CartClearedEvent create(
            ShoppingCartId cartId,
            CustomerId customerId,
            List<CartItem> clearedItems,
            Money clearedAmount) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new CartClearedEvent(cartId, customerId, clearedItems, clearedAmount,
                metadata.eventId(), metadata.occurredOn());
    }

    /**
     * 獲取被清空的商品項目數量
     * 
     * @return 商品項目數量
     */
    public int getClearedItemCount() {
        return clearedItems.size();
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
