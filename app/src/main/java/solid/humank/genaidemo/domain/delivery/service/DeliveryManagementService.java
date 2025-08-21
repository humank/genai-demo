package solid.humank.genaidemo.domain.delivery.service;

import java.time.LocalDateTime;
import java.util.Optional;

import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.delivery.model.aggregate.Delivery;
import solid.humank.genaidemo.domain.delivery.model.valueobject.DeliveryId;
import solid.humank.genaidemo.domain.delivery.repository.DeliveryRepository;
import solid.humank.genaidemo.domain.notification.service.NotificationService;

/** 配送管理服務 - 實現 DeliveryService 介面，負責處理配送的創建、狀態轉換和完成 */
@DomainService(name = "DeliveryManagementService", description = "配送管理服務，負責處理配送的創建、狀態轉換和完成", boundedContext = "Delivery")
public class DeliveryManagementService implements DeliveryService {
    private final DeliveryRepository deliveryRepository;
    private final NotificationService notificationService;

    public DeliveryManagementService(
            DeliveryRepository deliveryRepository, NotificationService notificationService) {
        this.deliveryRepository = deliveryRepository;
        this.notificationService = notificationService;
    }

    @Override
    public Delivery createDelivery(OrderId orderId, String shippingAddress) {
        Delivery delivery = new Delivery(orderId, shippingAddress);
        return deliveryRepository.save(delivery);
    }

    @Override
    public boolean arrangeDelivery(OrderId orderId) {
        // 創建配送
        Delivery delivery = createDelivery(orderId, getShippingAddress(orderId));

        // 設置預計配送時間
        LocalDateTime estimatedDeliveryTime = LocalDateTime.now().plusDays(3);
        updateEstimatedDeliveryTime(delivery.getId(), estimatedDeliveryTime);

        // 發送配送安排通知
        sendDeliveryArrangementNotification(orderId, estimatedDeliveryTime);

        return true;
    }

    @Override
    public boolean allocateDeliveryResources(
            DeliveryId deliveryId,
            String deliveryPersonId,
            String deliveryPersonName,
            String deliveryPersonContact,
            LocalDateTime estimatedDeliveryTime) {
        Optional<Delivery> optionalDelivery = deliveryRepository.findById(deliveryId);
        if (optionalDelivery.isEmpty()) {
            return false;
        }

        Delivery delivery = optionalDelivery.get();

        // 分配配送資源
        delivery.allocateResources(
                deliveryPersonId, deliveryPersonName, deliveryPersonContact, estimatedDeliveryTime);

        // 設置追蹤號碼
        delivery.setTrackingNumber("TRK" + System.currentTimeMillis());

        deliveryRepository.save(delivery);

        return true;
    }

