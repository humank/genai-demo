package solid.humank.genaidemo.infrastructure.inventory.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.infrastructure.inventory.persistence.entity.JpaInventoryEntity;

/** 庫存JPA儲存庫 */
@Repository
public interface JpaInventoryRepository extends JpaRepository<JpaInventoryEntity, UUID> {

    /** 根據產品ID查詢庫存 */
    Optional<JpaInventoryEntity> findByProductId(String productId);

    /** 查詢低於閾值的庫存 */
    @Query(
            "SELECT i FROM JpaInventoryEntity i WHERE i.threshold > 0 AND i.availableQuantity <"
                    + " i.threshold")
    List<JpaInventoryEntity> findBelowThreshold();

    /** 根據預留ID查詢庫存 */
    @Query("SELECT i FROM JpaInventoryEntity i JOIN i.reservations r WHERE r.id = :reservationId")
    Optional<JpaInventoryEntity> findByReservationId(@Param("reservationId") UUID reservationId);

    /** 根據訂單ID查詢相關的庫存 */
    @Query("SELECT i FROM JpaInventoryEntity i JOIN i.reservations r WHERE r.orderId = :orderId")
    List<JpaInventoryEntity> findByOrderId(@Param("orderId") UUID orderId);

    // ========== 統計查詢方法 ==========

    /** 根據狀態統計可用庫存總量 */
    @Query(
            "SELECT COALESCE(SUM(i.availableQuantity), 0) FROM JpaInventoryEntity i WHERE i.status"
                    + " = :status")
    Long sumAvailableQuantityByStatus(
            @Param("status")
                    solid.humank.genaidemo.infrastructure.inventory.persistence.entity
                                    .JpaInventoryEntity.InventoryStatusEnum
                            status);
}
