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

    public void setFinalAmount(Money finalAmount) {
        this.finalAmount = finalAmount;
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
