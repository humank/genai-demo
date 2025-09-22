
# 架構圖表總覽

> **基於 Rozanski & Woods 方法論的系統化架構視覺化**

## Overview

本目錄包含完整的系統架構圖表，按照 Rozanski & Woods 的七大 Viewpoints 和八大 Perspectives 進行組織。我們使用三種互補的圖表工具來滿足不同的視覺化需求。

## Tools

### 🌊 Mermaid - 主要架構圖表
- **用途**: GitHub 直接顯示的架構概覽
- **格式**: `.mmd` 文件
- **優勢**: 原生 GitHub 支援、版本控制友好
- **適用**: 系統概覽、服務互動、Deployment架構

### 📊 PlantUML - 詳細技術圖表  
- **用途**: 詳細的 UML 和技術設計圖
- **格式**: `.puml` 文件 + 自動生成 `.png/.svg`
- **優勢**: 功能強大、UML 標準、複雜圖表支援
- **適用**: 領域模型、Event Storming、時序圖

### Design
- **用途**: 概念設計和手繪風格圖
- **格式**: `.excalidraw` 文件 + 轉換 `.png`
- **優勢**: 直觀易用、手繪風格、AI 輔助
- **適用**: 概念設計、腦力激盪、Stakeholder圖

## 目錄結構

### 📁 按 Viewpoints 組織

```
../../diagrams/
├── viewpoints/                      # 七大Architectural Viewpoint
│   ├── functional/                  # Functional Viewpoint
│   │   ├── system-overview.mmd     # Overview
│   │   ├── domain-model-class.puml # 領域模型類圖 (PlantUML)
│   │   ├── bounded-contexts.mmd    # Bounded Context (Mermaid)
│   │   └── [27+ 自動生成的Aggregate Root圖表]
│   ├── information/                 # Information Viewpoint
│   │   ├── event-driven-architecture.mmd # Event-Driven Architecture (Mermaid)
│   │   ├── event-storming-big-picture.puml # Event Storming (PlantUML)
│   │   └── data-flow.mmd           # 資料流圖 (Mermaid)
│   ├── concurrency/                 # Concurrency Viewpoint
│   │   └── async-processing.mmd    # 非同步處理 (Mermaid)
│   ├── development/                 # Development Viewpoint
│   │   ├── hexagonal-architecture.mmd # Hexagonal Architecture (Mermaid)
│   │   ├── ddd-layered-architecture.mmd # DDD Layered Architecture (Mermaid)
│   │   └── module-dependencies.puml # 模組依賴 (PlantUML)
│   ├── deployment/                  # Deployment
│   │   ├── infrastructure-overview.mmd # Overview
│   │   └── deployment-diagram.puml # Deployment
│   └── operational/                 # Operational Viewpoint
│       ├── monitoring-architecture.mmd # Monitoring架構 (Mermaid)
│       └── observability.puml      # Observability (PlantUML)
├── perspectives/                    # 八大Architectural Perspective
│   ├── security/                   # Security Perspective
│   ├── performance/                # Performance & Scalability Perspective
│   ├── availability/               # Availability & Resilience Perspective
│   ├── evolution/                  # Evolution Perspective
│   ├── usability/                  # Usability Perspective
│   ├── regulation/                 # Regulation Perspective
│   ├── location/                   # Location Perspective
│   └── cost/                       # Cost Perspective
├── concepts/                       # Design
├── legacy/                         # 歷史圖表
└── tools-and-environment/                          # Tools
```

## 🎯 核心架構圖表

### Overview

| 圖表 | 類型 | 描述 | 狀態 |
|------|------|------|------|
| ## 系統概覽

