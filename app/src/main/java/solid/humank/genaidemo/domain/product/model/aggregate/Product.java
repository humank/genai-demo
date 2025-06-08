package solid.humank.genaidemo.domain.product.model.aggregate;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.pricing.model.valueobject.ProductCategory;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;

/**
 * 產品聚合根
 */
@AggregateRoot
public class Product {
    private ProductId productId;
    private String name;
    private String description;
    private Money basePrice;
    private ProductCategory category;
    private boolean isActive;

    // Private constructor for JPA
    private Product() {
    }

    public Product(ProductId productId, String name, String description, Money basePrice, ProductCategory category) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.category = category;
        this.isActive = true;
    }

    public ProductId getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Money getBasePrice() {
        return basePrice;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public boolean isActive() {
        return isActive;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updatePrice(Money newPrice) {
        this.basePrice = newPrice;
    }

    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }
}