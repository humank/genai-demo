package solid.humank.genaidemo.domain.seller.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerId;

/**
 * 賣家註冊事件 當新賣家註冊時發布此事件
 * 
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record SellerRegisteredEvent(
        SellerId sellerId,
        String sellerName,
        String email,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     * 
     * @param sellerId   賣家ID
     * @param sellerName 賣家名稱
     * @param email      賣家郵箱
     * @return 賣家註冊事件
     */
    public static SellerRegisteredEvent create(SellerId sellerId, String sellerName, String email) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new SellerRegisteredEvent(sellerId, sellerName, email,
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
        return sellerId.getId();
    }
}