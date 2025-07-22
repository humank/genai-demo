<!-- This file is auto-translated from docs/en/shared-kernel-refactoring.md -->
<!-- 此檔案由 docs/en/shared-kernel-refactoring.md 自動翻譯而來 -->
<!-- Please use Kiro AI to complete the actual translation -->
<!-- 請使用 Kiro AI 完成實際翻譯 -->

# Shared Kernel Refactoring Plan

## Current Issues
- `domain/common` package contains too much shared code
- Strong coupling between services
- Difficult version management

## Refactoring Plan

### 1. Split Shared Kernel

#### A. Core Value Objects Library (core-valueobjects)
```java
// Only includes truly common value objects
- Money
- Currency  
- CustomerId (as generic identifier)
- Base DomainEvent interface
```

#### B. Service-Specific Value Objects
```java
// Order Service specific
- OrderId
- OrderStatus
- OrderItem

// Payment Service specific  
- PaymentId
- PaymentStatus
- PaymentMethod

// Inventory Service specific
- ProductId
- InventoryLevel
- ReservationId
```

### 2. Event Contracts Library (event-contracts)
```java
// Cross-service event definitions
public class OrderCreatedEvent {
    private String orderId;
    private String customerId;
    private BigDecimal totalAmount;
    private List<OrderItemDto> items;
    // Only includes data needed by other services
}

public class PaymentProcessedEvent {
    private String paymentId;
    private String orderId;
    private PaymentStatus status;
    private BigDecimal amount;
}
```

### 3. Inter-Service API Contracts
```java
// Using OpenAPI specification
// order-service-api.yaml
paths:
  /orders:
    post:
      requestBody:
        $ref: '#/components/schemas/CreateOrderRequest'
      responses:
        '201':
          $ref: '#/components/schemas/OrderResponse'
```

## Implementation Recommendations

1. **Progressive Refactoring**: First establish new shared libraries, then gradually migrate
2. **Version Management**: Use semantic versioning
3. **Backward Compatibility**: Maintain backward compatibility of API contracts
4. **Documentation**: Complete API documentation and change logs

<!-- Translation placeholder - Use Kiro AI to translate this content -->
<!-- 翻譯佔位符 - 請使用 Kiro AI 翻譯此內容 -->
