package solid.humank.genaidemo.infrastructure.order.persistence.adapter;

import org.springframework.stereotype.Component;
import solid.humank.genaidemo.application.order.port.outgoing.OrderPersistencePort;
import solid.humank.genaidemo.domain.common.valueobject.CustomerId;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.order.repository.OrderRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 訂單持久化適配器
 * 實現應用層的 OrderPersistencePort 接口
 * 使用領域層的 OrderRepository 進行實際的持久化操作
 */
@Component
public class OrderPersistenceAdapter implements OrderPersistencePort {

    private final OrderRepository orderRepository;

    public OrderPersistenceAdapter(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void save(Order order) {
        orderRepository.save(order);
    }

    @Override
    public Optional<Order> findById(UUID orderId) {
        return orderRepository.findById(OrderId.of(orderId));
    }

    @Override
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Override
    public void delete(UUID orderId) {
        orderRepository.deleteById(OrderId.of(orderId));
    }

    @Override
    public void update(Order order) {
        orderRepository.save(order);
    }

    @Override
    public boolean exists(UUID orderId) {
        return orderRepository.findById(OrderId.of(orderId)).isPresent();
    }

    @Override
    public List<Order> findByCustomerId(UUID customerId) {
        return orderRepository.findByCustomerId(CustomerId.of(customerId));
    }
}
