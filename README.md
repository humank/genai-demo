# GenAI Demo

這是一個展示領域驅動設計 (DDD) 分層架構的示例專案。

## 專案結構

專案遵循 DDD 分層架構，包含以下層次：

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
│   │   ├── repository/               # 儲存庫接口 (OrderRepository)
│   │   └── validation/               # 領域驗證 (OrderValidator)
│   └── payment/                      # 支付領域
│       ├── model/                    # 支付領域模型
│       │   ├── aggregate/            # 聚合根 (Payment)
│       │   ├── entity/               # 實體
│       │   ├── valueobject/          # 值對象 (PaymentStatus)
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

## 架構原則

1. **領域層 (Domain Layer)**
   - 包含業務邏輯和規則
   - 不依賴其他層
   - 包含實體、值對象、聚合根、領域事件、領域服務等

2. **應用層 (Application Layer)**
   - 協調領域對象完成用例
   - 只依賴領域層
   - 包含應用服務、DTO、端口等

3. **基礎設施層 (Infrastructure Layer)**
   - 提供技術實現
   - 依賴領域層和應用層
   - 包含儲存庫實現、外部服務適配器、防腐層等

4. **介面層 (Interface Layer)**
   - 處理用戶交互
   - 依賴應用層
   - 包含控制器、視圖模型等

## 運行專案

```bash
./gradlew bootRun
```

## 測試

```bash
./gradlew test
```

## 架構測試

專案包含架構測試，確保代碼遵循 DDD 分層架構的設計原則：

```bash
./gradlew test --tests "solid.humank.genaidemo.architecture.ddd.DddLayeredArchitectureTest"
```