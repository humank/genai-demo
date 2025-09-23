
# Design

在This project中請遵循 DDD 戰術Design Pattern與分層原則，如同 `../app/src/test/java/solid/humank/genaidemo/architecture` 底下的 3 個 ArchUnit 測試的要求，來完成This project的程式碼開發工作。

## Design

### Layered Architecture

1. **Domain Layer (Domain Layer)**
   - 包含業務核心邏輯和規則
   - 不依賴於其他層
   - 包含Aggregate Root、Entity、Value Object、Domain Event、Domain Service和領域異常

2. **Application Layer (Application Layer)**
   - 協調領域對象完成用戶用例
   - 只依賴於Domain Layer
   - 包含應用服務、DTO、Command和查詢對象

3. **Infrastructure Layer (Infrastructure Layer)**
   - 提供技術實現
   - 依賴於Domain Layer，實現Domain Layer定義的接口
   - 包含儲存庫實現、External SystemAdapter、ORM 映射等

4. **Interface Layer (Interfaces Layer)**
   - 處理用戶交互
   - 只依賴於Application Layer
   - 包含控制器、視圖模型、請求/響應對象等

### Design

1. **Aggregate Root (Aggregate Root)**
   - 必須位於 `domain.*.model.aggregate` 包中
   - 使用 `@AggregateRoot` 註解標記
   - 控制對內部Entity的訪問
   - 確保業務不變性

2. **Entity (Entity)**
   - 必須位於 `domain.*.model.entity` 包中
   - 使用 `@Entity` 註解標記
   - 具有唯一標識
   - 可變但保持身份一致性

3. **Value Object (Value Object)**
   - 必須位於 `domain.*.model.valueobject` 包中
   - 使用 `@ValueObject` 註解標記
   - 不可變
   - 沒有唯一標識，通過屬性值比較相等性

4. **Domain Event (Domain Event)**
   - 必須位於 `domain.*.events` 包中
   - 實現 `DomainEvent` 接口
   - 不可變，記錄已發生的業務事件

5. **Domain Service (Domain Service)**
   - 必須位於 `domain.*.service` 包中
   - 無狀態，處理跨Aggregate的業務邏輯

6. **儲存庫 (Repository)**
   - 接口必須位於 `domain.*.repository` 包中
   - 實現必須位於 `infrastructure.persistence` 包中
   - 操作Aggregate Root

## Design

### Testing

This project建立了完整的測試輔助工具生態系統，位於 `app/src/test/java/solid/humank/genaidemo/testutils/` 目錄：

1. **測試資料建構器 (Test Data Builders)**
   - 使用Builder模式簡化測試資料創建
   - 支援鏈式呼叫和預設值設定
   - 提供領域特定的建構方法

2. **場景處理器 (Scenario Handlers)**
   - 處理複雜的測試場景邏輯
   - 使用Policy模式處理不同情境
   - 避免在測試中使用條件邏輯

3. **自定義匹配器 (Custom Matchers)**
   - 提供更具表達性的測試斷言
   - 改善測試失敗時的錯誤訊息
   - 支援領域特定的驗證邏輯

### Testing

1. **3A原則 (Arrange-Act-Assert)**
   - 每個測試方法都必須有清晰的三個區段
   - Arrange: 準備測試資料和Environment
   - Act: 執行被測試的操作
   - Assert: 驗證結果

2. **無條件邏輯**
   - 測試中不應包含if-else語句
   - 使用場景處理器處理複雜邏輯
   - 保持測試的簡潔性和可讀性

3. **測試獨立性**
   - 每個測試都應該是獨立且可重複的
   - 不依賴其他測試的執行順序
   - 使用適當的測試資料隔離機制

4. **描述性命名**
   - 使用清晰的測試方法名稱
   - 使用@DisplayName提供詳細描述
   - 測試名稱應該說明測試的目的和預期結果

5. **測試分類**
   - 使用測試標籤註解進行分類：
     - `@UnitTest`: 快速執行的Unit Test
     - `@IntegrationTest`: 需要外部依賴的Integration Test
     - `@SlowTest`: 執行時間較長的測試
     - `@BddTest`: Behavior-Driven Development (BDD)測試

