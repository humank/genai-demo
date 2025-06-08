package solid.humank.genaidemo.domain.payment.events;

import solid.humank.genaidemo.domain.common.event.AbstractDomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.PaymentId;

/**
 * 支付完成事件
 */
public class PaymentCompletedEvent extends AbstractDomainEvent {
    
    private final PaymentId paymentId;
    private final OrderId orderId;
    private final Money amount;
    private final String transactionId;
    
    public PaymentCompletedEvent(PaymentId paymentId, OrderId orderId, Money amount, String transactionId) {
        super("payment-service");
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.transactionId = transactionId;
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
    
    public String getTransactionId() {
        return transactionId;
    }
    
    @Override
    public String getEventType() {
        return "PaymentCompletedEvent";
    }
}