package solid.humank.genaidemo.domain.product;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 產品描述值對象
 */
@ValueObject(name = "ProductDescription", description = "產品詳細描述")
public class ProductDescription {
    
    private final String value;
    
    public ProductDescription(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("產品描述不能為空");
        }
        if (value.trim().length() > 500) {
            throw new IllegalArgumentException("產品描述不能超過500個字符");
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
        ProductDescription that = (ProductDescription) obj;
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