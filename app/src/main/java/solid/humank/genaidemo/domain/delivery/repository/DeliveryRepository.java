package solid.humank.genaidemo.domain.delivery.repository;

import solid.humank.genaidemo.domain.common.repository.Repository;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.delivery.model.aggregate.Delivery;
import solid.humank.genaidemo.domain.delivery.model.valueobject.DeliveryId;
import solid.humank.genaidemo.domain.delivery.model.valueobject.DeliveryStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 配送儲存庫接口
 */
public interface DeliveryRepository extends Repository<Delivery, DeliveryId> {
    
    /**
     * 保存配送
     * 
     * @param delivery 配送
     * @return 保存後的配送
     */
    @Override
    Delivery save(Delivery delivery);
    
    /**
     * 根據ID查詢配送
     * 
     * @param id 配送ID
     * @return 配送
     */
    Optional<Delivery> findById(DeliveryId id);
    
    /**
     * 根據UUID查詢配送（兼容舊代碼）
     * 
     * @param id UUID
     * @return 配送
     */
    Optional<Delivery> findByUUID(UUID id);
    
    /**
     * 根據訂單ID查詢配送
     * 
     * @param orderId 訂單ID
     * @return 配送
     */
    Optional<Delivery> findByOrderId(OrderId orderId);
    
    /**
     * 根據狀態查詢配送列表
     * 
     * @param status 配送狀態
     * @return 配送列表
     */
    List<Delivery> findByStatus(DeliveryStatus status);
    
    /**
     * 查詢配送失敗的配送列表
     * 
     * @return 配送列表
     */
    List<Delivery> findFailedDeliveries();
    
    /**
     * 查詢需要重新配送的配送列表
     * 
     * @return 配送列表
     */
    List<Delivery> findDeliveriesForRedelivery();
    
    /**
     * 查詢指定配送人員的配送列表
     * 
     * @param deliveryPersonId 配送人員ID
     * @return 配送列表
     */
    List<Delivery> findByDeliveryPersonId(String deliveryPersonId);
}