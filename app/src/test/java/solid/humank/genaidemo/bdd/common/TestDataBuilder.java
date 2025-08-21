package solid.humank.genaidemo.bdd.common;

import java.math.BigDecimal;
import java.util.UUID;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.aggregate.Product;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductCategory;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductDescription;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductName;
import solid.humank.genaidemo.domain.product.model.valueobject.StockQuantity;
import solid.humank.genaidemo.domain.promotion.model.valueobject.AddOnPurchaseRule;

/** 測試數據建構器 - 用於創建測試實體 */
public class TestDataBuilder {

    private static TestDataBuilder instance;

    private TestDataBuilder() {
        // 私有構造函數
    }

    public static TestDataBuilder getInstance() {
        if (instance == null) {
            instance = new TestDataBuilder();
        }
        return instance;
    }

    /** 創建測試產品 */
    public Product createTestProduct(
            String productId,
            String productName,
            String category,
            BigDecimal price,
            Integer stock) {
        return new Product(
                new ProductId(productId),
                new ProductName(productName),
                new ProductDescription(productName + " description"),
                Money.of(price),
                new ProductCategory(category, category + " category"),
                new StockQuantity(stock));
    }

    /** 創建加購規則 */
    public AddOnPurchaseRule createAddOnRule(
            String mainProductId,
            String addOnProductId,
            BigDecimal addOnPrice,
            BigDecimal originalPrice,
            Integer limitQuantity) {
        return new AddOnPurchaseRule(
                new ProductId(mainProductId),
                new ProductId(addOnProductId),
                Money.of(addOnPrice),
                Money.of(originalPrice));
    }

    /** 創建購物車項目 */
    public CartItem createCartItem(
            String productId, String productName, BigDecimal price, int quantity) {
        return new CartItem(productId, productName, price, quantity);
    }

    /** 創建加購購物車項目 */
    public CartItem createAddOnCartItem(
            String productId,
            String productName,
            BigDecimal addOnPrice,
            BigDecimal originalPrice,
            int quantity) {
        return new CartItem(productId, productName, addOnPrice, originalPrice, quantity, true);
    }

    /** 創建加購選項 */
    public AddOnOption createAddOnOption(
            String productId,
            String productName,
            BigDecimal originalPrice,
            BigDecimal addOnPrice,
            int limitQuantity) {
        return new AddOnOption(productId, productName, originalPrice, addOnPrice, limitQuantity);
    }

    /** 生成隨機產品 ID */
    public String generateProductId() {
        return "PROD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /** 從數據表格行創建產品 */
    public Product createProductFromTableRow(
            String productId, String productName, String category, String price, String stock) {
        return createTestProduct(
                productId, productName, category, new BigDecimal(price), Integer.parseInt(stock));
    }

    /** 從數據表格行創建加購規則 */
    public AddOnPurchaseRule createAddOnRuleFromTableRow(
            String mainProduct,
            String addOnProduct,
            String addOnPrice,
            String originalPrice,
            String limitQty) {
        return createAddOnRule(
                mainProduct,
                addOnProduct,
                new BigDecimal(addOnPrice),
                new BigDecimal(originalPrice),
                Integer.parseInt(limitQty));
    }

    /** 創建測試客戶 */
    public void createTestCustomer(String customerId, String customerName, boolean isVip) {
        TestContext context = TestContext.getInstance();
        context.setCustomerId(customerId);
        context.setCustomerName(customerName);
        context.setVipMember(isVip);
        if (isVip) {
            context.setMemberDiscount(new BigDecimal("0.05")); // 5% VIP 折扣
        }
    }
}
