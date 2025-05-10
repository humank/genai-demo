package solid.humank.genaidemo.domain.common.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.delivery.DeliveryStatus;
import solid.humank.genaidemo.domain.order.model.valueobject.OrderId;

import java.time.LocalDateTime;

/**
 * 物流訂單值對象
 * 領域層定義的物流訂單，避免依賴基礎設施層
 */
@ValueObject
public class DeliveryOrder {
    private final OrderId orderId;
    private final DeliveryStatus status;
    private final String trackingNumber;
    private final LocalDateTime estimatedDeliveryTime;

    public DeliveryOrder(OrderId orderId, DeliveryStatus status, String trackingNumber, LocalDateTime estimatedDeliveryTime) {
        this.orderId = orderId;
        this.status = status;
        this.trackingNumber = trackingNumber;
        this.estimatedDeliveryTime = estimatedDeliveryTime;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public LocalDateTime getEstimatedDeliveryTime() {
        return estimatedDeliveryTime;
    }

    public DeliveryOrder withStatus(DeliveryStatus newStatus) {
        return new DeliveryOrder(this.orderId, newStatus, this.trackingNumber, this.estimatedDeliveryTime);
    }

    public DeliveryOrder withTrackingNumber(String newTrackingNumber) {
        return new DeliveryOrder(this.orderId, this.status, newTrackingNumber, this.estimatedDeliveryTime);
    }

    public DeliveryOrder withEstimatedDeliveryTime(LocalDateTime newEstimatedDeliveryTime) {
        return new DeliveryOrder(this.orderId, this.status, this.trackingNumber, newEstimatedDeliveryTime);
    }
}