# 測試程式碼品質改善設計文件

## 概述

本設計文件描述如何系統性地改善現有測試程式碼的品質，包括重構BDD步驟定義、改善整合測試結構、建立測試輔助工具，以及實施測試最佳實踐。

## 架構

### 測試輔助工具架構

```
app/src/test/java/solid/humank/genaidemo/testutils/
├── builders/                    # 測試資料建構器
│   ├── OrderTestDataBuilder.java
│   ├── CustomerTestDataBuilder.java
│   └── ProductTestDataBuilder.java
├── handlers/                    # 場景處理器
│   ├── TestScenarioHandler.java
│   ├── TestExceptionHandler.java
│   └── BddStepHandler.java
├── fixtures/                    # 測試固定資料
│   ├── TestFixtures.java
│   └── TestConstants.java
├── matchers/                    # 自定義匹配器
│   ├── OrderMatchers.java
│   └── MoneyMatchers.java
├── annotations/                 # 測試標籤註解
│   ├── UnitTest.java
│   ├── IntegrationTest.java
│   └── SlowTest.java
└── base/                       # 測試基礎類別
    ├── BaseIntegrationTest.java
    └── BaseBddTest.java
```

## 組件和介面

### 1. 測試資料建構器 (Test Data Builders)

#### OrderTestDataBuilder
```java
public class OrderTestDataBuilder {
    public static OrderTestDataBuilder anOrder();
    public OrderTestDataBuilder withCustomerId(String customerId);
    public OrderTestDataBuilder withShippingAddress(String address);
    public OrderTestDataBuilder withItem(String productId, String name, int quantity, BigDecimal price);
    public Order build();
    public CreateOrderCommand buildCreateCommand();
}
```

#### CustomerTestDataBuilder
```java
public class CustomerTestDataBuilder {
    public static CustomerTestDataBuilder aCustomer();
    public CustomerTestDataBuilder withId(String id);
    public CustomerTestDataBuilder asNewMember();
    public CustomerTestDataBuilder withBirthdayInCurrentMonth();
    public Customer build();
}
```

### 2. 場景處理器 (Scenario Handlers)

#### TestScenarioHandler
```java
public class TestScenarioHandler {
    public void handleAddItemScenario(Order order, String productName, int quantity, int price, Consumer<Exception> exceptionSetter);
    public void handleDiscountScenario(Customer customer, Order order, DiscountService discountService);
    public void handlePaymentScenario(String paymentMethod, Map<String, Object> paymentDetails, Money orderTotal);
}
```

#### TestExceptionHandler
```java
public class TestExceptionHandler {
    private Exception capturedException;
    
    public void captureException(Exception exception);
    public void expectException(String expectedMessage);
    public boolean hasException();
    public Exception getCapturedException();
    public void reset();
}
```

### 3. 測試固定資料 (Test Fixtures)

#### TestFixtures
```java
public class TestFixtures {
    // 常用測試資料
    public static final String DEFAULT_CUSTOMER_ID = "test-customer-123";
    public static final String DEFAULT_SHIPPING_ADDRESS = "台北市信義區";
    public static final BigDecimal DEFAULT_PRODUCT_PRICE = new BigDecimal("1000");
    
    // 測試資料工廠方法
    public static CreateOrderCommand createOrderCommand();
    public static AddOrderItemCommand addOrderItemCommand(String orderId);
    public static Customer defaultCustomer();
    public static Order defaultOrder();
}
```

### 4. 自定義匹配器 (Custom Matchers)

#### OrderMatchers
```java
public class OrderMatchers {
    public static Matcher<Order> hasStatus(OrderStatus status);
    public static Matcher<Order> hasTotalAmount(BigDecimal amount);
    public static Matcher<Order> hasItemCount(int count);
    public static Matcher<Order> hasCustomerId(String customerId);
}
```

### 5. 測試標籤註解 (Test Tag Annotations)

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Tag("unit")
public @interface UnitTest {}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Tag("integration")
public @interface IntegrationTest {}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Tag("slow")
public @interface SlowTest {}
```

## 資料模型

### 測試上下文 (Test Context)

```java
public class TestContext {
    private final Map<String, Object> context = new HashMap<>();
    private final TestExceptionHandler exceptionHandler = new TestExceptionHandler();
    
