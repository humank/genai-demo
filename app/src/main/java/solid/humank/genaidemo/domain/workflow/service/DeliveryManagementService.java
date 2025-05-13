package solid.humank.genaidemo.domain.workflow.service;

import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationChannel;
import solid.humank.genaidemo.domain.notification.service.NotificationService;
import solid.humank.genaidemo.domain.workflow.model.aggregate.Delivery;
import solid.humank.genaidemo.domain.workflow.model.valueobject.DeliveryStatus;
import solid.humank.genaidemo.domain.workflow.repository.DeliveryRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 配送管理服務
 * 負責處理配送的創建、狀態轉換和完成
 */
public class DeliveryManagementService implements DeliveryService {
    private final DeliveryRepository deliveryRepository;
    private final NotificationService notificationService;

    public DeliveryManagementService(DeliveryRepository deliveryRepository, NotificationService notificationService) {
        this.deliveryRepository = deliveryRepository;
        this.notificationService = notificationService;
    }

    /**
     * 創建配送
     *
     * @param orderId 訂單ID
     * @param shippingAddress 配送地址
     * @return 配送ID
     */
    public UUID createDelivery(OrderId orderId, String shippingAddress) {
        Delivery delivery = new Delivery(orderId, shippingAddress);
        deliveryRepository.save(delivery);
        return delivery.getId();
    }

    @Override
    public boolean arrangeDelivery(OrderId orderId) {
        // 獲取訂單的配送地址
        String shippingAddress = getShippingAddress(orderId);
        
        // 創建配送
        UUID deliveryId = createDelivery(orderId, shippingAddress);
        
        // 設置預計配送時間
        LocalDateTime estimatedDeliveryTime = LocalDateTime.now().plusDays(3);
        updateEstimatedDeliveryTime(deliveryId, estimatedDeliveryTime);
        
        // 發送配送安排通知
        sendDeliveryArrangementNotification(orderId, estimatedDeliveryTime);
        
        return true;
    }

    @Override
    public boolean allocateDeliveryResources(OrderId orderId) {
        Optional<Delivery> optionalDelivery = deliveryRepository.findByOrderId(orderId);
        if (optionalDelivery.isEmpty()) {
            return false;
        }
        
        Delivery delivery = optionalDelivery.get();
        
        // 分配配送人員
        delivery.assignDeliveryPerson("DP001", "配送員A", "0912-345-678");
        
        // 設置追蹤號碼
        delivery.setTrackingNumber("TRK" + System.currentTimeMillis());
        
        deliveryRepository.save(delivery);
        
        // 發送配送狀態更新通知
        sendDeliveryStatusUpdateNotification(orderId, DeliveryStatus.IN_TRANSIT, delivery.getEstimatedDeliveryTime());
        
        return true;
    }

    @Override
    public boolean executeDelivery(OrderId orderId) {
        Optional<Delivery> optionalDelivery = deliveryRepository.findByOrderId(orderId);
        if (optionalDelivery.isEmpty()) {
            return false;
        }
        
        // 在實際應用中，這裡會有更多的邏輯
        // 例如更新配送狀態、記錄配送進度等
        
        return true;
    }

    @Override
    public boolean updateDeliveryAddress(OrderId orderId, String newAddress) {
        Optional<Delivery> optionalDelivery = deliveryRepository.findByOrderId(orderId);
        if (optionalDelivery.isEmpty()) {
            return false;
        }
        
        Delivery delivery = optionalDelivery.get();
        
        try {
            delivery.updateShippingAddress(newAddress);
            deliveryRepository.save(delivery);
            
            // 發送地址更新成功通知
            sendAddressUpdateSuccessNotification(orderId, newAddress);
            
            return true;
        } catch (IllegalStateException e) {
            // 處理更新地址失敗
            return false;
        }
    }

    @Override
    public String getDeliveryStatus(OrderId orderId) {
        Optional<Delivery> optionalDelivery = deliveryRepository.findByOrderId(orderId);
        return optionalDelivery.map(delivery -> delivery.getStatus().name()).orElse(null);
    }

    @Override
    public LocalDateTime getEstimatedDeliveryTime(OrderId orderId) {
        Optional<Delivery> optionalDelivery = deliveryRepository.findByOrderId(orderId);
        return optionalDelivery.map(Delivery::getEstimatedDeliveryTime).orElse(null);
    }

    @Override
    public String getDeliveryTrackingLink(OrderId orderId) {
        Optional<Delivery> optionalDelivery = deliveryRepository.findByOrderId(orderId);
        if (optionalDelivery.isEmpty()) {
            return null;
        }
        
        Delivery delivery = optionalDelivery.get();
        String trackingNumber = delivery.getTrackingNumber();
        
        if (trackingNumber == null) {
            return null;
        }
        
        return "https://example.com/tracking?number=" + trackingNumber;
    }

