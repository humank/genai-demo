# Gherkin 場景設計指南

## 概述

本指南基於專案中的 BDD 實踐，提供 Gherkin 場景的編寫方法、Given-When-Then 模式和最佳實踐。

## Gherkin 語法基礎

### 基本結構

```gherkin
Feature: 功能描述
  As a [角色]
  I want [功能]
  So that [價值]

  Background:
    Given 共同的前置條件

  Scenario: 場景描述
    Given 前置條件
    When 執行動作
    Then 預期結果
    And 額外條件
    But 例外情況

  Scenario Outline: 參數化場景
    Given 前置條件 "<參數>"
    When 執行動作
    Then 預期結果 "<結果>"
    
    Examples:
      | 參數 | 結果 |
      | 值1  | 結果1 |
      | 值2  | 結果2 |
```

## 客戶管理場景範例

### 1. 客戶註冊功能

```gherkin
Feature: 客戶註冊
  As a 潛在客戶
  I want to 註冊帳戶
  So that 我可以使用系統服務

  Background:
    Given 系統正常運行
    And 註冊服務可用

  Scenario: 成功註冊新客戶
    Given 我是一個新用戶
    And 我有有效的個人資訊
    When 我提交註冊表單，包含以下資訊：
      | 姓名     | 電子郵件           | 電話號碼    |
      | 張三     | zhang@example.com | 0912345678 |
    Then 系統應該創建新的客戶帳戶
    And 我應該收到歡迎郵件
    And 客戶狀態應該是 "ACTIVE"

  Scenario: 註冊時電子郵件已存在
    Given 系統中已存在電子郵件 "existing@example.com" 的客戶
    When 我嘗試使用相同電子郵件註冊
    Then 系統應該顯示錯誤訊息 "電子郵件已被使用"
    And 不應該創建新的客戶帳戶

  Scenario Outline: 無效資料註冊驗證
    Given 我是一個新用戶
    When 我提交包含無效資料的註冊表單：
      | 姓名   | 電子郵件   | 電話號碼   |
      | <姓名> | <電子郵件> | <電話號碼> |
    Then 系統應該顯示錯誤訊息 "<錯誤訊息>"
    And 不應該創建客戶帳戶

    Examples:
      | 姓名 | 電子郵件        | 電話號碼   | 錯誤訊息           |
      |      | test@email.com  | 0912345678 | 姓名不能為空       |
      | 張三 | invalid-email   | 0912345678 | 電子郵件格式無效   |
      | 張三 | test@email.com  | 123        | 電話號碼格式無效   |
```

### 2. 客戶升級功能

```gherkin
Feature: 客戶會員等級升級
  As a 一般會員客戶
  I want to 升級到 VIP 會員
  So that 我可以享受更多優惠和服務

  Background:
    Given 客戶 "張三" 已註冊並登入系統
    And 客戶當前會員等級是 "STANDARD"

  Scenario: 達到消費門檻自動升級
    Given 客戶 "張三" 的總消費金額是 $8000
    And VIP 升級門檻是 $10000
    When 客戶完成一筆 $2500 的訂單
    Then 客戶的總消費金額應該是 $10500
    And 客戶的會員等級應該自動升級為 "VIP"
    And 系統應該發送升級通知郵件
    And 客戶應該收到 VIP 歡迎禮包

  Scenario: 手動申請 VIP 升級
    Given 客戶 "張三" 的總消費金額是 $15000
    And 客戶符合 VIP 升級條件
    When 客戶申請升級到 VIP 會員
    Then 系統應該立即處理升級申請
    And 客戶的會員等級應該變更為 "VIP"
    And 升級生效日期應該是今天

  Scenario: 不符合條件的升級申請
    Given 客戶 "張三" 的總消費金額是 $5000
    And VIP 升級門檻是 $10000
    When 客戶申請升級到 VIP 會員
    Then 系統應該拒絕升級申請
    And 顯示訊息 "消費金額未達到 VIP 升級門檻"
    And 客戶的會員等級應該保持 "STANDARD"
```

## 訂單處理場景範例

### 1. 訂單創建和處理

