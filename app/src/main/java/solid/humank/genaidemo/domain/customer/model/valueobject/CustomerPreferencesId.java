package solid.humank.genaidemo.domain.customer.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 客戶偏好設定ID值對象
 */
@ValueObject(name = "CustomerPreferencesId", description = "客戶偏好設定唯一標識符")
public record CustomerPreferencesId(UUID value) {

    public CustomerPreferencesId {
        Objects.requireNonNull(value, "CustomerPreferences ID cannot be null");
    }

    public static CustomerPreferencesId generate() {
        return new CustomerPreferencesId(UUID.randomUUID());
    }

    public static CustomerPreferencesId of(String id) {
        return new CustomerPreferencesId(UUID.fromString(id));
    }

    public static CustomerPreferencesId of(UUID id) {
        return new CustomerPreferencesId(id);
    }

    public String getValue() {
        return value.toString();
    }
}