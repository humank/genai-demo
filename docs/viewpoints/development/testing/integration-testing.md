# Integration Testing Guide

> **Status**: ✅ Active  
> **Last Updated**: 2024-11-19

## Overview

This guide provides comprehensive guidance for writing integration tests in the GenAI Demo project.

---

## Quick Reference

For complete testing guidance, see:
- [Testing Strategy](testing-strategy.md) - Complete testing approach
- [Unit Testing](unit-testing.md) - Unit testing guide
- [Test Pyramid](test-pyramid.md) - Test distribution strategy

---

## What are Integration Tests?

Integration tests verify that different components of the system work together correctly. They test:
- Database interactions
- API endpoints
- Message queue integration
- External service integration
- Component interactions

---

## Integration Test Types

### 1. Repository Integration Tests

Test database operations with real database.

```java
@DataJpaTest
@ActiveProfiles("test")
class CustomerRepositoryIntegrationTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    void should_find_customers_by_email_domain() {
        // Given
        Customer customer1 = createCustomer("john@company.com");
        Customer customer2 = createCustomer("jane@company.com");
        Customer customer3 = createCustomer("bob@other.com");
        
        entityManager.persistAndFlush(customer1);
        entityManager.persistAndFlush(customer2);
        entityManager.persistAndFlush(customer3);
        entityManager.clear();
        
        // When
        List<Customer> results = customerRepository.findByEmailDomain("company.com");
        
        // Then
        assertThat(results).hasSize(2)
            .extracting(Customer::getEmail)
            .containsExactlyInAnyOrder("john@company.com", "jane@company.com");
    }
    
    @Test
    void should_handle_optimistic_locking() {
        // Given
        Customer customer = createCustomer("test@example.com");
        entityManager.persistAndFlush(customer);
        entityManager.clear();
        
        // When: Two concurrent updates
        Customer customer1 = customerRepository.findById(customer.getId()).orElseThrow();
        Customer customer2 = customerRepository.findById(customer.getId()).orElseThrow();
        
        customer1.updateName("Name 1");
        customerRepository.saveAndFlush(customer1);
        
        customer2.updateName("Name 2");
        
        // Then: Second update should fail
        assertThatThrownBy(() -> customerRepository.saveAndFlush(customer2))
            .isInstanceOf(OptimisticLockingFailureException.class);
    }
}
```

### 2. API Integration Tests

Test REST endpoints with Spring MockMvc.

```java
@WebMvcTest(CustomerController.class)
@ActiveProfiles("test")
class CustomerControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CustomerService customerService;
    
    @Test
    void should_create_customer_with_valid_data() throws Exception {
        // Given
        CreateCustomerRequest request = new CreateCustomerRequest(
            "John Doe",
            "john@example.com",
            "password123"
        );
        
        Customer expectedCustomer = Customer.builder()
            .id("CUST-001")
            .name("John Doe")
            .email("john@example.com")
            .build();
        
        when(customerService.createCustomer(any())).thenReturn(expectedCustomer);
        
        // When & Then
        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("CUST-001"))
            .andExpect(jsonPath("$.name").value("John Doe"))
            .andExpect(jsonPath("$.email").value("john@example.com"));
    }
    
    @Test
    void should_return_400_for_invalid_email() throws Exception {
        // Given
        CreateCustomerRequest request = new CreateCustomerRequest(
            "John Doe",
            "invalid-email",
            "password123"
        );
        
        // When & Then
        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("email"))
            .andExpect(jsonPath("$.errors[0].message").value("Email format is invalid"));
    }
}
```

### 3. Message Queue Integration Tests

Test event publishing and consumption.

```java
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
class EventIntegrationTest {
    
    @Autowired
    private DomainEventPublisher eventPublisher;
    
    @Autowired
    private TestEventListener testEventListener;
    
    @Test
    void should_publish_and_consume_customer_created_event() throws Exception {
        // Given
        CustomerCreatedEvent event = CustomerCreatedEvent.create(
            CustomerId.of("CUST-001"),
            new CustomerName("John Doe"),
            new Email("john@example.com"),
            MembershipLevel.STANDARD
        );
        
        // When
        eventPublisher.publish(event);
        
        // Then
        await().atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(testEventListener.getReceivedEvents())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(CustomerCreatedEvent.class);
            });
    }
}
```

---

## Test Configuration

### Test Database Configuration

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

### Test Properties

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  
  kafka:
    bootstrap-servers: ${spring.embedded.kafka.brokers}
  
  redis:
    host: localhost
    port: 6379
