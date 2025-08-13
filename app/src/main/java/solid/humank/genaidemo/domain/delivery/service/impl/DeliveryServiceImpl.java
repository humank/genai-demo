package solid.humank.genaidemo.domain.delivery.service.impl;

import org.springframework.stereotype.Service;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.delivery.model.aggregate.Delivery;
import solid.humank.genaidemo.domain.delivery.model.valueobject.DeliveryId;
import solid.humank.genaidemo.domain.delivery.repository.DeliveryRepository;
import solid.humank.genaidemo.domain.delivery.service.DeliveryService;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 配送服務實現
 * 暫時禁用以解決啟動問題
 */
// @Service
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;

    public DeliveryServiceImpl(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    @Override
    public Delivery createDelivery(OrderId orderId, String shippingAddress) {
        Delivery delivery = new Delivery(orderId, shippingAddress);
        return deliveryRepository.save(delivery);
    }

    @Override
    public boolean arrangeDelivery(OrderId orderId) {
        Optional<Delivery> deliveryOpt = deliveryRepository.findByOrderId(orderId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }
        
        // 在實際實現中，這裡可能會有更複雜的邏輯來安排配送
        return true;
    }

    @Override
    public boolean allocateDeliveryResources(DeliveryId deliveryId, String deliveryPersonId, 
                                           String deliveryPersonName, String deliveryPersonContact, 
                                           LocalDateTime estimatedDeliveryTime) {
        Optional<Delivery> deliveryOpt = deliveryRepository.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            Delivery delivery = deliveryOpt.get();
            delivery.allocateResources(deliveryPersonId, deliveryPersonName, 
                                      deliveryPersonContact, estimatedDeliveryTime);
            deliveryRepository.save(delivery);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean updateDeliveryAddress(DeliveryId deliveryId, String newAddress) {
        Optional<Delivery> deliveryOpt = deliveryRepository.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            Delivery delivery = deliveryOpt.get();
            delivery.updateAddress(newAddress);
            deliveryRepository.save(delivery);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean markAsDelivered(DeliveryId deliveryId) {
        Optional<Delivery> deliveryOpt = deliveryRepository.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            Delivery delivery = deliveryOpt.get();
            delivery.markAsDelivered();
            deliveryRepository.save(delivery);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean markAsFailed(DeliveryId deliveryId, String reason) {
        Optional<Delivery> deliveryOpt = deliveryRepository.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            Delivery delivery = deliveryOpt.get();
            delivery.markAsFailed(reason);
            deliveryRepository.save(delivery);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean markAsRefused(DeliveryId deliveryId, String reason) {
        Optional<Delivery> deliveryOpt = deliveryRepository.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            Delivery delivery = deliveryOpt.get();
            delivery.markAsRefused(reason);
            deliveryRepository.save(delivery);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean markAsDelayed(DeliveryId deliveryId, String reason, LocalDateTime newEstimatedDeliveryTime) {
        Optional<Delivery> deliveryOpt = deliveryRepository.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            Delivery delivery = deliveryOpt.get();
            delivery.markAsDelayed(reason, newEstimatedDeliveryTime);
            deliveryRepository.save(delivery);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean cancelDelivery(DeliveryId deliveryId) {
        Optional<Delivery> deliveryOpt = deliveryRepository.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            Delivery delivery = deliveryOpt.get();
            delivery.cancel();
            deliveryRepository.save(delivery);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean scheduleRedelivery(DeliveryId deliveryId) {
        Optional<Delivery> deliveryOpt = deliveryRepository.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return false;
        }

        try {
            Delivery delivery = deliveryOpt.get();
            delivery.scheduleRedelivery();
            deliveryRepository.save(delivery);
            return true;
        } catch (Exception e) {
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
        Optional<Delivery> deliveryOpt = deliveryRepository.findById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return null;
        }

        Delivery delivery = deliveryOpt.get();
        String trackingNumber = delivery.getTrackingNumber();
        if (trackingNumber == null) {
            return null;
        }

        // 在實際實現中，這裡可能會根據不同的物流商生成不同的追蹤鏈接
        return "https://tracking.example.com/" + trackingNumber;
    }
}