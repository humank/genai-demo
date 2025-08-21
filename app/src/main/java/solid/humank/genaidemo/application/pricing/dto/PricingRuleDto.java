package solid.humank.genaidemo.application.pricing.dto;

import java.time.LocalDateTime;

/** 定價規則DTO 用於應用層與外部系統交互 */
public class PricingRuleDto {
    private String priceId;
    private String productId;
    private String promotionId;
    private double finalPrice;
    private String currency;
    private double discountPercentage;
    private double discountAmount;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private boolean active;
    private ProductCategoryDto productCategory;
    private int normalCommissionRate;
    private int eventCommissionRate;

    public PricingRuleDto(
            String priceId,
            String productId,
            String promotionId,
            double finalPrice,
            String currency,
            double discountPercentage,
            double discountAmount,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            boolean active,
            ProductCategoryDto productCategory,
            int normalCommissionRate,
            int eventCommissionRate) {
        this.priceId = priceId;
        this.productId = productId;
        this.promotionId = promotionId;
        this.finalPrice = finalPrice;
        this.currency = currency;
        this.discountPercentage = discountPercentage;
        this.discountAmount = discountAmount;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.active = active;
        this.productCategory = productCategory;
        this.normalCommissionRate = normalCommissionRate;
        this.eventCommissionRate = eventCommissionRate;
    }

    public String getPriceId() {
        return priceId;
    }

    public String getProductId() {
        return productId;
    }

    public String getPromotionId() {
        return promotionId;
    }

    public double getFinalPrice() {
        return finalPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public double getDiscountPercentage() {
        return discountPercentage;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public boolean isActive() {
        return active;
    }

    public ProductCategoryDto getProductCategory() {
        return productCategory;
    }

    public int getNormalCommissionRate() {
        return normalCommissionRate;
    }

    public int getEventCommissionRate() {
        return eventCommissionRate;
    }
}
