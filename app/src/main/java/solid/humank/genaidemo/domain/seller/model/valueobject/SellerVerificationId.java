package solid.humank.genaidemo.domain.seller.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 賣家驗證ID值對象 - 使用 Record 實作 */
@ValueObject(name = "SellerVerificationId", description = "賣家驗證ID")
public record SellerVerificationId(UUID value) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public SellerVerificationId {
        Objects.requireNonNull(value, "SellerVerification ID cannot be null");
    }

    /**
     * 生成新的賣家驗證ID
     */
    public static SellerVerificationId generate() {
        return new SellerVerificationId(UUID.randomUUID());
    }

    /**
     * 從UUID創建賣家驗證ID
     */
    public static SellerVerificationId of(UUID uuid) {
        return new SellerVerificationId(uuid);
    }

    /**
     * 從字符串創建賣家驗證ID
     */
    public static SellerVerificationId of(String uuidString) {
        return new SellerVerificationId(UUID.fromString(uuidString));
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