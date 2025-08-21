package solid.humank.genaidemo.domain.seller.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 驗證狀態值對象 */
@ValueObject(name = "VerificationStatus", description = "驗證狀態")
public enum VerificationStatus {
    PENDING("待驗證"),
    VERIFIED("已驗證"),
    REJECTED("已拒絕"),
    EXPIRED("已過期");

    private final String description;

    VerificationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isVerified() {
        return this == VERIFIED;
    }

    public boolean canBeReverified() {
        return this == REJECTED || this == EXPIRED;
    }

    @Override
    public String toString() {
        return description;
    }
}