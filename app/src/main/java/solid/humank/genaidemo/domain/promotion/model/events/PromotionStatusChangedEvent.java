package solid.humank.genaidemo.domain.promotion.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionStatus;

/**
 * 促銷狀態變更事件 當促銷狀態發生變更時發布此事件
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record PromotionStatusChangedEvent(
        PromotionId promotionId,
        PromotionStatus oldStatus,
        PromotionStatus newStatus,
        String reason,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static PromotionStatusChangedEvent create(
            PromotionId promotionId,
            PromotionStatus oldStatus,
            PromotionStatus newStatus,
            String reason) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new PromotionStatusChangedEvent(promotionId, oldStatus, newStatus, reason,
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
