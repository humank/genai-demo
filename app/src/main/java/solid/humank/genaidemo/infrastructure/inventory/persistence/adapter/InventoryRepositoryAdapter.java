package solid.humank.genaidemo.infrastructure.inventory.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.inventory.model.aggregate.Inventory;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryId;
import solid.humank.genaidemo.domain.inventory.model.valueobject.ReservationId;
import solid.humank.genaidemo.domain.inventory.repository.InventoryRepository;
import solid.humank.genaidemo.infrastructure.common.persistence.adapter.BaseRepositoryAdapter;
import solid.humank.genaidemo.infrastructure.inventory.persistence.entity.JpaInventoryEntity;
import solid.humank.genaidemo.infrastructure.inventory.persistence.mapper.InventoryMapper;
import solid.humank.genaidemo.infrastructure.inventory.persistence.repository.JpaInventoryRepository;

/** 庫存儲存庫適配器 實現領域儲存庫接口 專門處理 Inventory 聚合根 */
@Component
public class InventoryRepositoryAdapter extends BaseRepositoryAdapter<Inventory, InventoryId, JpaInventoryEntity, UUID>
        implements InventoryRepository {

    private final JpaInventoryRepository jpaInventoryRepository;
    private final InventoryMapper mapper;

    public InventoryRepositoryAdapter(JpaInventoryRepository jpaInventoryRepository, InventoryMapper mapper) {
        super(jpaInventoryRepository);
        this.jpaInventoryRepository = jpaInventoryRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Inventory> findByProductId(String productId) {
        return jpaInventoryRepository
                .findByProductId(productId)
                .map(mapper::toDomainModel);
    }

    @Override
    public void delete(InventoryId inventoryId) {
        jpaInventoryRepository.deleteById(inventoryId.getId());
    }

    public void update(Inventory aggregateRoot) {
        save(aggregateRoot);
    }

    public boolean exists(InventoryId inventoryId) {
        return jpaInventoryRepository.existsById(inventoryId.getId());
    }

    @Override
    public Optional<Inventory> findByReservationId(ReservationId reservationId) {
        return jpaInventoryRepository
                .findByReservationId(reservationId.getId())
                .map(mapper::toDomainModel);
    }

    @Override
    public List<Inventory> findBelowThreshold() {
        return jpaInventoryRepository.findBelowThreshold().stream()
                .map(mapper::toDomainModel)
                .toList();
    }

    @Override
    public List<Inventory> findByOrderId(UUID orderId) {
        return jpaInventoryRepository.findByOrderId(orderId).stream()
                .map(mapper::toDomainModel)
                .toList();
    }

    // BaseRepositoryAdapter required methods
    @Override
    protected JpaInventoryEntity toJpaEntity(Inventory aggregateRoot) {
        return mapper.toJpaEntity(aggregateRoot);
    }

    @Override
    protected Inventory toDomainModel(JpaInventoryEntity entity) {
        return mapper.toDomainModel(entity);
    }

    @Override
    protected UUID convertToJpaId(InventoryId domainId) {
        return domainId.getId();
    }

    @Override
    protected InventoryId extractId(Inventory aggregateRoot) {
        return aggregateRoot.getId();
    }
}
