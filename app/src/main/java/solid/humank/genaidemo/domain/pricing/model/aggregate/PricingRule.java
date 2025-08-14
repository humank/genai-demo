package solid.humank.genaidemo.domain.pricing.model.aggregate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.pricing.model.entity.CommissionRate;
import solid.humank.genaidemo.domain.pricing.model.valueobject.PriceId;
import solid.humank.genaidemo.domain.pricing.model.valueobject.ProductCategory;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;

/** 定價規則聚合根 負責管理產品定價、折扣和佣金費率 */
@AggregateRoot
public class PricingRule {
    private PriceId priceId;
    private ProductId productId;
    private PromotionId promotionId;
    private Money finalPrice;
    private double discountPercentage;
    private Money discountAmount;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private boolean isActive;

    // 將CommissionRate納入聚合內
    private List<CommissionRate> commissionRates;
    private ProductCategory productCategory;

    // Private constructor for JPA
    @SuppressWarnings("unused")
    private PricingRule() {
        this.commissionRates = new ArrayList<>();
    }

    public PricingRule(
            PriceId priceId,
            ProductId productId,
            PromotionId promotionId,
            Money finalPrice,
            double discountPercentage,
            Money discountAmount,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            ProductCategory productCategory) {
        this.priceId = priceId;
        this.productId = productId;
        this.promotionId = promotionId;
        this.finalPrice = finalPrice;
        this.discountPercentage = discountPercentage;
        this.discountAmount = discountAmount;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.isActive = true;
        this.productCategory = productCategory;
        this.commissionRates = new ArrayList<>();

        // 初始化默認佣金費率
        addDefaultCommissionRate();
    }

    /** 添加佣金費率 */
    public void addCommissionRate(CommissionRate commissionRate) {
        if (commissionRate != null && commissionRate.getCategory().equals(this.productCategory)) {
            this.commissionRates.add(commissionRate);
        } else {
            throw new IllegalArgumentException("佣金費率的產品類別必須與定價規則的產品類別一致");
        }
    }

    /** 獲取所有佣金費率 */
    public List<CommissionRate> getCommissionRates() {
        return Collections.unmodifiableList(commissionRates);
    }

    /** 獲取當前適用的佣金費率 */
    public CommissionRate getCurrentCommissionRate() {
        // 如果有多個費率，可以根據業務規則選擇最適合的一個
        // 這裡簡單返回第一個
        return commissionRates.isEmpty() ? null : commissionRates.get(0);
    }

    /** 計算佣金金額 */
    public Money calculateCommission(boolean isEventPromotion) {
        CommissionRate rate = getCurrentCommissionRate();
        if (rate == null) {
            return Money.ZERO;
        }

        int ratePercentage = isEventPromotion ? rate.getEventRate() : rate.getNormalRate();
        return finalPrice.multiply(ratePercentage / 100.0);
    }

    /** 添加默認佣金費率 */
    private void addDefaultCommissionRate() {
        int normalRate = getDefaultNormalRate(productCategory);
        int eventRate = getDefaultEventRate(productCategory);
        CommissionRate defaultRate = new CommissionRate(productCategory, normalRate, eventRate);
        this.commissionRates.add(defaultRate);
    }

    /** 獲取默認一般費率 */
    private int getDefaultNormalRate(ProductCategory category) {
        return switch (category) {
            case ELECTRONICS -> 3;
            case FASHION -> 5;
            case GROCERIES -> 2;
            default -> 4;
        };
    }

    /** 獲取默認活動費率 */
    private int getDefaultEventRate(ProductCategory category) {
        return switch (category) {
            case ELECTRONICS -> 5;
            case FASHION -> 8;
            case GROCERIES -> 3;
            default -> 6;
        };
    }

    public boolean isValidNow() {
        LocalDateTime now = LocalDateTime.now();
        return isActive && now.isAfter(effectiveFrom) && now.isBefore(effectiveTo);
    }

    public PriceId getPriceId() {
        return priceId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public PromotionId getPromotionId() {
        return promotionId;
    }

    public Money getFinalPrice() {
        return finalPrice;
    }

    public double getDiscountPercentage() {
        return discountPercentage;
    }

    public Money getDiscountAmount() {
        return discountAmount;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public boolean isActive() {
        return isActive;
    }

    public ProductCategory getProductCategory() {
        return productCategory;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public void updateEffectiveDates(LocalDateTime from, LocalDateTime to) {
        this.effectiveFrom = from;
        this.effectiveTo = to;
    }

    /** 更新佣金費率 */
    public void updateCommissionRate(int normalRate, int eventRate) {
        CommissionRate rate = getCurrentCommissionRate();
        if (rate != null) {
            rate.setNormalRate(normalRate);
            rate.setEventRate(eventRate);
        } else {
            CommissionRate newRate = new CommissionRate(productCategory, normalRate, eventRate);
            addCommissionRate(newRate);
        }
    }
}
