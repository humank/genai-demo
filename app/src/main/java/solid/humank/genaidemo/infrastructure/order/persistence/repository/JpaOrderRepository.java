package solid.humank.genaidemo.infrastructure.order.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderEntity;

import java.util.List;
import java.util.Optional;

/**
 * JPA 訂單儲存庫
 * 用於與資料庫交互的 Spring Data JPA 儲存庫
 */
@Repository
public interface JpaOrderRepository extends JpaRepository<JpaOrderEntity, String> {
    
    /**
     * 根據客戶ID查詢訂單
     * 
     * @param customerId 客戶ID
     * @return 訂單列表
     */
    List<JpaOrderEntity> findByCustomerId(String customerId);
    
    /**
     * 根據訂單ID查詢訂單
     * 
     * @param id 訂單ID
     * @return 訂單
     */
    Optional<JpaOrderEntity> findById(String id);
}