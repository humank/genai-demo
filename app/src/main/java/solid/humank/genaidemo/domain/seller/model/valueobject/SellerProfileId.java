package solid.humank.genaidemo.domain.seller.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 賣家檔案ID值對象 - 使用 Record 實作 */
@ValueObject(name = "SellerProfileId", description = "賣家檔案ID")
public record SellerProfileId(UUID value) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public SellerProfileId {
        Objects.requireNonNull(value, "SellerProfile ID cannot be null");
    }

    /**
     * 生成新的賣家檔案ID
     */
    public static SellerProfileId generate() {
        return new SellerProfileId(UUID.randomUUID());
    }

    /**
     * 從UUID創建賣家檔案ID
     */
    public static SellerProfileId of(UUID uuid) {
        return new SellerProfileId(uuid);
    }

    /**
     * 從字符串創建賣家檔案ID
     */
    public static SellerProfileId of(String uuidString) {
        return new SellerProfileId(UUID.fromString(uuidString));
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