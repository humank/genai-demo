package solid.humank.genaidemo.application.order.port.outgoing;

import solid.humank.genaidemo.domain.common.valueobject.DeliveryOrder;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;

/**
 * 物流服務端口
 * 應用層定義的物流服務接口，由基礎設施層實現
 */
public interface LogisticsServicePort {
    /**
     * 創建物流訂單
     * 
     * @param orderId 訂單ID
     * @return 物流訂單
     */
    DeliveryOrder createDeliveryOrder(OrderId orderId);
    
    /**
     * 更新配送地址
     * 
     * @param orderId 訂單ID
     * @param address 新地址
     */
    void updateDeliveryAddress(OrderId orderId, String address);
}