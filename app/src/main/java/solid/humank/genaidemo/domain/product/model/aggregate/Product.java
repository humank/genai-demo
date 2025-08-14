package solid.humank.genaidemo.domain.product.model.aggregate;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.*;

/**
 * 產品聚合根
 */
@AggregateRoot(name = "Product", description = "產品聚合根，管理產品信息和庫存")
public class Product {
    
    private final ProductId id;
    private final ProductName name;
    private final ProductDescription description;
    private final Money price;
    private final ProductCategory category;
    private final StockQuantity stockQuantity;
    private final boolean inStock;
    
    public Product(ProductId id, ProductName name, ProductDescription description,
                  Money price, ProductCategory category, StockQuantity stockQuantity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.stockQuantity = stockQuantity;
        this.inStock = stockQuantity.getValue() > 0;
    }
    
    public ProductId getId() {
        return id;
    }
    
    public ProductName getName() {
        return name;
    }
    
    public ProductDescription getDescription() {
        return description;
    }
    
    public Money getPrice() {
        return price;
    }
    
    public ProductCategory getCategory() {
        return category;
    }
    
    public StockQuantity getStockQuantity() {
        return stockQuantity;
    }
    
    public boolean isInStock() {
        return inStock;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Product product = (Product) obj;
        return id.equals(product.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}