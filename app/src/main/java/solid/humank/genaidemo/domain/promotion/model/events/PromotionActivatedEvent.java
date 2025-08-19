package solid.humank.genaidemo.domain.promotion.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionType;

/**
 * 促銷活動激活事件
 * 
 * 當促銷活動被激活時發布此事件，通知定價 bounded context 更新商品價格
 * 
 * 使用 record 實作，自動獲得不可變性和基礎功能
 * 需求 8.5: 實現 PromotionActivatedEvent 到定價 bounded context 的事件流轉驗證
 */
public record PromotionActivatedEvent(
        PromotionId promotionId,
        String name,
        PromotionType type,
        Money discountAmount,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static PromotionActivatedEvent create(
            PromotionId promotionId,
            String name,
            PromotionType type,
            Money discountAmount,
            LocalDateTime validFrom,
            LocalDateTime validTo) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new PromotionActivatedEvent(
                promotionId, name, type, discountAmount, validFrom, validTo,
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