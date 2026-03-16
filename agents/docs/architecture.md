# Agent 系統架構設計

> **AWS Bedrock AgentCore 整合架構**

## 概述

本文檔描述 AI Agent 系統與現有電商平台的整合架構設計。

## 設計原則

### 1. 與現有架構一致

- 遵循 Hexagonal Architecture (Ports & Adapters)
- 符合 DDD 戰術模式
- 保持 Bounded Context 邊界

### 2. Agent 作為應用層擴展

```
┌─────────────────────────────────────────────────────────────┐
│                    Interfaces Layer                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ REST API   │  │ WebSocket   │  │ AgentCore   │         │
│  │ Controllers│  │ Handlers    │  │ Gateway     │  ← NEW  │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ Application │  │ Application │  │   Agent     │         │
│  │ Services    │  │ Services    │  │  Services   │  ← NEW  │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│           (Aggregates, Entities, Value Objects,              │
│            Domain Events, Domain Services)                   │
│                    [NO CHANGES]                              │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   Infrastructure Layer                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ Persistence │  │  External   │  │ AgentCore   │         │
│  │ Adapters    │  │  Services   │  │ Adapters    │  ← NEW  │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
```

### 3. 鬆耦合整合

- Agent 透過 Application Services 存取業務邏輯
- 不直接存取 Domain Layer
- 使用 Domain Events 進行跨 Context 通訊

## AgentCore 組件整合

### Gateway 整合

```
AgentCore Gateway
       │
       ▼
┌─────────────────┐
│ Agent Endpoint  │ ← /api/v1/agents/customer-service
│ (REST/WebSocket)│
└─────────────────┘
       │
       ▼
┌─────────────────┐
│ Authentication  │ ← JWT / API Key
│ & Authorization │
└─────────────────┘
       │
       ▼
┌─────────────────┐
│ Agent Runtime   │
└─────────────────┘
```

### Memory 整合

```
Agent Conversation
       │
       ▼
┌─────────────────┐
│ AgentCore       │
│ Memory          │
│ ┌─────────────┐ │
│ │ Short-term  │ │ ← 對話上下文
│ │ Memory      │ │
│ └─────────────┘ │
│ ┌─────────────┐ │
│ │ Long-term   │ │ ← 客戶偏好、歷史
│ │ Memory      │ │
│ └─────────────┘ │
└─────────────────┘
       │
       ▼
┌─────────────────┐
│ DynamoDB        │ ← 持久化存儲
└─────────────────┘
```

### Tools 整合

Agent Tools 作為 Application Services 的包裝：

```python
# 範例: 訂單查詢 Tool
@tool
def get_order_status(order_id: str) -> dict:
    """
    查詢訂單狀態
    
    Args:
        order_id: 訂單 ID
        
    Returns:
        訂單狀態資訊
    """
    # 調用現有的 OrderApplicationService
    response = order_service.get_order_by_id(order_id)
    return {
        "order_id": response.id,
        "status": response.status,
        "items": response.items,
        "estimated_delivery": response.estimated_delivery
    }
```

## 部署架構

### 開發環境

```
┌─────────────────────────────────────────┐
│           Local Development              │
│  ┌─────────────┐  ┌─────────────┐       │
│  │ agentcore   │  │ Spring Boot │       │
│  │ dev server  │  │ Application │       │
│  └─────────────┘  └─────────────┘       │
│         │                │               │
│         └────────┬───────┘               │
│                  ▼                       │
│         ┌─────────────┐                  │
│         │   H2 / Redis │                 │
│         │   (Local)    │                 │
│         └─────────────┘                  │
└─────────────────────────────────────────┘
```

### 生產環境

```
┌─────────────────────────────────────────────────────────────┐
│                    AWS Cloud                                 │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                 AgentCore Runtime                    │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │   │
│  │  │  Customer   │  │   Order     │  │  Promotion  │ │   │
│  │  │  Service    │  │  Processing │  │  Recommend  │ │   │
│  │  │   Agent     │  │   Agent     │  │   Agent     │ │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘ │   │
│  └─────────────────────────────────────────────────────┘   │
│                           │                                  │
│                           ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                    EKS Cluster                       │   │
│  │         (Existing E-Commerce Application)            │   │
│  └─────────────────────────────────────────────────────┘   │
│                           │                                  │
│                           ▼                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   Aurora    │  │ ElastiCache │  │   DynamoDB  │        │
│  │  Global DB  │  │   (Redis)   │  │  (Memory)   │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## 安全考量

### 認證與授權

1. **Gateway 層**: API Key / JWT Token 驗證
2. **Agent 層**: 角色權限控制 (RBAC)
3. **Service 層**: 現有的 Spring Security 整合

### 資料保護

1. **PII 處理**: 遵循現有的 `PiiMaskingEventPublisher`
2. **日誌脫敏**: 使用 `PiiMaskingPatternLayout`
3. **傳輸加密**: TLS 1.3

## 監控與可觀測性

### 指標收集

- Agent 調用次數和延遲
- Tool 執行成功率
- Memory 使用量
- Token 消耗量

### 整合現有監控

```
Agent Metrics
     │
     ▼
┌─────────────────┐
│ CloudWatch      │ ← 整合現有的 ObservabilityConfiguration
│ Metrics         │
└─────────────────┘
     │
     ▼
┌─────────────────┐
│ X-Ray Tracing   │ ← 整合現有的 XRayTracingConfig
└─────────────────┘
     │
     ▼
┌─────────────────┐
│ Grafana         │ ← 現有的 Dashboard
│ Dashboard       │
└─────────────────┘
```

## 下一步

1. 建立各 Agent 的詳細設計文檔
2. 定義 Tool 介面規範
3. 設計 Memory Schema
4. 規劃 CDK 部署 Stack

---

**文件版本**: 1.0  
**建立日期**: 2026-01-06
