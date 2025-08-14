package solid.humank.genaidemo.domain.order.model.events;

import solid.humank.genaidemo.domain.common.event.AbstractDomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;

/** 訂單提交事件 */
public class OrderSubmittedEvent extends AbstractDomainEvent {

    private final OrderId orderId;
    private final String customerId;
    private final Money totalAmount;
    private final int itemCount;

    public OrderSubmittedEvent(
            OrderId orderId, String customerId, Money totalAmount, int itemCount) {
        super("order-service");
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.itemCount = itemCount;
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

    public int getItemCount() {
        return itemCount;
    }

    @Override
    public String getEventType() {
        return "OrderSubmittedEvent";
    }
}
