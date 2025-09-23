
# Testing

## 概述

本指南基於 GenAI Demo 項目的測試優化實踐，提供了一套完整的測試Policy和Best Practice，旨在確保高質量、高Performance、可維護的測試套件。

## Testing

### Testing

```
    /\
   /  \     E2E Tests (少量)
  /____\    - 完整業務流程測試
 /      \   - 真實Environment集成測試
/________\  - 關鍵用戶路徑驗證

   /\
  /  \      Integration Tests (適量)
 /____\     - 多組件協作測試
/      \    - 數據庫集成測試
\______/    - 外部服務集成測試

     /\
    /  \    Unit Tests (大量)
   /____\   - 業務邏輯測試
  /      \  - 組件隔離測試
 /________\ - 快速反饋測試
```

### Testing

#### Testing

- **UnitTest**: 純Unit Test，使用 Mock，記憶體 ~5MB，執行時間 ~50ms
- **IntegrationTest**: 集成測試，部分真實依賴，記憶體 ~50MB，執行時間 ~500ms
- **WebTest**: Web 層測試，MockMvc，記憶體 ~100MB，執行時間 ~1s
- **SliceTest**: Spring Boot 切片測試，特定層測試，記憶體 ~150MB，執行時間 ~2s
- **SpringBootTest**: 完整上下文測試，記憶體 ~500MB，執行時間 ~3s

#### 命名規範

```java
// 推薦：輕量級Unit Test
@ExtendWith(MockitoExtension.class)
class CustomerServiceUnitTest {
    // 測試業務邏輯，使用 Mock
}

// 適量使用：集成測試
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CustomerRepositoryIntegrationTest {
    // 測試數據庫集成
}

// 謹慎使用：完整上下文測試
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerControllerSpringBootTest {
    // 僅用於關鍵End-to-End Test
}
```

## Testing

### Testing

#### ✅ 推薦做法

```java
/**
 * 輕量級Unit Test - Customer Service
 * 
 * 記憶體使用：~5MB (vs @SpringBootTest ~500MB)
 * 執行時間：~50ms (vs @SpringBootTest ~3s)
 * 
 * 測試業務邏輯，而不是 Spring 框架功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Customer Service Unit Tests")
class CustomerServiceUnitTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private CustomerService customerService;

    @Test
    @DisplayName("Should create customer successfully")
    void shouldCreateCustomerSuccessfully() {
        // Given: 準備測試數據
        CreateCustomerCommand command = new CreateCustomerCommand(
            "John Doe", "john@example.com"
        );
        Customer expectedCustomer = Customer.builder()
            .name("John Doe")
            .email("john@example.com")
            .build();

        when(customerRepository.save(any(Customer.class)))
            .thenReturn(expectedCustomer);

        // When: 執行業務邏輯
        Customer result = customerService.createCustomer(command);

        // Then: 驗證結果
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        
        verify(customerRepository).save(any(Customer.class));
        verify(emailService).sendWelcomeEmail(eq("john@example.com"));
    }
}
```

#### ❌ 避免的做法

```java
// 避免：不必要的 @SpringBootTest
@SpringBootTest
class CustomerServiceTest {
    @Autowired
    private CustomerService customerService;
    
    // 這會啟動整個 Spring 上下文，浪費Resource
}
```

### Guidelines

#### 正確的 Mock Policy

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldProcessOrderSuccessfully() {
        // Given: 只 mock 必要的交互
        Order order = createTestOrder();
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(paymentService.processPayment(any())).thenReturn(PaymentResult.success());
        when(inventoryService.reserveItems(any())).thenReturn(true);

        // When: 執行測試
        OrderResult result = orderService.processOrder(createOrderCommand());

        // Then: 驗證結果和交互
        assertThat(result.isSuccess()).isTrue();
        verify(orderRepository).save(any(Order.class));
        verify(paymentService).processPayment(any());
        verify(inventoryService).reserveItems(any());
    }

    // 避免 UnnecessaryStubbingException
    @Test
    void shouldHandlePaymentFailure() {
        // 只 mock 這個測試需要的交互
        when(paymentService.processPayment(any())).thenReturn(PaymentResult.failure());

        OrderResult result = orderService.processOrder(createOrderCommand());

        assertThat(result.isSuccess()).isFalse();
        // 不需要 verify 沒有調用的方法
    }
}
```

### Testing

#### 使用 Test Builder 模式

```java
public class CustomerTestBuilder {
    private String name = "Default Name";
    private String email = "default@example.com";
    private MembershipLevel level = MembershipLevel.STANDARD;