```gherkin
Feature: 訂單處理流程
  As a 客戶
  I want to 創建和處理訂單
  So that 我可以購買商品

  Background:
    Given 客戶 "李四" 已登入系統
    And 以下商品可用：
      | 商品ID | 商品名稱 | 價格  | 庫存 |
      | P001   | 筆記本   | $1200 | 50   |
      | P002   | 滑鼠     | $800  | 30   |
      | P003   | 鍵盤     | $1500 | 20   |

  Scenario: 成功創建訂單
    Given 客戶的購物車包含以下商品：
      | 商品ID | 數量 |
      | P001   | 2    |
      | P002   | 1    |
    When 客戶提交訂單
    Then 系統應該創建新訂單
    And 訂單狀態應該是 "PENDING"
    And 訂單總金額應該是 $3200
    And 庫存應該被預留：
      | 商品ID | 預留數量 |
      | P001   | 2        |
      | P002   | 1        |

  Scenario: 庫存不足時創建訂單
    Given 商品 "P003" 的庫存只有 5 件
    And 客戶嘗試訂購 10 件 "P003"
    When 客戶提交訂單
    Then 系統應該拒絕訂單
    And 顯示錯誤訊息 "商品庫存不足"
    And 不應該創建訂單
    And 不應該預留任何庫存

  Scenario: 訂單支付處理
    Given 客戶有一個待付款的訂單，訂單號 "ORD-001"
    And 訂單金額是 $3200
    When 客戶使用信用卡支付訂單
    And 支付處理成功
    Then 訂單狀態應該更新為 "PAID"
    And 系統應該發送訂單確認郵件
    And 應該開始準備出貨流程
```

### 2. 訂單取消和退款

```gherkin
Feature: 訂單取消和退款
  As a 客戶
  I want to 取消訂單並獲得退款
  So that 我可以在不需要商品時收回款項

  Background:
    Given 客戶 "王五" 已登入系統
    And 客戶有以下訂單：
      | 訂單號  | 狀態   | 金額   | 創建時間        |
      | ORD-001 | PAID   | $2400  | 2小時前         |
      | ORD-002 | SHIPPED| $1800  | 1天前           |

  Scenario: 在允許時間內取消已付款訂單
    Given 訂單 "ORD-001" 的狀態是 "PAID"
    And 訂單創建時間在 24 小時內
    When 客戶申請取消訂單 "ORD-001"
    Then 系統應該接受取消申請
    And 訂單狀態應該更新為 "CANCELLED"
    And 應該啟動退款流程
    And 預留的庫存應該被釋放
    And 客戶應該收到取消確認郵件

  Scenario: 嘗試取消已出貨的訂單
    Given 訂單 "ORD-002" 的狀態是 "SHIPPED"
    When 客戶申請取消訂單 "ORD-002"
    Then 系統應該拒絕取消申請
    And 顯示訊息 "已出貨的訂單無法取消"
    And 訂單狀態應該保持 "SHIPPED"
    And 建議客戶聯繫客服處理退貨

  Scenario: 退款處理流程
    Given 訂單 "ORD-001" 已被取消
    And 退款金額是 $2400
    When 系統處理退款
    Then 退款應該退回到原支付方式
    And 退款狀態應該是 "PROCESSING"
    And 客戶應該收到退款處理通知
    And 預計 3-5 個工作日到帳
```

## Step Definitions 實作

### 1. 客戶相關步驟定義

```java
@Component
public class CustomerStepDefinitions {
    
    @Autowired
    private CustomerApplicationService customerApplicationService;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private EmailService emailService;
    
    private CreateCustomerCommand currentCommand;
    private Customer createdCustomer;
    private Exception thrownException;
    
    @Given("我是一個新用戶")
    public void 我是一個新用戶() {
        // 準備新用戶的上下文
    }
    
    @Given("我有有效的個人資訊")
    public void 我有有效的個人資訊() {
        // 準備有效的個人資訊
    }
    
    @When("我提交註冊表單，包含以下資訊：")
    public void 我提交註冊表單包含以下資訊(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        Map<String, String> customerData = rows.get(0);
        
        currentCommand = new CreateCustomerCommand(
            customerData.get("姓名"),
            customerData.get("電子郵件"),
            customerData.get("電話號碼")
        );
        
        try {
            createdCustomer = customerApplicationService.createCustomer(currentCommand);
        } catch (Exception e) {
            thrownException = e;
        }
    }
    
    @Then("系統應該創建新的客戶帳戶")
    public void 系統應該創建新的客戶帳戶() {
        assertThat(createdCustomer).isNotNull();
        assertThat(createdCustomer.getId()).isNotNull();
    }
    
    @Then("我應該收到歡迎郵件")
    public void 我應該收到歡迎郵件() {
        verify(emailService).sendWelcomeEmail(
            eq(currentCommand.email()),
            eq(currentCommand.name())
        );
    }
    
    @Then("客戶狀態應該是 {string}")
    public void 客戶狀態應該是(String expectedStatus) {
        CustomerStatus status = CustomerStatus.valueOf(expectedStatus);
        assertThat(createdCustomer.getStatus()).isEqualTo(status);
    }
    
    @Given("系統中已存在電子郵件 {string} 的客戶")
    public void 系統中已存在電子郵件的客戶(String email) {
        Customer existingCustomer = CustomerTestBuilder.aCustomer()
            .withEmail(email)
            .build();
        customerRepository.save(existingCustomer);
    }
    
    @Then("系統應該顯示錯誤訊息 {string}")
    public void 系統應該顯示錯誤訊息(String expectedMessage) {
        assertThat(thrownException).isNotNull();
        assertThat(thrownException.getMessage()).contains(expectedMessage);
    }
    
    @Then("不應該創建新的客戶帳戶")
    public void 不應該創建新的客戶帳戶() {
        assertThat(createdCustomer).isNull();
    }
}
```

