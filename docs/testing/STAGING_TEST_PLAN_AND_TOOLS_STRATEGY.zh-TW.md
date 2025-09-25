# Staging 環境測試計劃和工具策略\n\n## 📋 **概覽**\n\n**建立日期**: 2025年9月24日 上午9:50 (台北時間)  \n**目標**: 建立完整的 Staging 環境測試計劃和工具策略  \n**範圍**: 涵蓋所有外部 AWS 服務整合測試  \n**負責團隊**: QA 工程師 + DevOps 工程師 + 架構師\n\n本文件提供針對 Staging 環境的完整測試計劃，包括測試策略、工具選擇、\n自動化方案和最佳實踐建議。由於 Local 環境已完全使用記憶體模擬，\n所有真實 AWS 服務的整合測試都必須在 Staging 環境中進行。\n\n## 🎯 **測試目標和策略**\n\n### **核心測試目標**\n\n1. **服務整合驗證**: 確保所有 AWS 服務正確整合和運作\n2. **效能基準建立**: 建立真實環境下的效能基準\n3. **故障恢復驗證**: 驗證系統的韌性和故障恢復能力\n4. **安全機制驗證**: 確保所有安全控制措施正確實施\n5. **資料一致性驗證**: 確保跨服務的資料同步和一致性\n6. **負載處理驗證**: 驗證系統在預期負載下的表現\n\n### **測試策略原則**\n\n- **真實環境**: 使用與生產環境相同的 AWS 服務配置\n- **自動化優先**: 所有測試都應該自動化執行\n- **持續整合**: 整合到 CI/CD 管道中\n- **快速反饋**: 提供快速的測試結果和問題定位\n- **成本控制**: 合理控制測試執行的 AWS 成本\n- **資料安全**: 確保測試資料的安全和隱私\n\n## 🏗️ **測試架構和分層**\n\n### **測試分層策略**\n\n```\nStaging 測試金字塔:\n├── E2E 測試 (10%) - 完整業務流程\n├── 整合測試 (30%) - 服務間整合\n├── 組件測試 (40%) - 單一服務與 AWS 服務整合\n└── 基礎設施測試 (20%) - AWS 資源配置和連線\n```\n\n### **測試環境架構**\n\n```\nStaging Environment:\n├── EKS Cluster (測試應用程式)\n├── ElastiCache Redis Cluster (分散式鎖)\n├── Aurora Global Database (資料存儲)\n├── MSK Kafka Cluster (事件處理)\n├── CloudWatch + X-Ray (監控追蹤)\n├── ALB + Route53 (負載均衡和 DNS)\n└── IAM + KMS + Secrets Manager (安全服務)\n```\n\n## 🔧 **推薦測試工具和技術棧**\n\n### **API 和服務測試工具**\n\n#### **1. REST Assured (推薦) ⭐⭐⭐⭐⭐**\n```java\n// 優點: Java 原生、與現有測試框架整合好\n@Test\nvoid should_create_customer_via_api() {\n    given()\n        .contentType(ContentType.JSON)\n        .body(customerRequest)\n    .when()\n        .post(\"/api/v1/customers\")\n    .then()\n        .statusCode(201)\n        .body(\"id\", notNullValue())\n        .body(\"name\", equalTo(\"John Doe\"));\n}\n```\n\n**使用場景**: API 整合測試、服務間通訊驗證  \n**整合方式**: 與 JUnit 5 整合，支援 TestContainers  \n**成本**: 免費開源\n\n#### **2. Postman + Newman ⭐⭐⭐⭐**\n```bash\n# 優點: 視覺化測試設計、豐富的斷言功能\nnewman run staging-api-tests.json \\\n  --environment staging-env.json \\\n  --reporters cli,html \\\n  --reporter-html-export test-report.html\n```\n\n**使用場景**: 快速 API 測試、手動測試轉自動化  \n**整合方式**: CI/CD 管道中執行 Newman  \n**成本**: 基本功能免費，進階功能付費\n\n### **負載和效能測試工具**\n\n#### **1. K6 (推薦) ⭐⭐⭐⭐⭐**\n```javascript\n// 優點: 現代化、雲原生、JavaScript 語法\nimport http from 'k6/http';\nimport { check } from 'k6';\n\nexport let options = {\n  stages: [\n    { duration: '2m', target: 100 },\n    { duration: '5m', target: 100 },\n    { duration: '2m', target: 0 },\n  ],\n};\n\nexport default function() {\n  let response = http.get('https://staging-api.example.com/health');\n  check(response, {\n    'status is 200': (r) => r.status === 200,\n    'response time < 500ms': (r) => r.timings.duration < 500,\n  });\n}\n```\n\n**使用場景**: 負載測試、效能基準測試  \n**整合方式**: Docker 容器執行、CloudWatch 指標整合  \n**成本**: 開源免費，雲服務付費\n\n#### **2. Artillery ⭐⭐⭐⭐**\n```yaml\n# 優點: 配置簡單、支援 WebSocket 和 Socket.io\nconfig:\n  target: 'https://staging-api.example.com'\n  phases:\n    - duration: 60\n      arrivalRate: 10\n    - duration: 120\n      arrivalRate: 50\n\nscenarios:\n  - name: \"Customer API Load Test\"\n    requests:\n      - get:\n          url: \"/api/v1/customers\"\n          expect:\n            - statusCode: 200\n            - contentType: json\n```\n\n**使用場景**: 快速負載測試、WebSocket 測試  \n**整合方式**: npm 安裝、CI/CD 整合  \n**成本**: 開源免費\n\n### **資料庫測試工具**\n\n#### **1. Testcontainers + PostgreSQL ⭐⭐⭐⭐⭐**\n```java\n// 優點: 與現有 Java 測試框架完美整合\n@Testcontainers\nclass DatabaseIntegrationTest {\n    \n    @Container\n    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(\"postgres:15\")\n            .withDatabaseName(\"testdb\")\n            .withUsername(\"test\")\n            .withPassword(\"test\");\n    \n    @Test\n    void should_connect_to_aurora_and_perform_crud_operations() {\n        // 測試 Aurora 連線和 CRUD 操作\n    }\n}\n```\n\n**使用場景**: 資料庫整合測試、Migration 測試  \n**整合方式**: JUnit 5 + Spring Boot Test  \n**成本**: 免費開源\n\n#### **2. Flyway Test Extensions ⭐⭐⭐⭐**\n```java\n// 優點: 專門針對 Flyway Migration 測試\n@FlywayTest\nclass MigrationTest {\n    \n    @Test\n    @FlywayTest(locationsForMigrate = {\"db/migration\"})\n    void should_migrate_database_successfully() {\n        // 測試資料庫 Migration\n    }\n}\n```\n\n**使用場景**: 資料庫 Migration 測試  \n**整合方式**: 與 Flyway 和 Spring Boot 整合  \n**成本**: 免費開源\n\n### **訊息佇列測試工具**\n\n#### **1. Embedded Kafka (本機) + Real MSK (Staging) ⭐⭐⭐⭐⭐**\n```java\n// 優點: 與 Spring Kafka 完美整合\n@SpringBootTest\n@EmbeddedKafka(partitions = 1, topics = {\"test-topic\"})\nclass KafkaIntegrationTest {\n    \n    @Test\n    void should_publish_and_consume_messages() {\n        // 測試 Kafka 訊息發布和消費\n    }\n}\n```\n\n**使用場景**: Kafka 整合測試、事件驅動架構測試  \n**整合方式**: Spring Boot Test + Spring Kafka  \n**成本**: 免費開源\n\n### **監控和追蹤測試工具**\n\n#### **1. AWS X-Ray SDK Test Utilities ⭐⭐⭐⭐**\n```java\n// 優點: AWS 官方支援、與 X-Ray 完美整合\n@Test\nvoid should_create_traces_in_xray() {\n    Subsegment subsegment = AWSXRay.beginSubsegment(\"test-operation\");\n    try {\n        // 執行業務邏輯\n        customerService.createCustomer(request);\n    } finally {\n        AWSXRay.endSubsegment();\n    }\n    \n    // 驗證 X-Ray 追蹤資料\n}\n```\n\n**使用場景**: 分散式追蹤測試、效能監控驗證  \n**整合方式**: AWS X-Ray SDK + Spring Boot  \n**成本**: AWS X-Ray 使用費用\n\n### **安全測試工具**\n\n#### **1. OWASP ZAP ⭐⭐⭐⭐**\n```bash\n# 優點: 全面的安全掃描、免費開源\ndocker run -t owasp/zap2docker-stable zap-baseline.py \\\n  -t https://staging-api.example.com \\\n  -r zap-report.html\n```\n\n**使用場景**: 安全漏洞掃描、API 安全測試  \n**整合方式**: Docker 容器、CI/CD 整合  \n**成本**: 免費開源\n\n#### **2. AWS Security Hub API ⭐⭐⭐⭐⭐**\n```java\n// 優點: AWS 原生、與其他 AWS 服務整合\n@Test\nvoid should_have_no_high_severity_findings() {\n    SecurityHubClient client = SecurityHubClient.create();\n    GetFindingsResponse findings = client.getFindings(\n        GetFindingsRequest.builder()\n            .filters(AwsSecurityFindingFilters.builder()\n                .severityLabel(StringFilter.builder()\n                    .value(\"HIGH\")\n                    .comparison(StringFilterComparison.EQUALS)\n                    .build())\n                .build())\n            .build());\n    \n    assertThat(findings.findings()).isEmpty();\n}\n```\n\n**使用場景**: AWS 安全合規檢查、安全態勢監控  \n**整合方式**: AWS SDK + JUnit 5  \n**成本**: AWS Security Hub 使用費用\n\n## 🚀 **自動化測試實施方案**\n\n### **CI/CD 整合策略**\n\n#### **GitHub Actions 工作流程**\n```yaml\nname: Staging Integration Tests\n\non:\n  push:\n    branches: [main, develop]\n  schedule:\n    - cron: '0 2 * * *'  # 每日執行\n\njobs:\n  staging-tests:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      \n      - name: Setup Java 21\n        uses: actions/setup-java@v4\n        with:\n          java-version: '21'\n          distribution: 'temurin'\n      \n      - name: Configure AWS Credentials\n        uses: aws-actions/configure-aws-credentials@v4\n        with:\n          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}\n          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}\n          aws-region: ap-northeast-1\n      \n      - name: Run Infrastructure Tests\n        run: ./scripts/test-infrastructure.sh\n      \n      - name: Run Service Integration Tests\n        run: ./gradlew stagingIntegrationTest\n      \n      - name: Run Load Tests\n        run: ./scripts/run-load-tests.sh\n      \n      - name: Run Security Scans\n        run: ./scripts/security-scan.sh\n      \n      - name: Generate Test Report\n        run: ./scripts/generate-test-report.sh\n```\n\n### **測試資料管理策略**\n\n#### **測試資料生成工具**\n```java\n// 使用 Java Faker 生成測試資料\n@Component\npublic class TestDataGenerator {\n    \n    private final Faker faker = new Faker();\n    \n    public Customer generateCustomer() {\n        return Customer.builder()\n            .name(faker.name().fullName())\n            .email(faker.internet().emailAddress())\n            .phone(faker.phoneNumber().phoneNumber())\n            .build();\n    }\n    \n    public List<Customer> generateCustomers(int count) {\n        return IntStream.range(0, count)\n            .mapToObj(i -> generateCustomer())\n            .collect(Collectors.toList());\n    }\n}\n```\n\n#### **測試資料清理策略**\n```java\n@TestExecutionListener\npublic class TestDataCleanupListener implements TestExecutionListener {\n    \n    @Override\n    public void afterTestMethod(TestContext testContext) {\n        // 清理測試產生的資料\n        cleanupTestData();\n    }\n    \n    private void cleanupTestData() {\n        // 清理資料庫測試資料\n        // 清理 Redis 測試 keys\n        // 清理 S3 測試檔案\n        // 清理 CloudWatch 測試指標\n    }\n}\n```\n\n## 📊 **測試監控和報告**\n\n### **測試指標收集**\n\n#### **關鍵測試指標**\n- **測試覆蓋率**: API 端點覆蓋率、業務流程覆蓋率\n- **測試執行時間**: 各類測試的執行時間趨勢\n- **測試成功率**: 測試通過率和失敗率統計\n- **效能指標**: 響應時間、吞吐量、資源使用率\n- **錯誤率**: 各種錯誤類型的統計和分析\n\n#### **測試報告生成**\n```bash\n#!/bin/bash\n# generate-test-report.sh\n\necho \"Generating comprehensive test report...\"\n\n# 收集測試結果\nallure generate build/allure-results --clean -o build/reports/allure\n\n# 生成效能報告\nk6 run --out json=performance-results.json performance-tests.js\n\n# 生成安全掃描報告\nzap-cli --zap-url http://localhost:8080 report -o security-report.html -f html\n\n# 整合所有報告\npython scripts/merge-reports.py\n\necho \"Test report generated: build/reports/comprehensive-test-report.html\"\n```\n\n### **告警和通知機制**\n\n#### **Slack 整合**\n```yaml\n- name: Notify Test Results\n  if: always()\n  uses: 8398a7/action-slack@v3\n  with:\n    status: ${{ job.status }}\n    text: |\n      Staging Tests ${{ job.status }}!\n      \n      📊 Test Summary:\n      - Integration Tests: ${{ steps.integration.outcome }}\n      - Load Tests: ${{ steps.load.outcome }}\n      - Security Scans: ${{ steps.security.outcome }}\n      \n      📈 Performance:\n      - Average Response Time: ${{ env.AVG_RESPONSE_TIME }}ms\n      - Peak Throughput: ${{ env.PEAK_THROUGHPUT }} req/s\n      \n      🔗 Full Report: ${{ env.REPORT_URL }}\n  env:\n    SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}\n```\n\n## 💰 **成本控制和優化**\n\n### **測試成本估算**\n\n| 服務 | 預估月成本 | 優化建議 |\n|------|------------|----------|\n| **EKS Cluster** | $150-200 | 使用 Spot Instances |\n| **ElastiCache** | $100-150 | 測試後自動關閉 |\n| **Aurora** | $200-300 | 使用 Aurora Serverless |\n| **MSK** | $150-200 | 最小配置，按需擴展 |\n| **CloudWatch** | $50-100 | 設定日誌保留期限 |\n| **總計** | $650-950 | 可優化至 $400-600 |\n\n### **成本優化策略**\n\n1. **按需啟動**: 只在測試時啟動 Staging 環境\n2. **資源共享**: 多個測試共享同一套基礎設施\n3. **自動清理**: 測試完成後自動清理資源\n4. **Spot Instances**: 使用 Spot Instances 降低計算成本\n5. **預留實例**: 對於長期使用的資源購買預留實例\n\n## 🔒 **安全和合規考量**\n\n### **測試資料安全**\n\n- **資料脫敏**: 所有測試資料都必須脫敏處理\n- **存取控制**: 嚴格控制 Staging 環境的存取權限\n- **資料清理**: 測試完成後徹底清理敏感資料\n- **加密傳輸**: 所有資料傳輸都使用 TLS 加密\n- **審計日誌**: 記錄所有測試活動的審計日誌\n\n### **合規要求**\n\n- **GDPR**: 確保個人資料保護合規\n- **SOC 2**: 遵循 SOC 2 安全控制要求\n- **ISO 27001**: 符合資訊安全管理標準\n- **PCI DSS**: 如涉及支付資料，需符合 PCI DSS 要求\n\n## 📋 **實施時程和里程碑**\n\n### **第一階段 (2 週)**\n- [ ] 建立基礎測試框架和工具\n- [ ] 實施 Redis/ElastiCache 整合測試\n- [ ] 建立 CI/CD 整合\n- [ ] 完成基礎設施測試\n\n### **第二階段 (4 週)**\n- [ ] 實施完整的服務整合測試\n- [ ] 建立負載和效能測試\n- [ ] 實施安全測試\n- [ ] 建立測試報告和監控\n\n### **第三階段 (6 週)**\n- [ ] 實施故障模擬和韌性測試\n- [ ] 完善測試自動化\n- [ ] 建立測試資料管理\n- [ ] 完成文件和培訓\n\n---\n\n**文件維護者**: QA Team + DevOps Team  \n**最後更新**: 2025年9月24日 上午9:50 (台北時間)  \n**審核狀態**: 待審核  \n**版本**: 1.0.0\n"## 
🛠️ **具體工具配置和實施指南**

