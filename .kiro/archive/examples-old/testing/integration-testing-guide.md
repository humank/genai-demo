# Integration Testing Guide

## Overview

Integration tests verify that different components of the system work together correctly. They test the interaction between your code and external systems like databases, message queues, and HTTP endpoints.

**Purpose**: Test component integration with partial Spring context and real infrastructure.

**Key Characteristics**:
- **Moderate Speed**: < 500ms per test
- **Moderate Memory**: < 50MB per test
- **Partial Context**: Only load necessary Spring components
- **Real Infrastructure**: Use test databases (H2), test containers, or embedded services

---

## When to Use Integration Tests

### ✅ Perfect For

- **Repository Testing**: Database queries and persistence
- **REST API Testing**: Controller endpoints and request/response handling
- **Message Processing**: Kafka consumers and producers
- **Serialization**: JSON/XML conversion
- **Spring Configuration**: Bean wiring and configuration validation

### ❌ Not Suitable For

- Pure business logic (use Unit Tests)
- Complete user journeys (use E2E Tests)
- External service integration (use Contract Tests or E2E Tests)

---

## Test Base Class

All integration tests should extend `BaseIntegrationTest` for consistent setup and resource management.

### BaseIntegrationTest Features

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({
    UnifiedTestHttpClientConfiguration.class,
    TestProfileConfiguration.class,
    TestPerformanceConfiguration.class
})
public abstract class BaseIntegrationTest {
    
    @Autowired
    protected TestRestTemplate restTemplate;
    
    @LocalServerPort
    protected int port;
    
    protected String baseUrl;
    
