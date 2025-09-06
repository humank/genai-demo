package solid.humank.genaidemo.domain.promotion.model.valueobject;

import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;

/** 加價購規則 */
@ValueObject
public record AddOnPurchaseRule(
        ProductId mainProductId,
        ProductId addOnProductId,
        Money specialPrice,
        Money regularPrice) implements PromotionRule {

    public AddOnPurchaseRule {
        Objects.requireNonNull(mainProductId, "Main product ID cannot be null");
        Objects.requireNonNull(addOnProductId, "Add-on product ID cannot be null");
        Objects.requireNonNull(specialPrice, "Special price cannot be null");
        Objects.requireNonNull(regularPrice, "Regular price cannot be null");

        if (specialPrice.amount().compareTo(regularPrice.amount()) > 0) {
            throw new IllegalArgumentException("Special price cannot be higher than regular price");
        }
    }

    public static AddOnPurchaseRule create(
            ProductId mainProductId,
            ProductId addOnProductId,
            Money specialPrice,
            Money regularPrice) {
        return new AddOnPurchaseRule(mainProductId, addOnProductId, specialPrice, regularPrice);
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
