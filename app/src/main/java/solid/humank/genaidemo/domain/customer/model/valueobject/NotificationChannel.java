package solid.humank.genaidemo.domain.customer.model.valueobject;

/** 通知渠道枚舉 */
public enum NotificationChannel {
    EMAIL("電子郵件"),
    SMS("簡訊"),
    PUSH("推播通知"),
    IN_APP("應用內通知");

    private final String description;

    NotificationChannel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
