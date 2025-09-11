# Test Optimization and Maintenance Guidelines

## Overview

This guide is based on the test optimization practices of the GenAI Demo project, providing a complete set of testing strategies and best practices aimed at ensuring high-quality, high-performance, and maintainable test suites.

## Testing Architecture Principles

### 1. Test Pyramid Strategy

```text
    /\
   /  \     E2E Tests (Few)
  /____\    - Complete business process testing
 /      \   - Real environment integration testing
/________\  - Critical user path validation

   /\
  /  \      Integration Tests (Some)
 /____\     - Multi-component collaboration testing
/      \    - Database integration testing
\______/    - External service integration testing

     /\
    /  \    Unit Tests (Many)
   /____\   - Business logic testing
  /      \  - Component isolation testing
 /________\ - Fast feedback testing
```

### 2. Test Classification and Naming Conventions

#### Test Type Classification

- **UnitTest**: Pure unit tests, using Mock, memory ~5MB, execution time ~50ms
- **IntegrationTest**: Integration tests, partial real dependencies, memory ~50MB, execution time ~500ms
- **WebTest**: Web layer tests, MockMvc, memory ~100MB, execution time ~1s
- **SliceTest**: Spring Boot slice tests, specific layer tests, memory ~150MB, execution time ~2s
- **SpringBootTest**: Full context tests, memory ~500MB, execution time ~3s

#### Naming Conventions

```java
// Recommended: Lightweight unit tests
@ExtendWith(MockitoExtension.class)
class CustomerServiceUnitTest {
    // Test business logic, using Mock
}

// Moderate use: Integration tests
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CustomerRepositoryIntegrationTest {
    // Test database integration
}

// Use with caution: Full context tests
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerControllerSpringBootTest {
    // Only for critical end-to-end tests
}
```

## Test Optimization Best Practices

### 1. Prioritize Lightweight Unit Tests

#### ✅ Recommended Approach

```java
/**
 * Lightweight Unit Test - Customer Service
 * 
 * Memory usage: ~5MB (vs @SpringBootTest ~500MB)
 * Execution time: ~50ms (vs @SpringBootTest ~3s)
 * 
 * Test business logic, not Spring framework functionality
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
        // Given: Prepare test data
        CreateCustomerCommand command = new CreateCustomerCommand(
            "John Doe", "john@example.com"
        );
        Customer expectedCustomer = Customer.builder()
            .name("John Doe")
            .email("john@example.com")
            .build();

        when(customerRepository.save(any(Customer.class)))
            .thenReturn(expectedCustomer);

        // When: Execute business logic
        Customer result = customerService.createCustomer(command);

        // Then: Verify results
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        
        verify(customerRepository).save(any(Customer.class));
        verify(emailService).sendWelcomeEmail(eq("john@example.com"));
    }
}
```

#### ❌ Avoid This Approach

```java
// Avoid: Unnecessary @SpringBootTest
@SpringBootTest
class CustomerServiceTest {
    @Autowired
    private CustomerService customerService;
    
    // This starts the entire Spring context, wasting resources
}
```

### 2. Mock Usage Guidelines

#### Correct Mock Strategy

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
        // Given: Only mock necessary interactions
        Order order = createTestOrder();
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(paymentService.processPayment(any())).thenReturn(PaymentResult.success());
        when(inventoryService.reserveItems(any())).thenReturn(true);

        // When: Execute test
        OrderResult result = orderService.processOrder(createOrderCommand());

        // Then: Verify results and interactions
        assertThat(result.isSuccess()).isTrue();
        verify(orderRepository).save(any(Order.class));
        verify(paymentService).processPayment(any());
        verify(inventoryService).reserveItems(any());
    }

    // Avoid UnnecessaryStubbingException
    @Test
    void shouldHandlePaymentFailure() {
        // Only mock interactions needed for this test
        when(paymentService.processPayment(any())).thenReturn(PaymentResult.failure());

        OrderResult result = orderService.processOrder(createOrderCommand());

        assertThat(result.isSuccess()).isFalse();
        // No need to verify methods that weren't called
    }
}
```

### 3. Test Data Management

#### Using Test Builder Pattern

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

// Usage example
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

### 4. Configuration Test Optimization

#### Profile Configuration Testing

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
        // Given: Mock environment configuration
        when(environment.getActiveProfiles()).thenReturn(new String[] { "test" });

        // When: Check configuration
        boolean isTest = profileConfiguration.isTestProfile();

        // Then: Verify results
        assertThat(isTest).isTrue();
        assertThat(profileConfiguration.isProductionProfile()).isFalse();
    }

    @Test
    void shouldHandleNullProfilesGracefully() {
        // Given: Handle null case
        when(environment.getActiveProfiles()).thenReturn(null);

        // When & Then: Should handle gracefully
        assertThat(profileConfiguration.isTestProfile()).isFalse();
        assertThat(profileConfiguration.isProductionProfile()).isFalse();
    }
}
```