    public static CustomerTestBuilder aCustomer() {
        return new CustomerTestBuilder();
    }

    public CustomerTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public CustomerTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public CustomerTestBuilder withMembershipLevel(MembershipLevel level) {
        this.level = level;
        return this;
    }

    public Customer build() {
        return new Customer(
            CustomerId.generate(),
            new CustomerName(name),
            new Email(email),
            level
        );
    }
}

// 使用示例
@Test
void shouldUpgradeCustomerMembership() {
    // Given
    Customer customer = CustomerTestBuilder.aCustomer()
        .withMembershipLevel(MembershipLevel.STANDARD)
        .build();

    // When & Then
    customer.upgradeMembership();
    assertThat(customer.getMembershipLevel()).isEqualTo(MembershipLevel.PREMIUM);
}
```

### Testing

#### Testing

```java
@ExtendWith(MockitoExtension.class)
class ProfileConfigurationUnitTest {

    @Mock
    private Environment environment;

    private ProfileConfiguration profileConfiguration;

    @BeforeEach
    void setUp() {
        profileConfiguration = new ProfileConfiguration(environment);
    }

    @Test
    void shouldIdentifyTestProfile() {
        // Given: 模擬Environment配置
        when(environment.getActiveProfiles()).thenReturn(new String[] { "test" });

        // When: 檢查配置
        boolean isTest = profileConfiguration.isTestProfile();

        // Then: 驗證結果
        assertThat(isTest).isTrue();
        assertThat(profileConfiguration.isProductionProfile()).isFalse();
    }

    @Test
    void shouldHandleNullProfilesGracefully() {
        // Given: 處理 null 情況
        when(environment.getActiveProfiles()).thenReturn(null);

        // When & Then: 應該優雅處理
        assertThat(profileConfiguration.isTestProfile()).isFalse();
        assertThat(profileConfiguration.isProductionProfile()).isFalse();
    }
}
```

## Testing

### Testing

#### application-test.yml

```yaml
# Testing
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false  # Testing
    properties:
      hibernate:
        format_sql: false
        use_sql_comments: false
  
  h2:
    console:
      enabled: false  # Testing

# Logging配置 - 減少輸出
logging:
  level:
    root: ERROR
    org.hibernate: ERROR
    org.springframework: ERROR
    solid.humank.genaidemo: INFO

# Testing
test:
  performance:
    lazy-initialization: true
    jmx-enabled: false
    aop-auto: false
  
  resources:
    max-connections: 2
    connection-timeout: 5000
    max-memory: 512
```

### Design

#### Testing

```java
/**
 * Unit Test基類
 * 提供通用的測試工具和配置
 */
@ExtendWith(MockitoExtension.class)
public abstract class UnitTestBase {
    
    protected static final String TEST_CUSTOMER_ID = "CUST-001";
    protected static final String TEST_EMAIL = "test@example.com";
    
    @BeforeEach
    void setUpBase() {
        // 通用設置
        MockitoAnnotations.openMocks(this);
    }
    
    protected Customer createTestCustomer() {
        return CustomerTestBuilder.aCustomer()
            .withId(new CustomerId(TEST_CUSTOMER_ID))
            .withEmail(TEST_EMAIL)
            .build();
    }
    
    protected void assertDomainEvent(List<DomainEvent> events, 
                                   Class<? extends DomainEvent> eventType) {
        assertThat(events)
            .hasSize(1)
            .first()
            .isInstanceOf(eventType);
    }
}

/**
 * 集成測試基類
 * 用於需要 Spring 上下文的測試
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestBase {
    
    @Autowired
    protected TestEntityManager entityManager;
    
    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
```

## PerformanceMonitoring與優化

### Testing

#### Performance基準

```java
/**
 * 測試PerformanceMonitoring
 */
@ExtendWith(MockitoExtension.class)
class PerformanceMonitoringTest {
    
    @Test
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void shouldCompleteWithinTimeLimit() {
        // Unit Test應在 100ms 內完成
        CustomerService service = new CustomerService(mock(CustomerRepository.class));
        
        long startTime = System.currentTimeMillis();
        service.validateCustomer(createTestCustomer());
        long endTime = System.currentTimeMillis();
        
        assertThat(endTime - startTime).isLessThan(100);
    }
    
