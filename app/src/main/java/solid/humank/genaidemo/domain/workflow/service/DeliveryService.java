package solid.humank.genaidemo.domain.workflow.service;

import solid.humank.genaidemo.domain.common.valueobject.OrderId;

import java.time.LocalDateTime;

/**
 * 配送服務接口
 * 定義與配送相關的操作
 */
public interface DeliveryService {
    
    /**
     * 安排配送
     *
     * @param orderId 訂單ID
     * @return 是否安排成功
     */
    boolean arrangeDelivery(OrderId orderId);
    
    /**
     * 分配配送資源
     *
     * @param orderId 訂單ID
     * @return 是否分配成功
     */
    boolean allocateDeliveryResources(OrderId orderId);
    
    /**
     * 執行配送
     *
     * @param orderId 訂單ID
     * @return 是否執行成功
     */
    boolean executeDelivery(OrderId orderId);
    
    /**
     * 更新配送地址
     *
     * @param orderId 訂單ID
     * @param newAddress 新地址
     * @return 是否更新成功
     */
    boolean updateDeliveryAddress(OrderId orderId, String newAddress);
    
    /**
     * 獲取配送狀態
     *
     * @param orderId 訂單ID
     * @return 配送狀態
     */
    String getDeliveryStatus(OrderId orderId);
    
    /**
     * 獲取預計配送時間
     *
     * @param orderId 訂單ID
     * @return 預計配送時間
     */
    LocalDateTime getEstimatedDeliveryTime(OrderId orderId);
    
    /**
     * 獲取配送追蹤鏈接
     *
     * @param orderId 訂單ID
     * @return 配送追蹤鏈接
     */
    String getDeliveryTrackingLink(OrderId orderId);
    
    /**
     * 記錄配送失敗
     *
     * @param orderId 訂單ID
     * @param reason 失敗原因
     * @return 是否記錄成功
     */
    boolean recordDeliveryFailure(OrderId orderId, String reason);
    
    /**
     * 安排重新配送
     *
     * @param orderId 訂單ID
     * @return 是否安排成功
     */
    boolean arrangeRedelivery(OrderId orderId);
}