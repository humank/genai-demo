package solid.humank.genaidemo.infrastructure.product.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 產品 JPA Repository
 */
@Repository
public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, String> {
    
    /**
     * 根據狀態查找產品
     */
    List<ProductJpaEntity> findByStatus(String status);
    
    /**
     * 根據狀態分頁查找產品
     */
    Page<ProductJpaEntity> findByStatus(String status, Pageable pageable);
    
    /**
     * 根據產品名稱查找產品
     */
    List<ProductJpaEntity> findByNameContainingIgnoreCaseAndStatus(String name, String status);
    
    /**
     * 根據類別查找產品
     */
    List<ProductJpaEntity> findByCategoryAndStatus(String category, String status);
    
    /**
     * 根據產品ID和狀態查找產品
     */
    Optional<ProductJpaEntity> findByProductIdAndStatus(String productId, String status);
    
    /**
     * 統計活躍產品數量
     */
    @Query("SELECT COUNT(p) FROM ProductJpaEntity p WHERE p.status = :status")
    long countByStatus(@Param("status") String status);
    
    /**
     * 檢查產品是否存在且為活躍狀態
     */
    boolean existsByProductIdAndStatus(String productId, String status);
}