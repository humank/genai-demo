package solid.humank.genaidemo.domain.seller.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerId;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerRatingId;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/**
 * 賣家評級添加事件
 * 當客戶為賣家添加評級時發布此事件
 */
public record SellerRatingAddedEvent(
        SellerId sellerId,
        SellerRatingId ratingId,
        CustomerId customerId,
        int rating,
        String comment,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static SellerRatingAddedEvent create(SellerId sellerId, SellerRatingId ratingId,
            CustomerId customerId, int rating, String comment) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new SellerRatingAddedEvent(sellerId, ratingId, customerId, rating, comment,
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