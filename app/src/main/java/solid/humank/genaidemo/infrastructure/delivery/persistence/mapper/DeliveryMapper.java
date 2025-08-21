package solid.humank.genaidemo.infrastructure.delivery.persistence.mapper;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.delivery.model.aggregate.Delivery;
import solid.humank.genaidemo.domain.delivery.model.valueobject.DeliveryId;
import solid.humank.genaidemo.domain.delivery.model.valueobject.DeliveryStatus;
import solid.humank.genaidemo.infrastructure.common.persistence.mapper.DomainMapper;
import solid.humank.genaidemo.infrastructure.delivery.persistence.entity.JpaDeliveryEntity;

/**
 * 配送映射器
 * 負責在領域模型和JPA實體之間進行轉換
 */
@Component
public class DeliveryMapper implements DomainMapper<Delivery, JpaDeliveryEntity> {

    @Override
    public JpaDeliveryEntity toJpaEntity(Delivery delivery) {
        if (delivery == null) {
            return null;
        }

        return new JpaDeliveryEntity(
                delivery.getId().getId(),
                delivery.getOrderId().getValue(),
                delivery.getShippingAddress(),
                delivery.getStatus().name(),
                delivery.getDeliveryPersonName(),
                delivery.getDeliveryPersonContact(),
                delivery.getEstimatedDeliveryTime(),
                delivery.getActualDeliveryTime(),
                delivery.getFailureReason(),
                delivery.getRefusalReason(),
                delivery.getDelayReason(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt());
    }

    @Override
    public Delivery toDomainModel(JpaDeliveryEntity entity) {
        if (entity == null) {
            return null;
        }

        // Create basic delivery object
        Delivery delivery = new Delivery(
                DeliveryId.fromUUID(entity.getId()),
                OrderId.of(entity.getOrderId()),
                entity.getShippingAddress());

        // Use reflection to set the internal state since this is infrastructure layer
        // and we need to reconstruct the aggregate from persistence
        try {
            setFieldValue(delivery, "status", DeliveryStatus.valueOf(entity.getStatus()));

            if (entity.getDeliveryPersonName() != null) {
                setFieldValue(delivery, "deliveryPersonName", entity.getDeliveryPersonName());
            }
            if (entity.getDeliveryPersonContact() != null) {
                setFieldValue(delivery, "deliveryPersonContact", entity.getDeliveryPersonContact());
            }
            if (entity.getEstimatedDeliveryTime() != null) {
                setFieldValue(delivery, "estimatedDeliveryTime", entity.getEstimatedDeliveryTime());
            }
            if (entity.getActualDeliveryTime() != null) {
                setFieldValue(delivery, "actualDeliveryTime", entity.getActualDeliveryTime());
            }
            if (entity.getFailureReason() != null) {
                setFieldValue(delivery, "failureReason", entity.getFailureReason());
            }
            if (entity.getRefusalReason() != null) {
                setFieldValue(delivery, "refusalReason", entity.getRefusalReason());
            }
            if (entity.getDelayReason() != null) {
                setFieldValue(delivery, "delayReason", entity.getDelayReason());
            }

            // Set timestamps using reflection
            setFieldValue(delivery, "updatedAt", entity.getUpdatedAt());

        } catch (Exception e) {
            throw new RuntimeException("Failed to reconstruct Delivery aggregate from persistence", e);
        }

        return delivery;
    }

    private void setFieldValue(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}