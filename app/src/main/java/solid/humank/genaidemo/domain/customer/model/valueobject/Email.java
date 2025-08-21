package solid.humank.genaidemo.domain.customer.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 電子郵件值對象 - 使用 Record 實作 */
@ValueObject
public record Email(String value) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public Email {
        if (value == null || !isValidEmail(value)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        // 正規化為小寫
        value = value.toLowerCase();
    }

    /**
     * 驗證電子郵件格式
     *
     * @param email 電子郵件字符串
     * @return 是否為有效格式
     */
    private static boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }

    /**
     * 獲取電子郵件（向後相容方法）
     *
     * @return 電子郵件字符串
     */
    public String getEmail() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
