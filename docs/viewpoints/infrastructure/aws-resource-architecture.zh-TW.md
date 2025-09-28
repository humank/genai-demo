# Infrastructure Viewpoint - AWS 資源架構設計

**文件版本**: 1.0  
**最後更新**: 2025年9月24日 下午5:15 (台北時間)  
**作者**: Architecture Team  
**狀態**: Active

## 📋 目錄

- 概覽
- 架構設計原則
- AWS 資源架構
- 網路架構
- 計算資源
- 資料儲存
- 訊息服務
- 監控和可觀測性
- 成本優化
- 架構圖表

## 概覽

GenAI Demo 採用 Multi-Region Active-Active 架構，部署在 AWS 雲端平台上，主要服務台灣和亞太地區用戶。系統設計遵循 AWS Well-Architected Framework 的五大支柱，確保高可用性、安全性、效能、成本效益和營運卓越。

### 核心設計目標

- **高可用性**: 99.9% 可用性目標，支援跨區域故障轉移
- **可擴展性**: 支援 10x 流量增長，自動擴展機制
- **安全性**: 零信任架構，端到端加密
- **成本效益**: 按需付費，資源優化
- **營運卓越**: 自動化部署，全面監控

## 架構設計原則

### 1. 雲端原生設計 (Cloud-Native)

```yaml
容器化: 
  - 應用程式: Docker 容器
  - 編排: Amazon EKS (Kubernetes)
  - 映像: Amazon ECR

微服務架構:
  - 領域驅動設計 (DDD)
  - 事件驅動架構
  - API-First 設計

無伺服器服務:
  - Lambda: 自動化任務
  - EventBridge: 事件路由
  - Step Functions: 工作流程
```

### 2. 多區域架構 (Multi-Region)

```yaml
主要區域: ap-east-2 (台北)
  - 用途: 主要服務區域
  - 用戶: 台灣、香港、東南亞
  - 服務: 完整服務堆疊

次要區域: ap-northeast-1 (東京)
  - 用途: 災難恢復、讀取副本
  - 用戶: 日本、韓國
  - 服務: 讀取服務、故障轉移
```

### 3. 基礎設施即程式碼 (IaC)

```yaml
工具: AWS CDK (TypeScript)
版本控制: Git
部署: CI/CD Pipeline
測試: 單元測試 + 整合測試
```

## AWS 資源架構

### 整體架構概覽

```mermaid
graph TB
    subgraph "Internet"
        User[用戶]
        DNS[Route 53]
    end
    
    subgraph "ap-east-2 (台北) - 主要區域"
        subgraph "Network Layer"
            VPC1[VPC 10.0.0.0/16]
            ALB1[Application Load Balancer]
            NAT1[NAT Gateway]
        end
        
        subgraph "Compute Layer"
            EKS1[EKS Cluster]
            Nodes1[Worker Nodes]
            Pods1[Application Pods]
        end
        
        subgraph "Data Layer"
            Aurora1[Aurora Global - Writer]
            Redis1[ElastiCache Redis]
            MSK1[MSK Kafka]
            Glue1[Glue Data Catalog]
        end
        
        subgraph "Monitoring"
            CW1[CloudWatch]
            XRay1[X-Ray]
            Grafana1[Managed Grafana]
        end
    end
    
    subgraph "ap-northeast-1 (東京) - 次要區域"
        subgraph "Network Layer DR"
            VPC2[VPC 10.1.0.0/16]
            ALB2[Application Load Balancer]
        end
        
        subgraph "Compute Layer DR"
            EKS2[EKS Cluster]
            Nodes2[Worker Nodes]
        end
        
        subgraph "Data Layer DR"
            Aurora2[Aurora Global - Reader]
            Redis2[ElastiCache Redis]
            MSK2[MSK Kafka]
            Glue2[Glue Data Catalog DR]
        end
    end
    
    User --> DNS
    DNS --> ALB1
    DNS -.-> ALB2
    ALB1 --> EKS1
    ALB2 --> EKS2
    EKS1 --> Aurora1
    EKS2 --> Aurora2
    Aurora1 -.-> Aurora2
    Aurora1 --> Glue1
    Aurora2 --> Glue2
    Glue1 -.-> Glue2
    
    style VPC1 fill:#e1f5fe
    style VPC2 fill:#fff3e0
    style Aurora1 fill:#c8e6c9
    style Aurora2 fill:#ffcdd2
```

