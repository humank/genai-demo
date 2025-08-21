package solid.humank.genaidemo.domain.promotion.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;

/** 限量特價規則 */
@ValueObject
public final class LimitedQuantityRule implements PromotionRule {
    private final ProductId productId;
    private final Money specialPrice;
    private final Money regularPrice;
    private final int totalQuantity;
    private final String promotionId;

    public LimitedQuantityRule(
            ProductId productId,
            Money specialPrice,
            Money regularPrice,
            int totalQuantity,
            String promotionId) {
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

    @Override
    public boolean matches(CartSummary cartSummary) {
        // 檢查購物車是否包含目標商品
        return cartSummary.items().stream()
                .anyMatch(item -> item.productId().equals(productId.getId()));
    }

    @Override
    public Money calculateDiscount(CartSummary cartSummary) {
        if (!matches(cartSummary)) {
            return Money.twd(0);
        }

        // 計算限量特價的折扣
        return cartSummary.items().stream()
                .filter(item -> item.productId().equals(productId.getId()))
                .findFirst()
                .map(
                        item -> {
                            int applicableQuantity = Math.min(item.quantity(), totalQuantity);
                            return regularPrice.subtract(specialPrice).multiply(applicableQuantity);
                        })
                .orElse(Money.twd(0));
    }

    @Override
    public String getDescription() {
        return String.format(
                "限量特價：%s 特價 %s，限量 %d 件", productId.getId(), specialPrice, totalQuantity);
    }
}
