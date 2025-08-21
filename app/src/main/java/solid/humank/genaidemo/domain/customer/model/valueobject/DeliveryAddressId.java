package solid.humank.genaidemo.domain.customer.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 配送地址ID值對象
 */
@ValueObject(name = "DeliveryAddressId", description = "配送地址唯一標識符")
public record DeliveryAddressId(UUID value) {

    public DeliveryAddressId {
        Objects.requireNonNull(value, "DeliveryAddress ID cannot be null");
    }

    public static DeliveryAddressId generate() {
        return new DeliveryAddressId(UUID.randomUUID());
    }

    public static DeliveryAddressId of(String id) {
        return new DeliveryAddressId(UUID.fromString(id));
    }

    public static DeliveryAddressId of(UUID id) {
        return new DeliveryAddressId(id);
    }

    public String getValue() {
        return value.toString();
    }
}