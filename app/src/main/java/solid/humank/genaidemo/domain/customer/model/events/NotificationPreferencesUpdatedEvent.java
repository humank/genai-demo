package solid.humank.genaidemo.domain.customer.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.customer.model.valueobject.NotificationPreferences;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/**
 * 通知偏好設定更新事件 當客戶更新通知偏好設定時發布此事件
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record NotificationPreferencesUpdatedEvent(
        CustomerId customerId,
        NotificationPreferences oldPreferences,
        NotificationPreferences newPreferences,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static NotificationPreferencesUpdatedEvent create(
            CustomerId customerId,
            NotificationPreferences oldPreferences,
            NotificationPreferences newPreferences) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new NotificationPreferencesUpdatedEvent(customerId, oldPreferences, newPreferences,
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
        return customerId.getId();
    }
}
