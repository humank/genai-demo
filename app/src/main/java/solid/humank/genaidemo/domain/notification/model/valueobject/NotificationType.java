package solid.humank.genaidemo.domain.notification.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 通知類型值對象 - 統一的通知類型定義 */
@ValueObject
public enum NotificationType {
    // 訂單相關
    ORDER_CREATED("訂單創建"),
    ORDER_CONFIRMED("訂單確認"),
    ORDER_STATUS("訂單狀態"),
    ORDER_UPDATE("訂單更新"),
    ORDER_COMPLETED("訂單完成"),
    ORDER_CANCELLED("訂單取消"),

    // 支付相關
    PAYMENT_SUCCESS("支付成功"),
    PAYMENT_FAILED("支付失敗"),
    PAYMENT_STATUS("支付狀態"),

    // 配送相關
    DELIVERY_STATUS_UPDATE("配送狀態更新"),
    DELIVERY_STATUS("配送狀態"),
    DELIVERY_UPDATE("配送更新"),

    // 庫存相關
    INVENTORY_INSUFFICIENT("庫存不足"),

    // 促銷和行銷
    PROMOTION("促銷活動"),
    MARKETING("行銷活動"),
    PRODUCT_RECOMMENDATION("商品推薦"),

    // 會員相關
    REWARD_POINTS("紅利點數"),
    MEMBERSHIP("會員相關"),

    // 系統相關
    SYSTEM_ANNOUNCEMENT("系統公告");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
