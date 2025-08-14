package solid.humank.genaidemo.domain.workflow.model.aggregate;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.workflow.model.valueobject.DeliveryStatus;

/** 配送聚合根 管理配送的生命週期 */
@AggregateRoot
public class Delivery {
    private final UUID id;
    private final OrderId orderId;
    private String shippingAddress; // 修改為非 final，允許更新
    private DeliveryStatus status;
    private String deliveryPersonId;
    private String deliveryPersonName;
    private String deliveryPersonContact;
    private LocalDateTime estimatedDeliveryTime;
    private LocalDateTime actualDeliveryTime;
    private String trackingNumber;
    private String failureReason;
    private boolean redeliveryScheduled;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 建立配送
     *
     * @param orderId 訂單ID
     * @param shippingAddress 配送地址
     */
    public Delivery(OrderId orderId, String shippingAddress) {
        this.id = UUID.randomUUID();
        this.orderId = Objects.requireNonNull(orderId, "訂單ID不能為空");
        this.shippingAddress = Objects.requireNonNull(shippingAddress, "配送地址不能為空");
        this.status = DeliveryStatus.PENDING_SHIPMENT;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.redeliveryScheduled = false;
    }

    /**
     * 分配配送人員
     *
     * @param deliveryPersonId 配送人員ID
     * @param deliveryPersonName 配送人員姓名
     * @param deliveryPersonContact 配送人員聯繫方式
     */
    public void assignDeliveryPerson(
            String deliveryPersonId, String deliveryPersonName, String deliveryPersonContact) {
        this.deliveryPersonId = Objects.requireNonNull(deliveryPersonId, "配送人員ID不能為空");
        this.deliveryPersonName = Objects.requireNonNull(deliveryPersonName, "配送人員姓名不能為空");
        this.deliveryPersonContact = Objects.requireNonNull(deliveryPersonContact, "配送人員聯繫方式不能為空");
        this.status = DeliveryStatus.IN_TRANSIT;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 設置預計配送時間
     *
     * @param estimatedDeliveryTime 預計配送時間
     */
    public void setEstimatedDeliveryTime(LocalDateTime estimatedDeliveryTime) {
        this.estimatedDeliveryTime = Objects.requireNonNull(estimatedDeliveryTime, "預計配送時間不能為空");
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 設置追蹤號碼
     *
     * @param trackingNumber 追蹤號碼
     */
    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = Objects.requireNonNull(trackingNumber, "追蹤號碼不能為空");
        this.updatedAt = LocalDateTime.now();
    }

    /** 標記為已送達 */
    public void markAsDelivered() {
        if (status != DeliveryStatus.IN_TRANSIT) {
            throw new IllegalStateException("只有配送中的配送可以標記為已送達");
        }

        this.status = DeliveryStatus.DELIVERED;
        this.actualDeliveryTime = LocalDateTime.now();
        this.updatedAt = this.actualDeliveryTime;
    }

    /**
     * 標記為延遲
     *
     * @param reason 延遲原因
     * @param newEstimatedDeliveryTime 新的預計配送時間
     */
    public void markAsDelayed(String reason, LocalDateTime newEstimatedDeliveryTime) {
        if (status != DeliveryStatus.IN_TRANSIT) {
            throw new IllegalStateException("只有配送中的配送可以標記為延遲");
        }

        this.status = DeliveryStatus.DELAYED;
        this.failureReason = Objects.requireNonNull(reason, "延遲原因不能為空");
        this.estimatedDeliveryTime =
                Objects.requireNonNull(newEstimatedDeliveryTime, "新的預計配送時間不能為空");
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 標記為配送失敗
     *
     * @param reason 失敗原因
     */
    public void markAsDeliveryFailed(String reason) {
        if (status != DeliveryStatus.IN_TRANSIT && status != DeliveryStatus.DELAYED) {
            throw new IllegalStateException("只有配送中或延遲的配送可以標記為配送失敗");
        }

        this.status = DeliveryStatus.DELIVERY_FAILED;
        this.failureReason = Objects.requireNonNull(reason, "失敗原因不能為空");
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 標記為拒收
     *
     * @param reason 拒收原因
     */
    public void markAsRefused(String reason) {
        if (status != DeliveryStatus.IN_TRANSIT) {
            throw new IllegalStateException("只有配送中的配送可以標記為拒收");
        }

        this.status = DeliveryStatus.REFUSED;
        this.failureReason = Objects.requireNonNull(reason, "拒收原因不能為空");
        this.updatedAt = LocalDateTime.now();
    }

    /** 安排重新配送 */
    public void scheduleRedelivery() {
        if (status != DeliveryStatus.DELIVERY_FAILED) {
            throw new IllegalStateException("只有配送失敗的配送可以安排重新配送");
        }

        this.status = DeliveryStatus.PENDING_SHIPMENT;
        this.redeliveryScheduled = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新配送地址
     *
     * @param newAddress 新地址
     */
    public void updateShippingAddress(String newAddress) {
        if (status != DeliveryStatus.PENDING_SHIPMENT) {
            throw new IllegalStateException("只有待出貨的配送可以更新地址");
        }

        this.shippingAddress = Objects.requireNonNull(newAddress, "配送地址不能為空");
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public UUID getId() {
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

    public LocalDateTime getActualDeliveryTime() {
        return actualDeliveryTime;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public boolean isRedeliveryScheduled() {
        return redeliveryScheduled;
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
