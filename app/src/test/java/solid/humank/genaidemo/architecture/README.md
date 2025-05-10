# 架構測試說明

本目錄包含用於確保專案架構符合 DDD 最佳實踐的測試。

## 包結構規則

1. `solid.humank.genaidemo.domain.common` - 通用和共用的物件
2. `solid.humank.genaidemo.domain.order` 和 `solid.humank.genaidemo.domain.payment` - 子領域
   - 子領域下的 `model` 包含 DDD 戰術設計中的領域模型層元素

## 測試內容

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

## 靈活性

測試規則允許一些例外情況，例如：

- 通用註解類不需要遵循包結構規則
- 共用物件可以位於 `domain.common` 包中
- 值對象可以位於 `common.valueobject` 或 `model.valueobject` 包中

這些靈活性使得架構測試能夠適應實際開發需求，同時確保整體架構符合 DDD 最佳實踐。