```

---

## Best Practices

### 1. Test Data Management

```java
@TestComponent
public class TestDataFactory {
    
    public Customer createCustomer(String email) {
        return Customer.builder()
            .id(UUID.randomUUID().toString())
            .name("Test Customer")
            .email(email)
            .membershipLevel(MembershipLevel.STANDARD)
            .build();
    }
    
    public Order createOrder(Customer customer) {
        return Order.builder()
            .id(UUID.randomUUID().toString())
            .customerId(customer.getId())
            .status(OrderStatus.PENDING)
            .items(List.of(createOrderItem()))
            .build();
    }
}
```

### 2. Test Isolation

```java
@DataJpaTest
@Transactional
class CustomerRepositoryTest {
    
    @BeforeEach
    void setUp() {
        // Clean database before each test
        customerRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }
    
    @AfterEach
    void tearDown() {
        // Cleanup after each test
        customerRepository.deleteAll();
    }
}
```

### 3. Async Testing

```java
@Test
void should_process_order_asynchronously() throws Exception {
    // Given
    Order order = createOrder();
    
    // When
    orderService.processOrderAsync(order.getId());
    
    // Then
    await().atMost(10, TimeUnit.SECONDS)
        .pollInterval(100, TimeUnit.MILLISECONDS)
        .untilAsserted(() -> {
            Order processedOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(processedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        });
}
```

---

## Common Patterns

### Testing Transactions

```java
@Test
@Transactional
void should_rollback_on_exception() {
    // Given
    Customer customer = createCustomer();
    customerRepository.save(customer);
    
    // When
    assertThatThrownBy(() -> {
        customer.updateEmail("invalid-email");
        customerRepository.save(customer);
        throw new RuntimeException("Simulated error");
    }).isInstanceOf(RuntimeException.class);
    
    // Then: Changes should be rolled back
    entityManager.clear();
    Customer reloadedCustomer = customerRepository.findById(customer.getId()).orElseThrow();
    assertThat(reloadedCustomer.getEmail()).isNotEqualTo("invalid-email");
}
```

### Testing Pagination

```java
@Test
void should_paginate_customers_correctly() {
    // Given
    for (int i = 0; i < 25; i++) {
        customerRepository.save(createCustomer("customer" + i + "@example.com"));
    }
    entityManager.flush();
    
    // When
    Page<Customer> page1 = customerRepository.findAll(PageRequest.of(0, 10));
    Page<Customer> page2 = customerRepository.findAll(PageRequest.of(1, 10));
    Page<Customer> page3 = customerRepository.findAll(PageRequest.of(2, 10));
    
    // Then
    assertThat(page1.getContent()).hasSize(10);
    assertThat(page2.getContent()).hasSize(10);
    assertThat(page3.getContent()).hasSize(5);
    assertThat(page1.getTotalElements()).isEqualTo(25);
    assertThat(page1.getTotalPages()).isEqualTo(3);
}
```

### Testing Caching

```java
@Test
void should_cache_customer_lookups() {
    // Given
    Customer customer = createCustomer();
    customerRepository.save(customer);
    entityManager.flush();
    entityManager.clear();
    
    // When: First call - should hit database
    Customer result1 = customerService.findById(customer.getId());
    
    // When: Second call - should hit cache
    Customer result2 = customerService.findById(customer.getId());
    
    // Then
    assertThat(result1).isEqualTo(result2);
    verify(customerRepository, times(1)).findById(customer.getId());
}
```

---

## Performance Standards

### Execution Time

- **Repository Tests**: < 500ms per test
- **API Tests**: < 500ms per test
- **Message Queue Tests**: < 1s per test

### Memory Usage

- **Maximum**: < 50MB per test
- **Average**: < 20MB per test

### Success Rate

- **Target**: > 95%
- **Flaky Tests**: < 1%

---

## Troubleshooting

### Common Issues

**Issue**: Tests fail intermittently
- **Cause**: Race conditions, timing issues
- **Solution**: Use `await()` for async operations, ensure proper test isolation

**Issue**: Database state pollution
- **Cause**: Tests not cleaning up properly
- **Solution**: Use `@Transactional` with `@Rollback`, or explicit cleanup in `@AfterEach`

**Issue**: Slow test execution
- **Cause**: Too many database operations, unnecessary setup
- **Solution**: Use test data builders, minimize database calls, use in-memory database

---


**Document Version**: 1.0  
**Last Updated**: 2024-11-19  
**Owner**: Development Team
