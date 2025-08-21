package solid.humank.genaidemo.domain.promotion.model.specification;

import java.util.Optional;
import solid.humank.genaidemo.domain.common.annotations.Specification;
import solid.humank.genaidemo.domain.promotion.model.valueobject.LimitedQuantityRule;

/** 限量特價規格 檢查促銷庫存是否還有剩餘 */
@Specification(name = "LimitedQuantitySpecification", description = "限量特價規格，檢查促銷庫存是否還有剩餘")
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
