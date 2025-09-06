package solid.humank.genaidemo.domain.product.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 產品名稱值對象 - 使用 Record 實作 */
@ValueObject
public record ProductName(String value) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public ProductName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }
        // 正規化：去除前後空白
        value = value.trim();
    }

    /**
     * 獲取名稱（向後相容方法）
     *
     * @return 產品名稱
     */
    public String getName() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
