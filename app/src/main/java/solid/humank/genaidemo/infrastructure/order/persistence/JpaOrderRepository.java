package solid.humank.genaidemo.infrastructure.order.persistence;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.application.order.port.outgoing.OrderPersistencePort;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderEntity;
import solid.humank.genaidemo.infrastructure.order.persistence.mapper.OrderMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA 訂單儲存庫實現
 * 實作 OrderPersistencePort 接口
 * 使用 Spring Data JPA 進行數據庫操作
 */
@Repository
@Primary
public class JpaOrderRepository implements OrderPersistencePort {

    private final SpringDataJpaOrderRepository springDataJpaOrderRepository;

    public JpaOrderRepository(SpringDataJpaOrderRepository springDataJpaOrderRepository) {
        this.springDataJpaOrderRepository = springDataJpaOrderRepository;
    }

    @Override
    public void save(Order order) {
        JpaOrderEntity jpaEntity = OrderMapper.toJpaEntity(order);
        springDataJpaOrderRepository.save(jpaEntity);
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return springDataJpaOrderRepository.findById(orderId.toString())
                .map(OrderMapper::toDomainEntity);
    }

    @Override
    public List<Order> findAll() {
        return springDataJpaOrderRepository.findAll().stream()
                .map(OrderMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(OrderId orderId) {
        springDataJpaOrderRepository.deleteById(orderId.toString());
    }

    @Override
    public void update(Order order) {
        // 在 JPA 中，save 方法會根據 ID 是否存在來決定是插入還是更新
        save(order);
    }

    @Override
    public boolean exists(OrderId orderId) {
        return springDataJpaOrderRepository.existsById(orderId.toString());
    }
}