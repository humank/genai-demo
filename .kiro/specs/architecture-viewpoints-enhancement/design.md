# 架構視點與觀點全面強化實作設計

**建立日期**: 2025年9月24日 上午10:11 (台北時間)  
**設計版本**: 1.0  
**負責團隊**: 架構師 + 全端開發團隊

## 📋 設計概述

本設計文檔基於 [需求文檔](requirements.md) 中的 13 個核心需求，提供詳細的技術設計方案。特別針對需求12 (Staging 環境測試計劃和工具策略) 提供完整的設計架構。

## 🎯 整體架構設計

### 核心設計原則

1. **分層測試策略**: Local (記憶體模擬) → Staging (真實 AWS) → Production
2. **自動化優先**: 所有測試流程完全自動化
3. **成本控制**: 合理控制 Staging 環境的 AWS 成本
4. **快速反饋**: 提供快速的測試結果和問題定位
5. **安全合規**: 確保測試過程符合安全和合規要求

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

CI/CD 層:
├── GitHub Actions (主要 CI/CD)
├── AWS CodePipeline (AWS 原生管道)
├── AWS CodeBuild (建構服務)
└── AWS CodeDeploy (部署服務)
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
