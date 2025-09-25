# CDK 資源配置完整清單

**生成時間**: 2025年9月24日 下午4:37 (台北時間)  
**CDK 版本**: 2.216.0  
**環境**: Multi-Environment (development/staging/production)  
**主要區域**: ap-east-2 (台北)

## 🏗️ 架構概覽

### 部署架構
```
GenAI Demo Infrastructure (Multi-Region Active-Active)
├── 主要區域: ap-east-2 (台北)
├── 次要區域: ap-northeast-1 (東京)
└── 災難恢復: 跨區域複製和故障轉移
```

### Stack 依賴關係
```
NetworkStack (基礎)
├── SecurityStack
├── AlertingStack
├── ElastiCacheStack
├── EKSStack
├── RdsStack
├── MSKStack
├── CoreStack
└── ObservabilityStack
    └── AnalyticsStack (可選)
```

## 📋 完整資源清單

### 1. NetworkStack - 網路基礎設施

#### VPC 配置
```yaml
資源類型: AWS::EC2::VPC
CIDR: 10.0.0.0/16
可用區域: 2個 AZ
NAT Gateway: 1個 (成本優化)
```

#### 子網路配置
```yaml
Public Subnets:
  - CIDR: 10.0.0.0/24 (AZ-1)
  - CIDR: 10.0.1.0/24 (AZ-2)
  - 用途: ALB, NAT Gateway

Private Subnets:
  - CIDR: 10.0.2.0/24 (AZ-1)  
  - CIDR: 10.0.3.0/24 (AZ-2)
  - 用途: 應用程式, EKS, MSK

Database Subnets:
  - CIDR: 10.0.4.0/28 (AZ-1)
  - CIDR: 10.0.5.0/28 (AZ-2)
  - 用途: RDS, ElastiCache (隔離)
```

#### 安全群組
```yaml
ALB Security Group:
  - 入站: HTTP (80), HTTPS (443) from 0.0.0.0/0
  - 出站: 限制

App Security Group:
  - 入站: 8080 from ALB SG
  - 出站: 全部允許

Database Security Group:
  - 入站: 5432 from App SG
  - 出站: 限制
```

### 2. SecurityStack - 安全基礎設施

#### KMS 加密
```yaml
資源: AWS::KMS::Key
用途: 應用程式資料加密
金鑰輪換: 啟用
移除政策: 環境相依 (production: RETAIN)
```

#### IAM 角色
```yaml
Application Role:
  - 服務: EC2
  - 政策: CloudWatchAgentServerPolicy
  - KMS: 加密/解密權限
```

### 3. EKSStack - Kubernetes 容器平台

#### EKS Cluster
```yaml
資源: Custom::AWSCDK-EKS-Cluster
Kubernetes 版本: 1.28
網路: Private subnets
端點存取: Private
KubectlLayer: v1.28 (已修復)
```

#### Managed Node Groups
```yaml
節點類型: t3.medium, t3.large
最小節點: 2
最大節點: 10
期望節點: 2
AMI: Amazon Linux 2
容量類型: On-Demand
磁碟大小: 20GB
```

#### 自動擴展系統
```yaml
KEDA (Event-Driven Autoscaling):
  - Helm Chart: keda 2.12.0
  - Namespace: keda-system
  - 觸發器: Prometheus metrics

HPA (Horizontal Pod Autoscaler):
  - API版本: autoscaling/v2
  - CPU閾值: 70%
  - Memory閾值: 80%
  - 最小副本: 2, 最大副本: 10

Cluster Autoscaler:
  - 版本: v1.28.2
  - 自動發現: ASG tags
  - 擴展策略: least-waste
```

#### Service Accounts & IAM
```yaml
Cluster Autoscaler Service Account:
  - Namespace: kube-system
  - IAM權限: ASG管理, EC2描述
  - IRSA: 啟用
```

### 4. RdsStack - 資料庫服務

#### Aurora Global Database (Production)
```yaml
引擎: Aurora PostgreSQL 15.4
部署模式: Global Cluster
主要區域: ap-east-2 (Writer + Reader)
次要區域: ap-northeast-1 (Reader)
```

#### RDS Instance (Development/Staging)
```yaml
引擎: PostgreSQL 15.4
實例類型: 環境相依 (t3.micro - db.r6g.large)
儲存: GP3, 自動擴展
Multi-AZ: 環境相依
```