### Testing

1. **步驟定義簡潔性**
   - 每個步驟定義只負責一個明確的操作
   - 不包含複雜的條件邏輯
   - 使用場景處理器處理複雜場景

2. **測試資料管理**
   - 使用測試建構器創建測試資料
   - 使用TestFixtures提供常用資料
   - 避免硬編碼的測試資料

3. **異常處理**
   - 使用統一的異常處理機制
   - 通過TestExceptionHandler捕獲和驗證異常
   - 提供清晰的異常驗證方法
   - 實現 `DomainEvent` 接口
   - 不可變
   - 記錄領域中發生的重要事件

5. **儲存庫 (Repository)**
   - 接口必須位於 `domain.*.repository` 包中
   - 實現必須位於 `infrastructure.*.persistence` 包中
   - 操作Aggregate Root
   - 提供持久化和查詢功能

6. **Domain Service (Domain Service)**
   - 必須位於 `domain.*.service` 包中
   - 使用 `@DomainService` 註解標記
   - 無狀態
   - 處理跨Aggregate的業務邏輯

7. **規格 (Specification)**
   - 必須位於 `domain.*.specification` 包中
   - 實現 `Specification` 接口
   - 封裝複雜的業務規則和查詢條件

8. **Anti-Corruption Layer (Anti-Corruption Layer)**
   - 必須位於 `infrastructure.*.acl` 包中
   - 隔離External System
   - 翻譯外部模型到內部模型

## Testing

### Testing

基於 Cucumber-JVM 針對Aggregate Root撰寫Unit Test，不依賴 Spring Boot 框架。

- **測試範圍**：Aggregate Root、Entity、Value Object、Domain Service
- **測試工具**：Cucumber-JVM、JUnit 5
- **測試目標**：驗證業務規則和不變性

#### Testing

1. **Feature 文件**
   - 使用 Gherkin 語法描述業務場景
   - 位於 `src/test/resources/features` 目錄
   - 按子領域組織，如 `inventory/inventory_management.feature`

2. **步驟定義**
   - 位於 `src/test/java/solid/humank/genaidemo/bdd` 目錄
   - 按子領域組織，如 `inventory/InventoryStepDefinitions.java`
   - 實現 Feature 文件中的步驟

3. **測試隔離**
   - 每個測試場景應該是獨立的
   - 使用適當的測試數據
   - 在測試後進行清理

### Testing

對Application Layer、Infrastructure Layer和Interface Layer進行測試，可啟動 Spring Boot 進行測試。

- **測試範圍**：應用服務、儲存庫實現、控制器
- **測試工具**：Spring Boot Test、MockMvc、TestContainers
- **測試目標**：驗證組件集成和端到端流程

#### Testing

- 驗證用例協調邏輯
- 確保正確調用領域對象
- 檢查事務管理和事件發布

#### Testing

- 驗證 CRUD 操作
- 確保查詢條件正確
- 檢查樂觀鎖定和並發控制

#### Testing

- 驗證 HTTP 請求處理
- 確保正確的狀態碼和響應格式
- 檢查輸入驗證和錯誤處理

## Guidelines

### 1. 確保 Repository Adapter正確實現領域儲存庫接口

- **完整實現所有方法**：Repository Adapter必須實現領域儲存庫接口中定義的所有方法，如 `save`、`findById`、`findAll` 等。

- **正確的轉換邏輯**：Adapter需要正確地將領域模型轉換為持久化模型（JPA Entity），反之亦然。例如，在 `OrderRepositoryAdapter` 中，需要確保 `OrderMapper.toJpaEntity()` 和 `OrderMapper.toDomainEntity()` 方法被正確調用。

- **事務管理**：確保Adapter正確處理事務，特別是在涉及多個Entity的操作中。

- **異常處理**：適當地捕獲和轉換Infrastructure Layer的異常，避免將技術細節洩露到Domain Layer。

- **Domain Event處理**：如果領域模型發布了事件，確保Adapter能夠正確地處理這些事件，例如在保存Aggregate Root後發布事件。

