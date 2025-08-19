package solid.humank.genaidemo.domain.shoppingcart.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartId;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartStatus;

/**
 * 購物車狀態變更事件 當購物車狀態發生變更時發布此事件
 * 
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record CartStatusChangedEvent(
        ShoppingCartId cartId,
        CustomerId consumerId,
        ShoppingCartStatus oldStatus,
        ShoppingCartStatus newStatus,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     * 
     * @param cartId     購物車ID
     * @param consumerId 客戶ID
     * @param oldStatus  舊狀態
     * @param newStatus  新狀態
     * @return 購物車狀態變更事件
     */
    public static CartStatusChangedEvent create(
            ShoppingCartId cartId,
            CustomerId consumerId,
            ShoppingCartStatus oldStatus,
            ShoppingCartStatus newStatus) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new CartStatusChangedEvent(cartId, consumerId, oldStatus, newStatus,
                metadata.eventId(), metadata.occurredOn());
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
