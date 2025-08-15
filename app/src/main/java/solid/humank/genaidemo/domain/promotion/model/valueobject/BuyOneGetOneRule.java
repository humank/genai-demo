package solid.humank.genaidemo.domain.promotion.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.shoppingcart.model.aggregate.ShoppingCart;

/** 買一送一規則 */
@ValueObject(name = "BuyOneGetOneRule", description = "買一送一規則")
public record BuyOneGetOneRule(ProductId targetProductId, int buyQuantity, int getQuantity)
        implements PromotionRule {

    public BuyOneGetOneRule {
        if (targetProductId == null) {
            throw new IllegalArgumentException("目標商品ID不能為空");
        }
        if (buyQuantity <= 0) {
            throw new IllegalArgumentException("購買數量必須大於 0");
        }
        if (getQuantity <= 0) {
            throw new IllegalArgumentException("贈送數量必須大於 0");
        }
    }

    @Override
    public boolean matches(ShoppingCart cart) {
        return cart.getItems().stream()
                .filter(item -> item.productId().equals(targetProductId))
                .anyMatch(item -> item.quantity() >= buyQuantity);
    }

    @Override
    public Money calculateDiscount(ShoppingCart cart) {
        if (!matches(cart)) {
            return Money.twd(0);
        }

        return cart.getItems().stream()
                .filter(item -> item.productId().equals(targetProductId))
                .findFirst()
                .map(
                        item -> {
                            // 計算可以享受優惠的組數
                            int sets = item.quantity() / buyQuantity;

                            // 計算贈送的商品價值
                            int freeItems = sets * getQuantity;
                            return item.unitPrice().multiply(freeItems);
                        })
                .orElse(Money.twd(0));
    }

    @Override
    public String getDescription() {
        return String.format("買 %d 送 %d：%s", buyQuantity, getQuantity, targetProductId.getId());
    }

    // 便利建構子：標準買一送一
    public static BuyOneGetOneRule standard(ProductId productId) {
        return new BuyOneGetOneRule(productId, 1, 1);
    }
}
