package solid.humank.genaidemo.domain.review.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 圖片狀態值對象
 */
@ValueObject(name = "ImageStatus", description = "圖片狀態")
public enum ImageStatus {
    PENDING("待處理"),
    PROCESSED("已處理"),
    FAILED("處理失敗"),
    DELETED("已刪除");

    private final String description;

    ImageStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isProcessed() {
        return this == PROCESSED;
    }

    public boolean canBeDeleted() {
        return this != DELETED;
    }

    public boolean isActive() {
        return this == PROCESSED;
    }
}