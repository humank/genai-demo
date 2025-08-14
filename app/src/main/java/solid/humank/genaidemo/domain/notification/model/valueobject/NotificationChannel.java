package solid.humank.genaidemo.domain.notification.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 通知渠道值對象 */
@ValueObject
public enum NotificationChannel {
    EMAIL("電子郵件"),
    SMS("短信"),
    PUSH("推送通知"),
    APP("應用內通知");

    private final String description;

    NotificationChannel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
