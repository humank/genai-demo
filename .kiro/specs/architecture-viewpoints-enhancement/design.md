# 架構視點與觀點全面強化實作設計

**建立日期**: 2025年9月24日 上午10:11 (台北時間)  
**設計版本**: 1.0  
**負責團隊**: 架構師 + 全端開發團隊

## 📋 設計概述

本設計文檔基於 [需求文檔](requirements.md) 中的 13 個核心需求，提供詳細的技術設計方案。特別針對需求12 (Staging 環境測試計劃和工具策略) 提供完整的設計架構。

## 🎯 整體架構設計

### 核心設計原則

1. **Active-Active 雙活架構**: 台灣和日本兩個區域同時提供完整服務
2. **分層測試策略**: Local (記憶體模擬) → Staging (真實 AWS) → Production
3. **自動化優先**: 所有測試流程完全自動化
4. **成本控制**: 合理控制 Staging 環境的 AWS 成本
5. **快速反饋**: 提供快速的測試結果和問題定位
6. **安全合規**: 確保測試過程符合安全和合規要求

## 🌏 需求4.1: Active-Active 多區域架構設計

### 設計目標

建立真正的 Active-Active 雙活架構，實現：
- **零停機時間**: 任一區域故障時系統持續運行
- **負載分散**: 兩個區域同時承擔生產流量
- **資料一致性**: 跨區域資料同步和衝突解決
- **智能路由**: 基於地理位置和健康狀況的流量分配

### Active-Active 架構拓撲

```
全球用戶流量
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│                Route 53 DNS 智能路由                        │
│  - 地理位置路由 (台灣用戶 → 台灣, 日本用戶 → 日本)          │
│  - 健康檢查和自動故障轉移                                   │
│  - 延遲優化路由 (全球用戶)                                  │
└─────────────────────────────────────────────────────────────┘
       │                                    │
       ▼                                    ▼
┌─────────────────────────────┐    ┌─────────────────────────────┐
│     台灣區域 (Primary)      │    │     日本區域 (Secondary)    │
│   ap-northeast-1           │    │   ap-northeast-1           │
├─────────────────────────────┤    ├─────────────────────────────┤
│ ┌─────────────────────────┐ │    │ ┌─────────────────────────┐ │
│ │ EKS Cluster (Active)    │ │◄──►│ │ EKS Cluster (Active)    │ │
│ │ - 完整應用程式部署      │ │    │ │ - 完整應用程式部署      │ │
│ │ - 自動擴展              │ │    │ │ - 自動擴展              │ │
│ │ - 負載均衡              │ │    │ │ - 負載均衡              │ │
│ └─────────────────────────┘ │    │ └─────────────────────────┘ │
│ ┌─────────────────────────┐ │    │ ┌─────────────────────────┐ │
│ │ Aurora Global DB        │ │◄──►│ │ Aurora Global DB        │ │
│ │ - 讀寫主節點            │ │    │ │ - 讀寫次節點            │ │
│ │ - 雙向同步 (<1s)        │ │    │ │ - 雙向同步 (<1s)        │ │
│ │ - 衝突解決              │ │    │ │ - 衝突解決              │ │
│ └─────────────────────────┘ │    │ └─────────────────────────┘ │
│ ┌─────────────────────────┐ │    │ ┌─────────────────────────┐ │
│ │ MSK Kafka Cluster       │ │◄──►│ │ MSK Kafka Cluster       │ │
│ │ - 事件生產和消費        │ │    │ │ - 事件生產和消費        │ │
│ │ - MirrorMaker 2.0       │ │    │ │ - MirrorMaker 2.0       │ │
│ │ - 跨區域複製            │ │    │ │ - 跨區域複製            │ │
│ └─────────────────────────┘ │    │ └─────────────────────────┘ │
│ ┌─────────────────────────┐ │    │ ┌─────────────────────────┐ │
│ │ ElastiCache Redis       │ │◄──►│ │ ElastiCache Redis       │ │
│ │ - 本地快取              │ │    │ │ - 本地快取              │ │
│ │ - 跨區域複製            │ │    │ │ - 跨區域複製            │ │
│ │ - 分散式鎖              │ │    │ │ - 分散式鎖              │ │
│ └─────────────────────────┘ │    │ └─────────────────────────┘ │
└─────────────────────────────┘    └─────────────────────────────┘
```

### 詳細組件設計

#### 1. Aurora Global Database Active-Active 配置