## Test Environment Configuration

### 1. Test Profile Setup

#### application-test.yml

```yaml
# Test environment configuration - Performance and isolation optimization
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false  # Don't show SQL in test environment
    properties:
      hibernate:
        format_sql: false
        use_sql_comments: false
  
  h2:
    console:
      enabled: false  # Disable H2 console in test environment

# Logging configuration - Reduce output
logging:
  level:
    root: ERROR
    org.hibernate: ERROR
    org.springframework: ERROR
    solid.humank.genaidemo: INFO

# Test-specific configuration
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

### 2. Test Base Class Design

#### Abstract Test Base Classes

```java
/**
 * Unit test base class
 * Provides common test tools and configuration
 */
@ExtendWith(MockitoExtension.class)
public abstract class UnitTestBase {
    
    protected static final String TEST_CUSTOMER_ID = "CUST-001";
    protected static final String TEST_EMAIL = "test@example.com";
    
    @BeforeEach
    void setUpBase() {
        // Common setup
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
 * Integration test base class
 * For tests that need Spring context
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

## Performance Monitoring and Optimization

### 1. Test Performance Metrics

#### Performance Benchmarks

```java
/**
 * Test performance monitoring
 */
@ExtendWith(MockitoExtension.class)
class PerformanceMonitoringTest {
    
    @Test
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void shouldCompleteWithinTimeLimit() {
        // Unit tests should complete within 100ms
        CustomerService service = new CustomerService(mock(CustomerRepository.class));
        
        long startTime = System.currentTimeMillis();
        service.validateCustomer(createTestCustomer());
        long endTime = System.currentTimeMillis();
        
        assertThat(endTime - startTime).isLessThan(100);
    }
    
    @Test
    void shouldUseMinimalMemory() {
        // Monitor memory usage
        Runtime runtime = Runtime.getRuntime();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Execute test logic
        CustomerService service = new CustomerService(mock(CustomerRepository.class));
        service.processCustomers(createTestCustomers(1000));
        
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = afterMemory - beforeMemory;
        
        // Unit test memory usage should be less than 10MB
        assertThat(memoryUsed).isLessThan(10 * 1024 * 1024);
    }
}
```

### 2. Test Execution Optimization

#### Gradle Test Configuration

```gradle
// build.gradle
test {
    useJUnitPlatform()
    
    // Performance optimization
    maxHeapSize = "2g"
    jvmArgs = [
        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=100",
        "-Djunit.jupiter.execution.parallel.enabled=true",
        "-Djunit.jupiter.execution.parallel.mode.default=concurrent"
    ]
    
    // Test classification
    systemProperty 'junit.jupiter.conditions.deactivate', 'org.junit.*DisabledCondition'
    
    // Report configuration
    reports {
        html.required = true
        junitXml.required = true
    }
    
    // Test event listening
    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
        showStandardStreams = false
    }
    
    // Parallel execution configuration
    systemProperty 'junit.jupiter.execution.parallel.config.strategy', 'dynamic'
    systemProperty 'junit.jupiter.execution.parallel.config.dynamic.factor', '2'
}

// Test task separation
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

## Error Handling and Debugging

### 1. Common Problem Solutions

#### UnnecessaryStubbingException

```java
// ❌ Problem code
@BeforeEach
void setUp() {
    // Global stubbing, but not all tests will use it
    when(repository.findById(any())).thenReturn(Optional.of(entity));
    when(service.process(any())).thenReturn(result);
}

// ✅ Solution
@Test
void shouldProcessEntity() {
    // Only stub in tests that need it
    when(repository.findById(eq("123"))).thenReturn(Optional.of(entity));
    
    Result result = service.processEntity("123");
    
    assertThat(result).isNotNull();
}
```

#### NullPointerException Handling

```java
// ✅ Defensive programming
public boolean isTestProfile() {
    String[] activeProfiles = environment.getActiveProfiles();
    if (activeProfiles == null) {
        return false;  // Gracefully handle null case
    }
    
    return Arrays.asList(activeProfiles).contains("test");
}

// ✅ Test null case
@Test
void shouldHandleNullProfilesGracefully() {
    when(environment.getActiveProfiles()).thenReturn(null);
    
    boolean result = profileConfiguration.isTestProfile();
    
    assertThat(result).isFalse();
}
```

### 2. Test Debugging Techniques

#### Debugging Configuration

```java
@ExtendWith(MockitoExtension.class)
class DebuggingTest {
    
    @Test
    void shouldDebugMockInteractions() {
        // Enable verbose Mock logging
        CustomerRepository mockRepo = mock(CustomerRepository.class, 
            withSettings().verboseLogging());
        
        when(mockRepo.findById(any())).thenReturn(Optional.empty());
        
        CustomerService service = new CustomerService(mockRepo);
        service.findCustomer("123");
        
        // Verify interactions
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

## CI/CD Integration

### 1. GitHub Actions Configuration

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

### 2. Quality Gates

#### SonarQube Configuration

```properties
# sonar-project.properties
sonar.projectKey=genai-demo
sonar.projectName=GenAI Demo
sonar.projectVersion=1.0

# Test coverage requirements
sonar.coverage.jacoco.xmlReportPaths=app/build/reports/jacoco/test/jacocoTestReport.xml
sonar.junit.reportPaths=app/build/test-results/test/

# Quality gates
sonar.qualitygate.wait=true
sonar.coverage.minimum=80
sonar.duplicated_lines_density.maximum=3
sonar.maintainability_rating.minimum=A
```

## Monitoring and Maintenance

### 1. Test Metrics Monitoring

#### Key Metrics

- **Test execution time**: Unit tests < 100ms, integration tests < 1s
- **Memory usage**: Unit tests < 10MB, integration tests < 100MB
- **Test coverage**: Code coverage > 80%, branch coverage > 70%
- **Test stability**: Failure rate < 1%

#### Monitoring Script

```bash
#!/bin/bash
# scripts/test-metrics.sh

echo "=== Test Performance Metrics ==="

# Execute tests and record time
start_time=$(date +%s)
./gradlew test --no-daemon
end_time=$(date +%s)
execution_time=$((end_time - start_time))

echo "Total execution time: ${execution_time}s"

# Analyze test results
total_tests=$(find app/build/test-results -name "*.xml" -exec grep -o 'tests="[0-9]*"' {} \; | grep -o '[0-9]*' | awk '{sum += $1} END {print sum}')
failed_tests=$(find app/build/test-results -name "*.xml" -exec grep -o 'failures="[0-9]*"' {} \; | grep -o '[0-9]*' | awk '{sum += $1} END {print sum}')

echo "Total tests: $total_tests"
echo "Failed tests: $failed_tests"
echo "Success rate: $(echo "scale=2; ($total_tests - $failed_tests) * 100 / $total_tests" | bc)%"

# Memory usage analysis
echo "Memory usage analysis:"
grep -r "Memory usage" app/src/test/java/ | wc -l
```

### 2. Regular Maintenance Tasks

#### Weekly Maintenance Checklist

- [ ] Check if test execution times are within expected ranges
- [ ] Review new tests for naming convention compliance
- [ ] Clean up unnecessary @SpringBootTest tests
- [ ] Update test data and Mock configurations
- [ ] Check test coverage reports

#### Monthly Optimization Tasks

- [ ] Analyze slow tests and optimize
- [ ] Refactor duplicate test code
- [ ] Update test dependency versions
- [ ] Review if test architecture needs adjustment

## Team Collaboration Standards

### 1. Code Review Checkpoints

#### Test-related PR Checklist

```markdown
## Test Code Review Checklist

### Must Check Items
- [ ] Do new features have corresponding unit tests?
- [ ] Do test names clearly describe test intent?
- [ ] Are appropriate test types used (Unit/Integration/SpringBoot)?
- [ ] Is Mock usage reasonable, avoiding over-mocking?
- [ ] Are tests independent, not dependent on execution order?

### Performance Checks
- [ ] Are new test execution times reasonable?
- [ ] Are unnecessary @SpringBootTest avoided?
- [ ] Is test data concise, avoiding oversized test sets?

### Code Quality
- [ ] Do test codes follow AAA pattern (Arrange-Act-Assert)?
- [ ] Are there appropriate error case tests?
- [ ] Are test assertions specific and meaningful?
```

### 2. Training and Knowledge Sharing

#### New Team Member Training

1. **Testing Fundamentals Training**
   - Test pyramid theory
   - Unit tests vs integration tests
   - Mock usage best practices

2. **Project-specific Training**
   - Project test architecture introduction
   - Testing tools and framework usage
   - Common problems and solutions

3. **Practical Exercises**
   - Writing first unit test
   - Refactoring existing tests
   - Performance optimization practice

## Summary

This test optimization guide is based on actual project experience and provides:

1. **Clear testing strategy**: Test pyramid + layered testing
2. **Specific implementation guidelines**: Code examples + configuration templates
3. **Performance optimization solutions**: Memory optimization + execution time optimization
4. **Quality assurance mechanisms**: CI/CD integration + monitoring metrics
5. **Team collaboration standards**: Code Review + training plans

By following these guidelines, you can ensure high-quality, high-performance, and maintainable test suites, laying a solid foundation for the project's long-term success.
