# 跨視點和觀點文件交叉引用連結

## 概覽

本文件提供所有架構文檔之間的智能交叉引用連結，幫助讀者快速導航到相關文檔，理解不同視點和觀點之間的關聯。

## 🔗 核心導航連結

### 主要入口點

- **[文檔中心首頁](README.md)** - 完整的導航和搜尋系統
- **[Viewpoint-Perspective 交叉引用矩陣](viewpoint-perspective-matrix.md)** - 視點與觀點的影響程度分析
- **[架構決策記錄 (ADR)](architecture/adr/)** - 所有重要架構決策的記錄

### 快速導航

| 導航類型 | 入口文檔 | 說明 |
|----------|----------|------|
| **按角色導航** | [文檔中心 - 按角色導航](README.md#👨‍💼-按角色導航) | 架構師、開發者、DevOps、安全工程師等 |
| **按關注點導航** | [文檔中心 - 按關注點導航](README.md#🔍-按關注點導航) | 系統架構、DDD、資料架構、安全等 |
| **視覺化導航** | [文檔中心 - 視覺化導航](README.md#📊-視覺化導航) | 圖表總覽和視覺化架構 |
| **智能搜尋** | [文檔中心 - 智能搜尋](README.md#🔍-智能搜尋和導航) | 關鍵字搜尋和主題導航 |

## 📊 視點間交叉引用

### 功能視點 (Functional Viewpoint)

**主文檔**: [功能視點總覽](viewpoints/functional/README.md)

#### 強關聯視點
- **[資訊視點](viewpoints/information/README.md)** - 領域事件和資料流設計
- **[開發視點](viewpoints/development/README.md)** - DDD 戰術模式實現
- **[部署視點](viewpoints/deployment/README.md)** - 功能模組的部署策略

#### 相關文檔
- **[領域模型設計](viewpoints/functional/domain-model.md)** ↔ **[領域事件設計](viewpoints/information/domain-events.md)**
- **[聚合根設計](viewpoints/functional/aggregates.md)** ↔ **[六角形架構實現](viewpoints/development/hexagonal-architecture.md)**
- **[界限上下文](viewpoints/functional/bounded-contexts.md)** ↔ **[基礎設施即程式碼](viewpoints/deployment/infrastructure-as-code.md)**

#### 相關圖表
- **[領域模型圖](diagrams/plantuml/domain-model-diagram.svg)** - 完整領域模型視覺化
- **[界限上下文圖](diagrams/plantuml/bounded-context-diagram.svg)** - 上下文劃分
- **[Event Storming 系列](diagrams/plantuml/event-storming/)** - 業務流程分析

### 資訊視點 (Information Viewpoint)

**主文檔**: [資訊視點總覽](viewpoints/information/README.md)

#### 強關聯視點
- **[功能視點](viewpoints/functional/README.md)** - 業務邏輯和資料模型的對應
- **[並發視點](viewpoints/concurrency/README.md)** - 事件驅動和非同步處理
- **[運營視點](viewpoints/operational/README.md)** - 資料監控和可觀測性

#### 相關文檔
- **[領域事件設計](viewpoints/information/domain-events.md)** ↔ **[聚合根設計](viewpoints/functional/aggregates.md)**
- **[架構元素](viewpoints/information/architecture-elements.md)** ↔ **[並發視點總覽](viewpoints/concurrency/README.md)**

#### 相關圖表
- **## 事件驅動架構圖

```mermaid
graph LR
    subgraph 領域事件 ["領域事件"]
        OCE[OrderCreatedEvent]
        OIAE[OrderItemAddedEvent]
        PRE[PaymentRequestedEvent]
        PFE[PaymentFailedEvent]
    end
    
    subgraph 事件處理 ["事件處理"]
        EP[DomainEventPublisherService]
        EB[DomainEventBus]
        OS[OrderProcessingSaga]
    end
    
    subgraph 事件監聽器 ["事件監聽器"]
        PS[PaymentService]
        LS[LogisticsService]
    end
    
    AGG[Order<br>聚合根] -->|產生| OCE
    AGG -->|產生| OIAE
    OCE -->|發布至| EP
    OIAE -->|發布至| EP
    EP -->|發送至| EB
    EB -->|分發| OS
    EB -->|分發| PS
    EB -->|分發| LS
    OS -->|協調| PS
    OS -->|協調| LS
    PS -->|產生| PRE
    PS -->|產生| PFE
    PRE -->|發布至| EP
    PFE -->|發布至| EP
    
    classDef event fill:#ffcc99,stroke:#333,stroke-width:2px
    classDef publisher fill:#99ccff,stroke:#333,stroke-width:2px
    classDef handler fill:#cc99ff,stroke:#333,stroke-width:2px
    classDef aggregateRoot fill:#bbf,stroke:#333,stroke-width:2px
    
    class OCE,OIAE,PRE,PFE event
    class EP,EB publisher
    class OS,PS,LS handler
    class AGG aggregateRoot
```** - 事件驅動模式
- **[CQRS 模式圖](diagrams/plantuml/cqrs-pattern-diagram.svg)** - 命令查詢責任分離
- **[Event Storming 詳細分析](diagrams/plantuml/event-storming/)** - Big Picture、Process Level、Design Level

### 並發視點 (Concurrency Viewpoint)

**主文檔**: [並發視點總覽](viewpoints/concurrency/README.md)

#### 強關聯視點
- **[資訊視點](viewpoints/information/README.md)** - 事件驅動架構和資料一致性
- **[運營視點](viewpoints/operational/README.md)** - 並發監控和性能調優

#### 相關文檔
- **並發處理策略** ↔ **[領域事件設計](viewpoints/information/domain-events.md)**
- **非同步處理** ↔ **[可觀測性概覽](viewpoints/operational/observability-overview.md)**

#### 相關圖表
- **## 事件驅動架構圖

```mermaid
graph LR
    subgraph 領域事件 ["領域事件"]
        OCE[OrderCreatedEvent]
        OIAE[OrderItemAddedEvent]
        PRE[PaymentRequestedEvent]
        PFE[PaymentFailedEvent]
    end
    
    subgraph 事件處理 ["事件處理"]
        EP[DomainEventPublisherService]
        EB[DomainEventBus]
        OS[OrderProcessingSaga]
    end
    
    subgraph 事件監聽器 ["事件監聽器"]
        PS[PaymentService]
        LS[LogisticsService]
    end
    
    AGG[Order<br>聚合根] -->|產生| OCE
    AGG -->|產生| OIAE
    OCE -->|發布至| EP
    OIAE -->|發布至| EP
    EP -->|發送至| EB
    EB -->|分發| OS
    EB -->|分發| PS
    EB -->|分發| LS
    OS -->|協調| PS
    OS -->|協調| LS
    PS -->|產生| PRE
    PS -->|產生| PFE
    PRE -->|發布至| EP
    PFE -->|發布至| EP
    
    classDef event fill:#ffcc99,stroke:#333,stroke-width:2px
    classDef publisher fill:#99ccff,stroke:#333,stroke-width:2px
    classDef handler fill:#cc99ff,stroke:#333,stroke-width:2px
    classDef aggregateRoot fill:#bbf,stroke:#333,stroke-width:2px
    
    class OCE,OIAE,PRE,PFE event
    class EP,EB publisher
    class OS,PS,LS handler
    class AGG aggregateRoot
```** - 並發事件處理
- **[系統架構概覽圖](diagrams/mermaid/architecture-overview.md)** - 並發處理層

### 開發視點 (Development Viewpoint)

**主文檔**: [開發視點總覽](viewpoints/development/README.md)

#### 強關聯視點
- **[功能視點](viewpoints/functional/README.md)** - DDD 戰術模式的實現
- **[部署視點](viewpoints/deployment/README.md)** - 開發環境和 CI/CD 流程

#### 相關文檔
- **[六角形架構實現](viewpoints/development/hexagonal-architecture.md)** ↔ **[領域模型設計](viewpoints/functional/domain-model.md)**
- **[開發工作流程](viewpoints/development/development-workflow.md)** ↔ **[基礎設施即程式碼](viewpoints/deployment/infrastructure-as-code.md)**
- **[Epic 實現指南](viewpoints/development/epic-implementation.md)** ↔ **[聚合根設計](viewpoints/functional/aggregates.md)**

#### 相關圖表
- **[六角形架構圖](diagrams/mermaid/hexagonal-architecture.md)** - 端口和適配器實現
- **[DDD 分層架構圖](diagrams/mermaid/ddd-layered-architecture.md)** - 完整的開發架構

### 部署視點 (Deployment Viewpoint)

**主文檔**: [部署視點總覽](viewpoints/deployment/README.md)

#### 強關聯視點
- **[開發視點](viewpoints/development/README.md)** - CI/CD 流程和建置策略
- **[運營視點](viewpoints/operational/README.md)** - 部署後的監控和維護

#### 相關文檔
- **[基礎設施即程式碼](viewpoints/deployment/infrastructure-as-code.md)** ↔ **[開發工作流程](viewpoints/development/development-workflow.md)**
- **[生產部署檢查清單](viewpoints/deployment/production-deployment-checklist.md)** ↔ **[可觀測性概覽](viewpoints/operational/observability-overview.md)**
- **[可觀測性部署](viewpoints/deployment/observability-deployment.md)** ↔ **[生產環境測試指南](viewpoints/operational/production-observability-testing-guide.md)**

#### 相關圖表
- **[部署架構圖](diagrams/plantuml/deployment-diagram.svg)** - 完整部署架構
- **## AWS 基礎設施圖

```mermaid
graph TB
    subgraph "AWS Infrastructure"
        EKS[EKS Cluster]
        RDS[RDS Database]
        S3[S3 Storage]
        CloudWatch[CloudWatch]
        ALB[Application Load Balancer]
    end
    
    ALB --> EKS
    EKS --> RDS
    EKS --> S3
    EKS --> CloudWatch
```** - 雲端基礎設施
- **## 多環境架構圖

```mermaid
graph TB
    subgraph DEV ["Development Environment"]
        DEV_APP[Spring Boot App<br/>Profile: dev]
        H2_DB[(H2 Database)]
        MEMORY_EVENTS[In-Memory Events]
    end
    
    subgraph PROD ["Production Environment"]
        PROD_APP[Spring Boot App<br/>Profile: production]
        RDS_DB[(RDS PostgreSQL)]
        MSK_EVENTS[MSK Events]
    end
    
    subgraph CONFIG ["Configuration"]
        BASE_CONFIG[application.yml]
        DEV_CONFIG[application-dev.yml]
        PROD_CONFIG[application-production.yml]
    end
    
    BASE_CONFIG --> DEV_CONFIG
    BASE_CONFIG --> PROD_CONFIG
    
    DEV_CONFIG --> DEV_APP
    PROD_CONFIG --> PROD_APP
    
    DEV_APP --> H2_DB
    DEV_APP --> MEMORY_EVENTS
    
    PROD_APP --> RDS_DB
    PROD_APP --> MSK_EVENTS
    
    classDef dev fill:#e8f5e8,stroke:#2e7d32,stroke-width:2px
    classDef prod fill:#ffebee,stroke:#c62828,stroke-width:2px
    classDef config fill:#fff3e0,stroke:#e65100,stroke-width:2px
    
    class DEV_APP,H2_DB,MEMORY_EVENTS dev
    class PROD_APP,RDS_DB,MSK_EVENTS prod
    class BASE_CONFIG,DEV_CONFIG,PROD_CONFIG config
```** - 環境管理策略

### 運營視點 (Operational Viewpoint)

**主文檔**: [運營視點總覽](viewpoints/operational/README.md)

#### 強關聯視點
- **[部署視點](viewpoints/deployment/README.md)** - 部署策略和運營準備
- **[並發視點](viewpoints/concurrency/README.md)** - 並發監控和性能調優
- **[資訊視點](viewpoints/information/README.md)** - 資料監控和事件追蹤

#### 相關文檔
- **[可觀測性概覽](viewpoints/operational/observability-overview.md)** ↔ **[可觀測性部署](viewpoints/deployment/observability-deployment.md)**
- **[生產環境測試指南](viewpoints/operational/production-observability-testing-guide.md)** ↔ **[生產部署檢查清單](viewpoints/deployment/production-deployment-checklist.md)**
- **[配置指南](viewpoints/operational/configuration-guide.md)** ↔ **[基礎設施即程式碼](viewpoints/deployment/infrastructure-as-code.md)**

#### 相關圖表
- **## 可觀測性架構圖

```mermaid
graph TB
    subgraph APP ["Spring Boot Application"]
        ACTUATOR[Spring Boot Actuator]
        OTEL[OpenTelemetry Agent]
        LOGBACK[Logback JSON Logging]
        MICROMETER[Micrometer Metrics]
    end
    
    subgraph K8S ["Kubernetes Cluster"]
        FLUENT[Fluent Bit DaemonSet]
        PROMETHEUS[Prometheus]
        GRAFANA[Grafana]
    end
    
    subgraph AWS ["AWS Services"]
        CW_LOGS[CloudWatch Logs]
        CW_METRICS[CloudWatch Metrics]
        XRAY[AWS X-Ray]
        OPENSEARCH[OpenSearch Service]
    end
    
    ACTUATOR --> PROMETHEUS
    LOGBACK --> FLUENT
    OTEL --> XRAY
    MICROMETER --> PROMETHEUS
    
    FLUENT --> CW_LOGS
    PROMETHEUS --> CW_METRICS
    GRAFANA --> PROMETHEUS
    
    CW_LOGS --> OPENSEARCH
    
    classDef application fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef kubernetes fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef aws fill:#fff3e0,stroke:#e65100,stroke-width:2px
    
    class ACTUATOR,OTEL,LOGBACK,MICROMETER application
    class FLUENT,PROMETHEUS,GRAFANA kubernetes
    class CW_LOGS,CW_METRICS,XRAY,OPENSEARCH aws
```** - 監控系統架構
- **[可觀測性詳細圖](diagrams/plantuml/observability-diagram.svg)** - 監控組件詳細設計

## 🎯 觀點間交叉引用

### 安全性觀點 (Security Perspective)

**主文檔**: [安全性觀點總覽](perspectives/security/README.md)

#### 跨視點應用
- **[功能視點安全考量](viewpoints/functional/quality-considerations.md)** - 業務邏輯安全
- **[資訊視點安全考量](viewpoints/information/README.md)** - 資料安全和隱私
- **[開發視點安全考量](viewpoints/development/README.md)** - 安全編碼實踐
- **[部署視點安全考量](viewpoints/deployment/README.md)** - 基礎設施安全
- **[運營視點安全考量](viewpoints/operational/README.md)** - 安全監控和事件響應

#### 相關文檔
- **[跨視點安全應用](perspectives/security/cross-viewpoint-application.md)** - 安全在各視點的具體實現

#### 相關圖表
- **[安全架構圖](diagrams/plantuml/security-architecture-diagram.svg)** - 完整安全架構設計

### 性能觀點 (Performance Perspective)

**主文檔**: [性能觀點總覽](perspectives/performance/README.md)

#### 跨視點應用
- **[資訊視點性能考量](viewpoints/information/README.md)** - 資料存取和查詢優化
- **[並發視點性能考量](viewpoints/concurrency/README.md)** - 並發處理和負載均衡
- **[部署視點性能考量](viewpoints/deployment/README.md)** - 基礎設施性能配置
- **[運營視點性能考量](viewpoints/operational/README.md)** - 性能監控和調優

#### 相關圖表
- **[系統性能架構](diagrams/mermaid/architecture-overview.md)** - 性能關鍵路徑
- **## 事件驅動性能

```mermaid
graph LR
    subgraph 領域事件 ["領域事件"]
        OCE[OrderCreatedEvent]
        OIAE[OrderItemAddedEvent]
        PRE[PaymentRequestedEvent]
        PFE[PaymentFailedEvent]
    end
    
    subgraph 事件處理 ["事件處理"]
        EP[DomainEventPublisherService]
        EB[DomainEventBus]
        OS[OrderProcessingSaga]
    end
    
    subgraph 事件監聽器 ["事件監聽器"]
        PS[PaymentService]
        LS[LogisticsService]
    end
    
    AGG[Order<br>聚合根] -->|產生| OCE
    AGG -->|產生| OIAE
    OCE -->|發布至| EP
    OIAE -->|發布至| EP
    EP -->|發送至| EB
    EB -->|分發| OS
    EB -->|分發| PS
    EB -->|分發| LS
    OS -->|協調| PS
    OS -->|協調| LS
    PS -->|產生| PRE
    PS -->|產生| PFE
    PRE -->|發布至| EP
    PFE -->|發布至| EP
    
    classDef event fill:#ffcc99,stroke:#333,stroke-width:2px
    classDef publisher fill:#99ccff,stroke:#333,stroke-width:2px
    classDef handler fill:#cc99ff,stroke:#333,stroke-width:2px
    classDef aggregateRoot fill:#bbf,stroke:#333,stroke-width:2px
    
    class OCE,OIAE,PRE,PFE event
    class EP,EB publisher
    class OS,PS,LS handler
    class AGG aggregateRoot
```** - 高性能事件處理

### 可用性觀點 (Availability Perspective)

**主文檔**: [可用性觀點總覽](perspectives/availability/README.md)

#### 跨視點應用
- **[功能視點可用性考量](viewpoints/functional/README.md)** - 關鍵功能的容錯設計
- **[並發視點可用性考量](viewpoints/concurrency/README.md)** - 並發故障隔離
- **[部署視點可用性考量](viewpoints/deployment/README.md)** - 高可用部署策略
- **[運營視點可用性考量](viewpoints/operational/README.md)** - 故障檢測和自動恢復

#### 相關圖表
- **## 高可用架構

```mermaid
graph TB
    subgraph DEV ["Development Environment"]
        DEV_APP[Spring Boot App<br/>Profile: dev]
        H2_DB[(H2 Database)]
        MEMORY_EVENTS[In-Memory Events]
    end
    
    subgraph PROD ["Production Environment"]
        PROD_APP[Spring Boot App<br/>Profile: production]
        RDS_DB[(RDS PostgreSQL)]
        MSK_EVENTS[MSK Events]
    end
    
    subgraph CONFIG ["Configuration"]
        BASE_CONFIG[application.yml]
        DEV_CONFIG[application-dev.yml]
        PROD_CONFIG[application-production.yml]
    end
    
    BASE_CONFIG --> DEV_CONFIG
    BASE_CONFIG --> PROD_CONFIG
    
    DEV_CONFIG --> DEV_APP
    PROD_CONFIG --> PROD_APP
    
    DEV_APP --> H2_DB
    DEV_APP --> MEMORY_EVENTS
    
    PROD_APP --> RDS_DB
    PROD_APP --> MSK_EVENTS
    
    classDef dev fill:#e8f5e8,stroke:#2e7d32,stroke-width:2px
    classDef prod fill:#ffebee,stroke:#c62828,stroke-width:2px
    classDef config fill:#fff3e0,stroke:#e65100,stroke-width:2px
    
    class DEV_APP,H2_DB,MEMORY_EVENTS dev
    class PROD_APP,RDS_DB,MSK_EVENTS prod
    class BASE_CONFIG,DEV_CONFIG,PROD_CONFIG config
```** - 多環境高可用設計
- **## 災難恢復架構

```mermaid
graph TB
    subgraph "AWS Infrastructure"
        EKS[EKS Cluster]
        RDS[RDS Database]
        S3[S3 Storage]
        CloudWatch[CloudWatch]
        ALB[Application Load Balancer]
    end
    
    ALB --> EKS
    EKS --> RDS
    EKS --> S3
    EKS --> CloudWatch
```** - 災難恢復策略

### 演進性觀點 (Evolution Perspective)

**主文檔**: [演進性觀點總覽](perspectives/evolution/README.md)

#### 跨視點應用
- **[功能視點演進考量](viewpoints/functional/README.md)** - 功能擴展和業務規則靈活性
- **[開發視點演進考量](viewpoints/development/README.md)** - 程式碼品質和技術債務管理

#### 相關圖表
- **[演進架構圖](diagrams/mermaid/ddd-layered-architecture.md)** - 可演進的分層架構

### 使用性觀點 (Usability Perspective)

**主文檔**: [使用性觀點總覽](perspectives/usability/README.md)

#### 跨視點應用
- **[功能視點使用性考量](viewpoints/functional/README.md)** - 用戶體驗和介面設計

#### 相關圖表
- **[API 交互圖](diagrams/mermaid/api-interactions.md)** - 用戶介面和 API 交互

### 法規觀點 (Regulation Perspective)

**主文檔**: [法規觀點總覽](perspectives/regulation/README.md)

#### 跨視點應用
- **[資訊視點法規考量](viewpoints/information/README.md)** - 資料治理和隱私保護
- **[運營視點法規考量](viewpoints/operational/README.md)** - 合規監控和稽核支援

#### 相關圖表
- **[合規架構圖](diagrams/plantuml/observability-diagram.svg)** - 合規監控和稽核

### 位置觀點 (Location Perspective)

**主文檔**: [位置觀點總覽](perspectives/location/README.md)

#### 跨視點應用
- **[部署視點位置考量](viewpoints/deployment/README.md)** - 地理分佈部署策略

#### 相關圖表
- **## 地理分佈圖

```mermaid
graph TB
    subgraph "AWS Infrastructure"
        EKS[EKS Cluster]
        RDS[RDS Database]
        S3[S3 Storage]
        CloudWatch[CloudWatch]
        ALB[Application Load Balancer]
    end
    
    ALB --> EKS
    EKS --> RDS
    EKS --> S3
    EKS --> CloudWatch
```** - 多區域部署架構

### 成本觀點 (Cost Perspective)

**主文檔**: [成本觀點總覽](perspectives/cost/README.md)

#### 跨視點應用
- **[開發視點成本考量](viewpoints/development/README.md)** - 開發效率和維護成本
- **[部署視點成本考量](viewpoints/deployment/README.md)** - 基礎設施成本優化
- **[運營視點成本考量](viewpoints/operational/README.md)** - 運營成本監控

#### 相關圖表
- **## 成本優化圖

```mermaid
graph TB
    subgraph DEV ["Development Environment"]
        DEV_APP[Spring Boot App<br/>Profile: dev]
        H2_DB[(H2 Database)]
        MEMORY_EVENTS[In-Memory Events]
    end
    
    subgraph PROD ["Production Environment"]
        PROD_APP[Spring Boot App<br/>Profile: production]
        RDS_DB[(RDS PostgreSQL)]
        MSK_EVENTS[MSK Events]
    end
    
    subgraph CONFIG ["Configuration"]
        BASE_CONFIG[application.yml]
        DEV_CONFIG[application-dev.yml]
        PROD_CONFIG[application-production.yml]
    end
    
    BASE_CONFIG --> DEV_CONFIG
    BASE_CONFIG --> PROD_CONFIG
    
    DEV_CONFIG --> DEV_APP
    PROD_CONFIG --> PROD_APP
    
    DEV_APP --> H2_DB
    DEV_APP --> MEMORY_EVENTS
    
    PROD_APP --> RDS_DB
    PROD_APP --> MSK_EVENTS
    
    classDef dev fill:#e8f5e8,stroke:#2e7d32,stroke-width:2px
    classDef prod fill:#ffebee,stroke:#c62828,stroke-width:2px
    classDef config fill:#fff3e0,stroke:#e65100,stroke-width:2px
    
    class DEV_APP,H2_DB,MEMORY_EVENTS dev
    class PROD_APP,RDS_DB,MSK_EVENTS prod
    class BASE_CONFIG,DEV_CONFIG,PROD_CONFIG config
```** - 成本效益架構設計

## 🔧 專業領域交叉引用

### API 設計和整合

#### 核心文檔
- **[API 版本策略](api/API_VERSIONING_STRATEGY.md)** ↔ **[演進性觀點](perspectives/evolution/README.md)**
- **[前端整合指南](api/frontend-integration.md)** ↔ **[使用性觀點](perspectives/usability/README.md)**
- **[可觀測性 API](api/observability-api.md)** ↔ **[運營視點](viewpoints/operational/README.md)**

#### 相關圖表
- **[API 交互圖](diagrams/mermaid/api-interactions.md)** - API 設計和交互關係

### MCP (Model Context Protocol) 整合

#### 核心文檔
- **[MCP 整合指南](mcp/README.md)** ↔ **[開發視點](viewpoints/development/README.md)**
- **[Excalidraw MCP 使用](mcp/excalidraw-mcp-usage-guide.md)** ↔ **[圖表工具指南](diagrams/diagram-tools-guide.md)**

### 測試和品質保證

#### 核心文檔
- **[測試性能監控](testing/test-performance-monitoring.md)** ↔ **[性能觀點](perspectives/performance/README.md)**
- **[測試配置範例](testing/test-configuration-examples.md)** ↔ **[開發視點](viewpoints/development/README.md)**

## 📊 圖表交叉引用

### 按圖表類型分類

#### Mermaid 圖表 (GitHub 直接顯示)
- **[系統架構概覽](diagrams/mermaid/architecture-overview.md)** - 連結到所有視點
- **[DDD 分層架構](diagrams/mermaid/ddd-layered-architecture.md)** - 連結到功能視點和開發視點
- **[六角形架構](diagrams/mermaid/hexagonal-architecture.md)** - 連結到開發視點
- **[事件驅動架構](diagrams/mermaid/event-driven-architecture.md)** - 連結到資訊視點和並發視點
- **[API 交互圖](diagrams/mermaid/api-interactions.md)** - 連結到使用性觀點

#### PlantUML 圖表 (詳細 UML)
- **[領域模型圖](diagrams/plantuml/domain-model-diagram.svg)** - 連結到功能視點
- **[界限上下文圖](diagrams/plantuml/bounded-context-diagram.svg)** - 連結到功能視點
- **[Event Storming 系列](diagrams/plantuml/event-storming/)** - 連結到資訊視點
- **[安全架構圖](diagrams/plantuml/security-architecture-diagram.svg)** - 連結到安全性觀點
- **[部署架構圖](diagrams/plantuml/deployment-diagram.svg)** - 連結到部署視點
- **[可觀測性圖](diagrams/plantuml/observability-diagram.svg)** - 連結到運營視點

### 按視點分類的圖表

#### 功能視點相關圖表
- **[領域模型圖](diagrams/plantuml/domain-model-diagram.svg)**
- **[界限上下文圖](diagrams/plantuml/bounded-context-diagram.svg)**
- **[用例圖](diagrams/plantuml/use-case-diagram.svg)**

#### 資訊視點相關圖表
- **[Event Storming 系列](diagrams/plantuml/event-storming/)**
- **[CQRS 模式圖](diagrams/plantuml/cqrs-pattern-diagram.svg)**
- **[事件溯源圖](diagrams/plantuml/event-sourcing-diagram.svg)**

#### 並發視點相關圖表
- **## 事件驅動架構圖

```mermaid
graph LR
    subgraph 領域事件 ["領域事件"]
        OCE[OrderCreatedEvent]
        OIAE[OrderItemAddedEvent]
        PRE[PaymentRequestedEvent]
        PFE[PaymentFailedEvent]
    end
    
    subgraph 事件處理 ["事件處理"]
        EP[DomainEventPublisherService]
        EB[DomainEventBus]
        OS[OrderProcessingSaga]
    end
    
    subgraph 事件監聽器 ["事件監聽器"]
        PS[PaymentService]
        LS[LogisticsService]
    end
    
    AGG[Order<br>聚合根] -->|產生| OCE
    AGG -->|產生| OIAE
    OCE -->|發布至| EP
    OIAE -->|發布至| EP
    EP -->|發送至| EB
    EB -->|分發| OS
    EB -->|分發| PS
    EB -->|分發| LS
    OS -->|協調| PS
    OS -->|協調| LS
    PS -->|產生| PRE
    PS -->|產生| PFE
    PRE -->|發布至| EP
    PFE -->|發布至| EP
    
    classDef event fill:#ffcc99,stroke:#333,stroke-width:2px
    classDef publisher fill:#99ccff,stroke:#333,stroke-width:2px
    classDef handler fill:#cc99ff,stroke:#333,stroke-width:2px
    classDef aggregateRoot fill:#bbf,stroke:#333,stroke-width:2px
    
    class OCE,OIAE,PRE,PFE event
    class EP,EB publisher
    class OS,PS,LS handler
    class AGG aggregateRoot
```**
- **[並發處理圖](diagrams/mermaid/architecture-overview.md)**

#### 開發視點相關圖表
- **[六角形架構圖](diagrams/mermaid/hexagonal-architecture.md)**
- **[DDD 分層架構圖](diagrams/mermaid/ddd-layered-architecture.md)**

#### 部署視點相關圖表
- **[部署架構圖](diagrams/plantuml/deployment-diagram.svg)**
- **## AWS 基礎設施圖

```mermaid
graph TB
    subgraph "AWS Infrastructure"
        EKS[EKS Cluster]
        RDS[RDS Database]
        S3[S3 Storage]
        CloudWatch[CloudWatch]
        ALB[Application Load Balancer]
    end
    
    ALB --> EKS
    EKS --> RDS
    EKS --> S3
    EKS --> CloudWatch
```**
- **## 多環境架構圖

```mermaid
graph TB
    subgraph DEV ["Development Environment"]
        DEV_APP[Spring Boot App<br/>Profile: dev]
        H2_DB[(H2 Database)]
        MEMORY_EVENTS[In-Memory Events]
    end
    
    subgraph PROD ["Production Environment"]
        PROD_APP[Spring Boot App<br/>Profile: production]
        RDS_DB[(RDS PostgreSQL)]
        MSK_EVENTS[MSK Events]
    end
    
    subgraph CONFIG ["Configuration"]
        BASE_CONFIG[application.yml]
        DEV_CONFIG[application-dev.yml]
        PROD_CONFIG[application-production.yml]
    end
    
    BASE_CONFIG --> DEV_CONFIG
    BASE_CONFIG --> PROD_CONFIG
    
    DEV_CONFIG --> DEV_APP
    PROD_CONFIG --> PROD_APP
    
    DEV_APP --> H2_DB
    DEV_APP --> MEMORY_EVENTS
    
    PROD_APP --> RDS_DB
    PROD_APP --> MSK_EVENTS
    
    classDef dev fill:#e8f5e8,stroke:#2e7d32,stroke-width:2px
    classDef prod fill:#ffebee,stroke:#c62828,stroke-width:2px
    classDef config fill:#fff3e0,stroke:#e65100,stroke-width:2px
    
    class DEV_APP,H2_DB,MEMORY_EVENTS dev
    class PROD_APP,RDS_DB,MSK_EVENTS prod
    class BASE_CONFIG,DEV_CONFIG,PROD_CONFIG config
```**

#### 運營視點相關圖表
- **## 可觀測性架構圖

```mermaid
graph TB
    subgraph APP ["Spring Boot Application"]
        ACTUATOR[Spring Boot Actuator]
        OTEL[OpenTelemetry Agent]
        LOGBACK[Logback JSON Logging]
        MICROMETER[Micrometer Metrics]
    end
    
    subgraph K8S ["Kubernetes Cluster"]
        FLUENT[Fluent Bit DaemonSet]
        PROMETHEUS[Prometheus]
        GRAFANA[Grafana]
    end
    
    subgraph AWS ["AWS Services"]
        CW_LOGS[CloudWatch Logs]
        CW_METRICS[CloudWatch Metrics]
        XRAY[AWS X-Ray]
        OPENSEARCH[OpenSearch Service]
    end
    
    ACTUATOR --> PROMETHEUS
    LOGBACK --> FLUENT
    OTEL --> XRAY
    MICROMETER --> PROMETHEUS
    
    FLUENT --> CW_LOGS
    PROMETHEUS --> CW_METRICS
    GRAFANA --> PROMETHEUS
    
    CW_LOGS --> OPENSEARCH
    
    classDef application fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef kubernetes fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef aws fill:#fff3e0,stroke:#e65100,stroke-width:2px
    
    class ACTUATOR,OTEL,LOGBACK,MICROMETER application
    class FLUENT,PROMETHEUS,GRAFANA kubernetes
    class CW_LOGS,CW_METRICS,XRAY,OPENSEARCH aws
```**
- **[可觀測性詳細圖](diagrams/plantuml/observability-diagram.svg)**

## 🎯 使用建議

### 導航策略

1. **新手入門**: 從 [文檔中心首頁](README.md) 開始，使用角色導航找到適合的入口點
2. **深度學習**: 使用 [Viewpoint-Perspective 矩陣](viewpoint-perspective-matrix.md) 理解關聯關係
3. **實踐應用**: 根據具體需求使用關注點導航快速定位相關文檔
4. **問題解決**: 使用智能搜尋功能和交叉引用快速找到解決方案

### 文檔維護

- **自動化同步**: 使用 `python scripts/sync-diagram-references.py` 維護圖表引用
- **連結檢查**: 定期運行 `./scripts/validate-docs.sh` 檢查連結有效性
- **交叉引用更新**: 新增文檔時更新本文件的交叉引用關係

---

**維護說明**: 本文件隨著架構文檔的演進自動更新，確保交叉引用的準確性和完整性。