```typescript
// 台灣區域 Aurora 主集群
const taiwanAuroraCluster = new rds.DatabaseCluster(this, 'TaiwanAuroraCluster', {
  engine: rds.DatabaseClusterEngine.auroraPostgres({
    version: rds.AuroraPostgresEngineVersion.VER_15_4
  }),
  globalClusterIdentifier: 'genai-demo-global-cluster',
  // 主要區域配置
  isPrimaryCluster: true,
  // 讀寫能力
  readers: [
    rds.ClusterInstance.provisioned('reader-1', {
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.R6G, ec2.InstanceSize.LARGE)
    })
  ],
  writer: rds.ClusterInstance.provisioned('writer', {
    instanceType: ec2.InstanceType.of(ec2.InstanceClass.R6G, ec2.InstanceSize.XLARGE)
  }),
  // 跨區域複製配置
  backupRetention: cdk.Duration.days(7),
  // 效能監控
  monitoringInterval: cdk.Duration.minutes(1),
  // 自動故障轉移
  enablePerformanceInsights: true
});

// 日本區域 Aurora 次集群 (具備讀寫能力)
const japanAuroraCluster = new rds.DatabaseCluster(this, 'JapanAuroraCluster', {
  engine: rds.DatabaseClusterEngine.auroraPostgres({
    version: rds.AuroraPostgresEngineVersion.VER_15_4
  }),
  globalClusterIdentifier: 'genai-demo-global-cluster',
  // 次要區域但具備讀寫能力
  isSecondaryCluster: true,
  enableGlobalWriteForwarding: true, // 啟用全球寫入轉發
  readers: [
    rds.ClusterInstance.provisioned('reader-1', {
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.R6G, ec2.InstanceSize.LARGE)
    })
  ],
  writer: rds.ClusterInstance.provisioned('writer', {
    instanceType: ec2.InstanceType.of(ec2.InstanceClass.R6G, ec2.InstanceSize.XLARGE)
  })
});

// 衝突解決策略
const conflictResolutionLambda = new lambda.Function(this, 'ConflictResolution', {
  runtime: lambda.Runtime.NODEJS_18_X,
  handler: 'index.handler',
  code: lambda.Code.fromInline(`
    exports.handler = async (event) => {
      // 基於時間戳和區域優先級的衝突解決
      const { conflictData } = event;
      
      // 台灣區域優先級較高 (業務主要在台灣)
      if (conflictData.taiwanTimestamp && conflictData.japanTimestamp) {
        const timeDiff = Math.abs(conflictData.taiwanTimestamp - conflictData.japanTimestamp);
        
        // 如果時間差小於1秒，使用區域優先級
        if (timeDiff < 1000) {
          return { winner: 'taiwan', reason: 'region_priority' };
        }
        
        // 否則使用最新時間戳
        return {
          winner: conflictData.taiwanTimestamp > conflictData.japanTimestamp ? 'taiwan' : 'japan',
          reason: 'latest_timestamp'
        };
      }
    };
  `)
});
```

#### 2. EKS Active-Active 集群配置

```typescript
// 台灣 EKS 集群
const taiwanEksCluster = new eks.Cluster(this, 'TaiwanEKSCluster', {
  version: eks.KubernetesVersion.V1_28,
  defaultCapacity: 3,
  defaultCapacityInstance: ec2.InstanceType.of(
    ec2.InstanceClass.M6I, 
    ec2.InstanceSize.LARGE
  ),
  // 多可用區部署
  vpcSubnets: [
    { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS }
  ],
  // 自動擴展配置
  autoScalingGroupProvider: {
    minCapacity: 3,
    maxCapacity: 20,
    desiredCapacity: 5
  }
});

// 日本 EKS 集群 (相同配置)
const japanEksCluster = new eks.Cluster(this, 'JapanEKSCluster', {
  version: eks.KubernetesVersion.V1_28,
  defaultCapacity: 3,
  defaultCapacityInstance: ec2.InstanceType.of(
    ec2.InstanceClass.M6I, 
    ec2.InstanceSize.LARGE
  ),
  vpcSubnets: [
    { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS }
  ],
  autoScalingGroupProvider: {
    minCapacity: 3,
    maxCapacity: 20,
    desiredCapacity: 5
  }
});

// 跨區域 VPC 對等連接
const crossRegionPeering = new ec2.CfnVPCPeeringConnection(this, 'CrossRegionPeering', {
  vpcId: taiwanVpc.vpcId,
  peerVpcId: japanVpc.vpcId,
  peerRegion: 'ap-northeast-1'
});

// ⚠️ 原 CodePipeline 設計 (已棄用 - 2025年1月21日)
// 已改用 GitOps 方案: GitHub Actions + ArgoCD + Argo Rollouts
// 參見: docs/gitops-deployment-guide.md

/*
// 原同步部署管道設計 (保留作為歷史記錄)
const syncDeploymentPipeline = new codepipeline.Pipeline(this, 'SyncDeployment', {
  pipelineName: 'ActiveActive-SyncDeployment',
  stages: [
    {
      stageName: 'Source',
      actions: [
        new codepipeline_actions.GitHubSourceAction({
          actionName: 'GitHub_Source',
          owner: 'your-org',
          repo: 'genai-demo',
          oauthToken: cdk.SecretValue.secretsManager('github-token'),
          output: sourceOutput
        })
      ]
    },
    {
      stageName: 'Build',
      actions: [
        new codepipeline_actions.CodeBuildAction({
          actionName: 'Build_Application',
          project: buildProject,
          input: sourceOutput,
          outputs: [buildOutput]
        })
      ]
    },
    {
      stageName: 'Deploy_Taiwan',
      actions: [
        new codepipeline_actions.EksAction({
          actionName: 'Deploy_Taiwan_EKS',
          cluster: taiwanEksCluster,
          input: buildOutput
        })
      ]
    },
    {
      stageName: 'Deploy_Japan',
      actions: [
        new codepipeline_actions.EksAction({
          actionName: 'Deploy_Japan_EKS',
          cluster: japanEksCluster,
          input: buildOutput
        })
      ]
    },
    {
      stageName: 'Verify_Sync',
      actions: [
        new codepipeline_actions.LambdaInvokeAction({
          actionName: 'Verify_Deployment_Sync',
          lambda: verificationLambda
        })
      ]
    }
  ]
});
*/

// ✅ 新 GitOps 部署架構 (2025年1月21日起)
// 
// GitHub Actions Workflow (.github/workflows/ci-cd.yml):
// 1. Source: GitHub repository (自動觸發)
// 2. Build: 
//    - 並行測試 (Unit, Integration, BDD, Architecture)
//    - 安全掃描 (Trivy, CodeQL)
//    - Docker 多架構建構 (amd64, arm64)
//    - 推送至 ECR
// 3. Deploy:
//    - 更新 Kubernetes manifests
//    - 提交至 Git (觸發 ArgoCD)
//
// ArgoCD + Argo Rollouts:
// 1. ArgoCD 自動同步 (3分鐘間隔)
// 2. Argo Rollouts 執行 Canary 部署:
//    - Backend: 10% → 25% → 50% → 75% → 100%
//    - Frontend: 20% → 50% → 100%
// 3. 自動化分析和回滾
//
// 多區域部署:
// - Taiwan (ap-northeast-1): 主要區域
// - Japan (ap-northeast-1): 次要區域
// - 透過 ArgoCD ApplicationSet 管理多區域部署
// - 使用 Smart Routing Layer 進行區域間流量管理
```

