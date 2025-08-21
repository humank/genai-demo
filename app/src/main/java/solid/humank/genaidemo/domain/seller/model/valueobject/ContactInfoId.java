package solid.humank.genaidemo.domain.seller.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 聯繫資訊ID值對象 - 使用 Record 實作 */
@ValueObject(name = "ContactInfoId", description = "聯繫資訊ID")
public record ContactInfoId(UUID value) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public ContactInfoId {
        Objects.requireNonNull(value, "ContactInfo ID cannot be null");
    }

    /**
     * 生成新的聯繫資訊ID
     */
    public static ContactInfoId generate() {
        return new ContactInfoId(UUID.randomUUID());
    }

    /**
     * 從UUID創建聯繫資訊ID
     */
    public static ContactInfoId of(UUID uuid) {
        return new ContactInfoId(uuid);
    }

    /**
     * 從字符串創建聯繫資訊ID
     */
    public static ContactInfoId of(String uuidString) {
        return new ContactInfoId(UUID.fromString(uuidString));
    }

    /**
     * 獲取ID值（向後相容方法）
     */
    public UUID getId() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}