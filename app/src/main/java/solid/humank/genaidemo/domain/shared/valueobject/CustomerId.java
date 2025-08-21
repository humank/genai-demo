package solid.humank.genaidemo.domain.shared.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 客戶ID值對象 - 使用 Record 實作，Shared Kernel 中的統一客戶標識符 */
@ValueObject(name = "CustomerId", description = "客戶唯一標識符，支援 String 和 UUID 格式")
public record CustomerId(String value) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public CustomerId {
        Objects.requireNonNull(value, "Customer ID cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be empty");
        }
    }

    /**
     * 從UUID創建客戶ID
     *
     * @param uuid UUID
     */
    public CustomerId(UUID uuid) {
        this(Objects.requireNonNull(uuid, "Customer UUID cannot be null").toString());
    }

    /**
     * 生成新的客戶ID
     *
     * @return 新的客戶ID
     */
    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID().toString());
    }

    /**
     * 從字符串創建客戶ID
     *
     * @param id 字符串ID
     * @return 客戶ID
     */
    public static CustomerId of(String id) {
        return new CustomerId(id);
    }

    /**
     * 從UUID創建客戶ID
     *
     * @param uuid UUID
     * @return 客戶ID
     */
    public static CustomerId of(UUID uuid) {
        return new CustomerId(uuid);
    }

    /**
     * 獲取ID字符串（向後相容方法）
     *
     * @return ID字符串
     */
    public String getId() {
        return value;
    }

    /**
     * 獲取ID值（別名）
     *
     * @return ID值
     */
    public String getValue() {
        return value;
    }

    /**
     * 嘗試轉換為UUID（如果格式正確）
     *
     * @return UUID，如果格式不正確則拋出異常
     */
    public UUID toUUID() {
        return UUID.fromString(value);
    }

    /**
     * 檢查是否為有效的UUID格式
     *
     * @return 如果是有效UUID格式返回true
     */
    public boolean isUUIDFormat() {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
