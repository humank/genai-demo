package solid.humank.genaidemo.domain.review.model.valueobject;

import java.util.UUID;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 評價ID值對象 */
@ValueObject(name = "ReviewId", description = "評價唯一識別碼")
public record ReviewId(String value) {

    public ReviewId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("評價ID不能為空");
        }
    }

    public static ReviewId generate() {
        return new ReviewId(UUID.randomUUID().toString());
    }

    public static ReviewId of(String value) {
        return new ReviewId(value);
    }
}
