package solid.humank.genaidemo.testutils.builders;

import java.math.BigDecimal;
import java.util.UUID;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.aggregate.Product;

/** 產品測試資料建構器 使用Builder模式來簡化測試中的產品資料創建 */
public class ProductTestDataBuilder {

    private String productId = "product-" + UUID.randomUUID().toString().substring(0, 8);
    private String name = "測試產品";
    private String description = "這是一個測試產品";
    private BigDecimal price = new BigDecimal("1000");
    private String category = "電子產品";

    @SuppressWarnings("unused") // 保留供未來使用，目前通過 stockQuantity > 0 來判斷可用性
    private boolean available = true;

    private int stockQuantity = 100;
    private String imageUrl = "https://placehold.co/600x400";

    /** 創建新的產品建構器 */
    public static ProductTestDataBuilder aProduct() {
        return new ProductTestDataBuilder();
    }

    /** 設置產品ID */
    public ProductTestDataBuilder withId(String productId) {
        this.productId = productId;
        return this;
    }

    /** 設置產品名稱 */
    public ProductTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    /** 設置產品描述 */
    public ProductTestDataBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    /** 設置產品價格 */
    public ProductTestDataBuilder withPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    /** 設置產品價格（整數） */
    public ProductTestDataBuilder withPrice(int price) {
        this.price = new BigDecimal(price);
        return this;
    }

    /** 設置產品類別 */
    public ProductTestDataBuilder withCategory(String category) {
        this.category = category;
        return this;
    }

    /** 設置產品可用性 */
    public ProductTestDataBuilder withAvailability(boolean available) {
        this.available = available;
        return this;
    }

    /** 設置為不可用產品 */
    public ProductTestDataBuilder asUnavailable() {
        this.available = false;
        return this;
    }

    /** 設置庫存數量 */
    public ProductTestDataBuilder withStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
        return this;
    }

    /** 設置為缺貨產品 */
    public ProductTestDataBuilder asOutOfStock() {
        this.stockQuantity = 0;
        return this;
    }

    /** 設置為高價產品 */
    public ProductTestDataBuilder asExpensive() {
        this.price = new BigDecimal("50000");
        return this;
    }

    /** 設置為超高價產品（用於測試異常情況） */
    public ProductTestDataBuilder asSuperExpensive() {
        this.name = "超貴產品";
        this.price = new BigDecimal("1000000");
        return this;
    }

    /** 建構Product領域物件 */
    public Product build() {
        solid.humank.genaidemo.domain.product.model.valueobject.ProductId productIdVO = new solid.humank.genaidemo.domain.product.model.valueobject.ProductId(
                productId);
        // 創建所需的值對象
        solid.humank.genaidemo.domain.product.model.valueobject.ProductName productName = new solid.humank.genaidemo.domain.product.model.valueobject.ProductName(
                name);
        solid.humank.genaidemo.domain.product.model.valueobject.ProductDescription productDescription = new solid.humank.genaidemo.domain.product.model.valueobject.ProductDescription(
                description);
        solid.humank.genaidemo.domain.product.model.valueobject.ProductCategory categoryVO = new solid.humank.genaidemo.domain.product.model.valueobject.ProductCategory(
                category, category);
        solid.humank.genaidemo.domain.product.model.valueobject.StockQuantity stockQuantityVO = new solid.humank.genaidemo.domain.product.model.valueobject.StockQuantity(
                this.stockQuantity);

        Product product = new Product(
                productIdVO,
                productName,
                productDescription,
                Money.of(price),
                categoryVO,
                stockQuantityVO,
                this.imageUrl);
        // 注意：Product 類可能沒有 setter 方法，因為它遵循 DDD 原則
        // 如果需要設置這些值，應該通過構造函數或業務方法
        return product;
    }

    /** 建構iPhone產品 */
    public Product buildIPhone() {
        return this.withName("iPhone 15")
                .withDescription("Apple iPhone 15")
                .withPrice(35000)
                .withCategory("智慧型手機")
                .build();
    }

    /** 建構MacBook產品 */
    public Product buildMacBook() {
        return this.withName("MacBook Pro")
                .withDescription("Apple MacBook Pro 14吋")
                .withPrice(58000)
                .withCategory("筆記型電腦")
                .build();
    }

    /** 建構AirPods產品 */
    public Product buildAirPods() {
        return this.withName("AirPods")
                .withDescription("Apple AirPods Pro")
                .withPrice(8000)
                .withCategory("耳機")
                .build();
    }
}
