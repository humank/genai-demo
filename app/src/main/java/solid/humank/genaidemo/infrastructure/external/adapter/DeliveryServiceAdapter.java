package solid.humank.genaidemo.infrastructure.external.adapter;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.delivery.model.aggregate.Delivery;
import solid.humank.genaidemo.domain.delivery.model.valueobject.DeliveryId;
import solid.humank.genaidemo.domain.delivery.repository.DeliveryRepository;
import solid.humank.genaidemo.domain.delivery.service.DeliveryService;

/** 配送服務適配器 用於適配新舊兩個配送服務 暫時禁用以解決啟動問題 */
// @Service
public class DeliveryServiceAdapter implements DeliveryService {    private static final Logger logger = LoggerFactory.getLogger(DeliveryServiceAdapter.class);

    private final solid.humank.genaidemo.domain.delivery.service.DeliveryService deliveryService;
    private final DeliveryRepository deliveryRepository;

    public DeliveryServiceAdapter(
            solid.humank.genaidemo.domain.delivery.service.DeliveryService deliveryService,
            DeliveryRepository deliveryRepository) {
        this.deliveryService = deliveryService;
        this.deliveryRepository = deliveryRepository;
    }

    @Override
    public Delivery createDelivery(OrderId orderId, String shippingAddress) {
        // 創建新的Delivery
        Delivery delivery = new Delivery(orderId, shippingAddress);
        // 保存到儲存庫
        return deliveryRepository.save(delivery);
    }

    @Override
    public boolean arrangeDelivery(OrderId orderId) {
        // 使用舊的服務安排配送
        return deliveryService.arrangeDelivery(orderId);
    }

    @Override
    public boolean allocateDeliveryResources(
            DeliveryId deliveryId,
            String deliveryPersonId,
            String deliveryPersonName,
            String deliveryPersonContact,
            LocalDateTime estimatedDeliveryTime) {
        // 獲取配送
        Optional<Delivery> deliveryOpt = deliveryRepository.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            // 分配資源
            Delivery delivery = deliveryOpt.get();
            delivery.allocateResources(
                    deliveryPersonId,
                    deliveryPersonName,
                    deliveryPersonContact,
                    estimatedDeliveryTime);
            // 保存更新
            deliveryRepository.save(delivery);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean updateDeliveryAddress(DeliveryId deliveryId, String newAddress) {
        // 獲取配送
        Optional<Delivery> deliveryOpt = deliveryRepository.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            // 更新地址
            Delivery delivery = deliveryOpt.get();
            delivery.updateAddress(newAddress);
            // 保存更新
            deliveryRepository.save(delivery);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean markAsDelivered(DeliveryId deliveryId) {
        // 獲取配送
        Optional<Delivery> deliveryOpt = deliveryRepository.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            // 標記為已送達
            Delivery delivery = deliveryOpt.get();
            delivery.markAsDelivered();
            // 保存更新
            deliveryRepository.save(delivery);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean markAsFailed(DeliveryId deliveryId, String reason) {
        // 獲取配送
        Optional<Delivery> deliveryOpt = deliveryRepository.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            // 標記為失敗
            Delivery delivery = deliveryOpt.get();
            delivery.markAsFailed(reason);
            // 保存更新
            deliveryRepository.save(delivery);
            // 記錄失敗（簡化實作）
            logger.info("記錄配送失敗: {}, 原因: {}", delivery.getOrderId(), reason);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean markAsRefused(DeliveryId deliveryId, String reason) {
        // 獲取配送
        Optional<Delivery> deliveryOpt = deliveryRepository.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            // 標記為拒收
            Delivery delivery = deliveryOpt.get();
            delivery.markAsRefused(reason);
            // 保存更新
            deliveryRepository.save(delivery);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean markAsDelayed(
            DeliveryId deliveryId, String reason, LocalDateTime newEstimatedDeliveryTime) {
        // 獲取配送
        Optional<Delivery> deliveryOpt = deliveryRepository.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            // 標記為延遲
            Delivery delivery = deliveryOpt.get();
            delivery.markAsDelayed(reason, newEstimatedDeliveryTime);
            // 保存更新
            deliveryRepository.save(delivery);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean cancelDelivery(DeliveryId deliveryId) {
        // 獲取配送
        Optional<Delivery> deliveryOpt = deliveryRepository.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            // 取消配送
            Delivery delivery = deliveryOpt.get();
            delivery.cancel();
            // 保存更新
            deliveryRepository.save(delivery);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean scheduleRedelivery(DeliveryId deliveryId) {
        // 獲取配送
        Optional<Delivery> deliveryOpt = deliveryRepository.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            // 安排重新配送
            Delivery delivery = deliveryOpt.get();
            delivery.scheduleRedelivery();
            // 保存更新
            deliveryRepository.save(delivery);
            // 安排重新配送（簡化實作）
            logger.info("安排重新配送: {}", delivery.getOrderId());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<Delivery> getDelivery(DeliveryId deliveryId) {
        // 獲取配送
        return deliveryRepository.findById(deliveryId);
    }

    @Override
    public Optional<Delivery> getDeliveryByOrderId(OrderId orderId) {
        // 根據訂單ID獲取配送
        return deliveryRepository.findByOrderId(orderId);
    }

    @Override
    public String getDeliveryTrackingLink(DeliveryId deliveryId) {
        // 獲取配送
        Optional<Delivery> deliveryOpt = deliveryRepository.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return null;
        }

        // 獲取追蹤號
        Delivery delivery = deliveryOpt.get();
        String trackingNumber = delivery.getTrackingNumber();
        if (trackingNumber == null) {
            return null;
        }

        // 生成追蹤鏈接（簡化實作）
        return "https://tracking.example.com/" + trackingNumber;
    }
}
