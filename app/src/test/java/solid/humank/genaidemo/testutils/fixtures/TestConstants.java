package solid.humank.genaidemo.testutils.fixtures;

import java.math.BigDecimal;

/**
 * 測試常數類別
 * 定義測試中使用的常數值
 */
public final class TestConstants {
    
    // 防止實例化
    private TestConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    // 客戶相關常數
    public static final class Customer {
        public static final String DEFAULT_ID = "test-customer-123";
        public static final String DEFAULT_NAME = "測試客戶";
        public static final String DEFAULT_EMAIL = "test@example.com";
        public static final String VIP_CUSTOMER_ID = "vip-customer-456";
        public static final String NEW_MEMBER_ID = "new-member-789";
        public static final int DEFAULT_REWARD_POINTS = 1000;
        public static final int HIGH_REWARD_POINTS = 5000;
        public static final int LOW_REWARD_POINTS = 100;
    }
    
    // 訂單相關常數
    public static final class Order {
        public static final String DEFAULT_SHIPPING_ADDRESS = "台北市信義區測試地址123號";
        public static final String ALTERNATIVE_ADDRESS = "台北市大安區測試地址456號";
        public static final String THIRD_ADDRESS = "台北市中正區測試地址789號";
        public static final int DEFAULT_QUANTITY = 1;
        public static final int MULTIPLE_QUANTITY = 2;
    }
    
    // 產品相關常數
    public static final class Product {
        public static final String DEFAULT_ID = "product-123";
        public static final String DEFAULT_NAME = "iPhone 15";
        public static final String EXPENSIVE_PRODUCT_NAME = "超貴產品";
        public static final String OUT_OF_STOCK_PRODUCT = "缺貨產品";
        
        public static final BigDecimal DEFAULT_PRICE = new BigDecimal("35000");
        public static final BigDecimal EXPENSIVE_PRICE = new BigDecimal("1000000");
        public static final BigDecimal CHEAP_PRICE = new BigDecimal("100");
        
        public static final String ELECTRONICS_CATEGORY = "電子產品";
        public static final String SMARTPHONE_CATEGORY = "智慧型手機";
        public static final String LAPTOP_CATEGORY = "筆記型電腦";
        public static final String HEADPHONE_CATEGORY = "耳機";
        
        public static final int DEFAULT_STOCK = 100;
        public static final int LOW_STOCK = 5;
        public static final int OUT_OF_STOCK = 0;
    }
    
    // 金額相關常數
    public static final class Money {
        public static final BigDecimal ZERO = BigDecimal.ZERO;
        public static final BigDecimal SMALL_AMOUNT = new BigDecimal("100");
        public static final BigDecimal MEDIUM_AMOUNT = new BigDecimal("1000");
        public static final BigDecimal LARGE_AMOUNT = new BigDecimal("10000");
        public static final BigDecimal HUGE_AMOUNT = new BigDecimal("100000");
        
        public static final BigDecimal DISCOUNT_10_PERCENT = new BigDecimal("0.1");
        public static final BigDecimal DISCOUNT_15_PERCENT = new BigDecimal("0.15");
        public static final BigDecimal DISCOUNT_20_PERCENT = new BigDecimal("0.2");
    }
    
    // 折扣相關常數
    public static final class Discount {
        public static final int NEW_MEMBER_PERCENTAGE = 15;
        public static final int BIRTHDAY_PERCENTAGE = 10;
        public static final int VIP_PERCENTAGE = 20;
        
        public static final BigDecimal FIXED_DISCOUNT_SMALL = new BigDecimal("100");
        public static final BigDecimal FIXED_DISCOUNT_MEDIUM = new BigDecimal("500");
        public static final BigDecimal FIXED_DISCOUNT_LARGE = new BigDecimal("1000");
    }
    
    // 支付相關常數
    public static final class Payment {
        public static final String CREDIT_CARD = "Credit Card";
        public static final String DEBIT_CARD = "Debit Card";
        public static final String DIGITAL_WALLET = "Digital Wallet";
        public static final String BANK_TRANSFER = "Bank Transfer";
        
        public static final int DEFAULT_CASHBACK_PERCENTAGE = 2;
        public static final int HIGH_CASHBACK_PERCENTAGE = 5;
        public static final int MAX_CASHBACK_AMOUNT = 500;
        
        public static final BigDecimal MIN_ORDER_FOR_DISCOUNT = new BigDecimal("1000");
        public static final BigDecimal INSTANT_DISCOUNT_AMOUNT = new BigDecimal("100");
    }
    
    // 庫存相關常數
    public static final class Inventory {
        public static final int DEFAULT_AVAILABLE_QUANTITY = 100;
        public static final int LOW_QUANTITY = 5;
        public static final int RESERVE_QUANTITY = 2;
        public static final int THRESHOLD_QUANTITY = 10;
    }
    
    // 配送相關常數
    public static final class Delivery {
        public static final String DEFAULT_DRIVER_ID = "driver-123";
        public static final String DEFAULT_DRIVER_NAME = "張三";
        public static final String DEFAULT_DRIVER_PHONE = "0912345678";
        public static final int ESTIMATED_DELIVERY_HOURS = 2;
    }
    
    // 通知相關常數
    public static final class Notification {
        public static final String DEFAULT_RECIPIENT = "test@example.com";
        public static final String DEFAULT_PHONE = "0912345678";
        public static final String TEST_FAILURE_REASON = "測試失敗原因";
    }
    
    // 錯誤訊息常數
    public static final class ErrorMessages {
        public static final String ORDER_NO_ITEMS = "Cannot submit an order with no items";
        public static final String INSUFFICIENT_INVENTORY = "Insufficient inventory";
        public static final String INSUFFICIENT_REWARD_POINTS = "Insufficient reward points";
        public static final String ORDER_AMOUNT_EXCEEDED = "訂單總金額超過允許的最大值";
        public static final String EXPECTED_EXCEPTION_NOT_THROWN = "Expected exception was not thrown";
    }
    
    // 測試標籤常數
    public static final class Tags {
        public static final String UNIT = "unit";
        public static final String INTEGRATION = "integration";
        public static final String SLOW = "slow";
        public static final String BDD = "bdd";
        public static final String ARCHITECTURE = "architecture";
    }
}