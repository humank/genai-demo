---
inclusion: always
---

# Rozanski & Woods 架構方法論 Steering 規則

## 📚 方法論概述

本 steering 基於 Nick Rozanski 和 Eóin Woods 的《Software Systems Architecture: Working With Stakeholders Using Viewpoints and Perspectives》第二版，將架構視點 (Viewpoints) 和觀點 (Perspectives) 方法論整合到我們的 DDD + 六角架構開發流程中。

**核心原則**: 系統性架構思考、利害關係人導向、品質屬性驅動、多視點協調

## 🏗️ 基礎架構模式與設計原則

### DDD 戰術模式

#### 聚合根設計

- 使用 `@AggregateRoot` 註解
- 實現 `AggregateRootInterface`
- 負責收集領域事件
- 維護業務不變性

#### 值對象實現

- 使用不可變 Records
- 添加 `@ValueObject` 註解
- 包含驗證邏輯
- 提供靜態工廠方法

#### 領域事件模式

- Records 實現 `DomainEvent` 介面
- 聚合根收集，應用服務發布
- 事件處理器在基礎設施層
- 支持事件溯源和 CQRS

#### 規格模式 (Specification)

- 封裝複雜業務規則
- 使用 `@Specification` 註解
- 支持組合和重用
- 分離查詢條件邏輯

#### 策略模式 (Policy)

- 封裝業務決策邏輯
- 使用 `@Policy` 註解
- 支持運行時策略切換
- 處理可變業務規則

### 六角架構實現

#### 端口與適配器

- **主端口**: 用例介面 (application layer)
- **次端口**: 倉儲介面 (domain layer)
- **主適配器**: REST 控制器 (interfaces layer)
- **次適配器**: JPA 倉儲 (infrastructure layer)

#### 依賴反轉

```java
// Domain 定義介面
public interface CustomerRepository {
    void save(Customer customer);
}

// Infrastructure 實現介面
@Repository
public class JpaCustomerRepository implements CustomerRepository {
    // JPA 實現
}
```

#### 分層依賴規則

- Interfaces → Application → Domain
- Infrastructure → Domain (實現介面)
- 禁止跨層直接依賴

### 事件驅動架構

#### 事件發布模式

1. 聚合根收集事件 (`collectEvent()`)
2. 應用服務發布事件 (`publishEventsFromAggregate()`)
3. 事件處理器異步處理 (`@TransactionalEventListener`)

#### 事件存儲選項

- **開發環境**: JPA Event Store
- **測試環境**: In-Memory Event Store  
- **生產環境**: EventStore DB

#### CQRS 實現

- 命令端：聚合根 + 事件
- 查詢端：讀取模型 + 投影
- 事件同步：異步事件處理

詳細實現請參考：[領域事件指南](domain-events.md)

---

## 🎯 第一階段 - 基礎強化 Steering

### 1. 架構視點一致性 Steering

**適用範圍**: 所有新功能開發和架構變更

#### 強制要求

- 每個新功能必須考慮所有 6 個架構視點的影響：
  - **功能視點**: 明確聚合邊界和領域職責
  - **資訊視點**: 定義事件流和資料一致性策略
  - **並發視點**: 評估異步處理和事務邊界
  - **開發視點**: 確保模組依賴和建置策略
  - **部署視點**: 考慮基礎設施和環境影響
  - **運營視點**: 包含監控、日誌和維護需求

#### 實施檢查點

```markdown
## 視點檢查清單 (每個功能必須完成)

### 功能視點
- [ ] 聚合根邊界明確定義
- [ ] 領域服務職責清晰
- [ ] 用例實現符合 DDD 戰術模式

### 資訊視點  
- [ ] 領域事件設計完整
- [ ] 資料一致性策略明確
- [ ] 事件溯源考量完備

### 並發視點
- [ ] 異步處理策略文檔化
- [ ] 事務邊界明確定義
- [ ] 並發衝突處理機制

### 開發視點
- [ ] 模組依賴符合六角架構
- [ ] 測試策略包含所有層次
- [ ] 建置腳本更新完成

### 部署視點
- [ ] CDK 基礎設施更新
- [ ] 環境配置變更記錄
- [ ] 部署策略影響評估

### 運營視點
- [ ] 監控指標定義
- [ ] 日誌結構設計
- [ ] 故障處理程序
```

