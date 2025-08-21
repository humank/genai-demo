package solid.humank.genaidemo.domain.review.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 審核狀態值對象
 */
@ValueObject(name = "ModerationStatus", description = "審核狀態")
public enum ModerationStatus {
    PENDING("待審核"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String description;

    ModerationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompleted() {
        return this == COMPLETED;
    }

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isCancelled() {
        return this == CANCELLED;
    }
}