# DDD 戰術模式教學專案

這個專案展示了領域驅動設計（Domain-Driven Design，DDD）中各種戰術模式的實作方式。專案以電商訂單系統為例，展示如何在實際應用中運用 DDD 的各種概念和模式。

## 專案特色

- 完整實作 DDD 戰術模式
- 使用 Java 17+ 的現代特性
- 豐富的註解和說明
- 實際的業務場景示例
- 模組化的設計

## UML 文檔

本專案包含完整的 UML 文檔，用於描述系統的架構、設計和行為。所有 UML 圖表都位於 `docs/uml/` 目錄下。

### 主要 UML 圖表

#### 基礎圖表
- **類別圖** ([class-diagram.svg](docs/uml/class-diagram.svg)): 展示系統中的主要類別及其關係
- **對象圖** ([object-diagram.svg](docs/uml/object-diagram.svg)): 展示領域模型的實例關係
- **組件圖** ([component-diagram.svg](docs/uml/component-diagram.svg)): 展示系統的主要組件及其交互
- **部署圖** ([deployment-diagram.svg](docs/uml/deployment-diagram.svg)): 描述系統的部署架構
- **套件圖** ([package-diagram.svg](docs/uml/package-diagram.svg)): 展示系統的套件結構和依賴關係
- **時序圖** ([sequence-diagram.svg](docs/uml/sequence-diagram.svg)): 描述訂單處理的主要流程
- **狀態圖** ([state-diagram.svg](docs/uml/state-diagram.svg)): 展示訂單在不同狀態之間的轉換
- **活動圖概覽** ([activity-diagram-overview.svg](docs/uml/activity-diagram-overview.svg)): 高層次展示訂單系統的主要業務流程
- **活動圖詳細** ([activity-diagram-detail.svg](docs/uml/activity-diagram-detail.svg)): 詳細展示訂單處理的具體步驟
- **使用案例圖** ([use-case-diagram.svg](docs/uml/use-case-diagram.svg)): 描述系統的主要功能和參與者

#### 領域驅動設計圖表
- **領域模型圖** ([domain-model-diagram.svg](docs/uml/domain-model-diagram.svg)): 詳細展示系統中的聚合根、實體、值對象和領域服務
- **六角形架構圖** ([hexagonal-architecture-diagram.svg](docs/uml/hexagonal-architecture-diagram.svg)): 詳細展示系統的端口和適配器模式
- **Saga模式圖** ([saga-pattern-diagram.svg](docs/uml/saga-pattern-diagram.svg)): 展示分布式事務處理流程
- **限界上下文圖** ([bounded-context-diagram.svg](docs/uml/bounded-context-diagram.svg)): 展示系統中不同上下文之間的關係
- **事件風暴圖** ([event-storming-diagram.svg](docs/uml/event-storming-diagram.svg)): 展示系統中的命令、事件、聚合根、策略和讀模型

#### 進階架構圖表
- **CQRS模式圖** ([cqrs-pattern-diagram.svg](docs/uml/cqrs-pattern-diagram.svg)): 展示命令和查詢責任分離模式
- **事件溯源圖** ([event-sourcing-diagram.svg](docs/uml/event-sourcing-diagram.svg)): 展示事件的存儲和重放機制
- **API接口圖** ([api-interface-diagram.svg](docs/uml/api-interface-diagram.svg)): 展示系統對外提供的API接口
- **數據模型圖** ([data-model-diagram.svg](docs/uml/data-model-diagram.svg)): 展示系統的數據庫模型和關係
- **安全架構圖** ([security-architecture-diagram.svg](docs/uml/security-architecture-diagram.svg)): 展示系統的安全機制和認證授權流程
- **可觀測性架構圖** ([observability-diagram.svg](docs/uml/observability-diagram.svg)): 展示系統的監控、日誌和可觀測性架構

更多關於 UML 文檔的詳細說明，請參閱 [UML 文檔說明](docs/uml/README.md)。

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
    private final ExternalLogisticsSystem legacySystem;

    public LogisticsAntiCorruptionLayer(ExternalLogisticsSystem legacySystem) {
        this.legacySystem = legacySystem;
    }

    /**
     * 將訂單轉換為外部物流系統可以理解的格式並創建配送單
     */
    public DeliveryOrder createDeliveryOrder(Order order) {
        // 將我們的領域模型轉換為外部系統期望的格式
        Map<String, String> externalFormat = Map.of(
            "reference_no", order.getId().toString(),
            "delivery_address", "從訂單中獲取地址", // 實際應用中會從Order中獲取
            "customer_contact", order.getCustomerId(),
            "items_count", String.valueOf(order.getItems().size()),
            "total_amount", order.getTotalAmount().toString()
        );

        // 調用外部系統並轉換回我們的模型
        String deliveryId = legacySystem.createDelivery(externalFormat);
        return new DeliveryOrder(
            OrderId.of(deliveryId),
            DeliveryStatus.CREATED
        );
    }

    /**
     * 查詢配送狀態並轉換為我們的領域模型能理解的格式
     */
    public DeliveryStatus getDeliveryStatus(OrderId orderId) {
        String externalStatus = legacySystem.getDeliveryStatus(orderId.toString());
        return mapExternalStatus(externalStatus);
    }

    private DeliveryStatus mapExternalStatus(String externalStatus) {
        return switch (externalStatus.toUpperCase()) {
            case "INIT", "PENDING" -> DeliveryStatus.CREATED;
            case "IN_TRANSIT" -> DeliveryStatus.IN_TRANSIT;
            case "DELIVERED" -> DeliveryStatus.DELIVERED;
            case "FAILED" -> DeliveryStatus.FAILED;
            default -> DeliveryStatus.UNKNOWN;
        };
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
  -d '{"customerId":"customer123","items":[{"productId":"prod-1","quantity":2}]}'
```