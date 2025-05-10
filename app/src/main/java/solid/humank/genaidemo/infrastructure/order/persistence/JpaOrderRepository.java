package solid.humank.genaidemo.infrastructure.order.persistence;

import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.application.order.port.outgoing.OrderPersistencePort;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 訂單持久化適配器
 * 實作 OrderPersistencePort 接口
 * 注意：這是一個簡化的內存實現，實際應用中會使用真實的資料庫
 */
@Repository
public class JpaOrderRepository implements OrderPersistencePort {

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
    public void delete(OrderId orderId) {
        orderStore.remove(orderId);
    }

    @Override
    public void update(Order order) {
        // 在記憶體實現中，更新和保存是一樣的
        save(order);
    }

    @Override
    public boolean exists(OrderId orderId) {
        return orderStore.containsKey(orderId);
    }
}