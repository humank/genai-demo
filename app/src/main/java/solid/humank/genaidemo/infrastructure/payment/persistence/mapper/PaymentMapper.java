package solid.humank.genaidemo.infrastructure.payment.persistence.mapper;

import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.PaymentId;
import solid.humank.genaidemo.domain.common.valueobject.PaymentStatus;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;
import solid.humank.genaidemo.domain.payment.model.valueobject.PaymentMethod;
import solid.humank.genaidemo.infrastructure.payment.persistence.entity.JpaPaymentEntity;

import java.util.Currency;
import java.util.UUID;

/**
 * 支付映射器
 * 負責在領域模型和持久化模型之間進行轉換
 */
public class PaymentMapper {
    
    /**
     * 將領域模型轉換為持久化模型
     * 
     * @param payment 支付領域模型
     * @return 支付持久化模型
     */
    public static JpaPaymentEntity toJpaEntity(Payment payment) {
        JpaPaymentEntity jpaEntity = new JpaPaymentEntity();
        jpaEntity.setId(payment.getId().toString());
        jpaEntity.setOrderId(payment.getOrderId().toString());
        jpaEntity.setAmount(payment.getAmount().getAmount());
        jpaEntity.setCurrency(payment.getAmount().getCurrency().getCurrencyCode());
        jpaEntity.setStatus(payment.getStatus());
        
        if (payment.getPaymentMethod() != null) {
            jpaEntity.setPaymentMethod(payment.getPaymentMethod());
        }
        
        jpaEntity.setTransactionId(payment.getTransactionId());
        jpaEntity.setFailureReason(payment.getFailureReason());
        jpaEntity.setCreatedAt(payment.getCreatedAt());
        jpaEntity.setUpdatedAt(payment.getUpdatedAt());
        jpaEntity.setCanRetry(payment.canRetry());
        
        return jpaEntity;
    }
    
    /**
     * 將持久化模型轉換為領域模型
     * 注意：這裡使用了一個簡化的方法來重建支付聚合根
     * 在實際應用中，可能需要更複雜的重建邏輯
     * 
     * @param jpaEntity 支付持久化模型
     * @return 支付領域模型
     */
    public static Payment toDomainEntity(JpaPaymentEntity jpaEntity) {
        // 創建支付聚合根
        OrderId orderId = OrderId.of(jpaEntity.getOrderId());
        Money amount = Money.of(
                jpaEntity.getAmount(),
                Currency.getInstance(jpaEntity.getCurrency())
        );
        
        // 使用反射或其他方式重建支付聚合根
        // 這裡使用一個簡化的方法
        Payment payment = new Payment(orderId, amount);
        
        // 設置支付方式
        if (jpaEntity.getPaymentMethod() != null) {
            payment.setPaymentMethod(jpaEntity.getPaymentMethod());
        }
        
        // 根據狀態設置相應的屬性
        PaymentStatus status = jpaEntity.getStatus();
        switch (status) {
            case COMPLETED:
                payment.complete(jpaEntity.getTransactionId());
                break;
            case FAILED:
                payment.fail(jpaEntity.getFailureReason());
                break;
            case REFUNDED:
                payment.complete(jpaEntity.getTransactionId());
                payment.refund();
                break;
            default:
                // 保持 PENDING 狀態
                break;
        }
        
        // 注意：這裡我們沒有處理所有可能的狀態和屬性
        // 在實際應用中，可能需要更複雜的重建邏輯，例如：
        // - 處理支付歷史記錄
        // - 處理支付事件
        // - 處理支付關聯的訂單
        
        return payment;
    }
}