#### 3. Route 53 智能流量管理

```typescript
// 主要 DNS 記錄 - 台灣區域
const taiwanRecord = new route53.ARecord(this, 'TaiwanRecord', {
  zone: hostedZone,
  recordName: 'api',
  target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(taiwanALB)),
  setIdentifier: 'taiwan-region',
  // 地理位置路由 - 台灣和亞洲用戶
  geoLocation: route53.GeoLocation.country('TW'),
  healthCheck: taiwanHealthCheck
});

// 次要 DNS 記錄 - 日本區域
const japanRecord = new route53.ARecord(this, 'JapanRecord', {
  zone: hostedZone,
  recordName: 'api',
  target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(japanALB)),
  setIdentifier: 'japan-region',
  // 地理位置路由 - 日本用戶
  geoLocation: route53.GeoLocation.country('JP'),
  healthCheck: japanHealthCheck
});

// 全球用戶的延遲路由
const globalRecord = new route53.ARecord(this, 'GlobalRecord', {
  zone: hostedZone,
  recordName: 'api',
  target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(taiwanALB)),
  setIdentifier: 'global-latency',
  // 延遲路由 - 選擇最低延遲的區域
  region: 'ap-northeast-1'
});

// 進階健康檢查
const taiwanHealthCheck = new route53.HealthCheck(this, 'TaiwanHealthCheck', {
  type: route53.HealthCheckType.HTTPS,
  resourcePath: '/actuator/health/readiness',
  fqdn: 'taiwan.api.genai-demo.com',
  port: 443,
  requestInterval: 30,
  failureThreshold: 2, // 2次失敗後切換
  // 複合健康檢查
  childHealthChecks: [
    databaseHealthCheck,
    applicationHealthCheck,
    redisHealthCheck
  ]
});
```

#### 4. 應用程式層區域感知配置

```java
// 區域感知配置
@Configuration
@Profile({"taiwan", "japan"})
public class RegionAwareConfiguration {
    
    @Value("${aws.region}")
    private String currentRegion;
    
    @Bean
    @ConditionalOnProperty(name = "aws.region", havingValue = "ap-northeast-1")
    public RegionService taiwanRegionService() {
        return new RegionService("taiwan", "ap-northeast-1");
    }
    
    @Bean
    @ConditionalOnProperty(name = "aws.region", havingValue = "ap-northeast-1") 
    public RegionService japanRegionService() {
        return new RegionService("japan", "ap-northeast-1");
    }
    
    @Bean
    public DataSourceRouter dataSourceRouter() {
        return new DataSourceRouter(currentRegion);
    }
}

// 資料源路由器
@Component
public class DataSourceRouter {
    
    private final String currentRegion;
    private final Map<String, DataSource> regionDataSources;
    
    public DataSource getWriteDataSource() {
        // 優先使用本地區域進行寫入
        return regionDataSources.get(currentRegion + "-write");
    }
    
    public DataSource getReadDataSource() {
        // 讀取可以使用本地區域或最近的區域
        DataSource localRead = regionDataSources.get(currentRegion + "-read");
        
        if (isHealthy(localRead)) {
            return localRead;
        }
        
        // 故障轉移到其他區域
        return regionDataSources.get(getBackupRegion() + "-read");
    }
    
    private boolean isHealthy(DataSource dataSource) {
        try {
            Connection conn = dataSource.getConnection();
            conn.close();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}

// 跨區域事件處理
@Component
public class CrossRegionEventHandler {
    
    @EventListener
    @Async
    public void handleCrossRegionSync(DomainEvent event) {
        // 確保事件在兩個區域都被處理
        if (event.getOriginRegion().equals(currentRegion)) {
            // 本地事件，需要同步到其他區域
            syncEventToOtherRegion(event);
        } else {
            // 來自其他區域的事件，檢查是否需要本地處理
            processRemoteEvent(event);
        }
    }
    
    private void syncEventToOtherRegion(DomainEvent event) {
        // 使用 MSK MirrorMaker 或直接 API 調用
        crossRegionEventPublisher.publish(event, getTargetRegion());
    }
}
```

### 技術棧選擇

```
測試框架層:
├── JUnit 5 + Spring Boot Test (Java 原生整合)
├── REST Assured (API 測試)
├── K6 (負載測試)
├── Testcontainers (容器化測試)
└── OWASP ZAP (安全測試)

AWS 服務層:
├── EKS (應用程式運行)
├── ElastiCache Redis (分散式鎖)
├── Aurora Global Database (資料存儲)
├── MSK Kafka (事件處理)
├── CloudWatch + X-Ray (監控追蹤)
└── Security Hub (安全合規)

CI/CD 層 (GitOps 架構):
├── GitHub Actions (CI - 建構、測試、安全掃描)
├── ArgoCD (CD - 持續部署、同步管理)
├── Argo Rollouts (漸進式部署 - Canary/Blue-Green)
└── ~~AWS CodePipeline/CodeBuild/CodeDeploy~~ (已棄用，改用 GitOps)
```

## 🏗️ 需求12: Staging 環境測試計劃和工具策略設計

### 設計目標

基於現有的 [STAGING_TEST_PLAN_AND_TOOLS_STRATEGY.md](../../../docs/testing/STAGING_TEST_PLAN_AND_TOOLS_STRATEGY.md) 和 [STAGING_ENVIRONMENT_TESTING.md](../../../docs/testing/STAGING_ENVIRONMENT_TESTING.md)，建立完整的 Staging 環境測試自動化體系。

### 測試架構設計

#### 1. 測試分層架構

