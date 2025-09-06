package solid.humank.genaidemo.domain.product.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 產品ID值物件 */
@ValueObject
public record ProductId(String id) {

    public ProductId {
        Objects.requireNonNull(id, "Product ID cannot be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Product ID cannot be empty");
        }
    }

    public static ProductId generateNew() {
        return new ProductId(UUID.randomUUID().toString());
    }

    public static ProductId of(String id) {
        return new ProductId(id);
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}