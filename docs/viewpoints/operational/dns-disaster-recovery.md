# Operational Viewpoint - DNS 解析與災難恢復

**文件版本**: 1.0  
**最後更新**: 2025年9月24日 下午5:15 (台北時間)  
**作者**: Operations Team  
**狀態**: Active

## 📋 目錄

- [概覽](#概覽)
- [DNS 解析架構](#dns-解析架構)
- [正常流量路由](#正常流量路由)
- [災難恢復機制](#災難恢復機制)
- [故障轉移流程](#故障轉移流程)
- [監控和告警](#監控和告警)
- [運維流程](#運維流程)
- [效能優化](#效能優化)

## 概覽

GenAI Demo 採用 Multi-Region Active-Active 架構，透過 Amazon Route 53 實現智能 DNS 解析和自動故障轉移。系統設計確保在主要區域 (ap-east-2) 發生故障時，能夠自動切換到次要區域 (ap-northeast-1)，提供持續的服務可用性。

### 運維目標

- **高可用性**: 99.9% 服務可用性
- **快速恢復**: RTO < 5分鐘，RPO < 1分鐘
- **自動故障轉移**: 無需人工干預
- **透明切換**: 用戶無感知的區域切換
- **全球效能**: 最佳化的全球存取體驗

## DNS 解析架構

### 整體 DNS 架構

```mermaid
graph TB
    subgraph "Global DNS Infrastructure"
        subgraph "用戶端"
            User[用戶瀏覽器]
            Mobile[行動應用程式]
            API[API 客戶端]
        end
        
        subgraph "DNS 解析鏈"
            LocalDNS[本地 DNS 解析器]
            ISP_DNS[ISP DNS 伺服器]
            Root[根 DNS 伺服器]
            TLD[.io TLD 伺服器]
        end
        
        subgraph "Route 53"
            HostedZone[Hosted Zone<br/>kimkao.io]
            HealthChecks[Health Checks]
            
            subgraph "DNS Records"
                ARecord[A Record<br/>genai-demo.kimkao.io]
                CNAMERecord[CNAME Records]
                AAAARecord[AAAA Record (IPv6)]
            end
            
            subgraph "Routing Policies"
                Weighted[Weighted Routing]
                Latency[Latency-based Routing]
                Failover[Failover Routing]
                Geolocation[Geolocation Routing]
            end
        end
    end
    
    subgraph "CloudFront Distribution"
        CF[CloudFront Edge Locations]
        CFOrigin[Origin Configuration]
    end
    
    subgraph "ap-east-2 (台北) - Primary"
        ALB1[Application Load Balancer]
        EKS1[EKS Cluster]
        Health1[Health Check Endpoint]
    end
    
    subgraph "ap-northeast-1 (東京) - Secondary"
        ALB2[Application Load Balancer]
        EKS2[EKS Cluster]
        Health2[Health Check Endpoint]
    end
    
    User --> LocalDNS
    Mobile --> LocalDNS
    API --> LocalDNS
    LocalDNS --> ISP_DNS
    ISP_DNS --> Root
    Root --> TLD
    TLD --> HostedZone
    HostedZone --> ARecord
    HostedZone --> CNAMERecord
    HostedZone --> AAAARecord
    ARecord --> Weighted
    ARecord --> Latency
    ARecord --> Failover
    ARecord --> Geolocation
    HealthChecks --> Health1
    HealthChecks --> Health2
    Failover --> CF
    CF --> CFOrigin
    CFOrigin --> ALB1
    CFOrigin -.-> ALB2
    ALB1 --> EKS1
    ALB2 --> EKS2
    
    style HostedZone fill:#e3f2fd
    style HealthChecks fill:#ffcdd2
    style ALB1 fill:#c8e6c9
    style ALB2 fill:#fff3e0
```

### DNS 記錄配置

```yaml
Route 53 Hosted Zone: kimkao.io
DNS Records:
  主要記錄:
    - genai-demo.kimkao.io (A Record)
    - api.genai-demo.kimkao.io (CNAME)
    - www.genai-demo.kimkao.io (CNAME)
  
  故障轉移記錄:
    Primary:
      - 記錄名稱: api.genai-demo.kimkao.io
      - 類型: A (Alias)
      - 目標: ALB ap-east-2
      - 路由政策: Failover (Primary)
      - 健康檢查: 啟用
      - TTL: 60秒
    
    Secondary:
      - 記錄名稱: api.genai-demo.kimkao.io
      - 類型: A (Alias)
      - 目標: ALB ap-northeast-1
      - 路由政策: Failover (Secondary)
      - 健康檢查: 啟用
      - TTL: 60秒
  
  延遲路由記錄:
    Taipei:
      - 記錄名稱: api-latency.genai-demo.kimkao.io
      - 區域: ap-east-2
      - 目標: ALB ap-east-2
      - 健康檢查: 啟用
    
    Tokyo:
      - 記錄名稱: api-latency.genai-demo.kimkao.io
      - 區域: ap-northeast-1
      - 目標: ALB ap-northeast-1
      - 健康檢查: 啟用
```

## 正常流量路由

### 用戶訪問 https://genai-demo.kimkao.io 的完整流程

```mermaid
sequenceDiagram
    participant User as 用戶瀏覽器
    participant LocalDNS as 本地 DNS
    participant Route53 as Route 53
    participant HealthCheck as Health Check
    participant CloudFront as CloudFront
    participant ALB as ALB (台北)
    participant EKS as EKS Cluster
    participant App as Application Pod
    participant RDS as Aurora DB
    participant Redis as ElastiCache
    
    Note over User,Redis: 正常情況下的完整請求流程
    
    User->>LocalDNS: DNS 查詢 genai-demo.kimkao.io
    LocalDNS->>Route53: 遞歸查詢
    Route53->>HealthCheck: 檢查主要區域健康狀態
    HealthCheck-->>Route53: 主要區域健康 ✅
    Route53-->>LocalDNS: 返回 CloudFront IP
    LocalDNS-->>User: 返回 IP 地址
    
    User->>CloudFront: HTTPS 請求 (TLS 1.3)
    CloudFront->>ALB: 轉發到台北 ALB
    ALB->>EKS: 負載均衡到 Pod
    EKS->>App: 路由到應用程式
    
    App->>RDS: 資料庫查詢
    RDS-->>App: 返回資料
    App->>Redis: 快取操作
    Redis-->>App: 返回快取資料
    
    App-->>EKS: 處理完成
    EKS-->>ALB: 返回回應
    ALB-->>CloudFront: 返回回應
    CloudFront-->>User: 返回最終回應
    
    Note over User,Redis: 整個流程通常在 200-500ms 內完成
```

### DNS 解析詳細步驟

```mermaid
graph TD
    subgraph "Step 1: 初始 DNS 查詢"
        A1[用戶輸入 genai-demo.kimkao.io]
        A2[瀏覽器檢查本地快取]
        A3[查詢作業系統 DNS 快取]
        A4[查詢本地 DNS 解析器]
    end
    
    subgraph "Step 2: 遞歸 DNS 解析"
        B1[本地 DNS 查詢根伺服器]
        B2[根伺服器返回 .io TLD 伺服器]
        B3[查詢 .io TLD 伺服器]
        B4[TLD 返回 kimkao.io 權威伺服器]
    end
    
    subgraph "Step 3: Route 53 權威解析"
        C1[查詢 Route 53 權威伺服器]
        C2[Route 53 執行健康檢查]
        C3[選擇最佳路由政策]
        C4[返回目標 IP 地址]
    end
    
    subgraph "Step 4: 連線建立"
        D1[瀏覽器連線到 CloudFront]
        D2[CloudFront 選擇最近邊緣節點]
        D3[建立 TLS 連線]
        D4[轉發請求到源站]
    end
    
    A1 --> A2
    A2 --> A3
    A3 --> A4
    A4 --> B1
    B1 --> B2
    B2 --> B3
    B3 --> B4
    B4 --> C1
    C1 --> C2
    C2 --> C3
    C3 --> C4
    C4 --> D1
    D1 --> D2
    D2 --> D3
    D3 --> D4
    
    style C2 fill:#ffcdd2
    style C3 fill:#e8f5e8
    style D2 fill:#e3f2fd
```

### 路由政策決策流程

```mermaid
flowchart TD
    Start[DNS 查詢開始] --> HealthCheck{健康檢查}
    
    HealthCheck -->|主要區域健康| PrimaryHealthy[主要區域可用]
    HealthCheck -->|主要區域故障| PrimaryFailed[主要區域故障]
    
    PrimaryHealthy --> LatencyCheck{延遲路由檢查}
    LatencyCheck -->|用戶在亞洲| AsiaRoute[路由到台北 ap-east-2]
    LatencyCheck -->|用戶在其他地區| GlobalRoute[基於延遲路由]
    
    PrimaryFailed --> SecondaryCheck{次要區域檢查}
    SecondaryCheck -->|次要區域健康| SecondaryRoute[故障轉移到東京 ap-northeast-1]
    SecondaryCheck -->|次要區域也故障| ErrorPage[返回錯誤頁面]
    
    AsiaRoute --> CloudFrontTaipei[CloudFront → 台北 ALB]
    GlobalRoute --> CloudFrontOptimal[CloudFront → 最佳區域]
    SecondaryRoute --> CloudFrontTokyo[CloudFront → 東京 ALB]
    ErrorPage --> MaintenancePage[維護頁面]
    
    style HealthCheck fill:#ffcdd2
    style LatencyCheck fill:#e8f5e8
    style SecondaryCheck fill:#fff3e0
    style AsiaRoute fill:#c8e6c9
    style SecondaryRoute fill:#e3f2fd
```

## 災難恢復機制

### 故障檢測與轉移架構

```mermaid
graph TB
    subgraph "健康檢查系統"
        subgraph "Route 53 Health Checks"
            HC1[主要區域健康檢查<br/>ap-east-2]
            HC2[次要區域健康檢查<br/>ap-northeast-1]
        end
        
        subgraph "檢查配置"
            Config1[檢查間隔: 30秒<br/>失敗閾值: 3次<br/>檢查路徑: /actuator/health]
            Config2[檢查間隔: 30秒<br/>失敗閾值: 3次<br/>檢查路徑: /actuator/health]
        end
    end
    
    subgraph "監控與告警"
        CW[CloudWatch Alarms]
        SNS[SNS Topics]
        Lambda[Lambda Functions]
        Slack[Slack 通知]
        Email[Email 通知]
        PagerDuty[PagerDuty 告警]
    end
    
    subgraph "自動化回應"
        EventBridge[EventBridge Rules]
        AutoScale[Auto Scaling Actions]
        Runbooks[Systems Manager Runbooks]
        Recovery[Recovery Procedures]
    end
    
    HC1 --> Config1
    HC2 --> Config2
    HC1 --> CW
    HC2 --> CW
    CW --> SNS
    SNS --> Lambda
    SNS --> Slack
    SNS --> Email
    SNS --> PagerDuty
    CW --> EventBridge
    EventBridge --> AutoScale
    EventBridge --> Runbooks
    Runbooks --> Recovery
    
    style HC1 fill:#c8e6c9
    style HC2 fill:#fff3e0
    style CW fill:#ffcdd2
    style Recovery fill:#e3f2fd
```

### 災難恢復場景

#### 場景 1: 主要區域部分故障

```mermaid
sequenceDiagram
    participant User as 用戶
    participant Route53 as Route 53
    participant HC as Health Check
    participant Primary as 台北區域 (故障)
    participant Secondary as 東京區域
    participant Ops as 運維團隊
    
    Note over User,Ops: 主要區域 ALB 故障，但 EKS 正常
    
    User->>Route53: DNS 查詢
    Route53->>HC: 執行健康檢查
    HC->>Primary: 檢查 /actuator/health
    Primary--xHC: 連線失敗 ❌
    HC->>HC: 失敗計數 +1 (1/3)
    
    Note over HC: 等待 30 秒
    
    HC->>Primary: 重新檢查
    Primary--xHC: 連線失敗 ❌
    HC->>HC: 失敗計數 +1 (2/3)
    
    Note over HC: 等待 30 秒
    
    HC->>Primary: 第三次檢查
    Primary--xHC: 連線失敗 ❌
    HC->>HC: 失敗計數 +1 (3/3)
    HC->>Route53: 標記主要區域為不健康
    
    Route53->>Secondary: 切換到次要區域
    Route53-->>User: 返回東京區域 IP
    User->>Secondary: 請求轉發到東京
    Secondary-->>User: 正常回應 ✅
    
    HC->>Ops: 發送告警通知
    Ops->>Primary: 開始故障排除
```

#### 場景 2: 主要區域完全故障

```mermaid
sequenceDiagram
    participant User as 用戶
    participant Route53 as Route 53
    participant Primary as 台北區域 (完全故障)
    participant Secondary as 東京區域
    participant RDS as Aurora Global
    participant Ops as 運維團隊
    
    Note over User,Ops: 台北區域完全不可用 (網路/電力故障)
    
    User->>Route53: DNS 查詢
    Route53->>Primary: 健康檢查
    Primary--xRoute53: 區域完全不可達 ❌
    
    Route53->>Route53: 立即標記為不健康
    Route53->>Secondary: 自動故障轉移
    Route53-->>User: 返回東京區域 IP
    
    User->>Secondary: 請求轉發到東京
    Secondary->>RDS: 查詢資料 (讀取副本)
    RDS-->>Secondary: 返回資料
    Secondary-->>User: 正常回應 ✅
    
    Route53->>Ops: 發送緊急告警
    Note over Ops: RTO: < 5 分鐘達成 ✅
    
    Ops->>Ops: 評估故障範圍
    Ops->>Secondary: 如需要，提升為主要區域
```

## 故障轉移流程

### 自動故障轉移時序圖

```mermaid
gantt
    title 故障轉移時間線 (RTO < 5分鐘)
    dateFormat X
    axisFormat %M:%S
    
    section 檢測階段
    健康檢查失敗 (第1次)    :0, 30s
    健康檢查失敗 (第2次)    :30s, 60s
    健康檢查失敗 (第3次)    :60s, 90s
    
    section 切換階段
    DNS 記錄更新           :90s, 95s
    DNS 傳播              :95s, 155s
    
    section 恢復階段
    用戶流量切換完成        :155s, 180s
    告警通知發送           :90s, 120s
    運維團隊響應           :120s, 300s
```

### 故障轉移決策矩陣

```yaml
故障轉移觸發條件:
  自動觸發:
    - 健康檢查連續失敗 3 次 (90秒)
    - HTTP 5xx 錯誤率 > 50% (持續 2分鐘)
    - 回應時間 > 10秒 (持續 1分鐘)
    - 連線超時 > 30秒

  手動觸發:
    - 計劃性維護
    - 安全事件
    - 效能問題
    - 運維決策

故障轉移動作:
  DNS 層面:
    - 更新 Route 53 記錄
    - 調整 TTL 為 60秒
    - 啟用次要區域路由
    - 停用主要區域路由

  應用層面:
    - 切換資料庫連線到讀取副本
    - 更新快取配置
    - 調整監控閾值
    - 啟用降級模式

  通知層面:
    - 發送 Slack 通知
    - 觸發 PagerDuty 告警
    - 更新狀態頁面
    - 通知相關團隊
```

### 故障恢復流程

```mermaid
flowchart TD
    Start[故障檢測] --> Assess[評估故障範圍]
    
    Assess --> Minor{輕微故障?}
    Minor -->|是| QuickFix[快速修復]
    Minor -->|否| MajorFault[重大故障處理]
    
    QuickFix --> TestPrimary[測試主要區域]
    TestPrimary --> PrimaryOK{主要區域恢復?}
    PrimaryOK -->|是| Failback[故障回切]
    PrimaryOK -->|否| ExtendedDR[延長 DR 模式]
    
    MajorFault --> ActivateDR[啟動完整 DR]
    ActivateDR --> PromoteSecondary[提升次要區域]
    PromoteSecondary --> UpdateDNS[更新 DNS 配置]
    UpdateDNS --> NotifyUsers[通知用戶]
    
    Failback --> GradualShift[漸進式切換]
    GradualShift --> MonitorHealth[監控健康狀態]
    MonitorHealth --> Complete[恢復完成]
    
    ExtendedDR --> PlanRecovery[制定恢復計劃]
    PlanRecovery --> ExecuteRecovery[執行恢復]
    ExecuteRecovery --> TestPrimary
    
    NotifyUsers --> PlanRecovery
    
    style Start fill:#ffcdd2
    style ActivateDR fill:#fff3e0
    style Failback fill:#c8e6c9
    style Complete fill:#e8f5e8
```

## 監控和告警

### 監控儀表板

```mermaid
graph TB
    subgraph "Route 53 監控"
        subgraph "健康檢查指標"
            HC_Status[健康檢查狀態]
            HC_Latency[健康檢查延遲]
            HC_Success[成功率統計]
        end
        
        subgraph "DNS 查詢指標"
            DNS_Queries[DNS 查詢數量]
            DNS_Latency[DNS 解析延遲]
            DNS_Errors[DNS 錯誤率]
        end
    end
    
    subgraph "應用程式監控"
        subgraph "區域健康狀態"
            Primary_Health[台北區域健康度]
            Secondary_Health[東京區域健康度]
            Cross_Region[跨區域延遲]
        end
        
        subgraph "業務指標"
            Request_Rate[請求速率]
            Error_Rate[錯誤率]
            Response_Time[回應時間]
        end
    end
    
    subgraph "基礎設施監控"
        subgraph "網路指標"
            Network_Latency[網路延遲]
            Bandwidth[頻寬使用]
            Packet_Loss[封包遺失]
        end
        
        subgraph "資源使用"
            CPU_Usage[CPU 使用率]
            Memory_Usage[記憶體使用率]
            Disk_Usage[磁碟使用率]
        end
    end
    
    HC_Status --> Primary_Health
    HC_Status --> Secondary_Health
    DNS_Queries --> Request_Rate
    Primary_Health --> CPU_Usage
    Secondary_Health --> Memory_Usage
    
    style HC_Status fill:#e3f2fd
    style Primary_Health fill:#c8e6c9
    style Secondary_Health fill:#fff3e0
    style Request_Rate fill:#e8f5e8
```

### 告警配置

```yaml
CloudWatch Alarms:
  健康檢查告警:
    PrimaryHealthCheckFailure:
      指標: Route53 HealthCheckStatus
      閾值: < 1 (不健康)
      評估期間: 2 個數據點，共 2 分鐘
      動作: SNS → PagerDuty (P1)
    
    SecondaryHealthCheckFailure:
      指標: Route53 HealthCheckStatus
      閾值: < 1 (不健康)
      評估期間: 2 個數據點，共 2 分鐘
      動作: SNS → PagerDuty (P0 - 兩個區域都故障)

  應用程式告警:
    HighErrorRate:
      指標: ALB 5xx 錯誤率
      閾值: > 5%
      評估期間: 3 個數據點，共 3 分鐘
      動作: SNS → Slack + Email
    
    HighLatency:
      指標: ALB 回應時間
      閾值: > 2 秒 (95th percentile)
      評估期間: 2 個數據點，共 4 分鐘
      動作: SNS → Slack

  DNS 告警:
    DNSResolutionFailure:
      指標: Route53 查詢失敗率
      閾值: > 1%
      評估期間: 2 個數據點，共 2 分鐘
      動作: SNS → PagerDuty (P1)

SNS Topics:
  genai-demo-critical-alerts:
    訂閱者:
      - PagerDuty 整合
      - 運維團隊 Email
      - Slack #alerts 頻道
  
  genai-demo-warning-alerts:
    訂閱者:
      - Slack #monitoring 頻道
      - 開發團隊 Email
```

## 運維流程

### 日常運維檢查清單

```yaml
每日檢查 (自動化):
  健康檢查狀態:
    - ✅ 主要區域健康檢查正常
    - ✅ 次要區域健康檢查正常
    - ✅ DNS 解析正常
    - ✅ SSL 憑證有效 (>30天)

  效能指標:
    - ✅ 平均回應時間 < 1秒
    - ✅ 錯誤率 < 1%
    - ✅ 可用性 > 99.9%
    - ✅ DNS 解析時間 < 100ms

每週檢查 (手動):
  故障轉移測試:
    - 🔧 模擬主要區域故障
    - 🔧 驗證自動切換功能
    - 🔧 測試故障回切流程
    - 🔧 檢查告警通知

  容量規劃:
    - 📊 分析流量趨勢
    - 📊 評估資源使用率
    - 📊 預測容量需求
    - 📊 更新擴展計劃

每月檢查 (深度):
  災難恢復演練:
    - 🎯 完整 DR 演練
    - 🎯 RTO/RPO 驗證
    - 🎯 流程文檔更新
    - 🎯 團隊培訓

  安全審查:
    - 🔒 存取權限審查
    - 🔒 SSL/TLS 配置檢查
    - 🔒 安全群組規則審查
    - 🔒 合規性檢查
```

### 故障排除手冊

```yaml
常見問題診斷:
  DNS 解析問題:
    症狀: 用戶無法存取網站
    檢查步驟:
      1. 驗證 Route 53 健康檢查狀態
      2. 檢查 DNS 記錄配置
      3. 測試從不同地點的 DNS 解析
      4. 檢查 TTL 設定
    解決方案:
      - 更新 DNS 記錄
      - 清除 DNS 快取
      - 調整健康檢查配置

  健康檢查失敗:
    症狀: Route 53 顯示區域不健康
    檢查步驟:
      1. 檢查 ALB 狀態
      2. 驗證目標群組健康狀態
      3. 檢查 /actuator/health 端點
      4. 查看應用程式日誌
    解決方案:
      - 重啟不健康的實例
      - 調整健康檢查參數
      - 修復應用程式問題

  跨區域延遲高:
    症狀: 用戶回報存取速度慢
    檢查步驟:
      1. 檢查 CloudFront 快取命中率
      2. 測量區域間網路延遲
      3. 分析 ALB 存取日誌
      4. 檢查資料庫查詢效能
    解決方案:
      - 優化 CloudFront 配置
      - 調整快取策略
      - 優化資料庫查詢
      - 考慮增加邊緣節點

緊急聯絡資訊:
  P0 事件 (服務完全中斷):
    - PagerDuty: 自動呼叫值班工程師
    - Slack: #incident-response
    - 升級路徑: 值班工程師 → 技術主管 → CTO

  P1 事件 (部分功能影響):
    - Slack: #alerts
    - Email: ops-team@company.com
    - 回應時間: 1小時內

  P2 事件 (效能問題):
    - Slack: #monitoring
    - 回應時間: 4小時內
```

## 效能優化

### DNS 效能優化

```yaml
DNS 快取優化:
  TTL 設定:
    - A 記錄: 300秒 (正常情況)
    - A 記錄: 60秒 (故障轉移期間)
    - CNAME 記錄: 3600秒
    - NS 記錄: 86400秒

  解析器優化:
    - 使用 Route 53 Resolver
    - 啟用 DNS64 支援
    - 配置條件轉發規則
    - 監控查詢模式

CloudFront 優化:
  快取策略:
    - 靜態資源: 24小時
    - API 回應: 5分鐘
    - 動態內容: 不快取
    - 錯誤頁面: 5分鐘

  邊緣節點:
    - 啟用所有邊緣節點
    - 使用 HTTP/2 和 HTTP/3
    - 啟用 Gzip 壓縮
    - 配置自定義錯誤頁面

網路效能:
  連線優化:
    - 啟用 TCP Fast Open
    - 使用 Keep-Alive 連線
    - 優化 SSL/TLS 握手
    - 實施 HTTP/2 Server Push

  頻寬管理:
    - 監控頻寬使用
    - 實施 QoS 政策
    - 優化資料傳輸
    - 使用 CDN 分流
```

### 全球效能監控

```mermaid
graph TB
    subgraph "全球監控點"
        subgraph "亞太地區"
            AP1[台北監控點]
            AP2[東京監控點]
            AP3[新加坡監控點]
            AP4[雪梨監控點]
        end
        
        subgraph "北美地區"
            NA1[紐約監控點]
            NA2[洛杉磯監控點]
            NA3[多倫多監控點]
        end
        
        subgraph "歐洲地區"
            EU1[倫敦監控點]
            EU2[法蘭克福監控點]
            EU3[巴黎監控點]
        end
    end
    
    subgraph "效能指標"
        DNS_Time[DNS 解析時間]
        Connect_Time[連線建立時間]
        SSL_Time[SSL 握手時間]
        TTFB[首位元組時間]
        Load_Time[頁面載入時間]
    end
    
    subgraph "告警閾值"
        DNS_Alert[DNS > 200ms]
        Connect_Alert[連線 > 500ms]
        SSL_Alert[SSL > 300ms]
        TTFB_Alert[TTFB > 1s]
        Load_Alert[載入 > 3s]
    end
    
    AP1 --> DNS_Time
    AP2 --> Connect_Time
    NA1 --> SSL_Time
    EU1 --> TTFB
    AP3 --> Load_Time
    
    DNS_Time --> DNS_Alert
    Connect_Time --> Connect_Alert
    SSL_Time --> SSL_Alert
    TTFB --> TTFB_Alert
    Load_Time --> Load_Alert
    
    style AP1 fill:#c8e6c9
    style AP2 fill:#c8e6c9
    style DNS_Alert fill:#ffcdd2
    style Connect_Alert fill:#ffcdd2
```

---

**文件狀態**: ✅ 完成  
**下一步**: 查看 [Deployment Viewpoint](../deployment/deployment-architecture.md) 了解部署架構  
**相關文件**: 
- [Infrastructure Viewpoint](../infrastructure/aws-resource-architecture.md)
- [Security Viewpoint](../security/iam-permissions-architecture.md)
- [Deployment Viewpoint](../deployment/deployment-architecture.md)