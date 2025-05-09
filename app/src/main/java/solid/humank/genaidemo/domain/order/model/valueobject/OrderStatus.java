package solid.humank.genaidemo.domain.order.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 訂單狀態枚舉
 */
@ValueObject
public enum OrderStatus {
    CREATED("已建立"),
    PENDING("處理中"),
    CONFIRMED("已確認"),
    PAID("已付款"),
    SHIPPING("配送中"),
    DELIVERED("已送達"),
    CANCELLED("已取消");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(OrderStatus nextStatus) {
        switch (this) {
            case CREATED:
                return nextStatus == PENDING || nextStatus == CANCELLED;
            case PENDING:
                return nextStatus == CONFIRMED || nextStatus == CANCELLED;
            case CONFIRMED:
                return nextStatus == PAID || nextStatus == CANCELLED;
            case PAID:
                return nextStatus == SHIPPING || nextStatus == CANCELLED;
            case SHIPPING:
                return nextStatus == DELIVERED || nextStatus == CANCELLED;
            case DELIVERED:
            case CANCELLED:
                return false;
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}