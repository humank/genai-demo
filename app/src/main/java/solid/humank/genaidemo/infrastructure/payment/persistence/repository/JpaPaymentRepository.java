package solid.humank.genaidemo.infrastructure.payment.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.infrastructure.payment.persistence.entity.JpaPaymentEntity;

import java.util.List;
import java.util.Optional;

/**
 * JPA 支付儲存庫
 * 用於與資料庫交互的 Spring Data JPA 儲存庫
 */
@Repository
public interface JpaPaymentRepository extends JpaRepository<JpaPaymentEntity, String> {
    
    /**
     * 根據訂單ID查詢支付
     * 
     * @param orderId 訂單ID
     * @return 支付列表
     */
    List<JpaPaymentEntity> findByOrderId(String orderId);
    
    /**
     * 根據支付ID查詢支付
     * 
     * @param id 支付ID
     * @return 支付
     */
    Optional<JpaPaymentEntity> findById(String id);
}