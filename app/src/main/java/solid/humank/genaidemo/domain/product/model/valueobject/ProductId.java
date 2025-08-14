package solid.humank.genaidemo.domain.product.model.valueobject;

import java.util.Objects;
import java.util.UUID;

/** 產品ID值物件 */
public class ProductId {
    private final String id;

    public ProductId(String id) {
        this.id = id;
    }

    public static ProductId generateNew() {
        return new ProductId(UUID.randomUUID().toString());
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductId productId = (ProductId) o;
        return Objects.equals(id, productId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }
}
