package solid.humank.genaidemo.application.order.port.outgoing;

import java.util.List;
import java.util.Optional;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/** 訂單持久化端口 定義訂單持久化的操作接口 使用領域值對象保持領域模型的完整性 */
public interface OrderPersistencePort {

    /** 保存訂單 */
    void save(Order order);

    /** 根據ID查找訂單 */
    Optional<Order> findById(OrderId orderId);

    /** 查找所有訂單 */
    List<Order> findAll();

    /** 刪除訂單 */
    void delete(OrderId orderId);

    /** 更新訂單 */
    void update(Order order);

    /** 檢查訂單是否存在 */
    boolean exists(OrderId orderId);

    /** 根據客戶ID查找訂單 */
    List<Order> findByCustomerId(CustomerId customerId);

    /** 分頁查詢訂單 */
    List<Order> findAll(int page, int size);

    /** 計算訂單總數 */
    long count();
}
