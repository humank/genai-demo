package solid.humank.genaidemo.domain.seller.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerId;

/**
 * 賣家檔案更新事件
 * 當賣家更新商業資訊時發布此事件
 */
public record SellerProfileUpdatedEvent(
        SellerId sellerId,
        String businessName,
        String businessAddress,
        String description,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static SellerProfileUpdatedEvent create(SellerId sellerId, String businessName,
            String businessAddress, String description) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new SellerProfileUpdatedEvent(sellerId, businessName, businessAddress, description,
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