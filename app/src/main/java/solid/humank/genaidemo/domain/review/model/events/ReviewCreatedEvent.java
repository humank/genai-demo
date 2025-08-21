package solid.humank.genaidemo.domain.review.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewId;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewRating;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/**
 * 評價創建事件
 */
public record ReviewCreatedEvent(
        ReviewId reviewId,
        ProductId productId,
        CustomerId reviewerId,
        ReviewRating rating,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static ReviewCreatedEvent create(
            ReviewId reviewId,
            ProductId productId,
            CustomerId reviewerId,
            ReviewRating rating) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new ReviewCreatedEvent(
                reviewId, productId, reviewerId, rating,
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
        return reviewId.value();
    }
}