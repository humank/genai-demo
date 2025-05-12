package solid.humank.genaidemo.domain.order.repository;

import solid.humank.genaidemo.domain.common.repository.Repository;
import solid.humank.genaidemo.domain.common.valueobject.CustomerId;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 訂單儲存庫接口
 */
public interface OrderRepository extends Repository<Order, OrderId> {
    
    /**
     * 保存訂單
     * 
     * @param order 訂單
     * @return 保存後的訂單
     */
    @Override
    Order save(Order order);
    
    /**
     * 根據ID查詢訂單
     * 
     * @param id 訂單ID
     * @return 訂單
     */
    Optional<Order> findById(OrderId id);
    
    /**
     * 根據客戶ID查詢訂單列表
     * 
     * @param customerId 客戶ID
     * @return 訂單列表
     */
    List<Order> findByCustomerId(CustomerId customerId);
    
    /**
     * 根據客戶ID查詢訂單列表
     * 
     * @param customerId 客戶ID
     * @return 訂單列表
     */
    List<Order> findByCustomerId(UUID customerId);
}