### **REST Assured 配置範例**

#### **基礎配置**
```java
// StagingTestConfiguration.java
@TestConfiguration
@ActiveProfiles("staging")
public class StagingTestConfiguration {
    
    @Bean
    @Primary
    public RestAssuredConfig restAssuredConfig() {
        return RestAssuredConfig.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000)
                .setParam(CoreConnectionPNames.SO_TIMEOUT, 30000))
            .logConfig(LogConfig.logConfig()
                .enableLoggingOfRequestAndResponseIfValidationFails());
    }
    
    @Bean
    public StagingApiClient stagingApiClient(@Value("${staging.api.base-url}") String baseUrl) {
        return new StagingApiClient(baseUrl);
    }
}
```

#### **API 測試基礎類別**
```java
// BaseStagingApiTest.java
@SpringBootTest
@ActiveProfiles("staging")
public abstract class BaseStagingApiTest {
    
    @Value("${staging.api.base-url}")
    protected String baseUrl;
    
    @BeforeEach
    void setUp() {
        RestAssured.baseURI = baseUrl;
        RestAssured.config = RestAssuredConfig.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000));
    }
    
    protected String getAuthToken() {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("username", "test-user", "password", "test-pass"))
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .path("token");
    }
}
```

### **K6 負載測試配置**

