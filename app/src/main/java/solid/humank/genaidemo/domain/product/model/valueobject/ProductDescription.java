package solid.humank.genaidemo.domain.product.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

@ValueObject
public class ProductDescription {
    private final String description;

    public ProductDescription(String description) {
        this.description = description != null ? description.trim() : "";
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductDescription that = (ProductDescription) o;
        return description.equals(that.description);
    }

    @Override
    public int hashCode() {
        return description.hashCode();
    }

    @Override
    public String toString() {
        return description;
    }
}