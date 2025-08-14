package solid.humank.genaidemo.domain.promotion.model.specification;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

        // 將當前時間轉換為促銷規則的時區
        ZoneId systemZone = ZoneId.systemDefault();
        ZonedDateTime zonedCurrentTime = currentTime.atZone(systemZone);
        ZonedDateTime convertedTime = zonedCurrentTime.withZoneSameInstant(rule.getTimeZone());
        LocalDateTime timeInPromotionZone = convertedTime.toLocalDateTime();

        // 檢查時間是否在有效範圍內（包含起始時間）
        return (timeInPromotionZone.isEqual(rule.getStartTime())
                        || timeInPromotionZone.isAfter(rule.getStartTime()))
                && timeInPromotionZone.isBefore(rule.getEndTime());
    }
}