### CDK Stack 架構

```mermaid
graph TD
    subgraph "CDK Application"
        App[CDK App]
        
        subgraph "Foundation Stacks"
            Network[NetworkStack]
            Security[SecurityStack]
        end
        
        subgraph "Core Stacks"
            EKS[EKSStack]
            RDS[RdsStack]
            Cache[ElastiCacheStack]
            MSK[MSKStack]
        end
        
        subgraph "Platform Stacks"
            Observability[ObservabilityStack]
            Alerting[AlertingStack]
            Core[CoreStack]
        end
        
        subgraph "Optional Stacks"
            Analytics[AnalyticsStack]
            DR[DisasterRecoveryStack]
        end
    end
    
    App --> Network
    App --> Security
    Network --> EKS
    Network --> RDS
    Network --> Cache
    Network --> MSK
    Security --> EKS
    Security --> RDS
    EKS --> Observability
    RDS --> Observability
    Observability --> Alerting
    Core --> Analytics
    
    style Network fill:#e3f2fd
    style Security fill:#fce4ec
    style EKS fill:#e8f5e8
    style RDS fill:#fff3e0
```

## 網路架構

### VPC 設計

```mermaid
graph TB
    subgraph "VPC 10.0.0.0/16"
        subgraph "可用區域 A"
            PubA[Public Subnet<br/>10.0.0.0/24]
            PrivA[Private Subnet<br/>10.0.2.0/24]
            DBA[DB Subnet<br/>10.0.4.0/28]
        end
        
        subgraph "可用區域 B"
            PubB[Public Subnet<br/>10.0.1.0/24]
            PrivB[Private Subnet<br/>10.0.3.0/24]
            DBB[DB Subnet<br/>10.0.5.0/28]
        end
        
        subgraph "網路元件"
            IGW[Internet Gateway]
            NAT[NAT Gateway]
            ALB[Application Load Balancer]
        end
        
        subgraph "安全群組"
            ALBSG[ALB Security Group]
            AppSG[App Security Group]
            DBSG[DB Security Group]
        end
    end
    
    Internet --> IGW
    IGW --> PubA
    IGW --> PubB
    PubA --> NAT
    NAT --> PrivA
    NAT --> PrivB
    PubA --> ALB
    PubB --> ALB
    ALB --> PrivA
    ALB --> PrivB
    PrivA --> DBA
    PrivB --> DBB
    ALB -.-> ALBSG
    PrivA -.-> AppSG
    DBA -.-> DBSG
    
    style PubA fill:#e1f5fe
    style PubB fill:#e1f5fe
    style PrivA fill:#e8f5e8
    style PrivB fill:#e8f5e8
    style DBA fill:#fff3e0
    style DBB fill:#fff3e0
```

### 網路安全設計

```yaml
安全群組規則:
  ALB Security Group:
    入站:
      - Port 80 (HTTP): 0.0.0.0/0
      - Port 443 (HTTPS): 0.0.0.0/0
    出站:
      - Port 8080: App Security Group

  App Security Group:
    入站:
      - Port 8080: ALB Security Group
      - Port 22: Bastion Security Group (管理用)
    出站:
      - Port 443: 0.0.0.0/0 (AWS APIs)
      - Port 5432: DB Security Group
      - Port 6379: Redis Security Group
      - Port 9092: MSK Security Group

  DB Security Group:
    入站:
      - Port 5432: App Security Group
    出站: 無
```

## 計算資源

### Amazon EKS 架構

