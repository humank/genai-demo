package solid.humank.genaidemo.domain.product.model.aggregate;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.common.lifecycle.AggregateLifecycle;
import solid.humank.genaidemo.domain.common.lifecycle.AggregateLifecycleAware;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.pricing.model.valueobject.ProductCategory;
import solid.humank.genaidemo.domain.product.model.events.ProductCreatedEvent;
import solid.humank.genaidemo.domain.product.model.events.ProductPriceChangedEvent;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;

/**
 * 產品聚合根
 */
@AggregateRoot
@AggregateLifecycle.ManagedLifecycle
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
        
        // 發布產品創建事件
        AggregateLifecycleAware.apply(new ProductCreatedEvent(
            this.productId,
            this.name,
            this.basePrice,
            this.category
        ));
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
        Money oldPrice = this.basePrice;
        this.basePrice = newPrice;
        
        // 發布產品價格變更事件
        AggregateLifecycleAware.apply(new ProductPriceChangedEvent(
            this.productId,
            this.name,
            oldPrice,
            this.basePrice
        ));
    }

    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }
}