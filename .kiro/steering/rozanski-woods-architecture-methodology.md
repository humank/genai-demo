---
inclusion: always
---

# Rozanski & Woods 架構方法論 Steering 規則

## 架構視點強制檢查

### 每個新功能必須完成以下視點檢查：

#### 功能視點

- [ ] 聚合根邊界明確定義
- [ ] 領域服務職責清晰
- [ ] 用例實現符合 DDD 戰術模式

#### 資訊視點  

- [ ] 領域事件設計完整
- [ ] 資料一致性策略明確
- [ ] 事件溯源考量完備

#### 並發視點

- [ ] 異步處理策略文檔化
- [ ] 事務邊界明確定義
- [ ] 並發衝突處理機制

#### 開發視點

- [ ] 模組依賴符合六角架構
- [ ] 測試策略包含所有層次
- [ ] 建置腳本更新完成

#### 部署視點

- [ ] CDK 基礎設施更新
- [ ] 環境配置變更記錄
- [ ] 部署策略影響評估

#### 運營視點

- [ ] 監控指標定義
- [ ] 日誌結構設計
- [ ] 故障處理程序

## 品質屬性場景要求

### 每個用戶故事必須包含至少一個品質屬性場景

#### 場景格式：來源 → 刺激 → 環境 → 產出物 → 回應 → 回應測量

#### 量化指標要求

- 性能場景：具體時間、吞吐量或資源使用指標
- 安全場景：通過 CDK Nag 規則驗證
- 可用性場景：包含 RTO 和 RPO
- 可擴展性場景：定義負載增長和擴展策略

## 架構合規性規則

### 強制 ArchUnit 規則

```java
// Domain 層依賴限制
@ArchTest
static final ArchRule domainLayerRules = classes()
    .that().resideInAPackage("..domain..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage("..domain..", "java..", "org.springframework..");

// 聚合根規則
@ArchTest  
static final ArchRule aggregateRootRules = classes()
    .that().areAnnotatedWith(AggregateRoot.class)
    .should().implement(AggregateRootInterface.class);

// 事件處理器規則
@ArchTest
static final ArchRule eventHandlerRules = classes()
    .that().areAnnotatedWith(Component.class)
    .and().haveSimpleNameEndingWith("EventHandler")
    .should().beAnnotatedWith(TransactionalEventListener.class);

// 值對象規則
@ArchTest
static final ArchRule valueObjectRules = classes()
    .that().areAnnotatedWith(ValueObject.class)
    .should().beRecords();
```

## ADR 必要內容

### 每個 ADR 必須包含：

- 利害關係人影響分析
- 影響半徑評估 (Local/Bounded Context/System/Enterprise)
- 風險評估 (高/中/低)
- 回滾策略和觸發條件
- 遷移路徑 (階段一/二/三)

## 可觀測性要求

### 新功能強制要求

- 每個聚合根必須有對應業務指標
- 每個用例必須有執行追蹤和性能指標
- 每個領域事件必須有發布和處理指標
- 關鍵路徑必須有監控和告警

## 四大觀點檢查清單

### 安全性觀點

- [ ] API 端點通過 CDK Nag 檢查
- [ ] 敏感資料加密存儲和傳輸
- [ ] 身份認證和授權機制
- [ ] 安全事件日誌和監控

### 性能與可擴展性觀點

- [ ] 關鍵路徑性能基準測試 (< 2秒)
- [ ] 資料庫查詢優化和索引策略
- [ ] 快取策略實施
- [ ] 水平擴展能力驗證

### 可用性與韌性觀點

- [ ] 健康檢查端點實施
- [ ] 故障恢復和重試機制
- [ ] 斷路器模式實施
- [ ] 災難恢復計劃和測試

### 演進性觀點

- [ ] 介面向後相容性保證
- [ ] 版本管理策略實施
- [ ] 模組化和鬆耦合設計
- [ ] 重構安全性保證 (測試覆蓋)

## 並發策略要求

### 異步處理設計必須明確：

- 事件處理順序依賴
- 事務邊界和一致性保證
- 並發衝突檢測和處理機制
- 死鎖預防和檢測策略

## 韌性模式強制應用

### 外部服務調用必須實施：

- 斷路器模式
- 重試機制 (最多3次，指數退避)
- 降級策略
- 死信佇列處理

### 關鍵業務流程必須有：

- 故障恢復時間測試
- 監控和告警配置
- 運營手冊更新

## 技術演進標準

### 新技術引入必須滿足：

- [ ] 技術成熟度達到"成長"階段以上
- [ ] 完整文檔和社群支援
- [ ] 團隊學習和維護能力
- [ ] 遷移風險可控且有回滾計劃

### 版本升級要求：

- 關鍵依賴升級必須有自動化測試覆蓋
- 主要版本升級必須在測試環境驗證
- 遺留技術淘汰必須有明確時間表

## 合規性監控指標

- 視點覆蓋率：100%
- 品質屬性場景覆蓋率：100%
- ArchUnit 測試通過率：100%
- 架構債務趨勢：持續下降
