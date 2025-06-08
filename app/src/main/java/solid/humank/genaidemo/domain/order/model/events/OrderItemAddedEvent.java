package solid.humank.genaidemo.domain.order.model.events;

import solid.humank.genaidemo.domain.common.event.AbstractDomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;

/**
 * 訂單項添加事件
 */
public class OrderItemAddedEvent extends AbstractDomainEvent {
    
    private final OrderId orderId;
    private final String productId;
    private final int quantity;
    private final Money price;
    
    public OrderItemAddedEvent(OrderId orderId, String productId, int quantity, Money price) {
        super("order-service");
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }
    
    public OrderId getOrderId() {
        return orderId;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public Money getPrice() {
        return price;
    }
    
    @Override
    public String getEventType() {
        return "OrderItemAddedEvent";
    }
}