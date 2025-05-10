package solid.humank.genaidemo.application.payment.port.incoming;

import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 支付管理用例
 * 定義支付管理的操作接口
 */
public interface PaymentManagementUseCase {
    
    /**
     * 處理支付
     */
    Payment processPayment(UUID orderId, Money amount);
    
    /**
     * 獲取支付
     */
    Optional<Payment> getPayment(UUID paymentId);
    
    /**
     * 獲取訂單的支付
     */
    Optional<Payment> getPaymentByOrderId(UUID orderId);
    
    /**
     * 獲取所有支付
     */
    List<Payment> getAllPayments();
    
    /**
     * 退款
     */
    void refundPayment(UUID paymentId);
}