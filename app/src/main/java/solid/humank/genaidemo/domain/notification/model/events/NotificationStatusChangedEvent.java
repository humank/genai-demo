package solid.humank.genaidemo.domain.notification.model.events;

import solid.humank.genaidemo.domain.common.event.AbstractDomainEvent;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationId;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationStatus;

/** 通知狀態變更事件 */
public class NotificationStatusChangedEvent extends AbstractDomainEvent {

    private final NotificationId notificationId;
    private final String customerId;
    private final NotificationStatus oldStatus;
    private final NotificationStatus newStatus;
    private final String reason;

    public NotificationStatusChangedEvent(
            NotificationId notificationId,
            String customerId,
            NotificationStatus oldStatus,
            NotificationStatus newStatus,
            String reason) {
        super("notification-service");
        this.notificationId = notificationId;
        this.customerId = customerId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }

    public NotificationId getNotificationId() {
        return notificationId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public NotificationStatus getOldStatus() {
        return oldStatus;
    }

    public NotificationStatus getNewStatus() {
        return newStatus;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String getEventType() {
        return "NotificationStatusChangedEvent";
    }
}