    @Override
    public boolean recordDeliveryFailure(OrderId orderId, String reason) {
        Optional<Delivery> optionalDelivery = deliveryRepository.findByOrderId(orderId);
        if (optionalDelivery.isEmpty()) {
            return false;
        }
        
        Delivery delivery = optionalDelivery.get();
        
        try {
            delivery.markAsDeliveryFailed(reason);
            deliveryRepository.save(delivery);
            
            // 發送配送失敗通知
            sendDeliveryFailureNotification(orderId, reason);
            
            return true;
        } catch (IllegalStateException e) {
            // 處理標記失敗
            return false;
        }
    }

    @Override
    public boolean arrangeRedelivery(OrderId orderId) {
        Optional<Delivery> optionalDelivery = deliveryRepository.findByOrderId(orderId);
        if (optionalDelivery.isEmpty()) {
            return false;
        }
        
        Delivery delivery = optionalDelivery.get();
        
        try {
            delivery.scheduleRedelivery();
            deliveryRepository.save(delivery);
            
            // 發送重新配送通知
            sendRedeliveryNotification(orderId);
            
            return true;
        } catch (IllegalStateException e) {
            // 處理安排重新配送失敗
            return false;
        }
    }

    /**
     * 標記配送為已送達
     *
     * @param orderId 訂單ID
     * @return 是否標記成功
     */
    public boolean markAsDelivered(OrderId orderId) {
        Optional<Delivery> optionalDelivery = deliveryRepository.findByOrderId(orderId);
        if (optionalDelivery.isEmpty()) {
            return false;
        }
        
        Delivery delivery = optionalDelivery.get();
        
        try {
            delivery.markAsDelivered();
            deliveryRepository.save(delivery);
            
            // 發送配送完成通知
            sendDeliveryCompletionNotification(orderId);
            
            return true;
        } catch (IllegalStateException e) {
            // 處理標記失敗
            return false;
        }
    }

    /**
     * 標記配送為延遲
     *
     * @param orderId 訂單ID
     * @param reason 延遲原因
     * @param newEstimatedDeliveryTime 新的預計配送時間
     * @return 是否標記成功
     */
    public boolean markAsDelayed(OrderId orderId, String reason, LocalDateTime newEstimatedDeliveryTime) {
        Optional<Delivery> optionalDelivery = deliveryRepository.findByOrderId(orderId);
        if (optionalDelivery.isEmpty()) {
            return false;
        }
        
        Delivery delivery = optionalDelivery.get();
        
        try {
            delivery.markAsDelayed(reason, newEstimatedDeliveryTime);
            deliveryRepository.save(delivery);
            
            // 發送配送延遲通知
            sendDeliveryDelayNotification(orderId, reason, newEstimatedDeliveryTime);
            
            return true;
        } catch (IllegalStateException e) {
            // 處理標記失敗
            return false;
        }
    }

    /**
     * 標記配送為拒收
     *
     * @param orderId 訂單ID
     * @param reason 拒收原因
     * @return 是否標記成功
     */
    public boolean markAsRefused(OrderId orderId, String reason) {
        Optional<Delivery> optionalDelivery = deliveryRepository.findByOrderId(orderId);
        if (optionalDelivery.isEmpty()) {
            return false;
        }
        
        Delivery delivery = optionalDelivery.get();
        
        try {
            delivery.markAsRefused(reason);
            deliveryRepository.save(delivery);
            
            // 發送拒收通知
            sendRefusalNotification(orderId, reason);
            
            return true;
        } catch (IllegalStateException e) {
            // 處理標記失敗
            return false;
        }
    }

    /**
     * 更新預計配送時間
     *
     * @param deliveryId 配送ID
     * @param estimatedDeliveryTime 預計配送時間
     * @return 是否更新成功
     */
    public boolean updateEstimatedDeliveryTime(UUID deliveryId, LocalDateTime estimatedDeliveryTime) {
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
    
    private String getCustomerId(OrderId orderId) {
        // 在實際應用中，這裡會查詢訂單獲取客戶ID
        return "customer-123";
    }
    
    private List<NotificationChannel> getCustomerPreferredChannels(String customerId) {
        // 在實際應用中，這裡會查詢客戶偏好
        return List.of(NotificationChannel.EMAIL, NotificationChannel.SMS);
    }
    
    private void sendDeliveryArrangementNotification(OrderId orderId, LocalDateTime estimatedDeliveryTime) {
        // 在實際應用中，這裡會發送配送安排通知
    }
    
    private void sendDeliveryStatusUpdateNotification(OrderId orderId, DeliveryStatus status, LocalDateTime estimatedDeliveryTime) {
        // 在實際應用中，這裡會發送配送狀態更新通知
        String customerId = getCustomerId(orderId);
        List<NotificationChannel> channels = getCustomerPreferredChannels(customerId);
        String trackingLink = getDeliveryTrackingLink(orderId);
        
        notificationService.sendDeliveryStatusUpdateNotification(
                customerId,
                orderId,
                status.name(),
                estimatedDeliveryTime,
                trackingLink,
                channels
        );
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
    
    private void sendDeliveryDelayNotification(OrderId orderId, String reason, LocalDateTime newEstimatedDeliveryTime) {
        // 在實際應用中，這裡會發送配送延遲通知
    }
    
    private void sendRefusalNotification(OrderId orderId, String reason) {
        // 在實際應用中，這裡會發送拒收通知
    }
}