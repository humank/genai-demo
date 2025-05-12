# 從混亂到整潔：DDD 與六角形架構重構之旅

本文檔記錄了將一個混亂的程式碼結構逐步重構為符合領域驅動設計 (DDD) 和六角形架構 (Hexagonal Architecture) 的過程。這個重構過程不僅改善了代碼的組織結構，還提高了系統的可維護性、可測試性和靈活性。

## 初始狀態：混亂的程式碼結構

最初，專案存在以下問題：

1. **層之間的依賴混亂**：
   - 應用層直接依賴基礎設施層的類
   - 領域邏輯散布在不同層次中
   - 缺乏明確的架構邊界

2. **包結構不一致**：
   - 相似功能的類分散在不同的包中
   - 命名不一致，難以理解類的職責
   - 缺乏模塊化和內聚性

3. **業務邏輯與技術細節混合**：
   - 領域模型直接依賴於持久化框架
   - 業務規則與 UI 邏輯混合
   - 難以單獨測試核心業務邏輯

## 重構目標

我們的重構目標是建立一個符合以下原則的架構：

1. **清晰的分層架構**：
   - 領域層 (Domain Layer)：包含業務核心邏輯和規則
   - 應用層 (Application Layer)：協調領域對象完成用戶用例
   - 基礎設施層 (Infrastructure Layer)：提供技術實現
   - 介面層 (Interfaces Layer)：處理用戶交互

2. **依賴方向的控制**：
   - 內層不依賴外層
   - 使用依賴倒置原則解決跨層依賴

3. **領域驅動設計的戰術模式**：
   - 聚合根、實體、值對象的正確使用
   - 領域事件、領域服務、儲存庫等模式的應用
   - 防腐層隔離外部系統

## 重構步驟

### 第一階段：建立基礎架構

1. **定義清晰的包結構**：
   ```
   solid.humank.genaidemo/
   ├── domain/                           # 領域層
   │   ├── common/                       # 共享領域概念
   │   │   ├── annotations/              # DDD 相關註解
   │   │   ├── events/                   # 領域事件基礎設施
   │   │   ├── specification/            # 規格模式
   │   │   ├── repository/               # 儲存庫接口
   │   │   └── valueobject/              # 共享值對象
   │   ├── order/                        # 訂單領域
   │   │   ├── model/                    # 訂單領域模型
   │   │   │   ├── aggregate/            # 聚合根 (Order)
   │   │   │   ├── events/               # 領域事件
   │   │   │   ├── factory/              # 工廠
   │   │   │   ├── policy/               # 策略
   │   │   │   ├── service/              # 領域服務
   │   │   │   └── specification/        # 規格
   │   │   ├── repository/               # 儲存庫接口
   │   │   └── validation/               # 領域驗證
   │   ├── payment/                      # 支付領域
   │   │   ├── model/                    # 支付領域模型
   │   │   │   ├── aggregate/            # 聚合根 (Payment)
   │   │   │   ├── events/               # 領域事件
   │   │   │   ├── service/              # 領域服務
   │   │   │   └── valueobject/          # 值對象
   │   │   ├── events/                   # 支付領域事件
   │   │   └── repository/               # 儲存庫接口
   │   ├── inventory/                    # 庫存領域
   │   │   └── model/                    # 庫存領域模型
   │   │       ├── aggregate/            # 聚合根 (Inventory)
   │   │       └── valueobject/          # 值對象
   │   ├── delivery/                     # 配送領域
   │   │   └── model/                    # 配送領域模型
   │   │       ├── aggregate/            # 聚合根 (Delivery)
   │   │       └── valueobject/          # 值對象
   │   ├── notification/                 # 通知領域
   │   │   └── model/                    # 通知領域模型
   │   │       ├── aggregate/            # 聚合根 (Notification)
   │   │       └── valueobject/          # 值對象
   │   └── workflow/                     # 工作流領域
   │       └── model/                    # 工作流領域模型
   │           ├── aggregate/            # 聚合根 (OrderWorkflow)
   │           └── valueobject/          # 值對象
   ├── application/                      # 應用層
   │   ├── common/                       # 共享應用服務
   │   │   └── valueobject/              # 應用層值對象
   │   ├── order/                        # 訂單應用服務
   │   │   ├── dto/                      # DTO
   │   │   │   ├── response/             # 響應 DTO
   │   │   │   └── 各種命令和請求 DTO
   │   │   ├── port/                     # 端口
   │   │   │   ├── incoming/             # 入站端口 (OrderManagementUseCase)
   │   │   │   └── outgoing/             # 出站端口
   │   │   └── service/                  # 應用服務 (OrderApplicationService)
   │   ├── payment/                      # 支付應用服務
   │   │   ├── dto/                      # DTO
   │   │   ├── port/                     # 端口
   │   │   │   ├── incoming/             # 入站端口
   │   │   │   └── outgoing/             # 出站端口
   │   │   └── service/                  # 應用服務
   │   ├── inventory/                    # 庫存應用服務
   │   │   └── port/                     # 端口
   │   │       └── incoming/             # 入站端口
   │   ├── logistics/                    # 物流應用服務
   │   │   └── port/                     # 端口
   │   │       └── incoming/             # 入站端口
   │   └── notification/                 # 通知應用服務
   │       └── port/                     # 端口
   │           └── incoming/             # 入站端口
   ├── infrastructure/                   # 基礎設施層
   │   ├── common/                       # 共享基礎設施
   │   │   └── event/                    # 事件基礎設施
   │   ├── order/                        # 訂單基礎設施
   │   │   ├── acl/                      # 防腐層
   │   │   ├── config/                   # 配置
   │   │   ├── external/                 # 外部服務適配器
   │   │   └── persistence/              # 持久化
   │   │       ├── adapter/              # 儲存庫適配器
   │   │       ├── entity/               # JPA 實體
   │   │       ├── mapper/               # 映射器
   │   │       └── repository/           # JPA 儲存庫
   │   ├── payment/                      # 支付基礎設施
   │   │   ├── config/                   # 配置
   │   │   ├── external/                 # 外部服務適配器
   │   │   └── persistence/              # 持久化
   │   │       ├── adapter/              # 儲存庫適配器
   │   │       ├── entity/               # JPA 實體
   │   │       ├── mapper/               # 映射器
   │   │       └── repository/           # JPA 儲存庫
   │   ├── config/                       # 全局配置
   │   ├── external/                     # 共享外部服務
   │   └── saga/                         # Saga 協調器
   │       └── definition/               # Saga 定義
   ├── interfaces/                       # 介面層
   │   └── web/                          # Web 介面
   │       ├── order/                    # 訂單控制器
   │       │   ├── dto/                  # Web DTO
   │       │   └── OrderController.java
   │       └── payment/                  # 支付控制器
   │           ├── dto/                  # Web DTO
   │           └── PaymentController.java
   ├── exceptions/                       # 全局異常處理
   ├── utils/                            # 工具類
   └── GenAiDemoApplication.java         # 應用程序入口
   ```

