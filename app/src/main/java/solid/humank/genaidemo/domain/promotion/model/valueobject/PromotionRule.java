package solid.humank.genaidemo.domain.promotion.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;

/** 促銷規則介面 */
@ValueObject
public sealed interface PromotionRule
        permits PercentageDiscountRule,
                FixedAmountDiscountRule,
                FlashSaleRule,
                BuyOneGetOneRule,
                AddOnPurchaseRule,
                LimitedQuantityRule,
                GiftWithPurchaseRule,
                ConvenienceStoreVoucherRule {

    /** 檢查規則是否匹配購物車 */
    boolean matches(CartSummary cartSummary);

    /** 計算折扣金額 */
    Money calculateDiscount(CartSummary cartSummary);

    /** 獲取規則描述 */
    String getDescription();
}