```mermaid
graph TB
    subgraph USERS ["用戶與角色"]
        CUSTOMER[👤 顧客<br/>購物與下單]
        SELLER[🏪 賣家<br/>商品管理]
        ADMIN[👨‍💼 管理員<br/>系統管理]
        DELIVERY[🚚 配送員<br/>物流配送]
    end
    
    subgraph FRONTEND ["前端應用"]
        WEB_APP[🌐 Web 應用<br/>Next.js 14 + TypeScript<br/>顧客購物界面]
        MOBILE_APP[📱 移動應用<br/>Angular 18 + TypeScript<br/>消費者應用]
        ADMIN_PANEL[🖥️ 管理面板<br/>React Admin Dashboard<br/>後台管理系統]
        SELLER_PORTAL[🏪 賣家門戶<br/>商家管理界面<br/>商品與訂單管理]
    end
    
    subgraph API_GATEWAY ["API 網關層"]
        GATEWAY[🚪 API Gateway<br/>路由與認證<br/>限流與監控]
        LOAD_BALANCER[⚖️ 負載均衡器<br/>流量分發<br/>健康檢查]
    end
    
    subgraph MICROSERVICES ["微服務架構"]
        subgraph CORE_SERVICES ["核心業務服務"]
            CUSTOMER_SVC[👤 Customer Service<br/>客戶管理服務<br/>會員系統與檔案]
            ORDER_SVC[📦 Order Service<br/>訂單管理服務<br/>訂單生命週期]
            PRODUCT_SVC[🛍️ Product Service<br/>商品管理服務<br/>商品目錄與搜尋]
            PAYMENT_SVC[💰 Payment Service<br/>支付處理服務<br/>多種支付方式]
            INVENTORY_SVC[📊 Inventory Service<br/>庫存管理服務<br/>庫存追蹤與預留]
        end
        
        subgraph BUSINESS_SERVICES ["業務支援服務"]
            CART_SVC[🛒 Shopping Cart Service<br/>購物車服務<br/>購物流程管理]
            PRICING_SVC[💲 Pricing Service<br/>定價服務<br/>動態定價與折扣]
            PROMOTION_SVC[🎁 Promotion Service<br/>促銷服務<br/>優惠券與活動]
            DELIVERY_SVC[🚚 Delivery Service<br/>配送服務<br/>物流與追蹤]
            REVIEW_SVC[⭐ Review Service<br/>評價服務<br/>商品評價系統]
        end
        
        subgraph PLATFORM_SERVICES ["平台服務"]
            NOTIFICATION_SVC[🔔 Notification Service<br/>通知服務<br/>多渠道消息推送]
            SEARCH_SVC[🔍 Search Service<br/>搜尋服務<br/>全文搜索與推薦]
            ANALYTICS_SVC[📈 Analytics Service<br/>分析服務<br/>數據統計與報表]
            AUDIT_SVC[📋 Audit Service<br/>審計服務<br/>操作日誌與合規]
        end
    end
    
    subgraph INFRASTRUCTURE ["基礎設施層"]
        subgraph DATABASES ["數據存儲"]
            POSTGRES[(🗄️ PostgreSQL<br/>主資料庫<br/>事務性數據)]
            REDIS[(⚡ Redis<br/>快取資料庫<br/>會話與快取)]
            OPENSEARCH[(🔍 OpenSearch<br/>搜尋引擎<br/>全文搜索)]
            S3[(📁 S3<br/>對象存儲<br/>文件與媒體)]
        end
        
        subgraph MESSAGE_QUEUE ["消息隊列"]
            MSK[📊 Amazon MSK<br/>Kafka 集群<br/>事件流處理]
            SQS[📬 Amazon SQS<br/>消息隊列<br/>異步任務處理]
            SNS[📢 Amazon SNS<br/>通知服務<br/>消息推送]
        end
        
        subgraph EXTERNAL_SERVICES ["外部服務"]
            STRIPE[💳 Stripe<br/>支付網關<br/>信用卡處理]
            PAYPAL[💰 PayPal<br/>支付平台<br/>數字錢包]
            EMAIL_SVC[📧 Email Service<br/>郵件服務<br/>SES/SMTP]
            SMS_SVC[📱 SMS Service<br/>簡訊服務<br/>SNS/Twilio]
            LOGISTICS[🚚 Logistics API<br/>物流服務<br/>第三方配送]
        end
    end
    
    subgraph OBSERVABILITY ["可觀測性"]
        MONITORING[📊 Monitoring<br/>Prometheus + Grafana<br/>指標監控]
        LOGGING[📝 Logging<br/>ELK Stack<br/>日誌聚合]
        TRACING[🔍 Tracing<br/>AWS X-Ray<br/>分布式追蹤]
        ALERTING[🚨 Alerting<br/>CloudWatch Alarms<br/>告警通知]
    end
    
    subgraph SECURITY ["安全與合規"]
        IAM[🔐 Identity & Access<br/>AWS IAM<br/>身份認證授權]
        WAF[🛡️ Web Application Firewall<br/>AWS WAF<br/>應用防護]
        SECRETS[🔑 Secrets Management<br/>AWS Secrets Manager<br/>密鑰管理]
        COMPLIANCE[📋 Compliance<br/>合規監控<br/>GDPR/PCI DSS]
    end
    
    %% User to Frontend Connections
    CUSTOMER --> WEB_APP
    CUSTOMER --> MOBILE_APP
    SELLER --> SELLER_PORTAL
    ADMIN --> ADMIN_PANEL
    DELIVERY --> MOBILE_APP
    
    %% Frontend to API Gateway
    WEB_APP --> GATEWAY
    MOBILE_APP --> GATEWAY
    ADMIN_PANEL --> GATEWAY
    SELLER_PORTAL --> GATEWAY
    
    %% API Gateway to Load Balancer
    GATEWAY --> LOAD_BALANCER
    
    %% Load Balancer to Core Services
    LOAD_BALANCER --> CUSTOMER_SVC
    LOAD_BALANCER --> ORDER_SVC
    LOAD_BALANCER --> PRODUCT_SVC
    LOAD_BALANCER --> PAYMENT_SVC
    LOAD_BALANCER --> INVENTORY_SVC
    
    %% Load Balancer to Business Services
    LOAD_BALANCER --> CART_SVC
    LOAD_BALANCER --> PRICING_SVC
    LOAD_BALANCER --> PROMOTION_SVC
    LOAD_BALANCER --> DELIVERY_SVC
    LOAD_BALANCER --> REVIEW_SVC
    
    %% Load Balancer to Platform Services
    LOAD_BALANCER --> NOTIFICATION_SVC
    LOAD_BALANCER --> SEARCH_SVC
    LOAD_BALANCER --> ANALYTICS_SVC
    LOAD_BALANCER --> AUDIT_SVC
    
    %% Service to Database Connections
    CUSTOMER_SVC --> POSTGRES
    ORDER_SVC --> POSTGRES
    PRODUCT_SVC --> POSTGRES
    PAYMENT_SVC --> POSTGRES
    INVENTORY_SVC --> POSTGRES
    CART_SVC --> REDIS
    PRICING_SVC --> REDIS
    PROMOTION_SVC --> POSTGRES
    DELIVERY_SVC --> POSTGRES
    REVIEW_SVC --> POSTGRES
    SEARCH_SVC --> OPENSEARCH
    ANALYTICS_SVC --> POSTGRES
    AUDIT_SVC --> POSTGRES
    
    %% Service to Cache Connections
    CUSTOMER_SVC --> REDIS
    PRODUCT_SVC --> REDIS
    PRICING_SVC --> REDIS
    SEARCH_SVC --> REDIS
    
    %% Service to Message Queue Connections
    ORDER_SVC --> MSK
    PAYMENT_SVC --> MSK
    INVENTORY_SVC --> MSK
    NOTIFICATION_SVC --> MSK
    NOTIFICATION_SVC --> SQS
    NOTIFICATION_SVC --> SNS
    ANALYTICS_SVC --> MSK
    AUDIT_SVC --> MSK
    
    %% Service to External Service Connections
    PAYMENT_SVC --> STRIPE
    PAYMENT_SVC --> PAYPAL
    NOTIFICATION_SVC --> EMAIL_SVC
    NOTIFICATION_SVC --> SMS_SVC
    DELIVERY_SVC --> LOGISTICS
    
    %% File Storage Connections
    PRODUCT_SVC --> S3
    CUSTOMER_SVC --> S3
    AUDIT_SVC --> S3
    
    %% Observability Connections
    CUSTOMER_SVC --> MONITORING
    ORDER_SVC --> MONITORING
    PRODUCT_SVC --> MONITORING
    PAYMENT_SVC --> MONITORING
    INVENTORY_SVC --> MONITORING
    CART_SVC --> MONITORING
    PRICING_SVC --> MONITORING
    PROMOTION_SVC --> MONITORING
    DELIVERY_SVC --> MONITORING
    REVIEW_SVC --> MONITORING
    NOTIFICATION_SVC --> MONITORING
    SEARCH_SVC --> MONITORING
    ANALYTICS_SVC --> MONITORING
    AUDIT_SVC --> MONITORING
    
    MONITORING --> LOGGING
    MONITORING --> TRACING
    MONITORING --> ALERTING
    
    %% Security Connections
    GATEWAY --> IAM
    GATEWAY --> WAF
    CUSTOMER_SVC --> SECRETS
    PAYMENT_SVC --> SECRETS
    NOTIFICATION_SVC --> SECRETS
    AUDIT_SVC --> COMPLIANCE
    
    %% Inter-Service Communication (Event-Driven)
    ORDER_SVC -.->|OrderCreated| INVENTORY_SVC
    ORDER_SVC -.->|OrderCreated| PAYMENT_SVC
    ORDER_SVC -.->|OrderCreated| NOTIFICATION_SVC
    PAYMENT_SVC -.->|PaymentProcessed| ORDER_SVC
    PAYMENT_SVC -.->|PaymentProcessed| DELIVERY_SVC
    INVENTORY_SVC -.->|StockReserved| ORDER_SVC
    INVENTORY_SVC -.->|StockUpdated| PRODUCT_SVC
    CUSTOMER_SVC -.->|CustomerRegistered| NOTIFICATION_SVC
    REVIEW_SVC -.->|ReviewCreated| PRODUCT_SVC
    DELIVERY_SVC -.->|DeliveryStatusChanged| ORDER_SVC
    DELIVERY_SVC -.->|DeliveryStatusChanged| NOTIFICATION_SVC
    
    %% Styling
    classDef user fill:#e3f2fd,stroke:#1976d2,stroke-width:2px
    classDef frontend fill:#e8f5e8,stroke:#388e3c,stroke-width:2px
    classDef gateway fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    classDef core fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef business fill:#e1f5fe,stroke:#0277bd,stroke-width:2px
    classDef platform fill:#fce4ec,stroke:#c2185b,stroke-width:2px
    classDef database fill:#f1f8e9,stroke:#689f38,stroke-width:2px
    classDef message fill:#fff8e1,stroke:#fbc02d,stroke-width:2px
    classDef external fill:#ffebee,stroke:#d32f2f,stroke-width:2px
    classDef observability fill:#f3e5f5,stroke:#9c27b0,stroke-width:2px
    classDef security fill:#e8eaf6,stroke:#3f51b5,stroke-width:2px
    
    class CUSTOMER,SELLER,ADMIN,DELIVERY user
    class WEB_APP,MOBILE_APP,ADMIN_PANEL,SELLER_PORTAL frontend
    class GATEWAY,LOAD_BALANCER gateway
    class CUSTOMER_SVC,ORDER_SVC,PRODUCT_SVC,PAYMENT_SVC,INVENTORY_SVC core
    class CART_SVC,PRICING_SVC,PROMOTION_SVC,DELIVERY_SVC,REVIEW_SVC business
    class NOTIFICATION_SVC,SEARCH_SVC,ANALYTICS_SVC,AUDIT_SVC platform
    class POSTGRES,REDIS,OPENSEARCH,S3 database
    class MSK,SQS,SNS message
    class STRIPE,PAYPAL,EMAIL_SVC,SMS_SVC,LOGISTICS external
    class MONITORING,LOGGING,TRACING,ALERTING observability
    class IAM,WAF,SECRETS,COMPLIANCE security
``` | Mermaid | 完整系統架構概覽，展示7層架構和組件關係 | ✅ 新增 |
| ## Hexagonal Architecture

