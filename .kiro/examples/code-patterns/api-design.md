# API Design Patterns

## Overview

RESTful API design patterns and best practices for our project.

**Related Standards**: [Development Standards](../../steering/development-standards.md)

---

## RESTful URL Design

### Resource Naming

```java
// ✅ GOOD: RESTful conventions
GET    /api/v1/customers                    // List customers
GET    /api/v1/customers/{id}               // Get customer
POST   /api/v1/customers                    // Create customer
PUT    /api/v1/customers/{id}               // Update customer (full)
PATCH  /api/v1/customers/{id}               // Update customer (partial)
DELETE /api/v1/customers/{id}               // Delete customer

// Nested resources
GET    /api/v1/customers/{id}/orders        // Get customer's orders
POST   /api/v1/customers/{id}/orders        // Create order for customer

// Actions (non-CRUD)
POST   /api/v1/orders/{id}/submit           // Submit order
POST   /api/v1/orders/{id}/cancel           // Cancel order
POST   /api/v1/orders/{id}/ship             // Ship order

// ❌ BAD: Non-RESTful
GET    /api/v1/getCustomer?id=123
POST   /api/v1/createCustomer
POST   /api/v1/customer/delete
```

---

## Request/Response Design

### DTOs and Validation

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
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
            customer.getId().getValue(),
            customer.getName().getValue(),
            customer.getEmail().getValue(),
            AddressDto.from(customer.getAddress()),
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }
}
```

### HTTP Status Codes

```java
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {
    
    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> listCustomers(Pageable pageable) {
        // 200 OK for successful GET
        return ResponseEntity.ok(customerService.findAll(pageable));
    }
    
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = customerService.createCustomer(request);
        // 201 Created for successful POST
        return ResponseEntity
            .created(URI.create("/api/v1/customers/" + customer.getId()))
            .body(CustomerResponse.from(customer));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable String id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        Customer customer = customerService.updateCustomer(id, request);
        // 200 OK for successful PUT
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String id) {
        customerService.deleteCustomer(id);
        // 204 No Content for successful DELETE
        return ResponseEntity.noContent().build();
    }
}
```

---

## Pagination and Filtering

```java
@RestController
public class ProductController {
    
    @GetMapping("/api/v1/products")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        
        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
            .category(category)
            .minPrice(minPrice)
            .maxPrice(maxPrice)
            .build();
        
        Page<Product> products = productService.search(criteria, pageable);
        Page<ProductResponse> response = products.map(ProductResponse::from);
        
        return ResponseEntity.ok(response);
    }
}

// Response includes pagination metadata
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {"sorted": true, "unsorted": false}
  },
  "totalElements": 150,
  "totalPages": 8,
  "last": false,
  "first": true
}
```

---

## API Versioning

```java
// URL versioning (recommended)
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerControllerV1 { }

@RestController
@RequestMapping("/api/v2/customers")
public class CustomerControllerV2 { }

// Header versioning (alternative)
@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    
    @GetMapping(headers = "API-Version=1")
    public ResponseEntity<CustomerResponseV1> getCustomerV1(@PathVariable String id) { }
    
    @GetMapping(headers = "API-Version=2")
    public ResponseEntity<CustomerResponseV2> getCustomerV2(@PathVariable String id) { }
}
```

---

## Error Responses

```java
// Consistent error format
public record ErrorResponse(
    String errorCode,
    String message,
    Map<String, Object> context,
    List<FieldError> fieldErrors,
    Instant timestamp
) {}

// Example responses
// 400 Bad Request - Validation error
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "fieldErrors": [
    {"field": "email", "message": "Email format is invalid", "rejectedValue": "invalid-email"}
  ],
  "timestamp": "2025-01-17T10:30:00Z"
}

// 404 Not Found
{
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "Customer with id CUST-001 not found",
  "context": {"resourceType": "Customer", "resourceId": "CUST-001"},
  "timestamp": "2025-01-17T10:30:00Z"
}

// 409 Conflict - Business rule violation
{
  "errorCode": "BUSINESS_RULE_VIOLATION",
  "message": "Cannot submit empty order",
  "context": {"rule": "ORDER_EMPTY"},
  "timestamp": "2025-01-17T10:30:00Z"
}
```

---

## API Documentation

```java
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer Management", description = "APIs for managing customers")
public class CustomerController {
    
    @Operation(
        summary = "Create a new customer",
        description = "Creates a new customer with the provided information"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Customer created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Customer creation request",
                required = true
            )
            @Valid @RequestBody CreateCustomerRequest request) {
        // Implementation
    }
}
```

---

## Summary

Key API design principles:

1. **Use RESTful conventions** for URLs and HTTP methods
2. **Return appropriate status codes** (200, 201, 204, 400, 404, 409, 500)
3. **Validate input** with Bean Validation
4. **Provide consistent error responses** with error codes
5. **Support pagination and filtering** for list endpoints
6. **Version your APIs** for backward compatibility
7. **Document with OpenAPI** (Swagger)

---

**Related Documentation**:
- [Development Standards](../../steering/development-standards.md)
- [Error Handling](error-handling.md)
- [Code Quality Checklist](../../steering/code-quality-checklist.md)
