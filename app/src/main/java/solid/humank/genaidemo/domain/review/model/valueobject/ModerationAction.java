package solid.humank.genaidemo.domain.review.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 審核動作值對象
 */
@ValueObject(name = "ModerationAction", description = "審核動作")
public enum ModerationAction {
    APPROVE("通過"),
    REJECT("拒絕"),
    HIDE("隱藏");

    private final String description;

    ModerationAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isApproval() {
        return this == APPROVE;
    }

    public boolean isRejection() {
        return this == REJECT;
    }

    public boolean isHiding() {
        return this == HIDE;
    }
}