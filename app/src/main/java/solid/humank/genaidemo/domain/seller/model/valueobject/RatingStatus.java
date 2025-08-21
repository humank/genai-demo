package solid.humank.genaidemo.domain.seller.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 評級狀態值對象 */
@ValueObject(name = "RatingStatus", description = "評級狀態")
public enum RatingStatus {
    ACTIVE("活躍"),
    HIDDEN("隱藏"),
    DELETED("已刪除");

    private final String description;

    RatingStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}