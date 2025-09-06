package solid.humank.genaidemo.domain.promotion.model.valueobject;

import java.util.UUID;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 促銷ID值對象 */
@ValueObject(name = "PromotionId", description = "促銷活動唯一識別碼")
public record PromotionId(String value) {

    public PromotionId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("促銷ID不能為空");
        }
    }

    public static PromotionId generate() {
        return new PromotionId(UUID.randomUUID().toString());
    }

    public static PromotionId of(String value) {
        return new PromotionId(value);
    }
}
