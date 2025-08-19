package solid.humank.genaidemo.infrastructure.order.persistence.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.aggregate.OrderWorkflow;
import solid.humank.genaidemo.domain.order.model.valueobject.WorkflowId;
import solid.humank.genaidemo.domain.order.model.valueobject.WorkflowStatus;
import solid.humank.genaidemo.domain.order.repository.OrderWorkflowRepository;
import solid.humank.genaidemo.infrastructure.common.persistence.adapter.BaseRepositoryAdapter;
import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderWorkflowEntity;
import solid.humank.genaidemo.infrastructure.order.persistence.mapper.OrderWorkflowMapper;
import solid.humank.genaidemo.infrastructure.order.persistence.repository.JpaOrderWorkflowRepository;

/**
 * 訂單工作流儲存庫適配器
 * 實現領域儲存庫接口，遵循統一的 Repository Pattern
 */
@Component
@Transactional
public class OrderWorkflowRepositoryAdapter
        extends BaseRepositoryAdapter<OrderWorkflow, WorkflowId, JpaOrderWorkflowEntity, String>
        implements OrderWorkflowRepository {

    private final OrderWorkflowMapper mapper;

    public OrderWorkflowRepositoryAdapter(JpaOrderWorkflowRepository jpaRepository, OrderWorkflowMapper mapper) {
        super(jpaRepository);
        this.mapper = mapper;
    }

    @Override
    protected JpaOrderWorkflowEntity toJpaEntity(OrderWorkflow aggregateRoot) {
        return mapper.toJpaEntity(aggregateRoot);
    }

    @Override
    protected OrderWorkflow toDomainModel(JpaOrderWorkflowEntity entity) {
        return mapper.toDomainModel(entity);
    }

    @Override
    protected String convertToJpaId(WorkflowId domainId) {
        return domainId.getValue();
    }

    @Override
    protected WorkflowId extractId(OrderWorkflow aggregateRoot) {
        return aggregateRoot.getId();
    }

    @Override
    public Optional<OrderWorkflow> findByOrderId(OrderId orderId) {
        return ((JpaOrderWorkflowRepository) jpaRepository).findByOrderId(orderId.getValue())
                .map(mapper::toDomainModel);
    }

    @Override
    public List<OrderWorkflow> findByStatus(WorkflowStatus status) {
        return ((JpaOrderWorkflowRepository) jpaRepository).findByStatus(status.name())
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderWorkflow> findByCreatedAtAfter(LocalDateTime time) {
        return ((JpaOrderWorkflowRepository) jpaRepository).findByCreatedAtAfter(time)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderWorkflow> findByUpdatedAtAfter(LocalDateTime time) {
        return ((JpaOrderWorkflowRepository) jpaRepository).findByUpdatedAtAfter(time)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderWorkflow> findCancelledWorkflows(String reason) {
        return ((JpaOrderWorkflowRepository) jpaRepository).findCancelledWorkflows(reason)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
}