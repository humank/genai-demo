package solid.humank.genaidemo.domain.customer.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 客戶姓名值對象 - 使用 Record 實作 */
@ValueObject
public record CustomerName(String value) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public CustomerName {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        // 正規化：去除前後空白
        value = value.trim();
    }

    /**
     * 獲取姓名（向後相容方法）
     *
     * @return 姓名字符串
     */
    public String getName() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
