package solid.humank.genaidemo.domain.inventory.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 庫存異動ID值對象
 */
@ValueObject(name = "StockMovementId", description = "庫存異動唯一標識符")
public record StockMovementId(UUID value) {

    public StockMovementId {
        Objects.requireNonNull(value, "StockMovement ID cannot be null");
    }

    public static StockMovementId generate() {
        return new StockMovementId(UUID.randomUUID());
    }

    public static StockMovementId of(String id) {
        return new StockMovementId(UUID.fromString(id));
    }

    public static StockMovementId of(UUID id) {
        return new StockMovementId(id);
    }

    public String getValue() {
        return value.toString();
    }
}