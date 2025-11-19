# Testing Examples

> **Purpose**: Practical, runnable examples for testing  
> **Last Updated**: 2024-11-19  
> **Owner**: QA Team

---

## Overview

This document provides practical examples for unit testing, integration testing, and BDD testing using JUnit 5, Mockito, and Cucumber.

---

## Example 1: Unit Test - Domain Logic

### Scenario

Test business logic in a domain aggregate without external dependencies.

### Code Under Test

```java
@AggregateRoot
public class Order {
    private OrderId id;
    private CustomerId customerId;
    private List<OrderItem> items = new ArrayList<>();
    private OrderStatus status;
    private Money totalAmount;
    
    public void addItem(Product product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        if (status != OrderStatus.DRAFT) {
            throw new BusinessRuleViolationException("Cannot add items to non-draft order");
        }
        
        OrderItem item = new OrderItem(product.getId(), quantity, product.getPrice());
        items.add(item);
        recalculateTotal();
        
        collectEvent(OrderItemAddedEvent.create(id, item));
    }
    
    public void submit() {
        if (items.isEmpty()) {
            throw new BusinessRuleViolationException("Cannot submit empty order");
        }
        
        if (status != OrderStatus.DRAFT) {
            throw new BusinessRuleViolationException("Order already submitted");
        }
        
        status = OrderStatus.PENDING;
        collectEvent(OrderSubmittedEvent.create(id, customerId, totalAmount));
    }
    
    private void recalculateTotal() {
        totalAmount = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(Money.ZERO, Money::add);
    }
}
```

### Unit Test

```java
@ExtendWith(MockitoExtension.class)
class OrderTest {
    
    private Order order;
    private CustomerId customerId;
    private Product product;
    
    @BeforeEach
    void setUp() {
        customerId = CustomerId.of("customer-123");
        order = new Order(OrderId.generate(), customerId);
        product = new Product(
            ProductId.of("product-456"),
            "Test Product",
            Money.of(29.99)
        );
    }
    
    @Test
    @DisplayName("Should add item to draft order successfully")
    void should_add_item_to_draft_order() {
        // When
        order.addItem(product, 2);
        
        // Then
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotalAmount()).isEqualTo(Money.of(59.98));
        assertThat(order.hasUncommittedEvents()).isTrue();
        
        List<DomainEvent> events = order.getUncommittedEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(OrderItemAddedEvent.class);
    }
    
    @Test
    @DisplayName("Should throw exception when adding item with zero quantity")
    void should_throw_exception_when_quantity_is_zero() {
        // When & Then
        assertThatThrownBy(() -> order.addItem(product, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Quantity must be positive");
    }
    
    @Test
    @DisplayName("Should throw exception when adding item to submitted order")
    void should_throw_exception_when_adding_to_submitted_order() {
        // Given
        order.addItem(product, 1);
        order.submit();
        
        // When & Then
        assertThatThrownBy(() -> order.addItem(product, 1))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessage("Cannot add items to non-draft order");
    }
    
    @Test
    @DisplayName("Should submit order with items successfully")
    void should_submit_order_with_items() {
        // Given
        order.addItem(product, 2);
        
        // When
        order.submit();
        
        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        
        List<DomainEvent> events = order.getUncommittedEvents();
        assertThat(events).hasSize(2); // OrderItemAdded + OrderSubmitted
        assertThat(events.get(1)).isInstanceOf(OrderSubmittedEvent.class);
    }
    
    @Test
    @DisplayName("Should throw exception when submitting empty order")
    void should_throw_exception_when_submitting_empty_order() {
        // When & Then
        assertThatThrownBy(() -> order.submit())
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessage("Cannot submit empty order");
    }
}
```

---

## Example 2: Integration Test - Repository

### Scenario

Test repository implementation with actual database.

### Code Under Test

```java
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    
    @Query("SELECT o FROM OrderEntity o WHERE o.customerId = :customerId ORDER BY o.createdAt DESC")
    List<OrderEntity> findByCustomerId(@Param("customerId") String customerId);
    
    @Query("SELECT o FROM OrderEntity o WHERE o.status = :status AND o.createdAt >= :since")
    List<OrderEntity> findByStatusSince(
        @Param("status") String status,
        @Param("since") LocalDateTime since
    );
}
```