```mermaid
graph TB
    subgraph "Core Domain"
        Domain[Domain Logic]
        Ports[Ports/Interfaces]
    end
    
    subgraph "Adapters"
        WebAdapter[Web Adapter]
        DatabaseAdapter[Database Adapter]
        MessageAdapter[Message Adapter]
        ExternalAdapter[External Service Adapter]
    end
    
    WebAdapter --> Ports
    Ports --> Domain
    Domain --> Ports
    Ports --> DatabaseAdapter
    Ports --> MessageAdapter
    Ports --> ExternalAdapter
``` | Mermaid | Port和Adapter架構 | ✅ 已更新 |
| ## DDD Layered Architecture

```mermaid
graph TB
    subgraph "Domain Layer"
        Aggregates[Aggregate Roots]
        Entities[Entities]
        ValueObjects[Value Objects]
        DomainEvents[Domain Events]
    end
    
    subgraph "Application Layer"
        ApplicationServices[Application Services]
        CommandHandlers[Command Handlers]
        EventHandlers[Event Handlers]
    end
    
    subgraph "Infrastructure Layer"
        Repositories[Repositories]
        ExternalServices[External Services]
        EventStore[Event Store]
    end
    
    ApplicationServices --> Aggregates
    CommandHandlers --> Aggregates
    EventHandlers --> DomainEvents
    Repositories --> Entities
    ExternalServices --> ApplicationServices
``` | Mermaid | Domain-Driven Design分層 | ✅ 已更新 |
| ## Event-Driven Architecture

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
``` | Mermaid | 事件處理機制 | ✅ 已更新 |

### 領域模型圖表

| 圖表 | 類型 | 描述 | 狀態 |
|------|------|------|------|
| \1 | PlantUML | DDD Aggregate Root總覽 | ✅ 自動生成 |
| \1 | PlantUML | CustomerAggregate Root詳細設計 | ✅ 自動生成 |
| \1 | PlantUML | 訂單Aggregate Root詳細設計 | ✅ 自動生成 |
| \1 | PlantUML | 支付Aggregate Root詳細設計 | ✅ 自動生成 |

### Event Storming 圖表

| 圖表 | 類型 | 描述 | 狀態 |
|------|------|------|------|
| \1 | PlantUML | Event Storming全景圖 | ✅ 自動生成 |
| \1 | PlantUML | Process Level事件圖 | ✅ 自動生成 |
| \1 | PlantUML | 業務流程詳細圖 | ✅ 自動生成 |

### 基礎設施圖表

| 圖表 | 類型 | 描述 | 狀態 |
|------|------|------|------|
| **[AWS 基礎設施架構](aws-infrastructure.md)** | **Mermaid** | **完整 AWS CDK 基礎設施架構文檔** | **✅ 新增** |
| ## AWS 基礎設施圖表

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
``` | Mermaid | AWS 服務架構圖 | ✅ 已更新 |
| ## 多Environment架構

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
``` | Mermaid | 開發/測試/生產Environment | ✅ 已更新 |
| ## Observability架構

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
``` | Mermaid | Monitoring、Logging、Tracing系統 | ✅ 已更新 |
| ## 基礎設施概覽

