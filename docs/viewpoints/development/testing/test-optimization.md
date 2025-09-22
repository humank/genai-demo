# 測試優化指南

## 概述

本指南基於專案的測試優化實踐，提供了一套完整的測試策略和最佳實踐，旨在確保高品質、高效能、可維護的測試套件。

## 🏗️ 測試金字塔策略

### 測試分層原則

```
    /\
   /  \     E2E 測試 (5%)
  /____\    - 完整業務流程測試
 /      \   - 真實環境整合測試
/________\  - 關鍵用戶路徑驗證

   /\
  /  \      整合測試 (15%)
 /____\     - 多組件協作測試
/      \    - 資料庫整合測試
\______/    - 外部服務整合測試

     /\
    /  \    單元測試 (80%)
   /____\   - 業務邏輯測試
  /      \  - 組件隔離測試
 /________\ - 快速反饋測試
```

### 測試分類與效能基準

| 測試類型 | 記憶體使用 | 執行時間 | 成功率 | 使用場景 |
|----------|------------|----------|--------|----------|
| 單元測試 | ~5MB | ~50ms | >99% | 業務邏輯、工具函數 |
| 整合測試 | ~50MB | ~500ms | >95% | 資料庫、API 端點 |
| E2E 測試 | ~500MB | ~3s | >90% | 完整用戶旅程 |

## 🎯 測試優化最佳實踐

### 1. 優先使用輕量級單元測試

#### ✅ 推薦做法：Mock-based 單元測試

```java
/**
 * 輕量級單元測試 - Customer Service
 * 
 * 記憶體使用：~5MB (vs @SpringBootTest ~500MB)
 * 執行時間：~50ms (vs @SpringBootTest ~3s)
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
    void should_create_customer_successfully() {
        // Given: 準備測試資料
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

#### ❌ 避免的做法：不必要的 @SpringBootTest

```java
// 避免：不必要的 @SpringBootTest
@SpringBootTest
class CustomerServiceTest {
    @Autowired
    private CustomerService customerService;
    
    // 這會啟動整個 Spring 上下文，浪費資源
}
```

### 2. 正確的 Mock 使用策略

#### 精確 Mock 原則

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
    void should_process_order_successfully() {
        // Given: 只 mock 必要的互動
        Order order = createTestOrder();
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(paymentService.processPayment(any())).thenReturn(PaymentResult.success());
        when(inventoryService.reserveItems(any())).thenReturn(true);

        // When: 執行測試
        OrderResult result = orderService.processOrder(createOrderCommand());

        // Then: 驗證結果和互動
        assertThat(result.isSuccess()).isTrue();
        verify(orderRepository).save(any(Order.class));
        verify(paymentService).processPayment(any());
        verify(inventoryService).reserveItems(any());
    }

    // 避免 UnnecessaryStubbingException
    @Test
    void should_handle_payment_failure() {
        // 只 mock 這個測試需要的互動
        when(paymentService.processPayment(any())).thenReturn(PaymentResult.failure());

        OrderResult result = orderService.processOrder(createOrderCommand());

        assertThat(result.isSuccess()).isFalse();
        // 不需要 verify 沒有呼叫的方法
    }
}
```

### 3. Test Builder 模式

#### 可維護的測試資料建構

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
void should_upgrade_customer_membership() {
    // Given
    Customer customer = CustomerTestBuilder.aCustomer()
        .withMembershipLevel(MembershipLevel.STANDARD)
        .build();

    // When & Then
    customer.upgradeMembership();
    assertThat(customer.getMembershipLevel()).isEqualTo(MembershipLevel.PREMIUM);
}
```

## 🔧 測試環境配置優化

### 1. 測試 Profile 設置

#### application-test.yml 優化配置

```yaml
# 測試環境配置 - 優化效能和隔離
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false  # 測試環境不顯示 SQL
    properties:
      hibernate:
        format_sql: false
        use_sql_comments: false
  
  h2:
    console:
      enabled: false  # 測試環境停用 H2 控制台

# 日誌配置 - 減少輸出
logging:
  level:
    root: ERROR
    org.hibernate: ERROR
    org.springframework: ERROR
    solid.humank.genaidemo: INFO

