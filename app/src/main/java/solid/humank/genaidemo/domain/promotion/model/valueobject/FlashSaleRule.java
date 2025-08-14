package solid.humank.genaidemo.domain.promotion.model.valueobject;

import java.time.LocalDateTime;
import java.time.ZoneId;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;

/** 限時特價規則 */
@ValueObject
public class FlashSaleRule {
    private final ProductId productId;
    private final Money specialPrice;
    private final Money regularPrice;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final ZoneId timeZone;

    public FlashSaleRule(
            ProductId productId,
            Money specialPrice,
            Money regularPrice,
            LocalDateTime startTime,
            LocalDateTime endTime,
            ZoneId timeZone) {
        this.productId = productId;
        this.specialPrice = specialPrice;
        this.regularPrice = regularPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timeZone = timeZone;
    }

    public ProductId getProductId() {
        return productId;
    }

    public Money getSpecialPrice() {
        return specialPrice;
    }

    public Money getRegularPrice() {
        return regularPrice;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public ZoneId getTimeZone() {
        return timeZone;
    }
}
