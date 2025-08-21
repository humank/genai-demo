package solid.humank.genaidemo.domain.shoppingcart.model.valueobject;

import java.util.UUID;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 購物車ID值對象 */
@ValueObject(name = "ShoppingCartId", description = "購物車唯一識別碼")
public record ShoppingCartId(String value) {

    public ShoppingCartId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("購物車ID不能為空");
        }
    }

    public static ShoppingCartId generate() {
        return new ShoppingCartId(UUID.randomUUID().toString());
    }

    public static ShoppingCartId of(String value) {
        return new ShoppingCartId(value);
    }
}
