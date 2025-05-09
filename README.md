# DDD 戰術模式教學專案

這個專案展示了領域驅動設計（Domain-Driven Design，DDD）中各種戰術模式的實作方式。專案以電商訂單系統為例，展示如何在實際應用中運用 DDD 的各種概念和模式。

## 專案特色

- 完整實作 DDD 戰術模式
- 使用 Java 17+ 的現代特性
- 豐富的註解和說明
- 實際的業務場景示例
- 模組化的設計

## 核心概念和模式

### 1. 值物件（Value Objects）

**原理：** 以其屬性值定義的物件，沒有唯一標識，具有不可變性。
**使用時機：** 當物件的特性由其屬性值完全定義時。
**範例：**

```java
@ValueObject
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        // 確保不變條件
        if (amount == null) throw new IllegalArgumentException("金額不能為空");
        if (currency == null) throw new IllegalArgumentException("幣別不能為空");
    }
}
```

### 2. 實體（Entities）

**原理：** 具有唯一標識的物件，可以改變其屬性。
**使用時機：** 當物件需要在其生命週期中被追蹤和識別時。
**範例：**

```java
@Entity
public class OrderItem {
    private final String productId;
    private int quantity;
    // ...
}
```

### 3. 聚合根（Aggregate Roots）

**原理：** 確保業務規則和不變條件的實體，是外部訪問內部實體的唯一入口。
**使用時機：** 當需要確保一組相關物件的一致性時。
**範例：**

```java
@AggregateRoot
@ManagedLifecycle
public class Order {
    private final OrderId orderId;
    private final List<OrderItem> items;
    // ...
}
```

### 4. 領域事件（Domain Events）

**原理：** 用於通知系統中其他部分領域中發生的重要事件。
**使用時機：** 當需要在不同界限上下文之間通信，或實現最終一致性時。
**範例：**

```java
public record OrderCreatedEvent(
    OrderId orderId,
    String customerId,
    Money totalAmount
) implements DomainEvent {
    // ...
}
```

### 5. 規格模式（Specification Pattern）

**原理：** 將複雜的業務規則封裝成可重用的物件。
**使用時機：** 當有複雜的、可組合的業務規則需要評估時。
**範例：**

```java
public class OrderDiscountSpecification implements Specification<Order> {
    @Override
    public boolean isSatisfiedBy(Order order) {
        return isWeekend() && 
               hasMinimumAmount(order) && 
               hasMultipleItems(order);
    }
}
```

### 6. 防腐層（Anti-corruption Layer）

**原理：** 隔離和轉換不同系統之間的模型差異。
**使用時機：** 當需要整合遺留系統或第三方服務時。
**範例：**

```java
public class LogisticsAntiCorruptionLayer {
    public DeliveryOrder createDeliveryOrder(Order order) {
        // 將內部訂單模型轉換為外部系統期望的格式
        Map<String, String> externalFormat = Map.of(
            "reference_no", order.getOrderId().toString(),
            "delivery_address", "...",
            // ...
        );
        // ...
    }
}
```

### 7. 領域政策（Domain Policies）

**原理：** 封裝可能變化的業務規則。
**使用時機：** 當業務規則可能因時間、地點或其他因素而改變時。
**範例：**

```java
public class OrderDiscountPolicy implements DomainPolicy<Order, Money> {
    @Override
    public Money apply(Order order) {
        if (!isApplicableTo(order)) return order.getTotalAmount();
        return calculateDiscountedAmount(order);
    }
}
```

### 8. Saga 模式

**原理：** 協調分散式交易和長時間運行的業務流程。
**使用時機：** 當需要處理跨多個服務的業務流程，並確保最終一致性時。
**範例：**

```java
public class OrderProcessingSaga {
    public void process(OrderSagaContext context) {
        new SagaDefinition<>(context)
            .step("驗證訂單", this::validateOrder)
            .step("處理付款", this::processPayment)
            .step("建立物流訂單", this::createDeliveryOrder)
            .execute();
    }
}
```

