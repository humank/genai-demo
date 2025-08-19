package solid.humank.genaidemo.domain.customer.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.customer.model.valueobject.MembershipLevel;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/**
 * 會員等級升級事件
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record MembershipLevelUpgradedEvent(
        CustomerId customerId,
        MembershipLevel oldLevel,
        MembershipLevel newLevel,
        UUID eventId,
        LocalDateTime occurredOn)
        implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static MembershipLevelUpgradedEvent create(
            CustomerId customerId, MembershipLevel oldLevel, MembershipLevel newLevel) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new MembershipLevelUpgradedEvent(customerId, oldLevel, newLevel,
                metadata.eventId(), metadata.occurredOn());
    }

    // 向後兼容的構造函數
    public MembershipLevelUpgradedEvent(
            CustomerId customerId, MembershipLevel oldLevel, MembershipLevel newLevel) {
        this(customerId, oldLevel, newLevel, UUID.randomUUID(), LocalDateTime.now());
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
        return customerId.getValue();
    }
}
