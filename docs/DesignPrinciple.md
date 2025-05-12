# 系統開發設計遵循規範

在本專案中請遵循 ddd tactical design pattern 與分層原則，如同 ../app/src/test/java/solid/humank/genaidemo/architecture 底下的3 個 ArchUnit 測試的要求，來完成本專案的程式碼開發工作。

## Vibe coding

1. 針對目前已經有的 feature (基於BDD gherkin 語法) , 對應 AggregateRoot 來撰寫單元測試; 該測試必須透過 cucumber-jvm 套件來完成單元測試程式碼實作, 不依賴於 spring boot 系列框架
2. 針對 Repository 的實作，需參考 DDD Repository pattern, 以及六邊形架構分層設計來實作，避免將 Aggregate 直接暴露寫入到 jpa 的實作
3. JPA 的實作是使用 Spring Data JPA
4. 針對 Interfaces/web (六邊形架構或者 DDD tactical design pattern 的 presentation layer)，需要針對 controller 撰寫測試程式，這個可以透過 spring boot 框架進行整合測試
5. 針對 application service 也需要撰寫測試, 透過 spring boot 框架進行整合測試


## DesignDetail

### 系統問題與改善策略

#### 1. OrderStatus 枚舉中缺少狀態（PENDING、CONFIRMED、SHIPPING）

我會檢查 OrderStatus 枚舉，並添加缺少的狀態。目前 OrderStatus 枚舉中有 CREATED、SUBMITTED、PAID 等狀態，但缺少 PENDING、CONFIRMED、SHIPPING 等狀態，這導致 Order 聚合根中使用這些狀態時出現編譯錯誤。

改善方案：
- 在 OrderStatus 枚舉中添加缺少的狀態
- 或者修改 Order 聚合根中的代碼，使用已存在的狀態（例如，將 PENDING 改為 SUBMITTED，CONFIRMED 改為 PAID，SHIPPING 改為 SHIPPED）
- 確保狀態轉換邏輯（canTransitionTo 方法）也相應更新

#### 2. 應用層服務中的方法參數和返回值與領域模型不匹配

應用層服務（如 PaymentApplicationService）中的方法參數和返回值與領域模型不匹配，例如：
- UUID 和 PaymentId 之間的轉換問題
- PaymentFailedEvent 構造函數參數不匹配
- 方法調用不存在的 DTO 方法

改善方案：
- 創建適當的轉換方法，將 UUID 轉換為 PaymentId
- 更新 PaymentFailedEvent 構造函數調用，提供正確的參數
- 實現缺少的 DTO 方法，如 PaymentResponseDto.fromDomain()
- 確保 DTO 和領域模型之間的映射正確

#### 3. DTO 類中缺少方法

一些 DTO 類（如 PaymentResponseDto）缺少必要的方法，如 fromDomain()、getId()、getCurrency() 等。

改善方案：
- 為 PaymentResponseDto 實現 fromDomain() 靜態方法，用於從領域模型創建 DTO
- 為 DTO 類添加缺少的 getter 方法
- 確保 DTO 類的結構與領域模型匹配，以便於轉換

#### 4. 其他改進

除了上述直接問題外，我還會考慮以下改進：

- 確保 Repository 適配器正確實現領域儲存庫接口
- 檢查 Mapper 類是否正確處理領域模型和持久化模型之間的轉換
- 確保 JPA 實體類與數據庫表結構匹配
- 檢查 Cucumber 測試步驟定義是否覆蓋所有場景

#### 實施策略

我會採取以下策略來實施這些改進：

1. 首先修復領域層的問題（如 OrderStatus 枚舉）
2. 然後修復基礎設施層的問題（如 Repository 和 Mapper）
3. 接著修復應用層的問題（如服務和 DTO）
4. 最後修復接口層的問題（如控制器和請求/響應對象）

這種自下而上的方法可以確保每一層都建立在穩固的基礎上，並且可以逐步驗證每一層的正確性。

### 詳細改進說明

#### 1. 確保 Repository 適配器正確實現領域儲存庫接口

在 DDD 和六邊形架構中，Repository 適配器是連接領域層和基礎設施層的關鍵組件。我需要確保：

