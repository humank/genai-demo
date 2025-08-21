package solid.humank.genaidemo.testutils.stubs;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import solid.humank.genaidemo.testutils.isolation.TestResource;

/**
 * Notification Service Stub
 */
public class NotificationServiceStub implements TestResource {

    private final List<NotificationRecord> sentNotifications = new CopyOnWriteArrayList<>();
    private boolean isCleanedUp = false;

    public void sendNotification(String recipient, String subject, String message) {
        if (isCleanedUp) {
            throw new IllegalStateException("Service has been cleaned up");
        }

        sentNotifications.add(new NotificationRecord(recipient, subject, message, System.currentTimeMillis()));
    }

    public void sendEmail(String email, String subject, String body) {
        sendNotification(email, subject, body);
    }

    public void sendSms(String phoneNumber, String message) {
        sendNotification(phoneNumber, "SMS", message);
    }

    public List<NotificationRecord> getSentNotifications() {
        return List.copyOf(sentNotifications);
    }

    public int getNotificationCount() {
        return isCleanedUp ? 0 : sentNotifications.size();
    }

    public boolean hasNotificationFor(String recipient) {
        return !isCleanedUp && sentNotifications.stream()
                .anyMatch(record -> record.recipient().equals(recipient));
    }

    public void clear() {
        if (!isCleanedUp) {
            sentNotifications.clear();
        }
    }

    @Override
    public void cleanup() throws Exception {
        if (!isCleanedUp) {
            sentNotifications.clear();
            isCleanedUp = true;
        }
    }

    @Override
    public String getResourceName() {
        return "NotificationServiceStub";
    }

    @Override
    public boolean isCleanedUp() {
        return isCleanedUp;
    }

    public record NotificationRecord(String recipient, String subject, String message, long timestamp) {
    }
}