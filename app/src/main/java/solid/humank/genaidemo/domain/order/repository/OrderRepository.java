package solid.humank.genaidemo.domain.order.repository;

import solid.humank.genaidemo.domain.common.repository.Repository;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;

import java.util.List;
import java.util.Optional;

/**
 * 訂單儲存庫接口
 * 定義在領域層，由基礎設施層實現
 */
public interface OrderRepository extends Repository<Order, OrderId> {
    /**
     * 保存訂單
     */
    void save(Order order);

    /**
     * 根據ID查詢訂單
     */
    Optional<Order> findById(OrderId orderId);

    /**
     * 查詢所有訂單
     */
    List<Order> findAll();

    /**
     * 刪除訂單
     */
    void delete(OrderId orderId);

    /**
     * 檢查訂單是否存在
     */
    boolean exists(OrderId orderId);
}