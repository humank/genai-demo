package solid.humank.genaidemo.domain.payment.events;

import solid.humank.genaidemo.domain.common.event.AbstractDomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.PaymentId;

import java.util.UUID;

/**
 * 支付請求事件
 */
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
    
    /**
     * 從 UUID 創建支付請求事件
     * 
     * @param paymentId 支付ID
     * @param orderId 訂單ID
     * @param amount 金額
     * @return 支付請求事件
     */
    public static PaymentRequestedEvent fromUUID(UUID paymentId, UUID orderId, Money amount) {
        return new PaymentRequestedEvent(
                PaymentId.fromUUID(paymentId),
                OrderId.fromUUID(orderId),
                amount
        );
    }
    
    /**
     * 從字符串創建支付請求事件
     * 
     * @param paymentId 支付ID
     * @param orderId 訂單ID
     * @param amount 金額
     * @return 支付請求事件
     */
    public static PaymentRequestedEvent fromString(String paymentId, String orderId, Money amount) {
        return new PaymentRequestedEvent(
                PaymentId.fromString(paymentId),
                OrderId.of(orderId),
                amount
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
    
    @Override
    public String getEventType() {
        return "PaymentRequestedEvent";
    }
}