package solid.humank.genaidemo.domain.pricing.service;

import java.util.Optional;
import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.pricing.model.aggregate.PricingRule;
import solid.humank.genaidemo.domain.pricing.model.entity.CommissionRate;
import solid.humank.genaidemo.domain.pricing.model.valueobject.ProductCategory;
import solid.humank.genaidemo.domain.pricing.repository.PricingRuleRepository;
import solid.humank.genaidemo.domain.product.model.aggregate.Product;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;

/** 佣金服務 重構後作為領域服務，協調聚合之間的操作 */
@DomainService(description = "佣金服務，協調聚合之間的佣金計算操作和定價規則")
public class CommissionService {

    private final PricingRuleRepository pricingRuleRepository;

    public CommissionService(PricingRuleRepository pricingRuleRepository) {
        this.pricingRuleRepository = pricingRuleRepository;
    }

    /**
     * 獲取產品類別的佣金費率
     *
     * @param category 產品類別
     * @return 佣金費率
     */
    public CommissionRate getCommissionRate(ProductCategory category) {
        // 根據產品類別查找定價規則
        return pricingRuleRepository.findByProductCategory(category).stream()
                .filter(PricingRule::isValidNow)
                .findFirst()
                .map(PricingRule::getCurrentCommissionRate)
                .orElse(new CommissionRate(0, 0));
    }

    /**
     * 獲取產品類別和活動的佣金費率
     *
     * @param category 產品類別
     * @param event 活動名稱
     * @return 佣金費率
     */
    public CommissionRate getCommissionRate(ProductCategory category, String event) {
        // 根據產品類別查找定價規則
        return pricingRuleRepository.findByProductCategory(category).stream()
                .filter(PricingRule::isValidNow)
                .findFirst()
                .map(PricingRule::getCurrentCommissionRate)
                .orElse(new CommissionRate(0, 0));
    }

    /**
     * 計算產品佣金金額
     *
     * @param product 產品
     * @param salePrice 銷售價格
     * @param event 活動名稱
     * @return 佣金金額
     */
    public Money calculateCommission(Product product, Money salePrice, String event) {
        boolean isEventPromotion = event != null && !event.isEmpty();
        return calculateCommission(product, isEventPromotion);
    }

    /** 計算產品佣金金額 現在使用PricingRule聚合來計算佣金 */
    public Money calculateCommission(Product product, boolean isEventPromotion) {
        ProductId productId = product.getId();

        // 查找產品對應的定價規則
        Optional<PricingRule> pricingRuleOpt =
                pricingRuleRepository.findByProductId(productId).stream()
                        .filter(PricingRule::isValidNow)
                        .findFirst();

        if (pricingRuleOpt.isPresent()) {
            PricingRule pricingRule = pricingRuleOpt.get();
            return pricingRule.calculateCommission(isEventPromotion);
        }

        // 如果沒有找到定價規則，返回零佣金
        return Money.ZERO;
    }

    /** 通知賣家費率變更 */
    public void notifySeller(Product product, String event, int days) {
        // 實際實現中，這裡應該發送通知給賣家
    }
}