    @Override
    public boolean updateDeliveryAddress(DeliveryId deliveryId, String newAddress) {
        Optional<Delivery> optionalDelivery = deliveryRepository.findById(deliveryId);
        if (optionalDelivery.isEmpty()) {
            return false;
        }

        Delivery delivery = optionalDelivery.get();

        try {
            delivery.updateAddress(newAddress);
            deliveryRepository.save(delivery);

            // 發送地址更新成功通知
            sendAddressUpdateSuccessNotification(delivery.getOrderId(), newAddress);

            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    public boolean markAsDelivered(DeliveryId deliveryId) {
        Optional<Delivery> optionalDelivery = deliveryRepository.findById(deliveryId);
        if (optionalDelivery.isEmpty()) {
            return false;
        }

        Delivery delivery = optionalDelivery.get();

        try {
            delivery.markAsDelivered();
            deliveryRepository.save(delivery);

            // 發送配送完成通知
            sendDeliveryCompletionNotification(delivery.getOrderId());

            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    public boolean markAsFailed(DeliveryId deliveryId, String reason) {
        Optional<Delivery> optionalDelivery = deliveryRepository.findById(deliveryId);
        if (optionalDelivery.isEmpty()) {
            return false;
        }

        Delivery delivery = optionalDelivery.get();

        try {
            delivery.markAsFailed(reason);
            deliveryRepository.save(delivery);

            // 發送配送失敗通知
            sendDeliveryFailureNotification(delivery.getOrderId(), reason);

            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    public boolean markAsRefused(DeliveryId deliveryId, String reason) {
        Optional<Delivery> optionalDelivery = deliveryRepository.findById(deliveryId);
        if (optionalDelivery.isEmpty()) {
            return false;
        }

        Delivery delivery = optionalDelivery.get();

        try {
            delivery.markAsRefused(reason);
            deliveryRepository.save(delivery);

            // 發送拒收通知
            sendRefusalNotification(delivery.getOrderId(), reason);

            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    public boolean markAsDelayed(
            DeliveryId deliveryId, String reason, LocalDateTime newEstimatedDeliveryTime) {
        Optional<Delivery> optionalDelivery = deliveryRepository.findById(deliveryId);
        if (optionalDelivery.isEmpty()) {
            return false;
        }

        Delivery delivery = optionalDelivery.get();

        try {
            delivery.markAsDelayed(reason, newEstimatedDeliveryTime);
            deliveryRepository.save(delivery);

            // 發送配送延遲通知
            sendDeliveryDelayNotification(delivery.getOrderId(), reason, newEstimatedDeliveryTime);

            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    public boolean cancelDelivery(DeliveryId deliveryId) {
        Optional<Delivery> optionalDelivery = deliveryRepository.findById(deliveryId);
        if (optionalDelivery.isEmpty()) {
            return false;
        }

        Delivery delivery = optionalDelivery.get();

        try {
            delivery.cancel();
            deliveryRepository.save(delivery);

            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    public boolean scheduleRedelivery(DeliveryId deliveryId) {
        Optional<Delivery> optionalDelivery = deliveryRepository.findById(deliveryId);
        if (optionalDelivery.isEmpty()) {
            return false;
        }

        Delivery delivery = optionalDelivery.get();

        try {
            delivery.rearrange();
            deliveryRepository.save(delivery);

            // 發送重新配送通知
            sendRedeliveryNotification(delivery.getOrderId());

            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    public Optional<Delivery> getDelivery(DeliveryId deliveryId) {
        return deliveryRepository.findById(deliveryId);
    }

    @Override
    public Optional<Delivery> getDeliveryByOrderId(OrderId orderId) {
        return deliveryRepository.findByOrderId(orderId);
    }

    @Override
    public String getDeliveryTrackingLink(DeliveryId deliveryId) {
        return "https://tracking.example.com/" + deliveryId.getId();
    }

    /**
     * 更新預計配送時間
     *
     * @param deliveryId            配送ID
     * @param estimatedDeliveryTime 預計配送時間
     * @return 是否更新成功
     */
    public boolean updateEstimatedDeliveryTime(
            DeliveryId deliveryId, LocalDateTime estimatedDeliveryTime) {
        Optional<Delivery> optionalDelivery = deliveryRepository.findById(deliveryId);
        if (optionalDelivery.isEmpty()) {
            return false;
        }

        Delivery delivery = optionalDelivery.get();
        delivery.setEstimatedDeliveryTime(estimatedDeliveryTime);
        deliveryRepository.save(delivery);

        return true;
    }

    // 以下是輔助方法，在實際應用中需要實現

    private String getShippingAddress(OrderId orderId) {
        // 在實際應用中，這裡會查詢訂單獲取配送地址
        return "台北市信義區";
    }

    private void sendDeliveryArrangementNotification(
            OrderId orderId, LocalDateTime estimatedDeliveryTime) {
        // 在實際應用中，這裡會發送配送安排通知
    }

    private void sendAddressUpdateSuccessNotification(OrderId orderId, String newAddress) {
        // 在實際應用中，這裡會發送地址更新成功通知
    }

    private void sendDeliveryFailureNotification(OrderId orderId, String reason) {
        // 在實際應用中，這裡會發送配送失敗通知
    }

    private void sendRedeliveryNotification(OrderId orderId) {
        // 在實際應用中，這裡會發送重新配送通知
    }

    private void sendDeliveryCompletionNotification(OrderId orderId) {
        // 在實際應用中，這裡會發送配送完成通知
    }

    private void sendDeliveryDelayNotification(
            OrderId orderId, String reason, LocalDateTime newEstimatedDeliveryTime) {
        // 在實際應用中，這裡會發送配送延遲通知
    }

    private void sendRefusalNotification(OrderId orderId, String reason) {
        // 在實際應用中，這裡會發送拒收通知
    }
}