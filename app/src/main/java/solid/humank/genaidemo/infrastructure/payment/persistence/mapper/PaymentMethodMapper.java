package solid.humank.genaidemo.infrastructure.payment.persistence.mapper;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.payment.model.aggregate.PaymentMethod;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.infrastructure.payment.persistence.entity.JpaPaymentMethodEntity;

/** 支付方式映射器 */
@Component
public class PaymentMethodMapper {

    public JpaPaymentMethodEntity toJpaEntity(PaymentMethod paymentMethod) {
        JpaPaymentMethodEntity entity = new JpaPaymentMethodEntity();
        entity.setPaymentMethodId(paymentMethod.getPaymentMethodId());
        entity.setCustomerId(paymentMethod.getCustomerId().getValue());
        entity.setType(paymentMethod.getType());
        entity.setDisplayName(paymentMethod.getDisplayName());
        entity.setProvider(paymentMethod.getProvider());
        entity.setMaskedNumber(paymentMethod.getMaskedNumber());
        entity.setExpiryDate(paymentMethod.getExpiryDate());
        entity.setDefault(paymentMethod.isDefault());
        entity.setActive(paymentMethod.isActive());
        entity.setCreatedAt(paymentMethod.getCreatedAt());
        entity.setUpdatedAt(paymentMethod.getUpdatedAt());
        return entity;
    }

    public PaymentMethod toDomainModel(JpaPaymentMethodEntity entity) {
        CustomerId customerId = CustomerId.of(entity.getCustomerId());

        return new PaymentMethod(
                entity.getPaymentMethodId(),
                customerId,
                entity.getType(),
                entity.getDisplayName(),
                entity.getProvider(),
                entity.getMaskedNumber());
    }
}