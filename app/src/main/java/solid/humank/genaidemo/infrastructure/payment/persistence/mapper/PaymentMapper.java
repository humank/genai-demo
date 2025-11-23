package solid.humank.genaidemo.infrastructure.payment.persistence.mapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Currency;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.PaymentId;
import solid.humank.genaidemo.domain.common.valueobject.PaymentStatus;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;
import solid.humank.genaidemo.domain.payment.model.valueobject.PaymentMethod;
import solid.humank.genaidemo.infrastructure.common.persistence.mapper.DomainMapper;
import solid.humank.genaidemo.infrastructure.payment.persistence.entity.JpaPaymentEntity;

/** 支付映射器 負責在領域模型和持久化模型之間進行轉換 */
@Component
public class PaymentMapper implements DomainMapper<Payment, JpaPaymentEntity> {

    /**
     * 將領域模型轉換為持久化模型
     *
     * @param payment 支付領域模型
     * @return 支付持久化模型
     */
    @Override
    public JpaPaymentEntity toJpaEntity(Payment payment) {
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
     * 將持久化模型轉換為領域模型 使用反射機制重建支付聚合根，保持其完整性
     *
     * @param jpaEntity 支付持久化模型
     * @return 支付領域模型
     */
    @Override
    public Payment toDomainModel(JpaPaymentEntity jpaEntity) {
        try {
            // 創建必要的值對象
            PaymentId paymentId = PaymentId.fromString(jpaEntity.getId());
            OrderId orderId = OrderId.fromString(jpaEntity.getOrderId());
            Money amount = Money.of(jpaEntity.getAmount(), Currency.getInstance(jpaEntity.getCurrency()));

            // 使用反射創建Payment實例
            Constructor<Payment> constructor = Payment.class.getDeclaredConstructor(
                    PaymentId.class,
                    OrderId.class,
                    Money.class,
                    PaymentStatus.class,
                    PaymentMethod.class,
                    String.class,
                    String.class,
                    LocalDateTime.class,
                    LocalDateTime.class,
                    boolean.class);
            constructor.setAccessible(true);

            return constructor.newInstance(
                    paymentId,
                    orderId,
                    amount,
                    jpaEntity.getStatus(),
                    jpaEntity.getPaymentMethod(),
                    jpaEntity.getTransactionId(),
                    jpaEntity.getFailureReason(),
                    jpaEntity.getCreatedAt(),
                    jpaEntity.getUpdatedAt(),
                    jpaEntity.isCanRetry());
        } catch (Exception e) {
            // 如果反射失敗，使用備用方法
            return createPaymentAlternative(jpaEntity);
        }
    }

    /** 備用方法：當反射方法失敗時使用 */
    private Payment createPaymentAlternative(JpaPaymentEntity jpaEntity) {
        // 創建支付聚合根
        OrderId orderId = OrderId.fromString(jpaEntity.getOrderId());
        Money amount = Money.of(jpaEntity.getAmount(), Currency.getInstance(jpaEntity.getCurrency()));

        // 創建基本的Payment對象
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

        // 嘗試使用反射設置其他屬性
        try {
            // 設置ID
            Field idField = Payment.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(payment, PaymentId.fromString(jpaEntity.getId()));

            // 設置創建時間和更新時間
            Field createdAtField = Payment.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(payment, jpaEntity.getCreatedAt());

            Field updatedAtField = Payment.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(payment, jpaEntity.getUpdatedAt());
        } catch (Exception e) {
            // 忽略反射錯誤，使用已設置的屬性
        }

        return payment;
    }

}