```mermaid
graph TB
    subgraph "EKS Control Plane"
        API[Kubernetes API Server]
        ETCD[etcd]
        Scheduler[kube-scheduler]
        Controller[kube-controller-manager]
    end
    
    subgraph "EKS Data Plane"
        subgraph "Managed Node Group"
            Node1[Worker Node 1<br/>t3.medium]
            Node2[Worker Node 2<br/>t3.medium]
            Node3[Worker Node 3<br/>t3.large]
        end
        
        subgraph "Application Pods"
            Pod1[genai-demo-app-1]
            Pod2[genai-demo-app-2]
            Pod3[genai-demo-app-3]
        end
        
        subgraph "System Pods"
            DNS[CoreDNS]
            Proxy[kube-proxy]
            CNI[AWS VPC CNI]
        end
        
        subgraph "Add-ons"
            KEDA[KEDA Operator]
            HPA[HPA Controller]
            CA[Cluster Autoscaler]
            CSI[EBS CSI Driver]
        end
    end
    
    subgraph "AWS Services"
        ECR[Elastic Container Registry]
        ELB[Elastic Load Balancer]
        EBS[Elastic Block Store]
    end
    
    API --> Node1
    API --> Node2
    API --> Node3
    Node1 --> Pod1
    Node2 --> Pod2
    Node3 --> Pod3
    Pod1 --> ECR
    Pod2 --> ECR
    Pod3 --> ECR
    ELB --> Pod1
    ELB --> Pod2
    ELB --> Pod3
    Node1 --> EBS
    Node2 --> EBS
    Node3 --> EBS
    
    style API fill:#e3f2fd
    style Node1 fill:#e8f5e8
    style Node2 fill:#e8f5e8
    style Node3 fill:#e8f5e8
    style Pod1 fill:#fff3e0
    style Pod2 fill:#fff3e0
    style Pod3 fill:#fff3e0
```

### 自動擴展架構

```mermaid
graph LR
    subgraph "指標來源"
        CW[CloudWatch Metrics]
        Prom[Prometheus Metrics]
        Custom[Custom Metrics]
    end
    
    subgraph "擴展控制器"
        HPA[Horizontal Pod Autoscaler]
        KEDA[KEDA ScaledObject]
        CA[Cluster Autoscaler]
    end
    
    subgraph "擴展目標"
        Pods[Application Pods]
        Nodes[Worker Nodes]
        ASG[Auto Scaling Group]
    end
    
    CW --> HPA
    CW --> KEDA
    Prom --> KEDA
    Custom --> KEDA
    HPA --> Pods
    KEDA --> Pods
    CA --> Nodes
    CA --> ASG
    Pods -.-> Nodes
    
    style CW fill:#e1f5fe
    style HPA fill:#e8f5e8
    style KEDA fill:#e8f5e8
    style CA fill:#e8f5e8
    style Pods fill:#fff3e0
```

### 計算資源配置

```yaml
EKS Cluster:
  版本: Kubernetes 1.28
  端點: Private
  日誌: API, Audit, Authenticator, ControllerManager, Scheduler

Managed Node Groups:
  最小節點: 2
  最大節點: 10
  期望節點: 2-3 (環境相依)
  實例類型: 
    - t3.medium (開發)
    - t3.large (生產)
  AMI: Amazon Linux 2
  磁碟: 20GB GP3

Pod 規格:
  CPU 請求: 100m
  CPU 限制: 500m
  記憶體請求: 128Mi
  記憶體限制: 512Mi

自動擴展:
  HPA: CPU 70%, Memory 80%
  KEDA: 自定義指標 (執行緒池、佇列長度)
  Cluster Autoscaler: 節點使用率 70%
```

## 資料儲存

### 資料庫架構 (Aurora Global)

