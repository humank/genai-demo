package solid.humank.genaidemo.infrastructure.inventory.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.infrastructure.inventory.persistence.entity.JpaInventoryEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 庫存JPA儲存庫
 */
@Repository
public interface JpaInventoryRepository extends JpaRepository<JpaInventoryEntity, UUID> {

    /**
     * 根據產品ID查詢庫存
     */
    Optional<JpaInventoryEntity> findByProductId(String productId);

    /**
     * 查詢低於閾值的庫存
     */
    @Query("SELECT i FROM JpaInventoryEntity i WHERE i.threshold > 0 AND i.availableQuantity < i.threshold")
    List<JpaInventoryEntity> findBelowThreshold();

    /**
     * 根據預留ID查詢庫存
     */
    @Query("SELECT i FROM JpaInventoryEntity i JOIN i.reservations r WHERE r.id = :reservationId")
    Optional<JpaInventoryEntity> findByReservationId(@Param("reservationId") UUID reservationId);

    /**
     * 根據訂單ID查詢相關的庫存
     */
    @Query("SELECT i FROM JpaInventoryEntity i JOIN i.reservations r WHERE r.orderId = :orderId")
    List<JpaInventoryEntity> findByOrderId(@Param("orderId") UUID orderId);
}