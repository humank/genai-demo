package solid.humank.genaidemo.domain.customer.model.valueobject;

/** 客戶狀態枚舉 */
public enum CustomerStatus {
    ACTIVE("活躍"),
    INACTIVE("非活躍"),
    SUSPENDED("暫停"),
    BLOCKED("封鎖"),
    PENDING_VERIFICATION("待驗證");

    private final String description;

    CustomerStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canMakePurchase() {
        return this == ACTIVE;
    }

    public boolean canReceiveNotifications() {
        return this == ACTIVE || this == INACTIVE;
    }
}