```
Staging 測試金字塔:
┌─────────────────────────────────────┐
│ E2E 測試 (10%)                      │ ← 完整業務流程驗證
├─────────────────────────────────────┤
│ 整合測試 (30%)                      │ ← 服務間整合驗證
├─────────────────────────────────────┤
│ 組件測試 (40%)                      │ ← AWS 服務整合驗證
├─────────────────────────────────────┤
│ 基礎設施測試 (20%)                  │ ← AWS 資源配置驗證
└─────────────────────────────────────┘
```

#### 2. 測試環境拓撲

```
Staging Environment Topology:
┌─────────────────────────────────────────────────────────────┐
│                    AWS Staging Environment                   │
├─────────────────────────────────────────────────────────────┤
│ ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│ │ EKS Cluster │  │ ElastiCache │  │   Aurora    │          │
│ │   (App)     │  │   (Redis)   │  │ (Database)  │          │
│ └─────────────┘  └─────────────┘  └─────────────┘          │
│ ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│ │ MSK Kafka   │  │ CloudWatch  │  │ Security    │          │
│ │ (Events)    │  │ (Monitor)   │  │    Hub      │          │
│ └─────────────┘  └─────────────┘  └─────────────┘          │
├─────────────────────────────────────────────────────────────┤
│                    Test Execution Layer                     │
├─────────────────────────────────────────────────────────────┤
│ ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│ │ GitHub      │  │ Test Data   │  │ Monitoring  │          │
│ │ Actions     │  │ Management  │  │ & Alerts    │          │
│ └─────────────┘  └─────────────┘  └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
```

### 詳細組件設計

#### 1. 測試工具整合設計

##### REST Assured 整合架構
```java
// 設計模式: Page Object Model for API Testing
@Component
public class StagingApiTestClient {
    
    private final RestTemplate restTemplate;
    private final String baseUrl;
    
    // 客戶 API 測試客戶端
    public CustomerApiClient customers() {
        return new CustomerApiClient(baseUrl + "/api/v1/customers", restTemplate);
    }
    
    // 訂單 API 測試客戶端
    public OrderApiClient orders() {
        return new OrderApiClient(baseUrl + "/api/v1/orders", restTemplate);
    }
    
    // 分散式鎖測試客戶端
    public DistributedLockApiClient locks() {
        return new DistributedLockApiClient(baseUrl + "/api/test/locks", restTemplate);
    }
}

// 具體實現範例
public class CustomerApiClient {
    
    public ValidatableResponse createCustomer(CreateCustomerRequest request) {
        return given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/")
        .then();
    }
    
    public ValidatableResponse getCustomer(String customerId) {
        return given()
        .when()
            .get("/{id}", customerId)
        .then();
    }
}
```

##### K6 負載測試架構
```javascript
// 設計模式: 模組化測試腳本
// k6/modules/api-client.js
export class ApiClient {
    constructor(baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    createCustomer(customerData) {
        return http.post(`${this.baseUrl}/api/v1/customers`, 
            JSON.stringify(customerData), {
                headers: { 'Content-Type': 'application/json' }
            });
    }
    
    acquireLock(lockKey, options) {
        return http.post(`${this.baseUrl}/api/test/locks/${lockKey}/acquire`,
            JSON.stringify(options), {
                headers: { 'Content-Type': 'application/json' }
            });
    }
}

// k6/scenarios/distributed-lock-load-test.js
import { ApiClient } from '../modules/api-client.js';

export let options = {
    scenarios: {
        lock_contention: {
            executor: 'constant-vus',
            vus: 50,
            duration: '5m',
        },
        lock_performance: {
            executor: 'ramping-vus',
            startVUs: 1,
            stages: [
                { duration: '2m', target: 20 },
                { duration: '5m', target: 20 },
                { duration: '2m', target: 0 },
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<2000'],
        http_req_failed: ['rate<0.1'],
        'lock_acquisition_success_rate': ['rate>0.8'],
    },
};
```

#### 2. 測試資料管理設計

##### 測試資料生成策略
```java
// 設計模式: Builder Pattern + Factory Pattern
@Component
public class StagingTestDataFactory {
    
    private final Faker faker = new Faker();
    
    public CustomerTestDataBuilder customerBuilder() {
        return CustomerTestDataBuilder.create()
            .withName(faker.name().fullName())
            .withEmail(generateUniqueEmail())
            .withPhone(faker.phoneNumber().phoneNumber());
    }
    
    public OrderTestDataBuilder orderBuilder() {
        return OrderTestDataBuilder.create()
            .withCustomerId(generateTestCustomerId())
            .withItems(generateRandomItems())
            .withTotalAmount(calculateTotalAmount());
    }
    
    private String generateUniqueEmail() {
        return String.format("test-%s-%d@staging.example.com", 
            faker.internet().slug(), System.currentTimeMillis());
    }
}

// 測試資料清理策略
@Component
public class StagingTestDataCleaner {
    
    @EventListener
    public void cleanupAfterTest(TestExecutionEvent event) {
        if (event.getTestContext().hasAttribute("testDataKeys")) {
            List<String> keys = event.getTestContext().getAttribute("testDataKeys");
            cleanupTestData(keys);
        }
    }
    
    private void cleanupTestData(List<String> keys) {
        // 清理資料庫測試資料
        cleanupDatabaseData(keys);
        // 清理 Redis 測試 keys
        cleanupRedisData(keys);
        // 清理 S3 測試檔案
        cleanupS3Data(keys);
    }
}
```

#### 3. 監控和告警設計

