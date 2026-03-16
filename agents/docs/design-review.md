# Agent 設計評審文檔

> **文件版本**: 1.0  
> **最後更新**: 2026-01-08  
> **狀態**: 📋 待評審

## 概述

本文檔用於團隊設計評審，涵蓋智能客服 Agent 的架構設計、技術選型和實作方案。

---

## 評審項目

### 1. 智能客服 Agent

**Spec 位置**: `agents/specs/customer-service-agent/`

**POC 程式碼**: `agents/src/main/java/solid/humank/genaidemo/agents/customerservice/`

---

## 架構設計評審

### 整體架構

```
┌─────────────────────────────────────────────────────────────────┐
│                    Frontend Applications                         │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│              CustomerServiceAgentController                      │
│              POST /api/v1/agents/customer-service/chat          │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│              CustomerServiceAgentService                         │
│              (對話管理、Tool 調度、回應生成)                      │
└─────────────────────────────────────────────────────────────────┘
                    │                       │
                    ▼                       ▼
┌───────────────────────────┐   ┌───────────────────────────────┐
│     AgentCoreClient       │   │     AgentMemoryService        │
│     (LLM 調用)            │   │     (對話記憶)                │
└───────────────────────────┘   └───────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                      ToolRegistry                                │
│   ┌─────────────┐ ┌─────────────┐ ┌─────────────┐              │
│   │ OrderTool   │ │CustomerTool │ │ PaymentTool │  ...         │
│   │ Adapter     │ │ Adapter     │ │ Adapter     │              │
│   └─────────────┘ └─────────────┘ └─────────────┘              │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                 Existing Application Services                    │
│   (OrderApplicationService, CustomerApplicationService, ...)    │
└─────────────────────────────────────────────────────────────────┘
```

### 評審檢查清單

#### ✅ 架構原則

- [ ] 符合六邊形架構原則
- [ ] Agent 層與 Domain 層適當分離
- [ ] Tool Adapter 作為 Port 連接 Application Services
- [ ] 依賴方向正確（Agent → Application → Domain）

#### ✅ 設計模式

- [ ] Strategy Pattern: AgentCoreClient 介面支援多種實作
- [ ] Adapter Pattern: Tool Adapters 轉換 Agent 請求
- [ ] Registry Pattern: ToolRegistry 管理可用工具
- [ ] Builder Pattern: Request/Response 物件建構

#### ✅ 關注點分離

- [ ] Controller 只處理 HTTP 請求/回應
- [ ] Service 負責業務邏輯協調
- [ ] Client 負責外部服務調用
- [ ] Memory 負責狀態管理

---

## 技術選型評審

### LLM 選擇

| 選項 | 優點 | 缺點 | 建議 |
|------|------|------|------|
| Claude 3 Sonnet | 平衡效能和成本、中文能力強 | 需要 Bedrock 權限 | ✅ 推薦 |
| Claude 3 Haiku | 成本最低、速度快 | 複雜任務能力較弱 | 備選 |
| Claude 3 Opus | 能力最強 | 成本高、延遲較高 | 不建議 |

**評審問題**:
1. 是否已申請 Bedrock Claude 模型存取權限？
2. 預估每月 API 調用成本？
3. 是否需要 fallback 到其他模型？

### Memory 儲存

| 選項 | 優點 | 缺點 | 建議 |
|------|------|------|------|
| DynamoDB | 低延遲、高可用、Serverless | 查詢彈性較低 | ✅ 推薦 |
| ElastiCache Redis | 極低延遲、豐富資料結構 | 需要管理叢集 | 備選 |
| In-Memory | 簡單、無外部依賴 | 無持久化、無法擴展 | 僅 POC |

**評審問題**:
1. 對話記憶需要保留多久？
2. 是否需要跨 Session 的客戶記憶？
3. 記憶資料的備份策略？

---

## 安全性評審

### 認證與授權

- [ ] JWT Token 驗證機制
- [ ] 客戶只能查詢自己的訂單
- [ ] 敏感操作需要額外確認

### 資料保護

- [ ] PII 資料脫敏（信用卡、電話、Email）
- [ ] 對話記錄不包含敏感資訊
- [ ] 日誌中不記錄完整請求內容

### 速率限制

- [ ] 每用戶每分鐘請求限制
- [ ] 防止 DDoS 攻擊
- [ ] 異常流量告警

**評審問題**:
1. 速率限制的具體數值？（建議：100 req/min/user）
2. 超過限制的處理方式？
3. 是否需要 IP 層級的限制？

---

## 效能評審

### 延遲目標

| 指標 | 目標值 | 測量方式 |
|------|--------|----------|
| P50 延遲 | < 1.5s | CloudWatch Metrics |
| P95 延遲 | < 3s | CloudWatch Metrics |
| P99 延遲 | < 5s | CloudWatch Metrics |

### 吞吐量目標

| 指標 | 目標值 | 備註 |
|------|--------|------|
| 並發請求 | 100 req/s | 單一 Agent 實例 |
| 每日請求 | 100,000 | 預估初期流量 |

