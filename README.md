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
├── ddd/                        # DDD 核心概念實現
│   ├── annotations/           # DDD 相關註解
│   ├── context/              # 界限上下文相關
│   ├── events/               # 領域事件基礎設施
│   ├── lifecycle/            # 聚合生命週期管理
│   ├── policy/              # 領域政策
│   ├── repositories/        # 儲存庫
│   ├── saga/               # Saga 模式實現
│   ├── shared/             # 共享核心
│   └── specification/      # 規格模式
├── examples/                  # 範例實現
│   └── order/               # 訂單相關範例
│       ├── acl/            # 防腐層
│       ├── events/         # 訂單相關事件
│       ├── policy/         # 訂單相關政策
│       ├── saga/          # 訂單處理 Saga
│       ├── specification/ # 訂單相關規格
│       └── validation/    # 訂單驗證
└── model/                    # 基礎模型定義
```

## 快速開始

### 環境需求

- Java 17+
- Gradle 7.x+
- Spring Boot 3.x+

### 構建專案

```bash
./gradle clean build
```

### 運行範例

```bash
./gradle bootRun
```

### 測試 API

```bash
# 創建訂單
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"customer123","items":[{"productId":"prod1","quantity":2}]}'

# 查詢訂單
curl http://localhost:8080/api/orders/{orderId}
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

## 授權

本專案採用 MIT 授權。


## 發佈至DeepWiki做更多詳細內容說明

[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/humank/genai-demo)

## 聯繫方式

如有問題或建議，歡迎提出 Issue。
