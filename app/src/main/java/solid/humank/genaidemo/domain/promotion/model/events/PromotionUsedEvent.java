package solid.humank.genaidemo.domain.promotion.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;

/**
 * 促銷活動使用事件
 * 
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record PromotionUsedEvent(
        PromotionId promotionId,
        String customerId,
        String orderId,
        int usageCount,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static PromotionUsedEvent create(
            PromotionId promotionId, String customerId, String orderId, int usageCount) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new PromotionUsedEvent(promotionId, customerId, orderId, usageCount,
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
        return promotionId.value();
    }
}