### Integration Test

```java
@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private OrderRepository repository;
    
    @Test
    @DisplayName("Should find orders by customer ID")
    void should_find_orders_by_customer_id() {
        // Given
        String customerId = "customer-123";
        
        OrderEntity order1 = createOrder(customerId, OrderStatus.PENDING);
        OrderEntity order2 = createOrder(customerId, OrderStatus.COMPLETED);
        OrderEntity order3 = createOrder("customer-456", OrderStatus.PENDING);
        
        entityManager.persistAndFlush(order1);
        entityManager.persistAndFlush(order2);
        entityManager.persistAndFlush(order3);
        
        // When
        List<OrderEntity> results = repository.findByCustomerId(customerId);
        
        // Then
        assertThat(results).hasSize(2);
        assertThat(results)
            .extracting(OrderEntity::getCustomerId)
            .containsOnly(customerId);
        assertThat(results)
            .extracting(OrderEntity::getStatus)
            .containsExactly(OrderStatus.COMPLETED, OrderStatus.PENDING); // DESC order
    }
    
    @Test
    @DisplayName("Should find orders by status since date")
    void should_find_orders_by_status_since() {
        // Given
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        LocalDateTime lastWeek = LocalDateTime.now().minusDays(7);
        
        OrderEntity recentOrder = createOrder("customer-123", OrderStatus.PENDING);
        recentOrder.setCreatedAt(yesterday);
        
        OrderEntity oldOrder = createOrder("customer-456", OrderStatus.PENDING);
        oldOrder.setCreatedAt(lastWeek);
        
        entityManager.persistAndFlush(recentOrder);
        entityManager.persistAndFlush(oldOrder);
        
        // When
        List<OrderEntity> results = repository.findByStatusSince(
            OrderStatus.PENDING.name(),
            LocalDateTime.now().minusDays(2)
        );
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(recentOrder.getId());
    }
    
    private OrderEntity createOrder(String customerId, OrderStatus status) {
        OrderEntity order = new OrderEntity();
        order.setId(UUID.randomUUID().toString());
        order.setCustomerId(customerId);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }
}
```

---

## Example 3: Integration Test - REST API

### Scenario

Test REST API endpoints with MockMvc.

### Code Under Test

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    
    private final OrderApplicationService orderService;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        
        CreateOrderCommand command = new CreateOrderCommand(
            request.customerId(),
            request.items(),
            request.shippingAddress()
        );
        
        Order order = orderService.createOrder(command);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(OrderResponse.from(order));
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        Order order = orderService.getOrder(OrderId.of(orderId));
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
```

### Integration Test

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private OrderApplicationService orderService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("Should create order successfully")
    void should_create_order() throws Exception {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
            "customer-123",
            List.of(new OrderItemRequest("product-456", 2, new BigDecimal("29.99"))),
            new AddressRequest("123 Main St", "San Francisco", "CA", "94102", "US")
        );
        
        Order order = createMockOrder();
        when(orderService.createOrder(any(CreateOrderCommand.class)))
            .thenReturn(order);
        
        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").value(order.getId().getValue()))
            .andExpect(jsonPath("$.customerId").value("customer-123"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.totalAmount").value(59.98));
        
        verify(orderService).createOrder(any(CreateOrderCommand.class));
    }
    
    @Test
    @DisplayName("Should return 400 when request is invalid")
    void should_return_400_when_request_invalid() throws Exception {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
            "", // Invalid: empty customer ID
            List.of(), // Invalid: empty items
            null // Invalid: null address
        );
        
        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors").isArray())
            .andExpect(jsonPath("$.fieldErrors[*].field").value(
                hasItems("customerId", "items", "shippingAddress")
            ));
        
        verify(orderService, never()).createOrder(any());
    }
    
    @Test
    @DisplayName("Should return 404 when order not found")
    void should_return_404_when_order_not_found() throws Exception {
        // Given
        String orderId = "non-existent-order";
        when(orderService.getOrder(OrderId.of(orderId)))
            .thenThrow(new OrderNotFoundException(orderId));
        
        // When & Then
        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value(containsString("not found")));
    }
}
```

