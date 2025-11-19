# Adding a REST API Endpoint

> **Last Updated**: 2025-10-25

## Overview

This guide shows how to add a new REST API endpoint to the Enterprise E-Commerce Platform, following RESTful principles and our architecture standards.

## Example: Add Get Customer Orders Endpoint

We'll add an endpoint to retrieve all orders for a specific customer.

### Step 1: Define API Specification

**Endpoint:** `GET /api/v1/customers/{customerId}/orders`

**Request:**

- Path parameter: `customerId` (String)
- Query parameters:
  - `status` (optional): Filter by order status
  - `page` (optional, default: 0): Page number
  - `size` (optional, default: 20): Page size

**Response:** `200 OK`

```json
{
  "content": [
    {
      "orderId": "ORD-001",
      "customerId": "CUST-001",
      "status": "PENDING",
      "totalAmount": 150.00,
      "createdAt": "2025-10-25T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

**Error Responses:**

- `404 Not Found`: Customer not found
- `400 Bad Request`: Invalid parameters

### Step 2: Create Request/Response DTOs

#### Response DTO

```java
// Location: interfaces/rest/customer/dto/response/CustomerOrdersResponse.java
package solid.humank.genaidemo.interfaces.rest.customer.dto.response;

import java.util.List;

public record CustomerOrdersResponse(
    List<OrderSummaryDto> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static CustomerOrdersResponse from(Page<Order> orderPage) {
        List<OrderSummaryDto> orders = orderPage.getContent().stream()
            .map(OrderSummaryDto::from)
            .toList();
            
        return new CustomerOrdersResponse(
            orders,
            orderPage.getNumber(),
            orderPage.getSize(),
            orderPage.getTotalElements(),
            orderPage.getTotalPages()
        );
    }
}

public record OrderSummaryDto(
    String orderId,
    String customerId,
    String status,
    BigDecimal totalAmount,
    LocalDateTime createdAt
) {
    public static OrderSummaryDto from(Order order) {
        return new OrderSummaryDto(
            order.getId().value(),
            order.getCustomerId().value(),
            order.getStatus().name(),
            order.getTotalAmount().amount(),
            order.getCreatedAt()
        );
    }
}
```

### Step 3: Add Repository Method

```java
// Location: domain/order/repository/OrderRepository.java
public interface OrderRepository {
    // Existing methods...
    
    Page<Order> findByCustomerId(CustomerId customerId, Pageable pageable);
    
    Page<Order> findByCustomerIdAndStatus(
        CustomerId customerId,
        OrderStatus status,
        Pageable pageable
    );
}
```

### Step 4: Implement Repository Method

```java
// Location: infrastructure/order/persistence/repository/JpaOrderRepository.java
@Repository
public class JpaOrderRepository implements OrderRepository {
    
    @Override
    public Page<Order> findByCustomerId(CustomerId customerId, Pageable pageable) {
        Page<OrderEntity> entityPage = jpaRepository
            .findByCustomerId(customerId.value(), pageable);
        return entityPage.map(mapper::toDomain);
    }
    
