# API Versioning Strategy

> **Last Updated**: 2025-10-24  
> **Status**: Active  
> **Owner**: API & Architecture Team

## Overview

This document defines the API versioning strategy for the Enterprise E-Commerce Platform. Our approach ensures backward compatibility while allowing the API to evolve, protecting existing integrations while enabling innovation.

## Versioning Approach

### URL-Based Versioning

We use URL-based versioning as our primary strategy:

```
https://api.ecommerce.example.com/api/v1/orders
https://api.ecommerce.example.com/api/v2/orders
https://api.ecommerce.example.com/api/v3/orders
```

**Rationale**:
- ✅ Clear and explicit
- ✅ Easy to route and cache
- ✅ Simple for clients to understand
- ✅ Works with all HTTP clients
- ✅ No special headers required

---

## Version Lifecycle

### Version States

```
┌─────────────────────────────────────────────────────────┐
│              API Version Lifecycle                      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Alpha (Internal Only)                                  │
│  - Development and testing                              │
│  - Breaking changes allowed                             │
│  - No stability guarantees                              │
│  Duration: 1-2 months                                   │
│         │                                               │
│         ▼                                               │
│  Beta (Selected Partners)                               │
│  - Feature complete                                     │
│  - Minor changes possible                               │
│  - Feedback collection                                  │
│  Duration: 2-3 months                                   │
│         │                                               │
│         ▼                                               │
│  Stable (General Availability)                          │
│  - Production ready                                     │
│  - Backward compatible changes only                     │
│  - Full support                                         │
│  Duration: 12-18 months                                 │
│         │                                               │
│         ▼                                               │
│  Deprecated                                             │
│  - Marked for removal                                   │
│  - Bug fixes only                                       │
│  - Migration guide provided                             │
│  Duration: 6 months                                     │
│         │                                               │
│         ▼                                               │
│  Retired                                                │
│  - No longer available                                  │
│  - Returns 410 Gone                                     │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Version Support Policy

| Version | Status | Support Level | End of Life |
|---------|--------|---------------|-------------|
| **v3** | Alpha | Internal testing | TBD |
| **v2** | Stable | Full support | 2026-12-31 |
| **v1** | Deprecated | Bug fixes only | 2025-12-31 |

---

## Breaking vs Non-Breaking Changes

### Breaking Changes (Require New Version)

❌ **Breaking changes that require a new API version**:

1. **Removing fields**
   ```json
   // v1 - Has email field
   {
     "id": "123",
     "name": "John Doe",
     "email": "john@example.com"
   }
   
   // v2 - Email field removed (BREAKING)
   {
     "id": "123",
     "name": "John Doe"
   }
   ```

2. **Changing field types**
   ```json
   // v1 - Price as number
   {
     "price": 19.99
   }
   
   // v2 - Price as object (BREAKING)
   {
     "price": {
       "amount": 19.99,
       "currency": "USD"
     }
   }
   ```

3. **Renaming fields**
   ```json
   // v1
   {
     "customer_id": "123"
   }
   
   // v2 - Field renamed (BREAKING)
   {
     "customerId": "123"
   }
   ```

4. **Changing URL structure**
   ```
   // v1
   GET /api/v1/customers/{id}/orders
   
   // v2 - URL changed (BREAKING)
   GET /api/v2/orders?customerId={id}
   ```

5. **Changing HTTP methods**
   ```
   // v1
   POST /api/v1/orders/{id}/cancel
   
   // v2 - Method changed (BREAKING)
   DELETE /api/v2/orders/{id}
   ```

6. **Adding required fields**
   ```json
   // v1 - Optional field
   {
     "name": "John Doe"
   }
   
   // v2 - Field now required (BREAKING)
   {
     "name": "John Doe",
     "email": "john@example.com"  // Now required
   }
   ```

### Non-Breaking Changes (Same Version)

✅ **Non-breaking changes that can be made within the same version**:

1. **Adding optional fields**
   ```json
   // Before
   {
     "id": "123",
     "name": "John Doe"
   }
   
   // After - New optional field (NON-BREAKING)
   {
     "id": "123",
     "name": "John Doe",
     "phone": "+1234567890"  // New optional field
   }
   ```

2. **Adding new endpoints**
   ```
   // Existing
   GET /api/v1/orders
   
   // New endpoint (NON-BREAKING)
   GET /api/v1/orders/recent
   ```

3. **Adding new optional query parameters**
   ```
   // Before
   GET /api/v1/orders?status=pending
   
   // After - New optional parameter (NON-BREAKING)
   GET /api/v1/orders?status=pending&sortBy=date
   ```

4. **Adding new HTTP methods to existing endpoints**
   ```
   // Existing
   GET /api/v1/orders/{id}
   
   // New method (NON-BREAKING)
   PATCH /api/v1/orders/{id}
   ```

5. **Expanding enum values**
   ```json
   // Before
   {
     "status": "pending" | "completed"
   }
   
   // After - New enum value (NON-BREAKING)
   {
     "status": "pending" | "processing" | "completed"
   }
   ```

---

## Version Implementation

### Controller Versioning

```java
// Version 1 Controller
@RestController
@RequestMapping("/api/v1/orders")
public class OrderControllerV1 {
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseV1> getOrder(@PathVariable String id) {
        Order order = orderService.findById(id);
        return ResponseEntity.ok(OrderResponseV1.from(order));
    }
    
