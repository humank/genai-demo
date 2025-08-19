package solid.humank.genaidemo.domain.promotion.model.valueobject;

import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 優惠券ID值對象 */
@ValueObject(name = "VoucherId", description = "優惠券唯一識別碼")
public record VoucherId(String value) {

    public VoucherId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("優惠券ID不能為空");
        }
    }

    public static VoucherId generate() {
        return new VoucherId(UUID.randomUUID().toString());
    }

    public static VoucherId of(String value) {
        return new VoucherId(value);
    }
}