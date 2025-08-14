package solid.humank.genaidemo.infrastructure.order.external;

import java.time.LocalDateTime;
import solid.humank.genaidemo.domain.common.valueobject.DeliveryOrder;
import solid.humank.genaidemo.domain.common.valueobject.DeliveryStatus;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;

/** 外部物流適配器 模擬與外部物流系統的交互 */
public class ExternalLogisticsAdapter {
    /**
     * 創建物流訂單
     *
     * @param orderId 訂單ID
     * @return 物流訂單
     */
    public DeliveryOrder createDeliveryOrder(OrderId orderId) {
        // 模擬與外部物流系統的交互
        // 實際應用中會調用外部物流系統的API

        // 生成物流訂單
        LocalDateTime estimatedDeliveryTime = LocalDateTime.now().plusDays(3);
        String trackingNumber = "TN-" + System.currentTimeMillis();

        return new DeliveryOrder(
                orderId, DeliveryStatus.PENDING, trackingNumber, estimatedDeliveryTime);
    }

    /**
     * 更新配送地址
     *
     * @param orderId 訂單ID
     * @param address 新地址
     */
    public void updateDeliveryAddress(OrderId orderId, String address) {
        // 模擬與外部物流系統的交互
        // 實際應用中會調用外部物流系統的API
    }

    /**
     * 取消物流訂單
     *
     * @param orderId 訂單ID
     */
    public void cancelDelivery(OrderId orderId) {
        // 模擬與外部物流系統的交互
        // 實際應用中會調用外部物流系統的API
    }

    /**
     * 標記物流訂單為失敗
     *
     * @param orderId 訂單ID
     * @param reason 失敗原因
     */
    public void failDelivery(OrderId orderId, String reason) {
        // 模擬與外部物流系統的交互
        // 實際應用中會調用外部物流系統的API
    }
}