#### **基礎負載測試腳本**
```javascript
// load-test-basic.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 自定義指標
export let errorRate = new Rate('errors');

export let options = {
  stages: [
    { duration: '2m', target: 10 },   // 暖身
    { duration: '5m', target: 50 },   // 正常負載
    { duration: '2m', target: 100 },  // 峰值負載
    { duration: '5m', target: 100 },  // 維持峰值
    { duration: '2m', target: 0 },    // 降載
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95% 請求 < 500ms
    http_req_failed: ['rate<0.1'],     // 錯誤率 < 10%
    errors: ['rate<0.1'],              // 自定義錯誤率 < 10%
  },
};

export default function() {
  // 健康檢查
  let healthResponse = http.get(`${__ENV.BASE_URL}/actuator/health`);
  check(healthResponse, {
    'health check status is 200': (r) => r.status === 200,
    'health check response time < 200ms': (r) => r.timings.duration < 200,
  }) || errorRate.add(1);

  // 客戶 API 測試
  let customerResponse = http.get(`${__ENV.BASE_URL}/api/v1/customers`);
  check(customerResponse, {
    'customer API status is 200': (r) => r.status === 200,
    'customer API response time < 1000ms': (r) => r.timings.duration < 1000,
    'customer API returns JSON': (r) => r.headers['Content-Type'].includes('application/json'),
  }) || errorRate.add(1);

  sleep(1);
}
```

