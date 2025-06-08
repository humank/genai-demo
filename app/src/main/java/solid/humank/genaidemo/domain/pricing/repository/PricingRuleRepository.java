package solid.humank.genaidemo.domain.pricing.repository;

import solid.humank.genaidemo.domain.common.repository.Repository;
import solid.humank.genaidemo.domain.pricing.model.aggregate.PricingRule;
import solid.humank.genaidemo.domain.pricing.model.valueobject.PriceId;
import solid.humank.genaidemo.domain.pricing.model.valueobject.ProductCategory;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 定價規則倉儲接口
 * 負責PricingRule聚合的持久化操作
 */
public interface PricingRuleRepository extends Repository<PricingRule, PriceId> {
    @Override
    Optional<PricingRule> findById(PriceId priceId);
    
    /**
     * 根據產品ID查找定價規則
     */
    List<PricingRule> findByProductId(ProductId productId);
    
    /**
     * 根據促銷ID查找定價規則
     */
    List<PricingRule> findByPromotionId(PromotionId promotionId);
    
    /**
     * 查找所有活躍的定價規則
     */
    List<PricingRule> findActiveRules();
    
    /**
     * 查找指定時間有效的定價規則
     */
    List<PricingRule> findRulesValidAt(LocalDateTime dateTime);
    
    /**
     * 根據產品類別查找定價規則
     */
    List<PricingRule> findByProductCategory(ProductCategory category);
    
    @Override
    PricingRule save(PricingRule pricingRule);
    
    void delete(PriceId priceId);
}