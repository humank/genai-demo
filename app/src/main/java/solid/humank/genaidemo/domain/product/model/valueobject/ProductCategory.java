package solid.humank.genaidemo.domain.product.model.valueobject;

import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

@ValueObject
public record ProductCategory(String name, String description) {

    public ProductCategory {
        Objects.requireNonNull(name, "Product category name cannot be null");
        Objects.requireNonNull(description, "Product category description cannot be null");
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product category name cannot be empty");
        }
    }

    public static ProductCategory of(String name, String description) {
        return new ProductCategory(name, description);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
