package solid.humank.genaidemo.infrastructure.persistence.adapter;

import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.delivery.model.aggregate.Delivery;
import solid.humank.genaidemo.domain.delivery.model.valueobject.DeliveryId;
import solid.humank.genaidemo.domain.delivery.model.valueobject.DeliveryStatus;
import solid.humank.genaidemo.domain.delivery.repository.DeliveryRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 配送儲存庫適配器
 * 用於適配新舊兩個配送儲存庫
 */
@Repository
public class DeliveryRepositoryAdapter implements DeliveryRepository {
    
    private final solid.humank.genaidemo.domain.workflow.repository.DeliveryRepository workflowDeliveryRepository;
    
    public DeliveryRepositoryAdapter(solid.humank.genaidemo.domain.workflow.repository.DeliveryRepository workflowDeliveryRepository) {
        this.workflowDeliveryRepository = workflowDeliveryRepository;
    }

    @Override
    public long count() {
        // 實現 Repository 接口中的 count 方法
        return workflowDeliveryRepository.count();
    }
    
    @Override
    public List<Delivery> findAll() {
        // 實現 Repository 接口中的 findAll 方法
        return workflowDeliveryRepository.findAll()
            .stream()
            .map(this::convertToNewDelivery)
            .collect(Collectors.toList());
    }
    
    @Override
    public void delete(Delivery delivery) {
        // 實現 Repository 接口中的 delete 方法
        workflowDeliveryRepository.delete(convertToOldDelivery(delivery));
    }
    
    @Override
    public void deleteById(DeliveryId id) {
        // 實現 Repository 接口中的 deleteById 方法
        workflowDeliveryRepository.deleteById(id.getId());
    }

    @Override
    public Delivery save(Delivery delivery) {
        // 將新的Delivery轉換為舊的Delivery
        solid.humank.genaidemo.domain.workflow.model.aggregate.Delivery oldDelivery = 
            convertToOldDelivery(delivery);
        
        // 保存舊的Delivery
        solid.humank.genaidemo.domain.workflow.model.aggregate.Delivery savedOldDelivery = 
            workflowDeliveryRepository.save(oldDelivery);
        
        // 將保存後的舊Delivery轉換回新的Delivery
        return convertToNewDelivery(savedOldDelivery);
    }

    @Override
    public Optional<Delivery> findById(DeliveryId id) {
        // 使用UUID查詢舊的Delivery
        return workflowDeliveryRepository.findById(id.getId())
            .map(this::convertToNewDelivery);
    }

    @Override
    public Optional<Delivery> findByUUID(UUID id) {
        // 直接使用UUID查詢舊的Delivery
        return workflowDeliveryRepository.findById(id)
            .map(this::convertToNewDelivery);
    }

    @Override
    public Optional<Delivery> findByOrderId(OrderId orderId) {
        // 使用OrderId查詢舊的Delivery
        return workflowDeliveryRepository.findByOrderId(orderId)
            .map(this::convertToNewDelivery);
    }

    @Override
    public List<Delivery> findByStatus(DeliveryStatus status) {
        // 將新的DeliveryStatus轉換為舊的DeliveryStatus
        solid.humank.genaidemo.domain.workflow.model.valueobject.DeliveryStatus oldStatus = 
            convertToOldDeliveryStatus(status);
        
        // 使用舊的DeliveryStatus查詢舊的Delivery列表
        return workflowDeliveryRepository.findByStatus(oldStatus)
            .stream()
            .map(this::convertToNewDelivery)
            .collect(Collectors.toList());
    }

    @Override
    public List<Delivery> findFailedDeliveries() {
        // 使用舊的儲存庫查詢失敗的配送
        return workflowDeliveryRepository.findFailedDeliveries()
            .stream()
            .map(this::convertToNewDelivery)
            .collect(Collectors.toList());
    }

    @Override
    public List<Delivery> findDeliveriesForRedelivery() {
        // 使用舊的儲存庫查詢需要重新配送的配送
        return workflowDeliveryRepository.findDeliveriesForRedelivery()
            .stream()
            .map(this::convertToNewDelivery)
            .collect(Collectors.toList());
    }