##### CloudWatch 整合監控
```java
// 設計模式: Observer Pattern for Test Monitoring
@Component
public class StagingTestMonitor {
    
    private final CloudWatchClient cloudWatchClient;
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void onTestStart(TestStartEvent event) {
        publishMetric("StagingTest.Started", 1.0, 
            Map.of("testClass", event.getTestClass().getSimpleName()));
    }
    
    @EventListener
    public void onTestSuccess(TestSuccessEvent event) {
        publishMetric("StagingTest.Success", 1.0,
            Map.of("testMethod", event.getTestMethod().getName()));
    }
    
    @EventListener
    public void onTestFailure(TestFailureEvent event) {
        publishMetric("StagingTest.Failure", 1.0,
            Map.of(
                "testMethod", event.getTestMethod().getName(),
                "errorType", event.getException().getClass().getSimpleName()
            ));
    }
    
    private void publishMetric(String metricName, Double value, Map<String, String> dimensions) {
        // 發布到 CloudWatch
        cloudWatchClient.putMetricData(PutMetricDataRequest.builder()
            .namespace("GenAIDemo/StagingTests")
            .metricData(MetricDatum.builder()
                .metricName(metricName)
                .value(value)
                .dimensions(convertToDimensions(dimensions))
                .timestamp(Instant.now())
                .build())
            .build());
    }
}
```

##### 告警配置設計
```yaml
# CloudWatch Alarms Configuration
StagingTestAlarms:
  TestFailureRate:
    MetricName: StagingTest.Failure
    Threshold: 0.1  # 10% 失敗率
    ComparisonOperator: GreaterThanThreshold
    EvaluationPeriods: 2
    Period: 300
    
  TestExecutionTime:
    MetricName: StagingTest.Duration
    Threshold: 1800  # 30 分鐘
    ComparisonOperator: GreaterThanThreshold
    EvaluationPeriods: 1
    Period: 300
    
  RedisConnectionFailure:
    MetricName: Redis.ConnectionFailure
    Threshold: 5
    ComparisonOperator: GreaterThanThreshold
    EvaluationPeriods: 1
    Period: 60
```

#### 4. CI/CD 整合設計

##### GitHub Actions 工作流程設計
```yaml
# .github/workflows/staging-comprehensive-tests.yml
name: Staging Comprehensive Tests

on:
  schedule:
    - cron: '0 2 * * *'  # 每日凌晨 2 點
  workflow_dispatch:
    inputs:
      test_suite:
        description: 'Test suite to run'
        required: true
        default: 'all'
        type: choice
        options:
        - all
        - integration
        - load
        - security
        - resilience

jobs:
  setup:
    runs-on: ubuntu-latest
    outputs:
      test-id: ${{ steps.generate-id.outputs.test-id }}
    steps:
      - id: generate-id
        run: echo "test-id=staging-test-$(date +%Y%m%d-%H%M%S)" >> $GITHUB_OUTPUT

  infrastructure-tests:
    needs: setup
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Test AWS Infrastructure
        run: ./scripts/test-aws-infrastructure.sh
        env:
          TEST_ID: ${{ needs.setup.outputs.test-id }}

  integration-tests:
    needs: [setup, infrastructure-tests]
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [redis, database, kafka, monitoring]
    steps:
      - uses: actions/checkout@v4
      - name: Run ${{ matrix.service }} Integration Tests
        run: ./scripts/staging-${{ matrix.service }}-tests.sh
        env:
          TEST_ID: ${{ needs.setup.outputs.test-id }}

  load-tests:
    needs: [setup, integration-tests]
    runs-on: ubuntu-latest
    if: github.event.inputs.test_suite == 'all' || github.event.inputs.test_suite == 'load'
    steps:
      - uses: actions/checkout@v4
      - name: Run Load Tests
        run: ./scripts/run-k6-load-tests.sh
        env:
          TEST_ID: ${{ needs.setup.outputs.test-id }}

  security-tests:
    needs: [setup, integration-tests]
    runs-on: ubuntu-latest
    if: github.event.inputs.test_suite == 'all' || github.event.inputs.test_suite == 'security'
    steps:
      - uses: actions/checkout@v4
      - name: Run Security Tests
        run: ./scripts/run-security-tests.sh
        env:
          TEST_ID: ${{ needs.setup.outputs.test-id }}

  resilience-tests:
    needs: [setup, integration-tests]
    runs-on: ubuntu-latest
    if: github.event.inputs.test_suite == 'all' || github.event.inputs.test_suite == 'resilience'
    steps:
      - uses: actions/checkout@v4
      - name: Run Resilience Tests
        run: ./scripts/run-chaos-tests.sh
        env:
          TEST_ID: ${{ needs.setup.outputs.test-id }}

  report-generation:
    needs: [setup, integration-tests, load-tests, security-tests, resilience-tests]
    if: always()
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Generate Comprehensive Report
        run: ./scripts/generate-staging-test-report.sh
        env:
          TEST_ID: ${{ needs.setup.outputs.test-id }}
      - name: Upload Reports
        uses: actions/upload-artifact@v4
        with:
          name: staging-test-reports-${{ needs.setup.outputs.test-id }}
          path: reports/
```

#### 5. 成本控制設計

##### 資源管理策略
```bash
# scripts/manage-staging-resources.sh
#!/bin/bash

# 成本控制策略實現
manage_staging_resources() {
    local action=$1  # start, stop, cleanup
    
    case $action in
        "start")
            echo "🚀 Starting Staging Resources..."
            # 啟動 EKS 節點
            aws eks update-nodegroup-config \
                --cluster-name staging-cluster \
                --nodegroup-name staging-nodes \
                --scaling-config minSize=2,maxSize=5,desiredSize=2
            
            # 啟動 ElastiCache
            aws elasticache modify-replication-group \
                --replication-group-id staging-redis \
                --apply-immediately
            ;;
            
        "stop")
            echo "⏹️ Stopping Staging Resources..."
            # 縮減 EKS 節點
            aws eks update-nodegroup-config \
                --cluster-name staging-cluster \
                --nodegroup-name staging-nodes \
                --scaling-config minSize=0,maxSize=2,desiredSize=0
            ;;
            
        "cleanup")
            echo "🧹 Cleaning up Test Resources..."
            # 清理測試產生的資源
            cleanup_test_data
            cleanup_cloudwatch_logs
            cleanup_s3_test_files
            ;;
    esac
}

# 成本監控
monitor_test_costs() {
    local test_id=$1
    
    # 獲取測試期間的成本
    aws ce get-cost-and-usage \
        --time-period Start=$(date -d '1 hour ago' -I),End=$(date -I) \
        --granularity HOURLY \
        --metrics BlendedCost \
        --group-by Type=DIMENSION,Key=SERVICE
}
```

