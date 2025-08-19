package solid.humank.genaidemo.infrastructure.notification.persistence.mapper;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.notification.model.aggregate.Notification;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationChannel;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationId;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationStatus;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationType;
import solid.humank.genaidemo.infrastructure.common.persistence.mapper.DomainMapper;
import solid.humank.genaidemo.infrastructure.notification.persistence.entity.JpaNotificationEntity;

/**
 * 通知映射器
 * 負責在領域模型和JPA實體之間進行轉換
 */
@Component
public class NotificationMapper implements DomainMapper<Notification, JpaNotificationEntity> {

    @Override
    public JpaNotificationEntity toJpaEntity(Notification notification) {
        if (notification == null) {
            return null;
        }

        // Get the first channel for simplicity (in a real system, you might store all
        // channels)
        String channel = notification.getChannels().isEmpty() ? "EMAIL" : notification.getChannels().get(0).name();

        return new JpaNotificationEntity(
                notification.getId().getValue(),
                notification.getCustomerId(),
                notification.getType().name(),
                notification.getSubject(),
                notification.getContent(),
                channel,
                notification.getStatus().name(),
                notification.getScheduledTime(),
                notification.getSentTime(),
                notification.getRetryCount(),
                notification.getFailureReason(),
                notification.getCreatedAt(),
                notification.getUpdatedAt());
    }

    @Override
    public Notification toDomainModel(JpaNotificationEntity entity) {
        if (entity == null) {
            return null;
        }

        // Create notification with basic constructor
        java.util.List<NotificationChannel> channels = java.util.Arrays
                .asList(NotificationChannel.valueOf(entity.getChannel()));

        Notification notification = new Notification(
                NotificationId.of(entity.getId()),
                entity.getCustomerId(),
                NotificationType.valueOf(entity.getType()),
                entity.getTitle(),
                entity.getContent(),
                channels);

        // Use reflection to set the internal state since this is infrastructure layer
        // and we need to reconstruct the aggregate from persistence
        try {
            setFieldValue(notification, "status", NotificationStatus.valueOf(entity.getStatus()));

            if (entity.getScheduledTime() != null) {
                setFieldValue(notification, "scheduledTime", entity.getScheduledTime());
            }
            if (entity.getSentTime() != null) {
                setFieldValue(notification, "sentTime", entity.getSentTime());
            }

            setFieldValue(notification, "retryCount", entity.getRetryCount());

            if (entity.getErrorMessage() != null) {
                setFieldValue(notification, "failureReason", entity.getErrorMessage());
            }

            // Set timestamps using reflection
            setFieldValue(notification, "updatedAt", entity.getUpdatedAt());

        } catch (Exception e) {
            throw new RuntimeException("Failed to reconstruct Notification aggregate from persistence", e);
        }

        return notification;
    }

    private void setFieldValue(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}