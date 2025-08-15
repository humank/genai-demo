package solid.humank.genaidemo.domain.promotion.model.valueobject;

import java.time.LocalDateTime;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 日期範圍值對象 */
@ValueObject(name = "DateRange", description = "日期範圍值對象，用於表示促銷活動的有效期間")
public record DateRange(LocalDateTime startDate, LocalDateTime endDate) {
    public DateRange {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("開始日期和結束日期不能為空");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("開始日期不能晚於結束日期");
        }
    }

    public boolean contains(LocalDateTime dateTime) {
        return !dateTime.isBefore(startDate) && !dateTime.isAfter(endDate);
    }

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return contains(now);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endDate);
    }

    public boolean isUpcoming() {
        return LocalDateTime.now().isBefore(startDate);
    }

    public long getDurationInDays() {
        return java.time.Duration.between(startDate, endDate).toDays();
    }
}
