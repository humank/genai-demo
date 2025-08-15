package solid.humank.genaidemo.domain.promotion.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.shoppingcart.model.aggregate.ShoppingCart;

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
    public boolean matches(ShoppingCart cart) {
        // 檢查購物車是否包含主要商品
        return cart.getItems().stream().anyMatch(item -> item.productId().equals(mainProductId));
    }

    @Override
    public Money calculateDiscount(ShoppingCart cart) {
        if (!matches(cart)) {
            return Money.twd(0);
        }

        // 計算加價購商品的折扣
        return cart.getItems().stream()
                .filter(item -> item.productId().equals(addOnProductId))
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
