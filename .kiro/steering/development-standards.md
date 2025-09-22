# Development Standards and Guidelines

## Technology Stack Requirements

### Backend Technologies

- Spring Boot 3.4.5 + Java 21 + Gradle 8.x
- Spring Data JPA + Hibernate + Flyway
- H2 (dev/test) + PostgreSQL (prod)
- SpringDoc OpenAPI 3 + Swagger UI
- Spring Boot Actuator + AWS X-Ray + Micrometer

### Frontend Technologies

- CMC Management: Next.js 14 + React 18 + TypeScript
- Consumer App: Angular 18 + TypeScript
- UI Components: shadcn/ui + Radix UI

### Testing Frameworks

- JUnit 5 + Mockito + AssertJ
- Cucumber 7 (BDD) + Gherkin
- ArchUnit (Architecture Testing)

### Documentation and Diagrams

#### Diagram Format Standards

**PlantUML Diagrams:**
- **Primary Format**: PNG (recommended for GitHub documentation)
  - Better readability and text clarity in GitHub
  - Consistent rendering across different browsers
  - Optimal file size for documentation
- **Secondary Format**: SVG (for high-resolution needs)
  - Use for printing or scalable displays
  - Vector format for infinite zoom capability
- **Generation Command**: `./scripts/generate-diagrams.sh --format=png`
- **Documentation Links**: Always reference PNG files in Markdown

**Mermaid Diagrams:**
- **Native GitHub Support**: Use `.mmd` files or inline code blocks
- **Direct Rendering**: GitHub renders Mermaid diagrams automatically
- **Preferred for**: Process flows, simple architecture diagrams
- **File Extension**: `.mmd` for standalone files

**Diagram Organization:**
```
docs/diagrams/
├── generated/          # Generated PNG/SVG files from PlantUML
├── viewpoints/         # PlantUML source files organized by viewpoint
├── mermaid/           # Mermaid diagram files (.mmd)
└── legacy/            # Legacy diagram files
```

## Error Handling Standards

### Exception Design Patterns

#### Custom Exception Hierarchy

```java
// Base domain exception
public abstract class DomainException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> context;
    
    protected DomainException(String errorCode, String message, Map<String, Object> context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context != null ? context : Map.of();
    }
    
    public String getErrorCode() { return errorCode; }
    public Map<String, Object> getContext() { return context; }
}

// Business rule violation
public class BusinessRuleViolationException extends DomainException {
    public BusinessRuleViolationException(String rule, String message) {
        super("BUSINESS_RULE_VIOLATION", message, Map.of("rule", rule));
    }
}

// Resource not found
public class ResourceNotFoundException extends DomainException {
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super("RESOURCE_NOT_FOUND", 
              String.format("%s with id %s not found", resourceType, resourceId),
              Map.of("resourceType", resourceType, "resourceId", resourceId));
    }
}
```

#### Error Code Standards

- Format: `{DOMAIN}_{ERROR_TYPE}_{SPECIFIC_ERROR}`
- Examples:
  - `CUSTOMER_VALIDATION_INVALID_EMAIL`
  - `ORDER_BUSINESS_RULE_INSUFFICIENT_INVENTORY`
  - `PAYMENT_INTEGRATION_GATEWAY_TIMEOUT`

#### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex) {
        ErrorResponse response = ErrorResponse.builder()
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .context(ex.getContext())
            .timestamp(Instant.now())
            .build();
            
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        // Handle validation errors with field-level details
    }
}
```

### Logging Standards

#### Structured Logging Format

```java
// Use structured logging with consistent fields
log.info("Order processed successfully", 
    kv("orderId", order.getId()),
    kv("customerId", order.getCustomerId()),
    kv("amount", order.getTotalAmount()),
    kv("processingTimeMs", processingTime));

// Error logging with context
log.error("Payment processing failed",
    kv("orderId", orderId),
    kv("paymentMethod", paymentMethod),
    kv("errorCode", ex.getErrorCode()),
    ex);