### 2. 品質屬性場景驅動開發 Steering

**適用範圍**: 所有用戶故事和功能需求

#### 強制要求

- 每個用戶故事必須包含至少一個品質屬性場景
- 品質屬性場景必須使用標準格式：**來源 → 刺激 → 環境 → 產出物 → 回應 → 回應測量**

#### 品質屬性場景模板

```gherkin
# 性能場景範例
Scenario: 高負載下的訂單處理性能
  Given 系統處於正常運營狀態 (環境)
  When 1000個並發用戶同時提交訂單 (來源 + 刺激)  
  Then 系統應在2秒內回應95%的請求 (回應 + 回應測量)
  And 系統保持穩定運行 (產出物)

# 安全場景範例  
Scenario: 未授權存取防護
  Given 系統部署在生產環境 (環境)
  When 惡意用戶嘗試存取受保護的API (來源 + 刺激)
  Then 系統拒絕存取並記錄安全事件 (回應)
  And 在1秒內回應403錯誤 (回應測量)
  And 觸發安全監控告警 (產出物)
```

#### 量化指標要求

- **性能場景**: 必須有具體的時間、吞吐量或資源使用指標
- **安全場景**: 必須通過 CDK Nag 規則驗證
- **可用性場景**: 必須包含 RTO (恢復時間目標) 和 RPO (恢復點目標)
- **可擴展性場景**: 必須定義負載增長和擴展策略

### 3. 架構合規性自動化 Steering

**適用範圍**: 所有代碼提交和 CI/CD 流程

#### 強制要求

- 所有代碼提交必須通過自動化架構合規性檢查
- 違反架構規則的代碼不得合併到主分支
- 架構合規性報告必須包含在發布文檔中

#### ArchUnit 規則擴展

```java
// 必須實施的架構規則
@ArchTest
static final ArchRule domainLayerRules = classes()
    .that().resideInAPackage("..domain..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage("..domain..", "java..", "org.springframework..");

@ArchTest  
static final ArchRule aggregateRootRules = classes()
    .that().areAnnotatedWith(AggregateRoot.class)
    .should().implement(AggregateRootInterface.class);

@ArchTest
static final ArchRule eventHandlerRules = classes()
    .that().areAnnotatedWith(Component.class)
    .and().haveSimpleNameEndingWith("EventHandler")
    .should().beAnnotatedWith(TransactionalEventListener.class);

@ArchTest
static final ArchRule valueObjectRules = classes()
    .that().areAnnotatedWith(ValueObject.class)
    .should().beRecords();
```

---

## 🔧 第二階段 - 治理完善 Steering

### 4. 利害關係人需求追蹤 Steering

**適用範圍**: 所有 ADR 和重大架構決策

#### 利害關係人分類

- **業務利害關係人**: 產品經理、領域專家、最終用戶
- **技術利害關係人**: 開發團隊、架構師、DevOps 工程師  
- **運營利害關係人**: 系統管理員、支援團隊、安全團隊
- **合規利害關係人**: 法務、稽核、資安團隊

#### ADR 模板擴展

```markdown
## 利害關係人影響分析

### 主要受益者
- [利害關係人類型]: [具體受益]

### 主要受影響者  
- [利害關係人類型]: [具體影響和緩解措施]

### 需求追溯
- [需求ID]: [利害關係人] - [具體需求描述]

### 驗收標準
- [利害關係人]: [如何驗證需求滿足]
```

### 5. 架構決策影響半徑評估 Steering

**適用範圍**: 所有架構決策記錄 (ADR)

#### 影響半徑分類

- **Local**: 單一類別或模組內的變更
- **Bounded Context**: 影響單一有界上下文
- **System**: 跨有界上下文的系統級變更  
- **Enterprise**: 影響多個系統或組織的變更

#### 影響評估模板

