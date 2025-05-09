package solid.humank.genaidemo.infrastructure.order.external;

import java.util.HashMap;
import java.util.Map;
import solid.humank.genaidemo.domain.order.model.valueobject.Money;
import solid.humank.genaidemo.domain.order.model.valueobject.OrderId;
import solid.humank.genaidemo.application.order.port.outgoing.PaymentServicePort;
import solid.humank.genaidemo.domain.order.model.valueobject.PaymentResult;

/**
 * 外部支付適配器
 * 實現支付服務端口，與外部支付系統交互
 */
public class ExternalPaymentAdapter implements PaymentServicePort {
    // 模擬支付記錄存儲
    private final Map<String, PaymentRecord> paymentRecords = new HashMap<>();
    
    /**
     * 處理支付
     */
    @Override
    public PaymentResult processPayment(OrderId orderId, Money amount) {
        // 模擬與外部支付系統的交互
        try {
            // 生成支付ID
            String paymentId = "PAY-" + System.currentTimeMillis();
            
            // 記錄支付
            paymentRecords.put(paymentId, new PaymentRecord(
                paymentId,
                orderId.toString(),
                amount,
                PaymentStatus.COMPLETED
            ));
            
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
        // 查找訂單的支付記錄
        for (PaymentRecord record : paymentRecords.values()) {
            if (record.orderId.equals(orderId.toString())) {
                // 更新支付狀態
                paymentRecords.put(record.paymentId, new PaymentRecord(
                    record.paymentId,
                    record.orderId,
                    record.amount,
                    PaymentStatus.CANCELLED
                ));
                
                // 返回成功結果
                return PaymentResult.successful(record.paymentId);
            }
        }
        
        // 未找到支付記錄
        return PaymentResult.failed("Payment record not found for order: " + orderId);
    }
    
    /**
     * 查詢支付狀態
     */
    @Override
    public PaymentResult getPaymentStatus(OrderId orderId) {
        // 查找訂單的支付記錄
        for (PaymentRecord record : paymentRecords.values()) {
            if (record.orderId.equals(orderId.toString())) {
                // 返回成功結果
                return PaymentResult.successful(record.paymentId);
            }
        }
        
        // 未找到支付記錄
        return PaymentResult.failed("Payment record not found for order: " + orderId);
    }
    
    /**
     * 退款處理
     */
    @Override
    public PaymentResult processRefund(OrderId orderId, Money amount) {
        // 查找訂單的支付記錄
        for (PaymentRecord record : paymentRecords.values()) {
            if (record.orderId.equals(orderId.toString())) {
                // 檢查支付狀態
                if (record.status != PaymentStatus.COMPLETED) {
                    return PaymentResult.failed("Cannot refund payment with status: " + record.status);
                }
                
                // 更新支付狀態
                paymentRecords.put(record.paymentId, new PaymentRecord(
                    record.paymentId,
                    record.orderId,
                    record.amount,
                    PaymentStatus.REFUNDED
                ));
                
                // 返回成功結果
                return PaymentResult.successful(record.paymentId);
            }
        }
        
        // 未找到支付記錄
        return PaymentResult.failed("Payment record not found for order: " + orderId);
    }
    
    /**
     * 支付狀態枚舉
     */
    private enum PaymentStatus {
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED,
        REFUNDED
    }
    
    /**
     * 支付記錄
     */
    private static class PaymentRecord {
        private final String paymentId;
        private final String orderId;
        private final Money amount;
        private final PaymentStatus status;
        
        public PaymentRecord(String paymentId, String orderId, Money amount, PaymentStatus status) {
            this.paymentId = paymentId;
            this.orderId = orderId;
            this.amount = amount;
            this.status = status;
        }
    }
}