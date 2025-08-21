package solid.humank.genaidemo.domain.notification.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 通知狀態值對象 */
@ValueObject
public enum NotificationStatus {
    PENDING("待發送"),
    SENT("已發送"),
    DELIVERED("已送達"),
    FAILED("發送失敗"),
    READ("已讀");

    private final String description;

    NotificationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