    public <T> void put(String key, T value);
    public <T> T get(String key, Class<T> type);
    public TestExceptionHandler getExceptionHandler();
    public void reset();
}
```

## 錯誤處理

### 1. 統一異常處理策略

- **測試異常捕獲**：使用TestExceptionHandler統一處理測試中的異常
- **斷言失敗訊息**：提供清晰的錯誤訊息，包含期望值和實際值
- **測試資料驗證**：在測試資料建構時進行基本驗證

### 2. 錯誤訊息改善

```java
public class EnhancedAssertions {
    public static void assertOrderStatus(Order order, OrderStatus expectedStatus) {
        assertThat(order.getStatus())
            .as("Order %s should have status %s but was %s", 
                order.getId(), expectedStatus, order.getStatus())
            .isEqualTo(expectedStatus);
    }
}
```

## 測試策略

### 1. BDD步驟定義重構策略

#### 重構前 (問題代碼)
```java
@When("添加產品 {string} 到訂單，數量為 {int}，單價為 {int}")
public void addItemToOrder(String productName, int quantity, int price) {
    try {
        if (productName.equals("超貴產品") && price >= 1000000) {
            thrownException = new IllegalArgumentException("訂單總金額超過允許的最大值");
            return;
        }
        order.addItem("product-" + productName.hashCode(), productName, quantity, Money.twd(price));
    } catch (Exception e) {
        thrownException = e;
    }
}
```

#### 重構後 (改善代碼)
```java
@When("添加產品 {string} 到訂單，數量為 {int}，單價為 {int}")
public void addItemToOrder(String productName, int quantity, int price) {
    scenarioHandler.handleAddItemScenario(order, productName, quantity, price, 
        testContext.getExceptionHandler()::captureException);
}
```

### 2. 整合測試3A結構改善

#### 重構前 (違反3A原則)
```java
@Test
public void testOrderCreationEventFlow() {
    // Arrange + Act 混合
    CreateOrderCommand createCommand = new CreateOrderCommand("customer-flow-test", "台北市信義區");
    OrderResponse orderResponse = orderApplicationService.createOrder(createCommand);
    
    // Assert
    verify(orderEventHandler, timeout(1000)).handleOrderCreated(any(OrderCreatedEvent.class));
    
    // Act again
    AddOrderItemCommand addItemCommand = AddOrderItemCommand.of(/*...*/);
    orderApplicationService.addOrderItem(addItemCommand);
    
    // Assert again
    verify(orderEventHandler, timeout(1000)).handleOrderItemAdded(any(OrderItemAddedEvent.class));
}
```

#### 重構後 (遵循3A原則)
```java
@Test
@DisplayName("應該在訂單創建時發布OrderCreatedEvent")
public void shouldPublishOrderCreatedEventWhenOrderIsCreated() {
    // Arrange
    CreateOrderCommand createCommand = OrderTestDataBuilder.anOrder()
        .withCustomerId("customer-flow-test")
        .withShippingAddress("台北市信義區")
        .buildCreateCommand();
    
    // Act
    OrderResponse orderResponse = orderApplicationService.createOrder(createCommand);
    
    // Assert
    verify(orderEventHandler, timeout(1000)).handleOrderCreated(any(OrderCreatedEvent.class));
}

@Test
@DisplayName("應該在添加訂單項目時發布OrderItemAddedEvent")
public void shouldPublishOrderItemAddedEventWhenItemIsAdded() {
    // Arrange
    String orderId = createTestOrderAndGetId();
    AddOrderItemCommand addItemCommand = OrderTestDataBuilder.anOrder()
        .withItem("product-123", "iPhone 15", 1, new BigDecimal("35000"))
        .buildAddItemCommand(orderId);
    
    // Act
    orderApplicationService.addOrderItem(addItemCommand);
    
    // Assert
    verify(orderEventHandler, timeout(1000)).handleOrderItemAdded(any(OrderItemAddedEvent.class));
}
```

### 3. 測試資料管理策略

#### Object Mother模式
```java
public class OrderMother {
    public static Order simpleOrder() {
        return OrderTestDataBuilder.anOrder()
            .withCustomerId(TestFixtures.DEFAULT_CUSTOMER_ID)
            .withShippingAddress(TestFixtures.DEFAULT_SHIPPING_ADDRESS)
            .build();
    }
    
    public static Order orderWithItems() {
        return OrderTestDataBuilder.anOrder()
            .withCustomerId(TestFixtures.DEFAULT_CUSTOMER_ID)
            .withItem("product-1", "iPhone 15", 1, new BigDecimal("35000"))
            .withItem("product-2", "AirPods", 2, new BigDecimal("8000"))
            .build();
    }
}
```

## 實施計劃

### 階段1：建立測試輔助工具基礎設施
1. 創建testutils包結構
2. 實施測試資料建構器
3. 建立測試固定資料類別
4. 創建測試標籤註解

### 階段2：重構BDD步驟定義
1. 創建場景處理器
2. 重構包含條件邏輯的步驟定義
3. 統一異常處理機制
4. 改善測試可讀性

### 階段3：改善整合測試結構
1. 識別違反3A原則的測試
2. 拆分複雜的測試方法
3. 使用測試輔助工具簡化測試設置
4. 改善測試命名和文檔

### 階段4：程式碼品質清理
1. 移除未使用的import和變數
2. 更新過時的註解
3. 統一程式碼格式
4. 添加測試標籤

### 階段5：效能和可維護性優化
1. 識別慢速測試並優化
2. 改善測試獨立性
3. 強化錯誤訊息
4. 建立測試執行策略

## 品質保證

### 1. 程式碼審查檢查清單
- [ ] 每個測試方法遵循3A原則
- [ ] 沒有條件邏輯在測試中
- [ ] 使用描述性的測試名稱
- [ ] 適當的測試標籤
- [ ] 清晰的錯誤訊息

### 2. 自動化檢查
- 靜態程式碼分析規則
- 測試覆蓋率要求
- 測試執行時間監控
- 架構測試驗證

### 3. 測試指標
- 測試執行時間
- 測試穩定性（成功率）
- 程式碼覆蓋率
- 測試可讀性評分