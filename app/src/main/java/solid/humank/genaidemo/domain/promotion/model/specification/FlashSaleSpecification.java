package solid.humank.genaidemo.domain.promotion.model.specification;

import java.time.LocalDateTime;
import solid.humank.genaidemo.domain.common.annotations.Specification;
import solid.humank.genaidemo.domain.promotion.model.valueobject.FlashSaleRule;

/** 限時特價規格 檢查當前時間是否在限時特價的有效時間範圍內 */
@Specification(description = "限時特價規格，檢查當前時間是否在限時特價的有效時間範圍內")
public class FlashSaleSpecification implements PromotionSpecification {
    private final FlashSaleRule rule;

    public FlashSaleSpecification(FlashSaleRule rule) {
        this.rule = rule;
    }

    @Override
    public boolean isSatisfiedBy(PromotionContext context) {
        LocalDateTime currentTime = context.getCurrentTime();

        // 使用FlashSaleRule中的flashSalePeriod來檢查時間範圍
        return rule.flashSalePeriod().contains(currentTime);
    }
}
