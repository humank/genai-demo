package solid.humank.genaidemo.application.pricing.dto;

import java.time.LocalDateTime;

/** 創建定價規則命令 用於接收創建定價規則的請求 */
public class CreatePricingRuleCommand {
    private String productId;
    private String promotionId;
    private double finalPrice;
    private String currency;
    private double discountPercentage;
    private double discountAmount;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private ProductCategoryDto productCategory;

    public CreatePricingRuleCommand(
            String productId,
            String promotionId,
            double finalPrice,
            String currency,
            double discountPercentage,
            double discountAmount,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            ProductCategoryDto productCategory) {
        this.productId = productId;
        this.promotionId = promotionId;
        this.finalPrice = finalPrice;
        this.currency = currency;
        this.discountPercentage = discountPercentage;
        this.discountAmount = discountAmount;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.productCategory = productCategory;
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

    public ProductCategoryDto getProductCategory() {
        return productCategory;
    }
}
