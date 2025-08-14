package solid.humank.genaidemo.domain.product.model.events;

import solid.humank.genaidemo.domain.common.event.AbstractDomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;

/** 產品價格變更事件 */
public class ProductPriceChangedEvent extends AbstractDomainEvent {

    private final ProductId productId;
    private final String name;
    private final Money oldPrice;
    private final Money newPrice;

    public ProductPriceChangedEvent(
            ProductId productId, String name, Money oldPrice, Money newPrice) {
        super("product-service");
        this.productId = productId;
        this.name = name;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
    }

    public ProductId getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public Money getOldPrice() {
        return oldPrice;
    }

    public Money getNewPrice() {
        return newPrice;
    }

    @Override
    public String getEventType() {
        return "ProductPriceChangedEvent";
    }
}
