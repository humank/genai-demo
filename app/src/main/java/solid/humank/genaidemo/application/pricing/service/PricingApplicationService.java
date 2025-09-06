package solid.humank.genaidemo.application.pricing.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import solid.humank.genaidemo.application.pricing.dto.CreatePricingRuleCommand;
import solid.humank.genaidemo.application.pricing.dto.PricingRuleDto;
import solid.humank.genaidemo.application.pricing.dto.ProductCategoryDto;
import solid.humank.genaidemo.application.pricing.dto.UpdateCommissionRateCommand;
import solid.humank.genaidemo.application.pricing.mapper.ProductCategoryMapper;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.pricing.model.aggregate.PricingRule;
import solid.humank.genaidemo.domain.pricing.model.entity.CommissionRate;
import solid.humank.genaidemo.domain.pricing.model.valueobject.PriceId;
import solid.humank.genaidemo.domain.pricing.model.valueobject.ProductCategory;
import solid.humank.genaidemo.domain.pricing.repository.PricingRuleRepository;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;

/** 定價應用服務 提供定價相關的應用層功能 */
@Service
public class PricingApplicationService {

    private final PricingRuleRepository pricingRuleRepository;

    public PricingApplicationService(PricingRuleRepository pricingRuleRepository) {
        this.pricingRuleRepository = pricingRuleRepository;
    }

    /** 創建定價規則 */
    public PricingRuleDto createPricingRule(CreatePricingRuleCommand command) {
        PriceId priceId = PriceId.generate();
        ProductId productId = new ProductId(command.getProductId());
        PromotionId promotionId = command.getPromotionId() != null ? new PromotionId(command.getPromotionId()) : null;
        Money finalPrice = Money.of(command.getFinalPrice(), command.getCurrency());
        Money discountAmount = Money.of(command.getDiscountAmount(), command.getCurrency());
        ProductCategory category = ProductCategoryMapper.toDomain(command.getProductCategory());

        PricingRule pricingRule = new PricingRule(
                priceId,
                productId,
                promotionId,
                finalPrice,
                command.getDiscountPercentage(),
                discountAmount,
                command.getEffectiveFrom(),
                command.getEffectiveTo(),
                category);

        PricingRule savedRule = pricingRuleRepository.save(pricingRule);
        return mapToDto(savedRule);
    }

    /** 更新佣金費率 */
    public PricingRuleDto updateCommissionRate(UpdateCommissionRateCommand command) {
        Optional<PricingRule> pricingRuleOpt = pricingRuleRepository.findById(new PriceId(command.getPriceId()));

        if (pricingRuleOpt.isPresent()) {
            PricingRule pricingRule = pricingRuleOpt.get();
            pricingRule.updateCommissionRate(command.getNormalRate(), command.getEventRate());
            PricingRule savedRule = pricingRuleRepository.save(pricingRule);
            return mapToDto(savedRule);
        }

        throw new IllegalArgumentException("定價規則不存在: " + command.getPriceId());
    }

    /** 獲取產品的定價規則 */
    public List<PricingRuleDto> getPricingRulesForProduct(String productId) {
        List<PricingRule> rules = pricingRuleRepository.findByProductId(new ProductId(productId));
        return rules.stream().map(this::mapToDto).toList();
    }

    /** 獲取產品類別的定價規則 */
    public List<PricingRuleDto> getPricingRulesByCategory(ProductCategoryDto categoryDto) {
        ProductCategory category = ProductCategoryMapper.toDomain(categoryDto);
        List<PricingRule> rules = pricingRuleRepository.findByProductCategory(category);
        return rules.stream().map(this::mapToDto).toList();
    }

    /** 將領域對象映射為DTO */
    private PricingRuleDto mapToDto(PricingRule pricingRule) {
        CommissionRate rate = pricingRule.getCurrentCommissionRate();

        return new PricingRuleDto(
                pricingRule.getPriceId().getId(),
                pricingRule.getProductId().getId(),
                pricingRule.getPromotionId() != null ? pricingRule.getPromotionId().value() : null,
                pricingRule.getFinalPrice().getAmount().doubleValue(),
                pricingRule.getFinalPrice().getCurrency().getCurrencyCode(),
                pricingRule.getDiscountPercentage(),
                pricingRule.getDiscountAmount().getAmount().doubleValue(),
                pricingRule.getEffectiveFrom(),
                pricingRule.getEffectiveTo(),
                pricingRule.isActive(),
                ProductCategoryMapper.toDto(pricingRule.getProductCategory()),
                rate != null ? rate.getNormalRate() : 0,
                rate != null ? rate.getEventRate() : 0);
    }

    /**
     * 應用促銷定價
     * 
     * @param promotionId    促銷ID
     * @param promotionName  促銷名稱
     * @param promotionType  促銷類型
     * @param discountAmount 折扣金額
     * @param validFrom      有效開始時間
     * @param validTo        有效結束時間
     */
    public void applyPromotionPricing(String promotionId, String promotionName, String promotionType,
            double discountAmount, java.time.LocalDateTime validFrom, java.time.LocalDateTime validTo) {

        // 查找與此促銷相關的定價規則
        PromotionId promoId = new PromotionId(promotionId);
        List<PricingRule> existingRules = pricingRuleRepository.findByPromotionId(promoId);

        // 更新現有規則或創建新規則
        for (PricingRule rule : existingRules) {
            // 更新折扣金額和有效期
            // 這裡需要在領域模型中添加更新促銷定價的方法
            // 暫時保存現有規則
            pricingRuleRepository.save(rule);
        }

        // 如果沒有現有規則，可能需要為相關產品創建新的定價規則
        // 這裡的具體邏輯取決於業務需求
    }
}
