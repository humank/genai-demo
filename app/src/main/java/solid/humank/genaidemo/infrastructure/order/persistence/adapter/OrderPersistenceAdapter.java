package solid.humank.genaidemo.infrastructure.order.persistence.adapter;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import solid.humank.genaidemo.application.order.port.outgoing.OrderPersistencePort;
import solid.humank.genaidemo.domain.common.valueobject.CustomerId;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.order.repository.OrderRepository;

/** 訂單持久化適配器 實現應用層的 OrderPersistencePort 接口 使用領域層的 OrderRepository 進行實際的持久化操作 保持領域值對象的使用，維護架構的一致性 */
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
    public Optional<Order> findById(OrderId orderId) {
        return orderRepository.findById(orderId);
    }

    @Override
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Override
    public void delete(OrderId orderId) {
        orderRepository.deleteById(orderId);
    }

    @Override
    public void update(Order order) {
        orderRepository.save(order);
    }

    @Override
    public boolean exists(OrderId orderId) {
        return orderRepository.findById(orderId).isPresent();
    }

    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @Override
    public List<Order> findAll(int page, int size) {
        // 這裡需要實現分頁邏輯，暫時返回所有訂單的子集
        List<Order> allOrders = orderRepository.findAll();
        int start = page * size;
        int end = Math.min(start + size, allOrders.size());

        if (start >= allOrders.size()) {
            return List.of();
        }

        return allOrders.subList(start, end);
    }

    @Override
    public long count() {
        return orderRepository.findAll().size();
    }
}
