package solid.humank.genaidemo.domain.customer.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 支付方式ID值對象
 */
@ValueObject(name = "PaymentMethodId", description = "支付方式唯一標識符")
public record PaymentMethodId(UUID value) {

    public PaymentMethodId {
        Objects.requireNonNull(value, "PaymentMethod ID cannot be null");
    }

    public static PaymentMethodId generate() {
        return new PaymentMethodId(UUID.randomUUID());
    }

    public static PaymentMethodId of(String id) {
        return new PaymentMethodId(UUID.fromString(id));
    }

    public static PaymentMethodId of(UUID id) {
        return new PaymentMethodId(id);
    }

    public String getValue() {
        return value.toString();
    }
}