**評審問題**:
1. LLM API 調用是否為主要瓶頸？
2. 是否需要實作回應快取？
3. 長對話的效能影響？

---

## 可觀測性評審

### Metrics

- [ ] `agent.invocations` - 調用次數
- [ ] `agent.latency` - 回應延遲
- [ ] `agent.tool.executions` - Tool 執行次數
- [ ] `agent.errors` - 錯誤次數
- [ ] `agent.escalations` - 升級到人工次數

### Tracing

- [ ] 整合 AWS X-Ray
- [ ] 追蹤完整請求鏈路
- [ ] Tool 執行時間追蹤

### Logging

- [ ] 結構化日誌格式
- [ ] 對話 ID 關聯
- [ ] 錯誤堆疊追蹤

**評審問題**:
1. 是否需要建立專用 Dashboard？
2. 告警閾值設定？
3. 日誌保留期限？

---

## 整合評審

### 與現有系統整合

| 系統 | 整合方式 | 狀態 |
|------|----------|------|
| OrderApplicationService | 直接調用 | ✅ POC 完成 |
| CustomerApplicationService | 直接調用 | 📋 待實作 |
| PaymentApplicationService | 直接調用 | 📋 待實作 |
| NotificationApplicationService | 直接調用 | 📋 待實作 |

### API 設計

```
POST /api/v1/agents/customer-service/chat
Content-Type: application/json

Request:
{
  "sessionId": "session-123",
  "customerId": "customer-456",
  "message": "我想查詢訂單狀態",
  "language": "zh-TW"
}

Response:
{
  "sessionId": "session-123",
  "message": "訂單編號: ORD-001\n狀態: 已出貨\n...",
  "actions": [
    {
      "type": "show_order",
      "data": { "orderId": "ORD-001", "status": "SHIPPED" }
    }
  ],
  "metadata": {
    "conversationId": "session-123",
    "turnCount": 1,
    "detectedIntent": "order_query",
    "processingTimeMs": 1234
  }
}
```

**評審問題**:
1. API 版本策略？
2. 錯誤回應格式是否與現有 API 一致？
3. 是否需要支援 WebSocket？

---

## 測試策略評審

### 測試覆蓋

| 測試類型 | 覆蓋範圍 | 目標覆蓋率 |
|----------|----------|------------|
| 單元測試 | Tool Adapters, Memory Service | > 80% |
| 整合測試 | Agent Service, API 端點 | > 70% |
| E2E 測試 | 完整對話流程 | 關鍵路徑 |

### Mock 策略

- AgentCoreClient: Mock LLM 回應
- Application Services: 使用真實服務或 Mock
- Memory: In-Memory 實作

**評審問題**:
1. 如何測試 LLM 回應品質？
2. 是否需要對話品質的自動化測試？
3. 效能測試的執行頻率？

---

## 部署策略評審

### 環境配置

| 環境 | LLM | Memory | 用途 |
|------|-----|--------|------|
| Local | Mock | In-Memory | 開發 |
| Test | Mock | In-Memory | 自動化測試 |
| Staging | Bedrock | DynamoDB | 整合測試 |
| Production | Bedrock | DynamoDB | 正式環境 |

### 漸進式發布

1. **Phase 1**: 內部測試 (10% 流量)
2. **Phase 2**: Beta 測試 (30% 流量)
3. **Phase 3**: 全面發布 (100% 流量)

**評審問題**:
1. Rollback 策略？
2. Feature Flag 機制？
3. 監控指標閾值？

---

## 風險評估

| 風險 | 機率 | 影響 | 緩解措施 |
|------|------|------|----------|
| LLM API 延遲過高 | 中 | 高 | 超時設定、降級回應 |
| LLM 回應品質不佳 | 中 | 中 | Prompt 優化、人工審核 |
| 成本超出預算 | 低 | 中 | 監控用量、設定上限 |
| 安全漏洞 | 低 | 高 | 安全審計、滲透測試 |

---

## 評審決議

### 待討論事項

1. [ ] LLM 模型選擇確認
2. [ ] Memory 儲存方案確認
3. [ ] 安全性要求確認
4. [ ] 效能目標確認
5. [ ] 部署時程確認

### 評審結果

| 項目 | 狀態 | 備註 |
|------|------|------|
| 架構設計 | ⏳ 待評審 | |
| 技術選型 | ⏳ 待評審 | |
| 安全性 | ⏳ 待評審 | |
| 效能 | ⏳ 待評審 | |
| 整合方案 | ⏳ 待評審 | |

### 評審參與者

- [ ] 架構師
- [ ] 後端開發
- [ ] 前端開發
- [ ] DevOps
- [ ] 產品經理

---

## 下一步行動

評審通過後：

1. 完成剩餘 Tool Adapters 實作
2. 建立 DynamoDB Memory 實作
3. 整合 Bedrock Client
4. 編寫完整測試
5. 部署到 Staging 環境

---

**評審日期**: _______________  
**評審結果**: ⏳ 待評審 / ✅ 通過 / ❌ 需修改  
**評審人簽名**: _______________
