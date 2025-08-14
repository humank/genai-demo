package solid.humank.genaidemo.domain.payment.model.events;

import solid.humank.genaidemo.domain.common.event.AbstractDomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.PaymentId;

/** 支付失敗事件 */
public class PaymentFailedEvent extends AbstractDomainEvent {

    private final PaymentId paymentId;
    private final OrderId orderId;
    private final Money amount;
    private final String failureReason;
    private final boolean canRetry;

    public PaymentFailedEvent(
            PaymentId paymentId,
            OrderId orderId,
            Money amount,
            String failureReason,
            boolean canRetry) {
        super("payment-service");
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.failureReason = failureReason;
        this.canRetry = canRetry;
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

    public String getFailureReason() {
        return failureReason;
    }

    public boolean isCanRetry() {
        return canRetry;
    }

    @Override
    public String getEventType() {
        return "PaymentFailedEvent";
    }
}
