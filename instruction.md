# DDD 架構重構指南

本文檔提供了將現有專案重構為符合領域驅動設計 (DDD) 分層架構的指南，特別針對包含多個領域（如訂單和支付）的專案。

## 多領域的 DDD 架構建議結構

```
solid.humank.genaidemo/
├── domain/                           # 領域層
│   ├── common/                       # 共享領域概念
│   │   ├── model/                    # 共享模型
│   │   └── events/                   # 共享事件
│   ├── order/                        # 訂單領域
│   │   ├── model/                    # 訂單領域模型
│   │   │   ├── aggregate/            # 聚合根 (Order)
│   │   │   ├── entity/               # 實體
│   │   │   ├── valueobject/          # 值對象 (OrderId, Money)
│   │   │   ├── events/               # 領域事件 (OrderCreatedEvent)
│   │   │   ├── service/              # 領域服務
│   │   │   ├── factory/              # 工廠 (OrderFactory)
│   │   │   ├── policy/               # 策略 (OrderDiscountPolicy)
│   │   │   └── specification/        # 規格 (OrderDiscountSpecification)
│   │   └── repository/               # 儲存庫接口 (OrderRepository)
│   └── payment/                      # 支付領域
│       ├── model/                    # 支付領域模型
│       │   ├── aggregate/            # 聚合根 (Payment)
│       │   ├── entity/               # 實體
│       │   ├── valueobject/          # 值對象 (PaymentId, PaymentStatus)
│       │   ├── events/               # 領域事件 (PaymentProcessedEvent)
│       │   ├── service/              # 領域服務 (PaymentService)
│       │   └── factory/              # 工廠
│       └── repository/               # 儲存庫接口 (PaymentRepository)
├── application/                      # 應用層
│   ├── common/                       # 共享應用服務
│   ├── order/                        # 訂單應用服務
│   │   ├── service/                  # 應用服務 (OrderApplicationService)
│   │   ├── dto/                      # DTO
│   │   │   ├── command/              # 命令 (CreateOrderCommand)
│   │   │   ├── query/                # 查詢 (GetOrderQuery)
│   │   │   └── response/             # 響應 (OrderResponse)
│   │   └── port/                     # 端口
│   │       ├── incoming/             # 入站端口 (OrderManagementUseCase)
│   │       └── outgoing/             # 出站端口 (OrderPersistencePort)
│   └── payment/                      # 支付應用服務
│       ├── service/                  # 應用服務 (PaymentApplicationService)
│       ├── dto/                      # DTO
│       └── port/                     # 端口
│           ├── incoming/             # 入站端口 (PaymentProcessingUseCase)
│           └── outgoing/             # 出站端口 (PaymentPersistencePort)
├── infrastructure/                   # 基礎設施層
│   ├── common/                       # 共享基礎設施
│   │   ├── persistence/              # 共享持久化
│   │   └── config/                   # 共享配置
│   ├── order/                        # 訂單基礎設施
│   │   ├── persistence/              # 持久化 (OrderRepositoryImpl)
│   │   ├── external/                 # 外部服務適配器
│   │   └── acl/                      # 防腐層 (LogisticsAntiCorruptionLayer)
│   ├── payment/                      # 支付基礎設施
│   │   ├── persistence/              # 持久化 (PaymentRepositoryImpl)
│   │   └── external/                 # 外部支付服務適配器 (ExternalPaymentAdapter)
│   ├── saga/                         # Saga 協調器 (OrderProcessingSaga)
│   └── config/                       # 配置 (OrderConfig)
└── interface/                        # 介面層
    ├── web/                          # Web 控制器
    │   ├── order/                    # 訂單控制器 (OrderController)
    │   │   └── dto/                  # Web DTO (CreateOrderRequest)
    │   └── payment/                  # 支付控制器 (PaymentController)
    │       └── dto/                  # Web DTO
    └── rest/                         # REST API
        ├── order/                    # 訂單 API
        └── payment/                  # 支付 API
```

## 主要問題

1. **層之間的依賴混亂**：
   - 應用層依賴了基礎設施層的類（如 DTO 類）
   - 基礎設施層的某些組件直接訪問領域層
   - 防腐層、驗證層和 Saga 的位置不符合標準 DDD 架構

