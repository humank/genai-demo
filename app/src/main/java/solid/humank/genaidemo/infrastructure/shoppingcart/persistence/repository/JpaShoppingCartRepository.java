package solid.humank.genaidemo.infrastructure.shoppingcart.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.infrastructure.shoppingcart.persistence.entity.JpaShoppingCartEntity;

/** 購物車 JPA Repository */
@Repository
public interface JpaShoppingCartRepository extends JpaRepository<JpaShoppingCartEntity, UUID> {

    /** 根據客戶ID查找購物車 */
    Optional<JpaShoppingCartEntity> findByCustomerId(String customerId);

    /** 根據客戶ID和狀態查找購物車 */
    Optional<JpaShoppingCartEntity> findByCustomerIdAndStatus(
            String customerId, JpaShoppingCartEntity.CartStatusEnum status);

    /** 查找活躍的購物車 */
    @Query("SELECT c FROM JpaShoppingCartEntity c WHERE c.status = 'ACTIVE'")
    java.util.List<JpaShoppingCartEntity> findActiveCarts();

    /** 查找廢棄的購物車 */
    @Query(
            "SELECT c FROM JpaShoppingCartEntity c WHERE c.status = 'ABANDONED' AND c.updatedAt <"
                    + " :cutoffTime")
    java.util.List<JpaShoppingCartEntity> findAbandonedCartsBefore(
            @Param("cutoffTime") java.time.LocalDateTime cutoffTime);
}
