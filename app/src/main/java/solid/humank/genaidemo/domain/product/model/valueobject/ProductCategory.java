package solid.humank.genaidemo.domain.product.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

@ValueObject
public class ProductCategory {
    private final String name;
    private final String description;

    public ProductCategory(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductCategory that = (ProductCategory) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
