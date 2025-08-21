package solid.humank.genaidemo.domain.inventory.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 庫存閾值ID值對象
 */
@ValueObject(name = "InventoryThresholdId", description = "庫存閾值唯一標識符")
public record InventoryThresholdId(UUID value) {

    public InventoryThresholdId {
        Objects.requireNonNull(value, "InventoryThreshold ID cannot be null");
    }

    public static InventoryThresholdId generate() {
        return new InventoryThresholdId(UUID.randomUUID());
    }

    public static InventoryThresholdId of(String id) {
        return new InventoryThresholdId(UUID.fromString(id));
    }

    public static InventoryThresholdId of(UUID id) {
        return new InventoryThresholdId(id);
    }

    public String getValue() {
        return value.toString();
    }
}