```mermaid
graph TB
    subgraph "雲端基礎設施" ["雲端基礎設施 (Cloud Infrastructure)"]
        subgraph "AWS 區域" ["AWS Region (us-east-1)"]
            subgraph "可用區 A" ["Availability Zone A"]
                EKS_A[EKS 節點群組 A<br/>Kubernetes Nodes]
                RDS_PRIMARY[(RDS 主資料庫<br/>PostgreSQL Primary)]
                REDIS_A[(Redis 主節點<br/>ElastiCache Primary)]
            end
            
            subgraph "可用區 B" ["Availability Zone B"]
                EKS_B[EKS 節點群組 B<br/>Kubernetes Nodes]
                RDS_STANDBY[(RDS 備用資料庫<br/>PostgreSQL Standby)]
                REDIS_B[(Redis 副本節點<br/>ElastiCache Replica)]
            end
            
            subgraph "可用區 C" ["Availability Zone C"]
                EKS_C[EKS 節點群組 C<br/>Kubernetes Nodes]
                OPENSEARCH[(OpenSearch 集群<br/>Search & Analytics)]
            end
        end
        
        subgraph "全球服務" ["Global Services"]
            CLOUDFRONT[CloudFront<br/>全球 CDN]
            ROUTE53[Route 53<br/>DNS 服務]
            WAF[AWS WAF<br/>Web 應用防火牆]
        end
        
        subgraph "區域服務" ["Regional Services"]
            ALB[Application Load Balancer<br/>應用負載均衡器]
            API_GW[API Gateway<br/>API 管理]
            S3[(S3 存儲桶<br/>檔案存儲)]
            MSK[MSK Kafka<br/>事件流]
            EVENT_BRIDGE[EventBridge<br/>事件路由]
        end
    end
    
    subgraph "容器化平台" ["容器化平台 (Container Platform)"]
        subgraph "EKS 集群" ["EKS Cluster"]
            subgraph "系統命名空間" ["System Namespaces"]
                KUBE_SYSTEM[kube-system<br/>Kubernetes 系統組件]
                AWS_LOAD_BALANCER[aws-load-balancer-controller<br/>負載均衡控制器]
                CLUSTER_AUTOSCALER[cluster-autoscaler<br/>集群自動擴展]
                METRICS_SERVER[metrics-server<br/>指標服務器]
            end
            
            subgraph "應用命名空間" ["Application Namespaces"]
                PROD_NS[production<br/>生產環境]
                STAGING_NS[staging<br/>測試環境]
                MONITORING_NS[monitoring<br/>監控系統]
            end
            
            subgraph "微服務部署" ["Microservices Deployment"]
                CUSTOMER_SVC[customer-service<br/>客戶服務]
                ORDER_SVC[order-service<br/>訂單服務]
                PRODUCT_SVC[product-service<br/>產品服務]
                PAYMENT_SVC[payment-service<br/>支付服務]
                INVENTORY_SVC[inventory-service<br/>庫存服務]
                NOTIFICATION_SVC[notification-service<br/>通知服務]
            end
        end
        
        subgraph "容器註冊表" ["Container Registry"]
            ECR[AWS ECR<br/>容器映像註冊表]
            IMAGE_SCANNING[映像安全掃描<br/>Image Security Scanning]
            LIFECYCLE_POLICY[生命週期政策<br/>Lifecycle Policy]
        end
    end
    
    subgraph "CI/CD 管道" ["CI/CD Pipeline"]
        subgraph "源代碼管理" ["Source Code Management"]
            GITHUB[GitHub<br/>源代碼倉庫]
            GITHUB_ACTIONS[GitHub Actions<br/>CI/CD 工作流程]
        end
        
        subgraph "建置和測試" ["Build & Test"]
            BUILD_STAGE[建置階段<br/>Build Stage]
            TEST_STAGE[測試階段<br/>Test Stage]
            SECURITY_SCAN[安全掃描<br/>Security Scan]
            QUALITY_GATE[品質閘道<br/>Quality Gate]
        end
        
        subgraph "部署自動化" ["Deployment Automation"]
            CDK_DEPLOY[CDK 部署<br/>Infrastructure Deployment]
            K8S_DEPLOY[Kubernetes 部署<br/>Application Deployment]
            ROLLBACK[回滾機制<br/>Rollback Mechanism]
        end
    end
    
    subgraph "基礎設施即代碼" ["基礎設施即代碼 (IaC)"]
        subgraph "AWS CDK" ["AWS CDK"]
            NETWORK_STACK[網路堆疊<br/>Network Stack]
            SECURITY_STACK[安全堆疊<br/>Security Stack]
            DATABASE_STACK[資料庫堆疊<br/>Database Stack]
            APPLICATION_STACK[應用堆疊<br/>Application Stack]
            MONITORING_STACK[監控堆疊<br/>Monitoring Stack]
        end
        
        subgraph "Kubernetes 配置" ["Kubernetes Configuration"]
            HELM_CHARTS[Helm Charts<br/>應用程式包管理]
            KUSTOMIZE[Kustomize<br/>配置管理]
            ARGOCD[ArgoCD<br/>GitOps 部署]
        end
    end
    
    subgraph "監控和可觀測性" ["監控和可觀測性 (Observability)"]
        subgraph "指標監控" ["Metrics Monitoring"]
            PROMETHEUS[Prometheus<br/>指標收集]
            GRAFANA[Grafana<br/>視覺化儀表板]
            CLOUDWATCH[CloudWatch<br/>AWS 原生監控]
        end
        
        subgraph "日誌管理" ["Log Management"]
            FLUENTD[Fluentd<br/>日誌收集器]
            CLOUDWATCH_LOGS[CloudWatch Logs<br/>日誌存儲]
            OPENSEARCH_LOGS[OpenSearch<br/>日誌搜尋分析]
        end
        
        subgraph "分散式追蹤" ["Distributed Tracing"]
            XRAY[AWS X-Ray<br/>分散式追蹤]
            JAEGER[Jaeger<br/>追蹤收集器]
            OTEL[OpenTelemetry<br/>可觀測性框架]
        end
        
        subgraph "告警系統" ["Alerting System"]
            SNS[SNS<br/>通知服務]
            PAGERDUTY[PagerDuty<br/>事件管理]
            SLACK[Slack<br/>團隊通知]
        end
    end
    
    subgraph "安全和合規" ["安全和合規 (Security & Compliance)"]
        subgraph "身份和存取管理" ["Identity & Access Management"]
            IAM[AWS IAM<br/>身份管理]
            RBAC[Kubernetes RBAC<br/>角色存取控制]
            SERVICE_ACCOUNT[Service Account<br/>服務帳戶]
        end
        
        subgraph "網路安全" ["Network Security"]
            VPC[VPC<br/>虛擬私有雲]
            SECURITY_GROUP[Security Groups<br/>安全群組]
            NACL[Network ACLs<br/>網路存取控制清單]
            NAT_GW[NAT Gateway<br/>網路位址轉換]
        end
        
        subgraph "資料保護" ["Data Protection"]
            KMS[AWS KMS<br/>金鑰管理服務]
            SECRETS_MANAGER[Secrets Manager<br/>機密管理]
            ENCRYPTION[資料加密<br/>Data Encryption]
        end
    end
    
    %% 流量路由
    ROUTE53 -->|DNS 解析| CLOUDFRONT
    CLOUDFRONT -->|快取| WAF
    WAF -->|過濾| ALB
    ALB -->|負載均衡| API_GW
    API_GW -->|路由| EKS_A
    API_GW -->|路由| EKS_B
    API_GW -->|路由| EKS_C
    
    %% EKS 集群內部
    EKS_A -->|運行| CUSTOMER_SVC
    EKS_A -->|運行| ORDER_SVC
    EKS_B -->|運行| PRODUCT_SVC
    EKS_B -->|運行| PAYMENT_SVC
    EKS_C -->|運行| INVENTORY_SVC
    EKS_C -->|運行| NOTIFICATION_SVC
    
    %% 資料庫連接
    CUSTOMER_SVC -->|讀寫| RDS_PRIMARY
    ORDER_SVC -->|讀寫| RDS_PRIMARY
    PRODUCT_SVC -->|快取| REDIS_A
    PAYMENT_SVC -->|搜尋| OPENSEARCH
    
    %% 高可用性
    RDS_PRIMARY -.->|複製| RDS_STANDBY
    REDIS_A -.->|複製| REDIS_B
    
    %% 事件處理
    ORDER_SVC -->|發布事件| MSK
    PAYMENT_SVC -->|發布事件| EVENT_BRIDGE
    MSK -->|消費事件| NOTIFICATION_SVC
    
    %% CI/CD 流程
    GITHUB -->|觸發| GITHUB_ACTIONS
    GITHUB_ACTIONS -->|建置| BUILD_STAGE
    BUILD_STAGE -->|測試| TEST_STAGE
    TEST_STAGE -->|掃描| SECURITY_SCAN
    SECURITY_SCAN -->|檢查| QUALITY_GATE
    QUALITY_GATE -->|通過| CDK_DEPLOY
    CDK_DEPLOY -->|部署基礎設施| NETWORK_STACK
    QUALITY_GATE -->|通過| K8S_DEPLOY
    K8S_DEPLOY -->|部署應用| HELM_CHARTS
    
    %% 容器映像管理
    BUILD_STAGE -->|推送映像| ECR
    ECR -->|掃描| IMAGE_SCANNING
    ECR -->|拉取映像| EKS_A
    
    %% 監控連接
    CUSTOMER_SVC -->|指標| PROMETHEUS
    ORDER_SVC -->|日誌| FLUENTD
    PAYMENT_SVC -->|追蹤| XRAY
    PROMETHEUS -->|視覺化| GRAFANA
    FLUENTD -->|轉發| CLOUDWATCH_LOGS
    XRAY -->|分析| JAEGER
    
    %% 告警
    PROMETHEUS -->|告警| SNS
    CLOUDWATCH -->|告警| SNS
    SNS -->|通知| PAGERDUTY
    SNS -->|通知| SLACK
    
    %% 安全
    EKS_A -->|使用| IAM
    CUSTOMER_SVC -->|RBAC| SERVICE_ACCOUNT
    RDS_PRIMARY -->|加密| KMS
    PAYMENT_SVC -->|機密| SECRETS_MANAGER
    
    classDef cloud fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    classDef container fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
    classDef cicd fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef iac fill:#e8f5e8,stroke:#2e7d32,stroke-width:2px
    classDef observability fill:#fff8e1,stroke:#ff8f00,stroke-width:2px
    classDef security fill:#ffebee,stroke:#c62828,stroke-width:2px
    
    class EKS_A,EKS_B,EKS_C,RDS_PRIMARY,RDS_STANDBY,REDIS_A,REDIS_B,OPENSEARCH,CLOUDFRONT,ROUTE53,WAF,ALB,API_GW,S3,MSK,EVENT_BRIDGE cloud
    class KUBE_SYSTEM,AWS_LOAD_BALANCER,CLUSTER_AUTOSCALER,METRICS_SERVER,PROD_NS,STAGING_NS,MONITORING_NS,CUSTOMER_SVC,ORDER_SVC,PRODUCT_SVC,PAYMENT_SVC,INVENTORY_SVC,NOTIFICATION_SVC,ECR,IMAGE_SCANNING,LIFECYCLE_POLICY container
    class GITHUB,GITHUB_ACTIONS,BUILD_STAGE,TEST_STAGE,SECURITY_SCAN,QUALITY_GATE,CDK_DEPLOY,K8S_DEPLOY,ROLLBACK cicd
    class NETWORK_STACK,SECURITY_STACK,DATABASE_STACK,APPLICATION_STACK,MONITORING_STACK,HELM_CHARTS,KUSTOMIZE,ARGOCD iac
    class PROMETHEUS,GRAFANA,CLOUDWATCH,FLUENTD,CLOUDWATCH_LOGS,OPENSEARCH_LOGS,XRAY,JAEGER,OTEL,SNS,PAGERDUTY,SLACK observability
    class IAM,RBAC,SERVICE_ACCOUNT,VPC,SECURITY_GROUP,NACL,NAT_GW,KMS,SECRETS_MANAGER,ENCRYPTION security
``` | Mermaid | 雲端基礎設施架構 | ✅ 已更新 |
| ## Monitoring架構

