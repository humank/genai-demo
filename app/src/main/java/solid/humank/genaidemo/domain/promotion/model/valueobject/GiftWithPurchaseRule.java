package solid.humank.genaidemo.domain.promotion.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;

/**
 * 滿額贈禮規則
 */
@ValueObject
public class GiftWithPurchaseRule {
    private final Money minimumPurchaseAmount;
    private final ProductId giftProductId;
    private final Money giftValue;
    private final int maxGiftsPerOrder;
    private final boolean isMultipleGiftsAllowed;

    public GiftWithPurchaseRule(Money minimumPurchaseAmount, ProductId giftProductId, Money giftValue, 
                               int maxGiftsPerOrder, boolean isMultipleGiftsAllowed) {
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
}