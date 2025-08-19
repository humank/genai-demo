package solid.humank.genaidemo.domain.promotion.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;

/** 滿額贈禮規則 */
@ValueObject
public final class GiftWithPurchaseRule implements PromotionRule {
    private final Money minimumPurchaseAmount;
    private final ProductId giftProductId;
    private final Money giftValue;
    private final int maxGiftsPerOrder;
    private final boolean isMultipleGiftsAllowed;

    public GiftWithPurchaseRule(
            Money minimumPurchaseAmount,
            ProductId giftProductId,
            Money giftValue,
            int maxGiftsPerOrder,
            boolean isMultipleGiftsAllowed) {
        this.minimumPurchaseAmount = minimumPurchaseAmount;
        this.giftProductId = giftProductId;
        this.giftValue = giftValue;
        this.maxGiftsPerOrder = maxGiftsPerOrder;
        this.isMultipleGiftsAllowed = isMultipleGiftsAllowed;
    }

    public Money getMinimumPurchaseAmount() {
        return minimumPurchaseAmount;
    }

    public ProductId getGiftProductId() {
        return giftProductId;
    }

    public Money getGiftValue() {
        return giftValue;
    }

    public int getMaxGiftsPerOrder() {
        return maxGiftsPerOrder;
    }

    public boolean isMultipleGiftsAllowed() {
        return isMultipleGiftsAllowed;
    }

    @Override
    public boolean matches(CartSummary cartSummary) {
        // 檢查購物車總額是否達到最低消費金額
        return cartSummary.totalAmount().getAmount().compareTo(minimumPurchaseAmount.getAmount())
                >= 0;
    }

    @Override
    public Money calculateDiscount(CartSummary cartSummary) {
        if (!matches(cartSummary)) {
            return Money.twd(0);
        }

        // 滿額贈禮的折扣就是贈品的價值
        int giftQuantity = calculateGiftQuantity(cartSummary);
        return giftValue.multiply(giftQuantity);
    }

    @Override
    public String getDescription() {
        return String.format("滿額贈禮：消費滿 %s 贈送 %s", minimumPurchaseAmount, giftProductId.getId());
    }

    private int calculateGiftQuantity(CartSummary cartSummary) {
        if (!isMultipleGiftsAllowed) {
            return 1;
        }

        // 根據消費金額計算贈品數量
        Money totalAmount = cartSummary.totalAmount();
        int multiplier =
                totalAmount.getAmount().divide(minimumPurchaseAmount.getAmount()).intValue();
        return Math.min(multiplier, maxGiftsPerOrder);
    }
}