```markdown
## 架構決策影響半徑評估

### 影響半徑: [Local/Bounded Context/System/Enterprise]

### 直接影響
- **程式碼**: [受影響的套件和類別]
- **資料**: [受影響的資料模型和存儲]
- **介面**: [受影響的 API 和契約]
- **基礎設施**: [受影響的 AWS 資源]

### 間接影響
- **相依系統**: [可能受影響的外部系統]
- **團隊**: [需要協調的其他團隊]
- **流程**: [需要調整的開發或運營流程]

### 風險評估
- **高風險**: [可能導致系統故障的變更]
- **中風險**: [可能影響性能或可用性的變更]  
- **低風險**: [影響範圍有限的變更]

### 回滾策略
- **回滾觸發條件**: [何時需要回滾]
- **回滾步驟**: [具體的回滾程序]
- **回滾時間**: [預估的回滾時間]

### 遷移路徑
- **階段一**: [初始實施步驟]
- **階段二**: [漸進式遷移步驟]  
- **階段三**: [完整切換步驟]
```

### 6. 可觀測性驅動開發 Steering

**適用範圍**: 所有新功能開發

#### 可觀測性優先原則

- 新功能必須先定義可觀測性需求，再開始實施
- 每個聚合根必須有對應的業務指標
- 每個用例必須有執行追蹤和性能指標
- 每個領域事件必須有發布和處理指標

#### 可觀測性需求模板

```java
// 指標定義範例
@Component
public class CustomerMetrics {
    private final Counter customerCreatedCounter;
    private final Timer customerCreationTimer;
    private final Gauge activeCustomersGauge;
    
    // 業務指標
    public void recordCustomerCreated(Duration processingTime) {
        customerCreatedCounter.increment();
        customerCreationTimer.record(processingTime);
    }
}

// 追蹤範例
@Service
@Transactional
public class CustomerApplicationService {
    
    @NewSpan("customer-creation")
    public void createCustomer(@SpanTag("customerId") CreateCustomerCommand command) {
        // 自動追蹤的業務邏輯
    }
}

// 結構化日誌範例
log.info("Customer created successfully", 
    kv("customerId", customer.getId()),
    kv("membershipLevel", customer.getMembershipLevel()),
    kv("processingTimeMs", processingTime.toMillis()));
```

---

## 🚀 第三階段 - 能力擴展 Steering

### 7. 跨視點影響分析 Steering

**適用範圍**: 重大架構變更和技術決策

#### 跨視點影響矩陣

```markdown
## 跨視點影響分析矩陣

| 變更類型 | 功能 | 資訊 | 並發 | 開發 | 部署 | 運營 |
|----------|------|------|------|------|------|------|
| 新增聚合 | ✅ 直接 | ✅ 直接 | ⚠️ 間接 | ✅ 直接 | ⚠️ 間接 | ⚠️ 間接 |
| 事件模型變更 | ⚠️ 間接 | ✅ 直接 | ✅ 直接 | ⚠️ 間接 | ❌ 無影響 | ⚠️ 間接 |
| 基礎設施變更 | ❌ 無影響 | ⚠️ 間接 | ⚠️ 間接 | ⚠️ 間接 | ✅ 直接 | ✅ 直接 |

圖例: ✅ 直接影響 | ⚠️ 間接影響 | ❌ 無影響
```

#### 影響分析檢查清單

- [ ] 功能視點：聚合邊界和職責是否需要調整？
- [ ] 資訊視點：資料模型和事件結構是否需要變更？
- [ ] 並發視點：並發策略和事務邊界是否受影響？
- [ ] 開發視點：建置流程和測試策略是否需要更新？
- [ ] 部署視點：基礎設施和部署流程是否需要調整？
- [ ] 運營視點：監控、告警和維護程序是否需要更新？

### 8. AI 輔助架構決策 Steering

**適用範圍**: 複雜架構決策和設計選擇

#### AI 輔助決策流程

1. **問題定義**: 使用 AI 協助分析和結構化架構問題
2. **方案生成**: 利用 AI 生成多個可行的架構方案
3. **方案評估**: 使用 AI 進行方案的優缺點分析
4. **決策支援**: AI 提供基於最佳實踐的建議
5. **人工審核**: 架構師最終審核和決策

#### AI 輔助工具整合

