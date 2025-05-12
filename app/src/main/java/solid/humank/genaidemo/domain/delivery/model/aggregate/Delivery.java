package solid.humank.genaidemo.domain.delivery.model.aggregate;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.delivery.model.valueobject.DeliveryId;
import solid.humank.genaidemo.domain.delivery.model.valueobject.DeliveryStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 配送聚合根
 * 管理訂單的配送流程
 */
@AggregateRoot
public class Delivery {
    private final DeliveryId id;
    private final OrderId orderId;
    private String shippingAddress;
    private DeliveryStatus status;
    private String deliveryPersonId;
    private String deliveryPersonName;
    private String deliveryPersonContact;
    private LocalDateTime estimatedDeliveryTime;
    private String trackingNumber;
    private String failureReason;
    private String refusalReason;
    private String delayReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 建立配送
     *
     * @param orderId 訂單ID
     * @param shippingAddress 配送地址
     */
    public Delivery(OrderId orderId, String shippingAddress) {
        this(DeliveryId.generate(), orderId, shippingAddress);
    }

    /**
     * 建立配送
     *
     * @param id 配送ID
     * @param orderId 訂單ID
     * @param shippingAddress 配送地址
     */
    public Delivery(DeliveryId id, OrderId orderId, String shippingAddress) {
        this.id = Objects.requireNonNull(id, "配送ID不能為空");
        this.orderId = Objects.requireNonNull(orderId, "訂單ID不能為空");
        this.shippingAddress = Objects.requireNonNull(shippingAddress, "配送地址不能為空");
        this.status = DeliveryStatus.PENDING_SHIPMENT;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * 分配配送資源
     *
     * @param deliveryPersonId 配送員ID
     * @param deliveryPersonName 配送員姓名
     * @param deliveryPersonContact 配送員聯繫方式
     * @param estimatedDeliveryTime 預計送達時間
     */
    public void allocateResources(String deliveryPersonId, String deliveryPersonName, 
                                 String deliveryPersonContact, LocalDateTime estimatedDeliveryTime) {
        if (status != DeliveryStatus.PENDING_SHIPMENT) {
            throw new IllegalStateException("只有待發貨狀態的配送可以分配資源");
        }

        this.deliveryPersonId = Objects.requireNonNull(deliveryPersonId, "配送員ID不能為空");
        this.deliveryPersonName = Objects.requireNonNull(deliveryPersonName, "配送員姓名不能為空");
        this.deliveryPersonContact = Objects.requireNonNull(deliveryPersonContact, "配送員聯繫方式不能為空");
        this.estimatedDeliveryTime = Objects.requireNonNull(estimatedDeliveryTime, "預計送達時間不能為空");
        this.trackingNumber = generateTrackingNumber();
        this.status = DeliveryStatus.IN_TRANSIT;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新配送地址
     *
     * @param newAddress 新地址
     */
    public void updateAddress(String newAddress) {
        if (status != DeliveryStatus.PENDING_SHIPMENT) {
            throw new IllegalStateException("只有待發貨狀態的配送可以更新地址");
        }

        this.shippingAddress = Objects.requireNonNull(newAddress, "新地址不能為空");
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 標記為已送達
     */
    public void markAsDelivered() {
        if (status != DeliveryStatus.IN_TRANSIT && status != DeliveryStatus.DELAYED) {
            throw new IllegalStateException("只有配送中或延遲狀態的配送可以標記為已送達");
        }

        this.status = DeliveryStatus.DELIVERED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 標記為配送失敗
     *
     * @param reason 失敗原因
     */
    public void markAsFailed(String reason) {
        if (status != DeliveryStatus.IN_TRANSIT && status != DeliveryStatus.DELAYED) {
            throw new IllegalStateException("只有配送中或延遲狀態的配送可以標記為失敗");
        }

        this.status = DeliveryStatus.DELIVERY_FAILED;
        this.failureReason = Objects.requireNonNull(reason, "失敗原因不能為空");
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 標記為已拒收
     *
     * @param reason 拒收原因
     */
    public void markAsRefused(String reason) {
        if (status != DeliveryStatus.IN_TRANSIT && status != DeliveryStatus.DELAYED) {
            throw new IllegalStateException("只有配送中或延遲狀態的配送可以標記為拒收");
        }

        this.status = DeliveryStatus.REFUSED;
        this.refusalReason = Objects.requireNonNull(reason, "拒收原因不能為空");
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 標記為延遲
     *
     * @param reason 延遲原因
     * @param newEstimatedDeliveryTime 新的預計送達時間
     */
    public void markAsDelayed(String reason, LocalDateTime newEstimatedDeliveryTime) {
        if (status != DeliveryStatus.IN_TRANSIT) {
            throw new IllegalStateException("只有配送中狀態的配送可以標記為延遲");
        }

        this.status = DeliveryStatus.DELAYED;
        this.delayReason = Objects.requireNonNull(reason, "延遲原因不能為空");
        this.estimatedDeliveryTime = Objects.requireNonNull(newEstimatedDeliveryTime, "新的預計送達時間不能為空");
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 取消配送
     */
    public void cancel() {
        if (status != DeliveryStatus.PENDING_SHIPMENT && status != DeliveryStatus.DELIVERY_FAILED) {
            throw new IllegalStateException("只有待發貨或配送失敗狀態的配送可以取消");
        }

        this.status = DeliveryStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 重新安排配送
     */
    public void rearrange() {
        if (status != DeliveryStatus.DELIVERY_FAILED) {
            throw new IllegalStateException("只有配送失敗狀態的配送可以重新安排");
        }

        this.status = DeliveryStatus.PENDING_SHIPMENT;
        this.failureReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 生成追蹤號
     *
     * @return 追蹤號
     */
    private String generateTrackingNumber() {
        return "TRK-" + System.currentTimeMillis() + "-" + id.toString().substring(0, 8);
    }

    // Getters
    public DeliveryId getId() {
        return id;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public String getDeliveryPersonId() {
        return deliveryPersonId;
    }

    public String getDeliveryPersonName() {
        return deliveryPersonName;
    }

    public String getDeliveryPersonContact() {
        return deliveryPersonContact;
    }

    public LocalDateTime getEstimatedDeliveryTime() {
        return estimatedDeliveryTime;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getRefusalReason() {
        return refusalReason;
    }

    public String getDelayReason() {
        return delayReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Delivery delivery = (Delivery) o;
        return Objects.equals(id, delivery.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}