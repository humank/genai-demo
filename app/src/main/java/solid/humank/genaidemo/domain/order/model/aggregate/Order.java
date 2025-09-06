package solid.humank.genaidemo.domain.order.model.aggregate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.aggregate.AggregateReconstruction;
import solid.humank.genaidemo.domain.common.aggregate.AggregateStateTracker;
import solid.humank.genaidemo.domain.common.aggregate.CrossAggregateOperation;
import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.common.exception.BusinessRuleViolationException;
import solid.humank.genaidemo.domain.common.lifecycle.AggregateLifecycle;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.OrderItem;
import solid.humank.genaidemo.domain.common.valueobject.OrderStatus;
import solid.humank.genaidemo.domain.order.model.events.OrderCreatedEvent;
import solid.humank.genaidemo.domain.order.model.events.OrderItemAddedEvent;
import solid.humank.genaidemo.domain.order.model.events.OrderSubmittedEvent;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/** 訂單聚合根 封裝訂單相關的業務規則和行為 合併了EnhancedOrder的功能 */
@AggregateRoot(name = "Order", description = "訂單聚合根，封裝訂單相關的業務規則和行為", boundedContext = "Order", version = "1.0")
@AggregateLifecycle.ManagedLifecycle
public class Order extends solid.humank.genaidemo.domain.common.aggregate.AggregateRoot {
    private final OrderId id;
    private final AggregateStateTracker<Order> stateTracker = new AggregateStateTracker<>(this);
    private final CustomerId customerId;
    private final String shippingAddress;
    private final List<OrderItem> items;
    private OrderStatus status;
    private Money totalAmount;
    private Money effectiveAmount;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 建立訂單
     *
     * @param customerId      客戶ID字符串
     * @param shippingAddress 配送地址
     */
    public Order(String customerId, String shippingAddress) {
        this(OrderId.generate(), CustomerId.of(customerId), shippingAddress);
    }

    /**
     * 建立訂單
     *
     * @param customerId      客戶ID值對象
     * @param shippingAddress 配送地址
     */
    public Order(CustomerId customerId, String shippingAddress) {
        this(OrderId.generate(), customerId, shippingAddress);
    }

    /**
     * 建立訂單
     *
     * @param orderId         訂單ID
     * @param customerId      客戶ID值對象
     * @param shippingAddress 配送地址
     */
    public Order(OrderId orderId, CustomerId customerId, String shippingAddress) {
        Objects.requireNonNull(orderId, "訂單ID不能為空");
        Objects.requireNonNull(customerId, "客戶ID不能為空");
        requireNonEmpty(shippingAddress, "配送地址不能為空");

        this.id = orderId;
        this.customerId = customerId;
        this.shippingAddress = shippingAddress;
        this.items = new ArrayList<>();
        this.status = OrderStatus.CREATED;
        this.totalAmount = Money.zero();
        this.effectiveAmount = Money.zero();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;

        // 收集領域事件
        collectEvent(OrderCreatedEvent.create(
                this.id, this.customerId.toString(), Money.zero(), List.of()));
    }

    /**
     * 建立訂單 (兼容舊代碼)
     *
     * @param orderId         訂單ID
     * @param customerId      客戶ID字符串
     * @param shippingAddress 配送地址
     */
    public Order(OrderId orderId, String customerId, String shippingAddress) {
        this(orderId, CustomerId.of(customerId), shippingAddress);
    }

    /**
     * 用於重建聚合根的完整建構子（僅供Repository使用）
     *
     * @param id              訂單ID
     * @param customerId      客戶ID
     * @param shippingAddress 配送地址
     * @param items           訂單項列表
     * @param status          訂單狀態
     * @param totalAmount     訂單總金額
     * @param effectiveAmount 訂單實際金額
     * @param createdAt       創建時間
     * @param updatedAt       更新時間
     */
    @AggregateReconstruction.ReconstructionConstructor("從持久化狀態重建訂單聚合根")
    protected Order(
            OrderId id,
            CustomerId customerId,
            String shippingAddress,
            List<OrderItem> items,
            OrderStatus status,
            Money totalAmount,
            Money effectiveAmount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "訂單ID不能為空");
        this.customerId = Objects.requireNonNull(customerId, "客戶ID不能為空");
        this.shippingAddress = Objects.requireNonNull(shippingAddress, "配送地址不能為空");
        this.items = new ArrayList<>(Objects.requireNonNull(items, "訂單項列表不能為空"));
        this.status = Objects.requireNonNull(status, "訂單狀態不能為空");
        this.totalAmount = Objects.requireNonNull(totalAmount, "訂單總金額不能為空");
        this.effectiveAmount = Objects.requireNonNull(effectiveAmount, "訂單實際金額不能為空");
        this.createdAt = Objects.requireNonNull(createdAt, "創建時間不能為空");
        this.updatedAt = Objects.requireNonNull(updatedAt, "更新時間不能為空");

