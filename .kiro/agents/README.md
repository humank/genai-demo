# Custom Sub Agents for GenAI-Demo Project

## 概述

基於 genai-demo 專案的架構和需求，設計了 **6 個專門的 custom sub agents** (含 1 個 orchestrator)，補充現有的 6 個 agents，形成完整的開發支援體系。

## 🎭 Agent Orchestrator (推薦使用)

**檔案**: `.kiro/agents/orchestrator.md`

**角色**: 智能 agent 協調器，自動分析用戶意圖並調度合適的專業 agents

**核心能力**:
- 🧠 意圖分析: 理解任務複雜度和領域需求
- 🎯 智能選擇: 自動選擇 1-7 個合適的 agents
- 🔄 執行策略: 決定並行或順序執行
- 📊 結果整合: 整合多個 agent 的輸出
- ✅ 品質驗證: 確保完整性和一致性

**使用方式**:
```bash
# 推薦: 使用 orchestrator 作為入口
kiro chat --agent orchestrator
```

**範例**:
- "實作訂單取消功能" → 自動調度 4 agents (domain + test + doc + monitoring)
- "生產延遲問題" → 順序診斷 (monitoring → resilience → domain)
- "新增 bounded context" → 混合執行 (doc → 並行實作 → monitoring)

---

## 現有 Agents (6個)

1. **cmc-migration** - CMC 前端遷移到 Monorepo
2. **consumer-app** - Consumer 前端應用開發
3. **iac-infrastructure** - AWS CDK 基礎設施管理
4. **cmc-enhancement** - CMC 功能增強
5. **e2e-test** - E2E 測試和品質驗證
6. **monorepo-scaffold** - Monorepo 腳手架

## 新增 Custom Agents (5個)

### 1. DDD Domain Expert
**檔案**: `.kiro/agents/ddd-domain-expert.md`

**專長領域**:
- Domain-Driven Design 戰術和策略模式
- 13 個 Bounded Contexts 的領域建模
- Aggregate、Entity、Value Object 設計
- Domain Events 和 Event Storming
- 六角架構 (Hexagonal Architecture)

**適用場景**:
- 設計新的 bounded context 或 aggregate
- 重構領域模型
- 實作 domain events 和 sagas
- 解決 context 整合問題
- 進行 Event Storming 工作坊
- 審查 DDD 合規性

**關鍵能力**:
- 維護 13 個 bounded contexts 的一致性
- 確保領域層零基礎設施依賴
- 實作 ArchUnit 架構測試
- 編寫 BDD 場景對應業務規則

---

### 2. Architecture Doc Specialist
**檔案**: `.kiro/agents/architecture-doc-specialist.md`

**專長領域**:
- Rozanski & Woods 架構方法論
- 7 個 Viewpoints 文檔維護
- 8 個 Perspectives 應用
- PlantUML 圖表生成
- Architecture Decision Records (ADRs)

**適用場景**:
- 創建或更新 viewpoint 文檔
- 應用 perspectives 到架構
- 編寫 ADRs
- 生成或更新架構圖
- 驗證文檔品質
- 文檔多區域架構
- 創建運維 runbooks

**關鍵能力**:
- 維護 50+ PlantUML 圖表
- 確保 99.2% 連結健康度
- 自動化圖表生成和驗證
- 文檔品質檢查和元數據驗證

---

### 3. Multi-Region Resilience Expert
**檔案**: `.kiro/agents/multi-region-resilience-expert.md`

**專長領域**:
- Active-Active 多區域架構 (台灣 + 日本)
- 災難恢復自動化
- Chaos Engineering
- 99.99% 可用性目標
- RTO: 28s, RPO: 0.8s

**適用場景**:
- 設計多區域架構
- 實作災難恢復自動化
- 進行混沌工程測試
- 故障轉移問題排查
- 優化跨區域效能
- 分析複製延遲
- 規劃 DR 演練

**關鍵能力**:
- 三層容錯移轉策略 (DNS → 應用層 → 資料庫層)
- Aurora Global Database 管理
- Route 53 健康檢查和路由
- 每月自動化 DR 演練
- 成本效益分析 (ROI: 889%)

---

### 4. BDD Test Specialist
**檔案**: `.kiro/agents/bdd-test-specialist.md`

**專長領域**:
- Behavior-Driven Development (BDD)
- Cucumber 7 + Gherkin
- 28+ 可執行規格
- Living Documentation
- Step Definitions 實作

**適用場景**:
- 編寫新的 BDD 場景
- 實作 step definitions
- 重構重複的 step definitions
- 將 Event Storming 映射到可執行規格
- 除錯失敗的 BDD 測試
- 生成 living documentation

**關鍵能力**:
- 跨 13 個 bounded contexts 的場景覆蓋
- 80%+ 業務邏輯測試覆蓋率
- < 5 分鐘完整 BDD 套件執行
- 業務可讀的 Gherkin 場景
- Domain Event 驗證