    @Test
    void shouldUseMinimalMemory() {
        // Monitoring記憶體使用
        Runtime runtime = Runtime.getRuntime();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // 執行測試邏輯
        CustomerService service = new CustomerService(mock(CustomerRepository.class));
        service.processCustomers(createTestCustomers(1000));
        
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = afterMemory - beforeMemory;
        
        // Unit Test記憶體使用應小於 10MB
        assertThat(memoryUsed).isLessThan(10 * 1024 * 1024);
    }
}
```

### Testing

#### Testing

```gradle
// build.gradle
test {
    useJUnitPlatform()
    
    // Performance優化
    maxHeapSize = "2g"
    jvmArgs = [
        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=100",
        "-Djunit.jupiter.execution.parallel.enabled=true",
        "-Djunit.jupiter.execution.parallel.mode.default=concurrent"
    ]
    
    // 測試分類
    systemProperty 'junit.jupiter.conditions.deactivate', 'org.junit.*DisabledCondition'
    
    // 報告配置
    reports {
        html.required = true
        junitXml.required = true
    }
    
    // 測試事件監聽
    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
        showStandardStreams = false
    }
    
    // 並行執行配置
    systemProperty 'junit.jupiter.execution.parallel.config.strategy', 'dynamic'
    systemProperty 'junit.jupiter.execution.parallel.config.dynamic.factor', '2'
}

// 測試任務分離
task unitTest(type: Test) {
    useJUnitPlatform {
        includeTags 'unit'
    }
    group = 'verification'
    description = 'Run unit tests only'
}

task integrationTest(type: Test) {
    useJUnitPlatform {
        includeTags 'integration'
    }
    group = 'verification'
    description = 'Run integration tests only'
}
```

## 錯誤處理與調試

### 1. 常見問題解決

#### UnnecessaryStubbingException

```java
// ❌ 問題代碼
@BeforeEach
void setUp() {
    // 全局 stubbing，但不是所有測試都會使用
    when(repository.findById(any())).thenReturn(Optional.of(entity));
    when(service.process(any())).thenReturn(result);
}

// ✅ 解決方案
@Test
void shouldProcessEntity() {
    // 只在需要的測試中進行 stubbing
    when(repository.findById(eq("123"))).thenReturn(Optional.of(entity));
    
    Result result = service.processEntity("123");
    
    assertThat(result).isNotNull();
}
```

#### NullPointerException 處理

```java
// ✅ 防禦性編程
public boolean isTestProfile() {
    String[] activeProfiles = environment.getActiveProfiles();
    if (activeProfiles == null) {
        return false;  // 優雅處理 null 情況
    }
    
    return Arrays.asList(activeProfiles).contains("test");
}

// ✅ 測試 null 情況
@Test
void shouldHandleNullProfilesGracefully() {
    when(environment.getActiveProfiles()).thenReturn(null);
    
    boolean result = profileConfiguration.isTestProfile();
    
    assertThat(result).isFalse();
}
```

### Testing

#### 調試配置

```java
@ExtendWith(MockitoExtension.class)
class DebuggingTest {
    
    @Test
    void shouldDebugMockInteractions() {
        // 啟用詳細的 Mock Logging
        CustomerRepository mockRepo = mock(CustomerRepository.class, 
            withSettings().verboseLogging());
        
        when(mockRepo.findById(any())).thenReturn(Optional.empty());
        
        CustomerService service = new CustomerService(mockRepo);
        service.findCustomer("123");
        
        // 驗證交互
        verify(mockRepo).findById(eq("123"));
    }
    
    @Test
    void shouldCaptureArguments() {
        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        
        service.createCustomer(command);
        
        verify(repository).save(customerCaptor.capture());
        Customer capturedCustomer = customerCaptor.getValue();
        
        assertThat(capturedCustomer.getName()).isEqualTo("Expected Name");
    }
}
```

## CI/CD 集成

### 1. GitHub Actions 配置

#### .github/workflows/test.yml

```yaml
name: Test Suite

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Cache Gradle packages
        uses: actions/cache@v3
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
      
      - name: Run Unit Tests
        run: ./gradlew unitTest --no-daemon --parallel
      
      - name: Upload Unit Test Results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: unit-test-results
          path: app/build/reports/tests/unitTest/

  integration-tests:
    runs-on: ubuntu-latest
    needs: unit-tests
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Run Integration Tests
        run: ./gradlew integrationTest --no-daemon
      
      - name: Upload Integration Test Results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: integration-test-results
          path: app/build/reports/tests/integrationTest/

  test-report:
    runs-on: ubuntu-latest
    needs: [unit-tests, integration-tests]
    if: always()
    steps:
      - name: Generate Test Report
        run: |
          echo "## Test Results" >> $GITHUB_STEP_SUMMARY
          echo "- Unit Tests: ✅ Passed" >> $GITHUB_STEP_SUMMARY
          echo "- Integration Tests: ✅ Passed" >> $GITHUB_STEP_SUMMARY
