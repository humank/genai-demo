package solid.humank.genaidemo.domain.inventory.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 庫存預留ID值對象
 */
@ValueObject(name = "StockReservationId", description = "庫存預留唯一標識符")
public record StockReservationId(UUID value) {

    public StockReservationId {
        Objects.requireNonNull(value, "StockReservation ID cannot be null");
    }

    public static StockReservationId generate() {
        return new StockReservationId(UUID.randomUUID());
    }

    public static StockReservationId of(String id) {
        return new StockReservationId(UUID.fromString(id));
    }

    public static StockReservationId of(UUID id) {
        return new StockReservationId(id);
    }

    public String getValue() {
        return value.toString();
    }
}