#### **分散式鎖壓力測試**
```javascript
// distributed-lock-stress-test.js
import http from 'k6/http';
import { check } from 'k6';
import { SharedArray } from 'k6/data';

const lockKeys = new SharedArray('lockKeys', function() {
  return Array.from({length: 100}, (_, i) => `stress-test-lock-${i}`);
});

export let options = {
  scenarios: {
    lock_contention: {
      executor: 'constant-vus',
      vus: 50,
      duration: '5m',
    },
  },
};

export default function() {
  const lockKey = lockKeys[Math.floor(Math.random() * lockKeys.length)];
  
  // 嘗試獲取鎖
  let acquireResponse = http.post(`${__ENV.BASE_URL}/api/test/locks/${lockKey}/acquire`, 
    JSON.stringify({
      waitTime: 1,
      leaseTime: 5,
      timeUnit: 'SECONDS'
    }), {
      headers: { 'Content-Type': 'application/json' },
    });
  
  check(acquireResponse, {
    'lock acquire request successful': (r) => r.status === 200 || r.status === 409,
    'lock acquire response time < 2000ms': (r) => r.timings.duration < 2000,
  });
  
  if (acquireResponse.status === 200) {
    // 持有鎖一段時間
    sleep(Math.random() * 2);
    
    // 釋放鎖
    let releaseResponse = http.del(`${__ENV.BASE_URL}/api/test/locks/${lockKey}`);
    check(releaseResponse, {
      'lock release successful': (r) => r.status === 200,
    });
  }
}
```

