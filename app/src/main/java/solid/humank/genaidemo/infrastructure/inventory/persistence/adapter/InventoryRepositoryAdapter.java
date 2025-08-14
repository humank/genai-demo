package solid.humank.genaidemo.infrastructure.inventory.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import solid.humank.genaidemo.application.inventory.port.outgoing.InventoryPersistencePort;
import solid.humank.genaidemo.domain.inventory.model.aggregate.Inventory;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryId;
import solid.humank.genaidemo.domain.inventory.model.valueobject.ReservationId;
import solid.humank.genaidemo.domain.inventory.repository.InventoryRepository;
import solid.humank.genaidemo.infrastructure.inventory.persistence.entity.JpaInventoryEntity;
import solid.humank.genaidemo.infrastructure.inventory.persistence.mapper.InventoryMapper;
import solid.humank.genaidemo.infrastructure.inventory.persistence.repository.JpaInventoryRepository;

/** 庫存儲存庫適配器 實現領域儲存庫接口和應用層持久化端口 專門處理 Inventory 聚合根 */
@Component
public class InventoryRepositoryAdapter implements InventoryRepository, InventoryPersistencePort {

    private final JpaInventoryRepository jpaInventoryRepository;

    public InventoryRepositoryAdapter(JpaInventoryRepository jpaInventoryRepository) {
        this.jpaInventoryRepository = jpaInventoryRepository;
    }

    @Override
    public Inventory save(Inventory aggregateRoot) {
        JpaInventoryEntity entity = InventoryMapper.toJpaEntity(aggregateRoot);
        jpaInventoryRepository.save(entity);
        return aggregateRoot;
    }

    @Override
    public Optional<Inventory> findById(InventoryId inventoryId) {
        return jpaInventoryRepository
                .findById(inventoryId.getId())
                .map(InventoryMapper::toDomainModel);
    }

    @Override
    public Optional<Inventory> findByProductId(String productId) {
        return jpaInventoryRepository
                .findByProductId(productId)
                .map(InventoryMapper::toDomainModel);
    }

    @Override
    public List<Inventory> findAll() {
        return jpaInventoryRepository.findAll().stream()
                .map(InventoryMapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Inventory inventory) {
        jpaInventoryRepository.deleteById(inventory.getId().getId());
    }

    @Override
    public void deleteById(InventoryId inventoryId) {
        jpaInventoryRepository.deleteById(inventoryId.getId());
    }

    @Override
    public long count() {
        return jpaInventoryRepository.count();
    }

    @Override
    public boolean existsById(InventoryId id) {
        return jpaInventoryRepository.existsById(id.getId());
    }

    @Override
    public void delete(InventoryId inventoryId) {
        jpaInventoryRepository.deleteById(inventoryId.getId());
    }

    @Override
    public void update(Inventory aggregateRoot) {
        save(aggregateRoot);
    }

    @Override
    public boolean exists(InventoryId inventoryId) {
        return jpaInventoryRepository.existsById(inventoryId.getId());
    }

    @Override
    public Optional<Inventory> findByReservationId(ReservationId reservationId) {
        return jpaInventoryRepository
                .findByReservationId(reservationId.getId())
                .map(InventoryMapper::toDomainModel);
    }

    @Override
    public List<Inventory> findBelowThreshold() {
        return jpaInventoryRepository.findBelowThreshold().stream()
                .map(InventoryMapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<Inventory> findByOrderId(UUID orderId) {
        return jpaInventoryRepository.findByOrderId(orderId).stream()
                .map(InventoryMapper::toDomainModel)
                .collect(Collectors.toList());
    }
}