### 2. 檢查 Mapper 類是否正確處理領域模型和持久化模型之間的轉換

- **雙向轉換**：確保 `toJpaEntity()` 和 `toDomainEntity()` 方法能夠正確地雙向轉換所有屬性。

- **Value Object轉換**：特別注意Value Object（如 Money、OrderId）的轉換，確保它們的語義和不變性在轉換過程中得到保留。

- **集合轉換**：正確處理集合（如訂單項列表）的轉換，包括處理空集合的情況。

- **狀態轉換**：確保枚舉類型（如 OrderStatus）的轉換正確，特別是當 JPA Entity使用字符串存儲狀態時。

- **關聯關係**：處理Entity之間的關聯關係，確保它們在轉換過程中得到正確維護。

- **Aggregate邊界**：確保轉換過程尊重Aggregate邊界，不會意外地跨越Aggregate邊界加載數據。

### 3. 確保 JPA Entity類與數據庫表結構匹配

- **表名和列名**：確保 `@Table` 和 `@Column` 註解中的名稱與數據庫表和列名匹配。

- **主鍵Policy**：確保主鍵生成Policy（如 `@Id` 和 `@GeneratedValue`）與數據庫設計一致。

- **關聯關係**：確保 `@OneToMany`、`@ManyToOne` 等關聯關係註解正確反映數據庫表之間的關係。

- **級聯操作**：檢查級聯操作設置（如 `cascade = CascadeType.ALL`）是否符合業務需求。

- **數據類型**：確保 Java 類型與數據庫列類型兼容，特別是對於日期、時間和數字類型。

- **Constraint條件**：確保 `nullable`、`unique` 等Constraint條件與數據庫設計一致。

- **索引**：如果需要，添加 `@Index` 註解以優化查詢Performance。

### Testing

- **場景覆蓋**：確保所有 feature 文件中定義的場景都有對應的步驟定義。

- **步驟實現**：確保每個步驟定義都有正確的實現，並且使用了適當的斷言來驗證結果。

- **測試隔離**：確保每個測試場景都是獨立的，不會受到其他測試的影響。

- **測試數據**：確保測試使用了適當的測試數據，並且在測試後進行了清理。

- **異常處理**：確保測試正確處理了異常情況，特別是對於應該拋出異常的場景。

- **邊界條件**：確保測試覆蓋了邊界條件和極端情況。

## 常見問題與解決方案

### Testing

當 Cucumber 測試失敗時，可能的原因和解決方案：

- **步驟定義不匹配**：確保步驟定義與 feature 文件中的步驟完全匹配，包括空格和標點符號。
- **測試數據問題**：確保測試數據正確設置，特別是在多個步驟之間共享數據時。
- **斷言失敗**：檢查預期值和實際值，可能需要調整業務邏輯或測試預期。
- **狀態管理問題**：確保測試場景之間的狀態正確重置，避免測試相互干擾。

### 2. 領域模型與持久化模型轉換問題

- **數據丟失**：確保所有必要的屬性都在轉換過程中得到處理。
- **類型不匹配**：處理不同類型之間的轉換，如字符串到枚舉、字符串到日期等。
- **循環依賴**：處理Entity之間的循環引用，可能需要使用延遲加載或分離轉換過程。

### 3. Aggregate邊界問題

- **過大的Aggregate**：將過大的Aggregate拆分為多個較小的Aggregate，使用Aggregate間的引用。
- **Aggregate間一致性**：使用Domain Event或應用服務協調多個Aggregate之間的一致性。
- **查詢Performance**：對於複雜查詢，考慮使用專用的查詢模型或 Command Query Responsibility Segregation (Command Query Responsibility Segregation (CQRS)) 模式。

## conclusion

遵循這些Design Principle和測試Policy，可以幫助我們構建一個健壯、可維護和可測試的系統。DDD 戰術Design Pattern和Layered Architecture提供了一個清晰的結構，使我們能夠專注於業務邏輯，同時保持技術實現的靈活性。Cucumber BDD 測試確保我們的代碼符合業務需求，而Integration Test確保各個組件能夠正確地協同工作。
