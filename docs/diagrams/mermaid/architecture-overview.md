# 系統架構概覽

本文檔展示 GenAI Demo 專案的整體系統架構。

## 整體架構圖

```mermaid
graph TB
    subgraph "🌐 外部系統"
        USER[👤 用戶]
        EXTERNAL[🔗 外部 API]
        DB_EXTERNAL[🗄️ 外部資料庫]
    end
    
    subgraph "🖥️ 表現層 (Presentation Layer)"
        CMC[📱 CMC Frontend<br/>Next.js 14]
        CONSUMER[🛒 Consumer Frontend<br/>Angular 18]
        API[🔌 REST API<br/>Spring Boot 3.4.5]
    end
    
    subgraph "⚙️ 應用層 (Application Layer)"
        ORDER_APP[📦 Order Application Service]
        CUSTOMER_APP[👥 Customer Application Service]
        PRODUCT_APP[🏷️ Product Application Service]
        PAYMENT_APP[💳 Payment Application Service]
    end
    
    subgraph "🏛️ 領域層 (Domain Layer)"
        ORDER_DOM[📋 Order Domain]
        CUSTOMER_DOM[👤 Customer Domain]
        PRODUCT_DOM[📦 Product Domain]
        PAYMENT_DOM[💰 Payment Domain]
        
        subgraph "📊 領域事件"
            EVENTS[🔔 Domain Events]
        end
    end
    
    subgraph "🔧 基礎設施層 (Infrastructure Layer)"
        H2_DB[(🗃️ H2 Database)]
        EVENT_BUS[📡 Event Bus]
        CACHE[⚡ Cache]
        LOGGING[📝 Logging]
    end
    
    subgraph "☁️ 部署環境"
        DOCKER[🐳 Docker]
        K8S[⚓ Kubernetes]
        AWS[☁️ AWS EKS]
    end
    
    %% 用戶交互
    USER --> CMC
    USER --> CONSUMER
    
    %% 前端到 API
    CMC --> API
    CONSUMER --> API
    
    %% API 到應用層
    API --> ORDER_APP
    API --> CUSTOMER_APP
    API --> PRODUCT_APP
    API --> PAYMENT_APP
    
    %% 應用層到領域層
    ORDER_APP --> ORDER_DOM
    CUSTOMER_APP --> CUSTOMER_DOM
    PRODUCT_APP --> PRODUCT_DOM
    PAYMENT_APP --> PAYMENT_DOM
    
    %% 領域事件
    ORDER_DOM --> EVENTS
    CUSTOMER_DOM --> EVENTS
    PRODUCT_DOM --> EVENTS
    PAYMENT_DOM --> EVENTS
    
    %% 基礎設施
    ORDER_APP --> H2_DB
    CUSTOMER_APP --> H2_DB
    PRODUCT_APP --> H2_DB
    PAYMENT_APP --> H2_DB
    
    EVENTS --> EVENT_BUS
    API --> CACHE
    API --> LOGGING
    
    %% 外部系統
    API --> EXTERNAL
    H2_DB --> DB_EXTERNAL
    
    %% 部署
    API --> DOCKER
    CMC --> DOCKER
    CONSUMER --> DOCKER
    DOCKER --> K8S
    K8S --> AWS
    
    %% 樣式
    classDef frontend fill:#e1f5fe
    classDef application fill:#f3e5f5
    classDef domain fill:#e8f5e8
    classDef infrastructure fill:#fff3e0
    classDef external fill:#fce4ec
    
    class CMC,CONSUMER,API frontend
    class ORDER_APP,CUSTOMER_APP,PRODUCT_APP,PAYMENT_APP application
    class ORDER_DOM,CUSTOMER_DOM,PRODUCT_DOM,PAYMENT_DOM,EVENTS domain
    class H2_DB,EVENT_BUS,CACHE,LOGGING infrastructure
    class USER,EXTERNAL,DB_EXTERNAL external
```

## 架構特點

### 🏗️ 分層架構

- **表現層**: 處理用戶界面和 API 端點
- **應用層**: 協調業務用例和事務管理
- **領域層**: 核心業務邏輯和規則
- **基礎設施層**: 技術實現和外部整合

### 🔄 事件驅動

- 使用領域事件實現鬆耦合
- 支援異步處理和最終一致性
- 便於系統擴展和維護

### 🎯 DDD 戰術模式

- 聚合根管理一致性邊界
- 值對象確保資料完整性
- 領域服務處理跨聚合邏輯

### 🚀 現代技術棧

- Java 21 + Spring Boot 3.4.5
- Next.js 14 + Angular 18
- Docker + Kubernetes 部署
- ARM64 優化 (Apple Silicon + AWS Graviton3)

## 相關文檔

- [六角形架構](hexagonal-architecture.md) - 端口與適配器詳解
- [DDD 分層架構](ddd-layered-architecture.md) - DDD 實現細節
- [事件驅動架構](event-driven-architecture.md) - 事件處理機制
- [API 交互圖](api-interactions.md) - API 調用關係
