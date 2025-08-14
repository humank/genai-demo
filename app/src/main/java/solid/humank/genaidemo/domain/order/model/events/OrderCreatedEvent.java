package solid.humank.genaidemo.domain.order.model.events;

import java.util.List;
import solid.humank.genaidemo.domain.common.event.AbstractDomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;

/** 訂單創建事件 */
public class OrderCreatedEvent extends AbstractDomainEvent {

    private final OrderId orderId;
    private final String customerId;
    private final Money totalAmount;
    private final List<String> items;

    public OrderCreatedEvent(
            OrderId orderId, String customerId, Money totalAmount, List<String> items) {
        super("order-service");
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.items = items;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public List<String> getItems() {
        return items;
    }

    @Override
    public String getEventType() {
        return "OrderCreatedEvent";
    }
}
