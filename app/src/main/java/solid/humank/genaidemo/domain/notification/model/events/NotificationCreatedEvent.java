package solid.humank.genaidemo.domain.notification.model.events;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationChannel;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationId;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationType;

/**
 * 通知創建事件
 * 使用 record 實作，自動獲得不可變性和基礎功能
 */
public record NotificationCreatedEvent(
        NotificationId notificationId,
        String customerId,
        NotificationType type,
        String subject,
        List<String> channels,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static NotificationCreatedEvent create(
            NotificationId notificationId,
            String customerId,
            NotificationType type,
            String subject,
            List<NotificationChannel> channels) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        List<String> channelNames = channels.stream()
                .map(NotificationChannel::name)
                .toList();
        return new NotificationCreatedEvent(notificationId, customerId, type, subject, channelNames,
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
