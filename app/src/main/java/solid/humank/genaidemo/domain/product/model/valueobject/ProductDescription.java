package solid.humank.genaidemo.domain.product.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

@ValueObject
public record ProductDescription(String description) {

    public ProductDescription {
        if (description == null) {
            description = "";
        } else {
            description = description.trim();
        }
    }

    public static ProductDescription of(String description) {
        return new ProductDescription(description);
    }

    public static ProductDescription empty() {
        return new ProductDescription("");
    }

    public String getDescription() {
        return description;
    }

    public boolean isEmpty() {
        return description.isEmpty();
    }

    @Override
    public String toString() {
        return description;
    }
}