```mermaid
graph TB
    subgraph "ap-east-2 (主要區域)"
        subgraph "Aurora Cluster Primary"
            Writer1[Writer Instance<br/>db.r6g.large]
            Reader1[Reader Instance<br/>db.r6g.large]
        end
        
        subgraph "備份"
            Backup1[自動備份<br/>30天保留]
            Snapshot1[手動快照]
        end
    end
    
    subgraph "ap-northeast-1 (次要區域)"
        subgraph "Aurora Cluster Secondary"
            Reader2[Reader Instance<br/>db.r6g.large]
            Reader3[Reader Instance<br/>db.r6g.medium]
        end
        
        subgraph "備份"
            Backup2[跨區域備份]
        end
    end
    
    subgraph "應用程式"
        App1[EKS Pods - 台北]
        App2[EKS Pods - 東京]
    end
    
    App1 --> Writer1
    App1 --> Reader1
    App2 --> Reader2
    App2 --> Reader3
    Writer1 -.-> Reader2
    Writer1 --> Backup1
    Backup1 -.-> Backup2
    
    style Writer1 fill:#c8e6c9
    style Reader1 fill:#e1f5fe
    style Reader2 fill:#e1f5fe
    style Reader3 fill:#e1f5fe
    style Backup1 fill:#fff3e0
    style Backup2 fill:#ffcdd2
```

### 快取架構 (ElastiCache Redis)

```mermaid
graph TB
    subgraph "Redis Cluster"
        subgraph "Primary Node Group"
            Primary[Primary Node<br/>cache.r6g.large]
        end
        
        subgraph "Replica Node Groups"
            Replica1[Replica 1<br/>cache.r6g.large]
            Replica2[Replica 2<br/>cache.r6g.large]
        end
        
        subgraph "Configuration"
            Config[Parameter Group<br/>Redis 7.0]
            Subnet[Subnet Group<br/>Private Subnets]
        end
    end
    
    subgraph "應用程式使用"
        DistLock[分散式鎖]
        Cache[應用程式快取]
        Session[會話儲存]
    end
    
    Primary --> Replica1
    Primary --> Replica2
    DistLock --> Primary
    Cache --> Primary
    Cache --> Replica1
    Cache --> Replica2
    Session --> Primary
    
    style Primary fill:#c8e6c9
    style Replica1 fill:#e1f5fe
    style Replica2 fill:#e1f5fe
    style DistLock fill:#fff3e0
    style Cache fill:#fff3e0
    style Session fill:#fff3e0
```

### 資料儲存配置

```yaml
Aurora PostgreSQL:
  引擎版本: 15.4
  實例類型:
    Production: db.r6g.large
    Staging: db.r6g.medium
    Development: db.t3.medium
  儲存:
    類型: Aurora Storage
    加密: AES-256 (KMS)
    自動擴展: 啟用
  備份:
    自動備份: 30天 (Production), 7天 (Development)
    快照: 每週手動快照
    跨區域複製: 啟用 (Production)
  效能:
    Performance Insights: 啟用
    Enhanced Monitoring: 啟用

ElastiCache Redis:
  版本: 7.0
  節點類型:
    Production: cache.r6g.large
    Staging: cache.r6g.medium
    Development: cache.t3.micro
  配置:
    複製群組: 3節點
    Multi-AZ: 啟用
    自動故障轉移: 啟用
  安全:
    傳輸加密: TLS
    靜態加密: AES-256
    認證: AUTH token
```

## 訊息服務

### MSK Kafka 架構

