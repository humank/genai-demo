package solid.humank.genaidemo.infrastructure.inventory.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.application.inventory.port.outgoing.InventoryPersistencePort;
import solid.humank.genaidemo.domain.inventory.model.aggregate.Inventory;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryId;
import solid.humank.genaidemo.domain.inventory.model.valueobject.ReservationId;
import solid.humank.genaidemo.domain.inventory.repository.InventoryRepository;

/**
 * 庫存持久化適配器 實現應用層的 InventoryPersistencePort 接口 使用領域層的 InventoryRepository
 * 進行實際的持久化操作
 */
@Component
public class InventoryPersistenceAdapter implements InventoryPersistencePort {

    private final InventoryRepository inventoryRepository;

    public InventoryPersistenceAdapter(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public Inventory save(Inventory aggregateRoot) {
        return inventoryRepository.save(aggregateRoot);
    }

    @Override
    public Optional<Inventory> findById(InventoryId inventoryId) {
        return inventoryRepository.findById(inventoryId);
    }

    @Override
    public Optional<Inventory> findByProductId(String productId) {
        return inventoryRepository.findByProductId(productId);
    }

    @Override
    public List<Inventory> findAll() {
        return inventoryRepository.findAll();
    }

    @Override
    public void delete(InventoryId inventoryId) {
        inventoryRepository.delete(inventoryId);
    }

    @Override
    public void update(Inventory aggregateRoot) {
        inventoryRepository.save(aggregateRoot);
    }

    @Override
    public boolean exists(InventoryId inventoryId) {
        return inventoryRepository.findById(inventoryId).isPresent();
    }

    @Override
    public Optional<Inventory> findByReservationId(ReservationId reservationId) {
        return inventoryRepository.findByReservationId(reservationId);
    }

    @Override
    public List<Inventory> findBelowThreshold() {
        return inventoryRepository.findBelowThreshold();
    }

    @Override
    public List<Inventory> findByOrderId(UUID orderId) {
        return inventoryRepository.findByOrderId(orderId);
    }
}