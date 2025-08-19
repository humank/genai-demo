package solid.humank.genaidemo.domain.product.model.aggregate;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.events.ProductCreatedEvent;
import solid.humank.genaidemo.domain.product.model.events.ProductDescriptionUpdatedEvent;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductCategory;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductDescription;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductName;
import solid.humank.genaidemo.domain.product.model.valueobject.StockQuantity;

/** 產品聚合根 */
@AggregateRoot(name = "Product", description = "產品聚合根，管理產品信息和庫存", boundedContext = "Product", version = "1.0")
public class Product extends solid.humank.genaidemo.domain.common.aggregate.AggregateRoot {

    private final ProductId id;
    private ProductName name;
    private ProductDescription description;
    private Money price;
    private final ProductCategory category;
    private StockQuantity stockQuantity;
    private boolean inStock;
    private boolean isActive;

    public Product(
            ProductId id,
            ProductName name,
            ProductDescription description,
            Money price,
            ProductCategory category,
            StockQuantity stockQuantity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.stockQuantity = stockQuantity;
        this.inStock = stockQuantity.getValue() > 0;
        this.isActive = true;

        // 發布商品創建事件
        collectEvent(ProductCreatedEvent.create(id, name, price, category));
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

    public boolean isActive() {
        return isActive;
    }

    // 業務方法

    /** 更新商品價格 */
    public void updatePrice(Money newPrice) {
        if (newPrice == null) {
            throw new IllegalArgumentException("商品價格不能為空");
        }

        Money oldPrice = this.price;
        this.price = newPrice;

        // 發布商品價格變更事件
        collectEvent(
                new solid.humank.genaidemo.domain.product.model.events.ProductPriceChangedEvent(
                        this.id, oldPrice, newPrice));
    }

    /** 更新庫存 */
    public void updateStock(StockQuantity newStock) {
        if (newStock == null) {
            throw new IllegalArgumentException("庫存數量不能為空");
        }

        StockQuantity oldStock = this.stockQuantity;
        this.stockQuantity = newStock;
        this.inStock = newStock.getValue() > 0;

        // 發布商品庫存更新事件
        collectEvent(
                new solid.humank.genaidemo.domain.product.model.events.ProductStockUpdatedEvent(
                        this.id, oldStock, newStock));
    }

    /** 下架商品 */
    public void discontinue(String reason) {
        if (!this.isActive) {
            throw new IllegalStateException("商品已經下架");
        }

        this.isActive = false;

        // 發布商品下架事件
        collectEvent(
                new solid.humank.genaidemo.domain.product.model.events.ProductDiscontinuedEvent(
                        this.id, reason));
    }

    /** 重新上架商品 */
    public void activate() {
        if (this.isActive) {
            throw new IllegalStateException("商品已經上架");
        }

        this.isActive = true;

        // 發布商品重新上架事件
        collectEvent(
                solid.humank.genaidemo.domain.product.model.events.ProductActivatedEvent.create(
                        this.id));
    }

    /** 更新商品描述 */
    public void updateDescription(ProductDescription newDescription) {
        if (newDescription == null) {
            throw new IllegalArgumentException("商品描述不能為空");
        }

        ProductDescription oldDescription = this.description;
        this.description = newDescription;

        // 發布商品描述更新事件
        collectEvent(ProductDescriptionUpdatedEvent.create(this.id, oldDescription, newDescription));
    }

    /** 檢查商品是否可以購買 */
    public boolean canBePurchased() {
        return isActive && inStock;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Product product = (Product) obj;
        return id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