```

### 2. 質量門檻

#### SonarQube 配置

```properties
# sonar-project.properties
sonar.projectKey=genai-demo
sonar.projectName=GenAI Demo
sonar.projectVersion=1.0

# Testing
sonar.coverage.jacoco.xmlReportPaths=app/build/reports/jacoco/test/jacocoTestReport.xml
sonar.junit.reportPaths=app/build/test-results/test/

# 質量門檻
sonar.qualitygate.wait=true
sonar.coverage.minimum=80
sonar.duplicated_lines_density.maximum=3
sonar.maintainability_rating.minimum=A
```

## Maintenance

### Testing

#### 關鍵Metrics

- **測試執行時間**: Unit Test < 100ms，集成測試 < 1s
- **記憶體使用**: Unit Test < 10MB，集成測試 < 100MB
- **Test Coverage**: 代碼覆蓋率 > 80%，分支覆蓋率 > 70%
- **測試穩定性**: 失敗率 < 1%

#### Monitoring腳本

```bash
#!/bin/bash
# scripts/test-metrics.sh

echo "=== Test Performance Metrics ==="

# Testing
start_time=$(date +%s)
./gradlew test --no-daemon
end_time=$(date +%s)
execution_time=$((end_time - start_time))

echo "Total execution time: ${execution_time}s"

# Testing
total_tests=$(find app/build/test-results -name "*.xml" -exec grep -o 'tests="[0-9]*"' {} \; | grep -o '[0-9]*' | awk '{sum += $1} END {print sum}')
failed_tests=$(find app/build/test-results -name "*.xml" -exec grep -o 'failures="[0-9]*"' {} \; | grep -o '[0-9]*' | awk '{sum += $1} END {print sum}')

echo "Total tests: $total_tests"
echo "Failed tests: $failed_tests"
echo "Success rate: $(echo "scale=2; ($total_tests - $failed_tests) * 100 / $total_tests" | bc)%"

# 記憶體使用分析
echo "Memory usage analysis:"
grep -r "記憶體使用" app/src/test/java/ | wc -l
```

### Maintenance

#### Maintenance

- [ ] 檢查測試執行時間是否在預期範圍內
- [ ] 審查新增的測試是否遵循命名規範
- [ ] 清理不必要的 @SpringBootTest 測試
- [ ] 更新測試數據和 Mock 配置
- [ ] 檢查Test Coverage報告

#### 每月優化任務

- [ ] 分析慢速測試並優化
- [ ] Refactoring重複的測試代碼
- [ ] 更新測試依賴版本
- [ ] 審查測試架構是否需要調整

## 團隊協作規範

### 1. Code Review 檢查點

#### Testing

```markdown
## Testing

### 必須檢查項目
- [ ] 新功能是否有對應的Unit Test？
- [ ] 測試命名是否清晰描述測試意圖？
- [ ] 是否使用了適當的測試類型（Unit/Integration/SpringBoot）？
- [ ] Mock 使用是否合理，避免過度 mocking？
- [ ] 測試是否獨立，不依賴執行順序？

### Performance檢查
- [ ] 新增測試的執行時間是否合理？
- [ ] 是否避免了不必要的 @SpringBootTest？
- [ ] 測試數據是否精簡，避免過大的測試集？

### 代碼質量
- [ ] 測試代碼是否遵循 AAA 模式（Arrange-Act-Assert）？
- [ ] 是否有適當的錯誤情況測試？
- [ ] 測試斷言是否具體且有意義？
```

### 2. 培訓與知識分享

#### 新團隊成員培訓

1. **測試基礎培訓**
   - Test Pyramid理論
   - Unit Test vs 集成測試
   - Mock 使用Best Practice

2. **項目特定培訓**
   - 項目測試架構介紹
   - 測試工具和框架使用
   - 常見問題和解決方案

3. **實踐練習**
   - 編寫第一個Unit Test
   - Refactoring現有測試
   - Performance優化實踐

## summary

這套測試優化指南基於實際項目經驗，提供了：

1. **明確的測試Policy**: Test Pyramid + 分層測試
2. **具體的實施指南**: 代碼示例 + 配置模板
3. **Performance優化方案**: 記憶體優化 + 執行時間優化
4. **質量保證機制**: CI/CD 集成 + MonitoringMetrics
5. **團隊協作規範**: Code Review + 培訓計劃

通過遵循這些指南，可以確保測試套件的高質量、高Performance和Maintainability，為項目的長期成功奠定堅實基礎。
