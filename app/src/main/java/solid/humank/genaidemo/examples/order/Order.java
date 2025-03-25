package solid.humank.genaidemo.examples.order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import solid.humank.genaidemo.ddd.annotations.AggregateRoot;
import solid.humank.genaidemo.ddd.lifecycle.AggregateLifecycle;
import solid.humank.genaidemo.ddd.lifecycle.AggregateLifecycle.ManagedLifecycle;
import solid.humank.genaidemo.examples.order.events.OrderCreatedEvent;
import solid.humank.genaidemo.examples.order.events.OrderItemAddedEvent;
import solid.humank.genaidemo.exceptions.ValidationException;

@AggregateRoot
@ManagedLifecycle
public class Order {
    private final OrderId orderId;
    private final String customerId;
    private final List<OrderItem> items;
    private Money totalAmount;
    private Money finalAmount;
    private final OrderStatus status;

    protected Order() {
        this.orderId = OrderId.generate();
        this.customerId = "";
        this.items = new ArrayList<>();
        this.totalAmount = Money.twd(0);
        this.status = OrderStatus.CREATED;
    }

    public Order(String customerId) {
        // 前置條件檢查
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("客戶ID不能為空");
        }
        
        this.orderId = OrderId.generate();
        this.customerId = customerId;
        this.items = new ArrayList<>();
        this.totalAmount = Money.twd(0);
        this.status = OrderStatus.CREATED;

        // 發布訂單建立事件
        AggregateLifecycle.apply(new OrderCreatedEvent(
            this.orderId,
            this.customerId,
            this.totalAmount,
            new ArrayList<>()
        ));
    }

    public void addItem(String productId, String productName, int quantity, Money unitPrice) {
        // 參數驗證：雖然 OrderItem 構造函數會驗證，但這裡再次驗證是防禦性編程的良好實踐
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("商品ID不能為空");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("商品名稱不能為空");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("商品數量必須大於零");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("單價不能為空");
        }
        
        OrderItem item = new OrderItem(productId, productName, quantity, unitPrice);
        items.add(item);
        recalculateTotal();
        
        // 發布商品添加事件
        AggregateLifecycle.apply(new OrderItemAddedEvent(
            this.orderId,
            productId,
            quantity,
            unitPrice
        ));
    }

    private void recalculateTotal() {
        this.totalAmount = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(Money.twd(0), Money::add);
    }

    public List<Map<String, Object>> getItemsAsMap() {
        return items.stream()
            .map(item -> {
                Map<String, Object> map = new HashMap<>();
                map.put("productId", item.productId());
                map.put("productName", item.productName());
                map.put("quantity", item.quantity());
                map.put("unitPrice", item.unitPrice());
                map.put("subtotal", item.getSubtotal());
                return map;
            })
            .toList();
    }

    // Getters
    public OrderId getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<OrderItem> getItems() {
        return new ArrayList<>(items);
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void applyDiscount(Money discountedAmount) {
        // 參數驗證
        if (discountedAmount == null) {
            throw new IllegalArgumentException("折扣金額不能為空");
        }
        
        // 業務規則檢查：折扣後金額不應為負數
        if (discountedAmount.amount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("折扣後金額不能為負數");
        }
        
        this.finalAmount = discountedAmount;
    }
    
    /**
     * 驗證訂單是否可以被處理
     * 
     * @throws ValidationException 如果訂單無效則拋出此異常
     */
    public void validateForProcessing() {
        List<String> errors = new ArrayList<>();
        
        if (items.isEmpty()) {
            errors.add("訂單沒有商品項目");
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
    
    /**
     * 處理訂單
     * 這個方法遵循 Tell, Don't Ask 原則，讓聚合根自己執行業務邏輯
     */
    public void process() {
        validateForProcessing();
        // 訂單處理的其他業務邏輯可以在這裡添加
    }

    public Money getFinalAmount() {
        return finalAmount;
    }

    public UUID getId() {
        return this.orderId.id();
    }

    @Override
    public String toString() {
        return String.format(
            "訂單 ID: %s%n客戶 ID: %s%n狀態: %s%n總金額: %s%n品項數量: %d",
            orderId,
            customerId,
            status,
            totalAmount,
            items.size()
        );
    }
}
