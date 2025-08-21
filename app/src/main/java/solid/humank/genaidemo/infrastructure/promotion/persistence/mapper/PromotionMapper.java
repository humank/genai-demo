package solid.humank.genaidemo.infrastructure.promotion.persistence.mapper;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.promotion.model.aggregate.Promotion;
import solid.humank.genaidemo.domain.promotion.model.valueobject.DateRange;
import solid.humank.genaidemo.domain.promotion.model.valueobject.FlashSaleRule;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionRule;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionStatus;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionType;
import solid.humank.genaidemo.infrastructure.common.persistence.converter.JsonConverter;
import solid.humank.genaidemo.infrastructure.common.persistence.mapper.DomainMapper;
import solid.humank.genaidemo.infrastructure.promotion.persistence.entity.JpaPromotionEntity;

/**
 * 促銷映射器
 * 負責在領域模型和持久化模型之間進行轉換
 */
@Component
public class PromotionMapper implements DomainMapper<Promotion, JpaPromotionEntity> {

    private final JsonConverter jsonConverter;

    public PromotionMapper(JsonConverter jsonConverter) {
        this.jsonConverter = jsonConverter;
    }

    @Override
    public JpaPromotionEntity toJpaEntity(Promotion promotion) {
        JpaPromotionEntity entity = new JpaPromotionEntity();
        entity.setId(promotion.getId().value());
        entity.setName(promotion.getName());
        entity.setDescription(promotion.getDescription());
        entity.setType(promotion.getType().name());
        entity.setStatus(promotion.getStatus().name());
        entity.setStartDate(promotion.getValidPeriod().startDate());
        entity.setEndDate(promotion.getValidPeriod().endDate());
        entity.setUsageLimit(promotion.getUsageLimit());
        entity.setUsageCount(promotion.getUsageCount());
        entity.setCreatedAt(promotion.getCreatedAt());
        entity.setUpdatedAt(promotion.getUpdatedAt());
        entity.setRuleData(jsonConverter.toJson(promotion.getRule()));
        return entity;
    }

    @Override
    public Promotion toDomainModel(JpaPromotionEntity entity) {
        PromotionId id = PromotionId.of(entity.getId());
        PromotionType type = PromotionType.valueOf(entity.getType());
        DateRange dateRange = new DateRange(entity.getStartDate(), entity.getEndDate());
        PromotionRule rule = deserializeRule(type, entity.getRuleData(), dateRange);
        
        Promotion promotion = new Promotion(
                id,
                entity.getName(),
                entity.getDescription(),
                type,
                rule,
                dateRange
        );
        
        promotion.updateStatus(PromotionStatus.valueOf(entity.getStatus()));
        if (entity.getUsageLimit() != null) {
            promotion.setUsageLimit(entity.getUsageLimit());
        }
        
        return promotion;
    }

    private PromotionRule deserializeRule(PromotionType type, String ruleData, DateRange dateRange) {
        if (ruleData == null || ruleData.isEmpty()) {
            return createDefaultRule(type, dateRange);
        }

        try {
            // 直接從 JSON 反序列化，因為 Rule 類是 record，可以直接序列化/反序列化
            switch (type) {
                case FLASH_SALE:
                    return jsonConverter.fromJson(ruleData, 
                        solid.humank.genaidemo.domain.promotion.model.valueobject.FlashSaleRule.class);
                case ADD_ON_PURCHASE:
                    return jsonConverter.fromJson(ruleData, 
                        solid.humank.genaidemo.domain.promotion.model.valueobject.AddOnPurchaseRule.class);
                case LIMITED_QUANTITY:
                    return jsonConverter.fromJson(ruleData, 
                        solid.humank.genaidemo.domain.promotion.model.valueobject.LimitedQuantityRule.class);
                case GIFT_WITH_PURCHASE:
                    return jsonConverter.fromJson(ruleData, 
                        solid.humank.genaidemo.domain.promotion.model.valueobject.GiftWithPurchaseRule.class);
                default:
                    return createDefaultRule(type, dateRange);
            }
        } catch (Exception e) {
            return createDefaultRule(type, dateRange);
        }
    }

    private PromotionRule createDefaultRule(PromotionType type, DateRange dateRange) {
        switch (type) {
            case FLASH_SALE:
                return new FlashSaleRule(
                        new ProductId("default-product"),
                        Money.twd(0),
                        1,
                        dateRange
                );
            case ADD_ON_PURCHASE:
                return new solid.humank.genaidemo.domain.promotion.model.valueobject.AddOnPurchaseRule(
                        new ProductId("main-product"),
                        new ProductId("addon-product"),
                        Money.twd(0),
                        Money.twd(0)
                );
            case LIMITED_QUANTITY:
                return new solid.humank.genaidemo.domain.promotion.model.valueobject.LimitedQuantityRule(
                        new ProductId("limited-product"),
                        Money.twd(0),
                        Money.twd(0),
                        1,
                        "Default limited quantity"
                );
            case GIFT_WITH_PURCHASE:
                return new solid.humank.genaidemo.domain.promotion.model.valueobject.GiftWithPurchaseRule(
                        Money.twd(0),
                        new ProductId("gift-product"),
                        Money.twd(0),
                        1,
                        false
                );
            default:
                throw new IllegalArgumentException("Unknown promotion type: " + type);
        }
    }
}