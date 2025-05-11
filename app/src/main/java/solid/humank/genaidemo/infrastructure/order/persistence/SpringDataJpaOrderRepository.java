package solid.humank.genaidemo.infrastructure.order.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderEntity;

import java.util.Optional;

/**
 * Spring Data JPA 訂單儲存庫接口
 * 繼承 Spring Data JPA 的 JpaRepository 接口
 */
@Repository
public interface SpringDataJpaOrderRepository extends JpaRepository<JpaOrderEntity, String> {
    
    /**
     * 根據客戶ID查找訂單
     */
    Optional<JpaOrderEntity> findByCustomerId(String customerId);
}