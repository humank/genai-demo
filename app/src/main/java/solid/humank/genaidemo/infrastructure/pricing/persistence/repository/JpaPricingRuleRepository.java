package solid.humank.genaidemo.infrastructure.pricing.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import solid.humank.genaidemo.infrastructure.pricing.persistence.entity.JpaPricingRuleEntity;

/**
 * 定價規則JPA儲存庫
 */
@Repository
public interface JpaPricingRuleRepository extends JpaRepository<JpaPricingRuleEntity, String> {

    /**
     * 根據產品ID查詢定價規則
     */
    List<JpaPricingRuleEntity> findByProductId(String productId);

    /**
     * 根據促銷ID查詢定價規則
     */
    List<JpaPricingRuleEntity> findByPromotionId(String promotionId);

    /**
     * 查詢活躍的定價規則
     */
    List<JpaPricingRuleEntity> findByIsActiveTrue();

    /**
     * 查詢在指定時間有效的定價規則
     */
    @Query("SELECT p FROM JpaPricingRuleEntity p WHERE p.isActive = true AND p.validFrom <= :dateTime AND p.validTo >= :dateTime")
    List<JpaPricingRuleEntity> findRulesValidAt(@Param("dateTime") LocalDateTime dateTime);

    /**
     * 根據產品類別查詢定價規則
     */
    List<JpaPricingRuleEntity> findByProductCategory(String productCategory);
}