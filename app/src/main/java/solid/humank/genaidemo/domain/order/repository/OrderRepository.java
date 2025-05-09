package solid.humank.genaidemo.domain.order.repository;

import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.order.model.valueobject.OrderId;

import java.util.List;
import java.util.Optional;

/**
 * 訂單儲存庫接口
 * 定義訂單聚合根的持久化操作
 */
public interface OrderRepository {
    /**
     * 保存訂單
     */
    void save(Order order);

    /**
     * 根據ID查找訂單
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
     * 更新訂單
     */
    void update(Order order);

    /**
     * 檢查訂單是否存在
     */
    boolean exists(OrderId orderId);
}