### **Testcontainers 整合測試配置**

#### **Aurora 資料庫測試**
```java
// AuroraDatabaseIntegrationTest.java
@SpringBootTest
@ActiveProfiles("staging")
@Testcontainers
class AuroraDatabaseIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("genai_demo_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withInitScript("init-test-db.sql");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    @DisplayName("Should perform CRUD operations on Aurora database")
    void should_perform_crud_operations() {
        // Given
        Customer customer = Customer.builder()
            .name("Test Customer")
            .email("test@example.com")
            .build();
        
        // When - Create
        Customer savedCustomer = customerRepository.save(customer);
        
        // Then
        assertThat(savedCustomer.getId()).isNotNull();
        
        // When - Read
        Optional<Customer> foundCustomer = customerRepository.findById(savedCustomer.getId());
        
        // Then
        assertThat(foundCustomer).isPresent();
        assertThat(foundCustomer.get().getName()).isEqualTo("Test Customer");
        
        // When - Update
        foundCustomer.get().setName("Updated Customer");
        Customer updatedCustomer = customerRepository.save(foundCustomer.get());
        
        // Then
        assertThat(updatedCustomer.getName()).isEqualTo("Updated Customer");
        
        // When - Delete
        customerRepository.delete(updatedCustomer);
        
        // Then
        assertThat(customerRepository.findById(savedCustomer.getId())).isEmpty();
    }
    
    @Test
    @DisplayName("Should handle concurrent database operations")
    void should_handle_concurrent_database_operations() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    Customer customer = Customer.builder()
                        .name("Concurrent Customer " + threadId)
                        .email("concurrent" + threadId + "@example.com")
                        .build();
                    
                    Customer saved = customerRepository.save(customer);
                    if (saved.getId() != null) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        assertThat(successCount.get()).isEqualTo(threadCount);
    }
}
```

