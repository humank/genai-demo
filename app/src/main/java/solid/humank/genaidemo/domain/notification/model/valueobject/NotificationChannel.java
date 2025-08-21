package solid.humank.genaidemo.domain.notification.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 通知渠道值對象 - 統一的通知渠道定義 */
@ValueObject
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
