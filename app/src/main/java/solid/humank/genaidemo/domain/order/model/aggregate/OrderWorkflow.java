package solid.humank.genaidemo.domain.order.model.aggregate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.valueobject.WorkflowId;
import solid.humank.genaidemo.domain.order.model.valueobject.WorkflowStatus;

/** 訂單工作流聚合根 管理訂單從創建到完成的整個生命週期 已移至 Order Context */
@AggregateRoot(name = "OrderWorkflow", description = "訂單工作流聚合根，管理訂單從創建到完成的整個生命週期", boundedContext = "Order", version = "1.0")
public class OrderWorkflow extends solid.humank.genaidemo.domain.common.aggregate.AggregateRoot {
    private final WorkflowId id;
    private final OrderId orderId;
    private WorkflowStatus status;
    private final List<String> productIds;
    private boolean inventoryChecked;
    private boolean inventorySufficient;
    private String paymentMethod;
    private boolean paymentProcessed;
    private boolean paymentSuccessful;
    private boolean deliveryArranged;
    private String cancellationReason;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 建立訂單工作流
     *
     * @param orderId 訂單ID
     */
    public OrderWorkflow(OrderId orderId) {
        this(WorkflowId.generate(), orderId);
    }

    /**
     * 建立訂單工作流
     *
     * @param id      工作流ID
     * @param orderId 訂單ID
     */
    public OrderWorkflow(WorkflowId id, OrderId orderId) {
        this.id = Objects.requireNonNull(id, "工作流ID不能為空");
        this.orderId = Objects.requireNonNull(orderId, "訂單ID不能為空");
        this.status = WorkflowStatus.CREATED;
        this.productIds = new ArrayList<>();
        this.inventoryChecked = false;
        this.inventorySufficient = false;
        this.paymentProcessed = false;
        this.paymentSuccessful = false;
        this.deliveryArranged = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * 添加產品
     *
     * @param productId 產品ID
     */
    public void addProduct(String productId) {
        this.productIds.add(Objects.requireNonNull(productId, "產品ID不能為空"));
        this.updatedAt = LocalDateTime.now();
    }

    /** 提交訂單 */
    public void submitOrder() {
        if (status != WorkflowStatus.CREATED) {
            throw new IllegalStateException("只有已創建狀態的工作流可以提交訂單");
        }

        if (productIds.isEmpty()) {
            throw new IllegalStateException("訂單必須包含至少一個產品");
        }

        this.status = WorkflowStatus.PENDING_PAYMENT;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 驗證訂單
     *
     * @return 訂單是否有效
     */
    public boolean validateOrder() {
        // 在實際應用中，這裡可能會有更複雜的驗證邏輯
        return !productIds.isEmpty();
    }

    /**
     * 檢查庫存
     *
     * @param isSufficient 庫存是否充足
     */
    public void checkInventory(boolean isSufficient) {
        this.inventoryChecked = true;
        this.inventorySufficient = isSufficient;
        this.updatedAt = LocalDateTime.now();

        if (!isSufficient) {
            cancelOrder("Insufficient inventory");
        }
    }

    /**
     * 設置支付方式
     *
     * @param paymentMethod 支付方式
     */
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "支付方式不能為空");
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 處理支付
     *
     * @param isSuccessful 支付是否成功
     */
    public void processPayment(boolean isSuccessful) {
        this.paymentProcessed = true;
        this.paymentSuccessful = isSuccessful;
        this.updatedAt = LocalDateTime.now();

        if (isSuccessful) {
            confirmOrder();
        } else {
            cancelOrder("Payment failure");
        }
    }

    /** 確認訂單 */
    public void confirmOrder() {
        if (status != WorkflowStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("只有待支付狀態的工作流可以確認訂單");
        }

        if (!inventoryChecked || !inventorySufficient) {
            throw new IllegalStateException("必須先檢查庫存並確認庫存充足");
        }

        if (!paymentProcessed || !paymentSuccessful) {
            throw new IllegalStateException("必須先處理支付並確認支付成功");
        }

        this.status = WorkflowStatus.CONFIRMED;
        this.updatedAt = LocalDateTime.now();
    }

    /** 安排配送 */
    public void arrangeDelivery() {
        if (status != WorkflowStatus.CONFIRMED) {
            throw new IllegalStateException("只有已確認狀態的工作流可以安排配送");
        }

        this.deliveryArranged = true;
        this.status = WorkflowStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    /** 完成訂單 */
    public void completeOrder() {
        if (status != WorkflowStatus.PROCESSING) {
            throw new IllegalStateException("只有處理中狀態的工作流可以完成訂單");
        }

        if (!deliveryArranged) {
            throw new IllegalStateException("必須先安排配送");
        }

        this.status = WorkflowStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 取消訂單
     *
     * @param reason 取消原因
     */
    public void cancelOrder(String reason) {
        if (status == WorkflowStatus.COMPLETED) {
            throw new IllegalStateException("已完成的訂單不能取消");
        }

        this.status = WorkflowStatus.CANCELLED;
        this.cancellationReason = Objects.requireNonNull(reason, "取消原因不能為空");
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 標記工作流失敗
     *
     * @param reason 失敗原因
     */
    public void failWorkflow(String reason) {
        if (status == WorkflowStatus.COMPLETED || status == WorkflowStatus.CANCELLED) {
            throw new IllegalStateException("已完成或已取消的工作流不能標記為失敗");
        }

        this.status = WorkflowStatus.FAILED;
        this.cancellationReason = Objects.requireNonNull(reason, "失敗原因不能為空");
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public WorkflowId getId() {
        return id;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public List<String> getProductIds() {
        return Collections.unmodifiableList(productIds);
    }

    public boolean isInventoryChecked() {
        return inventoryChecked;
    }

    public boolean isInventorySufficient() {
        return inventorySufficient;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public boolean isPaymentProcessed() {
        return paymentProcessed;
    }

    public boolean isPaymentSuccessful() {
        return paymentSuccessful;
    }

    public boolean isDeliveryArranged() {
        return deliveryArranged;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OrderWorkflow that = (OrderWorkflow) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}