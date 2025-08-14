# 架構測試說明

本目錄包含用於確保專案架構符合 DDD 最佳實踐的測試。

## 包結構規則

1. `solid.humank.genaidemo.domain.common` - 通用和共用的物件
2. `solid.humank.genaidemo.domain.order` 和 `solid.humank.genaidemo.domain.payment` - 子領域
   - 子領域下的 `model` 包含 DDD 戰術設計中的領域模型層元素

## 測試內容

### DddArchitectureTest

確保專案遵循 DDD 分層架構的設計原則，包括：

1. **分層架構驗證**：

   - 確保遵循分層架構依賴方向
   - 領域層不依賴其他層
   - 應用層不依賴基礎設施層和介面層
   - 介面層不直接依賴基礎設施層和領域層

2. **組件位置驗證**：
   - 控制器應該位於介面層
   - 應用服務應該位於應用層
   - 儲存庫實現應該位於基礎設施層

### DddTacticalPatternsTest

確保正確實現 DDD 戰術模式，包括：

1. **領域模型驗證**：

   - 值對象應該是不可變的
   - 實體應該有唯一標識
   - 聚合根應該控制其內部實體的訪問
   - 領域事件應該是不可變的

2. **設計模式驗證**：
   - 儲存庫應該操作聚合根
   - 工廠應該創建聚合或複雜值對象
   - 領域服務應該是無狀態的
   - 規格應該實現 Specification 接口

### PackageStructureTest

確保專案的包結構符合 DDD 最佳實踐，包括：

1. **領域模型組織**：

   - 聚合根位於 `model.aggregate` 包中
   - 實體位於 `model.entity` 包中
   - 值對象位於 `common.valueobject` 或 `model.valueobject` 包中
   - 領域事件位於 `events` 或 `model.events` 包中
   - 領域服務位於 `service` 或 `model.service` 包中
   - 儲存庫接口位於 `repository` 包中
   - 工廠位於 `factory` 或 `model.factory` 包中
   - 規格位於 `specification` 或 `model.specification` 包中

2. **子領域模型結構**：

   - 子領域的模型元素位於 `model` 包中
   - 子領域的聚合根位於 `model.aggregate` 包中

3. **應用層組織**：

   - 應用服務位於 `application.service` 包中
   - DTO 位於 `application.dto` 包中
   - 端口位於 `application.port` 包中

4. **基礎設施層組織**：

   - 儲存庫實現位於 `infrastructure.persistence` 包中
   - 防腐層位於 `infrastructure.acl` 包中
   - 外部系統適配器位於 `infrastructure.external` 或 `infrastructure.*.external` 包中
   - Saga 位於 `infrastructure.saga` 包中

5. **介面層組織**：
   - 控制器位於 `interfaces.web` 包中
   - 請求/響應 DTO 位於 `interfaces.web.dto` 包中

### PromotionArchitectureTest

確保促銷模組遵循架構規範，包括：

1. **促銷模組架構驗證**：
   - 確保促銷相關類別位於正確的包結構中
   - 驗證促銷模組的依賴關係

## 測試品質改善

架構測試本身已經具有良好的品質，但在測試輔助工具改善過程中也受益：

1. **測試分類**：可以使用@UnitTest 標籤進行分類
2. **測試文檔**：改善了測試說明文檔的完整性
3. **測試一致性**：與其他測試保持一致的命名和結構規範

## 靈活性

測試規則允許一些例外情況，例如：

- 通用註解類不需要遵循包結構規則
- 共用物件可以位於 `domain.common` 包中
- 值對象可以位於 `common.valueobject` 或 `model.valueobject` 包中
- 測試輔助工具位於 `testutils` 包中，不受架構規則限制

這些靈活性使得架構測試能夠適應實際開發需求，同時確保整體架構符合 DDD 最佳實踐。

## 運行架構測試

```bash
# 運行所有架構測試
./gradlew testArchitecture

# 運行特定的架構測試
./gradlew test --tests "solid.humank.genaidemo.architecture.*"

```