    @PostMapping
    public ResponseEntity<OrderResponseV1> createOrder(
        @RequestBody CreateOrderRequestV1 request
    ) {
        Order order = orderService.createOrder(request.toCommand());
        return ResponseEntity.ok(OrderResponseV1.from(order));
    }
}

// Version 2 Controller
@RestController
@RequestMapping("/api/v2/orders")
public class OrderControllerV2 {
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseV2> getOrder(@PathVariable String id) {
        Order order = orderService.findById(id);
        return ResponseEntity.ok(OrderResponseV2.from(order));
    }
    
    @PostMapping
    public ResponseEntity<OrderResponseV2> createOrder(
        @RequestBody CreateOrderRequestV2 request
    ) {
        Order order = orderService.createOrder(request.toCommand());
        return ResponseEntity.ok(OrderResponseV2.from(order));
    }
}
```

### DTO Versioning

```java
// Version 1 DTOs
public record OrderResponseV1(
    String id,
    String customerId,
    BigDecimal totalAmount,
    String status,
    LocalDateTime createdAt
) {
    public static OrderResponseV1 from(Order order) {
        return new OrderResponseV1(
            order.getId().getValue(),
            order.getCustomerId().getValue(),
            order.getTotalAmount().getAmount(),
            order.getStatus().name(),
            order.getCreatedAt()
        );
    }
}

