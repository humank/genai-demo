package solid.humank.genaidemo.testutils.fixtures;

import java.math.BigDecimal;
import solid.humank.genaidemo.application.order.dto.AddOrderItemCommand;
import solid.humank.genaidemo.application.order.dto.CreateOrderCommand;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.product.model.aggregate.Product;
import solid.humank.genaidemo.testutils.builders.CustomerTestDataBuilder;
import solid.humank.genaidemo.testutils.builders.OrderTestDataBuilder;
import solid.humank.genaidemo.testutils.builders.ProductTestDataBuilder;

/** 測試固定資料類別 提供常用的測試資料和工廠方法 */
public class TestFixtures {

    // 常用測試常數
    public static final String DEFAULT_CUSTOMER_ID = "test-customer-123";
    public static final String DEFAULT_CUSTOMER_NAME = "測試客戶";
    public static final String DEFAULT_CUSTOMER_EMAIL = "test@example.com";
    public static final String DEFAULT_SHIPPING_ADDRESS = "台北市信義區測試地址123號";

    public static final String DEFAULT_PRODUCT_ID = "product-123";
    public static final String DEFAULT_PRODUCT_NAME = "iPhone 15";
    public static final BigDecimal DEFAULT_PRODUCT_PRICE = new BigDecimal("35000");
    public static final String DEFAULT_PRODUCT_CATEGORY = "智慧型手機";

    public static final String EXPENSIVE_PRODUCT_NAME = "超貴產品";
    public static final BigDecimal EXPENSIVE_PRODUCT_PRICE = new BigDecimal("1000000");

    public static final int DEFAULT_QUANTITY = 1;
    public static final int DEFAULT_REWARD_POINTS = 1000;

    // 測試資料工廠方法

    /** 創建預設的CreateOrderCommand */
    public static CreateOrderCommand createOrderCommand() {
        return new CreateOrderCommand(DEFAULT_CUSTOMER_ID, DEFAULT_SHIPPING_ADDRESS);
    }

    /** 創建自定義的CreateOrderCommand */
    public static CreateOrderCommand createOrderCommand(String customerId, String shippingAddress) {
        return new CreateOrderCommand(customerId, shippingAddress);
    }

    /** 創建預設的AddOrderItemCommand */
    public static AddOrderItemCommand addOrderItemCommand(String orderId) {
        return AddOrderItemCommand.of(
                orderId,
                DEFAULT_PRODUCT_ID,
                DEFAULT_PRODUCT_NAME,
                DEFAULT_QUANTITY,
                DEFAULT_PRODUCT_PRICE);
    }

    /** 創建自定義的AddOrderItemCommand */
    public static AddOrderItemCommand addOrderItemCommand(
            String orderId, String productId, String productName, int quantity, BigDecimal price) {
        return AddOrderItemCommand.of(orderId, productId, productName, quantity, price);
    }

    /** 創建預設客戶 */
    public static Customer defaultCustomer() {
        return CustomerTestDataBuilder.aCustomer()
                .withId(DEFAULT_CUSTOMER_ID)
                .withName(DEFAULT_CUSTOMER_NAME)
                .withEmail(DEFAULT_CUSTOMER_EMAIL)
                .withRewardPoints(DEFAULT_REWARD_POINTS)
                .build();
    }

    /** 創建新會員客戶 */
    public static Customer newMemberCustomer() {
        return CustomerTestDataBuilder.aCustomer()
                .withId(DEFAULT_CUSTOMER_ID)
                .asNewMember()
                .build();
    }

    /** 創建生日月份客戶 */
    public static Customer birthdayCustomer() {
        return CustomerTestDataBuilder.aCustomer()
                .withId(DEFAULT_CUSTOMER_ID)
                .withBirthdayInCurrentMonth()
                .build();
    }

    /** 創建VIP客戶 */
    public static Customer vipCustomer() {
        return CustomerTestDataBuilder.aCustomer().withId(DEFAULT_CUSTOMER_ID).buildVipCustomer();
    }

    /** 創建預設訂單 */
    public static Order defaultOrder() {
        return OrderTestDataBuilder.anOrder()
                .withCustomerId(DEFAULT_CUSTOMER_ID)
                .withShippingAddress(DEFAULT_SHIPPING_ADDRESS)
                .build();
    }

    /** 創建包含項目的訂單 */
    public static Order orderWithItems() {
        return OrderTestDataBuilder.anOrder()
                .withCustomerId(DEFAULT_CUSTOMER_ID)
                .withShippingAddress(DEFAULT_SHIPPING_ADDRESS)
                .withItem(
                        DEFAULT_PRODUCT_ID,
                        DEFAULT_PRODUCT_NAME,
                        DEFAULT_QUANTITY,
                        DEFAULT_PRODUCT_PRICE)
                .build();
    }

    /** 創建多項目訂單 */
    public static Order orderWithMultipleItems() {
        return OrderTestDataBuilder.anOrder()
                .withCustomerId(DEFAULT_CUSTOMER_ID)
                .withShippingAddress(DEFAULT_SHIPPING_ADDRESS)
                .withItem("product-1", "iPhone 15", 1, new BigDecimal("35000"))
                .withItem("product-2", "AirPods", 2, new BigDecimal("8000"))
                .withItem("product-3", "MacBook Pro", 1, new BigDecimal("58000"))
                .build();
    }

    /** 創建預設產品 */
    public static Product defaultProduct() {
        return ProductTestDataBuilder.aProduct()
                .withId(DEFAULT_PRODUCT_ID)
                .withName(DEFAULT_PRODUCT_NAME)
                .withPrice(DEFAULT_PRODUCT_PRICE)
                .withCategory(DEFAULT_PRODUCT_CATEGORY)
                .build();
    }

    /** 創建iPhone產品 */
    public static Product iPhoneProduct() {
        return ProductTestDataBuilder.aProduct().buildIPhone();
    }

    /** 創建MacBook產品 */
    public static Product macBookProduct() {
        return ProductTestDataBuilder.aProduct().buildMacBook();
    }

    /** 創建AirPods產品 */
    public static Product airPodsProduct() {
        return ProductTestDataBuilder.aProduct().buildAirPods();
    }

    /** 創建超貴產品（用於測試異常情況） */
    public static Product expensiveProduct() {
        return ProductTestDataBuilder.aProduct()
                .withName(EXPENSIVE_PRODUCT_NAME)
                .withPrice(EXPENSIVE_PRODUCT_PRICE)
                .build();
    }

    /** 創建缺貨產品 */
    public static Product outOfStockProduct() {
        return ProductTestDataBuilder.aProduct().withName("缺貨產品").asOutOfStock().build();
    }

    /** 創建預設金額 */
    public static Money defaultMoney() {
        return Money.of(DEFAULT_PRODUCT_PRICE);
    }

    /** 創建台幣金額 */
    public static Money twd(int amount) {
        return Money.twd(amount);
    }

    /** 創建台幣金額 */
    public static Money twd(BigDecimal amount) {
        return Money.of(amount);
    }
}