# 測試專用配置
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

### 2. 測試基類設計

#### 抽象測試基類

```java
/**
 * 單元測試基類
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
 * 整合測試基類
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

## ⚡ 效能監控與優化

### 1. 測試效能指標

#### 效能基準測試

```java
/**
 * 測試效能監控
 */
@ExtendWith(MockitoExtension.class)
class PerformanceMonitoringTest {
    
    @Test
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void should_complete_within_time_limit() {
        // 單元測試應在 100ms 內完成
        CustomerService service = new CustomerService(mock(CustomerRepository.class));
        
        long startTime = System.currentTimeMillis();
        service.validateCustomer(createTestCustomer());
        long endTime = System.currentTimeMillis();
        
        assertThat(endTime - startTime).isLessThan(100);
    }
    
    @Test
    void should_use_minimal_memory() {
        // 監控記憶體使用
        Runtime runtime = Runtime.getRuntime();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // 執行測試邏輯
        CustomerService service = new CustomerService(mock(CustomerRepository.class));
        service.processCustomers(createTestCustomers(1000));
        
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = afterMemory - beforeMemory;
        
        // 單元測試記憶體使用應小於 10MB
        assertThat(memoryUsed).isLessThan(10 * 1024 * 1024);
    }
}
```

### 2. Gradle 測試任務優化

#### 效能優化配置

```gradle
// build.gradle
test {
    useJUnitPlatform()
    
    // 效能優化
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

## 🐛 錯誤處理與除錯

### 1. 常見問題解決

#### UnnecessaryStubbingException

```java
// ❌ 問題程式碼
@BeforeEach
void setUp() {
    // 全域 stubbing，但不是所有測試都會使用
    when(repository.findById(any())).thenReturn(Optional.of(entity));
    when(service.process(any())).thenReturn(result);
}

// ✅ 解決方案
@Test
void should_process_entity() {
    // 只在需要的測試中進行 stubbing
    when(repository.findById(eq("123"))).thenReturn(Optional.of(entity));
    
    Result result = service.processEntity("123");
    
    assertThat(result).isNotNull();
}
```

#### NullPointerException 處理

```java
// ✅ 防禦性程式設計
public boolean isTestProfile() {
    String[] activeProfiles = environment.getActiveProfiles();
    if (activeProfiles == null) {
        return false;  // 優雅處理 null 情況
    }
    
    return Arrays.asList(activeProfiles).contains("test");
}

// ✅ 測試 null 情況
@Test
void should_handle_null_profiles_gracefully() {
    when(environment.getActiveProfiles()).thenReturn(null);
    
    boolean result = profileConfiguration.isTestProfile();
    
    assertThat(result).isFalse();
}
```

### 2. 測試除錯技巧

#### 除錯配置

```java
@ExtendWith(MockitoExtension.class)
class DebuggingTest {
    
    @Test
    void should_debug_mock_interactions() {
        // 啟用詳細的 Mock 日誌
        CustomerRepository mockRepo = mock(CustomerRepository.class, 
            withSettings().verboseLogging());
        
        when(mockRepo.findById(any())).thenReturn(Optional.empty());
        
        CustomerService service = new CustomerService(mockRepo);
        service.findCustomer("123");
        
        // 驗證互動
        verify(mockRepo).findById(eq("123"));
    }
    
    @Test
    void should_capture_arguments() {
        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        
        service.createCustomer(command);
        
        verify(repository).save(customerCaptor.capture());
        Customer capturedCustomer = customerCaptor.getValue();
        
        assertThat(capturedCustomer.getName()).isEqualTo("Expected Name");
    }
}
```

## 📊 監控與維護

### 1. 測試指標監控

#### 關鍵指標

- **測試執行時間**: 單元測試 < 100ms，整合測試 < 1s
- **記憶體使用**: 單元測試 < 10MB，整合測試 < 100MB
- **測試覆蓋率**: 程式碼覆蓋率 > 80%，分支覆蓋率 > 70%
- **測試穩定性**: 失敗率 < 1%

#### 監控腳本

```bash
#!/bin/bash
# scripts/test-metrics.sh

echo "=== Test Performance Metrics ==="

# 執行測試並記錄時間
start_time=$(date +%s)
./gradlew test --no-daemon
end_time=$(date +%s)
execution_time=$((end_time - start_time))

echo "Total execution time: ${execution_time}s"

# 分析測試結果
total_tests=$(find app/build/test-results -name "*.xml" -exec grep -o 'tests="[0-9]*"' {} \; | grep -o '[0-9]*' | awk '{sum += $1} END {print sum}')
failed_tests=$(find app/build/test-results -name "*.xml" -exec grep -o 'failures="[0-9]*"' {} \; | grep -o '[0-9]*' | awk '{sum += $1} END {print sum}')

echo "Total tests: $total_tests"
echo "Failed tests: $failed_tests"
echo "Success rate: $(echo "scale=2; ($total_tests - $failed_tests) * 100 / $total_tests" | bc)%"
```

### 2. 定期維護任務

#### 每週維護檢查清單

- [ ] 檢查測試執行時間是否在預期範圍內
- [ ] 審查新增的測試是否遵循命名規範
- [ ] 清理不必要的 @SpringBootTest 測試
- [ ] 更新測試資料和 Mock 配置
- [ ] 檢查測試覆蓋率報告

#### 每月優化任務

- [ ] 分析慢速測試並優化
- [ ] 重構重複的測試程式碼
- [ ] 更新測試依賴版本
- [ ] 審查測試架構是否需要調整

## 🤝 團隊協作規範

### 1. Code Review 檢查點

#### 測試相關 PR 檢查

```markdown
## 測試 Code Review Checklist

### 必須檢查項目
- [ ] 新功能是否有對應的單元測試？
- [ ] 測試命名是否清晰描述測試意圖？
- [ ] 是否使用了適當的測試類型（Unit/Integration/SpringBoot）？
- [ ] Mock 使用是否合理，避免過度 mocking？
- [ ] 測試是否獨立，不依賴執行順序？

### 效能檢查
- [ ] 新增測試的執行時間是否合理？
- [ ] 是否避免了不必要的 @SpringBootTest？
- [ ] 測試資料是否精簡，避免過大的測試集？

### 程式碼品質
- [ ] 測試程式碼是否遵循 AAA 模式（Arrange-Act-Assert）？
- [ ] 是否有適當的錯誤情況測試？
- [ ] 測試斷言是否具體且有意義？
```

### 2. 培訓與知識分享

#### 新團隊成員培訓

1. **測試基礎培訓**
   - 測試金字塔理論
   - 單元測試 vs 整合測試
   - Mock 使用最佳實踐

2. **專案特定培訓**
   - 專案測試架構介紹
   - 測試工具和框架使用
   - 常見問題和解決方案

3. **實踐練習**
   - 編寫第一個單元測試
   - 重構現有測試
   - 效能優化實踐

## 📚 相關資源

### 內部文檔
- [測試策略總覽](README.md) - 整體測試策略
- [TDD 實踐指南](tdd-practices/README.md) - 測試驅動開發
- [BDD 實踐指南](bdd-practices/README.md) - 行為驅動開發
- [效能測試指南](performance-monitoring/README.md) - 測試效能監控

### 外部資源
- [Test-Driven Development](https://martinfowler.com/bliki/TestDrivenDevelopment.html)
- [Testing Pyramid](https://martinfowler.com/articles/practical-test-pyramid.html)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)

## 總結

這套測試優化指南提供了：

1. **明確的測試策略**: 測試金字塔 + 分層測試
2. **具體的實施指南**: 程式碼範例 + 配置模板
3. **效能優化方案**: 記憶體優化 + 執行時間優化
4. **品質保證機制**: CI/CD 整合 + 監控指標
5. **團隊協作規範**: Code Review + 培訓計劃

通過遵循這些指南，可以確保測試套件的高品質、高效能和可維護性，為專案的長期成功奠定堅實基礎。

---

**最後更新**: 2025年1月21日  
**維護者**: QA Team & Development Team  
**版本**: 1.0

> 💡 **提示**: 好的測試不僅是品質保證，更是活文檔和設計工具。讓測試引導你的設計，讓設計簡化你的測試。