```mermaid
graph TB
    subgraph "MSK Cluster"
        subgraph "Broker Nodes"
            Broker1[Kafka Broker 1<br/>kafka.m5.large<br/>AZ-1]
            Broker2[Kafka Broker 2<br/>kafka.m5.large<br/>AZ-2]
            Broker3[Kafka Broker 3<br/>kafka.m5.large<br/>AZ-3]
        end
        
        subgraph "Zookeeper"
            ZK1[Zookeeper 1]
            ZK2[Zookeeper 2]
            ZK3[Zookeeper 3]
        end
        
        subgraph "Topics"
            Topic1[genai-demo-events-prod<br/>Partitions: 6<br/>Replication: 3]
            Topic2[genai-demo-deadletter<br/>Partitions: 3<br/>Replication: 3]
        end
    end
    
    subgraph "Producers"
        App1[Application Pods]
        Lambda1[Lambda Functions]
    end
    
    subgraph "Consumers"
        App2[Event Processors]
        Analytics[Analytics Pipeline]
    end
    
    Broker1 --> ZK1
    Broker2 --> ZK2
    Broker3 --> ZK3
    App1 --> Topic1
    Lambda1 --> Topic1
    Topic1 --> App2
    Topic1 --> Analytics
    Topic2 --> App2
    
    style Broker1 fill:#e8f5e8
    style Broker2 fill:#e8f5e8
    style Broker3 fill:#e8f5e8
    style Topic1 fill:#fff3e0
    style Topic2 fill:#ffcdd2
```

### 事件驅動架構

```mermaid
sequenceDiagram
    participant User as 用戶
    participant API as API Gateway
    participant App as Application
    participant Kafka as MSK Kafka
    participant Processor as Event Processor
    participant DB as Database
    participant Cache as Redis Cache
    
    User->>API: HTTP Request
    API->>App: Forward Request
    App->>DB: Write Data
    App->>Kafka: Publish Domain Event
    App->>Cache: Update Cache
    App-->>API: Response
    API-->>User: HTTP Response
    Kafka->>Processor: Consume Event
    Processor->>DB: Process Event
    Processor->>Cache: Invalidate Cache
```

## 監控和可觀測性

### 監控架構

```mermaid
graph TB
    subgraph "資料來源"
        App[Application Metrics]
        EKS[EKS Metrics]
        RDS[RDS Metrics]
        Redis[Redis Metrics]
        MSK[MSK Metrics]
        ALB[ALB Metrics]
    end
    
    subgraph "收集層"
        CWAgent[CloudWatch Agent]
        ContainerInsights[Container Insights]
        XRay[X-Ray Daemon]
        Prometheus[Prometheus]
    end
    
    subgraph "儲存層"
        CloudWatch[CloudWatch Metrics]
        XRayService[X-Ray Service]
        LogGroups[CloudWatch Logs]
    end
    
    subgraph "視覺化層"
        CWDashboard[CloudWatch Dashboard]
        Grafana[Managed Grafana]
        XRayConsole[X-Ray Console]
    end
    
    subgraph "告警層"
        Alarms[CloudWatch Alarms]
        SNS[SNS Topics]
        Email[Email Notifications]
        Slack[Slack Integration]
    end
    
    App --> CWAgent
    EKS --> ContainerInsights
    App --> XRay
    EKS --> Prometheus
    CWAgent --> CloudWatch
    ContainerInsights --> CloudWatch
    XRay --> XRayService
    Prometheus --> CloudWatch
    CloudWatch --> CWDashboard
    CloudWatch --> Grafana
    XRayService --> XRayConsole
    CloudWatch --> Alarms
    Alarms --> SNS
    SNS --> Email
    SNS --> Slack
    
    style CloudWatch fill:#e1f5fe
    style Grafana fill:#e8f5e8
    style Alarms fill:#ffcdd2
```

### 可觀測性配置

```yaml
CloudWatch Metrics:
  自定義指標:
    - 執行緒池使用率
    - JVM 記憶體使用
    - HTTP 請求指標
    - 業務指標 (訂單、用戶等)
  系統指標:
    - EKS: CPU, Memory, Network, Disk
    - RDS: CPU, Connections, IOPS, Latency
    - Redis: CPU, Memory, Commands, Connections
    - MSK: Throughput, Lag, Disk Usage

X-Ray Tracing:
  採樣率:
    Production: 5%
    Staging: 10%
    Development: 100%
  追蹤服務:
    - HTTP 請求
    - 資料庫查詢
    - Redis 操作
    - Kafka 訊息
    - 外部 API 調用

CloudWatch Logs:
  日誌群組:
    - /aws/genai-demo/application
    - /aws/eks/cluster-logs
    - /aws/rds/instance/postgresql
    - /aws/elasticache/redis
  保留期:
    Production: 30天
    Staging: 14天
    Development: 7天
```

