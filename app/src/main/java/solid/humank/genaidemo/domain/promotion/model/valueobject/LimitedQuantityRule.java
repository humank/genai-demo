package solid.humank.genaidemo.domain.promotion.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;

/**
 * 限量特價規則
 */
@ValueObject
public class LimitedQuantityRule {
    private final ProductId productId;
    private final Money specialPrice;
    private final Money regularPrice;
    private final int totalQuantity;
    private final String promotionId;

    public LimitedQuantityRule(ProductId productId, Money specialPrice, Money regularPrice, 
                              int totalQuantity, String promotionId) {
        this.productId = productId;
        this.specialPrice = specialPrice;
        this.regularPrice = regularPrice;
        this.totalQuantity = totalQuantity;
        this.promotionId = promotionId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public Money getSpecialPrice() {
        return specialPrice;
    }

    public Money getRegularPrice() {
        return regularPrice;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public String getPromotionId() {
        return promotionId;
    }
}