// Version 2 DTOs - Enhanced with more details
public record OrderResponseV2(
    String id,
    CustomerSummary customer,  // Enhanced: Full customer object
    Money totalAmount,          // Enhanced: Money object with currency
    String status,
    List<OrderItemV2> items,   // Enhanced: Include items
    LocalDateTime createdAt,
    LocalDateTime updatedAt    // New field
) {
    public static OrderResponseV2 from(Order order) {
        return new OrderResponseV2(
            order.getId().getValue(),
            CustomerSummary.from(order.getCustomer()),
            Money.from(order.getTotalAmount()),
            order.getStatus().name(),
            order.getItems().stream()
                .map(OrderItemV2::from)
                .toList(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
}
```

### Shared Business Logic

```java
// Domain service remains version-agnostic
@Service
public class OrderService {
    
    public Order findById(OrderId orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
    
    public Order createOrder(CreateOrderCommand command) {
        // Business logic is the same for all API versions
        Order order = Order.create(command);
        return orderRepository.save(order);
    }
}
```

---

## Deprecation Process

### Deprecation Announcement

#### 1. Add Deprecation Headers

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderControllerV1 {
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseV1> getOrder(@PathVariable String id) {
        Order order = orderService.findById(id);
        
        return ResponseEntity.ok()
            .header("Deprecation", "true")
            .header("Sunset", "2025-12-31T23:59:59Z")
            .header("Link", "</api/v2/orders>; rel=\"successor-version\"")
            .body(OrderResponseV1.from(order));
    }
}
```

#### 2. Update API Documentation

```yaml
# OpenAPI Specification
paths:
  /api/v1/orders/{id}:
    get:
      deprecated: true
      summary: Get order by ID (DEPRECATED - Use v2)
      description: |
        **This endpoint is deprecated and will be removed on 2025-12-31.**
        
        Please migrate to v2: GET /api/v2/orders/{id}
        
        Migration guide: https://docs.ecommerce.example.com/api/migration/v1-to-v2
```

#### 3. Send Email Notifications

```
Subject: API v1 Deprecation Notice - Action Required

Dear API Consumer,

We are writing to inform you that API v1 will be deprecated on 2025-06-30 
and will be completely removed on 2025-12-31.

Your application is currently using the following deprecated endpoints:
- GET /api/v1/orders
- POST /api/v1/orders

Please migrate to API v2 before the sunset date.

Migration Guide: https://docs.ecommerce.example.com/api/migration/v1-to-v2
Support: api-support@ecommerce.example.com

Thank you,
API Team
```

### Deprecation Timeline

```
┌─────────────────────────────────────────────────────────┐
│           Deprecation Timeline (6 months)               │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Month 0: Announcement                                  │
│  - Add deprecation headers                              │
│  - Update documentation                                 │
│  - Email all API consumers                              │
│  - Blog post announcement                               │
│         │                                               │
│         ▼                                               │
│  Month 1-2: Migration Support                           │
│  - Provide migration guide                              │
│  - Offer migration assistance                           │
│  - Monitor API usage                                    │
│         │                                               │
│         ▼                                               │
│  Month 3-4: Active Migration                            │
│  - Follow up with consumers                             │
│  - Track migration progress                             │
│  - Address migration issues                             │
│         │                                               │
│         ▼                                               │
│  Month 5: Final Warning                                 │
│  - Send final reminder                                  │
│  - Identify remaining consumers                         │
│  - Offer direct support                                 │
│         │                                               │
│         ▼                                               │
│  Month 6: Retirement                                    │
│  - Remove deprecated endpoints                          │
│  - Return 410 Gone status                               │
│  - Redirect to migration guide                          │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Handling Retired Endpoints

```java
@RestController
@RequestMapping("/api/v1")
public class RetiredEndpointsController {
    
    @RequestMapping("/**")
    public ResponseEntity<ErrorResponse> handleRetiredEndpoint(
        HttpServletRequest request
    ) {
        String requestedPath = request.getRequestURI();
        
        ErrorResponse error = ErrorResponse.builder()
            .status(410)
            .error("Gone")
            .message("This API version has been retired")
            .details(Map.of(
                "retiredVersion", "v1",
                "currentVersion", "v2",
                "migrationGuide", "https://docs.ecommerce.example.com/api/migration/v1-to-v2",
                "retiredDate", "2025-12-31"
            ))
            .timestamp(Instant.now())
            .build();
        
        return ResponseEntity.status(HttpStatus.GONE).body(error);
    }
}
```

---

## Migration Guides

### V1 to V2 Migration Guide

#### Overview of Changes

| Change | V1 | V2 | Impact |
|--------|----|----|--------|
| **Response Format** | Flat structure | Nested objects | Medium |
| **Date Format** | ISO 8601 | ISO 8601 with timezone | Low |
| **Error Format** | Simple message | Structured errors | Medium |
| **Pagination** | Offset-based | Cursor-based | High |

#### Field Mapping

```json
// V1 Response
{
  "id": "123",
  "customer_id": "456",
  "total_amount": 99.99,
  "status": "pending",
  "created_at": "2025-10-24T10:00:00"
}

// V2 Response
{
  "id": "123",
  "customer": {
    "id": "456",
    "name": "John Doe",
    "email": "john@example.com"
  },
  "totalAmount": {
    "amount": 99.99,
    "currency": "USD"
  },
  "status": "PENDING",
  "items": [
    {
      "productId": "789",
      "quantity": 2,
      "price": {
        "amount": 49.99,
        "currency": "USD"
      }
    }
  ],
  "createdAt": "2025-10-24T10:00:00Z",
  "updatedAt": "2025-10-24T10:05:00Z"
}
```

#### Code Examples

**Before (V1)**:
```javascript
// JavaScript client
const response = await fetch('https://api.ecommerce.example.com/api/v1/orders/123');
const order = await response.json();

console.log(order.customer_id);  // "456"
console.log(order.total_amount);  // 99.99
```

**After (V2)**:
```javascript
// JavaScript client
const response = await fetch('https://api.ecommerce.example.com/api/v2/orders/123');
const order = await response.json();

console.log(order.customer.id);        // "456"
console.log(order.totalAmount.amount); // 99.99
console.log(order.totalAmount.currency); // "USD"
```

---

## Backward Compatibility Strategies

### 1. Adapter Pattern

```java
// Adapter to convert between versions
@Component
public class OrderResponseAdapter {
    
    public OrderResponseV1 toV1(Order order) {
        return OrderResponseV1.from(order);
    }
    
    public OrderResponseV2 toV2(Order order) {
        return OrderResponseV2.from(order);
    }
    
    // Convert V2 to V1 for backward compatibility
    public OrderResponseV1 downgradeToV1(OrderResponseV2 v2Response) {
        return new OrderResponseV1(
            v2Response.id(),
            v2Response.customer().id(),
            v2Response.totalAmount().amount(),
            v2Response.status(),
            v2Response.createdAt()
        );
    }
}
```

### 2. Feature Flags for Gradual Rollout

```java
@RestController
@RequestMapping("/api/v2/orders")
public class OrderControllerV2 {
    
    private final FeatureFlagService featureFlags;
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseV2> getOrder(
        @PathVariable String id,
        @RequestHeader(value = "X-Client-Id", required = false) String clientId
    ) {
        Order order = orderService.findById(id);
        
        // Gradual rollout of new features
        if (featureFlags.isEnabledForClient("enhanced-response", clientId)) {
            return ResponseEntity.ok(OrderResponseV2.withEnhancements(order));
        } else {
            return ResponseEntity.ok(OrderResponseV2.from(order));
        }
    }
}
```

### 3. Content Negotiation (Alternative Approach)

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @GetMapping(value = "/{id}", produces = "application/vnd.ecommerce.v1+json")
    public ResponseEntity<OrderResponseV1> getOrderV1(@PathVariable String id) {
        Order order = orderService.findById(id);
        return ResponseEntity.ok(OrderResponseV1.from(order));
    }
    
    @GetMapping(value = "/{id}", produces = "application/vnd.ecommerce.v2+json")
    public ResponseEntity<OrderResponseV2> getOrderV2(@PathVariable String id) {
        Order order = orderService.findById(id);
        return ResponseEntity.ok(OrderResponseV2.from(order));
    }
}
```

---

## API Versioning Best Practices

### Do's ✅

1. **Version from Day One**: Start with v1, even if it's the first version
2. **Document Changes**: Maintain detailed changelog
3. **Provide Migration Guides**: Help clients upgrade smoothly
4. **Use Semantic Versioning**: Major.Minor.Patch for clarity
5. **Test Backward Compatibility**: Automated tests for each version
6. **Monitor Usage**: Track which versions are being used
7. **Communicate Early**: Give advance notice of deprecations

### Don'ts ❌

1. **Don't Break Contracts**: Never make breaking changes within a version
2. **Don't Rush Deprecation**: Give clients adequate time to migrate
3. **Don't Remove Without Warning**: Always announce deprecations
4. **Don't Version Everything**: Not every change needs a new version
5. **Don't Maintain Too Many Versions**: Limit to 2-3 active versions
6. **Don't Forget Documentation**: Keep docs in sync with code

---

## Monitoring and Analytics

### Version Usage Metrics

```java
@Component
public class ApiVersionMetrics {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void recordApiRequest(ApiRequestEvent event) {
        meterRegistry.counter("api.requests",
            Tags.of(
                "version", event.getVersion(),
                "endpoint", event.getEndpoint(),
                "status", String.valueOf(event.getStatusCode())
            )
        ).increment();
    }
}
```

### Version Analytics Dashboard

Track:
- **Requests per version**: Which versions are most used
- **Client distribution**: Which clients use which versions
- **Deprecation impact**: How many clients still use deprecated versions
- **Migration progress**: Track migration from old to new versions
- **Error rates by version**: Identify version-specific issues

---

**Related Documents**:
- [Overview](overview.md) - Evolution perspective introduction
- [Extensibility](extensibility.md) - Extension points and mechanisms
- [Technology Evolution](technology-evolution.md) - Framework upgrades
- [Refactoring](refactoring.md) - Code quality management
