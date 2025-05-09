package solid.humank.genaidemo.domain.order.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 訂單ID值對象
 */
@ValueObject
public class OrderId {
    private final UUID value;

    public OrderId(UUID value) {
        this.value = Objects.requireNonNull(value, "Order ID cannot be null");
    }

    public OrderId(String value) {
        this(UUID.fromString(value));
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }
    
    public static OrderId of(String value) {
        return new OrderId(value);
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderId orderId = (OrderId) o;
        return Objects.equals(value, orderId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}