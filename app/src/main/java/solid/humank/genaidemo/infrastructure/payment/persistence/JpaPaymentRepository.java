package solid.humank.genaidemo.infrastructure.payment.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.infrastructure.payment.persistence.entity.JpaPaymentEntity;

import java.util.Optional;

/**
 * 支付 JPA Repository 接口
 * 繼承 Spring Data JPA 的 JpaRepository 接口
 */
@Repository
public interface JpaPaymentRepository extends JpaRepository<JpaPaymentEntity, String> {
    
    /**
     * 根據訂單ID查找支付
     */
    Optional<JpaPaymentEntity> findByOrderId(String orderId);
}