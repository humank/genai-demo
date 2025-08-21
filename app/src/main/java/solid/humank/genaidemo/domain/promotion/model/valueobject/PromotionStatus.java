package solid.humank.genaidemo.domain.promotion.model.valueobject;

/** 促銷狀態枚舉 */
public enum PromotionStatus {
    ACTIVE("活躍"),
    INACTIVE("非活躍"),
    EXPIRED("已過期"),
    SUSPENDED("已暫停"),
    DELETED("已刪除");

    private final String description;

    PromotionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canApply() {
        return this == ACTIVE;
    }
}
