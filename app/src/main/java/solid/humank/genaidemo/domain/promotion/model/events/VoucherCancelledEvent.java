package solid.humank.genaidemo.domain.promotion.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.promotion.model.valueobject.VoucherId;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/**
 * 優惠券取消事件
 * 
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record VoucherCancelledEvent(
        VoucherId voucherId,
        CustomerId ownerId,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static VoucherCancelledEvent create(
            VoucherId voucherId, CustomerId ownerId) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new VoucherCancelledEvent(voucherId, ownerId,
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
        return voucherId.value();
    }
}