```markdown
## AI 輔助架構決策記錄

### 問題描述
[使用 AI 協助結構化的問題描述]

### AI 生成方案
#### 方案 A: [AI 建議的方案名稱]
- **優點**: [AI 分析的優點]
- **缺點**: [AI 分析的缺點]  
- **適用場景**: [AI 建議的適用場景]

#### 方案 B: [AI 建議的方案名稱]
- **優點**: [AI 分析的優點]
- **缺點**: [AI 分析的缺點]
- **適用場景**: [AI 建議的適用場景]

### AI 推薦理由
[AI 基於最佳實踐和項目上下文的推薦]

### 人工架構師決策
- **選擇方案**: [最終選擇的方案]
- **決策理由**: [人工架構師的額外考量]
- **AI 建議採納度**: [採納/部分採納/未採納]
```

#### AI 代碼審核要求

- AI 生成的代碼必須通過 ArchUnit 架構合規性檢查
- AI 重構建議必須經過人工架構師審核
- AI 輔助的設計模式應用必須符合 DDD 戰術模式

### 9. 架構債務管理 Steering

**適用範圍**: 所有妥協性架構決策和技術債務

#### 架構債務分類

- **設計債務**: 違反 DDD 或六角架構原則的設計
- **技術債務**: 過時的技術選型或實施方式
- **文檔債務**: 缺失或過時的架構文檔
- **測試債務**: 缺失的架構測試或品質屬性驗證

#### 架構債務記錄模板

```markdown
## 架構債務記錄

### 債務ID: ARCH-DEBT-[YYYY-MM-DD]-[序號]

### 債務類型: [設計/技術/文檔/測試]

### 債務描述
[具體的架構妥協或問題描述]

### 產生原因
- **時間壓力**: [是否因為交付時間壓力]
- **技術限制**: [是否因為技術或資源限制]
- **知識不足**: [是否因為當時知識或經驗不足]
- **需求變更**: [是否因為需求變更導致]

### 影響評估
- **技術影響**: [對系統技術品質的影響]
- **業務影響**: [對業務功能或性能的影響]
- **維護影響**: [對系統維護和演進的影響]

### 償還計劃
- **優先級**: [高/中/低]
- **預估工作量**: [人天或故事點]
- **目標償還時間**: [具體日期或里程碑]
- **償還策略**: [具體的償還方法]

### 風險評估
- **不償還風險**: [如果不償還可能的後果]
- **償還風險**: [償還過程中的風險]
```

---

## 🏢 第四階段 - 企業級成熟 Steering

### 10. 觀點驅動的非功能需求 Steering

**適用範圍**: 所有功能實現和系統設計

#### 四大觀點強制檢查

##### 安全性觀點檢查清單

- [ ] 所有 API 端點通過 CDK Nag 安全規則檢查
- [ ] 敏感資料加密存儲和傳輸
- [ ] 身份認證和授權機制實施
- [ ] 安全事件日誌記錄和監控
- [ ] 輸入驗證和 SQL 注入防護

##### 性能與可擴展性觀點檢查清單

- [ ] 關鍵路徑性能基準測試 (< 2秒回應時間)
- [ ] 資料庫查詢優化和索引策略
- [ ] 快取策略實施 (Redis/本地快取)
- [ ] 異步處理和事件驅動架構
- [ ] 水平擴展能力驗證

##### 可用性與韌性觀點檢查清單

- [ ] 健康檢查端點實施
- [ ] 故障恢復和重試機制
- [ ] 斷路器模式實施 (外部服務調用)
- [ ] 監控和告警系統配置
- [ ] 災難恢復計劃和測試

##### 演進性觀點檢查清單

- [ ] 介面向後相容性保證
- [ ] 版本管理策略實施
- [ ] 模組化和鬆耦合設計
- [ ] 技術債務識別和管理
- [ ] 重構安全性保證 (測試覆蓋)

### 11. 多環境架構一致性 Steering

**適用範圍**: 所有環境配置和部署流程

#### 環境架構管理

```yaml
# 環境架構配置版本化
environments:
  development:
    architecture_version: "1.2.0"
    differences_from_production:
      - "使用 H2 記憶體資料庫替代 PostgreSQL"
      - "關閉分散式追蹤以提升開發效率"
    
  testing:
    architecture_version: "1.2.0"  
    differences_from_production:
      - "使用較小的 AWS 實例規格"
      - "簡化的監控配置"
    
  production:
    architecture_version: "1.2.0"
    baseline: true
```

#### 架構漂移檢測

