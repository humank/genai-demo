package solid.humank.genaidemo.domain.delivery.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 配送狀態值對象
 */
@ValueObject
public enum DeliveryStatus {
    PENDING_SHIPMENT("待發貨"),
    IN_TRANSIT("配送中"),
    DELIVERED("已送達"),
    DELAYED("延遲"),
    DELIVERY_FAILED("配送失敗"),
    REFUSED("已拒收"),
    CANCELLED("已取消");

    private final String description;

    DeliveryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 檢查是否可以轉換到指定狀態
     *
     * @param targetStatus 目標狀態
     * @return 是否可以轉換
     */
    public boolean canTransitionTo(DeliveryStatus targetStatus) {
        switch (this) {
            case PENDING_SHIPMENT:
                return targetStatus == IN_TRANSIT || targetStatus == CANCELLED;
            case IN_TRANSIT:
                return targetStatus == DELIVERED || targetStatus == DELAYED || 
                       targetStatus == DELIVERY_FAILED || targetStatus == REFUSED;
            case DELAYED:
                return targetStatus == IN_TRANSIT || targetStatus == DELIVERED || 
                       targetStatus == DELIVERY_FAILED || targetStatus == REFUSED;
            case DELIVERY_FAILED:
                return targetStatus == IN_TRANSIT || targetStatus == CANCELLED;
            case DELIVERED:
            case REFUSED:
            case CANCELLED:
                return false; // 終態
            default:
                return false;
        }
    }
}