```

#### Log Levels Usage

- **ERROR**: System errors, exceptions that require immediate attention
- **WARN**: Business rule violations, recoverable errors
- **INFO**: Important business events, API calls, state changes
- **DEBUG**: Detailed execution flow, variable values
- **TRACE**: Very detailed debugging information

## API Design Standards

### REST API Conventions

#### URL Naming Standards

```
GET    /api/v1/customers                    # List customers
GET    /api/v1/customers/{id}               # Get customer by ID
POST   /api/v1/customers                    # Create customer
PUT    /api/v1/customers/{id}               # Update customer (full)
PATCH  /api/v1/customers/{id}               # Update customer (partial)
DELETE /api/v1/customers/{id}               # Delete customer

# Nested resources
GET    /api/v1/customers/{id}/orders        # Get customer's orders
POST   /api/v1/customers/{id}/orders        # Create order for customer

# Actions (non-CRUD operations)
POST   /api/v1/orders/{id}/cancel           # Cancel order
POST   /api/v1/orders/{id}/ship             # Ship order
```

#### HTTP Status Code Standards

- **200 OK**: Successful GET, PUT, PATCH
- **201 Created**: Successful POST
- **204 No Content**: Successful DELETE
- **400 Bad Request**: Validation errors, malformed requests
- **401 Unauthorized**: Authentication required
- **403 Forbidden**: Authorization failed
- **404 Not Found**: Resource not found
- **409 Conflict**: Business rule violation
- **422 Unprocessable Entity**: Semantic validation errors
- **500 Internal Server Error**: System errors

#### Request/Response Format Standards

```java
// Request DTO
public record CreateCustomerRequest(
    @NotBlank String name,
    @Email String email,
    @Valid AddressDto address
) {}

// Response DTO
public record CustomerResponse(
    String id,
    String name,
    String email,
    AddressDto address,
    Instant createdAt,
    Instant updatedAt
) {}

// Error Response
public record ErrorResponse(
    String errorCode,
    String message,
    Map<String, Object> context,
    Instant timestamp,
    List<FieldError> fieldErrors
) {}
```

#### API Versioning Strategy

- Use URL versioning: `/api/v1/`, `/api/v2/`
- Maintain backward compatibility for at least 2 versions
- Deprecation headers for old versions:

  ```
  Deprecation: true
  Sunset: 2024-12-31T23:59:59Z
  Link: </api/v2/customers>; rel="successor-version"
  ```

## Architecture Constraints

### Package Structure Standards

- `domain/{context}/model/` - Aggregate roots, entities, value objects
- `domain/{context}/events/` - Domain events (Records)
- `application/{context}/` - Use case implementations
- `infrastructure/{context}/persistence/` - Persistence adapters

### Layer Dependency Rules

```
interfaces/ → application/ → domain/ ← infrastructure/
```

### Domain Event Design Constraints

- Use immutable Records implementation
- Aggregate roots collect events, application services publish events
- Event handlers in infrastructure layer

## Testing Standards

### Test Layer Requirements (Test Pyramid)

- Unit Tests (80%): < 50ms, < 5MB
- Integration Tests (15%): < 500ms, < 50MB  
- E2E Tests (5%): < 3s, < 500MB

### Test Classification Standards

#### Unit Tests (Preferred)

- **Annotation**: `@ExtendWith(MockitoExtension.class)`
- **Applicable**: Pure business logic, utilities, configuration classes
- **Prohibited**: Spring context
- **When to Use**:
  - Testing domain logic in isolation
  - Validating business rules
  - Testing utility functions
  - Verifying calculations and transformations

#### Integration Tests (Use Cautiously)

- **Annotation**: `@DataJpaTest`, `@WebMvcTest`, `@JsonTest`
- **Applicable**: Database integration, external services
- **Requirement**: Partial Spring context
- **When to Use**:
  - Testing repository implementations
  - Validating database queries
  - Testing API endpoints
  - Verifying serialization/deserialization

#### E2E Tests (Minimal Use)

- **Annotation**: `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- **Applicable**: Complete business process verification
- **Requirement**: Full Spring context
- **When to Use**:
  - Testing complete user journeys
  - Validating system integration
  - Smoke testing critical paths

### Test Scenario Classification

#### Domain Logic Tests (Unit)

