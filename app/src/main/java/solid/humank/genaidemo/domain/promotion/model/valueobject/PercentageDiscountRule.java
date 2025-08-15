package solid.humank.genaidemo.domain.promotion.model.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.shoppingcart.model.aggregate.ShoppingCart;

/** 百分比折扣規則 */
@ValueObject(name = "PercentageDiscountRule", description = "百分比折扣規則")
public record PercentageDiscountRule(
        BigDecimal percentage, Money minimumAmount, Money maximumDiscount)
        implements PromotionRule {

    public PercentageDiscountRule {
        if (percentage == null
                || percentage.compareTo(BigDecimal.ZERO) <= 0
                || percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("折扣百分比必須在 0-100 之間");
        }
        if (minimumAmount == null || minimumAmount.getAmount().compareTo(BigDecimal.ZERO) < 0) {
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

        Money totalAmount = cart.calculateTotal();
        BigDecimal discountAmount =
                totalAmount
                        .getAmount()
                        .multiply(percentage)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        Money discount = new Money(discountAmount, totalAmount.getCurrency());

        // 如果有最大折扣限制，則應用限制
        if (maximumDiscount != null
                && discount.getAmount().compareTo(maximumDiscount.getAmount()) > 0) {
            return maximumDiscount;
        }

        return discount;
    }

    @Override
    public String getDescription() {
        String desc = String.format("滿 %s 享 %s%% 折扣", minimumAmount, percentage);
        if (maximumDiscount != null) {
            desc += String.format("，最高折扣 %s", maximumDiscount);
        }
        return desc;
    }

    // 便利建構子
    public static PercentageDiscountRule of(BigDecimal percentage, Money minimumAmount) {
        return new PercentageDiscountRule(percentage, minimumAmount, null);
    }
}
