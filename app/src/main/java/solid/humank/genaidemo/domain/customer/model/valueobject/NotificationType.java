package solid.humank.genaidemo.domain.customer.model.valueobject;

/** 通知類型枚舉 */
public enum NotificationType {
    ORDER_STATUS("訂單狀態"),
    DELIVERY_STATUS("配送狀態"),
    PAYMENT_STATUS("支付狀態"),
    PROMOTION("促銷活動"),
    SYSTEM_ANNOUNCEMENT("系統公告"),
    PRODUCT_RECOMMENDATION("商品推薦"),
    REWARD_POINTS("紅利點數"),
    MEMBERSHIP("會員相關"),
    MARKETING("行銷活動"),
    ORDER_UPDATE("訂單更新"),
    DELIVERY_UPDATE("配送更新");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