```java
@ExtendWith(MockitoExtension.class)
class CustomerUnitTest {
    
    @Test
    void should_throw_exception_when_email_is_invalid() {
        // Test business rule validation
        assertThatThrownBy(() -> new Customer("John", "invalid-email"))
            .isInstanceOf(InvalidEmailException.class)
            .hasMessage("Email format is invalid");
    }
    
    @Test
    void should_calculate_discount_correctly_for_premium_customer() {
        // Test business calculation
        Customer customer = createPremiumCustomer();
        Order order = createOrder(100.0);
        
        BigDecimal discount = customer.calculateDiscount(order);
        
        assertThat(discount).isEqualTo(new BigDecimal("10.00"));
    }
}
```

#### Repository Tests (Integration)

```java
@DataJpaTest
@ActiveProfiles("test")
class CustomerRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CustomerRepository repository;
    
    @Test
    void should_find_customers_by_email_domain() {
        // Given
        Customer customer1 = createCustomer("john@company.com");
        Customer customer2 = createCustomer("jane@company.com");
        Customer customer3 = createCustomer("bob@other.com");
        
        entityManager.persistAndFlush(customer1);
        entityManager.persistAndFlush(customer2);
        entityManager.persistAndFlush(customer3);
        
        // When
        List<Customer> results = repository.findByEmailDomain("company.com");
        
        // Then
        assertThat(results).hasSize(2)
            .extracting(Customer::getEmail)
            .containsExactlyInAnyOrder("john@company.com", "jane@company.com");
    }
}
```

#### API Tests (Integration)

```java
@WebMvcTest(CustomerController.class)
class CustomerControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CustomerService customerService;
    
    @Test
    void should_return_customer_when_valid_id_provided() throws Exception {
        // Given
        Customer customer = createCustomer();
        when(customerService.findById("123")).thenReturn(customer);
        
        // When & Then
        mockMvc.perform(get("/api/v1/customers/123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("123"))
            .andExpect(jsonPath("$.name").value("John Doe"));
    }
}
```

### Mock Strategy Guidelines

#### When to Mock

- External services (payment gateways, email services)
- Repositories in service tests
- Time-dependent operations
- Complex dependencies that are tested separately

#### When NOT to Mock

- Value objects and entities
- Simple data structures
- Domain logic being tested
- Infrastructure that can be easily replaced (in-memory implementations)

#### Mock Best Practices

```java
// ✅ Good: Specific, focused mocking
@Test
void should_send_welcome_email_when_customer_created() {
    // Given
    Customer customer = createCustomer();
    when(emailService.sendWelcomeEmail(customer.getEmail()))
        .thenReturn(EmailResult.success());
    
    // When
    customerService.createCustomer(customer);
    
    // Then
    verify(emailService).sendWelcomeEmail(customer.getEmail());
}

// ❌ Bad: Over-mocking, testing implementation details
@Test
void should_create_customer() {
    when(customerRepository.save(any())).thenReturn(customer);
    when(eventPublisher.publish(any())).thenReturn(true);
    when(validator.validate(any())).thenReturn(ValidationResult.valid());
    // ... too many mocks
}
```

### Test Data Management

#### Test Data Builders

```java
public class CustomerTestDataBuilder {
    private String name = "John Doe";
    private String email = "john@example.com";
    private CustomerType type = CustomerType.REGULAR;
    
    public static CustomerTestDataBuilder aCustomer() {
        return new CustomerTestDataBuilder();
    }
    
    public CustomerTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public CustomerTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }
    
    public CustomerTestDataBuilder premium() {
        this.type = CustomerType.PREMIUM;
        return this;
    }
    
    public Customer build() {
        return new Customer(name, email, type);
    }
}

// Usage
Customer customer = aCustomer()
    .withName("Jane Smith")
    .withEmail("jane@example.com")
    .premium()
    .build();
```

#### Test Database Management

```java
@TestConfiguration
public class TestDatabaseConfiguration {
    
    @Bean
    @Primary
    public DataSource testDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .addScript("classpath:schema-test.sql")
            .addScript("classpath:test-data.sql")
            .build();
    }
}
```