```mermaid
graph TB
    subgraph "應用層監控" ["應用層監控 (Application Monitoring)"]
        subgraph "微服務" ["Microservices"]
            CUSTOMER_SVC[客戶服務<br/>Customer Service]
            ORDER_SVC[訂單服務<br/>Order Service]
            PRODUCT_SVC[產品服務<br/>Product Service]
            PAYMENT_SVC[支付服務<br/>Payment Service]
            INVENTORY_SVC[庫存服務<br/>Inventory Service]
            NOTIFICATION_SVC[通知服務<br/>Notification Service]
        end
        
        subgraph "應用指標" ["Application Metrics"]
            BUSINESS_METRICS[業務指標<br/>Business Metrics]
            PERFORMANCE_METRICS[性能指標<br/>Performance Metrics]
            ERROR_METRICS[錯誤指標<br/>Error Metrics]
            CUSTOM_METRICS[自定義指標<br/>Custom Metrics]
        end
        
        subgraph "健康檢查" ["Health Checks"]
            LIVENESS_PROBE[存活探針<br/>Liveness Probe]
            READINESS_PROBE[就緒探針<br/>Readiness Probe]
            STARTUP_PROBE[啟動探針<br/>Startup Probe]
            DEPENDENCY_CHECK[依賴檢查<br/>Dependency Check]
        end
    end
    
    subgraph "指標收集層" ["指標收集層 (Metrics Collection)"]
        subgraph "指標暴露" ["Metrics Exposure"]
            ACTUATOR[Spring Actuator<br/>指標端點]
            MICROMETER[Micrometer<br/>指標庫]
            PROMETHEUS_ENDPOINT[Prometheus 端點<br/>/actuator/prometheus]
        end
        
        subgraph "指標收集器" ["Metrics Collectors"]
            PROMETHEUS[Prometheus<br/>指標收集器]
            CLOUDWATCH_AGENT[CloudWatch Agent<br/>AWS 指標收集]
            OTEL_COLLECTOR[OpenTelemetry Collector<br/>統一收集器]
        end
        
        subgraph "指標聚合" ["Metrics Aggregation"]
            PROMETHEUS_FEDERATION[Prometheus Federation<br/>聯邦集群]
            THANOS[Thanos<br/>長期存儲]
            CORTEX[Cortex<br/>多租戶指標]
        end
    end
    
    subgraph "日誌管理層" ["日誌管理層 (Log Management)"]
        subgraph "日誌生成" ["Log Generation"]
            STRUCTURED_LOGS[結構化日誌<br/>Structured Logs]
            APPLICATION_LOGS[應用日誌<br/>Application Logs]
            ACCESS_LOGS[存取日誌<br/>Access Logs]
            AUDIT_LOGS[審計日誌<br/>Audit Logs]
        end
        
        subgraph "日誌收集" ["Log Collection"]
            FLUENTD[Fluentd<br/>日誌收集器]
            FLUENT_BIT[Fluent Bit<br/>輕量級收集器]
            FILEBEAT[Filebeat<br/>檔案日誌收集]
            CLOUDWATCH_LOGS_AGENT[CloudWatch Logs Agent<br/>AWS 日誌代理]
        end
        
        subgraph "日誌處理" ["Log Processing"]
            LOGSTASH[Logstash<br/>日誌處理器]
            LAMBDA_PROCESSOR[Lambda 處理器<br/>無伺服器處理]
            KINESIS_ANALYTICS[Kinesis Analytics<br/>流處理]
        end
    end
    
    subgraph "追蹤系統層" ["追蹤系統層 (Tracing System)"]
        subgraph "追蹤生成" ["Trace Generation"]
            SPRING_SLEUTH[Spring Sleuth<br/>自動追蹤]
            OTEL_JAVA[OpenTelemetry Java<br/>追蹤 SDK]
            CUSTOM_SPANS[自定義 Span<br/>Custom Spans]
        end
        
        subgraph "追蹤收集" ["Trace Collection"]
            JAEGER_AGENT[Jaeger Agent<br/>追蹤代理]
            XRAY_DAEMON[X-Ray Daemon<br/>AWS 追蹤守護程序]
            OTEL_COLLECTOR_TRACE[OpenTelemetry Collector<br/>追蹤收集器]
        end
        
        subgraph "追蹤存儲" ["Trace Storage"]
            JAEGER_BACKEND[Jaeger Backend<br/>追蹤後端]
            XRAY_SERVICE[AWS X-Ray<br/>追蹤服務]
            ELASTICSEARCH_TRACE[Elasticsearch<br/>追蹤存儲]
        end
    end
    
    subgraph "存儲層" ["存儲層 (Storage Layer)"]
        subgraph "時序資料庫" ["Time Series Database"]
            PROMETHEUS_TSDB[Prometheus TSDB<br/>本地時序資料庫]
            CLOUDWATCH_METRICS[CloudWatch Metrics<br/>AWS 指標存儲]
            INFLUXDB[InfluxDB<br/>時序資料庫]
        end
        
        subgraph "日誌存儲" ["Log Storage"]
            CLOUDWATCH_LOGS[CloudWatch Logs<br/>AWS 日誌存儲]
            OPENSEARCH[OpenSearch<br/>搜尋和分析]
            S3_LOGS[S3<br/>長期日誌存儲]
        end
        
        subgraph "追蹤存儲" ["Trace Storage"]
            XRAY_TRACES[X-Ray Traces<br/>AWS 追蹤存儲]
            JAEGER_STORAGE[Jaeger Storage<br/>追蹤資料庫]
            ELASTICSEARCH_TRACES[Elasticsearch<br/>追蹤索引]
        end
    end
    
    subgraph "視覺化層" ["視覺化層 (Visualization Layer)"]
        subgraph "儀表板" ["Dashboards"]
            GRAFANA[Grafana<br/>統一儀表板]
            CLOUDWATCH_DASHBOARD[CloudWatch Dashboard<br/>AWS 原生儀表板]
            KIBANA[Kibana<br/>日誌視覺化]
            JAEGER_UI[Jaeger UI<br/>追蹤視覺化]
        end
        
        subgraph "業務儀表板" ["Business Dashboards"]
            EXECUTIVE_DASHBOARD[高管儀表板<br/>Executive Dashboard]
            OPERATIONAL_DASHBOARD[運營儀表板<br/>Operational Dashboard]
            TECHNICAL_DASHBOARD[技術儀表板<br/>Technical Dashboard]
            SLA_DASHBOARD[SLA 儀表板<br/>SLA Dashboard]
        end
    end
    
    subgraph "告警系統" ["告警系統 (Alerting System)"]
        subgraph "告警規則" ["Alert Rules"]
            PROMETHEUS_ALERTS[Prometheus 告警<br/>Prometheus Alerts]
            CLOUDWATCH_ALARMS[CloudWatch 告警<br/>CloudWatch Alarms]
            CUSTOM_ALERTS[自定義告警<br/>Custom Alerts]
        end
        
        subgraph "告警管理" ["Alert Management"]
            ALERTMANAGER[AlertManager<br/>告警管理器]
            SNS[SNS<br/>通知服務]
            PAGERDUTY[PagerDuty<br/>事件管理]
        end
        
        subgraph "通知渠道" ["Notification Channels"]
            EMAIL[電子郵件<br/>Email]
            SLACK[Slack<br/>團隊通訊]
            SMS[簡訊<br/>SMS]
            WEBHOOK[Webhook<br/>自定義通知]
        end
    end
    
    subgraph "分析和智能" ["分析和智能 (Analytics & Intelligence)"]
        subgraph "異常檢測" ["Anomaly Detection"]
            CLOUDWATCH_ANOMALY[CloudWatch 異常檢測<br/>CloudWatch Anomaly Detection]
            ML_MODELS[機器學習模型<br/>ML Models]
            STATISTICAL_ANALYSIS[統計分析<br/>Statistical Analysis]
        end
        
        subgraph "根因分析" ["Root Cause Analysis"]
            CORRELATION_ENGINE[關聯引擎<br/>Correlation Engine]
            DEPENDENCY_MAP[依賴映射<br/>Dependency Map]
            IMPACT_ANALYSIS[影響分析<br/>Impact Analysis]
        end
        
        subgraph "預測分析" ["Predictive Analytics"]
            CAPACITY_PLANNING[容量規劃<br/>Capacity Planning]
            TREND_ANALYSIS[趨勢分析<br/>Trend Analysis]
            FORECASTING[預測<br/>Forecasting]
        end
    end
    
    %% 應用層到指標收集
    CUSTOMER_SVC -->|暴露指標| ACTUATOR
    ORDER_SVC -->|暴露指標| ACTUATOR
    PRODUCT_SVC -->|暴露指標| ACTUATOR
    PAYMENT_SVC -->|暴露指標| ACTUATOR
    INVENTORY_SVC -->|暴露指標| ACTUATOR
    NOTIFICATION_SVC -->|暴露指標| ACTUATOR
    
    ACTUATOR -->|使用| MICROMETER
    MICROMETER -->|暴露| PROMETHEUS_ENDPOINT
    
    %% 健康檢查
    CUSTOMER_SVC -->|健康檢查| LIVENESS_PROBE
    ORDER_SVC -->|就緒檢查| READINESS_PROBE
    PAYMENT_SVC -->|啟動檢查| STARTUP_PROBE
    
    %% 指標收集
    PROMETHEUS_ENDPOINT -->|抓取| PROMETHEUS
    PROMETHEUS_ENDPOINT -->|收集| CLOUDWATCH_AGENT
    PROMETHEUS_ENDPOINT -->|收集| OTEL_COLLECTOR
    
    %% 指標聚合
    PROMETHEUS -->|聯邦| PROMETHEUS_FEDERATION
    PROMETHEUS -->|長期存儲| THANOS
    
    %% 日誌流程
    CUSTOMER_SVC -->|生成| STRUCTURED_LOGS
    ORDER_SVC -->|生成| APPLICATION_LOGS
    PAYMENT_SVC -->|生成| AUDIT_LOGS
    
    STRUCTURED_LOGS -->|收集| FLUENTD
    APPLICATION_LOGS -->|收集| FLUENT_BIT
    AUDIT_LOGS -->|收集| CLOUDWATCH_LOGS_AGENT
    
    FLUENTD -->|處理| LOGSTASH
    FLUENT_BIT -->|處理| LAMBDA_PROCESSOR
    
    %% 追蹤流程
    CUSTOMER_SVC -->|生成追蹤| SPRING_SLEUTH
    ORDER_SVC -->|生成追蹤| OTEL_JAVA
    PAYMENT_SVC -->|自定義 Span| CUSTOM_SPANS
    
    SPRING_SLEUTH -->|發送| JAEGER_AGENT
    OTEL_JAVA -->|發送| XRAY_DAEMON
    CUSTOM_SPANS -->|發送| OTEL_COLLECTOR_TRACE
    
    JAEGER_AGENT -->|存儲| JAEGER_BACKEND
    XRAY_DAEMON -->|存儲| XRAY_SERVICE
    
    %% 存儲
    PROMETHEUS -->|存儲| PROMETHEUS_TSDB
    CLOUDWATCH_AGENT -->|存儲| CLOUDWATCH_METRICS
    LOGSTASH -->|存儲| OPENSEARCH
    LAMBDA_PROCESSOR -->|存儲| CLOUDWATCH_LOGS
    JAEGER_BACKEND -->|存儲| JAEGER_STORAGE
    
    %% 視覺化
    PROMETHEUS_TSDB -->|查詢| GRAFANA
    CLOUDWATCH_METRICS -->|視覺化| CLOUDWATCH_DASHBOARD
    OPENSEARCH -->|視覺化| KIBANA
    JAEGER_STORAGE -->|視覺化| JAEGER_UI
    
    %% 業務儀表板
    GRAFANA -->|展示| EXECUTIVE_DASHBOARD
    GRAFANA -->|展示| OPERATIONAL_DASHBOARD
    GRAFANA -->|展示| TECHNICAL_DASHBOARD
    CLOUDWATCH_DASHBOARD -->|展示| SLA_DASHBOARD
    
    %% 告警
    PROMETHEUS -->|告警規則| PROMETHEUS_ALERTS
    CLOUDWATCH_METRICS -->|告警| CLOUDWATCH_ALARMS
    
    PROMETHEUS_ALERTS -->|管理| ALERTMANAGER
    CLOUDWATCH_ALARMS -->|通知| SNS
    ALERTMANAGER -->|路由| PAGERDUTY
    
    SNS -->|發送| EMAIL
    PAGERDUTY -->|通知| SLACK
    ALERTMANAGER -->|發送| SMS
    
    %% 分析和智能
    CLOUDWATCH_METRICS -->|異常檢測| CLOUDWATCH_ANOMALY
    PROMETHEUS_TSDB -->|分析| ML_MODELS
    OPENSEARCH -->|關聯分析| CORRELATION_ENGINE
    
    CORRELATION_ENGINE -->|依賴映射| DEPENDENCY_MAP
    ML_MODELS -->|容量規劃| CAPACITY_PLANNING
    CLOUDWATCH_ANOMALY -->|趨勢分析| TREND_ANALYSIS
    
    classDef application fill:#e8f5e8,stroke:#2e7d32,stroke-width:2px
    classDef collection fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
    classDef processing fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef storage fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    classDef visualization fill:#fff8e1,stroke:#ff8f00,stroke-width:2px
    classDef alerting fill:#ffebee,stroke:#c62828,stroke-width:2px
    classDef intelligence fill:#f1f8e9,stroke:#33691e,stroke-width:2px
    
    class CUSTOMER_SVC,ORDER_SVC,PRODUCT_SVC,PAYMENT_SVC,INVENTORY_SVC,NOTIFICATION_SVC,BUSINESS_METRICS,PERFORMANCE_METRICS,ERROR_METRICS,CUSTOM_METRICS,LIVENESS_PROBE,READINESS_PROBE,STARTUP_PROBE,DEPENDENCY_CHECK application
    class ACTUATOR,MICROMETER,PROMETHEUS_ENDPOINT,PROMETHEUS,CLOUDWATCH_AGENT,OTEL_COLLECTOR,PROMETHEUS_FEDERATION,THANOS,CORTEX collection
    class STRUCTURED_LOGS,APPLICATION_LOGS,ACCESS_LOGS,AUDIT_LOGS,FLUENTD,FLUENT_BIT,FILEBEAT,CLOUDWATCH_LOGS_AGENT,LOGSTASH,LAMBDA_PROCESSOR,KINESIS_ANALYTICS,SPRING_SLEUTH,OTEL_JAVA,CUSTOM_SPANS,JAEGER_AGENT,XRAY_DAEMON,OTEL_COLLECTOR_TRACE processing
    class PROMETHEUS_TSDB,CLOUDWATCH_METRICS,INFLUXDB,CLOUDWATCH_LOGS,OPENSEARCH,S3_LOGS,XRAY_TRACES,JAEGER_STORAGE,ELASTICSEARCH_TRACES,JAEGER_BACKEND,XRAY_SERVICE,ELASTICSEARCH_TRACE storage
    class GRAFANA,CLOUDWATCH_DASHBOARD,KIBANA,JAEGER_UI,EXECUTIVE_DASHBOARD,OPERATIONAL_DASHBOARD,TECHNICAL_DASHBOARD,SLA_DASHBOARD visualization
    class PROMETHEUS_ALERTS,CLOUDWATCH_ALARMS,CUSTOM_ALERTS,ALERTMANAGER,SNS,PAGERDUTY,EMAIL,SLACK,SMS,WEBHOOK alerting
    class CLOUDWATCH_ANOMALY,ML_MODELS,STATISTICAL_ANALYSIS,CORRELATION_ENGINE,DEPENDENCY_MAP,IMPACT_ANALYSIS,CAPACITY_PLANNING,TREND_ANALYSIS,FORECASTING intelligence
``` | Mermaid | Observability平台 | ✅ 已更新 |
| ## 非同步處理

