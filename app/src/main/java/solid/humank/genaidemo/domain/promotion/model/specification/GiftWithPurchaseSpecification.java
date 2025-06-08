package solid.humank.genaidemo.domain.promotion.model.specification;

import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.promotion.model.valueobject.GiftWithPurchaseRule;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 滿額贈禮規格
 * 檢查訂單金額是否滿足滿額贈禮條件
 */
public class GiftWithPurchaseSpecification implements PromotionSpecification {
    private final GiftWithPurchaseRule rule;

    public GiftWithPurchaseSpecification(GiftWithPurchaseRule rule) {
        this.rule = rule;
    }

    @Override
    public boolean isSatisfiedBy(PromotionContext context) {
        Order order = context.getOrder();
        Money orderTotal = order.getTotalAmount();
        
        // 檢查訂單金額是否滿足最低消費要求
        return orderTotal.isGreaterThan(rule.getMinimumPurchaseAmount()) || 
               orderTotal.equals(rule.getMinimumPurchaseAmount());
    }
    
    /**
     * 計算可獲得的贈品數量
     * 
     * @param context 促銷上下文
     * @return 可獲得的贈品數量
     */
    public int calculateGiftQuantity(PromotionContext context) {
        if (!isSatisfiedBy(context)) {
            return 0;
        }
        
        Order order = context.getOrder();
        Money orderTotal = order.getTotalAmount();
        
        if (!rule.isMultipleGiftsAllowed()) {
            return 1;
        }
        
        // 計算可獲得的贈品數量
        BigDecimal orderAmount = orderTotal.getAmount();
        BigDecimal minimumAmount = rule.getMinimumPurchaseAmount().getAmount();
        BigDecimal giftCount = orderAmount.divide(minimumAmount, 0, RoundingMode.DOWN);
        
        // 限制最大贈品數量
        return Math.min(giftCount.intValue(), rule.getMaxGiftsPerOrder());
    }
}