2. **建立架構測試**：
   - 創建 `DddArchitectureTest` 確保分層架構的依賴關係正確
   - 創建 `DddTacticalPatternsTest` 確保正確實現 DDD 戰術模式
   - 創建 `PackageStructureTest` 確保包結構符合規範

### 第二階段：領域模型重構

1. **識別聚合根和邊界**：
   - 將 `Order` 重構為聚合根，控制對 `OrderItem` 的訪問
   - 將 `Payment` 重構為獨立的聚合根
   - 識別其他聚合根，如 `Inventory`、`Delivery`、`Notification` 和 `OrderWorkflow`

2. **實現值對象**：
   - 創建不可變的值對象，如 `OrderId`、`Money`、`Address`
   - 確保值對象封裝了相關的業務規則
   - 將共享的值對象放在 `domain.common.valueobject` 包中

3. **定義領域事件**：
   - 創建領域事件，如 `OrderCreatedEvent`、`PaymentProcessedEvent`
   - 實現 `DomainEvent` 接口
   - 使用事件來解耦不同領域之間的交互

4. **設計儲存庫接口**：
   - 在領域層定義儲存庫接口，如 `OrderRepository`
   - 確保儲存庫操作的是聚合根
   - 將儲存庫接口放在對應領域的 `repository` 包中

### 第三階段：應用層重構

1. **實現應用服務**：
   - 創建 `OrderApplicationService` 和 `PaymentApplicationService`
   - 應用服務協調領域對象完成用例
   - 確保應用服務不包含業務邏輯

2. **定義命令和查詢**：
   - 創建命令對象，如 `CreateOrderCommand`、`ProcessPaymentCommand`
   - 創建查詢對象和響應 DTO
   - 將 DTO 放在對應應用服務的 `dto` 包中