    // Automatic setup and cleanup
    // Resource management
    // Utility methods for HTTP requests
}
```

**Benefits**:
- Automatic resource allocation and cleanup
- Consistent test environment setup
- Utility methods for common operations
- Performance monitoring integration
- Memory management

---

## Repository Integration Tests

### Testing with @DataJpaTest

`@DataJpaTest` provides a lightweight Spring context with only JPA components.

```java
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Customer Repository Integration Tests")
class CustomerRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    @DisplayName("Should save and retrieve customer")
    void shouldSaveAndRetrieveCustomer() {
        // Given
        CustomerEntity customer = new CustomerEntity();
        customer.setName("John Doe");
        customer.setEmail("john@example.com");
        customer.setMembershipLevel("BRONZE");
        
        // When
        CustomerEntity saved = customerRepository.save(customer);
        entityManager.flush();
        entityManager.clear(); // Clear persistence context
        
        CustomerEntity retrieved = customerRepository.findById(saved.getId()).orElseThrow();
        
        // Then
        assertThat(retrieved.getId()).isEqualTo(saved.getId());
        assertThat(retrieved.getName()).isEqualTo("John Doe");
        assertThat(retrieved.getEmail()).isEqualTo("john@example.com");
    }
    
    @Test
    @DisplayName("Should find customers by email")
    void shouldFindCustomersByEmail() {
        // Given
        CustomerEntity customer1 = createCustomer("john@example.com");
        CustomerEntity customer2 = createCustomer("jane@example.com");
        
        entityManager.persist(customer1);
        entityManager.persist(customer2);
        entityManager.flush();
        
        // When
        Optional<CustomerEntity> found = customerRepository.findByEmail("john@example.com");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john@example.com");
    }
    
    @Test
    @DisplayName("Should find customers by membership level")
    void shouldFindCustomersByMembershipLevel() {
        // Given
        CustomerEntity silver1 = createCustomer("silver1@example.com", "SILVER");
        CustomerEntity silver2 = createCustomer("silver2@example.com", "SILVER");
        CustomerEntity gold = createCustomer("gold@example.com", "GOLD");
        
        entityManager.persist(silver1);
        entityManager.persist(silver2);
        entityManager.persist(gold);
        entityManager.flush();
        
        // When
        List<CustomerEntity> silverMembers = customerRepository.findByMembershipLevel("SILVER");
        
        // Then
        assertThat(silverMembers).hasSize(2);
        assertThat(silverMembers)
            .extracting(CustomerEntity::getEmail)
            .containsExactlyInAnyOrder("silver1@example.com", "silver2@example.com");
    }
    
    @Test
    @DisplayName("Should update customer information")
    void shouldUpdateCustomerInformation() {
        // Given
        CustomerEntity customer = createCustomer("john@example.com");
        entityManager.persist(customer);
        entityManager.flush();
        entityManager.clear();
        
        // When
        CustomerEntity toUpdate = customerRepository.findById(customer.getId()).orElseThrow();
        toUpdate.setName("John Smith");
        toUpdate.setMembershipLevel("SILVER");
        customerRepository.save(toUpdate);
        entityManager.flush();
        entityManager.clear();
        
        // Then
        CustomerEntity updated = customerRepository.findById(customer.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("John Smith");
        assertThat(updated.getMembershipLevel()).isEqualTo("SILVER");
    }
    
    @Test
    @DisplayName("Should delete customer")
    void shouldDeleteCustomer() {
        // Given
        CustomerEntity customer = createCustomer("john@example.com");
        entityManager.persist(customer);
        entityManager.flush();
        
        // When
        customerRepository.deleteById(customer.getId());
        entityManager.flush();
        
        // Then
        Optional<CustomerEntity> deleted = customerRepository.findById(customer.getId());
        assertThat(deleted).isEmpty();
    }
    
    // Helper methods
    private CustomerEntity createCustomer(String email) {
        return createCustomer(email, "BRONZE");
    }
    
    private CustomerEntity createCustomer(String email, String membershipLevel) {
        CustomerEntity customer = new CustomerEntity();
        customer.setName("Test Customer");
        customer.setEmail(email);
        customer.setMembershipLevel(membershipLevel);
        return customer;
    }
}
```

### Testing Complex Queries

```java
@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Test
    @DisplayName("Should find orders with items using JOIN FETCH")
    void shouldFindOrdersWithItemsUsingJoinFetch() {
        // Given
        OrderEntity order = createOrderWithItems();
        entityManager.persist(order);
        entityManager.flush();
        entityManager.clear();
        
        // When
        Optional<OrderEntity> found = orderRepository.findByIdWithItems(order.getId());
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getItems()).hasSize(2);
        // Verify no N+1 query problem - items are already loaded
        assertThat(found.get().getItems().get(0).getProduct()).isNotNull();
    }
    
    @Test
    @DisplayName("Should find orders by customer with pagination")
    void shouldFindOrdersByCustomerWithPagination() {
        // Given
        String customerId = "CUST-001";
        for (int i = 0; i < 15; i++) {
            OrderEntity order = createOrder(customerId);
            entityManager.persist(order);
        }
        entityManager.flush();
        
        // When
        Pageable pageable = PageRequest.of(0, 10, Sort.by("orderDate").descending());
        Page<OrderEntity> page = orderRepository.findByCustomerId(customerId, pageable);
        
        // Then
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalElements()).isEqualTo(15);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("Should calculate total spending by customer")
    void shouldCalculateTotalSpendingByCustomer() {
        // Given
        String customerId = "CUST-001";
        OrderEntity order1 = createOrder(customerId, new BigDecimal("1000"));
        OrderEntity order2 = createOrder(customerId, new BigDecimal("2000"));
        
        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.flush();
        
        // When
        BigDecimal totalSpending = orderRepository.calculateTotalSpendingByCustomer(customerId);
        
        // Then
        assertThat(totalSpending).isEqualByComparingTo(new BigDecimal("3000"));
    }
    
    private OrderEntity createOrderWithItems() {
        OrderEntity order = new OrderEntity();
        order.setCustomerId("CUST-001");
        order.setStatus("PENDING");
        order.setOrderDate(LocalDateTime.now());
        
        OrderItemEntity item1 = new OrderItemEntity();
        item1.setProductId("PROD-001");
        item1.setQuantity(2);
        item1.setPrice(new BigDecimal("100"));
        item1.setOrder(order);
        
        OrderItemEntity item2 = new OrderItemEntity();
        item2.setProductId("PROD-002");
        item2.setQuantity(1);
        item2.setPrice(new BigDecimal("200"));
        item2.setOrder(order);
        
        order.setItems(List.of(item1, item2));
        return order;
    }
    
    private OrderEntity createOrder(String customerId) {
        return createOrder(customerId, new BigDecimal("1000"));
    }
    
    private OrderEntity createOrder(String customerId, BigDecimal totalAmount) {
        OrderEntity order = new OrderEntity();
        order.setCustomerId(customerId);
        order.setStatus("PENDING");
        order.setTotalAmount(totalAmount);
        order.setOrderDate(LocalDateTime.now());
        return order;
    }
}
```

---

## REST API Integration Tests

### Testing with @WebMvcTest

`@WebMvcTest` provides a lightweight context for testing controllers.

```java
@WebMvcTest(CustomerController.class)
@ActiveProfiles("test")
@DisplayName("Customer Controller Integration Tests")
class CustomerControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CustomerApplicationService customerService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("Should create customer successfully")
    void shouldCreateCustomerSuccessfully() throws Exception {
        // Given
        CreateCustomerRequest request = new CreateCustomerRequest(
            "John Doe",
            "john@example.com",
            "password123"
        );
        
        Customer customer = createCustomer();
        when(customerService.createCustomer(any())).thenReturn(customer);
        
        // When & Then
        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(customer.getId().getValue()))
            .andExpect(jsonPath("$.name").value("John Doe"))
            .andExpect(jsonPath("$.email").value("john@example.com"));
        
        verify(customerService).createCustomer(any(CreateCustomerCommand.class));
    }
    
    @Test
    @DisplayName("Should return 400 when request validation fails")
    void shouldReturn400WhenRequestValidationFails() throws Exception {
        // Given
        CreateCustomerRequest invalidRequest = new CreateCustomerRequest(
            "", // Empty name
            "invalid-email", // Invalid email format
            "123" // Too short password
        );
        
        // When & Then
        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors[*].field").value(hasItems("name", "email", "password")));
        
        verify(customerService, never()).createCustomer(any());
    }
    
    @Test
    @DisplayName("Should get customer by ID")
    void shouldGetCustomerById() throws Exception {
        // Given
        String customerId = "CUST-001";
        Customer customer = createCustomer(customerId);
        when(customerService.findById(customerId)).thenReturn(customer);
        
        // When & Then
        mockMvc.perform(get("/api/v1/customers/{id}", customerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(customerId))
            .andExpect(jsonPath("$.name").value("John Doe"));
    }
    
    @Test
    @DisplayName("Should return 404 when customer not found")
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        // Given
        String customerId = "NON-EXISTENT";
        when(customerService.findById(customerId))
            .thenThrow(new CustomerNotFoundException(customerId));
        
        // When & Then
        mockMvc.perform(get("/api/v1/customers/{id}", customerId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("CUSTOMER_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value(containsString(customerId)));
    }
    
    @Test
    @DisplayName("Should update customer profile")
    void shouldUpdateCustomerProfile() throws Exception {
        // Given
        String customerId = "CUST-001";
        UpdateProfileRequest request = new UpdateProfileRequest(
            "John Smith",
            "john.smith@example.com"
        );
        
        Customer updatedCustomer = createCustomer(customerId, "John Smith");
        when(customerService.updateProfile(eq(customerId), any())).thenReturn(updatedCustomer);
        
        // When & Then
        mockMvc.perform(put("/api/v1/customers/{id}/profile", customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("John Smith"));
    }
    
    private Customer createCustomer() {
        return createCustomer("CUST-001", "John Doe");
    }
    
    private Customer createCustomer(String id) {
        return createCustomer(id, "John Doe");
    }
    
    private Customer createCustomer(String id, String name) {
        return new Customer(
            CustomerId.of(id),
            new CustomerName(name),
            Email.of("john@example.com"),
            MembershipLevel.BRONZE
        );
    }
}
```

### Full Integration Tests with BaseIntegrationTest

```java
@TestPerformanceExtension(maxExecutionTimeMs = 3000, maxMemoryIncreaseMB = 100)
@DisplayName("Customer API Integration Tests")
class CustomerApiIntegrationTest extends BaseIntegrationTest {
    
    @Test
    @DisplayName("Should complete customer registration flow")
    void shouldCompleteCustomerRegistrationFlow() {
        logTestStart("Customer Registration Flow");
        
        // Given
        CreateCustomerRequest request = new CreateCustomerRequest(
            "Jane Doe",
            "jane@example.com",
            "SecurePass123!"
        );
        
        // When - Create customer
        ResponseEntity<CustomerResponse> createResponse = performPost(
            "/api/v1/customers",
            request,
            CustomerResponse.class
        );
        
        // Then - Verify creation
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().name()).isEqualTo("Jane Doe");
        
        String customerId = createResponse.getBody().id();
        
        // When - Retrieve customer
        ResponseEntity<CustomerResponse> getResponse = performGet(
            "/api/v1/customers/" + customerId,
            CustomerResponse.class
        );
        
        // Then - Verify retrieval
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().id()).isEqualTo(customerId);
        
        logTestEnd("Customer Registration Flow");
    }
    
    @Test
    @DisplayName("Should handle concurrent customer creation")
    void shouldHandleConcurrentCustomerCreation() throws InterruptedException {
        // Given
        int numberOfThreads = 10;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        List<String> createdCustomerIds = Collections.synchronizedList(new ArrayList<>());
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        
        // When - Create customers concurrently
        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    CreateCustomerRequest request = new CreateCustomerRequest(
                        "Customer " + index,
                        "customer" + index + "@example.com",
                        "Password123!"
                    );
                    
                    ResponseEntity<CustomerResponse> response = performPost(
                        "/api/v1/customers",
                        request,
                        CustomerResponse.class
                    );
                    
                    if (response.getStatusCode() == HttpStatus.CREATED) {
                        createdCustomerIds.add(response.getBody().id());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        
        // Then - Verify all customers were created
        assertThat(createdCustomerIds).hasSize(numberOfThreads);
        assertThat(createdCustomerIds).doesNotHaveDuplicates();
    }
}
```

---

## Testing JSON Serialization

### Using @JsonTest

```java
@JsonTest
@DisplayName("Customer JSON Serialization Tests")
class CustomerJsonTest {
    
    @Autowired
    private JacksonTester<CustomerResponse> json;
    
    @Test
    @DisplayName("Should serialize customer response correctly")
    void shouldSerializeCustomerResponseCorrectly() throws Exception {
        // Given
        CustomerResponse response = new CustomerResponse(
            "CUST-001",
            "John Doe",
            "john@example.com",
            "BRONZE",
            Instant.now()
        );
        
        // When
        JsonContent<CustomerResponse> result = json.write(response);
        
        // Then
        assertThat(result).hasJsonPathStringValue("$.id");
        assertThat(result).extractingJsonPathStringValue("$.id").isEqualTo("CUST-001");
        assertThat(result).extractingJsonPathStringValue("$.name").isEqualTo("John Doe");
        assertThat(result).extractingJsonPathStringValue("$.email").isEqualTo("john@example.com");
        assertThat(result).extractingJsonPathStringValue("$.membershipLevel").isEqualTo("BRONZE");
    }
    
    @Test
    @DisplayName("Should deserialize customer request correctly")
    void shouldDeserializeCustomerRequestCorrectly() throws Exception {
        // Given
        String jsonContent = """
            {
                "name": "John Doe",
                "email": "john@example.com",
                "password": "SecurePass123!"
            }
            """;
        
        // When
        CreateCustomerRequest request = json.parseObject(jsonContent);
        
        // Then
        assertThat(request.name()).isEqualTo("John Doe");
        assertThat(request.email()).isEqualTo("john@example.com");
        assertThat(request.password()).isEqualTo("SecurePass123!");
    }
}
```

---

## Database Transaction Management

### Testing Transactional Behavior

```java
@DataJpaTest
@ActiveProfiles("test")
class TransactionalBehaviorTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    @DisplayName("Should rollback transaction on exception")
    void shouldRollbackTransactionOnException() {
        // Given
        CustomerEntity customer = createCustomer("john@example.com");
        
        // When & Then
        assertThatThrownBy(() -> {
            customerRepository.save(customer);
            entityManager.flush();
            throw new RuntimeException("Simulated error");
        }).isInstanceOf(RuntimeException.class);
        
        // Verify rollback - customer should not exist
        Optional<CustomerEntity> found = customerRepository.findByEmail("john@example.com");
        assertThat(found).isEmpty();
    }
    
    @Test
    @DisplayName("Should commit transaction on success")
    void shouldCommitTransactionOnSuccess() {
        // Given
        CustomerEntity customer = createCustomer("john@example.com");
        
        // When
        customerRepository.save(customer);
        entityManager.flush();
        entityManager.clear();
        
        // Then - Customer should exist after commit
        Optional<CustomerEntity> found = customerRepository.findByEmail("john@example.com");
        assertThat(found).isPresent();
    }
    
    private CustomerEntity createCustomer(String email) {
        CustomerEntity customer = new CustomerEntity();
        customer.setName("Test Customer");
        customer.setEmail(email);
        customer.setMembershipLevel("BRONZE");
        return customer;
    }
}
```

---

## Test Data Management

### Using Test Fixtures

```java
@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = "/test-data/customers.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/test-data/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class CustomerRepositoryWithFixturesTest {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    @DisplayName("Should find customers from test fixtures")
    void shouldFindCustomersFromTestFixtures() {
        // Given - Data loaded from customers.sql
        
        // When
        List<CustomerEntity> customers = customerRepository.findAll();
        
        // Then
        assertThat(customers).isNotEmpty();
    }
}
```

### Test Data SQL Scripts

```sql
-- test-data/customers.sql
INSERT INTO customers (id, name, email, membership_level, created_at)
VALUES 
    ('CUST-001', 'John Doe', 'john@example.com', 'BRONZE', CURRENT_TIMESTAMP),
    ('CUST-002', 'Jane Smith', 'jane@example.com', 'SILVER', CURRENT_TIMESTAMP),
    ('CUST-003', 'Bob Johnson', 'bob@example.com', 'GOLD', CURRENT_TIMESTAMP);

