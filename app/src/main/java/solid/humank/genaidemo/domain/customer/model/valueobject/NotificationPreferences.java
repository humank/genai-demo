package solid.humank.genaidemo.domain.customer.model.valueobject;

import java.util.Set;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationChannel;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationType;

/** 通知偏好設定值對象 */
@ValueObject(name = "NotificationPreferences", description = "通知偏好設定值對象，管理消費者的通知偏好")
public record NotificationPreferences(
        Set<NotificationType> enabledTypes,
        Set<NotificationChannel> enabledChannels,
        boolean marketingEnabled) {
    public NotificationPreferences {
        enabledTypes = Set.copyOf(enabledTypes);
        enabledChannels = Set.copyOf(enabledChannels);
    }

    public boolean isEnabled(NotificationType type) {
        return enabledTypes.contains(type);
    }

    public boolean shouldSendVia(NotificationChannel channel) {
        return enabledChannels.contains(channel);
    }

    public static NotificationPreferences defaultPreferences() {
        return new NotificationPreferences(
                Set.of(NotificationType.ORDER_STATUS, NotificationType.DELIVERY_STATUS),
                Set.of(NotificationChannel.EMAIL),
                false);
    }

    public NotificationPreferences enableType(NotificationType type) {
        Set<NotificationType> newTypes = new java.util.HashSet<>(enabledTypes);
        newTypes.add(type);
        return new NotificationPreferences(newTypes, enabledChannels, marketingEnabled);
    }

    public NotificationPreferences disableType(NotificationType type) {
        Set<NotificationType> newTypes = new java.util.HashSet<>(enabledTypes);
        newTypes.remove(type);
        return new NotificationPreferences(newTypes, enabledChannels, marketingEnabled);
    }
}
