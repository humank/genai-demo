package solid.humank.genaidemo.application.common.valueobject;

/** 訂單狀態枚舉 (應用層) */
public enum OrderStatus {
    CREATED("已創建"),
    SUBMITTED("已提交"),
    PAID("已支付"),
    PROCESSING("處理中"),
    SHIPPED("已發貨"),
    DELIVERED("已送達"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    REJECTED("已拒絕"),
    PAYMENT_FAILED("支付失敗");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