### 2. 訂單相關步驟定義

```java
@Component
public class OrderStepDefinitions {
    
    @Autowired
    private OrderApplicationService orderApplicationService;
    
    @Autowired
    private InventoryService inventoryService;
    
    private Customer currentCustomer;
    private List<OrderItem> cartItems = new ArrayList<>();
    private Order createdOrder;
    private Exception orderException;
    
    @Given("客戶 {string} 已登入系統")
    public void 客戶已登入系統(String customerName) {
        currentCustomer = CustomerTestBuilder.aCustomer()
            .withName(customerName)
            .build();
    }
    
    @Given("以下商品可用：")
    public void 以下商品可用(DataTable dataTable) {
        List<Map<String, String>> products = dataTable.asMaps();
        
        for (Map<String, String> product : products) {
            String productId = product.get("商品ID");
            String productName = product.get("商品名稱");
            BigDecimal price = new BigDecimal(product.get("價格").replace("$", ""));
            int stock = Integer.parseInt(product.get("庫存"));
            
            // 設置商品和庫存
            setupProductWithStock(productId, productName, price, stock);
        }
    }
    
    @Given("客戶的購物車包含以下商品：")
    public void 客戶的購物車包含以下商品(DataTable dataTable) {
        List<Map<String, String>> items = dataTable.asMaps();
        
        cartItems.clear();
        for (Map<String, String> item : items) {
            String productId = item.get("商品ID");
            int quantity = Integer.parseInt(item.get("數量"));
            
            Product product = getProductById(productId);
            OrderItem orderItem = new OrderItem(
                ProductId.of(productId),
                quantity,
                product.getPrice()
            );
            cartItems.add(orderItem);
        }
    }
    
    @When("客戶提交訂單")
    public void 客戶提交訂單() {
        CreateOrderCommand command = new CreateOrderCommand(
            currentCustomer.getId(),
            cartItems
        );
        
        try {
            createdOrder = orderApplicationService.createOrder(command);
        } catch (Exception e) {
            orderException = e;
        }
    }
    
    @Then("系統應該創建新訂單")
    public void 系統應該創建新訂單() {
        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.getId()).isNotNull();
    }
    
    @Then("訂單狀態應該是 {string}")
    public void 訂單狀態應該是(String expectedStatus) {
        OrderStatus status = OrderStatus.valueOf(expectedStatus);
        assertThat(createdOrder.getStatus()).isEqualTo(status);
    }
    
    @Then("訂單總金額應該是 ${int}")
    public void 訂單總金額應該是(int expectedAmount) {
        Money expectedTotal = Money.twd(expectedAmount);
        assertThat(createdOrder.getTotalAmount()).isEqualTo(expectedTotal);
    }
    
    @Then("庫存應該被預留：")
    public void 庫存應該被預留(DataTable dataTable) {
        List<Map<String, String>> reservations = dataTable.asMaps();
        
        for (Map<String, String> reservation : reservations) {
            String productId = reservation.get("商品ID");
            int expectedReserved = Integer.parseInt(reservation.get("預留數量"));
            
            int actualReserved = inventoryService.getReservedQuantity(
                ProductId.of(productId)
            );
            assertThat(actualReserved).isEqualTo(expectedReserved);
        }
    }
    
    private void setupProductWithStock(String productId, String productName, 
                                     BigDecimal price, int stock) {
        // 實作商品和庫存設置邏輯
    }
    
    private Product getProductById(String productId) {
        // 實作商品查詢邏輯
        return null;
    }
}
```

