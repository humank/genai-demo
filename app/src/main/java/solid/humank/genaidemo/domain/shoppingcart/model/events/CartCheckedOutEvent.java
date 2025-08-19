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
 * 購物車結帳事件 當購物車進行結帳時發布此事件
 * 
 * 使用 record 實作，自動獲得不可變性
 * items 使用 List.copyOf 確保不可變性
 */
public record CartCheckedOutEvent(
        ShoppingCartId cartId,
        CustomerId consumerId,
        List<CartItem> items,
        Money totalAmount,
        int totalQuantity,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 確保 items 的不可變性
     */
    public CartCheckedOutEvent {
        items = List.copyOf(items);
    }

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     * 
     * @param cartId        購物車ID
     * @param consumerId    客戶ID
     * @param items         商品項目列表
     * @param totalAmount   總金額
     * @param totalQuantity 總數量
     * @return 購物車結帳事件
     */
    public static CartCheckedOutEvent create(
            ShoppingCartId cartId,
            CustomerId consumerId,
            List<CartItem> items,
            Money totalAmount,
            int totalQuantity) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new CartCheckedOutEvent(cartId, consumerId, items, totalAmount, totalQuantity,
                metadata.eventId(), metadata.occurredOn());
    }

    /**
     * 獲取商品項目數量
     * 
     * @return 商品項目數量
     */
    public int getItemCount() {
        return items.size();
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