---

## Example 4: BDD Test with Cucumber

### Scenario

Test business scenarios using Gherkin and Cucumber.

### Gherkin Feature File

```gherkin
# src/test/resources/features/order-submission.feature
Feature: Order Submission
  As a customer
  I want to submit an order
  So that I can purchase products

  Background:
    Given a customer with ID "customer-123"
    And the following products exist:
      | productId   | name          | price |
      | product-456 | Test Product  | 29.99 |
      | product-789 | Another Item  | 49.99 |

  Scenario: Submit order successfully
    Given the customer has items in shopping cart:
      | productId   | quantity |
      | product-456 | 2        |
      | product-789 | 1        |
    When the customer submits the order
    Then the order status should be "PENDING"
    And an order confirmation email should be sent
    And inventory should be reserved for:
      | productId   | quantity |
      | product-456 | 2        |
      | product-789 | 1        |

  Scenario: Cannot submit empty order
    Given the customer has an empty shopping cart
    When the customer attempts to submit the order
    Then the order submission should fail
    And the error message should be "Cannot submit empty order"

  Scenario: Cannot submit order with insufficient inventory
    Given the customer has items in shopping cart:
      | productId   | quantity |
      | product-456 | 100      |
    And product "product-456" has only 5 items in stock
    When the customer attempts to submit the order
    Then the order submission should fail
    And the error message should contain "Insufficient inventory"
```

### Step Definitions

```java
@SpringBootTest
@ActiveProfiles("test")
public class OrderSubmissionSteps {
    
    @Autowired
    private OrderApplicationService orderService;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private InventoryService inventoryService;
    
    private CustomerId customerId;
    private Order order;
    private Exception thrownException;
    
    @Given("a customer with ID {string}")
    public void aCustomerWithId(String customerIdValue) {
        customerId = CustomerId.of(customerIdValue);
        Customer customer = new Customer(
            customerId,
            new CustomerName("Test Customer"),
            new Email("test@example.com"),
            MembershipLevel.STANDARD
        );
        customerRepository.save(customer);
    }
    
    @Given("the following products exist:")
    public void theFollowingProductsExist(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        for (Map<String, String> row : rows) {
            Product product = new Product(
                ProductId.of(row.get("productId")),
                row.get("name"),
                Money.of(new BigDecimal(row.get("price")))
            );
            productRepository.save(product);
        }
    }
    
    @Given("the customer has items in shopping cart:")
    public void theCustomerHasItemsInShoppingCart(DataTable dataTable) {
        order = new Order(OrderId.generate(), customerId);
        
        List<Map<String, String>> rows = dataTable.asMaps();
        for (Map<String, String> row : rows) {
            ProductId productId = ProductId.of(row.get("productId"));
            int quantity = Integer.parseInt(row.get("quantity"));
            
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
            
            order.addItem(product, quantity);
        }
    }
    
    @Given("the customer has an empty shopping cart")
    public void theCustomerHasAnEmptyShoppingCart() {
        order = new Order(OrderId.generate(), customerId);
    }
    
    @Given("product {string} has only {int} items in stock")
    public void productHasOnlyItemsInStock(String productId, int stock) {
        inventoryService.setStock(ProductId.of(productId), stock);
    }
    
    @When("the customer submits the order")
    public void theCustomerSubmitsTheOrder() {
        try {
            order.submit();
            orderService.processOrder(order);
        } catch (Exception e) {
            thrownException = e;
        }
    }
    
    @When("the customer attempts to submit the order")
    public void theCustomerAttemptsToSubmitTheOrder() {
        try {
            order.submit();
            orderService.processOrder(order);
        } catch (Exception e) {
            thrownException = e;
        }
    }
    
    @Then("the order status should be {string}")
    public void theOrderStatusShouldBe(String expectedStatus) {
        assertThat(order.getStatus().name()).isEqualTo(expectedStatus);
    }
    
    @Then("an order confirmation email should be sent")
    public void anOrderConfirmationEmailShouldBeSent() {
        // Verify email was sent (check mock or test email service)
        List<DomainEvent> events = order.getUncommittedEvents();
        assertThat(events).anyMatch(e -> e instanceof OrderSubmittedEvent);
    }
    
    @Then("inventory should be reserved for:")
    public void inventoryShouldBeReservedFor(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        for (Map<String, String> row : rows) {
            ProductId productId = ProductId.of(row.get("productId"));
            int quantity = Integer.parseInt(row.get("quantity"));
            
            int reserved = inventoryService.getReservedQuantity(productId, order.getId());
            assertThat(reserved).isEqualTo(quantity);
        }
    }
    
    @Then("the order submission should fail")
    public void theOrderSubmissionShouldFail() {
        assertThat(thrownException).isNotNull();
    }
    
    @Then("the error message should be {string}")
    public void theErrorMessageShouldBe(String expectedMessage) {
        assertThat(thrownException).hasMessage(expectedMessage);
    }
    
    @Then("the error message should contain {string}")
    public void theErrorMessageShouldContain(String expectedText) {
        assertThat(thrownException.getMessage()).contains(expectedText);
    }
}
```