```mermaid
graph TB
    subgraph "同步處理層" ["同步處理層 (Synchronous Processing)"]
        WEB_REQUEST[Web 請求<br/>HTTP Request]
        API_CONTROLLER[API 控制器<br/>REST Controller]
        APP_SERVICE[應用服務<br/>Application Service]
        VALIDATION[輸入驗證<br/>Input Validation]
        IMMEDIATE_RESPONSE[即時響應<br/>Immediate Response]
    end
    
    subgraph "非同步處理層" ["非同步處理層 (Asynchronous Processing)"]
        subgraph "事件驅動處理" ["Event-Driven Processing"]
            EVENT_PUBLISHER[事件發布器<br/>Event Publisher]
            EVENT_BUS[事件匯流排<br/>Event Bus]
            EVENT_HANDLER[事件處理器<br/>Event Handler]
            SAGA_COORDINATOR[Saga 協調器<br/>Saga Coordinator]
        end
        
        subgraph "背景任務處理" ["Background Task Processing"]
            TASK_SCHEDULER[任務調度器<br/>Task Scheduler]
            ASYNC_EXECUTOR[非同步執行器<br/>Async Executor]
            BATCH_PROCESSOR[批次處理器<br/>Batch Processor]
            RETRY_MECHANISM[重試機制<br/>Retry Mechanism]
        end
        
        subgraph "訊息佇列處理" ["Message Queue Processing"]
            MESSAGE_PRODUCER[訊息生產者<br/>Message Producer]
            MESSAGE_QUEUE[訊息佇列<br/>Message Queue]
            MESSAGE_CONSUMER[訊息消費者<br/>Message Consumer]
            DLQ[死信佇列<br/>Dead Letter Queue]
        end
    end
    
    subgraph "並發控制" ["並發控制 (Concurrency Control)"]
        subgraph "鎖定機制" ["Locking Mechanisms"]
            OPTIMISTIC_LOCK[樂觀鎖<br/>Optimistic Locking]
            PESSIMISTIC_LOCK[悲觀鎖<br/>Pessimistic Locking]
            DISTRIBUTED_LOCK[分散式鎖<br/>Distributed Lock]
        end
        
        subgraph "線程池管理" ["Thread Pool Management"]
            WEB_THREAD_POOL[Web 線程池<br/>Web Thread Pool]
            ASYNC_THREAD_POOL[非同步線程池<br/>Async Thread Pool]
            SCHEDULED_THREAD_POOL[調度線程池<br/>Scheduled Thread Pool]
            VIRTUAL_THREAD_POOL[虛擬線程池<br/>Virtual Thread Pool]
        end
        
        subgraph "資源管理" ["Resource Management"]
            CONNECTION_POOL[連接池<br/>Connection Pool]
            CACHE_MANAGER[快取管理器<br/>Cache Manager]
            RATE_LIMITER[速率限制器<br/>Rate Limiter]
            CIRCUIT_BREAKER[斷路器<br/>Circuit Breaker]
        end
    end
    
    subgraph "非同步模式" ["非同步模式 (Async Patterns)"]
        subgraph "Future 模式" ["Future Pattern"]
            COMPLETABLE_FUTURE[CompletableFuture<br/>可完成的 Future]
            ASYNC_RESULT[非同步結果<br/>Async Result]
            CALLBACK_HANDLER[回調處理器<br/>Callback Handler]
        end
        
        subgraph "響應式模式" ["Reactive Pattern"]
            REACTIVE_STREAM[響應式流<br/>Reactive Stream]
            PUBLISHER[發布者<br/>Publisher]
            SUBSCRIBER[訂閱者<br/>Subscriber]
            BACKPRESSURE[背壓控制<br/>Backpressure]
        end
        
        subgraph "Actor 模式" ["Actor Pattern"]
            ACTOR_SYSTEM[Actor 系統<br/>Actor System]
            MESSAGE_PASSING[訊息傳遞<br/>Message Passing]
            MAILBOX[信箱<br/>Mailbox]
        end
    end
    
    subgraph "外部系統整合" ["外部系統整合 (External Integration)"]
        PAYMENT_API[支付 API<br/>Payment API]
        EMAIL_SERVICE[郵件服務<br/>Email Service]
        LOGISTICS_API[物流 API<br/>Logistics API]
        SEARCH_ENGINE[搜尋引擎<br/>Search Engine]
        ANALYTICS_SERVICE[分析服務<br/>Analytics Service]
    end
    
    subgraph "監控和可觀測性" ["監控和可觀測性 (Monitoring)"]
        ASYNC_METRICS[非同步指標<br/>Async Metrics]
        THREAD_MONITORING[線程監控<br/>Thread Monitoring]
        QUEUE_MONITORING[佇列監控<br/>Queue Monitoring]
        PERFORMANCE_TRACKING[性能追蹤<br/>Performance Tracking]
    end
    
    %% 同步處理流程
    WEB_REQUEST -->|HTTP| API_CONTROLLER
    API_CONTROLLER -->|調用| APP_SERVICE
    APP_SERVICE -->|驗證| VALIDATION
    VALIDATION -->|通過| IMMEDIATE_RESPONSE
    API_CONTROLLER -->|返回| IMMEDIATE_RESPONSE
    
    %% 非同步事件處理
    APP_SERVICE -->|發布事件| EVENT_PUBLISHER
    EVENT_PUBLISHER -->|發送| EVENT_BUS
    EVENT_BUS -->|分發| EVENT_HANDLER
    EVENT_HANDLER -->|協調| SAGA_COORDINATOR
    
    %% 背景任務處理
    APP_SERVICE -->|提交任務| TASK_SCHEDULER
    TASK_SCHEDULER -->|執行| ASYNC_EXECUTOR
    ASYNC_EXECUTOR -->|批次處理| BATCH_PROCESSOR
    BATCH_PROCESSOR -->|失敗重試| RETRY_MECHANISM
    
    %% 訊息佇列處理
    EVENT_PUBLISHER -->|生產訊息| MESSAGE_PRODUCER
    MESSAGE_PRODUCER -->|發送| MESSAGE_QUEUE
    MESSAGE_QUEUE -->|消費| MESSAGE_CONSUMER
    MESSAGE_CONSUMER -->|失敗| DLQ
    
    %% 並發控制
    APP_SERVICE -->|使用| OPTIMISTIC_LOCK
    SAGA_COORDINATOR -->|使用| DISTRIBUTED_LOCK
    ASYNC_EXECUTOR -->|管理| ASYNC_THREAD_POOL
    API_CONTROLLER -->|使用| WEB_THREAD_POOL
    TASK_SCHEDULER -->|使用| SCHEDULED_THREAD_POOL
    
    %% 資源管理
    APP_SERVICE -->|使用| CONNECTION_POOL
    EVENT_HANDLER -->|使用| CACHE_MANAGER
    API_CONTROLLER -->|限制| RATE_LIMITER
    MESSAGE_CONSUMER -->|保護| CIRCUIT_BREAKER
    
    %% 非同步模式
    ASYNC_EXECUTOR -->|返回| COMPLETABLE_FUTURE
    COMPLETABLE_FUTURE -->|完成| ASYNC_RESULT
    ASYNC_RESULT -->|回調| CALLBACK_HANDLER
    
    EVENT_BUS -->|流| REACTIVE_STREAM
    REACTIVE_STREAM -->|發布| PUBLISHER
    PUBLISHER -->|訂閱| SUBSCRIBER
    SUBSCRIBER -->|控制| BACKPRESSURE
    
    SAGA_COORDINATOR -->|使用| ACTOR_SYSTEM
    ACTOR_SYSTEM -->|傳遞| MESSAGE_PASSING
    MESSAGE_PASSING -->|存儲| MAILBOX
    
    %% 外部系統整合
    EVENT_HANDLER -->|調用| PAYMENT_API
    MESSAGE_CONSUMER -->|發送| EMAIL_SERVICE
    ASYNC_EXECUTOR -->|查詢| LOGISTICS_API
    BATCH_PROCESSOR -->|索引| SEARCH_ENGINE
    SAGA_COORDINATOR -->|報告| ANALYTICS_SERVICE
    
    %% 監控
    ASYNC_EXECUTOR -->|指標| ASYNC_METRICS
    ASYNC_THREAD_POOL -->|監控| THREAD_MONITORING
    MESSAGE_QUEUE -->|監控| QUEUE_MONITORING
    COMPLETABLE_FUTURE -->|追蹤| PERFORMANCE_TRACKING
    
    classDef sync fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
    classDef async fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef concurrency fill:#e8f5e8,stroke:#2e7d32,stroke-width:2px
    classDef pattern fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    classDef external fill:#ffebee,stroke:#c62828,stroke-width:2px
    classDef monitoring fill:#f1f8e9,stroke:#33691e,stroke-width:2px
    
    class WEB_REQUEST,API_CONTROLLER,APP_SERVICE,VALIDATION,IMMEDIATE_RESPONSE sync
    class EVENT_PUBLISHER,EVENT_BUS,EVENT_HANDLER,SAGA_COORDINATOR,TASK_SCHEDULER,ASYNC_EXECUTOR,BATCH_PROCESSOR,RETRY_MECHANISM,MESSAGE_PRODUCER,MESSAGE_QUEUE,MESSAGE_CONSUMER,DLQ async
    class OPTIMISTIC_LOCK,PESSIMISTIC_LOCK,DISTRIBUTED_LOCK,WEB_THREAD_POOL,ASYNC_THREAD_POOL,SCHEDULED_THREAD_POOL,VIRTUAL_THREAD_POOL,CONNECTION_POOL,CACHE_MANAGER,RATE_LIMITER,CIRCUIT_BREAKER concurrency
    class COMPLETABLE_FUTURE,ASYNC_RESULT,CALLBACK_HANDLER,REACTIVE_STREAM,PUBLISHER,SUBSCRIBER,BACKPRESSURE,ACTOR_SYSTEM,MESSAGE_PASSING,MAILBOX pattern
    class PAYMENT_API,EMAIL_SERVICE,LOGISTICS_API,SEARCH_ENGINE,ANALYTICS_SERVICE external
    class ASYNC_METRICS,THREAD_MONITORING,QUEUE_MONITORING,PERFORMANCE_TRACKING monitoring
``` | Mermaid | 並發和非同步架構 | ✅ 已更新 |

