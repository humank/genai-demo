package solid.humank.genaidemo.infrastructure.persistence.adapter;

import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.workflow.model.aggregate.Delivery;
import solid.humank.genaidemo.domain.workflow.model.valueobject.DeliveryStatus;
import solid.humank.genaidemo.domain.workflow.repository.DeliveryRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Workflow 配送儲存庫適配器 - 內存實現
 */
@Repository
public class WorkflowDeliveryRepositoryAdapter implements DeliveryRepository {
    
    private final Map<UUID, Delivery> deliveries = new ConcurrentHashMap<>();
    
    @Override
    public Delivery save(Delivery delivery) {
        deliveries.put(delivery.getId(), delivery);
        return delivery;
    }
    
    @Override
    public Optional<Delivery> findById(UUID id) {
        return Optional.ofNullable(deliveries.get(id));
    }
    
    @Override
    public Optional<Delivery> findByOrderId(OrderId orderId) {
        return deliveries.values().stream()
            .filter(delivery -> delivery.getOrderId().equals(orderId))
            .findFirst();
    }
    
    @Override
    public List<Delivery> findByStatus(DeliveryStatus status) {
        return deliveries.values().stream()
            .filter(delivery -> delivery.getStatus().equals(status))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Delivery> findFailedDeliveries() {
        return findByStatus(DeliveryStatus.DELIVERY_FAILED);
    }
    
    @Override
    public List<Delivery> findDeliveriesForRedelivery() {
        return deliveries.values().stream()
            .filter(delivery -> delivery.getStatus() == DeliveryStatus.DELIVERY_FAILED || 
                               delivery.getStatus() == DeliveryStatus.DELAYED)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Delivery> findByDeliveryPersonId(String deliveryPersonId) {
        return deliveries.values().stream()
            .filter(delivery -> deliveryPersonId.equals(delivery.getDeliveryPersonId()))
            .collect(Collectors.toList());
    }
    
    @Override
    public long count() {
        return deliveries.size();
    }
    
    @Override
    public boolean existsById(UUID id) {
        return deliveries.containsKey(id);
    }
    
    @Override
    public List<Delivery> findAll() {
        return new ArrayList<>(deliveries.values());
    }
    
    @Override
    public void delete(Delivery delivery) {
        deliveries.remove(delivery.getId());
    }
    
    @Override
    public void deleteById(UUID id) {
        deliveries.remove(id);
    }
}