### 安全和合規設計

#### 1. 測試資料安全
```java
// 設計模式: Strategy Pattern for Data Security
public interface TestDataSecurityStrategy {
    String maskSensitiveData(String data);
    void encryptTestData(TestDataContext context);
    void auditTestDataAccess(String userId, String operation);
}

@Component
public class StagingTestDataSecurity implements TestDataSecurityStrategy {
    
    @Override
    public String maskSensitiveData(String data) {
        // 實施資料脫敏
        return data.replaceAll("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", 
                              "***@***.***");
    }
    
    @Override
    public void encryptTestData(TestDataContext context) {
        // 加密敏感測試資料
        context.getCustomerData().forEach(customer -> {
            customer.setEmail(encryptPII(customer.getEmail()));
            customer.setPhone(encryptPII(customer.getPhone()));
        });
    }
    
    @Override
    public void auditTestDataAccess(String userId, String operation) {
        // 記錄測試資料存取審計
        auditLogger.info("Test data access: user={}, operation={}, timestamp={}", 
                        userId, operation, Instant.now());
    }
}
```

#### 2. 合規檢查自動化
```java
// GDPR 合規檢查
@Component
public class GDPRComplianceChecker {
    
    public ComplianceReport checkTestDataCompliance(TestDataSet testData) {
        ComplianceReport report = new ComplianceReport();
        
        // 檢查個人資料處理
        report.addCheck("personal_data_processing", 
                       checkPersonalDataProcessing(testData));
        
        // 檢查資料保留期限
        report.addCheck("data_retention", 
                       checkDataRetention(testData));
        
        // 檢查資料主體權利
        report.addCheck("data_subject_rights", 
                       checkDataSubjectRights(testData));
        
        return report;
    }
}
```

### 效能基準和優化設計

#### 1. 效能基準建立
```java
// 設計模式: Template Method Pattern for Performance Testing
public abstract class PerformanceBenchmarkTest {
    
    protected abstract void setupBenchmark();
    protected abstract void executeBenchmark();
    protected abstract void teardownBenchmark();
    protected abstract PerformanceMetrics collectMetrics();
    
    public final BenchmarkResult runBenchmark() {
        setupBenchmark();
        
        long startTime = System.nanoTime();
        executeBenchmark();
        long endTime = System.nanoTime();
        
        PerformanceMetrics metrics = collectMetrics();
        teardownBenchmark();
        
        return BenchmarkResult.builder()
            .executionTime(Duration.ofNanos(endTime - startTime))
            .metrics(metrics)
            .timestamp(Instant.now())
            .build();
    }
}

// Redis 效能基準測試
public class RedisPerformanceBenchmark extends PerformanceBenchmarkTest {
    
    @Override
    protected void executeBenchmark() {
        // 執行 1000 次鎖操作
        for (int i = 0; i < 1000; i++) {
            String lockKey = "benchmark-lock-" + i;
            distributedLockManager.acquireLock(lockKey, 1, TimeUnit.SECONDS);
            distributedLockManager.releaseLock(lockKey);
        }
    }
    
    @Override
    protected PerformanceMetrics collectMetrics() {
        return PerformanceMetrics.builder()
            .operationsPerSecond(calculateOPS())
            .averageLatency(calculateAverageLatency())
            .p95Latency(calculateP95Latency())
            .errorRate(calculateErrorRate())
            .build();
    }
}
```

## 🔄 其他需求設計概要

### 需求1-11: 並發控制到觀點卓越化

基於現有的實作基礎，其他需求的設計將採用類似的模式：

1. **需求1-8**: 基於現有的 Redis 分散式鎖架構擴展
2. **需求9-10**: GenBI 和 RAG 系統採用微服務架構
3. **需求11**: 觀點實現基於 Rozanski & Woods 方法論
4. **需求13**: AWS Insights 服務全面整合

### 需求13: AWS Insights 服務設計概要

```yaml
# AWS Insights 整合架構
AWS_Insights_Integration:
  Container_Insights:
    - EKS 集群監控
    - Pod 資源使用分析
    - 容器效能指標收集
    
  RDS_Performance_Insights:
    - Aurora 查詢效能分析
    - 慢查詢檢測和優化
    - 資料庫連線池監控
    
  Lambda_Insights:
    - 函數執行指標
    - 冷啟動分析
    - 成本優化建議
    
  Application_Insights:
    - 前端 RUM 監控
    - JavaScript 錯誤追蹤
    - Core Web Vitals 分析
```

## 📊 設計驗證和測試

### 設計驗證標準

1. **功能驗證**: 所有設計組件都有對應的測試用例
2. **效能驗證**: 符合需求文檔中的效能指標
3. **安全驗證**: 通過安全掃描和合規檢查
4. **可維護性驗證**: 程式碼覆蓋率 > 80%
5. **成本驗證**: Staging 環境成本控制在預算範圍內

### 設計審查檢查清單

- [ ] 架構設計符合 Rozanski & Woods 方法論
- [ ] 測試策略覆蓋所有關鍵路徑
- [ ] 安全設計符合企業標準
- [ ] 效能設計滿足 SLA 要求
- [ ] 成本設計在預算範圍內
- [ ] 可維護性設計支援長期演進

---

**設計負責人**: Kiro AI Assistant  
**最後更新**: 2025年9月24日 上午10:11 (台北時間)  
**審核狀態**: 待審核  
**版本**: 1.0

### 5. Active-Active 監控和告警設計

#### 跨區域統一監控儀表板

