package solid.humank.genaidemo.domain.review.model.valueobject;

/** 評價狀態枚舉 */
public enum ReviewStatus {
    PENDING("待審核"),
    APPROVED("已通過"),
    REJECTED("已拒絕"),
    UNDER_INVESTIGATION("調查中"),
    HIDDEN("已隱藏");

    private final String description;

    ReviewStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isVisible() {
        return this == APPROVED;
    }

    public boolean canModify() {
        return this == PENDING || this == REJECTED;
    }

    public boolean canReport() {
        return this == APPROVED;
    }
}
