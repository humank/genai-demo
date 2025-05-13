package solid.humank.genaidemo.domain.workflow.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 配送狀態值對象
 */
@ValueObject
public enum DeliveryStatus {
    PENDING_SHIPMENT("待出貨"),
    IN_TRANSIT("配送中"),
    DELIVERED("已送達"),
    DELAYED("延遲"),
    DELIVERY_FAILED("配送失敗"),
    REFUSED("拒收");

    private final String description;

    DeliveryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}