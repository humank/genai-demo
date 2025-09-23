package solid.humank.genaidemo.domain.observability.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 用戶 ID 值對象
 * 
 * 代表用戶的唯一標識符，用於關聯用戶行為和業務數據。
 * 支援匿名用戶追蹤（通過 Optional 處理）。
 * 
 * 設計原則：
 * - 不可變值對象
 * - 包含驗證邏輯
 * - 支援多種用戶 ID 格式
 */
@ValueObject
public record UserId(String value) {

    public UserId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (value.length() > 255) {
            throw new IllegalArgumentException("User ID cannot exceed 255 characters");
        }
    }

    /**
     * 從字符串創建用戶 ID
     * 
     * @param value 用戶 ID 字符串
     * @return 用戶 ID 實例
     */
    public static UserId of(String value) {
        return new UserId(value);
    }

    /**
     * 檢查是否為匿名用戶 ID
     * 
     * @return 如果是匿名用戶返回 true
     */
    public boolean isAnonymous() {
        return value.startsWith("anonymous-") || value.startsWith("guest-");
    }

    /**
     * 檢查是否為註冊用戶 ID
     * 
     * @return 如果是註冊用戶返回 true
     */
    public boolean isRegistered() {
        return !isAnonymous();
    }

    /**
     * 獲取用戶類型
     * 
     * @return 用戶類型字符串
     */
    public String getUserType() {
        return isAnonymous() ? "anonymous" : "registered";
    }

    /**
     * 獲取短格式的用戶 ID（用於日誌顯示）
     * 
     * @return 短格式用戶 ID
     */
    public String getShortFormat() {
        if (value.length() <= 8) {
            return value;
        }
        return value.substring(0, 8) + "...";
    }

    @Override
    public String toString() {
        return value;
    }
}