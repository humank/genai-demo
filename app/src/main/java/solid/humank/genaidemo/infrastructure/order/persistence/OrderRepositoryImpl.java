package solid.humank.genaidemo.infrastructure.order.persistence;

import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.domain.order.repository.OrderRepository;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 訂單儲存庫實現
 * 實作領域層定義的OrderRepository接口
 * 注意：這是一個簡化的內存實現，實際應用中會使用真實的資料庫
 */
@Repository
public class OrderRepositoryImpl implements OrderRepository {

    // 模擬數據存儲
    private final Map<OrderId, Order> orderStore = new HashMap<>();

    @Override
    public void save(Order order) {
        orderStore.put(order.getId(), order);
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return Optional.ofNullable(orderStore.get(orderId));
    }

    @Override
    public List<Order> findAll() {
        return orderStore.values().stream().collect(Collectors.toList());
    }

    @Override
    public void delete(Order order) {
        orderStore.remove(order.getId());
    }

    @Override
    public void delete(OrderId orderId) {
        orderStore.remove(orderId);
    }

    @Override
    public boolean exists(OrderId orderId) {
        return orderStore.containsKey(orderId);
    }
}