### **Kafka 整合測試配置**

#### **MSK Kafka 測試**
```java
// KafkaIntegrationTest.java
@SpringBootTest
@ActiveProfiles("staging")
@EmbeddedKafka(
    partitions = 1,
    topics = {"customer-events", "order-events"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
class KafkaIntegrationTest {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private CustomerEventHandler customerEventHandler;
    
    @Test
    @DisplayName("Should publish and consume customer events")
    void should_publish_and_consume_customer_events() throws InterruptedException {
        // Given
        CustomerCreatedEvent event = CustomerCreatedEvent.builder()
            .customerId("test-customer-123")
            .customerName("Test Customer")
            .email("test@example.com")
            .timestamp(Instant.now())
            .build();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<CustomerCreatedEvent> receivedEvent = new AtomicReference<>();
        
        // 設定事件監聽器
        customerEventHandler.setEventListener((receivedEvent::set));
        customerEventHandler.setLatch(latch);
        
        // When
        kafkaTemplate.send("customer-events", event.getCustomerId(), event);
        
        // Then
        boolean messageReceived = latch.await(10, TimeUnit.SECONDS);
        assertThat(messageReceived).isTrue();
        assertThat(receivedEvent.get()).isNotNull();
        assertThat(receivedEvent.get().getCustomerId()).isEqualTo("test-customer-123");
    }
    
    @Test
    @DisplayName("Should handle Kafka broker failures gracefully")
    void should_handle_kafka_broker_failures() {
        // Given
        CustomerCreatedEvent event = CustomerCreatedEvent.builder()
            .customerId("test-customer-456")
            .customerName("Test Customer 2")
            .email("test2@example.com")
            .timestamp(Instant.now())
            .build();
        
        // When & Then - 應該能處理發送失敗
        assertDoesNotThrow(() -> {
            kafkaTemplate.send("customer-events", event.getCustomerId(), event);
        });
    }
}
```

### **安全測試配置**

#### **OWASP ZAP 自動化掃描**
```bash
#!/bin/bash
# security-scan.sh

echo "🔒 Starting security scan with OWASP ZAP..."

# 啟動 ZAP daemon
docker run -d --name zap-daemon \
  -p 8080:8080 \
  owasp/zap2docker-stable zap.sh -daemon -host 0.0.0.0 -port 8080

# 等待 ZAP 啟動
sleep 30

# 執行基線掃描
docker run --rm \
  --network host \
  -v $(pwd)/security-reports:/zap/wrk/:rw \
  owasp/zap2docker-stable zap-baseline.py \
  -t ${STAGING_APP_URL} \
  -r baseline-report.html \
  -x baseline-report.xml

# 執行完整掃描
docker run --rm \
  --network host \
  -v $(pwd)/security-reports:/zap/wrk/:rw \
  owasp/zap2docker-stable zap-full-scan.py \
  -t ${STAGING_APP_URL} \
  -r full-scan-report.html \
  -x full-scan-report.xml

# 清理
docker stop zap-daemon
docker rm zap-daemon

echo "✅ Security scan completed. Reports available in security-reports/"
```