### 9. 聚合生命週期管理

**原理：** 管理聚合根的狀態變更和事件發布。
**使用時機：** 當需要追蹤和管理聚合根的完整生命週期時。
**範例：**

```java
@ManagedLifecycle
public class Order {
    public void addItem(String productId, int quantity) {
        // ...
        AggregateLifecycle.apply(new OrderItemAddedEvent(...));
    }
}
```

### 10. 共享核心（Shared Kernel）

**原理：** 在不同界限上下文之間共享的通用概念和組件。
**使用時機：** 當多個界限上下文需要共用某些基本概念時。
**範例：**

```java
public abstract class BusinessId {
    protected final UUID id;
    // 共享的識別碼基礎實作
}
```

## 專案結構

```text
app/src/main/java/solid/humank/genaidemo/
├── application                 # 應用層 - 協調領域對象完成用戶任務
│   ├── common                  # 共用應用層組件
│   ├── order                   # 訂單相關應用服務
│   │   ├── dto                 # 數據傳輸對象
│   │   │   ├── command         # 命令對象
│   │   │   ├── query           # 查詢對象
│   │   │   └── response        # 響應對象
│   │   ├── port                # 端口定義
│   │   │   ├── incoming        # 入站端口 (用例接口)
│   │   │   └── outgoing        # 出站端口 (基礎設施依賴)
│   │   └── service             # 應用服務實現
│   └── payment                 # 支付相關應用服務
│       ├── dto                 # 數據傳輸對象
│       ├── port                # 端口定義
│       └── service             # 應用服務實現
├── domain                      # 領域層 - 包含業務邏輯和規則的核心層
│   ├── common                  # 共用領域組件
│   │   ├── annotations         # DDD 相關註解
│   │   ├── context             # 界限上下文相關
│   │   ├── events              # 領域事件基礎設施
│   │   ├── factory             # 工廠模式組件
│   │   ├── lifecycle           # 聚合生命週期管理
│   │   ├── model               # 共用領域模型
│   │   ├── policy              # 領域政策
│   │   ├── repository          # 儲存庫接口
│   │   ├── specification       # 規格模式組件
│   │   ├── validation          # 領域驗證
│   │   └── valueobject         # 共用值對象
│   ├── order                   # 訂單領域模型
│   │   ├── events              # 訂單領域事件
│   │   ├── model               # 訂單模型
│   │   │   ├── aggregate       # 訂單聚合根
│   │   │   ├── entity          # 訂單實體
│   │   │   ├── events          # 訂單事件
│   │   │   ├── factory         # 訂單工廠
│   │   │   ├── policy          # 訂單政策
│   │   │   ├── service         # 訂單領域服務
│   │   │   ├── specification   # 訂單規格
│   │   │   └── valueobject     # 訂單值對象
│   │   ├── repository          # 訂單儲存庫接口
│   │   └── validation          # 訂單驗證
│   └── payment                 # 支付領域模型
│       ├── events              # 支付領域事件
│       ├── model               # 支付模型
│       │   ├── aggregate       # 支付聚合根
│       │   ├── entity          # 支付實體
│       │   ├── events          # 支付事件
│       │   ├── factory         # 支付工廠
│       │   ├── service         # 支付領域服務
│       │   └── valueobject     # 支付值對象
│       └── repository          # 支付儲存庫接口
├── infrastructure              # 基礎設施層 - 提供技術能力支持其他層
│   ├── common                  # 共用基礎設施
│   │   ├── config              # 共用配置
│   │   └── persistence         # 共用持久化組件
│   ├── config                  # 配置類
│   ├── order                   # 訂單相關基礎設施
│   │   ├── acl                 # 訂單防腐層
│   │   ├── external            # 外部系統適配器
│   │   └── persistence         # 訂單持久化實現
│   ├── payment                 # 支付相關基礎設施
│   │   ├── external            # 外部支付系統適配器
│   │   └── persistence         # 支付持久化實現
│   ├── persistence             # 通用持久化組件
│   └── saga                    # Saga 流程協調
│       └── definition          # Saga 定義
└── interfaces                  # 介面層 - 負責向用戶或外部系統展示信息
    └── web                     # Web 介面
        ├── order               # 訂單相關控制器
        │   └── dto             # 訂單 Web DTO
        └── payment             # 支付相關控制器
            └── dto             # 支付 Web DTO
```

