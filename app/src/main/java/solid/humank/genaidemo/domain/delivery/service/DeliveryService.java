package solid.humank.genaidemo.domain.delivery.service;

import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.delivery.model.aggregate.Delivery;
import solid.humank.genaidemo.domain.delivery.model.valueobject.DeliveryId;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 配送服務接口
 * 定義與配送相關的操作
 */
@DomainService
public interface DeliveryService {
    
    /**
     * 創建配送
     *
     * @param orderId 訂單ID
     * @param shippingAddress 配送地址
     * @return 創建的配送
     */
    Delivery createDelivery(OrderId orderId, String shippingAddress);
    
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
     * @param deliveryId 配送ID
     * @param deliveryPersonId 配送人員ID
     * @param deliveryPersonName 配送人員姓名
     * @param deliveryPersonContact 配送人員聯繫方式
     * @param estimatedDeliveryTime 預計送達時間
     * @return 是否分配成功
     */
    boolean allocateDeliveryResources(DeliveryId deliveryId, String deliveryPersonId, 
                                     String deliveryPersonName, String deliveryPersonContact,
                                     LocalDateTime estimatedDeliveryTime);
    
    /**
     * 更新配送地址
     *
     * @param deliveryId 配送ID
     * @param newAddress 新地址
     * @return 是否更新成功
     */
    boolean updateDeliveryAddress(DeliveryId deliveryId, String newAddress);
    
    /**
     * 標記配送為已送達
     *
     * @param deliveryId 配送ID
     * @return 是否標記成功
     */
    boolean markAsDelivered(DeliveryId deliveryId);
    
    /**
     * 標記配送為失敗
     *
     * @param deliveryId 配送ID
     * @param reason 失敗原因
     * @return 是否標記成功
     */
    boolean markAsFailed(DeliveryId deliveryId, String reason);
    
    /**
     * 標記配送為拒收
     *
     * @param deliveryId 配送ID
     * @param reason 拒收原因
     * @return 是否標記成功
     */
    boolean markAsRefused(DeliveryId deliveryId, String reason);
    
    /**
     * 標記配送為延遲
     *
     * @param deliveryId 配送ID
     * @param reason 延遲原因
     * @param newEstimatedDeliveryTime 新的預計送達時間
     * @return 是否標記成功
     */
    boolean markAsDelayed(DeliveryId deliveryId, String reason, LocalDateTime newEstimatedDeliveryTime);
    
    /**
     * 取消配送
     *
     * @param deliveryId 配送ID
     * @return 是否取消成功
     */
    boolean cancelDelivery(DeliveryId deliveryId);
    
    /**
     * 安排重新配送
     *
     * @param deliveryId 配送ID
     * @return 是否安排成功
     */
    boolean scheduleRedelivery(DeliveryId deliveryId);
    
    /**
     * 獲取配送
     *
     * @param deliveryId 配送ID
     * @return 配送
     */
    Optional<Delivery> getDelivery(DeliveryId deliveryId);
    
    /**
     * 根據訂單ID獲取配送
     *
     * @param orderId 訂單ID
     * @return 配送
     */
    Optional<Delivery> getDeliveryByOrderId(OrderId orderId);
    
    /**
     * 獲取配送追蹤鏈接
     *
     * @param deliveryId 配送ID
     * @return 配送追蹤鏈接
     */
    String getDeliveryTrackingLink(DeliveryId deliveryId);
}