#### **AWS Security Hub 整合**
```java
// SecurityComplianceTest.java
@SpringBootTest
@ActiveProfiles("staging")
class SecurityComplianceTest {
    
    private SecurityHubClient securityHubClient;
    
    @BeforeEach
    void setUp() {
        securityHubClient = SecurityHubClient.builder()
            .region(Region.AP_NORTHEAST_1)
            .build();
    }
    
    @Test
    @DisplayName("Should have no critical security findings")
    void should_have_no_critical_security_findings() {
        GetFindingsResponse findings = securityHubClient.getFindings(
            GetFindingsRequest.builder()
                .filters(AwsSecurityFindingFilters.builder()
                    .severityLabel(StringFilter.builder()
                        .value("CRITICAL")
                        .comparison(StringFilterComparison.EQUALS)
                        .build())
                    .recordState(StringFilter.builder()
                        .value("ACTIVE")
                        .comparison(StringFilterComparison.EQUALS)
                        .build())
                    .build())
                .build());
        
        assertThat(findings.findings())
            .as("No critical security findings should be present")
            .isEmpty();
    }
    
    @Test
    @DisplayName("Should have encryption enabled for all resources")
    void should_have_encryption_enabled() {
        GetFindingsResponse findings = securityHubClient.getFindings(
            GetFindingsRequest.builder()
                .filters(AwsSecurityFindingFilters.builder()
                    .title(StringFilter.builder()
                        .value("encryption")
                        .comparison(StringFilterComparison.CONTAINS)
                        .build())
                    .complianceStatus(StringFilter.builder()
                        .value("FAILED")
                        .comparison(StringFilterComparison.EQUALS)
                        .build())
                    .build())
                .build());
        
        assertThat(findings.findings())
            .as("All resources should have encryption enabled")
            .isEmpty();
    }
}
```

## 📈 **測試執行和監控腳本**

### **主要測試執行腳本**
```bash
#!/bin/bash
# run-staging-tests.sh

set -e

echo "🚀 Starting Staging Environment Tests..."

# 環境變數檢查
required_vars=("STAGING_APP_URL" "AWS_REGION" "STAGING_DB_HOST")
for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo "❌ Error: Environment variable $var is not set"
        exit 1
    fi
done

# 建立報告目錄
mkdir -p reports/{integration,load,security,infrastructure}

echo "📋 Test Environment:"
echo "  App URL: $STAGING_APP_URL"
echo "  AWS Region: $AWS_REGION"
echo "  Database: $STAGING_DB_HOST"
echo ""

# 1. 基礎設施測試
echo "🏗️ Running Infrastructure Tests..."
./scripts/test-infrastructure.sh || exit 1

# 2. 服務整合測試
echo "🔧 Running Service Integration Tests..."
./gradlew stagingIntegrationTest \
  -Dspring.profiles.active=staging \
  -Dstaging.app.url=$STAGING_APP_URL \
  || exit 1

# 3. 負載測試
echo "⚡ Running Load Tests..."
k6 run \
  --env BASE_URL=$STAGING_APP_URL \
  --out json=reports/load/load-test-results.json \
  scripts/k6/load-test-basic.js || exit 1

# 4. 安全掃描
echo "🔒 Running Security Scans..."
./scripts/security-scan.sh || exit 1

# 5. 效能基準測試
echo "📊 Running Performance Benchmarks..."
k6 run \
  --env BASE_URL=$STAGING_APP_URL \
  --out json=reports/load/performance-benchmark.json \
  scripts/k6/performance-benchmark.js || exit 1

# 6. 故障恢復測試
echo "🛡️ Running Resilience Tests..."
./scripts/chaos-engineering-tests.sh || exit 1

# 7. 生成綜合報告
echo "📋 Generating Test Reports..."
./scripts/generate-comprehensive-report.sh

echo "✅ All Staging Tests Completed Successfully!"
echo "📊 Reports available in: reports/"
echo "🔗 Main Report: reports/comprehensive-test-report.html"
```

### **測試監控腳本**
```bash
#!/bin/bash
# monitor-test-execution.sh

echo "📊 Monitoring test execution..."

# 監控應用程式健康狀態
monitor_app_health() {
    while true; do
        response=$(curl -s -o /dev/null -w "%{http_code}" $STAGING_APP_URL/actuator/health)
        if [ "$response" != "200" ]; then
            echo "⚠️  Application health check failed: HTTP $response"
        fi
        sleep 30
    done
}

# 監控資源使用率
monitor_resource_usage() {
    while true; do
        # 獲取 EKS 資源使用率
        kubectl top nodes
        kubectl top pods -n genai-demo
        
        # 獲取 RDS 連線數
        aws rds describe-db-instances \
          --db-instance-identifier staging-aurora \
          --query 'DBInstances[0].DbInstanceStatus'
        
        sleep 60
    done
}

# 背景執行監控
monitor_app_health &
HEALTH_PID=$!

monitor_resource_usage &
RESOURCE_PID=$!

# 等待測試完成信號
wait_for_test_completion() {
    while [ ! -f "/tmp/staging-tests-completed" ]; do
        sleep 10
    done
}

wait_for_test_completion

# 清理監控程序
kill $HEALTH_PID $RESOURCE_PID

echo "📊 Test monitoring completed"
```

