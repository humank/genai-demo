package solid.humank.genaidemo.domain.promotion.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionType;

/**
 * 促銷活動創建事件
 * 
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record PromotionCreatedEvent(
        PromotionId promotionId,
        String name,
        String description,
        PromotionType type,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static PromotionCreatedEvent create(
            PromotionId promotionId, String name, String description, PromotionType type) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new PromotionCreatedEvent(promotionId, name, description, type,
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
