package solid.humank.genaidemo.examples.order;

import java.util.UUID;

import solid.humank.genaidemo.ddd.annotations.ValueObject;

@ValueObject
public record OrderId(UUID id) {
    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId of(String id) {
        return new OrderId(UUID.fromString(id));
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