3. **定義端口**：
   - 創建入站端口（用例接口），如 `OrderManagementUseCase`
   - 創建出站端口，如 `OrderPersistencePort`、`PaymentServicePort`
   - 將端口放在對應應用服務的 `port` 包中

### 第四階段：基礎設施層重構

1. **實現儲存庫**：
   - 創建儲存庫實現，如 `OrderRepositoryImpl`
   - 使用 JPA 實現持久化
   - 創建映射器，如 `OrderMapper`，在領域模型和 JPA 實體之間轉換

2. **實現適配器**：
   - 創建外部系統適配器，如 `ExternalPaymentAdapter`、`LogisticsServiceAdapter`
   - 實現防腐層，如 `LogisticsAntiCorruptionLayer`
   - 將適配器放在對應領域的 `infrastructure` 包中

3. **配置依賴注入**：
   - 設置 Spring 配置，將接口與實現綁定
   - 確保依賴注入遵循依賴倒置原則
   - 創建配置類，如 `OrderJpaConfig`、`PaymentJpaConfig`

### 第五階段：介面層重構

1. **實現控制器**：
   - 創建 REST 控制器，如 `OrderController`、`PaymentController`
   - 控制器只負責處理 HTTP 請求和響應
   - 將控制器放在 `interfaces.web` 包中

2. **定義 API 模型**：
   - 創建請求和響應模型，如 `CreateOrderRequest`、`OrderResponse`
   - 使用映射器在 API 模型和應用層 DTO 之間轉換
   - 將 API 模型放在對應控制器的 `dto` 包中

## 重構後的架構

重構完成後，我們得到了一個符合 DDD 和六角形架構的系統：

1. **領域層**：
   - 包含純粹的業務邏輯，不依賴於外部技術
   - 使用聚合根、實體、值對象等模式表達業務概念
   - 通過領域事件實現領域間的鬆耦合通信

2. **應用層**：
   - 協調領域對象完成用例
   - 定義端口（接口）與外部世界交互
   - 不包含業務規則，只負責協調

3. **基礎設施層**：
   - 實現領域層定義的儲存庫接口
   - 提供與外部系統的集成
   - 通過防腐層隔離外部系統的影響

4. **介面層**：
   - 處理用戶交互，如 HTTP 請求
   - 將請求轉換為應用層可以處理的命令或查詢
   - 將應用層的響應轉換為適合 UI 的格式

## 架構測試

為了確保架構的完整性，我們實現了三種架構測試：

1. **DddArchitectureTest**：
   - 確保分層架構的依賴關係正確
   - 驗證內層不依賴外層
   - 檢查各層的職責邊界

2. **DddTacticalPatternsTest**：
   - 確保正確實現 DDD 戰術模式
   - 驗證值對象的不可變性
   - 檢查聚合根、實體、領域事件等的正確使用

3. **PackageStructureTest**：
   - 確保包結構符合規範
   - 驗證類位於正確的包中
   - 檢查命名一致性

## 重構效益

這次重構帶來了以下效益：

1. **提高可維護性**：
   - 清晰的分層架構使代碼更易於理解和維護
   - 明確的職責分配減少了代碼的混亂

2. **增強可測試性**：
   - 業務邏輯與技術細節分離，便於單元測試
   - 使用依賴倒置原則，便於模擬外部依賴

3. **提升靈活性**：
   - 可以輕鬆替換技術實現，如數據庫或 UI 框架
   - 領域模型的獨立性使其更容易適應業務變化

4. **改善團隊協作**：
   - 明確的架構邊界使不同團隊可以並行工作
   - 共享的領域語言提高了溝通效率

## 持續改進

重構是一個持續的過程，未來可以考慮以下改進：

1. **引入 CQRS 模式**：
   - 分離命令和查詢職責
   - 優化讀寫性能

2. **實現事件溯源**：
   - 存儲事件而非狀態
   - 提供完整的審計和歷史記錄

3. **微服務拆分**：
   - 基於領域邊界拆分為微服務
   - 使用事件驅動架構實現服務間通信

4. **增強可觀測性**：
   - 實現分布式追蹤
   - 添加更詳細的監控和日誌

## 結論

通過這次重構，我們將一個混亂的程式碼結構轉變為一個符合 DDD 和六角形架構的系統。這不僅提高了代碼質量，還使系統更加靈活、可維護和可測試。重構過程中的經驗和教訓也為團隊提供了寶貴的學習機會，幫助我們在未來的項目中更好地應用這些架構原則。