- **完整實現所有方法**：Repository 適配器必須實現領域儲存庫接口中定義的所有方法，如 `save`、`findById`、`findAll` 等。

- **正確的轉換邏輯**：適配器需要正確地將領域模型轉換為持久化模型（JPA 實體），反之亦然。例如，在 `OrderRepositoryAdapter` 中，我們需要確保 `OrderMapper.toJpaEntity()` 和 `OrderMapper.toDomainEntity()` 方法被正確調用。

- **事務管理**：確保適配器正確處理事務，特別是在涉及多個實體的操作中。

- **異常處理**：適當地捕獲和轉換基礎設施層的異常，避免將技術細節洩露到領域層。

- **領域事件處理**：如果領域模型發布了事件，確保適配器能夠正確地處理這些事件，例如在保存聚合根後發布事件。

#### 2. 檢查 Mapper 類是否正確處理領域模型和持久化模型之間的轉換

Mapper 類負責在領域模型和持久化模型之間進行轉換，這是保持領域模型純淨的關鍵。我需要檢查：

- **雙向轉換**：確保 `toJpaEntity()` 和 `toDomainEntity()` 方法能夠正確地雙向轉換所有屬性。

- **值對象轉換**：特別注意值對象（如 Money、OrderId）的轉換，確保它們的語義和不變性在轉換過程中得到保留。

- **集合轉換**：正確處理集合（如訂單項列表）的轉換，包括處理空集合的情況。

- **狀態轉換**：確保枚舉類型（如 OrderStatus）的轉換正確，特別是當 JPA 實體使用字符串存儲狀態時。

- **關聯關係**：處理實體之間的關聯關係，確保它們在轉換過程中得到正確維護。

- **聚合邊界**：確保轉換過程尊重聚合邊界，不會意外地跨越聚合邊界加載數據。

#### 3. 確保 JPA 實體類與數據庫表結構匹配

JPA 實體類是與數據庫直接交互的組件，必須與數據庫表結構匹配。我需要檢查：

- **表名和列名**：確保 `@Table` 和 `@Column` 註解中的名稱與數據庫表和列名匹配。

- **主鍵策略**：確保主鍵生成策略（如 `@Id` 和 `@GeneratedValue`）與數據庫設計一致。

- **關聯關係**：確保 `@OneToMany`、`@ManyToOne` 等關聯關係註解正確反映數據庫表之間的關係。

- **級聯操作**：檢查級聯操作設置（如 `cascade = CascadeType.ALL`）是否符合業務需求。

- **數據類型**：確保 Java 類型與數據庫列類型兼容，特別是對於日期、時間和數字類型。

- **約束條件**：確保 `nullable`、`unique` 等約束條件與數據庫設計一致。

- **索引**：如果需要，添加 `@Index` 註解以優化查詢性能。

從 `JpaOrderEntity` 代碼來看，它使用了 `@Enumerated(EnumType.STRING)` 來存儲 `OrderStatus` 枚舉，這是一個好的做法，因為它使數據庫中的值更具可讀性。但需要確保 `OrderStatus` 枚舉中的值與數據庫中的值匹配。

#### 4. 檢查 Cucumber 測試步驟定義是否覆蓋所有場景

Cucumber 測試是確保領域模型行為正確的重要工具。我需要檢查：

- **場景覆蓋**：確保所有 feature 文件中定義的場景都有對應的步驟定義。

- **步驟實現**：確保每個步驟定義都有正確的實現，並且使用了適當的斷言來驗證結果。

- **多語言支持**：如果需要支持多語言（如中文），確保步驟定義使用了正確的註解（如 `@假如`、`@當`、`@那麼`）。

- **測試隔離**：確保每個測試場景都是獨立的，不會受到其他測試的影響。

- **測試數據**：確保測試使用了適當的測試數據，並且在測試後進行了清理。

- **異常處理**：確保測試正確處理了異常情況，特別是對於應該拋出異常的場景。

- **邊界條件**：確保測試覆蓋了邊界條件和極端情況。

這些詳細的檢查點將幫助我們確保我們的代碼符合 DDD 戰術設計模式和分層原則，並且能夠正確地實現業務需求。特別是在處理領域模型和持久化模型之間的轉換時，我們需要特別小心，以確保領域模型的完整性和純淨性。