package solid.humank.genaidemo.infrastructure.delivery.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.delivery.model.aggregate.Delivery;
import solid.humank.genaidemo.domain.delivery.model.valueobject.DeliveryId;
import solid.humank.genaidemo.domain.delivery.model.valueobject.DeliveryStatus;
import solid.humank.genaidemo.domain.delivery.repository.DeliveryRepository;
import solid.humank.genaidemo.infrastructure.common.persistence.adapter.BaseRepositoryAdapter;
import solid.humank.genaidemo.infrastructure.delivery.persistence.entity.JpaDeliveryEntity;
import solid.humank.genaidemo.infrastructure.delivery.persistence.mapper.DeliveryMapper;
import solid.humank.genaidemo.infrastructure.delivery.persistence.repository.JpaDeliveryRepository;

/**
 * 配送儲存庫適配器
 * 實現領域儲存庫接口，遵循統一的 Repository Pattern
 */
@Component
@Transactional
public class DeliveryRepositoryAdapter
        extends BaseRepositoryAdapter<Delivery, DeliveryId, JpaDeliveryEntity, UUID>
        implements DeliveryRepository {

    private final DeliveryMapper mapper;

    public DeliveryRepositoryAdapter(JpaDeliveryRepository jpaRepository, DeliveryMapper mapper) {
        super(jpaRepository);
        this.mapper = mapper;
    }

    @Override
    protected JpaDeliveryEntity toJpaEntity(Delivery aggregateRoot) {
        return mapper.toJpaEntity(aggregateRoot);
    }

    @Override
    protected Delivery toDomainModel(JpaDeliveryEntity entity) {
        return mapper.toDomainModel(entity);
    }

    @Override
    protected UUID convertToJpaId(DeliveryId domainId) {
        return UUID.fromString(domainId.getValue());
    }

    @Override
    protected DeliveryId extractId(Delivery aggregateRoot) {
        return aggregateRoot.getId();
    }

    @Override
    public Optional<Delivery> findByUUID(UUID id) {
        return ((JpaDeliveryRepository) jpaRepository).findById(id)
                .map(mapper::toDomainModel);
    }

    @Override
    public Optional<Delivery> findByOrderId(OrderId orderId) {
        return ((JpaDeliveryRepository) jpaRepository).findByOrderId(orderId.getValue())
                .map(mapper::toDomainModel);
    }

    @Override
    public List<Delivery> findByStatus(DeliveryStatus status) {
        return ((JpaDeliveryRepository) jpaRepository).findByStatus(status.name())
                .stream()
                .map(mapper::toDomainModel)
                .toList();
    }

    @Override
    public List<Delivery> findFailedDeliveries() {
        return ((JpaDeliveryRepository) jpaRepository).findFailedDeliveries()
                .stream()
                .map(mapper::toDomainModel)
                .toList();
    }

    @Override
    public List<Delivery> findDeliveriesForRedelivery() {
        return ((JpaDeliveryRepository) jpaRepository).findDeliveriesForRedelivery()
                .stream()
                .map(mapper::toDomainModel)
                .toList();
    }

    @Override
    public List<Delivery> findByDeliveryPersonId(String deliveryPersonId) {
        return ((JpaDeliveryRepository) jpaRepository).findByDeliveryPersonName(deliveryPersonId)
                .stream()
                .map(mapper::toDomainModel)
                .toList();
    }
}