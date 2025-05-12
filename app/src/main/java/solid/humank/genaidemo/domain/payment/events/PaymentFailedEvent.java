package solid.humank.genaidemo.domain.payment.events;

import solid.humank.genaidemo.domain.common.events.AbstractDomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.PaymentId;

import java.util.UUID;

/**
 * 支付失敗事件
 */
public class PaymentFailedEvent extends AbstractDomainEvent {
    
    private final PaymentId paymentId;
    private final OrderId orderId;
    private final Money amount;
    private final String failureReason;
    
    public PaymentFailedEvent(PaymentId paymentId, OrderId orderId, Money amount, String failureReason) {
        super("payment-service");
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.failureReason = failureReason;
    }
    
    /**
     * 從 UUID 創建支付失敗事件
     * 
     * @param paymentId 支付ID
     * @param orderId 訂單ID
     * @param failureReason 失敗原因
     * @return 支付失敗事件
     */
    public static PaymentFailedEvent fromUUID(UUID paymentId, UUID orderId, String failureReason) {
        return new PaymentFailedEvent(
                PaymentId.fromUUID(paymentId),
                OrderId.fromUUID(orderId),
                Money.zero(), // 默認金額為零，實際應用中應該提供正確的金額
                failureReason
        );
    }
    
    /**
     * 從 UUID 創建支付失敗事件
     * 
     * @param paymentId 支付ID
     * @param orderId 訂單ID
     * @param amount 金額
     * @param failureReason 失敗原因
     * @return 支付失敗事件
     */
    public static PaymentFailedEvent fromUUID(UUID paymentId, UUID orderId, Money amount, String failureReason) {
        return new PaymentFailedEvent(
                PaymentId.fromUUID(paymentId),
                OrderId.fromUUID(orderId),
                amount,
                failureReason
        );
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
    
    @Override
    public String getEventType() {
        return "PaymentFailedEvent";
    }
}