### Running Cucumber Tests

```bash
# Run all Cucumber tests
./gradlew cucumber

# Run specific feature
./gradlew cucumber --tests "OrderSubmissionTest"

# Run with tags
./gradlew cucumber -Dcucumber.filter.tags="@smoke"
```

---

## Example 5: Performance Test

### Scenario

Test system performance under load.

### JMeter Test Plan

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan testname="Order API Load Test">
      <elementProp name="TestPlan.user_defined_variables" elementType="Arguments">
        <collectionProp name="Arguments.arguments">
          <elementProp name="baseUrl" elementType="Argument">
            <stringProp name="Argument.name">baseUrl</stringProp>
            <stringProp name="Argument.value">https://api.example.com</stringProp>
          </elementProp>
          <elementProp name="users" elementType="Argument">
            <stringProp name="Argument.name">users</stringProp>
            <stringProp name="Argument.value">100</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>
    
    <ThreadGroup testname="Order Creation Load">
      <stringProp name="ThreadGroup.num_threads">${users}</stringProp>
      <stringProp name="ThreadGroup.ramp_time">60</stringProp>
      <stringProp name="ThreadGroup.duration">300</stringProp>
      
      <HTTPSamplerProxy testname="Create Order">
        <stringProp name="HTTPSampler.domain">${baseUrl}</stringProp>
        <stringProp name="HTTPSampler.path">/api/v1/orders</stringProp>
        <stringProp name="HTTPSampler.method">POST</stringProp>
        <stringProp name="HTTPSampler.postBodyRaw">{
          "customerId": "customer-${__threadNum}",
          "items": [
            {"productId": "product-456", "quantity": 2, "price": 29.99}
          ]
        }</stringProp>
      </HTTPSamplerProxy>
      
      <ResponseAssertion testname="Response Time Assertion">
        <stringProp name="Assertion.test_field">Assertion.response_time</stringProp>
        <stringProp name="Assertion.test_type">Assertion.duration</stringProp>
        <stringProp name="Assertion.test_string">2000</stringProp>
      </ResponseAssertion>
    </ThreadGroup>
  </hashTree>
</jmeterTestPlan>
```

### Running Performance Tests

```bash
# Run JMeter test
jmeter -n -t order-load-test.jmx \
  -l results.jtl \
  -e -o report \
  -Jusers=100 \
  -JbaseUrl=https://api.example.com

# View results
open report/index.html
```

---

## Related Documentation

- [Testing Strategy](../development/testing/testing-strategy.md)
- [BDD Testing Guide](../development/testing/bdd-testing.md)
- [Performance Testing](../perspectives/performance/verification.md)
- [Test Performance Standards](../.kiro/steering/test-performance-standards.md)

---

**Document Version**: 1.0  
**Last Updated**: 2024-11-19  
**Owner**: QA Team
