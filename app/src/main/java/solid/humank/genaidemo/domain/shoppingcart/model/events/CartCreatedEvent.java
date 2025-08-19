package solid.humank.genaidemo.domain.shoppingcart.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartId;

/**
 * 購物車創建事件 當新的購物車被創建時發布此事件
 * 
 * 使用 record 實作，自動獲得：
 * - 不可變性
 * - equals/hashCode
 * - toString
 * - 構造函數
 * - getter 方法
 */
public record CartCreatedEvent(
        ShoppingCartId cartId,
        CustomerId customerId,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     * 
     * @param cartId     購物車ID
     * @param customerId 客戶ID
     * @return 購物車創建事件
     */
    public static CartCreatedEvent create(ShoppingCartId cartId, CustomerId customerId) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new CartCreatedEvent(cartId, customerId, metadata.eventId(), metadata.occurredOn());
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