### 架構測試

專案包含使用 ArchUnit 實現的架構測試，用於確保專案遵循 DDD 的最佳實踐：

```text
app/src/test/java/solid/humank/genaidemo/architecture/
├── DddArchitectureTest.java    # 確保遵循 DDD 分層架構
├── DddTacticalPatternsTest.java # 確保正確實現 DDD 戰術模式
└── PackageStructureTest.java   # 確保包結構符合 DDD 最佳實踐
```

### 如何解讀測試結果

如果架構測試失敗，錯誤訊息會指出哪些類別違反了架構規則。例如：

```text
Architecture Violation: Rule 'classes that reside in a package '..domain..' should not depend on classes that reside in any package ['..application..', '..infrastructure..', '..interfaces..']' was violated (1 times):
Method solid.humank.genaidemo.domain.order.model.Order.process() calls method solid.humank.genaidemo.application.order.service.OrderApplicationService.processOrder()
```

## 快速開始

### 環境需求

- Java 17+
- Gradle 7.x+
- Spring Boot 3.x+

### 技術棧

- **Spring Boot 3.4.5**: 應用程式框架
- **Spring Data JPA**: 資料持久化
- **H2 Database**: 內存資料庫，用於開發和測試
- **Lombok**: 減少樣板代碼
- **ArchUnit**: 架構測試工具

### 構建專案

```bash
./gradlew clean build
```

### 運行範例

```bash
./gradlew bootRun
```

應用程式將在 <http://localhost:8080> 啟動，H2 資料庫控制台可通過 <http://localhost:8080/h2-console> 訪問。

### 測試 API

```bash
# 創建訂單
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"customer123","items":[{"productId":"prod1","quantity":2}]}'

# 查詢訂單
curl http://localhost:8080/api/orders/{orderId}
```

### 運行架構測試

確保專案遵循 DDD 最佳實踐：

```bash
./gradlew test --tests "solid.humank.genaidemo.architecture.*"
```

## 最佳實踐和建議

1. **界限上下文的劃分**
   - 根據業務邊界明確劃分
   - 使用防腐層隔離外部系統
   - 謹慎共享模型

2. **領域模型的設計**
   - 保持聚合根的邊界清晰
   - 使用值物件提高不可變性
   - 通過領域事件實現鬆耦合

3. **代碼組織**
   - 依照 DDD 概念組織包結構
   - 使用註解明確標記模式
   - 保持測試覆蓋

4. **性能考量**
   - 適當使用懶加載
   - 注意聚合大小
   - 考慮緩存策略

## 學習路徑建議

1. **基礎概念**
   - 從值物件（Money, OrderId）開始理解
   - 學習實體和聚合根的概念
   - 理解領域事件的使用

2. **進階模式**
   - 研究規格模式的實現
   - 學習防腐層的應用
   - 理解 Saga 模式的使用

3. **整體架構**
   - 學習界限上下文的劃分
   - 理解領域事件的發布訂閱
   - 掌握聚合生命週期管理

4. **最佳實踐**
   - 研究測試案例
   - 理解錯誤處理
   - 學習性能優化技巧

## 常見問題

### Q: 如何選擇合適的聚合大小？

A: 聚合應該圍繞著業務不變條件來劃分，保持聚合邊界的內聚性。一般建議：

- 優先考慮小型聚合
- 確保聚合邊界清晰
- 通過領域事件處理跨聚合的業務流程

### Q: 什麼時候使用值物件 vs 實體？

A:

- 值物件：當物件的身份由其屬性決定時（如 Money）
- 實體：當物件需要在生命週期中被追蹤時（如 Order）

