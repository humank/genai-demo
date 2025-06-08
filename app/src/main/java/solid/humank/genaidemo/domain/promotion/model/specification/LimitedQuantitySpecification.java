package solid.humank.genaidemo.domain.promotion.model.specification;

import solid.humank.genaidemo.domain.promotion.model.valueobject.LimitedQuantityRule;

import java.util.Optional;

/**
 * 限量特價規格
 * 檢查促銷庫存是否還有剩餘
 */
public class LimitedQuantitySpecification implements PromotionSpecification {
    private final LimitedQuantityRule rule;

    public LimitedQuantitySpecification(LimitedQuantityRule rule) {
        this.rule = rule;
    }

    @Override
    public boolean isSatisfiedBy(PromotionContext context) {
        // 檢查促銷庫存是否還有剩餘
        Optional<Integer> remainingQuantity = context.getRemainingQuantity(rule.getPromotionId());
        return remainingQuantity.isPresent() && remainingQuantity.get() > 0;
    }
}