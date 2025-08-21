package solid.humank.genaidemo.domain.shoppingcart.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 購物車促銷ID值對象
 */
@ValueObject(name = "CartPromotionId", description = "購物車促銷唯一標識符")
public record CartPromotionId(UUID value) {

    public CartPromotionId {
        Objects.requireNonNull(value, "CartPromotion ID cannot be null");
    }

    public static CartPromotionId generate() {
        return new CartPromotionId(UUID.randomUUID());
    }

    public static CartPromotionId of(String id) {
        return new CartPromotionId(UUID.fromString(id));
    }

    public static CartPromotionId of(UUID id) {
        return new CartPromotionId(id);
    }

    public String getValue() {
        return value.toString();
    }
}