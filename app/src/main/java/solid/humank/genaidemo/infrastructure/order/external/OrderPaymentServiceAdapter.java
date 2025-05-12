package solid.humank.genaidemo.infrastructure.order.external;

import org.springframework.stereotype.Component;
import solid.humank.genaidemo.application.order.port.outgoing.PaymentServicePort;
import solid.humank.genaidemo.application.payment.port.incoming.PaymentManagementUseCase;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.PaymentResult;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;

import java.util.Optional;
import java.util.UUID;

/**
 * 訂單支付服務適配器
 * 實現應用層的 PaymentServicePort 接口
 * 使用 PaymentManagementUseCase 進行實際的支付操作
 */
@Component
public class OrderPaymentServiceAdapter implements PaymentServicePort {

    private final PaymentManagementUseCase paymentManagementUseCase;

    public OrderPaymentServiceAdapter(PaymentManagementUseCase paymentManagementUseCase) {
        this.paymentManagementUseCase = paymentManagementUseCase;
    }

    /**
     * 處理訂單支付
     */
    @Override
    public PaymentResult processPayment(UUID orderId, Money amount) {
        try {
            // 調用支付管理用例處理支付
            Payment payment = paymentManagementUseCase.processPayment(orderId, amount);
            
            // 返回支付結果
            return PaymentResult.successful(payment.getId().toString());
        } catch (Exception e) {
            // 返回失敗結果
            return PaymentResult.failed("Payment processing failed: " + e.getMessage());
        }
    }

    /**
     * 取消支付
     */
    @Override
    public PaymentResult cancelPayment(UUID orderId) {
        try {
            // 查找訂單的支付
            Optional<Payment> paymentOpt = paymentManagementUseCase.getPaymentByOrderId(orderId);
            
            if (paymentOpt.isPresent()) {
                // 取消支付
                paymentManagementUseCase.cancelPayment(paymentOpt.get().getIdAsUUID());
                
                // 返回成功結果
                return PaymentResult.successful(paymentOpt.get().getId().toString());
            } else {
                // 沒有找到支付
                return PaymentResult.failed("Payment not found for order: " + orderId);
            }
        } catch (Exception e) {
            // 返回失敗結果
            return PaymentResult.failed("Payment cancellation failed: " + e.getMessage());
        }
    }

    /**
     * 查詢支付狀態
     */
    @Override
    public PaymentResult getPaymentStatus(UUID orderId) {
        try {
            // 查找訂單的支付
            Optional<Payment> paymentOpt = paymentManagementUseCase.getPaymentByOrderId(orderId);
            
            if (paymentOpt.isPresent()) {
                // 返回支付狀態
                return PaymentResult.successful(paymentOpt.get().getStatus().toString());
            } else {
                // 沒有找到支付
                return PaymentResult.failed("Payment not found for order: " + orderId);
            }
        } catch (Exception e) {
            // 返回失敗結果
            return PaymentResult.failed("Payment status query failed: " + e.getMessage());
        }
    }

    /**
     * 退款處理
     */
    @Override
    public PaymentResult processRefund(UUID orderId, Money amount) {
        try {
            // 查找訂單的支付
            Optional<Payment> paymentOpt = paymentManagementUseCase.getPaymentByOrderId(orderId);
            
            if (paymentOpt.isPresent()) {
                // 退款
                paymentManagementUseCase.refundPayment(paymentOpt.get().getIdAsUUID());
                
                // 返回成功結果
                return PaymentResult.successful(paymentOpt.get().getId().toString());
            } else {
                // 沒有找到支付
                return PaymentResult.failed("Payment not found for order: " + orderId);
            }
        } catch (Exception e) {
            // 返回失敗結果
            return PaymentResult.failed("Payment refund failed: " + e.getMessage());
        }
    }
}