## BDD 最佳實踐

### 1. 場景設計原則

#### ✅ 好的場景設計

```gherkin
# 清晰的業務語言
Scenario: VIP 客戶享受免運費優惠
  Given 客戶 "張三" 是 VIP 會員
  And 客戶的訂單金額是 $1500
  When 系統計算運費
  Then 運費應該是 $0
  And 訂單應該顯示 "VIP 免運費優惠"

# 具體的測試數據
Scenario: 計算批量折扣
  Given 客戶購買以下商品：
    | 商品   | 數量 | 單價  |
    | 筆記本 | 10   | $1200 |
  When 系統應用批量折扣規則
  Then 應該給予 5% 的批量折扣
  And 最終價格應該是 $11400
```

#### ❌ 避免的場景設計

```gherkin
# 過於技術性的語言
Scenario: 測試 CustomerService.createCustomer 方法
  Given CustomerRepository 返回 null
  When 調用 createCustomer 方法
  Then 應該拋出 CustomerNotFoundException

# 模糊的測試條件
Scenario: 客戶註冊
  Given 用戶輸入一些資料
  When 提交表單
  Then 應該成功
```

### 2. 數據表使用技巧

```gherkin
# 垂直數據表 - 適合單一實體的多個屬性
Scenario: 創建客戶檔案
  When 我創建客戶，包含以下資訊：
    | 姓名     | 張三              |
    | 電子郵件 | zhang@example.com |
    | 電話     | 0912345678        |
    | 地址     | 台北市信義區      |
  Then 客戶檔案應該被成功創建

# 水平數據表 - 適合多個實體或參數化測試
Scenario Outline: 驗證不同會員等級的折扣
  Given 客戶的會員等級是 "<等級>"
  When 客戶購買價值 $1000 的商品
  Then 應該獲得 "<折扣>" 的優惠
  
  Examples:
    | 等級     | 折扣 |
    | STANDARD | 0%   |
    | PREMIUM  | 5%   |
    | VIP      | 10%  |
```

### 3. Background 使用指南

```gherkin
Feature: 訂單管理

  Background:
    Given 系統正常運行
    And 以下客戶已註冊：
      | 姓名 | 電子郵件           | 會員等級 |
      | 張三 | zhang@example.com  | VIP      |
      | 李四 | li@example.com     | STANDARD |
    And 以下商品可用：
      | 商品ID | 名稱   | 價格  | 庫存 |
      | P001   | 筆記本 | $1200 | 100  |
      | P002   | 滑鼠   | $800  | 50   |

  Scenario: VIP 客戶創建訂單
    Given 客戶 "張三" 已登入
    # 其他步驟...

  Scenario: 一般客戶創建訂單
    Given 客戶 "李四" 已登入
    # 其他步驟...
```

### 4. 標籤和組織

```gherkin
@smoke @customer
Feature: 客戶註冊

@regression @order @payment
Scenario: 訂單支付流程
  # 場景內容...

@integration @slow
Scenario: 完整的訂單處理流程
  # 場景內容...
```

## 執行和報告

### 1. Cucumber 配置

```java
@CucumberOptions(
    features = "src/test/resources/features",
    glue = "solid.humank.genaidemo.bdd.stepdefinitions",
    plugin = {
        "pretty",
        "html:build/reports/cucumber/html",
        "json:build/reports/cucumber/json/cucumber.json",
        "junit:build/reports/cucumber/xml/cucumber.xml"
    },
    tags = "@smoke or @regression"
)
public class CucumberTestRunner {
}
```

### 2. 測試執行

```bash
# 執行所有 BDD 測試
./gradlew cucumber

# 執行特定標籤的測試
./gradlew cucumber -Dcucumber.filter.tags="@smoke"

# 執行特定功能的測試
./gradlew cucumber -Dcucumber.filter.name="客戶註冊"
```

## 總結

良好的 Gherkin 場景設計應該：

1. **使用業務語言**：避免技術術語，使用利害關係人理解的語言
2. **具體且可測試**：提供明確的測試數據和預期結果
3. **獨立且可重複**：每個場景都應該能獨立執行
4. **涵蓋邊界情況**：包含正常流程和異常情況
5. **保持簡潔**：一個場景專注於一個業務規則或流程

通過遵循這些原則，BDD 場景可以成為活文檔，既是需求規格又是自動化測試。