package solid.humank.genaidemo.infrastructure.payment.persistence.mapper;

import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;
import solid.humank.genaidemo.infrastructure.payment.persistence.entity.JpaPaymentEntity;

import java.util.UUID;

/**
 * 支付映射器
 * 負責在領域模型和持久化模型之間進行轉換
 */
public class PaymentMapper {

    /**
     * 將領域模型轉換為持久化模型
     */
    public static JpaPaymentEntity toJpaEntity(Payment payment) {
        if (payment == null) {
            return null;
        }
        
        return new JpaPaymentEntity(
                payment.getId().toString(),
                payment.getOrderId().toString(),
                payment.getAmount().getAmount(),
                payment.getAmount().getCurrency(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getTransactionId(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.canRetry()
        );
    }

    /**
     * 將持久化模型轉換為領域模型
     */
    public static Payment toDomainEntity(JpaPaymentEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }
        
        // 創建 Payment 對象
        Payment payment = new Payment(
                UUID.fromString(jpaEntity.getOrderId()),
                Money.of(jpaEntity.getAmount(), jpaEntity.getCurrency()),
                jpaEntity.getPaymentMethod()
        );
        
        // 根據狀態設置相應的屬性
        switch (jpaEntity.getStatus()) {
            case COMPLETED:
                payment.complete(jpaEntity.getTransactionId());
                break;
            case FAILED:
                payment.fail(jpaEntity.getFailureReason());
                break;
            case REFUNDED:
                // 先完成支付，再退款
                payment.complete(jpaEntity.getTransactionId());
                payment.refund();
                break;
            default:
                // PENDING 狀態不需要額外處理
                break;
        }
        
        // 返回領域模型
        return payment;
    }
}