#### 資料庫配置
```yaml
Parameter Group:
  - max_connections: 100-200 (環境相依)
  - shared_buffers: 128MB-256MB
  - effective_cache_size: 512MB-1GB
  - 效能優化參數

Security:
  - 加密: 靜態和傳輸中
  - KMS金鑰: 自定義
  - 網路: 隔離子網路

Backup:
  - 保留期: 7-30天 (環境相依)
  - 備份視窗: 03:00-04:00 UTC
  - 維護視窗: 週日 04:00-05:00 UTC
```

#### 監控和告警
```yaml
CloudWatch Alarms:
  - CPU使用率: >80-90%
  - 連線數: >80% max_connections
  - 儲存空間: <2GB
  - 讀寫延遲: >200ms
  - Aurora副本延遲: >30秒

Performance Insights:
  - 啟用: 是
  - 保留期: 7天-長期 (環境相依)
  - 加密: KMS
```

### 5. MSKStack - Apache Kafka 訊息佇列

#### MSK Cluster
```yaml
Kafka版本: 2.8.1
節點數量: 3 (Multi-AZ)
實例類型: kafka.t3.small
儲存: 100GB per broker
```

#### Kafka 配置
```yaml
Topics:
  - auto.create.topics.enable: false
  - default.replication.factor: 3
  - min.insync.replicas: 2
  - num.partitions: 6

Log Retention:
  - 時間: 168小時 (7天) / 720小時 (30天 production)
  - 大小: 1GB per partition
  - 段大小: 100MB
```

#### 安全配置
```yaml
加密:
  - 傳輸中: TLS
  - 靜態: KMS
  
認證:
  - IAM: 啟用
  - SASL/SCRAM: 可選
```

### 6. ElastiCacheStack - Redis 分散式鎖

#### Redis Cluster
```yaml
引擎: Redis 7.0
節點類型: cache.t3.micro
節點數量: 3 (Multi-AZ)
複製群組: 啟用
自動故障轉移: 啟用
```

#### Redis 配置
```yaml
Memory Policy: allkeys-lru
Timeout: 300秒
TCP KeepAlive: 60秒
Persistence: RDB snapshots
Replication Backlog: 1MB
```

#### 安全配置
```yaml
加密:
  - 靜態加密: 啟用
  - 傳輸加密: 啟用
  - 認證令牌: 配置 (生產環境需更新)

網路:
  - VPC: 私有子網路
  - 安全群組: 限制 6379 port
```

### 7. ObservabilityStack - 監控和可觀測性

#### CloudWatch 監控
```yaml
Log Groups:
  - 應用程式日誌: /aws/genai-demo/application
  - EKS日誌: /aws/eks/cluster-logs
  - RDS日誌: /aws/rds/instance/postgresql
  - Redis日誌: /aws/elasticache/redis

Dashboard:
  - 名稱: GenAI-Demo-{environment}
  - 小工具: 系統概覽, 併發監控, 死鎖監控
```

#### X-Ray 分散式追蹤
```yaml
服務: AWS X-Ray
採樣規則: 自定義配置
追蹤保留: 30天
IAM角色: X-Ray寫入權限
```

#### Container Insights
```yaml
EKS監控: 啟用
指標收集: 節點和Pod級別
日誌收集: 應用程式和系統日誌
```

#### Amazon Managed Grafana
```yaml
工作區: 每環境一個
資料來源: CloudWatch, X-Ray
儀表板: 統一監控視圖
```

### 8. AlertingStack - 告警系統

#### SNS Topics
```yaml
Critical Alerts:
  - 嚴重系統故障
  - 資料庫連線失敗
  - 高錯誤率

Warning Alerts:
  - 效能降級
  - 資源使用率高
  - 備份失敗

Info Alerts:
  - 部署通知
  - 維護視窗
  - 配置變更
```

### 9. CoreStack - 核心基礎設施

#### 共享資源
```yaml
S3 Buckets:
  - 應用程式資產
  - 日誌歸檔
  - 備份儲存

Lambda Functions:
  - 自動化任務
  - 事件處理
  - 監控腳本

EventBridge Rules:
  - 排程任務
  - 事件路由
  - 自動化觸發
```

### 10. AnalyticsStack - 分析平台 (可選)

#### 資料分析
```yaml
Kinesis Data Streams:
  - 即時資料流
  - 事件處理
  - 指標收集

Kinesis Analytics:
  - 即時分析
  - 異常檢測
  - 趨勢分析

S3 Data Lake:
  - 歷史資料儲存
  - 批次分析
  - 資料歸檔
```

## 🔧 環境特定配置

