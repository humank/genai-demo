package solid.humank.genaidemo.domain.customer;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 客戶ID值對象
 */
@ValueObject(name = "CustomerId", description = "客戶唯一標識符")
public class CustomerId {
    
    private final String value;
    
    public CustomerId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("客戶ID不能為空");
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
        CustomerId that = (CustomerId) obj;
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