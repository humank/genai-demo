package solid.humank.genaidemo.domain.notification.model.events;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationId;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationStatus;

/**
 * 通知狀態變更事件
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record NotificationStatusChangedEvent(
        NotificationId notificationId,
        String customerId,
        NotificationStatus oldStatus,
        NotificationStatus newStatus,
        String reason,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static NotificationStatusChangedEvent create(
            NotificationId notificationId,
            String customerId,
            NotificationStatus oldStatus,
            NotificationStatus newStatus,
            String reason) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new NotificationStatusChangedEvent(notificationId, customerId, oldStatus, newStatus, reason,
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
        return notificationId.getId().toString();
    }
}
