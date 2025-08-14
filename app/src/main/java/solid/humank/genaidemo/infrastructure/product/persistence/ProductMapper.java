package solid.humank.genaidemo.infrastructure.product.persistence;

import solid.humank.genaidemo.domain.product.model.aggregate.Product;
import solid.humank.genaidemo.domain.product.model.valueobject.*;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import org.springframework.stereotype.Component;

import java.util.Currency;

/**
 * 產品領域模型與 JPA 實體之間的映射器
 */
@Component
public class ProductMapper {
    
    /**
     * 將 JPA 實體轉換為領域模型
     */
    public Product toDomain(ProductJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        ProductId productId = new ProductId(entity.getProductId());
        ProductName name = new ProductName(entity.getName());
        ProductDescription description = new ProductDescription(entity.getDescription());
        Money price = new Money(entity.getPrice(), Currency.getInstance(entity.getCurrency()));
        ProductCategory category = new ProductCategory(entity.getCategory(), entity.getCategory());
        // 對於庫存數量，我們需要從 inventory 表查詢，這裡先用默認值
        StockQuantity stockQuantity = new StockQuantity(0);
        
        return new Product(productId, name, description, price, category, stockQuantity);
    }
    
    /**
     * 將領域模型轉換為 JPA 實體
     */
    public ProductJpaEntity toEntity(Product product) {
        if (product == null) {
            return null;
        }
        
        return new ProductJpaEntity(
            product.getId().getId(),
            product.getName().getName(),
            product.getDescription().getDescription(),
            product.getPrice().getAmount(),
            product.getPrice().getCurrency().getCurrencyCode(),
            product.getCategory().getName()
        );
    }
    
    /**
     * 更新 JPA 實體
     */
    public void updateEntity(ProductJpaEntity entity, Product product) {
        if (entity == null || product == null) {
            return;
        }
        
        entity.setName(product.getName().getName());
        entity.setDescription(product.getDescription().getDescription());
        entity.setPrice(product.getPrice().getAmount());
        entity.setCurrency(product.getPrice().getCurrency().getCurrencyCode());
        entity.setCategory(product.getCategory().getName());
    }
}