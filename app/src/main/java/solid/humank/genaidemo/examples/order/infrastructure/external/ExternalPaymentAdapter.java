package solid.humank.genaidemo.examples.order.infrastructure.external;

import org.springframework.stereotype.Service;
import solid.humank.genaidemo.examples.order.Money;
import solid.humank.genaidemo.examples.order.OrderId;
import solid.humank.genaidemo.examples.order.application.port.outgoing.PaymentServicePort;
import solid.humank.genaidemo.examples.order.service.PaymentResult;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 外部支付服務適配器
 * 實現 PaymentServicePort 接口
 * 注意：這是一個簡化的模擬實現，實際應用中會與真實的支付系統整合
 */
@Service
public class ExternalPaymentAdapter implements PaymentServicePort {

    // 模擬支付記錄存儲
    private final Map<String, PaymentRecord> paymentRecords = new ConcurrentHashMap<>();

    @Override
    public PaymentResult processPayment(OrderId orderId, Money amount) {
        // 模擬外部支付處理
        String paymentId = UUID.randomUUID().toString();
        
        // 模擬成功率95%
        boolean isSuccessful = Math.random() > 0.05;
        
        if (isSuccessful) {
            paymentRecords.put(orderId.toString(), new PaymentRecord(
                    paymentId,
                    orderId.toString(),
                    amount,
                    PaymentStatus.PAID
            ));
            
            return PaymentResult.successful(paymentId);
        } else {
            return PaymentResult.failed("支付處理失敗，請稍後重試");
        }
    }

    @Override
    public PaymentResult cancelPayment(OrderId orderId) {
        PaymentRecord record = paymentRecords.get(orderId.toString());
        
        if (record == null) {
            return PaymentResult.failed("找不到相應的支付記錄");
        }
        
        // 更新支付狀態為已取消
        record.status = PaymentStatus.CANCELLED;
        paymentRecords.put(orderId.toString(), record);
        
        return PaymentResult.successful(record.paymentId);
    }

    @Override
    public PaymentResult getPaymentStatus(OrderId orderId) {
        PaymentRecord record = paymentRecords.get(orderId.toString());
        
        if (record == null) {
            return PaymentResult.failed("找不到相應的支付記錄");
        }
        
        return PaymentResult.successful(record.paymentId);
    }

    @Override
    public PaymentResult processRefund(OrderId orderId, Money amount) {
        PaymentRecord record = paymentRecords.get(orderId.toString());
        
        if (record == null) {
            return PaymentResult.failed("找不到相應的支付記錄");
        }
        
        if (record.status != PaymentStatus.PAID) {
            return PaymentResult.failed("只有已支付的訂單可以退款");
        }
        
        // 更新支付狀態為已退款
        record.status = PaymentStatus.REFUNDED;
        paymentRecords.put(orderId.toString(), record);
        
        return PaymentResult.successful(record.paymentId);
    }

    /**
     * 支付記錄內部類
     */
    private static class PaymentRecord {
        private final String paymentId;
        private final String orderId;
        private final Money amount;
        private PaymentStatus status;

        public PaymentRecord(String paymentId, String orderId, Money amount, PaymentStatus status) {
            this.paymentId = paymentId;
            this.orderId = orderId;
            this.amount = amount;
            this.status = status;
        }
    }

    /**
     * 支付狀態枚舉
     */
    private enum PaymentStatus {
        PENDING,
        PAID,
        CANCELLED,
        REFUNDED,
        FAILED
    }
}
