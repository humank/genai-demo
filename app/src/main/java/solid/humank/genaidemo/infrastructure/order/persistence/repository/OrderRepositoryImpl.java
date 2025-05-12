package solid.humank.genaidemo.infrastructure.order.persistence.repository;

import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.domain.common.valueobject.CustomerId;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.order.repository.OrderRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 訂單儲存庫實現
 * 使用內存存儲，僅用於測試
 */
@Repository
public class OrderRepositoryImpl implements OrderRepository {
    
    // 使用內存存儲，僅用於測試
    private final Map<String, Order> orderMap = new HashMap<>();
    
    @Override
    public Order save(Order order) {
        orderMap.put(order.getId().toString(), order);
        return order;
    }
    
    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(orderMap.get(id.toString()));
    }
    
    @Override
    public List<Order> findAll() {
        return new ArrayList<>(orderMap.values());
    }
    
    @Override
    public void delete(Order order) {
        orderMap.remove(order.getId().toString());
    }
    
    @Override
    public void deleteById(OrderId id) {
        orderMap.remove(id.toString());
    }
    
    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        return orderMap.values().stream()
                .filter(order -> order.getCustomerId().equals(customerId))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Order> findByCustomerId(UUID customerId) {
        return findByCustomerId(CustomerId.of(customerId));
    }
    
    @Override
    public long count() {
        return orderMap.size();
    }
}