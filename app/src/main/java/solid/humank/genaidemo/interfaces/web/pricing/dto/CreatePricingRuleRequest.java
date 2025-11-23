package solid.humank.genaidemo.interfaces.web.pricing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 創建定價規則請求 用於接收HTTP請求中的數據，包含完整的定價規則配置資訊 */
@Schema(
        description = "創建定價規則請求",
        example =
                """
        {
            "productId": "PROD-001",
            "promotionId": "PROMO-2024-001",
            "finalPrice": 899.99,
            "currency": "TWD",
            "discountPercentage": 15.0,
            "discountAmount": 100.0,
            "effectiveFrom": "2024-01-01T00:00:00",
            "effectiveTo": "2024-12-31T23:59:59",
            "productCategory": "ELECTRONICS",
            "pricingStrategy": "PERCENTAGE_DISCOUNT"
        }
        """)
public class CreatePricingRuleRequest {
    @Schema(
            description = "產品唯一識別碼",
            example = "PROD-001",
            requiredMode = Schema.RequiredMode.REQUIRED,
            pattern = "^PROD-[0-9]{3,}$")
    private String productId;

    @Schema(
            description = "促銷活動識別碼，可選欄位，用於關聯特定促銷活動",
            example = "PROMO-2024-001",
            pattern = "^PROMO-[0-9]{4}-[0-9]{3}$")
    private String promotionId;

    @Schema(
            description = "最終價格，經過所有折扣計算後的實際售價",
            example = "899.99",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minimum = "0.01",
            maximum = "999999.99")
    private double finalPrice;

    @Schema(
            description = "貨幣代碼，遵循 ISO 4217 標準",
            example = "TWD",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"TWD", "USD", "EUR", "JPY", "CNY"})
    private String currency;

    @Schema(
            description = "折扣百分比，範圍 0-100，表示折扣的百分比數值",
            example = "15.0",
            minimum = "0.0",
            maximum = "100.0",
            format = "percentage")
    private double discountPercentage;

    @Schema(description = "固定折扣金額，與折扣百分比擇一使用", example = "100.0", minimum = "0.0")
    private double discountAmount;

    @Schema(
            description = "定價規則生效開始時間",
            example = "2024-01-01T00:00:00",
            requiredMode = Schema.RequiredMode.REQUIRED,
            format = "date-time")
    private LocalDateTime effectiveFrom;

    @Schema(
            description = "定價規則生效結束時間",
            example = "2024-12-31T23:59:59",
            requiredMode = Schema.RequiredMode.REQUIRED,
            format = "date-time")
    private LocalDateTime effectiveTo;

    @Schema(description = "產品類別，用於分類管理和批量定價策略", example = "ELECTRONICS", requiredMode = Schema.RequiredMode.REQUIRED)
    private ProductCategoryDto productCategory;

    @Schema(description = "定價策略類型，決定如何計算最終價格的邏輯", example = "PERCENTAGE_DISCOUNT", requiredMode = Schema.RequiredMode.REQUIRED)
    private PricingStrategyType pricingStrategy;

    // 無參構造函數，用於JSON反序列化
    public CreatePricingRuleRequest() {}

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(String promotionId) {
        this.promotionId = promotionId;
    }

    public double getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(double finalPrice) {
        this.finalPrice = finalPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public double getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDateTime effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public ProductCategoryDto getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(ProductCategoryDto productCategory) {
        this.productCategory = productCategory;
    }

    public PricingStrategyType getPricingStrategy() {
        return pricingStrategy;
    }

    public void setPricingStrategy(PricingStrategyType pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }
}
