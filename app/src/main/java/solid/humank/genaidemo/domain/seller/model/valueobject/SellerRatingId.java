package solid.humank.genaidemo.domain.seller.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 賣家評級ID值對象 - 使用 Record 實作 */
@ValueObject(name = "SellerRatingId", description = "賣家評級ID")
public record SellerRatingId(UUID value) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public SellerRatingId {
        Objects.requireNonNull(value, "SellerRating ID cannot be null");
    }

    /**
     * 生成新的賣家評級ID
     */
    public static SellerRatingId generate() {
        return new SellerRatingId(UUID.randomUUID());
    }

    /**
     * 從UUID創建賣家評級ID
     */
    public static SellerRatingId of(UUID uuid) {
        return new SellerRatingId(uuid);
    }

    /**
     * 從字符串創建賣家評級ID
     */
    public static SellerRatingId of(String uuidString) {
        return new SellerRatingId(UUID.fromString(uuidString));
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