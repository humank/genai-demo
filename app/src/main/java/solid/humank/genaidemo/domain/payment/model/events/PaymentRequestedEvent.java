package solid.humank.genaidemo.domain.payment.model.events;

import solid.humank.genaidemo.domain.common.event.AbstractDomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.PaymentId;

/** 支付請求事件 */
public class PaymentRequestedEvent extends AbstractDomainEvent {

    private final PaymentId paymentId;
    private final OrderId orderId;
    private final Money amount;

    public PaymentRequestedEvent(PaymentId paymentId, OrderId orderId, Money amount) {
        super("order-service");
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
    }

    public PaymentId getPaymentId() {
        return paymentId;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public Money getAmount() {
        return amount;
    }

    @Override
    public String getEventType() {
        return "PaymentRequestedEvent";
    }
}
