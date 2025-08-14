package solid.humank.genaidemo.domain.notification.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 通知類型值對象 */
@ValueObject
public enum NotificationType {
    ORDER_CREATED("訂單創建"),
    ORDER_CONFIRMED("訂單確認"),
    PAYMENT_SUCCESS("支付成功"),
    PAYMENT_FAILED("支付失敗"),
    INVENTORY_INSUFFICIENT("庫存不足"),
    DELIVERY_STATUS_UPDATE("配送狀態更新"),
    ORDER_COMPLETED("訂單完成"),
    ORDER_CANCELLED("訂單取消");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
