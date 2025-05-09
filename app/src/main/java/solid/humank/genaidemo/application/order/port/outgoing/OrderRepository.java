package solid.humank.genaidemo.application.order.port.outgoing;

import solid.humank.genaidemo.domain.order.model.aggregate.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 訂單倉儲接口
 * 定義了訂單持久化的操作
 */
public interface OrderRepository {
    /**
     * 保存訂單
     */
    Order save(Order order);
    
    /**
     * 根據ID查詢訂單
     */
    Optional<Order> findById(UUID id);
    
    /**
     * 查詢所有訂單
     */
    List<Order> findAll();
    
    /**
     * 根據客戶ID查詢訂單
     */
    List<Order> findByCustomerId(UUID customerId);
    
    /**
     * 刪除訂單
     */
    void delete(Order order);
}