-- test-data/cleanup.sql
DELETE FROM customers;
```

---

## Performance Considerations

### Optimizing Test Execution

```java
@DataJpaTest
@ActiveProfiles("test")
@TestPerformanceExtension(maxExecutionTimeMs = 500, maxMemoryIncreaseMB = 50)
class OptimizedRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    @DisplayName("Should execute query efficiently")
    void shouldExecuteQueryEfficiently() {
        // Given
        for (int i = 0; i < 100; i++) {
            CustomerEntity customer = createCustomer("customer" + i + "@example.com");
            entityManager.persist(customer);
        }
        entityManager.flush();
        entityManager.clear();
        
        // When
        long startTime = System.currentTimeMillis();
        Page<CustomerEntity> page = customerRepository.findAll(PageRequest.of(0, 10));
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Then
        assertThat(page.getContent()).hasSize(10);
        assertThat(executionTime).isLessThan(100); // Should be fast
    }
    
    private CustomerEntity createCustomer(String email) {
        CustomerEntity customer = new CustomerEntity();
        customer.setName("Test Customer");
        customer.setEmail(email);
        customer.setMembershipLevel("BRONZE");
        return customer;
    }
}
```

---

## Common Patterns and Best Practices

### 1. Clean Test Data Between Tests

```java
@DataJpaTest
@ActiveProfiles("test")
class CleanDataTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @AfterEach
    void cleanUp() {
        customerRepository.deleteAll();
        entityManager.flush();
    }
    
    @Test
    void test1() {
        // Test with clean state
    }
    
    @Test
    void test2() {
        // Test with clean state
    }
}
```

### 2. Use Test Profiles

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### 3. Verify No N+1 Query Problems

```java
@Test
@DisplayName("Should not have N+1 query problem")
void shouldNotHaveNPlusOneQueryProblem() {
    // Given
    OrderEntity order = createOrderWithItems();
    entityManager.persist(order);
    entityManager.flush();
    entityManager.clear();
    
    // When - Use JOIN FETCH to avoid N+1
    Optional<OrderEntity> found = orderRepository.findByIdWithItems(order.getId());
    
    // Then - Items should be loaded without additional queries
    assertThat(found).isPresent();
    assertThat(found.get().getItems()).hasSize(2);
    
    // Access items without triggering additional queries
    found.get().getItems().forEach(item -> {
        assertThat(item.getProduct()).isNotNull();
    });
}
```

---

## Quick Reference

### Test Annotations

```java
// Repository tests
@DataJpaTest
@ActiveProfiles("test")

// Controller tests
@WebMvcTest(ControllerClass.class)
@ActiveProfiles("test")

// JSON serialization tests
@JsonTest

// Full integration tests
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
```

### Common Assertions

```java
// HTTP Status
assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

// JSON Path
mockMvc.perform(get("/api/v1/customers/123"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.id").value("123"))
    .andExpect(jsonPath("$.name").value("John Doe"));

// Database
assertThat(customerRepository.findById(id)).isPresent();
assertThat(customerRepository.count()).isEqualTo(5);
```

---

## Related Documentation

- **Testing Strategy**: #[[file:../../steering/testing-strategy.md]]
- **Unit Testing**: #[[file:unit-testing-guide.md]]
- **BDD Testing**: #[[file:bdd-cucumber-guide.md]]
- **Test Performance**: #[[file:test-performance-guide.md]]

---

**Document Version**: 1.0  
**Last Updated**: 2025-01-22  
**Owner**: Development Team
