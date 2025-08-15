package solid.humank.genaidemo.domain.promotion.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.shoppingcart.model.aggregate.ShoppingCart;

/** 固定金額折扣規則 */
@ValueObject(name = "FixedAmountDiscountRule", description = "固定金額折扣規則")
public record FixedAmountDiscountRule(Money discountAmount, Money minimumAmount)
        implements PromotionRule {

    public FixedAmountDiscountRule {
        if (discountAmount == null
                || discountAmount.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("折扣金額必須大於 0");
        }
        if (minimumAmount == null
                || minimumAmount.getAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("最低金額不能為負數");
        }
    }

    @Override
    public boolean matches(ShoppingCart cart) {
        Money totalAmount = cart.calculateTotal();
        return totalAmount.getAmount().compareTo(minimumAmount.getAmount()) >= 0;
    }

    @Override
    public Money calculateDiscount(ShoppingCart cart) {
        if (!matches(cart)) {
            return Money.twd(0);
        }

        // 確保折扣不超過總金額
        Money totalAmount = cart.calculateTotal();
        if (discountAmount.getAmount().compareTo(totalAmount.getAmount()) > 0) {
            return totalAmount;
        }

        return discountAmount;
    }

    @Override
    public String getDescription() {
        return String.format("滿 %s 減 %s", minimumAmount, discountAmount);
    }
}