## 🔄 自動化圖表生成

### 生成所有圖表

```bash
# 生成所有類型的圖表
./scripts/generate-all-diagrams.sh

# 只生成特定類型
./scripts/generate-all-diagrams.sh --plantuml
./scripts/generate-all-diagrams.sh --mermaid
./scripts/generate-all-diagrams.sh --excalidraw

# 清理後重新生成
./scripts/generate-all-diagrams.sh --clean
```

### Kiro Hook 自動化

系統已配置 Kiro Hook 來自動Monitoring程式碼變更並更新相關圖表：

- **DDD 註解Monitoring**: Monitoring `@AggregateRoot`、`@ValueObject`、`@Entity` 變更
- **BDD Feature Monitoring**: Monitoring `.feature` 檔案變更
- **自動圖表生成**: 程式碼變更時自動更新 PlantUML 圖表

## 📊 圖表統計

### 當前圖表數量

- **Mermaid 圖表**: 6 個主要架構圖
- **PlantUML 圖表**: 27+ 個自動生成的詳細圖表
- **Excalidraw 圖表**: 概念設計圖 (按需創建)
- **PNG/SVG 輸出**: 自動生成的圖片文件

### 覆蓋範圍

- ✅ **Functional Viewpoint**: 系統概覽、領域模型、Aggregate Root設計
- ✅ **Information Viewpoint**: Event-Driven Architecture、Event Storming 分析
- ✅ **Concurrency Viewpoint**: 非同步處理、並發控制
- ✅ **Development Viewpoint**: Hexagonal Architecture、DDD Layered Architecture
- ✅ **Deployment Viewpoint**: 基礎設施、Containerization、CI/CD
- ✅ **Operational Viewpoint**: Monitoring、Observability、告警

