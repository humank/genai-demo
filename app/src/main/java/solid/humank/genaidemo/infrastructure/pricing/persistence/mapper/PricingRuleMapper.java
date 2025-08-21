package solid.humank.genaidemo.infrastructure.pricing.persistence.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.pricing.model.aggregate.PricingRule;
import solid.humank.genaidemo.domain.pricing.model.valueobject.PriceId;
import solid.humank.genaidemo.domain.pricing.model.valueobject.ProductCategory;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;
import solid.humank.genaidemo.infrastructure.common.persistence.mapper.DomainMapper;
import solid.humank.genaidemo.infrastructure.pricing.persistence.entity.JpaPricingRuleEntity;

/**
 * 定價規則映射器
 * 負責在領域模型和JPA實體之間進行轉換
 */
@Component
public class PricingRuleMapper implements DomainMapper<PricingRule, JpaPricingRuleEntity> {

    @Override
    public JpaPricingRuleEntity toJpaEntity(PricingRule pricingRule) {
        if (pricingRule == null) {
            return null;
        }

        return new JpaPricingRuleEntity(
                pricingRule.getPriceId().getValue(),
                pricingRule.getProductId().getId(),
                pricingRule.getPromotionId().value(),
                pricingRule.getFinalPrice().getAmount(),
                pricingRule.getDiscountPercentage(),
                pricingRule.getDiscountAmount().getAmount(),
                pricingRule.getEffectiveFrom(),
                pricingRule.getEffectiveTo(),
                pricingRule.getProductCategory().name(),
                true, // isActive - assume all rules are active when saved
                LocalDateTime.now(), // createdAt
                LocalDateTime.now() // updatedAt
        );
    }

    @Override
    public PricingRule toDomainModel(JpaPricingRuleEntity entity) {
        if (entity == null) {
            return null;
        }

        return new PricingRule(
                new PriceId(entity.getId()),
                new ProductId(entity.getProductId()),
                PromotionId.of(entity.getPromotionId()),
                Money.of(entity.getBasePrice()),
                entity.getDiscountPercentage(),
                Money.of(entity.getDiscountAmount()),
                entity.getValidFrom(),
                entity.getValidTo(),
                ProductCategory.valueOf(entity.getProductCategory()));
    }
}