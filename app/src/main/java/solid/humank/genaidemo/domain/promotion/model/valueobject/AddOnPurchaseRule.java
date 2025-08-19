package solid.humank.genaidemo.domain.promotion.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;

/** 加價購規則 */
@ValueObject
public final class AddOnPurchaseRule implements PromotionRule {
    private final ProductId mainProductId;
    private final ProductId addOnProductId;
    private final Money specialPrice;
    private final Money regularPrice;

    public AddOnPurchaseRule(
            ProductId mainProductId,
            ProductId addOnProductId,
            Money specialPrice,
            Money regularPrice) {
        this.mainProductId = mainProductId;
        this.addOnProductId = addOnProductId;
        this.specialPrice = specialPrice;
        this.regularPrice = regularPrice;
    }

    public ProductId getMainProductId() {
        return mainProductId;
    }

    public ProductId getAddOnProductId() {
        return addOnProductId;
    }

    public Money getSpecialPrice() {
        return specialPrice;
    }

    public Money getRegularPrice() {
        return regularPrice;
    }

    @Override
    public boolean matches(CartSummary cartSummary) {
        // 檢查購物車是否包含主要商品
        return cartSummary.items().stream()
                .anyMatch(item -> item.productId().equals(mainProductId.getId()));
    }

    @Override
    public Money calculateDiscount(CartSummary cartSummary) {
        if (!matches(cartSummary)) {
            return Money.twd(0);
        }

        // 計算加價購商品的折扣
        return cartSummary.items().stream()
                .filter(item -> item.productId().equals(addOnProductId.getId()))
                .findFirst()
                .map(item -> regularPrice.subtract(specialPrice).multiply(item.quantity()))
                .orElse(Money.twd(0));
    }

    @Override
    public String getDescription() {
        return String.format(
                "加價購：購買 %s 可以 %s 加購 %s",
                mainProductId.getId(), specialPrice, addOnProductId.getId());
    }
}