### Q: 如何處理複雜的業務規則？

A: 可以使用以下方式：

- 規格模式封裝複雜條件
- 領域政策處理可變規則
- 領域服務處理跨聚合的邏輯

## 測試最佳實踐

### 測試策略分層

本專案採用以下測試策略：

1. **領域模型測試**：
   - 專注於測試聚合根和值物件的業務邏輯
   - 驗證核心業務規則的正確性
   - 不依賴外部系統或框架

2. **控制器測試**：
   - 使用 Mockito 模擬應用服務
   - 驗證控制器的請求處理和響應生成
   - 簡化測試環境準備，不啟動 Spring 容器

3. **架構測試保障**：
   - 使用 ArchUnit 驗證架構規則
   - 防止架構腐化
   - 確保 DDD 最佳實踐的遵循

### 測試隔離技巧

為確保測試的獨立性和可靠性，我們採用以下技巧：

1. **測試專用配置**：

   ```java
   @TestConfiguration
   public class TestConfig {
       @Bean
       @Primary
       public OrderManagementUseCase orderManagementUseCase() {
           return mock(OrderManagementUseCase.class);
       }
   }
   ```

2. **測試專用配置文件**：

   ```properties
   # application-test.properties
   spring.main.allow-bean-definition-overriding=true
   ```

3. **模擬外部依賴**：

   ```java
   @MockBean
   private OrderPersistencePort orderPersistencePort;
   ```

### 執行測試

```bash
# 執行所有測試
./gradlew test

# 僅執行單元測試
./gradlew test --tests "*Mock*"

# 僅執行架構測試
./gradlew test --tests "*Architecture*"
```

## 貢獻指南

1. Fork 本專案
2. 創建特性分支
3. 提交變更
4. 發起 Pull Request

歡迎貢獻以下內容：

- 新的設計模式示例
- 文檔改進
- 錯誤修復
- 測試案例

## 測試策略

本專案採用多層次的測試策略，確保代碼質量和架構完整性。

### 單元測試

單元測試主要專注於測試領域模型（特別是聚合根）的業務邏輯，確保核心業務規則的正確性：

```java
class OrderTest {
    @Test
    @DisplayName("添加訂單項目應正確計算總金額")
    void addItemShouldCalculateTotalAmountCorrectly() {
        // 準備
        Order order = new Order(OrderId.generate(), "customer-123", "台北市信義區");
        
        // 執行
        order.addItem("product-1", "iPhone 15", 1, Money.of(new BigDecimal("30000")));
        
        // 驗證
        assertEquals(new BigDecimal("30000"), order.getTotalAmount().getAmount());
    }
}
```

### 控制器測試

為了簡化測試環境的準備，控制器測試採用 Mockito 進行模擬，不依賴 Spring 容器：

```java
@ExtendWith(MockitoExtension.class)
class OrderControllerMockTest {
    @Mock
    private OrderManagementUseCase orderManagementUseCase;

    @InjectMocks
    private OrderController orderController;

    @Test
    @DisplayName("創建訂單應返回201狀態碼和訂單詳情")
    void createOrderShouldReturn201AndOrderDetails() {
        // 測試代碼...
    }
}
```

### 常見架構違規及解決方案

#### 1. 領域層依賴應用層

**問題**：領域模型不應該依賴應用服務。

**解決方案**：

- 將應用層邏輯移到應用服務中
- 使用領域事件進行通信
- 使用依賴倒置原則定義接口

#### 2. 聚合根直接訪問其他聚合根

**問題**：聚合根應該通過ID引用其他聚合根，而不是直接引用。

**解決方案**：

- 使用ID引用其他聚合根
- 使用領域事件進行聚合間通信
- 使用領域服務協調多個聚合

#### 3. 基礎設施關注點滲透到領域層

**問題**：領域模型不應該包含持久化或技術細節。

**解決方案**：

