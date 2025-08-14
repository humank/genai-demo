package solid.humank.genaidemo.domain.product;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 產品分類值對象
 */
@ValueObject(name = "ProductCategory", description = "產品分類")
public class ProductCategory {
    
    private final String value;
    
    public ProductCategory(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("產品分類不能為空");
        }
        if (value.trim().length() > 50) {
            throw new IllegalArgumentException("產品分類不能超過50個字符");
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
        ProductCategory that = (ProductCategory) obj;
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