package solid.humank.genaidemo.domain.review.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewId;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/**
 * 評價修改事件 當評價被修改時發布此事件
 * 
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record ReviewModifiedEvent(
        ReviewId reviewId,
        ProductId productId,
        CustomerId reviewerId,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     * 
     * @param reviewId   評價ID
     * @param productId  產品ID
     * @param reviewerId 評價者ID
     * @return 評價修改事件
     */
    public static ReviewModifiedEvent create(ReviewId reviewId, ProductId productId, CustomerId reviewerId) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new ReviewModifiedEvent(reviewId, productId, reviewerId,
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
