package solid.humank.genaidemo.domain.promotion.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;

/**
 * 促銷資訊更新事件 當促銷資訊發生變更時發布此事件
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record PromotionInfoUpdatedEvent(
        PromotionId promotionId,
        String oldName,
        String newName,
        String oldDescription,
        String newDescription,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static PromotionInfoUpdatedEvent create(
            PromotionId promotionId,
            String oldName,
            String newName,
            String oldDescription,
            String newDescription) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new PromotionInfoUpdatedEvent(promotionId, oldName, newName, oldDescription, newDescription,
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