        // 注意：這裡不發布領域事件，因為這是重建聚合根，而不是創建新訂單
    }

    /**
     * 建立訂單
     *
     * @param orderId 訂單ID字符串
     */
    public Order(String orderId) {
        this(OrderId.of(orderId), CustomerId.of("customer-123"), "台北市信義區");
    }

    /**
     * 添加訂單項
     *
     * @param productId   產品ID
     * @param productName 產品名稱
     * @param quantity    數量
     * @param price       價格
     */
    public void addItem(String productId, String productName, int quantity, Money price) {
        // 檢查訂單狀態
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException(
                    "Cannot add items to an order that is not in CREATED state");
        }

        // 創建訂單項
        OrderItem item = new OrderItem(productId, productName, quantity, price);
        items.add(item);

        // 更新總金額
        totalAmount = totalAmount.add(item.getSubtotal());
        effectiveAmount = totalAmount;
        updatedAt = LocalDateTime.now();

        // 收集領域事件
        collectEvent(OrderItemAddedEvent.create(this.id, productId, quantity, price));
    }

    /** 提交訂單 */
    public void submit() {
        // 驗證業務規則
        validateOrderSubmission();

        OrderStatus oldStatus = this.status;

        // 使用狀態追蹤器追蹤變化並自動產生事件
        stateTracker.trackChange("status", oldStatus, OrderStatus.PENDING,
                (oldValue, newValue) -> OrderSubmittedEvent.create(
                        this.id, this.customerId.toString(), this.totalAmount, this.items.size()));

        // 更新狀態
        status = OrderStatus.PENDING;
        updatedAt = LocalDateTime.now();

        // 跨聚合根操作：通知庫存系統預留商品
        CrossAggregateOperation.publishEvent(this,
                new solid.humank.genaidemo.domain.order.model.events.OrderInventoryReservationRequestedEvent(
                        this.id, this.customerId, this.items));
    }

    /** 驗證訂單提交的業務規則 */
    private void validateOrderSubmission() {
        BusinessRuleViolationException.Builder violationBuilder = new BusinessRuleViolationException.Builder("Order",
                this.id.getValue());

        if (items.isEmpty()) {
            violationBuilder.addError("ORDER_ITEMS_REQUIRED", "Cannot submit an order with no items");
        }

        if (status != OrderStatus.CREATED) {
            violationBuilder.addError("ORDER_STATUS_INVALID",
                    String.format("只有狀態為 CREATED 的訂單可以提交，當前狀態：%s", status));
        }

        if (totalAmount == null || totalAmount.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            violationBuilder.addError("ORDER_AMOUNT_INVALID", "訂單金額必須大於零");
        }

        // 檢查是否有錯誤並拋出異常
        BusinessRuleViolationException exception = violationBuilder.buildIfHasErrors();
        if (exception != null) {
            throw exception;
        }
    }

    /** 確認訂單 */
    public void confirm() {
        // 驗證業務規則
        validateOrderConfirmation();

        OrderStatus oldStatus = this.status;

        // 使用狀態追蹤器追蹤變化並自動產生事件
        stateTracker.trackChange("status", oldStatus, OrderStatus.CONFIRMED,
                (oldValue, newValue) -> new solid.humank.genaidemo.domain.order.model.events.OrderConfirmedEvent(
                        this.id, this.customerId, this.totalAmount));

        // 更新狀態
        status = OrderStatus.CONFIRMED;
        updatedAt = LocalDateTime.now();

        // 跨聚合根操作：通知支付系統準備支付
        CrossAggregateOperation.publishEvent(this,
                new solid.humank.genaidemo.domain.order.model.events.OrderPaymentRequestedEvent(
                        this.id, this.customerId, this.effectiveAmount));
    }

    /** 驗證訂單確認的業務規則 */
    private void validateOrderConfirmation() {
        if (!status.canTransitionTo(OrderStatus.CONFIRMED)) {
            throw new BusinessRuleViolationException("Order", this.id.getValue(),
                    "ORDER_CONFIRMATION_INVALID",
                    String.format("無法確認狀態為 %s 的訂單", status));
        }
    }

    /** 標記為已付款 */
    public void markAsPaid() {
        // 檢查狀態轉換
        if (!status.canTransitionTo(OrderStatus.PAID)) {
            throw new IllegalStateException("Cannot mark as paid an order in " + status + " state");
        }

        // 更新狀態
        status = OrderStatus.PAID;
        updatedAt = LocalDateTime.now();
    }

    /** 發貨 */
    public void ship() {
        // 檢查狀態轉換
        if (!status.canTransitionTo(OrderStatus.SHIPPING)) {
            throw new IllegalStateException("Cannot ship an order in " + status + " state");
        }

        // 更新狀態
        status = OrderStatus.SHIPPING;
        updatedAt = LocalDateTime.now();
    }

    /** 送達 */
    public void deliver() {
        // 檢查狀態轉換
        if (!status.canTransitionTo(OrderStatus.DELIVERED)) {
            throw new IllegalStateException("Cannot deliver an order in " + status + " state");
        }

        // 更新狀態
        status = OrderStatus.DELIVERED;
        updatedAt = LocalDateTime.now();
    }

    /** 取消訂單 */
    public void cancel() {
        // 檢查狀態
        if (status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Cannot cancel an order that is already delivered or cancelled");
        }

        // 更新狀態
        status = OrderStatus.CANCELLED;
        updatedAt = LocalDateTime.now();
    }

    /** 處理訂單 執行訂單處理流程 */
    public void process() {
        // 檢查狀態
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("只有處於建立狀態的訂單可以處理");
        }

        // 提交訂單
        submit();
    }

    /**
     * 應用折扣
     *
     * @param discountedAmount 折扣後金額
     */
    public void applyDiscount(Money discountedAmount) {
        // 檢查折扣金額
        if (discountedAmount.isGreaterThan(totalAmount)) {
            throw new IllegalArgumentException("折扣金額不能大於訂單總金額");
        }

        // 更新有效金額
        effectiveAmount = totalAmount.subtract(discountedAmount);
        updatedAt = LocalDateTime.now();
    }

    /**
     * 更新訂單項目列表
     *
     * @param updatedItems 更新後的訂單項目列表
     * @return 更新後的訂單
     */
    public Order updateItems(List<OrderItem> updatedItems) {
        // 檢查訂單狀態
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException(
                    "Cannot update items for an order that is not in CREATED state");
        }

        // 清空現有項目並添加新項目
        this.items.clear();
        this.items.addAll(updatedItems);

        // 重新計算總金額
        this.totalAmount = updatedItems.stream().map(OrderItem::getSubtotal).reduce(Money.zero(), Money::add);
        this.effectiveAmount = this.totalAmount;
        this.updatedAt = LocalDateTime.now();

        return this;
    }

    // Getters

    public OrderId getId() {
        return id;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    /**
     * 獲取客戶ID字符串 (兼容舊代碼)
     *
     * @return 客戶ID字符串
     */
    public String getCustomerIdAsString() {
        return customerId.toString();
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public Money getEffectiveAmount() {
        return effectiveAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // 重寫 equals 和 hashCode 方法

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof Order order) {
            return Objects.equals(id, order.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Order{"
                + "id="
                + id
                + ", customerId='"
                + customerId
                + '\''
                + ", status="
                + status
                + ", totalAmount="
                + totalAmount
                + ", items="
                + items.size()
                + '}';
    }

    /**
     * 確保字符串不為空（null 或空字符串）
     * 
     * @param value   要檢查的字符串
     * @param message 錯誤信息
     * @return 輸入的字符串
     * @throws IllegalArgumentException 如果字符串為空
     */
    private static String requireNonEmpty(String value, String message) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * 重建後驗證聚合根狀態
     */
    @AggregateReconstruction.PostReconstruction("驗證重建後的訂單聚合根狀態")
    public void validateReconstructedState() {
        BusinessRuleViolationException.Builder violationBuilder = new BusinessRuleViolationException.Builder("Order",
                this.id.getValue());

        if (this.id == null) {
            violationBuilder.addError("ORDER_ID_REQUIRED", "訂單ID不能為空");
        }

        if (this.customerId == null) {
            violationBuilder.addError("CUSTOMER_ID_REQUIRED", "客戶ID不能為空");
        }

        if (this.shippingAddress == null || this.shippingAddress.isBlank()) {
            violationBuilder.addError("SHIPPING_ADDRESS_REQUIRED", "配送地址不能為空");
        }

        if (this.status == null) {
            violationBuilder.addError("ORDER_STATUS_REQUIRED", "訂單狀態不能為空");
        }

        if (this.totalAmount == null) {
            violationBuilder.addError("TOTAL_AMOUNT_REQUIRED", "訂單總金額不能為空");
        }

        if (this.effectiveAmount == null) {
            violationBuilder.addError("EFFECTIVE_AMOUNT_REQUIRED", "訂單實際金額不能為空");
        }

        if (this.items == null) {
            violationBuilder.addError("ORDER_ITEMS_REQUIRED", "訂單項目列表不能為空");
        }

        // 檢查是否有錯誤並拋出異常
        BusinessRuleViolationException exception = violationBuilder.buildIfHasErrors();
        if (exception != null) {
            throw exception;
        }
    }
}
