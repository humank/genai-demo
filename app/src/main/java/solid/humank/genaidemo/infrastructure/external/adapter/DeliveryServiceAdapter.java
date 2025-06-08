package solid.humank.genaidemo.infrastructure.external.adapter;

import org.springframework.stereotype.Service;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.delivery.model.aggregate.Delivery;
import solid.humank.genaidemo.domain.delivery.model.valueobject.DeliveryId;
import solid.humank.genaidemo.domain.delivery.service.DeliveryService;
import solid.humank.genaidemo.infrastructure.persistence.adapter.DeliveryRepositoryAdapter;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 配送服務適配器
 * 用於適配新舊兩個配送服務
 */
@Service
public class DeliveryServiceAdapter implements DeliveryService {

    private final solid.humank.genaidemo.domain.workflow.service.DeliveryService workflowDeliveryService;
    private final DeliveryRepositoryAdapter deliveryRepositoryAdapter;

    public DeliveryServiceAdapter(solid.humank.genaidemo.domain.workflow.service.DeliveryService workflowDeliveryService, 
                                 DeliveryRepositoryAdapter deliveryRepositoryAdapter) {
        this.workflowDeliveryService = workflowDeliveryService;
        this.deliveryRepositoryAdapter = deliveryRepositoryAdapter;
    }

    @Override
    public Delivery createDelivery(OrderId orderId, String shippingAddress) {
        // 創建新的Delivery
        Delivery delivery = new Delivery(orderId, shippingAddress);
        // 保存到儲存庫
        return deliveryRepositoryAdapter.save(delivery);
    }

    @Override
    public boolean arrangeDelivery(OrderId orderId) {
        // 使用舊的服務安排配送
        return workflowDeliveryService.arrangeDelivery(orderId);
    }

    @Override
    public boolean allocateDeliveryResources(DeliveryId deliveryId, String deliveryPersonId, 
                                           String deliveryPersonName, String deliveryPersonContact, 
                                           LocalDateTime estimatedDeliveryTime) {
        // 獲取配送
        Optional<Delivery> deliveryOpt = deliveryRepositoryAdapter.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            // 分配資源
            Delivery delivery = deliveryOpt.get();
            delivery.allocateResources(deliveryPersonId, deliveryPersonName, 
                                      deliveryPersonContact, estimatedDeliveryTime);
            // 保存更新
            deliveryRepositoryAdapter.save(delivery);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean updateDeliveryAddress(DeliveryId deliveryId, String newAddress) {
        // 獲取配送
        Optional<Delivery> deliveryOpt = deliveryRepositoryAdapter.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            // 更新地址
            Delivery delivery = deliveryOpt.get();
            delivery.updateAddress(newAddress);
            // 保存更新
            deliveryRepositoryAdapter.save(delivery);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean markAsDelivered(DeliveryId deliveryId) {
        // 獲取配送
        Optional<Delivery> deliveryOpt = deliveryRepositoryAdapter.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            // 標記為已送達
            Delivery delivery = deliveryOpt.get();
            delivery.markAsDelivered();
            // 保存更新
            deliveryRepositoryAdapter.save(delivery);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean markAsFailed(DeliveryId deliveryId, String reason) {
        // 獲取配送
        Optional<Delivery> deliveryOpt = deliveryRepositoryAdapter.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            // 標記為失敗
            Delivery delivery = deliveryOpt.get();
            delivery.markAsFailed(reason);
            // 保存更新
            deliveryRepositoryAdapter.save(delivery);
            // 使用舊的服務記錄失敗
            workflowDeliveryService.recordDeliveryFailure(delivery.getOrderId(), reason);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean markAsRefused(DeliveryId deliveryId, String reason) {
        // 獲取配送
        Optional<Delivery> deliveryOpt = deliveryRepositoryAdapter.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            // 標記為拒收
            Delivery delivery = deliveryOpt.get();
            delivery.markAsRefused(reason);
            // 保存更新
            deliveryRepositoryAdapter.save(delivery);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean markAsDelayed(DeliveryId deliveryId, String reason, LocalDateTime newEstimatedDeliveryTime) {
        // 獲取配送
        Optional<Delivery> deliveryOpt = deliveryRepositoryAdapter.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            // 標記為延遲
            Delivery delivery = deliveryOpt.get();
            delivery.markAsDelayed(reason, newEstimatedDeliveryTime);
            // 保存更新
            deliveryRepositoryAdapter.save(delivery);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean cancelDelivery(DeliveryId deliveryId) {
        // 獲取配送
        Optional<Delivery> deliveryOpt = deliveryRepositoryAdapter.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            // 取消配送
            Delivery delivery = deliveryOpt.get();
            delivery.cancel();
            // 保存更新
            deliveryRepositoryAdapter.save(delivery);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean scheduleRedelivery(DeliveryId deliveryId) {
        // 獲取配送
        Optional<Delivery> deliveryOpt = deliveryRepositoryAdapter.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            // 安排重新配送
            Delivery delivery = deliveryOpt.get();
            delivery.scheduleRedelivery();
            // 保存更新
            deliveryRepositoryAdapter.save(delivery);
            // 使用舊的服務安排重新配送
            workflowDeliveryService.arrangeRedelivery(delivery.getOrderId());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<Delivery> getDelivery(DeliveryId deliveryId) {
        // 獲取配送
        return deliveryRepositoryAdapter.findById(deliveryId);
    }

    @Override
    public Optional<Delivery> getDeliveryByOrderId(OrderId orderId) {
        // 根據訂單ID獲取配送
        return deliveryRepositoryAdapter.findByOrderId(orderId);
    }

    @Override
    public String getDeliveryTrackingLink(DeliveryId deliveryId) {
        // 獲取配送
        Optional<Delivery> deliveryOpt = deliveryRepositoryAdapter.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return null;
        }

        // 獲取追蹤號
        Delivery delivery = deliveryOpt.get();
        String trackingNumber = delivery.getTrackingNumber();
        if (trackingNumber == null) {
            return null;
        }

        // 使用舊的服務獲取追蹤鏈接
        return workflowDeliveryService.getDeliveryTrackingLink(delivery.getOrderId());
    }
}