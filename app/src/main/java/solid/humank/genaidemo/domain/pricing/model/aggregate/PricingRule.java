package solid.humank.genaidemo.domain.pricing.model.aggregate;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.pricing.model.valueobject.PriceId;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;

import java.time.LocalDateTime;

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

    // Private constructor for JPA
    private PricingRule() {
    }

    public PricingRule(PriceId priceId, ProductId productId, PromotionId promotionId,
                      Money finalPrice, double discountPercentage, Money discountAmount,
                      LocalDateTime effectiveFrom, LocalDateTime effectiveTo) {
        this.priceId = priceId;
        this.productId = productId;
        this.promotionId = promotionId;
        this.finalPrice = finalPrice;
        this.discountPercentage = discountPercentage;
        this.discountAmount = discountAmount;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.isActive = true;
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
}