### Test Tagging System

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("unit")
public @interface UnitTest {}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("integration")
public @interface IntegrationTest {}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("slow")
public @interface SlowTest {}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("smoke")
public @interface SmokeTest {}
```

### Performance Benchmark Requirements

- Unit tests: < 50ms, < 5MB, success rate > 99%
- Integration tests: < 500ms, < 50MB, success rate > 95%
- End-to-end tests: < 3s, < 500MB, success rate > 90%

### Test Performance Monitoring

#### Performance Extensions and Monitoring

```java
// Use performance monitoring annotation for integration tests
@TestPerformanceExtension(maxExecutionTimeMs = 10000, maxMemoryIncreaseMB = 100)
@IntegrationTest
public class SimpleEndToEndValidationTest extends BaseIntegrationTest {
    // Test methods will be automatically monitored for performance
    // Generates reports in build/reports/test-performance/
}

// For more complex integration tests
@TestPerformanceExtension(maxExecutionTimeMs = 30000, maxMemoryIncreaseMB = 200)
public class ComplexIntegrationTest extends BaseIntegrationTest {
    // Performance monitoring with higher thresholds
    // Includes regression detection and slow test identification
}
```

**Performance Monitoring Features:**

- Automatic test execution time tracking with millisecond precision
- Memory usage monitoring (heap memory before/after each test)
- Performance regression detection with configurable thresholds
- Detailed report generation (text-based and HTML via separate generator)
- Slow test identification (>5s warning, >30s error)
- Resource cleanup and memory management

#### Test Resource Management

```java
// BaseIntegrationTest provides resource management utilities
public abstract class BaseIntegrationTest {
    
    protected void forceResourceCleanup() {
        // Force cleanup of test resources
    }
    
    protected boolean isMemoryUsageAcceptable() {
        // Check if memory usage is within acceptable limits
    }
    
    protected void waitForCondition(BooleanSupplier condition, Duration timeout, String description) {
        // Wait for asynchronous operations to complete
    }
}

### Test Environment Isolation

#### Advanced Test Configuration

```java
// Use TestPerformanceConfiguration for performance-focused tests
@TestConfiguration
@Profile("test")
public class TestPerformanceConfiguration {
    
    @Bean
    public TestPerformanceListener testPerformanceListener() {
        return new TestPerformanceListener();
    }
}

// Unified HTTP client configuration for consistent test behavior
@TestConfiguration
@Profile("test")
public class UnifiedTestHttpClientConfiguration {
    
    @Bean
    @Primary
    public TestRestTemplate testRestTemplate() {
        return new TestRestTemplate();
    }
}
```

#### Test Resource Management

```java
// TestPerformanceResourceManager for monitoring test resource usage
@TestComponent
public class TestPerformanceResourceManager {
    
    public ResourceUsageStats getResourceUsageStats() {
        // Get current memory and resource usage statistics
        // Returns: memory usage %, current/max memory, active resources
    }
    
    public void forceCleanup() {
        // Force cleanup of all test resources
        // Triggers System.gc() and logs cleanup completion
    }
}
```

**Resource Management Features:**

- Real-time memory usage monitoring with percentage calculations
- Automatic resource cleanup between tests
- Database cleanup with foreign key constraint handling
- Cache clearing and mock reset functionality
- Temporary resource cleanup and application state reset

#### Database Isolation

```java
@Transactional
@Rollback
public abstract class DatabaseTestBase {
    
    @BeforeEach
    void setUp() {
        // Clean database state
        cleanDatabase();
        // Set up test data
        setupTestData();
    }
    
    @AfterEach
    void tearDown() {
        // Cleanup is automatic with @Rollback
    }
}
```

#### External Service Isolation

```java
@TestConfiguration
public class TestExternalServiceConfiguration {
    
    @Bean
    @Primary
    public PaymentService mockPaymentService() {
        return Mockito.mock(PaymentService.class);
    }
    
    @Bean
    @Primary
    public EmailService inMemoryEmailService() {
        return new InMemoryEmailService();
    }
}

## Test Task Organization

### Gradle Test Tasks

```bash
# Daily development - fast feedback
./gradlew quickTest              # Unit tests only (< 2 minutes)

# Pre-commit verification
./gradlew preCommitTest          # Unit + Integration tests (< 5 minutes)

# Pre-release verification
./gradlew fullTest               # All test types including E2E and Cucumber

