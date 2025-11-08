# Unit Test 實踐指南

> **最後更新**: 2025-11-05

## 概述

本文件提供通用的 Unit Test（單元測試）實踐指南，適用於 Java 專案開發。無論你使用什麼框架或架構，這些原則和實踐都能幫助你寫出高品質、可維護的單元測試。

## 什麼是 Unit Test？

**Unit Test（單元測試）** 是針對程式碼中最小可測試單元（通常是一個方法或類別）進行的測試。它的目的是驗證每個單元在隔離狀態下是否按預期工作。

### 核心特徵

- **快速執行**：每個測試應在毫秒級完成（< 50ms）
- **獨立性**：測試之間互不依賴，可以任意順序執行
- **可重複性**：每次執行結果一致
- **隔離性**：不依賴外部資源（資料庫、網路、檔案系統）
- **自動化**：可以自動執行和驗證

## 為什麼要寫 Unit Test？

### 開發階段的好處

1. **快速回饋**：立即知道程式碼是否正確
2. **設計改善**：促使你寫出更模組化、可測試的程式碼
3. **重構信心**：有測試保護，可以放心重構
4. **文件作用**：測試本身就是最好的使用範例

### 維護階段的好處

1. **防止退化**：確保新功能不會破壞既有功能
2. **降低維護成本**：問題早期發現，修復成本低
3. **提升程式碼品質**：有測試的程式碼通常品質更高
4. **團隊協作**：新成員可以透過測試理解程式碼

## 基本結構

### AAA Pattern（Arrange-Act-Assert）

這是最常用的測試結構模式：

```java
@Test
void should_calculate_total_price_correctly() {
    // Arrange（準備）：設定測試資料和環境
    ShoppingCart cart = new ShoppingCart();
    cart.addItem(new Item("商品A", 100));
    cart.addItem(new Item("商品B", 200));
    
    // Act（執行）：執行要測試的行為
    int totalPrice = cart.calculateTotal();
    
    // Assert（驗證）：檢查結果是否符合預期
    assertEquals(300, totalPrice);
}
```

### Given-When-Then Pattern

另一種常見的結構模式，語意更清楚：

```java
@Test
void should_apply_discount_when_customer_is_vip() {
    // Given（給定）：設定前置條件
    Customer vipCustomer = new Customer("John", CustomerType.VIP);
    Order order = new Order(1000);
    
    // When（當）：執行測試行為
    int finalPrice = order.calculateFinalPrice(vipCustomer);
    
    // Then（則）：驗證結果
    assertEquals(900, finalPrice); // VIP 享 9 折優惠
}
```

## 測試命名規範

### 推薦格式

使用描述性的命名，讓人一看就知道測試的目的：

```java
// 格式：should_預期行為_when_條件
@Test
void should_return_empty_list_when_no_items_exist() { }

@Test
void should_throw_exception_when_email_is_invalid() { }

@Test
void should_calculate_discount_when_customer_is_premium() { }
```

### 避免的命名方式

```java
// ❌ 不好：太簡短，看不出測試什麼
@Test
void test1() { }

@Test
void testCalculate() { }

// ❌ 不好：使用縮寫，不易理解
@Test
void testCalcTotPrc() { }
```

## 測試類別組織

### 基本結構

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("購物車測試")
class ShoppingCartTest {
    
    private ShoppingCart cart;
    
    @BeforeEach
    void setUp() {
        // 每個測試前都會執行，用來初始化共用的測試資料
        cart = new ShoppingCart();
    }
    
    @Test
    @DisplayName("應該正確計算總價")
    void should_calculate_total_price_correctly() {
        // 測試實作
    }
    
    @Test
    @DisplayName("應該在商品數量為負數時拋出例外")
    void should_throw_exception_when_quantity_is_negative() {
        // 測試實作
    }
}
```

### 測試類別命名

```java
// 格式：{被測試的類別名稱}Test
public class OrderServiceTest { }
public class CustomerRepositoryTest { }
public class PriceCalculatorTest { }
```

## 斷言（Assertions）

### 基本斷言

```java
import static org.junit.jupiter.api.Assertions.*;

