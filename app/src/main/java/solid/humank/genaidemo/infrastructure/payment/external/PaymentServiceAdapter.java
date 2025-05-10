package solid.humank.genaidemo.infrastructure.payment.external;

import org.springframework.stereotype.Component;
import solid.humank.genaidemo.application.order.port.outgoing.PaymentServicePort;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.PaymentResult;

/**
 * 支付服務適配器
 * 實現應用層的 PaymentServicePort 接口
 * 使用內部實現進行實際的支付處理
 */
@Component
public class PaymentServiceAdapter implements PaymentServicePort {

    /**
     * 處理支付
     */
    @Override
    public PaymentResult processPayment(OrderId orderId, Money amount) {
        // 模擬與外部支付系統的交互
        try {
            // 生成支付ID
            String paymentId = "PAY-" + System.currentTimeMillis();
            
            // 返回成功結果
            return PaymentResult.successful(paymentId);
        } catch (Exception e) {
            // 返回失敗結果
            return PaymentResult.failed("Payment processing failed: " + e.getMessage());
        }
    }
    
    /**
     * 取消支付
     */
    @Override
    public PaymentResult cancelPayment(OrderId orderId) {
        // 模擬取消支付
        return PaymentResult.successful("CANCEL-" + orderId.toString());
    }
    
    /**
     * 查詢支付狀態
     */
    @Override
    public PaymentResult getPaymentStatus(OrderId orderId) {
        // 模擬查詢支付狀態
        return PaymentResult.successful("STATUS-" + orderId.toString());
    }
    
    /**
     * 退款處理
     */
    @Override
    public PaymentResult processRefund(OrderId orderId, Money amount) {
        // 模擬退款處理
        return PaymentResult.successful("REFUND-" + orderId.toString());
    }
}