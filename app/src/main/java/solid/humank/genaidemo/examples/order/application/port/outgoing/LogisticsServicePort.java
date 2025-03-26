package solid.humank.genaidemo.examples.order.application.port.outgoing;

import solid.humank.genaidemo.examples.order.OrderId;
import solid.humank.genaidemo.examples.order.acl.DeliveryOrder;
import solid.humank.genaidemo.examples.order.acl.DeliveryStatus;

/**
 * 物流服務端口 - 次要輸出端口
 * 定義系統與物流服務的交互方式
 */
public interface LogisticsServicePort {
    /**
     * 創建物流訂單
     * @param orderId 訂單ID
     * @return 物流訂單
     */
    DeliveryOrder createDeliveryOrder(OrderId orderId);

    /**
     * 取消物流訂單
     * @param orderId 訂單ID
     * @return 操作是否成功
     */
    boolean cancelDeliveryOrder(OrderId orderId);

    /**
     * 查詢物流狀態
     * @param orderId 訂單ID
     * @return 物流狀態
     */
    DeliveryStatus getDeliveryStatus(OrderId orderId);

    /**
     * 更新收貨地址
     * @param orderId 訂單ID
     * @param address 新地址
     * @return 更新是否成功
     */
    boolean updateDeliveryAddress(OrderId orderId, String address);

    /**
     * 確認收貨
     * @param orderId 訂單ID
     * @return 確認是否成功
     */
    boolean confirmDelivery(OrderId orderId);
}
