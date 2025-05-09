package solid.humank.genaidemo.application.order.port.outgoing;

import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.order.model.valueobject.OrderId;

import java.util.Optional;
import java.util.List;

/**
 * 訂單持久化端口 - 次要輸出端口
 * 定義系統與持久化層的交互方式
 */
public interface OrderPersistencePort {
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