2. **包結構不一致**：
   - 一些工廠類位於 `ddd.factories` 包而不是 `model.factory` 包
   - DTO 類位於 `infrastructure.web.dto` 而不是 `application.dto` 或 `interface.dto`

## 實施步驟

### 1. 重新組織領域層

- 將 `examples.order.model` 移動到 `domain.order.model`
- 將 `examples.payment.model` 移動到 `domain.payment.model`
- 確保每個領域的模型包含適當的子包（aggregate, valueobject, events 等）

### 2. 重新組織應用層

- 將 `examples.order.application` 移動到 `application.order`
- 將 `examples.payment.application` 移動到 `application.payment`
- 將 DTO 從 `infrastructure` 層移動到 `application.order.dto` 或 `interface.web.order.dto`

### 3. 重新組織基礎設施層

- 將 `examples.order.infrastructure` 移動到 `infrastructure.order`
- 將 `examples.payment.infrastructure` 移動到 `infrastructure.payment`
- 將 `acl` 包移動到 `infrastructure.order.acl`
- 將 `saga` 包移動到 `infrastructure.saga`

### 4. 重新組織介面層

- 將 Web 控制器移動到 `interface.web.order` 和 `interface.web.payment`
- 將 Web DTO 移動到 `interface.web.order.dto` 和 `interface.web.payment.dto`

## 具體重構示例

### 訂單聚合根

```java
// 從
package solid.humank.genaidemo.examples.order.model.aggregate;

// 到
package solid.humank.genaidemo.domain.order.model.aggregate;
```

### 支付應用服務

```java
// 從
package solid.humank.genaidemo.examples.payment.application.service;

// 到
package solid.humank.genaidemo.application.payment.service;
```

### 訂單儲存庫實現

```java
// 從
package solid.humank.genaidemo.examples.order.infrastructure.persistence;

// 到
package solid.humank.genaidemo.infrastructure.order.persistence;
```

### 訂單控制器

```java
// 從
package solid.humank.genaidemo.examples.order.controller;

// 到
package solid.humank.genaidemo.interface.web.order;
```

### 訂單 DTO

```java
// 從
package solid.humank.genaidemo.examples.order.infrastructure.web.dto;

// 到
package solid.humank.genaidemo.interface.web.order.dto;
```

## 修正層之間的依賴

### 1. 應用層不應依賴基礎設施層

- 將 `OrderResponse` 從 `infrastructure.web.dto` 移動到 `application.order.dto.response`
- 在應用層定義接口，在基礎設施層實現

### 2. 領域層不應依賴其他層

- 確保領域模型不引用應用層或基礎設施層的類

### 3. Saga 應該通過應用層與領域層交互

- `OrderProcessingSaga` 應該通過應用層的服務或端口與領域模型交互，而不是直接訪問

## 處理領域間的依賴

### 1. 領域事件

- 使用領域事件進行領域間通信
- 例如，訂單領域發布 `OrderSubmittedEvent`，支付領域訂閱並處理

### 2. 防腐層

- 在一個領域需要訪問另一個領域時，使用防腐層
- 例如，訂單領域通過防腐層訪問支付領域

### 3. 共享核心

- 將兩個領域共享的概念放在 `domain.common` 包中
- 例如，共享的 Money 值對象可以放在 `domain.common.valueobject`

## 實施策略

1. **漸進式重構**：
   - 不要一次性重構所有代碼，而是逐步進行
   - 先解決最嚴重的架構違規，如應用層依賴基礎設施層

2. **先修改包結構**：
   - 首先調整包結構，使其符合 DDD 分層架構
   - 然後修改類之間的依賴關係

3. **使用依賴倒置原則**：
   - 在應用層定義接口（端口）
   - 在基礎設施層實現這些接口（適配器）
   - 這樣應用層就不需要直接依賴基礎設施層

4. **重新設計 DTO**：
   - 為每一層定義專用的 DTO
   - 使用映射器在不同層的 DTO 之間轉換

通過這些改進，代碼結構將更加符合 DDD 分層架構的原則，使得 `dddLayeredArchitecture` 測試能夠通過。這不僅會提高代碼的可維護性和可測試性，還會使系統更加模塊化和靈活。