# Specific test types
./gradlew unitTest               # Fast unit tests (~5MB, ~50ms each)
./gradlew integrationTest        # Integration tests (~50MB, ~500ms each)
./gradlew e2eTest               # End-to-end tests (~500MB, ~3s each)
./gradlew cucumber              # BDD Cucumber tests

# Performance and reporting
./gradlew generatePerformanceReport  # Generate test performance reports
./gradlew runAllTestsWithReport     # Run all tests + generate reports
```

**Test Task Configuration Features:**

- **Memory Optimization**: Graduated memory allocation (2g → 6g → 8g)
- **JVM Tuning**: G1GC, string deduplication, optimized heap regions
- **HttpComponents Optimization**: Specialized JVM parameters for HTTP client tests
- **Timeout Management**: Progressive timeout configuration (2m → 30m → 1h)
- **Resource Management**: Automatic cleanup and memory monitoring
- **Performance Monitoring**: Integrated with TestPerformanceExtension

### Test Performance Reporting

```bash
# Generate performance reports
./gradlew generatePerformanceReport

# Run all tests with performance monitoring
./gradlew runAllTestsWithReport

# View generated reports
open build/reports/test-performance/performance-report.html
open build/reports/test-performance/overall-performance-summary.txt
```

**Available Reports:**

- **Individual Class Reports**: Detailed test execution analysis per test class
- **Overall Performance Summary**: Aggregated statistics and performance trends
- **HTML Reports**: Interactive charts and visual performance analysis
- **CSV Data**: Raw performance data for custom analysis
- **Slow Test Analysis**: Top 5 slowest tests and performance regression detection

## BDD Development Process

### Mandatory Steps

1. Write Gherkin scenarios (`src/test/resources/features/`)
2. Implement step definitions (Red)
3. TDD implement domain logic (Green)
4. Refactor optimization (Refactor)

## Code Standards

### Naming Conventions

```java
// Aggregate root
@AggregateRoot
public class Customer implements AggregateRootInterface { }

// Value object
@ValueObject
public record CustomerId(String value) { }

// Domain event
public record CustomerCreatedEvent(...) implements DomainEvent { }

// Test class
@ExtendWith(MockitoExtension.class)
class CustomerServiceUnitTest { }

// Test performance utilities (to avoid naming conflicts)
@TestComponent
public class TestPerformanceResourceManager { }

public class TestPerformanceMonitor implements BeforeAllCallback { }

@TestConfiguration
public class TestPerformanceConfiguration { }
```

**Test Utility Naming Standards:**

- Use `TestPerformance*` prefix for performance-related test utilities
- Use `Test*` prefix for general test utilities  
- Avoid generic names like `ResourceManager` or `Monitor` in test packages
- Include descriptive suffixes: `Manager`, `Monitor`, `Configuration`, `Extension`

### Mock Usage Rules

- Only mock interactions actually used in tests
- Avoid global stubbing
- Handle null cases

## ArchUnit Rules

### Mandatory Architecture Rules

- Layer dependency checks
- DDD tactical pattern verification
- Package naming convention checks

### Prohibited Anti-patterns

```java
// ❌ Wrong: Configuration class tests don't need full Spring context
@SpringBootTest
class DatabaseConfigurationTest { ... }

// ✅ Correct: Use unit tests
@ExtendWith(MockitoExtension.class)
class DatabaseConfigurationUnitTest { ... }
```

## Quality Standards

### Must-Achieve Metrics

- Code coverage > 80%
- Test execution time < 15s (unit tests)
- Test failure rate < 1%
- Architecture compliance 100%

### BDD Scenario Coverage Requirements

- Core business processes 100% coverage
- Exception handling scenario coverage
- User experience critical path coverage

## Development Workflow

### New Feature Development Sequence

1. BDD scenario design
2. Domain modeling (DDD)
3. TDD implementation
4. Integration testing
5. ArchUnit verification

### Daily Development Commands

```bash
./gradlew quickTest              # Development quick feedback (2s)
./gradlew unitTest               # Pre-commit full verification (11s)
./gradlew integrationTest        # PR integration test check
./gradlew test                   # Pre-release full test
```