- 每日自動檢測環境間的架構配置差異
- 未經審批的架構差異必須在24小時內修正
- 生產環境架構變更必須先在測試環境驗證

### 12. 架構知識共享 Steering

**適用範圍**: 團隊協作和知識管理

#### 架構文檔要求

- 每個聚合根必須有領域模型圖和職責說明
- 每個 ADR 必須包含學習資源和參考資料
- 複雜的架構模式必須有實作範例和最佳實踐

#### 知識共享機制

```markdown
## 架構知識共享計劃

### 月度架構分享會
- **第一週**: DDD 戰術模式實踐分享
- **第二週**: 事件驅動架構案例研討  
- **第三週**: 可觀測性最佳實踐
- **第四週**: 架構決策回顧和學習

### 新成員架構理解度評估
#### 基礎知識 (必須掌握)
- [ ] DDD 聚合根和有界上下文概念
- [ ] 六角架構端口與適配器模式
- [ ] 事件驅動架構和 CQRS 原理
- [ ] 項目的可觀測性系統使用

#### 進階知識 (建議掌握)  
- [ ] 架構決策記錄 (ADR) 撰寫
- [ ] AWS CDK 基礎設施管理
- [ ] 性能優化和故障排除
- [ ] 架構測試和合規性檢查
```

---

## 🌐 第五階段 - 持續演進 Steering

### 13. 並發策略明確化 Steering

**適用範圍**: 所有異步處理和並發設計

#### 並發策略文檔要求

```java
/**
 * 並發策略文檔範例
 * 
 * @ConcurrencyStrategy
 * - 策略: 事件驅動異步處理
 * - 一致性: 聚合內強一致性，聚合間最終一致性
 * - 衝突處理: 樂觀鎖定 + 重試機制
 * - 順序保證: 同一聚合的事件按時間順序處理
 */
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handle(CustomerCreatedEvent event) {
    // 異步事件處理邏輯
}
```

#### 並發設計檢查清單

- [ ] 事件處理順序依賴明確文檔化
- [ ] 事務邊界和一致性保證明確定義
- [ ] 並發衝突檢測和處理機制
- [ ] 死鎖預防和檢測策略
- [ ] 性能測試包含並發場景

### 14. 韌性模式強制應用 Steering

**適用範圍**: 所有外部服務調用和關鍵業務流程

#### 韌性模式實施要求

##### 斷路器模式

```java
@Component
public class ExternalServiceClient {
    
    @CircuitBreaker(name = "payment-service", fallbackMethod = "fallbackPayment")
    @Retry(name = "payment-service")
    @TimeLimiter(name = "payment-service")
    public CompletableFuture<PaymentResult> processPayment(PaymentRequest request) {
        // 外部支付服務調用
    }
    
    public CompletableFuture<PaymentResult> fallbackPayment(PaymentRequest request, Exception ex) {
        // 降級處理邏輯
        return CompletableFuture.completedFuture(PaymentResult.deferred(request.getOrderId()));
    }
}
```

##### 重試和死信佇列

```java
@Component
public class ResilientEventHandler {
    
    @Retryable(
        value = {TransientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void handleEvent(DomainEvent event) {
        // 事件處理邏輯
    }
    
    @Recover
    public void recover(TransientException ex, DomainEvent event) {
        // 最終失敗處理 - 發送到死信佇列
        deadLetterService.send(event, ex);
    }
}
```

#### 韌性檢查清單

- [ ] 外部服務調用實施斷路器模式
- [ ] 關鍵業務流程有降級策略
- [ ] 事件處理失敗有重試機制
- [ ] 死信佇列處理和監控
- [ ] 故障恢復時間測試和驗證

### 15. 技術棧演進策略 Steering

**適用範圍**: 技術選型和版本升級

#### 技術演進評估框架

```markdown
## 技術演進決策框架

### 技術評估維度
1. **技術成熟度**: [新興/成長/成熟/衰退]
2. **社群支援**: [活躍度/文檔完整性/生態系統]
3. **學習成本**: [團隊現有技能/培訓需求/上手難度]
4. **遷移成本**: [工作量估算/風險評估/時間規劃]
5. **長期價值**: [5年技術趨勢/競爭優勢/維護成本]

### 技術引入標準
#### 必須滿足條件
- [ ] 技術成熟度達到"成長"階段以上
- [ ] 有完整的文檔和社群支援
- [ ] 團隊有足夠的學習和維護能力
- [ ] 遷移風險可控且有回滾計劃

#### 優先考慮條件
- [ ] 解決現有技術棧的重大痛點
- [ ] 顯著提升開發效率或系統性能
- [ ] 符合長期技術發展趨勢
- [ ] 有成功的業界實踐案例
```

