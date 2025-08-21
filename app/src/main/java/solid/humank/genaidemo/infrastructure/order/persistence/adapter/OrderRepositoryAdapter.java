package solid.humank.genaidemo.infrastructure.order.persistence.adapter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.order.repository.OrderRepository;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.infrastructure.common.persistence.adapter.BaseRepositoryAdapter;
import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderEntity;
import solid.humank.genaidemo.infrastructure.order.persistence.mapper.OrderMapper;
import solid.humank.genaidemo.infrastructure.order.persistence.repository.JpaOrderRepository;

/** 訂單儲存庫適配器 實現領域儲存庫接口，並使用基礎設施層的 JPA 儲存庫 嚴格遵循 Repository Pattern，只接受和返回聚合根 */
@Component
@Primary
public class OrderRepositoryAdapter extends BaseRepositoryAdapter<Order, OrderId, JpaOrderEntity, String>
        implements OrderRepository {

    private final JpaOrderRepository jpaOrderRepository;
    private final OrderMapper mapper;

    public OrderRepositoryAdapter(JpaOrderRepository jpaOrderRepository, OrderMapper mapper) {
        super(jpaOrderRepository);
        this.jpaOrderRepository = jpaOrderRepository;
        this.mapper = mapper;
    }

    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        return jpaOrderRepository.findByCustomerId(customerId.toString()).stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByCustomerId(UUID customerId) {
        return findByCustomerId(CustomerId.of(customerId));
    }

    // BaseRepositoryAdapter required methods
    @Override
    protected JpaOrderEntity toJpaEntity(Order aggregateRoot) {
        return mapper.toJpaEntity(aggregateRoot);
    }

    @Override
    protected Order toDomainModel(JpaOrderEntity entity) {
        return mapper.toDomainModel(entity);
    }

    @Override
    protected String convertToJpaId(OrderId domainId) {
        return domainId.toString();
    }

    @Override
    protected OrderId extractId(Order aggregateRoot) {
        return aggregateRoot.getId();
    }
}
