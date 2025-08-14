package solid.humank.genaidemo.infrastructure.inventory.persistence.mapper;

import java.util.Map;
import java.util.UUID;
import solid.humank.genaidemo.domain.inventory.model.aggregate.Inventory;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryId;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryStatus;
import solid.humank.genaidemo.domain.inventory.model.valueobject.ReservationId;
import solid.humank.genaidemo.infrastructure.inventory.persistence.entity.JpaInventoryEntity;
import solid.humank.genaidemo.infrastructure.inventory.persistence.entity.JpaReservationEntity;

/** 庫存映射器 負責在領域模型和JPA實體之間進行轉換 */
public class InventoryMapper {

    /** 將領域模型轉換為JPA實體 */
    public static JpaInventoryEntity toJpaEntity(Inventory inventory) {
        JpaInventoryEntity entity = new JpaInventoryEntity();
        entity.setId(inventory.getId().getId());
        entity.setProductId(inventory.getProductId());
        entity.setProductName(inventory.getProductName());
        entity.setTotalQuantity(inventory.getTotalQuantity());
        entity.setAvailableQuantity(inventory.getAvailableQuantity());
        entity.setReservedQuantity(inventory.getReservedQuantity());
        entity.setThreshold(inventory.getThreshold());
        entity.setStatus(mapDomainStatusToJpa(inventory.getStatus()));
        entity.setCreatedAt(inventory.getCreatedAt());
        entity.setUpdatedAt(inventory.getUpdatedAt());

        // 轉換預留
        Map<ReservationId, Integer> domainReservations = inventory.getReservations();
        for (Map.Entry<ReservationId, Integer> entry : domainReservations.entrySet()) {
            JpaReservationEntity reservationEntity = new JpaReservationEntity();
            reservationEntity.setId(entry.getKey().getId());
            reservationEntity.setQuantity(entry.getValue());
            reservationEntity.setStatus(JpaReservationEntity.ReservationStatusEnum.ACTIVE);
            reservationEntity.setOrderId(UUID.randomUUID()); // 這裡需要從領域模型中獲取訂單ID
            entity.addReservation(reservationEntity);
        }

        return entity;
    }

    /** 將JPA實體轉換為領域模型 */
    public static Inventory toDomainModel(JpaInventoryEntity entity) {
        InventoryId inventoryId = InventoryId.fromUUID(entity.getId());

        // 創建基本的庫存聚合根
        Inventory inventory =
                new Inventory(
                        inventoryId,
                        entity.getProductId(),
                        entity.getProductName(),
                        entity.getTotalQuantity());

        // 設置閾值
        inventory.setThreshold(entity.getThreshold());

        // 如果需要處理預留，可以在這裡添加邏輯
        // 但由於領域模型的封裝性，我們可能需要通過領域服務來重建完整的聚合根狀態

        return inventory;
    }

    /** 將領域狀態映射到JPA狀態 */
    private static JpaInventoryEntity.InventoryStatusEnum mapDomainStatusToJpa(
            InventoryStatus status) {
        switch (status) {
            case ACTIVE:
                return JpaInventoryEntity.InventoryStatusEnum.ACTIVE;
            case INACTIVE:
                return JpaInventoryEntity.InventoryStatusEnum.INACTIVE;
            case DISCONTINUED:
                return JpaInventoryEntity.InventoryStatusEnum.DISCONTINUED;
            default:
                throw new IllegalArgumentException("Unknown inventory status: " + status);
        }
    }
}
