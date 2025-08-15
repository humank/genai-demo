package solid.humank.genaidemo.domain.voucher.model.valueobject;

/** 優惠券狀態枚舉 */
public enum VoucherStatus {
    ACTIVE("有效"),
    USED("已使用"),
    EXPIRED("已過期"),
    LOST("已報失"),
    REISSUED("已補發"),
    CANCELLED("已取消");

    private final String description;

    VoucherStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canUse() {
        return this == ACTIVE;
    }

    public boolean canReportLost() {
        return this == ACTIVE || this == EXPIRED;
    }

    public boolean canReissue() {
        return this == LOST;
    }
}