#### 版本升級策略

- 關鍵依賴的版本升級必須有自動化測試覆蓋
- 主要版本升級必須在測試環境充分驗證
- 遺留技術的淘汰必須有明確的時間表和遷移計劃

### 16. 跨團隊架構協調 Steering

**適用範圍**: 多團隊協作和跨系統整合

#### 團隊協調機制

```markdown
## 跨團隊架構協調框架

### 團隊關係模式 (基於 DDD 上下文映射)
- **共享核心 (Shared Kernel)**: 共同維護的核心領域模型
- **客戶-供應商 (Customer-Supplier)**: 明確的上下游關係
- **遵循者 (Conformist)**: 遵循上游團隊的介面設計
- **防腐層 (Anti-Corruption Layer)**: 隔離外部系統的複雜性

### 介面變更協調流程
1. **變更提案**: 提出介面變更需求和影響分析
2. **影響評估**: 評估對下游團隊的影響
3. **協調會議**: 與相關團隊討論變更方案
4. **版本規劃**: 制定向後相容的版本策略
5. **實施協調**: 協調各團隊的實施時程
6. **驗收測試**: 跨團隊的整合測試驗證
```

#### 架構治理委員會

- 跨有界上下文的重大架構決策需要架構治理委員會審核
- 技術標準和最佳實踐由架構治理委員會制定和維護
- 架構衝突升級到架構治理委員會仲裁

### 17. 架構成熟度持續改進 Steering

**適用範圍**: 組織架構能力建設和持續改進

#### 架構成熟度評估模型

```markdown
## 架構成熟度等級

### Level 1: 初始級 (Ad-hoc)
- 架構決策隨意性強，缺乏系統性方法
- 文檔不完整，知識主要依賴個人經驗
- 品質屬性考慮不足，主要關注功能實現

### Level 2: 可重複級 (Repeatable)  
- 有基本的架構流程和標準
- 開始使用架構模式和最佳實踐
- 有基礎的架構文檔和決策記錄

### Level 3: 已定義級 (Defined)
- 有完整的架構方法論和流程
- 架構決策系統化，有明確的評估標準
- 品質屬性得到充分考慮和驗證

### Level 4: 已管理級 (Managed)
- 架構過程可測量和可控制
- 有量化的架構品質指標
- 持續的架構評估和改進機制

### Level 5: 優化級 (Optimizing)
- 持續的架構創新和優化
- 基於數據驅動的架構決策
- 組織級的架構能力和知識管理
```

#### 持續改進機制

- **季度架構回顧**: 評估架構決策的效果和學習
- **年度成熟度評估**: 對照行業最佳實踐進行差距分析
- **架構創新實驗**: 鼓勵新技術和方法的探索實驗
- **外部基準測試**: 與行業領先實踐進行對標學習

---

## 📊 Steering 實施監控

### 合規性指標

- **視點覆蓋率**: 新功能的視點檢查完成率 (目標: 100%)
- **品質屬性場景覆蓋率**: 用戶故事包含品質屬性場景的比例 (目標: 100%)
- **架構合規性**: ArchUnit 測試通過率 (目標: 100%)
- **架構債務**: 未償還架構債務數量和趨勢 (目標: 持續下降)

### 效果指標

- **架構決策品質**: ADR 的完整性和可追溯性評分
- **跨視點一致性**: 架構變更的視點影響分析完整性
- **團隊架構理解度**: 團隊成員架構知識評估平均分
- **架構成熟度**: 季度架構成熟度評估等級

### 持續改進

- 每月回顧 steering 規則的執行效果
- 根據實際執行情況調整和優化 steering 內容
- 收集團隊反饋，持續改進 steering 的可操作性

---

**Steering 版本**: 1.0  
**生效日期**: 2025-09-13  
**下次回顧**: 2025-10-13  
**維護責任**: 架構團隊
