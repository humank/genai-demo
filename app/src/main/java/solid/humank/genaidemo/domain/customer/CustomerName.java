package solid.humank.genaidemo.domain.customer;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 客戶姓名值對象 */
@ValueObject(name = "CustomerName", description = "客戶姓名，包含驗證規則")
public class CustomerName {

    private final String value;

    public CustomerName(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("客戶姓名不能為空");
        }
        if (value.trim().length() > 50) {
            throw new IllegalArgumentException("客戶姓名不能超過50個字符");
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
        CustomerName that = (CustomerName) obj;
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