---

### 5. Observability & Monitoring Expert
**檔案**: `.kiro/agents/observability-monitoring-expert.md`

**專長領域**:
- 分散式系統可觀測性
- CloudWatch + X-Ray + Prometheus + Grafana
- 分散式追蹤
- 日誌聚合和結構化日誌
- 告警策略和儀表板

**適用場景**:
- 實作分散式追蹤
- 添加自訂指標
- 創建儀表板
- 設定告警
- 調查生產問題
- 分析效能瓶頸
- 監控多區域架構

**關鍵能力**:
- 跨區域的端到端追蹤
- 結構化日誌與 CloudWatch Insights
- 業務和技術指標
- 多層級告警 (Critical/Warning/Info)
- Golden Signals (Latency, Traffic, Errors, Saturation)

---

## Agent 使用矩陣

| 任務類型 | 推薦 Agent | 次要 Agent |
|---------|-----------|-----------|
| **後端領域建模** | DDD Domain Expert | Architecture Doc Specialist |
| **前端開發** | consumer-app / cmc-migration | e2e-test |
| **基礎設施部署** | iac-infrastructure | Multi-Region Resilience Expert |
| **多區域架構** | Multi-Region Resilience Expert | iac-infrastructure |
| **測試開發** | BDD Test Specialist | e2e-test |
| **文檔撰寫** | Architecture Doc Specialist | - |
| **監控告警** | Observability & Monitoring Expert | - |
| **災難恢復** | Multi-Region Resilience Expert | Observability & Monitoring Expert |
| **效能優化** | Observability & Monitoring Expert | Multi-Region Resilience Expert |

## 協作模式

### 場景 1: 新增 Bounded Context
1. **DDD Domain Expert**: 設計 aggregate 和 domain events
2. **BDD Test Specialist**: 編寫 BDD 場景
3. **Architecture Doc Specialist**: 更新 functional viewpoint 文檔
4. **Observability & Monitoring Expert**: 添加業務指標

### 場景 2: 多區域功能開發
1. **Multi-Region Resilience Expert**: 設計跨區域策略
2. **DDD Domain Expert**: 實作最終一致性
3. **iac-infrastructure**: 部署基礎設施
4. **Observability & Monitoring Expert**: 設定跨區域監控

### 場景 3: 生產問題排查
1. **Observability & Monitoring Expert**: 分析日誌和指標
2. **Multi-Region Resilience Expert**: 檢查區域健康狀態
3. **DDD Domain Expert**: 審查領域邏輯
4. **Architecture Doc Specialist**: 更新 runbook

### 場景 4: 架構重構
1. **Architecture Doc Specialist**: 撰寫 ADR
2. **DDD Domain Expert**: 重構領域模型
3. **BDD Test Specialist**: 更新 BDD 場景
4. **e2e-test**: 驗證端到端流程

## 使用建議

### 單一任務
直接調用最相關的 agent:
```bash
# 例如: 添加新的 domain event
kiro chat --agent ddd-domain-expert
```

### 複雜任務
並行調用多個 agents (最多 4-7 個):
```bash
# 例如: 實作新的多區域功能
kiro chat --parallel \
  --agent multi-region-resilience-expert \
  --agent ddd-domain-expert \
  --agent iac-infrastructure \
  --agent observability-monitoring-expert
```

### 順序任務
依序調用 agents:
1. 設計階段: Architecture Doc Specialist
2. 實作階段: DDD Domain Expert
3. 測試階段: BDD Test Specialist
4. 部署階段: iac-infrastructure
5. 監控階段: Observability & Monitoring Expert

## 總結

這 6 個新的 agents (含 1 個 orchestrator) 與現有的 6 個 agents 形成完整的開發支援體系:

**協調層** (1 agent):
- 🎭 orchestrator ⭐ NEW - 智能協調器

**前端開發** (3 agents):
- consumer-app
- cmc-migration
- cmc-enhancement

**後端開發** (2 agents):
- DDD Domain Expert ⭐ NEW
- BDD Test Specialist ⭐ NEW

**基礎設施** (3 agents):
- iac-infrastructure
- Multi-Region Resilience Expert ⭐ NEW
- monorepo-scaffold

**品質保證** (2 agents):
- e2e-test
- BDD Test Specialist ⭐ NEW

**文檔與監控** (2 agents):
- Architecture Doc Specialist ⭐ NEW
- Observability & Monitoring Expert ⭐ NEW

**總計**: 12 個 agents (6 現有 + 6 新增)

### 推薦使用方式

**最佳實踐**: 使用 `orchestrator` 作為入口點
```bash
kiro chat --agent orchestrator
```

Orchestrator 會自動:
- 分析你的意圖
- 選擇合適的專業 agents
- 協調並行或順序執行
- 整合所有輸出
- 確保品質和完整性

這些 agents 覆蓋了專案的所有關鍵領域，可以有效支援團隊的日常開發工作。