- 使用儲存庫模式隔離持久化邏輯
- 使用依賴倒置定義接口
- 將技術細節移到基礎設施層

## 六邊形架構實現

本專案採用六邊形架構（Hexagonal Architecture，又稱為端口和適配器架構），將應用核心與外部依賴隔離：

### 端口（Ports）

端口定義了應用核心與外部世界交互的接口：

1. **入站端口（Inbound/Primary Ports）**：
   - 定義應用核心提供的服務
   - 位於 `application.*.port.incoming` 包中
   - 例如：`OrderManagementUseCase`

```java
public interface OrderManagementUseCase {
    OrderResponse createOrder(CreateOrderRequestDto request);
    OrderResponse addOrderItem(AddOrderItemRequestDto request);
    OrderResponse submitOrder(String orderId);
    // ...
}
```

1. **出站端口（Outbound/Secondary Ports）**：
   - 定義應用核心需要的外部服務
   - 位於 `application.*.port.outgoing` 包中
   - 例如：`OrderPersistencePort`

```java
public interface OrderPersistencePort {
    void save(Order order);
    Optional<Order> findById(OrderId orderId);
    List<Order> findAll();
    // ...
}
```

### 適配器（Adapters）

適配器實現端口接口，連接應用核心與外部世界：

1. **入站適配器（Inbound/Primary Adapters）**：
   - 將外部請求轉換為應用核心可理解的格式
   - 位於 `interfaces.web.*` 包中
   - 例如：`OrderController`

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderManagementUseCase orderService;
    
    // 實現入站適配器邏輯
}
```

1. **出站適配器（Outbound/Secondary Adapters）**：
   - 實現應用核心定義的出站端口
   - 位於 `infrastructure.*.persistence` 和 `infrastructure.*.external` 包中
   - 例如：`OrderRepositoryAdapter`

```java
@Component
public class OrderRepositoryAdapter implements OrderPersistencePort {
    private final OrderRepository orderRepository;
    
    // 實現出站適配器邏輯
}
```

### 依賴倒置

六邊形架構通過依賴倒置原則實現核心邏輯與外部依賴的解耦：

```text
外部世界 → 適配器 → 端口 ← 應用核心
```

這種架構帶來的好處：

- 應用核心不依賴於基礎設施細節
- 便於替換外部組件（如數據庫、消息隊列）
- 提高可測試性，可以輕鬆模擬外部依賴

## 常見架構問題及解決方案

在實現 DDD 和六邊形架構時，可能遇到以下常見問題：

### 1. Bean 定義衝突

**問題**：多個適配器實現同一個端口接口，導致 Spring 容器中存在多個相同類型的 Bean。

**解決方案**：

- 使用 `@Primary` 標記主要實現
- 使用 `@Qualifier` 明確指定注入的 Bean
- 重命名適配器類，避免名稱衝突

### 2. 架構邊界模糊

**問題**：層與層之間的邊界不清晰，導致架構測試失敗。

**解決方案**：

- 明確定義每一層的職責
- 使用包結構強制執行架構規則
- 實現架構測試驗證邊界

### 3. 測試策略不當

**問題**：過度依賴整合測試，導致測試運行緩慢且脆弱。

**解決方案**：

- 優先使用單元測試
- 針對核心業務邏輯編寫豐富的單元測試
- 使用模擬對象隔離外部依賴

## 配置說明

專案使用 Spring Boot 的標準配置機制，主要配置位於 `application.properties` 文件：

```properties
# 應用程序相關配置
spring.application.name=genai-demo
server.port=8080

# 訂單相關配置
order.validation.max-items=${ORDER_MAX_ITEMS:100}
order.validation.max-amount=${ORDER_MAX_AMOUNT:1000000}

# 資料庫配置
spring.datasource.url=jdbc:h2:mem:genaidemo
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# H2 控制台配置
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA 配置
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

## DeepWiki整合

[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/humank/genai-demo)

## 授權

本專案採用 MIT 授權。

## 聯繫方式

如有問題或建議，歡迎提出 Issue。
