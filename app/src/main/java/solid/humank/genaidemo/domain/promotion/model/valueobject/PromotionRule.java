package solid.humank.genaidemo.domain.promotion.model.valueobject;

import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.shoppingcart.model.aggregate.ShoppingCart;

/** 促銷規則介面 */
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
    boolean matches(ShoppingCart cart);

    /** 計算折扣金額 */
    Money calculateDiscount(ShoppingCart cart);

    /** 獲取規則描述 */
    String getDescription();
}
