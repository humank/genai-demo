package solid.humank.genaidemo.infrastructure.external;

import solid.humank.genaidemo.application.order.port.outgoing.OrderRepository;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 訂單倉儲適配器
 * 移動到 external 包以符合架構測試要求
 */
@Repository
public class OrderRepositoryAdapter implements OrderRepository {
    
    private final Map<UUID, Order> orderStore = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        orderStore.put(UUID.fromString(order.getId().toString()), order);
        return order;
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return Optional.ofNullable(orderStore.get(id));
    }

    @Override
    public List<Order> findAll() {
        return new ArrayList<>(orderStore.values());
    }

    @Override
    public List<Order> findByCustomerId(UUID customerId) {
        return orderStore.values().stream()
                .filter(order -> UUID.fromString(order.getCustomerId()).equals(customerId))
                .toList();
    }

    @Override
    public void delete(Order order) {
        orderStore.remove(UUID.fromString(order.getId().toString()));
    }
}