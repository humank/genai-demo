package solid.humank.genaidemo.infrastructure.order.persistence;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.order.repository.OrderRepository;

import java.util.List;
import java.util.Optional;

/**
 * 訂單儲存庫適配器
 * 實現領域層的 OrderRepository 接口
 * 使用 JpaOrderRepository 進行實際的持久化操作
 */
@Component
@Primary
public class OrderRepositoryAdapter implements OrderRepository {

    private final JpaOrderRepository jpaOrderRepository;

    public OrderRepositoryAdapter(JpaOrderRepository jpaOrderRepository) {
        this.jpaOrderRepository = jpaOrderRepository;
    }

    @Override
    public void save(Order order) {
        jpaOrderRepository.save(order);
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return jpaOrderRepository.findById(orderId);
    }

    @Override
    public List<Order> findAll() {
        return jpaOrderRepository.findAll();
    }

    @Override
    public void delete(Order order) {
        jpaOrderRepository.delete(order.getId());
    }

    @Override
    public void delete(OrderId orderId) {
        jpaOrderRepository.delete(orderId);
    }

    @Override
    public boolean exists(OrderId orderId) {
        return jpaOrderRepository.exists(orderId);
    }
}