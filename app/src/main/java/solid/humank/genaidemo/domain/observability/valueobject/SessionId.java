package solid.humank.genaidemo.domain.observability.valueobject;

import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 會話 ID 值對象
 * 
 * 代表用戶會話的唯一標識符，用於追蹤用戶在系統中的活動。
 * 
 * 設計原則：
 * - 不可變值對象
 * - 包含驗證邏輯
 * - 支援多種格式的會話 ID
 */
@ValueObject
public record SessionId(String value) {

    public SessionId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        if (value.length() > 255) {
            throw new IllegalArgumentException("Session ID cannot exceed 255 characters");
        }
    }

    /**
     * 生成新的會話 ID
     * 
     * @return 新的會話 ID
     */
    public static SessionId generate() {
        return new SessionId("session-" + UUID.randomUUID().toString());
    }

    /**
     * 從字符串創建會話 ID
     * 
     * @param value 會話 ID 字符串
     * @return 會話 ID 實例
     */
    public static SessionId of(String value) {
        return new SessionId(value);
    }

    /**
     * 檢查是否為有效的 UUID 格式
     * 
     * @return 如果是 UUID 格式返回 true
     */
    public boolean isUuidFormat() {
        try {
            if (value.startsWith("session-")) {
                UUID.fromString(value.substring(8));
                return true;
            }
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 獲取短格式的會話 ID（用於日誌顯示）
     * 
     * @return 短格式會話 ID
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