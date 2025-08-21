package solid.humank.genaidemo.domain.common.valueobject;

import java.time.LocalDateTime;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.delivery.model.valueobject.DeliveryStatus;

/**
 * 物流訂單值對象 - 使用 Record 實作
 * 領域層定義的物流訂單，避免依賴基礎設施層
 *
 * <p>
 * 此值對象封裝了物流訂單的所有不可變屬性，包括訂單ID、物流狀態、追蹤號碼和預計送達時間。
 * 作為值對象，它是不可變的，任何狀態變更都會創建新的實例。
 */
@ValueObject
public record DeliveryOrder(
        OrderId orderId,
        DeliveryStatus status,
        String trackingNumber,
        LocalDateTime estimatedDeliveryTime) {

    /**
     * 獲取訂單ID（向後相容方法）
     *
     * @return 訂單ID
     */
    public OrderId getOrderId() {
        return orderId;
    }

    /**
     * 獲取物流狀態（向後相容方法）
     *
     * @return 物流狀態
     */
    public DeliveryStatus getStatus() {
        return status;
    }

    /**
     * 獲取追蹤號碼（向後相容方法）
     *
     * @return 追蹤號碼
     */
    public String getTrackingNumber() {
        return trackingNumber;
    }

    /**
     * 獲取預計送達時間（向後相容方法）
     *
     * @return 預計送達時間
     */
    public LocalDateTime getEstimatedDeliveryTime() {
        return estimatedDeliveryTime;
    }

    /**
     * 創建具有新狀態的物流訂單
     *
     * @param newStatus 新的物流狀態
     * @return 新的物流訂單實例
     */
    public DeliveryOrder withStatus(DeliveryStatus newStatus) {
        return new DeliveryOrder(orderId, newStatus, trackingNumber, estimatedDeliveryTime);
    }

    /**
     * 創建具有新追蹤號碼的物流訂單
     *
     * @param newTrackingNumber 新的追蹤號碼
     * @return 新的物流訂單實例
     */
    public DeliveryOrder withTrackingNumber(String newTrackingNumber) {
        return new DeliveryOrder(orderId, status, newTrackingNumber, estimatedDeliveryTime);
    }

    /**
     * 創建具有新預計送達時間的物流訂單
     *
     * @param newEstimatedDeliveryTime 新的預計送達時間
     * @return 新的物流訂單實例
     */
    public DeliveryOrder withEstimatedDeliveryTime(LocalDateTime newEstimatedDeliveryTime) {
        return new DeliveryOrder(orderId, status, trackingNumber, newEstimatedDeliveryTime);
    }
}