    @Override
    public List<Delivery> findByDeliveryPersonId(String deliveryPersonId) {
        // 使用舊的儲存庫查詢指定配送人員的配送
        return workflowDeliveryRepository.findByDeliveryPersonId(deliveryPersonId)
            .stream()
            .map(this::convertToNewDelivery)
            .collect(Collectors.toList());
    }

    /**
     * 將新的Delivery轉換為舊的Delivery
     */
    private solid.humank.genaidemo.domain.workflow.model.aggregate.Delivery convertToOldDelivery(Delivery newDelivery) {
        // 在實際實現中，這裡需要進行詳細的轉換
        // 這裡只是一個簡單的示例
        return new solid.humank.genaidemo.domain.workflow.model.aggregate.Delivery(
            newDelivery.getOrderId(),
            newDelivery.getShippingAddress()
        );
    }

    /**
     * 將舊的Delivery轉換為新的Delivery
     */
    private Delivery convertToNewDelivery(solid.humank.genaidemo.domain.workflow.model.aggregate.Delivery oldDelivery) {
        // 在實際實現中，這裡需要進行詳細的轉換
        // 這裡只是一個簡單的示例
        return new Delivery(
            DeliveryId.fromUUID(oldDelivery.getId()),
            oldDelivery.getOrderId(),
            oldDelivery.getShippingAddress()
        );
    }

    /**
     * 將新的DeliveryStatus轉換為舊的DeliveryStatus
     */
    private solid.humank.genaidemo.domain.workflow.model.valueobject.DeliveryStatus convertToOldDeliveryStatus(DeliveryStatus newStatus) {
        // 在實際實現中，這裡需要進行詳細的轉換
        // 這裡只是一個簡單的示例
        return switch (newStatus) {
            case PENDING_SHIPMENT -> solid.humank.genaidemo.domain.workflow.model.valueobject.DeliveryStatus.PENDING_SHIPMENT;
            case IN_TRANSIT -> solid.humank.genaidemo.domain.workflow.model.valueobject.DeliveryStatus.IN_TRANSIT;
            case DELIVERED -> solid.humank.genaidemo.domain.workflow.model.valueobject.DeliveryStatus.DELIVERED;
            case DELAYED -> solid.humank.genaidemo.domain.workflow.model.valueobject.DeliveryStatus.DELAYED;
            case DELIVERY_FAILED -> solid.humank.genaidemo.domain.workflow.model.valueobject.DeliveryStatus.DELIVERY_FAILED;
            case REFUSED -> solid.humank.genaidemo.domain.workflow.model.valueobject.DeliveryStatus.REFUSED;
            case CANCELLED -> 
                // 舊的DeliveryStatus沒有CANCELLED，這裡可能需要特殊處理
                solid.humank.genaidemo.domain.workflow.model.valueobject.DeliveryStatus.DELIVERY_FAILED;
        };
    }

    /**
     * 將舊的DeliveryStatus轉換為新的DeliveryStatus
     */
    private DeliveryStatus convertToNewDeliveryStatus(solid.humank.genaidemo.domain.workflow.model.valueobject.DeliveryStatus oldStatus) {
        // 在實際實現中，這裡需要進行詳細的轉換
        // 這裡只是一個簡單的示例
        return switch (oldStatus) {
            case PENDING_SHIPMENT -> DeliveryStatus.PENDING_SHIPMENT;
            case IN_TRANSIT -> DeliveryStatus.IN_TRANSIT;
            case DELIVERED -> DeliveryStatus.DELIVERED;
            case DELAYED -> DeliveryStatus.DELAYED;
            case DELIVERY_FAILED -> DeliveryStatus.DELIVERY_FAILED;
            case REFUSED -> DeliveryStatus.REFUSED;
            default -> throw new IllegalArgumentException("Unknown delivery status: " + oldStatus);
        };
    }
}