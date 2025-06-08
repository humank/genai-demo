package solid.humank.genaidemo.domain.pricing.repository;

import solid.humank.genaidemo.domain.common.repository.Repository;
import solid.humank.genaidemo.domain.pricing.model.aggregate.PricingRule;
import solid.humank.genaidemo.domain.pricing.model.valueobject.PriceId;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PricingRuleRepository extends Repository<PricingRule, PriceId> {
    @Override
    Optional<PricingRule> findById(PriceId priceId);
    List<PricingRule> findByProductId(ProductId productId);
    List<PricingRule> findByPromotionId(PromotionId promotionId);
    List<PricingRule> findActiveRules();
    List<PricingRule> findRulesValidAt(LocalDateTime dateTime);
    @Override
    PricingRule save(PricingRule pricingRule);
    void delete(PriceId priceId);
}