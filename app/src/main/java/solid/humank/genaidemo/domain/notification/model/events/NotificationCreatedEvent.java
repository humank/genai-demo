package solid.humank.genaidemo.domain.notification.model.events;

import solid.humank.genaidemo.domain.common.event.AbstractDomainEvent;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationChannel;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationId;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知創建事件
 */
public class NotificationCreatedEvent extends AbstractDomainEvent {
    
    private final NotificationId notificationId;
    private final String customerId;
    private final NotificationType type;
    private final String subject;
    private final List<String> channels;
    
    public NotificationCreatedEvent(NotificationId notificationId, String customerId, 
                                   NotificationType type, String subject, 
                                   List<NotificationChannel> channels) {
        super("notification-service");
        this.notificationId = notificationId;
        this.customerId = customerId;
        this.type = type;
        this.subject = subject;
        this.channels = channels.stream()
                               .map(NotificationChannel::name)
                               .collect(Collectors.toList());
    }
    
    public NotificationId getNotificationId() {
        return notificationId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public NotificationType getType() {
        return type;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public List<String> getChannels() {
        return channels;
    }
    
    @Override
    public String getEventType() {
        return "NotificationCreatedEvent";
    }
}