### Development 環境
```yaml
RDS: t3.micro, 單AZ, 7天備份
EKS: 2節點, t3.medium
Redis: cache.t3.micro, 3節點
MSK: kafka.t3.small, 3節點
監控: 基本告警
```

### Staging 環境
```yaml
RDS: t3.small, Multi-AZ, 14天備份
EKS: 3節點, t3.medium/large
Redis: cache.t3.small, 3節點
MSK: kafka.m5.large, 3節點
監控: 完整告警
```

### Production 環境
```yaml
RDS: Aurora Global, r6g.large, 30天備份
EKS: 5-10節點, t3.large/xlarge
Redis: cache.r6g.large, 3節點
MSK: kafka.m5.xlarge, 6節點
監控: 全面監控 + Grafana
```

## 📊 資源統計

### 總計資源數量
```yaml
CloudFormation Stacks: 9個主要 Stack
AWS Resources: ~150+ 資源
  - Compute: EKS (10-20 resources)
  - Database: RDS/Aurora (15-25 resources)
  - Networking: VPC (20-30 resources)
  - Security: IAM/KMS (15-20 resources)
  - Monitoring: CloudWatch (30-40 resources)
  - Storage: S3/EBS (10-15 resources)
  - Messaging: MSK/SNS (10-15 resources)
```

### 成本估算 (月費用)
```yaml
Development: ~$200-300 USD/月
Staging: ~$500-800 USD/月
Production: ~$1500-2500 USD/月
  - 主要成本: RDS Aurora Global, EKS節點, MSK
```

## 🚀 部署配置

### CDK 應用程式配置
```typescript
環境變數:
  - environment: development/staging/production
  - region: ap-east-2 (主要)
  - enableAnalytics: true/false
  - enableCdkNag: true/false
  - alertEmail: 告警郵件地址

Context 參數:
  - genai-demo:environments: 環境特定配置
  - genai-demo:regions: 區域配置
  - genai-demo:multi-region: 多區域設定
```

### 部署命令
```bash
# 開發環境
npm run deploy:dev

# 預備環境  
npm run deploy:staging

# 生產環境
npm run deploy:prod

# 特定 Stack
npx cdk deploy development-EKSStack
```

## 🔒 安全配置

### 加密
```yaml
靜態加密:
  - RDS: KMS自定義金鑰
  - S3: AES-256
  - EBS: KMS預設金鑰
  - ElastiCache: 啟用

傳輸加密:
  - ALB: TLS 1.2+
  - RDS: SSL/TLS
  - Redis: TLS
  - MSK: TLS
```

### 網路安全
```yaml
VPC:
  - 私有子網路隔離
  - NAT Gateway 出站控制
  - 安全群組最小權限

EKS:
  - 私有端點
  - RBAC 啟用
  - Pod安全政策
```

### 存取控制
```yaml
IAM:
  - 最小權限原則
  - 服務特定角色
  - 跨服務存取控制

Secrets:
  - AWS Secrets Manager
  - 自動輪換
  - KMS 加密
```

## 📈 監控和告警

### 關鍵指標
```yaml
應用程式:
  - 回應時間: <2秒
  - 錯誤率: <1%
  - 吞吐量: 1000 req/s

資料庫:
  - CPU: <80%
  - 連線數: <80% max
  - 延遲: <200ms

基礎設施:
  - EKS節點: CPU/Memory <70%
  - Redis: 記憶體使用 <80%
  - MSK: 磁碟使用 <80%
```

### 告警策略
```yaml
Critical (立即):
  - 服務完全中斷
  - 資料庫無法連線
  - 安全事件

Warning (15分鐘內):
  - 效能降級
  - 資源使用率高
  - 備份失敗

Info (1小時內):
  - 部署完成
  - 配置變更
  - 維護通知
```

## 🔄 災難恢復

### 備份策略
```yaml
RDS:
  - 自動備份: 每日
  - 快照: 每週
  - 跨區域複製: 生產環境

應用程式:
  - 容器映像: ECR複製
  - 配置: Git版本控制
  - 資料: S3跨區域複製
```

### 故障轉移
```yaml
RDS Aurora Global:
  - RTO: <1分鐘
  - RPO: <1秒
  - 自動故障轉移

EKS:
  - Multi-AZ部署
  - 自動節點替換
  - Pod自動重啟
```

---

**✅ CDK 資源配置清單完成！**  
**總計**: 9個主要 Stack, 150+ AWS 資源  
**架構**: Multi-Region Active-Active  
**狀態**: 生產就緒，已通過測試驗證