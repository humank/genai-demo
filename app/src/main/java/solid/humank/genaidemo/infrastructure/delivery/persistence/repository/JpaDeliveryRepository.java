package solid.humank.genaidemo.infrastructure.delivery.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import solid.humank.genaidemo.infrastructure.delivery.persistence.entity.JpaDeliveryEntity;

/**
 * 配送JPA儲存庫
 */
@Repository
public interface JpaDeliveryRepository extends JpaRepository<JpaDeliveryEntity, UUID> {

    /**
     * 根據訂單ID查詢配送
     */
    Optional<JpaDeliveryEntity> findByOrderId(String orderId);

    /**
     * 根據狀態查詢配送列表
     */
    List<JpaDeliveryEntity> findByStatus(String status);

    /**
     * 查詢配送失敗的配送列表
     */
    @Query("SELECT d FROM JpaDeliveryEntity d WHERE d.status = 'FAILED'")
    List<JpaDeliveryEntity> findFailedDeliveries();

    /**
     * 查詢需要重新配送的配送列表
     */
    @Query("SELECT d FROM JpaDeliveryEntity d WHERE d.status IN ('FAILED', 'REFUSED')")
    List<JpaDeliveryEntity> findDeliveriesForRedelivery();

    /**
     * 查詢指定配送人員的配送列表
     */
    List<JpaDeliveryEntity> findByDeliveryPersonName(@Param("deliveryPersonName") String deliveryPersonName);
}