## 成本優化

### 成本結構分析

```mermaid
pie title 月度成本分佈 (Production)
    "EKS + EC2" : 35
    "RDS Aurora" : 25
    "ElastiCache" : 15
    "MSK" : 10
    "Data Transfer" : 8
    "CloudWatch" : 4
    "其他服務" : 3
```

### 成本優化策略

```yaml
計算資源優化:
  EKS:
    - Spot Instances: 30% 節點使用 Spot
    - 自動擴展: 基於實際負載
    - 資源請求優化: 避免過度配置
  Lambda:
    - ARM Graviton2: 20% 成本節省
    - 記憶體優化: 基於實際使用

儲存優化:
  RDS:
    - Reserved Instances: 1年期 40% 節省
    - 儲存自動擴展: 避免過度配置
    - 讀取副本: 僅在需要時創建
  S3:
    - Intelligent Tiering: 自動成本優化
    - 生命週期政策: 自動歸檔

網路優化:
  - CloudFront: 減少 Data Transfer 成本
  - VPC Endpoints: 避免 NAT Gateway 費用
  - 區域內通訊: 最小化跨區域流量
```

## 架構圖表

### 完整系統架構圖

```mermaid
graph TB
    subgraph "用戶層"
        Web[Web Browser]
        Mobile[Mobile App]
        API_Client[API Client]
    end
    
    subgraph "CDN & DNS"
        CF[CloudFront]
        R53[Route 53]
    end
    
    subgraph "ap-east-2 (主要區域)"
        subgraph "網路層"
            VPC1[VPC]
            ALB1[ALB]
            NAT1[NAT Gateway]
        end
        
        subgraph "應用層"
            EKS1[EKS Cluster]
            subgraph "Pods"
                App1[genai-demo-app]
                Sidecar1[X-Ray Sidecar]
            end
        end
        
        subgraph "資料層"
            Aurora1[Aurora Writer]
            Redis1[Redis Primary]
            MSK1[MSK Cluster]
        end
        
        subgraph "監控層"
            CW1[CloudWatch]
            XRay1[X-Ray]
        end
    end
    
    subgraph "ap-northeast-1 (災難恢復)"
        VPC2[VPC]
        ALB2[ALB]
        EKS2[EKS Cluster]
        Aurora2[Aurora Reader]
        Redis2[Redis Replica]
    end
    
    subgraph "全域服務"
        IAM[IAM]
        KMS[KMS]
        Secrets[Secrets Manager]
        ECR[ECR]
    end
    
    Web --> CF
    Mobile --> CF
    API_Client --> R53
    CF --> R53
    R53 --> ALB1
    R53 -.-> ALB2
    ALB1 --> EKS1
    EKS1 --> App1
    App1 --> Sidecar1
    App1 --> Aurora1
    App1 --> Redis1
    App1 --> MSK1
    App1 --> CW1
    Sidecar1 --> XRay1
    Aurora1 -.-> Aurora2
    Redis1 -.-> Redis2
    App1 --> IAM
    App1 --> KMS
    App1 --> Secrets
    EKS1 --> ECR
    
    style Web fill:#e3f2fd
    style EKS1 fill:#e8f5e8
    style Aurora1 fill:#c8e6c9
    style Redis1 fill:#fff3e0
    style CW1 fill:#e1f5fe
```

---

**文件狀態**: ✅ 完成  
**下一步**: 查看 [Security Viewpoint](../security/iam-permissions-architecture.md) 了解 IAM 權限架構  
**相關文件**: 
- [Security Viewpoint - IAM 權限架構](../security/iam-permissions-architecture.md)
- [Deployment Viewpoint - 部署架構](../deployment/deployment-architecture.md)
- [Operational Viewpoint - DNS 解析與災難恢復](../operational/dns-disaster-recovery.md)
