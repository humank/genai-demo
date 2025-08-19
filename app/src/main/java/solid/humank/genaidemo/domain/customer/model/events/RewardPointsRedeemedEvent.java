package solid.humank.genaidemo.domain.customer.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/**
 * 紅利點數兌換事件 當客戶兌換紅利點數時發布此事件
 * 
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record RewardPointsRedeemedEvent(
        CustomerId customerId,
        int pointsRedeemed,
        int remainingPoints,
        String reason,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static RewardPointsRedeemedEvent create(CustomerId customerId, int pointsRedeemed, int remainingPoints,
            String reason) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new RewardPointsRedeemedEvent(customerId, pointsRedeemed, remainingPoints, reason,
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
        return customerId.getValue();
    }
}