    @Override
    public Page<Order> findByCustomerIdAndStatus(
        CustomerId customerId,
        OrderStatus status,
        Pageable pageable
    ) {
        Page<OrderEntity> entityPage = jpaRepository
            .findByCustomerIdAndStatus(customerId.value(), status, pageable);
        return entityPage.map(mapper::toDomain);
    }
}
```

### Step 5: Create Application Service Method

```java
// Location: application/customer/CustomerApplicationService.java
@Service
@Transactional(readOnly = true)
public class CustomerApplicationService {
    
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    
    public Page<Order> getCustomerOrders(
        CustomerId customerId,
        Optional<OrderStatus> status,
        Pageable pageable
    ) {
        // Verify customer exists
        customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
        
        // Get orders
        if (status.isPresent()) {
            return orderRepository.findByCustomerIdAndStatus(
                customerId, status.get(), pageable
            );
        } else {
            return orderRepository.findByCustomerId(customerId, pageable);
        }
    }
}
```

### Step 6: Add Controller Endpoint

```java
// Location: interfaces/rest/customer/controller/CustomerController.java
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {
    
    private final CustomerApplicationService customerService;
    
    @GetMapping("/{customerId}/orders")
    public ResponseEntity<CustomerOrdersResponse> getCustomerOrders(
        @PathVariable String customerId,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        // Parse parameters
        CustomerId customerIdVO = CustomerId.of(customerId);
        Optional<OrderStatus> statusFilter = Optional.ofNullable(status)
            .map(OrderStatus::valueOf);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        // Get orders
        Page<Order> orders = customerService.getCustomerOrders(
            customerIdVO, statusFilter, pageable
        );
        
        // Return response
        return ResponseEntity.ok(CustomerOrdersResponse.from(orders));
    }
}
```

### Step 7: Write Tests

#### Unit Test for Application Service

```java
@ExtendWith(MockitoExtension.class)
class CustomerApplicationServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private OrderRepository orderRepository;
    
    @InjectMocks
    private CustomerApplicationService customerService;
    
    @Test
    void should_return_customer_orders() {
        // Given
        CustomerId customerId = CustomerId.of("CUST-001");
        Customer customer = createCustomer(customerId);
        Page<Order> orders = createOrderPage();
        Pageable pageable = PageRequest.of(0, 20);
        
        when(customerRepository.findById(customerId))
            .thenReturn(Optional.of(customer));
        when(orderRepository.findByCustomerId(customerId, pageable))
            .thenReturn(orders);
        
        // When
        Page<Order> result = customerService.getCustomerOrders(
            customerId, Optional.empty(), pageable
        );
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
    }
    
    @Test
    void should_throw_exception_when_customer_not_found() {
        // Given
        CustomerId customerId = CustomerId.of("CUST-999");
        when(customerRepository.findById(customerId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> customerService.getCustomerOrders(
            customerId, Optional.empty(), PageRequest.of(0, 20)
        ))
            .isInstanceOf(CustomerNotFoundException.class);
    }
}
```

#### Integration Test for Controller

```java
@WebMvcTest(CustomerController.class)
class CustomerControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CustomerApplicationService customerService;
    
    @Test
    void should_return_customer_orders() throws Exception {
        // Given
        CustomerId customerId = CustomerId.of("CUST-001");
        Page<Order> orders = createOrderPage();
        
        when(customerService.getCustomerOrders(
            eq(customerId), any(), any()
        )).thenReturn(orders);
        
        // When & Then
        mockMvc.perform(get("/api/v1/customers/CUST-001/orders")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].orderId").value("ORD-001"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20));
    }
    
    @Test
    void should_return_404_when_customer_not_found() throws Exception {
        // Given
        when(customerService.getCustomerOrders(any(), any(), any()))
            .thenThrow(new CustomerNotFoundException(CustomerId.of("CUST-999")));
        
        // When & Then
        mockMvc.perform(get("/api/v1/customers/CUST-999/orders"))
            .andExpect(status().isNotFound());
    }
}
```

### Step 8: Update API Documentation

Add to `docs/api/rest/endpoints/customers.md`:

```markdown
## Get Customer Orders

Retrieves all orders for a specific customer.

### Request

```http
GET /api/v1/customers/{customerId}/orders?status=PENDING&page=0&size=20
```text

### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| customerId | String | Yes | Customer ID |
| status | String | No | Filter by order status |
| page | Integer | No | Page number (default: 0) |
| size | Integer | No | Page size (default: 20) |

### Response

```json
{
  "content": [
    {
      "orderId": "ORD-001",
      "customerId": "CUST-001",
      "status": "PENDING",
      "totalAmount": 150.00,
      "createdAt": "2025-10-25T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```text

### Example

```bash
curl -X GET "http://localhost:8080/api/v1/customers/CUST-001/orders?status=PENDING" \
  -H "Authorization: Bearer YOUR_TOKEN"
```text

```

### Step 9: Test Manually

```bash
# Start application
./gradlew bootRun

# Test endpoint
curl http://localhost:8080/api/v1/customers/CUST-001/orders

# Test with filters
curl "http://localhost:8080/api/v1/customers/CUST-001/orders?status=PENDING&page=0&size=10"
```

## Summary

You've successfully added a new REST endpoint! The key steps were:

1. ✅ Define API specification
2. ✅ Create DTOs
3. ✅ Add repository method
4. ✅ Implement repository
5. ✅ Create application service method
6. ✅ Add controller endpoint
7. ✅ Write tests
8. ✅ Update documentation
9. ✅ Test manually

## Best Practices

- Use DTOs for request/response
- Validate input parameters
- Handle errors properly
- Write comprehensive tests
- Document the API
- Follow RESTful conventions
- Use pagination for lists
- Return appropriate HTTP status codes


**Document Version**: 1.0  
**Last Updated**: 2025-10-25  
**Maintained By**: Development Team
