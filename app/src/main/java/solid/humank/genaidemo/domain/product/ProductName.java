package solid.humank.genaidemo.domain.product;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 產品名稱值對象 */
@ValueObject(name = "ProductName", description = "產品名稱，包含驗證規則")
public class ProductName {

    private final String value;

    public ProductName(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("產品名稱不能為空");
        }
        if (value.trim().length() > 100) {
            throw new IllegalArgumentException("產品名稱不能超過100個字符");
        }
        this.value = value.trim();
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ProductName that = (ProductName) obj;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
