package solid.humank.genaidemo.domain.product;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 產品ID值對象
 */
@ValueObject(name = "ProductId", description = "產品唯一標識符")
public class ProductId {
    
    private final String value;
    
    public ProductId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("產品ID不能為空");
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
        ProductId that = (ProductId) obj;
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