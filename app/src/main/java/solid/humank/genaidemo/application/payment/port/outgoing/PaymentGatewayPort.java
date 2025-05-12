package solid.humank.genaidemo.application.payment.port.outgoing;

import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.payment.model.valueobject.PaymentMethod;

import java.util.UUID;

/**
 * 支付網關端口
 * 定義與外部支付網關交互的接口
 */
public interface PaymentGatewayPort {
    
    /**
     * 處理支付
     * 
     * @param orderId 訂單ID
     * @param amount 金額
     * @param paymentMethod 支付方式
     * @return 交易ID
     */
    String processPayment(UUID orderId, Money amount, PaymentMethod paymentMethod);
    
    /**
     * 退款
     * 
     * @param transactionId 交易ID
     * @param amount 金額
     * @return 是否成功
     */
    boolean refund(String transactionId, Money amount);
    
    /**
     * 取消支付
     * 
     * @param transactionId 交易ID
     * @return 是否成功
     */
    boolean cancel(String transactionId);
}