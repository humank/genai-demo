package solid.humank.genaidemo.domain.review.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 回覆狀態值對象
 */
@ValueObject(name = "ResponseStatus", description = "回覆狀態")
public enum ResponseStatus {
    ACTIVE("活躍"),
    HIDDEN("隱藏"),
    DELETED("已刪除");

    private final String description;

    ResponseStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isVisible() {
        return this == ACTIVE;
    }

    public boolean canBeModified() {
        return this == ACTIVE;
    }

    public boolean isDeleted() {
        return this == DELETED;
    }
}