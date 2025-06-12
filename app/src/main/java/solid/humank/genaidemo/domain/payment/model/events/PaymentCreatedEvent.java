package solid.humank.genaidemo.domain.payment.model.events;

import solid.humank.genaidemo.domain.common.event.AbstractDomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.PaymentId;
import solid.humank.genaidemo.domain.payment.model.valueobject.PaymentMethod;

/**
 * 支付創建事件
 */
public class PaymentCreatedEvent extends AbstractDomainEvent {
    
    private final PaymentId paymentId;
    private final OrderId orderId;
    private final Money amount;
    private final PaymentMethod paymentMethod;
    
    public PaymentCreatedEvent(PaymentId paymentId, OrderId orderId, Money amount, PaymentMethod paymentMethod) {
        super("payment-service");
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
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
    
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
    
    @Override
    public String getEventType() {
        return "PaymentCreatedEvent";
    }
}