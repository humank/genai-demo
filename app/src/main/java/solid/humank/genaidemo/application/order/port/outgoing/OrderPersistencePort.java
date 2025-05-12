package solid.humank.genaidemo.application.order.port.outgoing;

import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.common.valueobject.CustomerId;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 訂單持久化端口
 * 定義訂單持久化的操作接口
 */
public interface OrderPersistencePort {
    
    /**
     * 保存訂單
     */
    void save(Order order);
    
    /**
     * 根據ID查找訂單
     */
    Optional<Order> findById(UUID orderId);
    
    /**
     * 查找所有訂單
     */
    List<Order> findAll();
    
    /**
     * 刪除訂單
     */
    void delete(UUID orderId);
    
    /**
     * 更新訂單
     */
    void update(Order order);
    
    /**
     * 檢查訂單是否存在
     */
    boolean exists(UUID orderId);
    
    /**
     * 根據客戶ID查找訂單
     */
    List<Order> findByCustomerId(UUID customerId);
    
    /**
     * 根據客戶ID查找訂單 (使用CustomerId值對象)
     */
    default List<Order> findByCustomerId(CustomerId customerId) {
        return findByCustomerId(customerId.getId());
    }
}