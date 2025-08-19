package solid.humank.genaidemo.domain.common.valueobject;

import java.time.LocalDateTime;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.delivery.model.valueobject.DeliveryStatus;

/**
 * 物流訂單值對象 領域層定義的物流訂單，避免依賴基礎設施層
 *
 * <p>此值對象封裝了物流訂單的所有不可變屬性，包括訂單ID、物流狀態、追蹤號碼和預計送達時間。 作為值對象，它是不可變的，任何狀態變更都會創建新的實例。
 */
@ValueObject
public class DeliveryOrder {
    private final OrderId orderId;
    private final DeliveryStatus status;
    private final String trackingNumber;
    private final LocalDateTime estimatedDeliveryTime;

    /**
     * 建立物流訂單值對象
     *
     * @param orderId 訂單ID
     * @param status 物流狀態
     * @param trackingNumber 追蹤號碼
     * @param estimatedDeliveryTime 預計送達時間
     */
    public DeliveryOrder(
            OrderId orderId,
            DeliveryStatus status,
            String trackingNumber,
            LocalDateTime estimatedDeliveryTime) {
        this.orderId = orderId;
        this.status = status;
        this.trackingNumber = trackingNumber;
        this.estimatedDeliveryTime = estimatedDeliveryTime;
    }

    /**
     * 獲取訂單ID
     *
     * @return 訂單ID
     */
    public OrderId getOrderId() {
        return orderId;
    }

    /**
     * 獲取物流狀態
     *
     * @return 物流狀態
     */
    public DeliveryStatus getStatus() {
        return status;
    }

    /**
     * 獲取追蹤號碼
     *
     * @return 追蹤號碼
     */
    public String getTrackingNumber() {
        return trackingNumber;
    }

    /**
     * 獲取預計送達時間
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
        return new DeliveryOrder(
                this.orderId, newStatus, this.trackingNumber, this.estimatedDeliveryTime);
    }

    /**
     * 創建具有新追蹤號碼的物流訂單
     *
     * @param newTrackingNumber 新的追蹤號碼
     * @return 新的物流訂單實例
     */
    public DeliveryOrder withTrackingNumber(String newTrackingNumber) {
        return new DeliveryOrder(
                this.orderId, this.status, newTrackingNumber, this.estimatedDeliveryTime);
    }

    /**
     * 創建具有新預計送達時間的物流訂單
     *
     * @param newEstimatedDeliveryTime 新的預計送達時間
     * @return 新的物流訂單實例
     */
    public DeliveryOrder withEstimatedDeliveryTime(LocalDateTime newEstimatedDeliveryTime) {
        return new DeliveryOrder(
                this.orderId, this.status, this.trackingNumber, newEstimatedDeliveryTime);
    }
}