```typescript
// 統一監控儀表板
const activeActiveMonitoringDashboard = new cloudwatch.Dashboard(this, 'ActiveActiveMonitoring', {
  dashboardName: 'ActiveActive-CrossRegion-Monitoring',
  widgets: [
    // 區域健康狀況總覽
    new cloudwatch.GraphWidget({
      title: 'Region Health Overview',
      left: [
        taiwanRegionHealthMetric,
        japanRegionHealthMetric
      ],
      right: [
        crossRegionLatencyMetric
      ]
    }),
    
    // Aurora Global Database 監控
    new cloudwatch.GraphWidget({
      title: 'Aurora Global Database Metrics',
      left: [
        auroraReplicationLagMetric,
        auroraWriteConflictsMetric
      ],
      right: [
        auroraCrossRegionIOMetric
      ]
    }),
    
    // 流量分配監控
    new cloudwatch.GraphWidget({
      title: 'Traffic Distribution',
      left: [
        taiwanTrafficMetric,
        japanTrafficMetric
      ],
      right: [
        route53FailoverMetric
      ]
    }),
    
    // 應用程式效能對比
    new cloudwatch.GraphWidget({
      title: 'Application Performance Comparison',
      left: [
        taiwanResponseTimeMetric,
        japanResponseTimeMetric
      ],
      right: [
        taiwanErrorRateMetric,
        japanErrorRateMetric
      ]
    })
  ]
});

// 跨區域告警配置
const crossRegionAlerts = [
  // 區域故障告警
  new cloudwatch.Alarm(this, 'RegionFailureAlarm', {
    alarmName: 'ActiveActive-RegionFailure',
    metric: regionHealthMetric,
    threshold: 1,
    comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
    evaluationPeriods: 2,
    alarmDescription: 'One or more regions are unhealthy'
  }),
  
  // 跨區域延遲告警
  new cloudwatch.Alarm(this, 'CrossRegionLatencyAlarm', {
    alarmName: 'ActiveActive-HighLatency',
    metric: crossRegionLatencyMetric,
    threshold: 100, // 100ms
    comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
    evaluationPeriods: 3,
    alarmDescription: 'Cross-region latency is too high'
  }),
  
  // 資料同步延遲告警
  new cloudwatch.Alarm(this, 'DataSyncLagAlarm', {
    alarmName: 'ActiveActive-DataSyncLag',
    metric: auroraReplicationLagMetric,
    threshold: 5, // 5秒
    comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
    evaluationPeriods: 2,
    alarmDescription: 'Aurora Global Database replication lag is too high'
  })
];
```

#### 自動化故障轉移邏輯

```typescript
// 智能故障轉移 Lambda
const intelligentFailoverFunction = new lambda.Function(this, 'IntelligentFailover', {
  runtime: lambda.Runtime.NODEJS_18_X,
  handler: 'index.handler',
  timeout: cdk.Duration.minutes(5),
  code: lambda.Code.fromInline(`
    const AWS = require('aws-sdk');
    const route53 = new AWS.Route53();
    const ecs = new AWS.ECS();
    
    exports.handler = async (event) => {
      const { failedRegion, healthyRegion } = event;
      
      console.log(\`Initiating failover from \${failedRegion} to \${healthyRegion}\`);
      
      try {
        // 1. 更新 Route 53 權重，將流量導向健康區域
        await updateRoute53Weights(failedRegion, healthyRegion);
        
        // 2. 擴展健康區域的容量以承接額外流量
        await scaleUpHealthyRegion(healthyRegion);
        
        // 3. 如果是資料庫故障，提升次要區域為主要區域
        if (event.failureType === 'database') {
          await promoteSecondaryDatabase(healthyRegion);
        }
        
        // 4. 更新應用程式配置，指向新的主要區域
        await updateApplicationConfig(healthyRegion);
        
        // 5. 發送通知
        await sendFailoverNotification(failedRegion, healthyRegion);
        
        return {
          statusCode: 200,
          body: JSON.stringify({
            message: 'Failover completed successfully',
            failedRegion,
            healthyRegion,
            timestamp: new Date().toISOString()
          })
        };
        
      } catch (error) {
        console.error('Failover failed:', error);
        await sendFailoverErrorNotification(error);
        throw error;
      }
    };
    
    async function updateRoute53Weights(failedRegion, healthyRegion) {
      // 將故障區域權重設為 0，健康區域權重設為 100
      const params = {
        HostedZoneId: process.env.HOSTED_ZONE_ID,
        ChangeBatch: {
          Changes: [
            {
              Action: 'UPSERT',
              ResourceRecordSet: {
                Name: 'api.genai-demo.com',
                Type: 'A',
                SetIdentifier: failedRegion,
                Weight: 0,
                AliasTarget: {
                  DNSName: process.env[\`\${failedRegion.toUpperCase()}_ALB_DNS\`],
                  EvaluateTargetHealth: true,
                  HostedZoneId: process.env[\`\${failedRegion.toUpperCase()}_ALB_ZONE\`]
                }
              }
            },
            {
              Action: 'UPSERT',
              ResourceRecordSet: {
                Name: 'api.genai-demo.com',
                Type: 'A',
                SetIdentifier: healthyRegion,
                Weight: 100,
                AliasTarget: {
                  DNSName: process.env[\`\${healthyRegion.toUpperCase()}_ALB_DNS\`],
                  EvaluateTargetHealth: true,
                  HostedZoneId: process.env[\`\${healthyRegion.toUpperCase()}_ALB_ZONE\`]
                }
              }
            }
          ]
        }
      };
      
      return route53.changeResourceRecordSets(params).promise();
    }
    
    async function scaleUpHealthyRegion(healthyRegion) {
      // 擴展 EKS 節點組以承接額外流量
      const eksParams = {
        clusterName: \`\${healthyRegion}-eks-cluster\`,
        nodegroupName: \`\${healthyRegion}-nodegroup\`,
        scalingConfig: {
          minSize: 5,
          maxSize: 30,
          desiredSize: 10 // 雙倍容量
        }
      };
      
      return ecs.updateNodegroupConfig(eksParams).promise();
    }
  `)
});

