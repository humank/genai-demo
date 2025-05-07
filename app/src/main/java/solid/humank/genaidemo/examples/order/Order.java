package solid.humank.genaidemo.examples.order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import solid.humank.genaidemo.ddd.annotations.AggregateRoot;
import solid.humank.genaidemo.ddd.events.DomainEventPublisherService;
import solid.humank.genaidemo.examples.order.events.OrderCreatedEvent;
import solid.humank.genaidemo.examples.order.events.OrderItemAddedEvent;

/**
 * 訂單聚合根
 */
@AggregateRoot
public class Order {
    private final OrderId id;
    private final String customerId;
    private final String shippingAddress;
    private final List<OrderItem> items;
    private OrderStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 額外的效果金額（用於存儲折扣後的金額）
    private Money effectiveAmount;
    
    /**
     * 根據訂單ID字串建立訂單
     */
    public Order(String orderId) {
        this(OrderId.of(orderId), "", "");
    }
    
    /**
     * 根據客戶ID建立訂單
     */
    public Order(String customerId, String shippingAddress) {
        this(OrderId.generate(), customerId, shippingAddress);
    }
    
    /**
     * 完整建構子
     */
    public Order(OrderId id, String customerId, String shippingAddress) {
        this.id = Objects.requireNonNull(id, "Order ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.shippingAddress = Objects.requireNonNull(shippingAddress, "Shipping address cannot be null");
        this.items = new ArrayList<>();
        this.status = OrderStatus.CREATED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        
        // 使用 DomainEventPublisherService 發布事件
        DomainEventPublisherService.publishEvent(
            new OrderCreatedEvent(
                this.id, 
                customerId, 
                Money.zero(), 
                Collections.emptyList()
            )
        );
    }

    /**
     * 添加訂單項
     */
    public void addItem(String productId, String productName, int quantity, Money price) {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("Cannot add items to an order that is not in CREATED state");
        }
        
        OrderItem newItem = new OrderItem(productId, productName, quantity, price);
        this.items.add(newItem);
        this.updatedAt = LocalDateTime.now();
        
        // 使用 DomainEventPublisherService 發布事件
        DomainEventPublisherService.publishEvent(
            new OrderItemAddedEvent(this.id, productId, quantity, price)
        );
    }

    /**
     * 提交訂單
     */
    public void submit() {
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot submit an order with no items");
        }
        
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("Only orders in CREATED state can be submitted");
        }
        
        transitionState(OrderStatus.PENDING);
    }

    /**
     * 確認訂單
     */
    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only orders in PENDING state can be confirmed");
        }
        
        transitionState(OrderStatus.CONFIRMED);
    }

    /**
     * 支付訂單
     */
    public void markAsPaid() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only orders in CONFIRMED state can be marked as paid");
        }
        
        transitionState(OrderStatus.PAID);
    }

    /**
     * 開始配送
     */
    public void ship() {
        if (status != OrderStatus.PAID) {
            throw new IllegalStateException("Only orders in PAID state can be shipped");
        }
        
        transitionState(OrderStatus.SHIPPING);
    }

    /**
     * 訂單送達
     */
    public void deliver() {
        if (status != OrderStatus.SHIPPING) {
            throw new IllegalStateException("Only orders in SHIPPING state can be delivered");
        }
        
        transitionState(OrderStatus.DELIVERED);
    }

    /**
     * 取消訂單
     */
    public void cancel() {
        if (status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel an order that is already delivered or cancelled");
        }
        
        transitionState(OrderStatus.CANCELLED);
    }

    /**
     * 狀態轉換
     */
    private void transitionState(OrderStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException("Cannot transition from " + status + " to " + newStatus);
        }
        
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 計算訂單總金額
     */
    public Money getTotalAmount() {
        return items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(Money.zero(), Money::add);
    }

    // Getters
    public OrderId getId() {
        return id;
    }
    
    public OrderId getOrderId() {
        return getId();
    }

    public String getCustomerId() {
        return customerId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * 處理訂單
     */
    public void process() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("只有處於建立狀態的訂單可以處理");
        }
        submit();
    }
    
    /**
     * 套用折扣
     */
    public void applyDiscount(Money discountAmount) {
        Money totalAmount = getTotalAmount();
        if (discountAmount.isGreaterThan(totalAmount)) {
            throw new IllegalArgumentException("折扣金額不能大於訂單總金額");
        }
        
        this.effectiveAmount = totalAmount.subtract(discountAmount);
    }
    
    /**
     * 取得有效金額（套用折扣後）
     */
    public Money getEffectiveAmount() {
        return effectiveAmount != null ? effectiveAmount : getTotalAmount();
    }
}
