package solid.humank.genaidemo.infrastructure.inventory.persistence.mapper;

import solid.humank.genaidemo.domain.inventory.model.aggregate.Inventory;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryId;
import solid.humank.genaidemo.domain.inventory.model.valueobject.InventoryStatus;
import solid.humank.genaidemo.domain.inventory.model.valueobject.ReservationId;
import solid.humank.genaidemo.infrastructure.inventory.persistence.entity.JpaInventoryEntity;
import solid.humank.genaidemo.infrastructure.inventory.persistence.entity.JpaReservationEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 庫存映射器
 * 負責在領域模型和JPA實體之間進行轉換
 */
public class InventoryMapper {

    /**
     * 將領域模型轉換為JPA實體
     */
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

    /**
     * 將JPA實體轉換為領域模型
     */
    public static Inventory toDomainModel(JpaInventoryEntity entity) {
        InventoryId inventoryId = InventoryId.fromUUID(entity.getId());
        Inventory inventory = new Inventory(
                inventoryId,
                entity.getProductId(),
                entity.getProductName(),
                entity.getTotalQuantity()
        );

        // 手動設置其他屬性
        // 注意：這裡假設Inventory類有一個包級別的構造函數或方法來設置這些屬性
        // 實際實現可能需要根據領域模型的設計進行調整
        setInventoryProperties(inventory, entity);

        return inventory;
    }

    /**
     * 設置庫存屬性
     * 注意：這是一個模擬方法，實際實現可能需要根據領域模型的設計進行調整
     */
    private static void setInventoryProperties(Inventory inventory, JpaInventoryEntity entity) {
        // 這裡假設Inventory類有一些包級別的方法來設置這些屬性
        // 實際實現可能需要根據領域模型的設計進行調整
        
        // 在實際應用中，可能需要通過反射或其他方式設置這些屬性
        // 或者在領域模型中提供適當的方法來重建聚合根
        
        // 這裡只是一個示例，實際實現可能會有所不同
    }

    /**
     * 將領域狀態映射到JPA狀態
     */
    private static JpaInventoryEntity.InventoryStatusEnum mapDomainStatusToJpa(InventoryStatus status) {
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

    /**
     * 將JPA狀態映射到領域狀態
     */
    private static InventoryStatus mapJpaStatusToDomain(JpaInventoryEntity.InventoryStatusEnum status) {
        switch (status) {
            case ACTIVE:
                return InventoryStatus.ACTIVE;
            case INACTIVE:
                return InventoryStatus.INACTIVE;
            case DISCONTINUED:
                return InventoryStatus.DISCONTINUED;
            default:
                throw new IllegalArgumentException("Unknown inventory status: " + status);
        }
    }
}