## Maintenance

### 更新圖表

1. **Mermaid 圖表**: 直接編輯 `.mmd` 文件
2. **PlantUML 圖表**: 編輯 `.puml` 文件，運行生成腳本
3. **Excalidraw 圖表**: 使用 Excalidraw 編輯器或 MCP 整合

### 品質檢查

```bash
# 驗證現有圖表
./scripts/generate-all-diagrams.sh --validate

# 生成圖表報告
./scripts/generate-all-diagrams.sh --report
```

### Best Practices

1. **命名規範**: 使用 `kebab-case` 命名
2. **目錄組織**: 按 Viewpoint 分類存放
3. **版本控制**: 源文件納入 Git，PNG 文件可選
4. **文檔關聯**: 在 Markdown 中引用圖表
5. **定期更新**: 保持圖表與實際實現同步

## Resources

- **[圖表工具使用指南](diagram-tools-guide.md)**: 詳細的工具使用說明
- **[Viewpoints 總覽](../viewpoints/README.md)**: 七大Architectural Viewpoint文檔
- **[Perspectives 總覽](../perspectives/README.md)**: 八大Architectural Perspective文檔
- **[自動化腳本](README.md)**: 圖表生成和維護腳本

---

**維護者**: 架構團隊  
**最後更新**: 2025年1月21日  
**圖表工具**: Mermaid + PlantUML + Excalidraw  
**自動化**: Kiro Hook + GitHub Actions