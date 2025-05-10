package solid.humank.genaidemo.infrastructure.order.persistence;

import org.springframework.stereotype.Component;
import solid.humank.genaidemo.application.order.port.outgoing.OrderPersistencePort;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.repository.OrderRepository;

import java.util.List;
import java.util.Optional;

/**
 * 訂單持久化適配器
 * 實現應用層的 OrderPersistencePort 接口
 * 橋接領域層的 OrderRepository 和應用層的 OrderPersistencePort
 */
@Component
public class OrderRepositoryAdapter implements OrderPersistencePort {

    private final OrderRepository orderRepository;

    public OrderRepositoryAdapter(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void save(Order order) {
        orderRepository.save(order);
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return orderRepository.findById(orderId);
    }

    @Override
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Override
    public void delete(OrderId orderId) {
        orderRepository.delete(orderId);
    }

    @Override
    public void update(Order order) {
        orderRepository.save(order);
    }

    @Override
    public boolean exists(OrderId orderId) {
        return orderRepository.exists(orderId);
    }
}