// 故障檢測和自動觸發
const failureDetectionRule = new events.Rule(this, 'FailureDetectionRule', {
  eventPattern: {
    source: ['aws.route53', 'aws.rds', 'aws.eks'],
    detailType: ['Health Check Failed', 'RDS DB Instance Event', 'EKS Cluster State Change']
  },
  targets: [new targets.LambdaFunction(intelligentFailoverFunction)]
});
```

### 6. 成本優化策略

#### Active-Active 成本控制

```typescript
// 智能成本優化
const costOptimizationFunction = new lambda.Function(this, 'CostOptimization', {
  runtime: lambda.Runtime.NODEJS_18_X,
  handler: 'index.handler',
  code: lambda.Code.fromInline(`
    exports.handler = async (event) => {
      const { taiwanMetrics, japanMetrics } = event;
      
      // 分析兩個區域的負載模式
      const taiwanLoad = calculateAverageLoad(taiwanMetrics);
      const japanLoad = calculateAverageLoad(japanMetrics);
      
      // 如果負載不均衡，調整資源分配
      if (Math.abs(taiwanLoad - japanLoad) > 0.3) {
        await rebalanceResources(taiwanLoad, japanLoad);
      }
      
      // 在低峰時段縮減資源
      const currentHour = new Date().getHours();
      if (isOffPeakHour(currentHour)) {
        await scaleDownForOffPeak();
      }
      
      return { optimizationApplied: true };
    };
    
    function calculateAverageLoad(metrics) {
      return metrics.reduce((sum, metric) => sum + metric.value, 0) / metrics.length;
    }
    
    async function rebalanceResources(taiwanLoad, japanLoad) {
      // 將資源從低負載區域移動到高負載區域
      if (taiwanLoad > japanLoad) {
        await scaleUp('taiwan');
        await scaleDown('japan');
      } else {
        await scaleUp('japan');
        await scaleDown('taiwan');
      }
    }
  `)
});

// 成本監控和預算告警
const activeActiveBudget = new budgets.CfnBudget(this, 'ActiveActiveBudget', {
  budget: {
    budgetName: 'ActiveActive-MultiRegion-Budget',
    budgetLimit: {
      amount: 2000, // 每月2000美元預算
      unit: 'USD'
    },
    timeUnit: 'MONTHLY',
    budgetType: 'COST',
    costFilters: {
      Region: ['ap-northeast-1', 'ap-northeast-1']
    }
  },
  notificationsWithSubscribers: [
    {
      notification: {
        notificationType: 'ACTUAL',
        comparisonOperator: 'GREATER_THAN',
        threshold: 80
      },
      subscribers: [{
        subscriptionType: 'EMAIL',
        address: 'devops@company.com'
      }]
    },
    {
      notification: {
        notificationType: 'FORECASTED',
        comparisonOperator: 'GREATER_THAN',
        threshold: 100
      },
      subscribers: [{
        subscriptionType: 'EMAIL',
        address: 'finance@company.com'
      }]
    }
  ]
});
```

### 7. 測試和驗證策略

#### Chaos Engineering 測試

```typescript
// 混沌工程測試
const chaosTestingFunction = new lambda.Function(this, 'ChaosTestingFunction', {
  runtime: lambda.Runtime.NODEJS_18_X,
  handler: 'index.handler',
  code: lambda.Code.fromInline(`
    exports.handler = async (event) => {
      const { testType, targetRegion } = event;
      
      switch (testType) {
        case 'region_failure':
          await simulateRegionFailure(targetRegion);
          break;
        case 'database_lag':
          await simulateDatabaseLag(targetRegion);
          break;
        case 'network_partition':
          await simulateNetworkPartition();
          break;
        case 'high_load':
          await simulateHighLoad(targetRegion);
          break;
      }
      
      // 監控系統響應
      return await monitorSystemResponse(testType, targetRegion);
    };
    
    async function simulateRegionFailure(region) {
      // 暫時停止區域的健康檢查響應
      console.log(\`Simulating failure in region: \${region}\`);
      // 實際實作會調用相應的 AWS API
    }
    
    async function monitorSystemResponse(testType, targetRegion) {
      // 監控故障轉移時間、資料一致性、用戶體驗等
      return {
        testType,
        targetRegion,
        failoverTime: '< 30 seconds',
        dataConsistency: 'maintained',
        userImpact: 'minimal'
      };
    }
  `)
});

// 定期混沌測試排程
const chaosTestingSchedule = new events.Rule(this, 'ChaosTestingSchedule', {
  schedule: events.Schedule.cron({
    minute: '0',
    hour: '2', // 凌晨2點執行
    day: '*',
    month: '*',
    year: '*'
  }),
  targets: [new targets.LambdaFunction(chaosTestingFunction)]
});
```

### 預期效益和 SLA 目標

#### 業務連續性指標
- **可用性**: 99.99% (年停機時間 < 53 分鐘)
- **RTO (恢復時間目標)**: < 30 秒
- **RPO (恢復點目標)**: < 1 秒
- **跨區域延遲**: < 50ms (95th percentile)

#### 效能指標
- **全球用戶響應時間**: < 200ms (95th percentile)
- **資料同步延遲**: < 1 秒
- **故障檢測時間**: < 30 秒
- **自動故障轉移時間**: < 30 秒

#### 成本效益
- **相比單區域增加成本**: < 80%
- **相比傳統 DR 節省成本**: > 40%
- **資源利用率**: > 70% (兩個區域平均)
- **故障轉移成本**: 接近零 (自動化)

這個 Active-Active 架構設計確保了真正的高可用性，同時通過智能負載分配和成本優化策略，實現了成本效益的最大化。
