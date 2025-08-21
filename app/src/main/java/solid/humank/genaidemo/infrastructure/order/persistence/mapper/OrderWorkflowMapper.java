package solid.humank.genaidemo.infrastructure.order.persistence.mapper;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.aggregate.OrderWorkflow;
import solid.humank.genaidemo.domain.order.model.valueobject.WorkflowId;
import solid.humank.genaidemo.infrastructure.common.persistence.mapper.DomainMapper;
import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderWorkflowEntity;

/**
 * 訂單工作流映射器
 * 負責在領域模型和JPA實體之間進行轉換
 */
@Component
public class OrderWorkflowMapper implements DomainMapper<OrderWorkflow, JpaOrderWorkflowEntity> {

    @Override
    public JpaOrderWorkflowEntity toJpaEntity(OrderWorkflow orderWorkflow) {
        if (orderWorkflow == null) {
            return null;
        }

        return new JpaOrderWorkflowEntity(
                orderWorkflow.getId().getValue(),
                orderWorkflow.getOrderId().getValue(),
                orderWorkflow.getStatus(),
                orderWorkflow.getCancellationReason(),
                orderWorkflow.getCreatedAt(),
                orderWorkflow.getUpdatedAt());
    }

    @Override
    public OrderWorkflow toDomainModel(JpaOrderWorkflowEntity entity) {
        if (entity == null) {
            return null;
        }

        OrderWorkflow orderWorkflow = new OrderWorkflow(
                WorkflowId.of(entity.getId()),
                OrderId.of(entity.getOrderId()));

        // Use reflection to set the internal state since this is infrastructure layer
        // and we need to reconstruct the aggregate from persistence
        try {
            setFieldValue(orderWorkflow, "status", entity.getStatus());

            if (entity.getCancellationReason() != null) {
                setFieldValue(orderWorkflow, "cancellationReason", entity.getCancellationReason());
            }

            // Set timestamps using reflection
            setFieldValue(orderWorkflow, "updatedAt", entity.getUpdatedAt());

        } catch (Exception e) {
            throw new RuntimeException("Failed to reconstruct OrderWorkflow aggregate from persistence", e);
        }

        return orderWorkflow;
    }

    private void setFieldValue(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}