## 🔄 **故障模擬和韌性測試**

### **Chaos Engineering 測試**
```bash
#!/bin/bash
# chaos-engineering-tests.sh

echo "🛡️ Starting Chaos Engineering Tests..."

# 1. Pod 故障模擬
echo "💥 Simulating pod failures..."
kubectl delete pod -l app=genai-demo -n genai-demo --grace-period=0 &

# 等待 pod 重啟
sleep 30

# 檢查服務恢復
curl -f $STAGING_APP_URL/actuator/health || {
    echo "❌ Service failed to recover from pod failure"
    exit 1
}

# 2. 網路延遲模擬
echo "🌐 Simulating network latency..."
# 使用 tc (traffic control) 模擬網路延遲
sudo tc qdisc add dev eth0 root netem delay 100ms

# 執行延遲環境下的測試
k6 run --env BASE_URL=$STAGING_APP_URL scripts/k6/latency-test.js

# 清理網路設定
sudo tc qdisc del dev eth0 root

# 3. 資料庫連線中斷模擬
echo "🗄️ Simulating database connection issues..."
# 暫時修改安全群組規則阻斷資料庫連線
aws ec2 revoke-security-group-ingress \
  --group-id $DB_SECURITY_GROUP_ID \
  --protocol tcp \
  --port 5432 \
  --source-group $APP_SECURITY_GROUP_ID

# 測試應用程式的錯誤處理
sleep 10

# 恢復資料庫連線
aws ec2 authorize-security-group-ingress \
  --group-id $DB_SECURITY_GROUP_ID \
  --protocol tcp \
  --port 5432 \
  --source-group $APP_SECURITY_GROUP_ID

echo "✅ Chaos Engineering Tests Completed"
```

### **自動化故障恢復驗證**
```java
// ResilienceTest.java
@SpringBootTest
@ActiveProfiles("staging")
class ResilienceTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("Should recover from temporary Redis unavailability")
    void should_recover_from_redis_unavailability() {
        // Given - 正常狀態下的操作
        ResponseEntity<String> normalResponse = restTemplate.getForEntity(
            "/api/v1/customers", String.class);
        assertThat(normalResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // When - 模擬 Redis 不可用（透過配置或網路）
        // 這裡可以透過修改 Redis 配置或使用 Testcontainers 來模擬
        
        // Then - 應用程式應該能夠降級運作
        ResponseEntity<String> degradedResponse = restTemplate.getForEntity(
            "/api/v1/customers", String.class);
        
        // 可能返回 200（降級模式）或 503（服務暫時不可用）
        assertThat(degradedResponse.getStatusCode())
            .isIn(HttpStatus.OK, HttpStatus.SERVICE_UNAVAILABLE);
        
        // When - Redis 恢復後
        // 恢復 Redis 連線
        
        // Then - 服務應該完全恢復
        await().atMost(Duration.ofMinutes(2))
            .pollInterval(Duration.ofSeconds(5))
            .until(() -> {
                ResponseEntity<String> recoveredResponse = restTemplate.getForEntity(
                    "/api/v1/customers", String.class);
                return recoveredResponse.getStatusCode() == HttpStatus.OK;
            });
    }
    
    @Test
    @DisplayName("Should handle high load gracefully")
    void should_handle_high_load_gracefully() throws InterruptedException {
        int threadCount = 50;
        int requestsPerThread = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        try {
                            ResponseEntity<String> response = restTemplate.getForEntity(
                                "/api/v1/health", String.class);
                            
                            if (response.getStatusCode().is2xxSuccessful()) {
                                successCount.incrementAndGet();
                            } else {
                                errorCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(5, TimeUnit.MINUTES);
        executor.shutdown();
        
        int totalRequests = threadCount * requestsPerThread;
        double successRate = (double) successCount.get() / totalRequests;
        
        // 在高負載下，成功率應該至少達到 95%
        assertThat(successRate).isGreaterThan(0.95);
        
        // 錯誤率應該低於 5%
        double errorRate = (double) errorCount.get() / totalRequests;
        assertThat(errorRate).isLessThan(0.05);
    }
}
```

---

**持續更新中...**  
**下一部分將包含**: 測試資料管理、CI/CD 整合詳細配置、成本優化實施方案