@Test
void basic_assertions_examples() {
    // 相等性斷言
    assertEquals(expected, actual);
    assertEquals(expected, actual, "錯誤訊息");
    
    // 布林值斷言
    assertTrue(condition);
    assertFalse(condition);
    
    // Null 檢查
    assertNull(object);
    assertNotNull(object);
    
    // 相同物件檢查
    assertSame(expected, actual);
    assertNotSame(expected, actual);
    
    // 陣列相等
    assertArrayEquals(expectedArray, actualArray);
}
```

### 例外斷言

```java
@Test
void should_throw_exception_when_input_is_invalid() {
    // 驗證是否拋出特定例外
    assertThrows(IllegalArgumentException.class, () -> {
        calculator.divide(10, 0);
    });
    
    // 驗證例外訊息
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
        validator.validate(null);
    });
    assertEquals("輸入不可為 null", exception.getMessage());
}
```

### AssertJ 流暢斷言（推薦）

```java
import static org.assertj.core.api.Assertions.*;

@Test
void assertj_examples() {
    // 更易讀的斷言
    assertThat(actual).isEqualTo(expected);
    assertThat(list).hasSize(3);
    assertThat(string).startsWith("Hello");
    assertThat(number).isGreaterThan(10);
    
    // 集合斷言
    assertThat(list)
        .hasSize(3)
        .contains("item1", "item2")
        .doesNotContain("item4");
    
    // 例外斷言
    assertThatThrownBy(() -> service.process(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("不可為 null");
}
```

## Mock 物件使用

### 什麼時候需要 Mock？

當被測試的程式碼依賴以下情況時，應該使用 Mock：

- **外部服務**：API 呼叫、第三方服務
- **資料庫**：Repository、DAO
- **檔案系統**：檔案讀寫操作
- **時間相關**：當前時間、日期
- **複雜依賴**：已經在其他地方測試過的複雜物件

### 使用 Mockito

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    void should_send_confirmation_email_when_order_is_created() {
        // Given
        Order order = new Order("ORD-001", 1000);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        
        // When
        orderService.createOrder(order);
        
        // Then
        verify(emailService).sendConfirmationEmail(order.getCustomerEmail());
        verify(orderRepository).save(order);
    }
}
```

### Mock 的常用操作

```java
// 設定回傳值
when(mockObject.method()).thenReturn(value);
when(mockObject.method(anyString())).thenReturn(value);

// 設定拋出例外
when(mockObject.method()).thenThrow(new RuntimeException());

// 驗證方法被呼叫
verify(mockObject).method();
verify(mockObject, times(2)).method();
verify(mockObject, never()).method();

// 驗證參數
verify(mockObject).method(eq("expected"));
verify(mockObject).method(argThat(arg -> arg.length() > 5));
```

### 什麼時候不該 Mock？

```java
// ❌ 不要 Mock 值物件（Value Objects）
// 值物件應該直接建立
Money price = new Money(100);  // 正確
Money price = mock(Money.class);  // 錯誤

// ❌ 不要 Mock 被測試的類別本身
OrderService service = mock(OrderService.class);  // 錯誤

// ❌ 不要 Mock 簡單的資料結構
List<String> list = new ArrayList<>();  // 正確
List<String> list = mock(List.class);  // 錯誤
```

## 測試資料準備

### Test Data Builder Pattern

建立可讀性高、易維護的測試資料：

```java
public class OrderTestDataBuilder {
    private String orderId = "ORD-001";
    private String customerId = "CUST-001";
    private int amount = 1000;
    private OrderStatus status = OrderStatus.PENDING;
    
    public static OrderTestDataBuilder anOrder() {
        return new OrderTestDataBuilder();
    }
    
    public OrderTestDataBuilder withOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }
    
    public OrderTestDataBuilder withAmount(int amount) {
        this.amount = amount;
        return this;
    }
    
    public OrderTestDataBuilder withStatus(OrderStatus status) {
        this.status = status;
        return this;
    }
    
    public Order build() {
        return new Order(orderId, customerId, amount, status);
    }
}

// 使用方式
@Test
void test_example() {
    Order order = anOrder()
        .withOrderId("ORD-123")
        .withAmount(5000)
        .withStatus(OrderStatus.CONFIRMED)
        .build();
    
    // 使用 order 進行測試
}
```

### Test Fixture

共用的測試資料設定：

```java
public class TestFixtures {
    
    public static Customer createStandardCustomer() {
        return new Customer("CUST-001", "張三", "zhang@example.com");
    }
    
    public static Customer createVipCustomer() {
        return new Customer("CUST-002", "李四", "li@example.com", CustomerType.VIP);
    }
    
    public static Order createOrderWithItems(int itemCount) {
        Order order = new Order();
        for (int i = 0; i < itemCount; i++) {
            order.addItem(new OrderItem("ITEM-" + i, 100));
        }
        return order;
    }
}
```

## 測試覆蓋率

### 目標設定

- **整體覆蓋率**：> 80%
- **核心業務邏輯**：> 90%
- **工具類別**：> 85%
- **配置類別**：可以較低（> 50%）

### 覆蓋率不是唯一指標

```java
// ❌ 高覆蓋率但測試品質差
@Test
void test_method() {
    service.complexMethod();  // 只執行，沒有驗證
}

// ✅ 有意義的測試
@Test
void should_return_correct_result_when_input_is_valid() {
    // Given
    Input input = new Input("valid");
    
    // When
    Result result = service.complexMethod(input);
    
    // Then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getValue()).isEqualTo("expected");
}
```

## 常見測試場景

### 1. 測試正常流程

```java
@Test
void should_create_user_successfully_when_data_is_valid() {
    // Given
    UserRequest request = new UserRequest("john@example.com", "password123");
    
    // When
    User user = userService.createUser(request);
    
    // Then
    assertThat(user).isNotNull();
    assertThat(user.getEmail()).isEqualTo("john@example.com");
    assertThat(user.isActive()).isTrue();
}
```

### 2. 測試邊界條件

```java
@Test
void should_handle_empty_list_correctly() {
    // Given
    List<Item> emptyList = Collections.emptyList();
    
    // When
    int total = calculator.calculateTotal(emptyList);
    
    // Then
    assertThat(total).isEqualTo(0);
}

@Test
void should_handle_maximum_value_correctly() {
    // Given
    int maxValue = Integer.MAX_VALUE;
    
    // When & Then
    assertThatThrownBy(() -> calculator.add(maxValue, 1))
        .isInstanceOf(ArithmeticException.class);
}
```

### 3. 測試例外情況

```java
@Test
void should_throw_exception_when_email_is_null() {
    // When & Then
    assertThatThrownBy(() -> validator.validateEmail(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Email 不可為 null");
}

@Test
void should_throw_exception_when_email_format_is_invalid() {
    // When & Then
    assertThatThrownBy(() -> validator.validateEmail("invalid-email"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("Email 格式不正確");
}
```

### 4. 測試集合操作

```java
@Test
void should_filter_active_users_correctly() {
    // Given
    List<User> users = Arrays.asList(
        new User("user1", true),
        new User("user2", false),
        new User("user3", true)
    );
    
    // When
    List<User> activeUsers = userService.filterActiveUsers(users);
    
    // Then
    assertThat(activeUsers)
        .hasSize(2)
        .extracting(User::getName)
        .containsExactly("user1", "user3");
}
```

### 5. 測試狀態變化

```java
@Test
void should_change_order_status_from_pending_to_confirmed() {
    // Given
    Order order = new Order();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    
    // When
    order.confirm();
    
    // Then
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    assertThat(order.getConfirmedAt()).isNotNull();
}
```

## 最佳實踐

### 1. 一個測試只驗證一件事

```java
// ❌ 不好：一個測試驗證太多事情
@Test
void test_user_creation() {
    User user = service.createUser(request);
    assertThat(user.getName()).isEqualTo("John");
    assertThat(user.getEmail()).isEqualTo("john@example.com");
    assertThat(user.isActive()).isTrue();
    assertThat(user.getCreatedAt()).isNotNull();
    // ... 更多驗證
}

// ✅ 好：分成多個測試，每個測試一個重點
@Test
void should_set_user_name_correctly() {
    User user = service.createUser(request);
    assertThat(user.getName()).isEqualTo("John");
}

@Test
void should_set_user_as_active_by_default() {
    User user = service.createUser(request);
    assertThat(user.isActive()).isTrue();
}
```

### 2. 測試應該獨立

```java
// ❌ 不好：測試之間有依賴
private static User sharedUser;

@Test
void test1_create_user() {
    sharedUser = service.createUser(request);
}

@Test
void test2_update_user() {
    service.updateUser(sharedUser);  // 依賴 test1
}

// ✅ 好：每個測試獨立
@Test
void should_create_user() {
    User user = service.createUser(request);
    assertThat(user).isNotNull();
}

@Test
void should_update_user() {
    User user = service.createUser(request);  // 自己建立需要的資料
    service.updateUser(user);
    assertThat(user.getUpdatedAt()).isNotNull();
}
```

### 3. 使用有意義的測試資料

```java
// ❌ 不好：使用無意義的測試資料
@Test
void test() {
    User user = new User("aaa", "bbb", 123);
    // ...
}

// ✅ 好：使用有意義的測試資料
@Test
void should_calculate_age_correctly() {
    User user = new User("張三", "zhang@example.com", 1990);
    int age = user.calculateAge(2025);
    assertThat(age).isEqualTo(35);
}
```

### 4. 避免測試實作細節

```java
// ❌ 不好：測試實作細節
@Test
void test_internal_method() {
    // 測試 private 方法或內部實作
}

// ✅ 好：測試公開行為
@Test
void should_return_correct_result() {
    // 透過公開 API 測試，不管內部如何實作
    Result result = service.process(input);
    assertThat(result).isEqualTo(expected);
}
```

### 5. 保持測試簡單

```java
// ❌ 不好：測試邏輯太複雜
@Test
void complex_test() {
    for (int i = 0; i < 10; i++) {
        if (i % 2 == 0) {
            // 複雜的條件判斷
        }
    }
}

// ✅ 好：測試邏輯簡單直接
@Test
void should_handle_even_numbers() {
    int result = calculator.process(2);
    assertThat(result).isEqualTo(4);
}

@Test
void should_handle_odd_numbers() {
    int result = calculator.process(3);
    assertThat(result).isEqualTo(9);
}
```

## 常見錯誤與解決方案

### 1. 測試太慢

**問題**：測試執行時間過長

**解決方案**：
- 使用 Mock 替代真實的外部依賴
- 避免在測試中使用 Thread.sleep()
- 不要在單元測試中連接真實資料庫
- 使用 @BeforeEach 而不是 @BeforeAll（除非確定需要）

### 2. 測試不穩定（Flaky Tests）

**問題**：測試有時通過，有時失敗

**解決方案**：
- 避免依賴系統時間，使用可控的時間源
- 不要依賴測試執行順序
- 避免使用隨機數，或使用固定的種子
- 清理測試產生的副作用

### 3. 過度使用 Mock

**問題**：Mock 太多，測試失去意義

**解決方案**：
```java
// ❌ 不好：Mock 太多
@Test
void test_with_too_many_mocks() {
    when(mock1.method()).thenReturn(value1);
    when(mock2.method()).thenReturn(value2);
    when(mock3.method()).thenReturn(value3);
    // ... 10 個 mock
}

// ✅ 好：只 Mock 必要的外部依賴
@Test
void should_process_order_correctly() {
    when(orderRepository.findById(orderId)).thenReturn(order);
    
    service.processOrder(orderId);
    
    verify(orderRepository).save(order);
}
```

## 測試執行

### 使用 Gradle

```bash
# 執行所有測試
./gradlew test

# 執行特定測試類別
./gradlew test --tests OrderServiceTest

# 執行特定測試方法
./gradlew test --tests OrderServiceTest.should_create_order

# 產生測試報告
./gradlew test jacocoTestReport

# 持續執行測試（檔案變更時自動執行）
./gradlew test --continuous
```

### 使用 Maven

```bash
# 執行所有測試
mvn test

# 執行特定測試類別
mvn test -Dtest=OrderServiceTest

# 跳過測試
mvn install -DskipTests

# 產生測試報告
mvn test jacoco:report
```

## 測試報告

### JaCoCo 覆蓋率報告

```gradle
// build.gradle
plugins {
    id 'jacoco'
}

jacoco {
    toolVersion = "0.8.11"
}

jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}

test {
    finalizedBy jacocoTestReport
}
```

查看報告：
```bash
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

## 進階主題

### 參數化測試

```java
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

class CalculatorTest {
    
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void should_return_true_for_positive_numbers(int number) {
        assertThat(calculator.isPositive(number)).isTrue();
    }
    
    @ParameterizedTest
    @CsvSource({
        "1, 1, 2",
        "2, 3, 5",
        "5, 5, 10"
    })
    void should_add_numbers_correctly(int a, int b, int expected) {
        assertThat(calculator.add(a, b)).isEqualTo(expected);
    }
}
```

### 測試生命週期

```java
import org.junit.jupiter.api.*;

class LifecycleTest {
    
    @BeforeAll
    static void beforeAll() {
        // 所有測試開始前執行一次
        System.out.println("測試類別初始化");
    }
    
    @BeforeEach
    void beforeEach() {
        // 每個測試前執行
        System.out.println("測試方法開始");
    }
    
    @Test
    void test1() {
        System.out.println("執行測試 1");
    }
    
    @Test
    void test2() {
        System.out.println("執行測試 2");
    }
    
    @AfterEach
    void afterEach() {
        // 每個測試後執行
        System.out.println("測試方法結束");
    }
    
    @AfterAll
    static void afterAll() {
        // 所有測試結束後執行一次
        System.out.println("測試類別清理");
    }
}
```

### 條件測試

```java
import org.junit.jupiter.api.condition.*;

class ConditionalTest {
    
    @Test
    @EnabledOnOs(OS.LINUX)
    void only_on_linux() {
        // 只在 Linux 上執行
    }
    
    @Test
    @EnabledOnJre(JRE.JAVA_21)
    void only_on_java_21() {
        // 只在 Java 21 上執行
    }
    
    @Test
    @EnabledIf("customCondition")
    void custom_condition() {
        // 自訂條件
    }
    
    boolean customCondition() {
        return System.getProperty("env").equals("dev");
    }
}
```

## 檢查清單

在提交程式碼前，確認以下事項：

### 測試品質檢查

- [ ] 每個測試都有清楚的命名
- [ ] 使用 AAA 或 Given-When-Then 結構
- [ ] 每個測試只驗證一個行為
- [ ] 測試之間互相獨立
- [ ] 沒有測試實作細節
- [ ] 使用有意義的測試資料
- [ ] 適當使用 Mock（不過度、不不足）

### 測試覆蓋檢查

- [ ] 測試了正常流程
- [ ] 測試了邊界條件
- [ ] 測試了例外情況
- [ ] 測試了錯誤處理
- [ ] 整體覆蓋率 > 80%

### 測試效能檢查

- [ ] 每個測試執行時間 < 50ms
- [ ] 沒有使用 Thread.sleep()
- [ ] 沒有連接真實資料庫
- [ ] 沒有呼叫外部 API

## 參考資源

### 推薦閱讀

- **書籍**：
  - "Test Driven Development: By Example" by Kent Beck
  - "Growing Object-Oriented Software, Guided by Tests" by Steve Freeman
  - "xUnit Test Patterns" by Gerard Meszaros

- **線上資源**：
  - JUnit 5 官方文件：https://junit.org/junit5/docs/current/user-guide/
  - Mockito 官方文件：https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
  - AssertJ 官方文件：https://assertj.github.io/doc/

### 常用工具

- **測試框架**：JUnit 5, TestNG
- **Mock 框架**：Mockito, EasyMock, PowerMock
- **斷言庫**：AssertJ, Hamcrest
- **覆蓋率工具**：JaCoCo, Cobertura
- **測試報告**：Allure, Surefire Report

## 總結

Unit Test 是軟體開發中不可或缺的一部分。好的單元測試能夠：

1. **提升程式碼品質**：促使你寫出更好的程式碼
2. **加快開發速度**：快速回饋，減少除錯時間
3. **降低維護成本**：及早發現問題，容易修復
4. **增加重構信心**：有測試保護，可以放心改善程式碼

記住：**寫測試不是負擔，而是投資**。投入時間寫好測試，長期來看會節省更多時間。

---

**文件版本**: 1.0  
**最後更新**: 2025-11-05  
**維護者**: 開發團隊
