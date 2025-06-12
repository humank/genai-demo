package solid.humank.genaidemo.domain.product.model.events;

import solid.humank.genaidemo.domain.common.event.AbstractDomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.pricing.model.valueobject.ProductCategory;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;

/**
 * 產品創建事件
 */
public class ProductCreatedEvent extends AbstractDomainEvent {
    
    private final ProductId productId;
    private final String name;
    private final Money basePrice;
    private final ProductCategory category;
    
    public ProductCreatedEvent(ProductId productId, String name, Money basePrice, ProductCategory category) {
        super("product-service");
        this.productId = productId;
        this.name = name;
        this.basePrice = basePrice;
        this.category = category;
    }
    
    public ProductId getProductId() {
        return productId;
    }
    
    public String getName() {
        return name;
    }
    
    public Money getBasePrice() {
        return basePrice;
    }
    
    public ProductCategory getCategory() {
        return category;
    }
    
    @Override
    public String getEventType() {
        return "ProductCreatedEvent";
    }
}