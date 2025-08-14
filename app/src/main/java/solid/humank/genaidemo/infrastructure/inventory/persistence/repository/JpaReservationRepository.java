package solid.humank.genaidemo.infrastructure.inventory.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.infrastructure.inventory.persistence.entity.JpaReservationEntity;

/** 庫存預留JPA儲存庫 */
@Repository
public interface JpaReservationRepository extends JpaRepository<JpaReservationEntity, UUID> {

    /** 根據庫存ID查詢預留 */
    List<JpaReservationEntity> findByInventoryId(UUID inventoryId);

    /** 根據訂單ID查詢預留 */
    List<JpaReservationEntity> findByOrderId(UUID orderId);

    /** 查詢已過期的預留 */
    @Query("SELECT r FROM JpaReservationEntity r WHERE r.status = 'ACTIVE' AND r.expiresAt < :now")
    List<JpaReservationEntity> findExpiredReservations(@Param("now") LocalDateTime now);

    /** 根據庫存ID和狀態查詢預留 */
    List<JpaReservationEntity> findByInventoryIdAndStatus(
            UUID inventoryId, JpaReservationEntity.ReservationStatusEnum status);
}
