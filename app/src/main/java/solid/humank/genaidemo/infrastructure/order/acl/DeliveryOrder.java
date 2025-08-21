package solid.humank.genaidemo.infrastructure.order.acl;

import java.time.LocalDateTime;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;

/** 配送訂單 用於與外部物流系統交互的數據結構 */
public record DeliveryOrder(
        OrderId orderId,
        DeliveryStatus status,
        String trackingNumber,
        LocalDateTime estimatedDeliveryTime) {
    /** 建立配送訂單 */
    public DeliveryOrder(OrderId orderId, DeliveryStatus status) {
        this(orderId, status, null, null);
    }

    /** 建立配送訂單 */
    public DeliveryOrder withTrackingNumber(String trackingNumber) {
        return new DeliveryOrder(orderId, status, trackingNumber, estimatedDeliveryTime);
    }

    /** 建立配送訂單 */
    public DeliveryOrder withEstimatedDeliveryTime(LocalDateTime estimatedDeliveryTime) {
        return new DeliveryOrder(orderId, status, trackingNumber, estimatedDeliveryTime);
    }

    /** 建立配送訂單 */
    public DeliveryOrder withStatus(DeliveryStatus status) {
        return new DeliveryOrder(orderId, status, trackingNumber, estimatedDeliveryTime);
    }
}
