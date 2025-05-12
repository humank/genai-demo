package solid.humank.genaidemo.infrastructure.order.persistence.adapter;

import org.springframework.stereotype.Component;
import solid.humank.genaidemo.domain.common.valueobject.CustomerId;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.order.repository.OrderRepository;
import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderEntity;
import solid.humank.genaidemo.infrastructure.order.persistence.mapper.OrderMapper;
import solid.humank.genaidemo.infrastructure.order.persistence.repository.JpaOrderRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 訂單儲存庫適配器
 * 實現領域儲存庫接口，並使用基礎設施層的 JPA 儲存庫
 */
@Component
public class OrderRepositoryAdapter implements OrderRepository {

    private final JpaOrderRepository jpaOrderRepository;

    public OrderRepositoryAdapter(JpaOrderRepository jpaOrderRepository) {
        this.jpaOrderRepository = jpaOrderRepository;
    }

    @Override
    public Order save(Order order) {
        // 將領域模型轉換為持久化模型
        JpaOrderEntity jpaEntity = OrderMapper.toJpaEntity(order);
        
        // 保存到資料庫
        JpaOrderEntity savedEntity = jpaOrderRepository.save(jpaEntity);
        
        // 將保存後的持久化模型轉換回領域模型
        return OrderMapper.toDomainEntity(savedEntity);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaOrderRepository.findById(id.toString())
                .map(OrderMapper::toDomainEntity);
    }

    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        return jpaOrderRepository.findByCustomerId(customerId.toString())
                .stream()
                .map(OrderMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByCustomerId(UUID customerId) {
        return findByCustomerId(CustomerId.fromUUID(customerId));
    }

    @Override
    public List<Order> findAll() {
        return jpaOrderRepository.findAll()
                .stream()
                .map(OrderMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Order order) {
        jpaOrderRepository.deleteById(order.getId().toString());
    }

    @Override
    public void deleteById(OrderId id) {
        jpaOrderRepository.deleteById(id.toString());
    }

    @Override
    public long count() {
        return jpaOrderRepository.count();
    }
}