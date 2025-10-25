# Remaining Contexts Events

## Overview

This document consolidates domain events for the remaining bounded contexts: Promotion, Notification, Review, Shopping Cart, Pricing, Seller, and Delivery.

**Last Updated**: 2025-10-25

---

## Promotion Context Events

### PromotionCreatedEvent

```java
public record PromotionCreatedEvent(
    PromotionId promotionId,
    String promotionName,
    DiscountType discountType,
    Money discountValue,
    LocalDateTime startDate,
    LocalDateTime endDate,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### PromotionAppliedEvent

```java
public record PromotionAppliedEvent(
    PromotionId promotionId,
    OrderId orderId,
    CustomerId customerId,
    Money discountAmount,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

---

## Notification Context Events

### NotificationSentEvent

```java
public record NotificationSentEvent(
    NotificationId notificationId,
    CustomerId customerId,
    NotificationType type,
    String channel,
    String subject,
    LocalDateTime sentAt,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### Example JSON

```json
{
  "eventType": "NotificationSent",
  "eventId": "600e8400-e29b-41d4-a716-446655440000",
  "occurredOn": "2025-10-25T10:00:30Z",
  "notificationId": "NOTIF-2025-001",
  "customerId": "CUST-001",
  "type": "ORDER_CONFIRMATION",
  "channel": "EMAIL",
  "subject": "訂單確認 - ORD-2025-001",
  "sentAt": "2025-10-25T10:00:30Z"
}
```

---

## Review Context Events

### ReviewCreatedEvent

```java
public record ReviewCreatedEvent(
    ReviewId reviewId,
    ProductId productId,
    CustomerId customerId,
    int rating,
    String comment,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### Example JSON

```json
{
  "eventType": "ReviewCreated",
  "eventId": "700e8400-e29b-41d4-a716-446655440000",
  "occurredOn": "2025-10-28T15:00:00Z",
  "reviewId": "REV-2025-001",
  "productId": "PROD-001",
  "customerId": "CUST-001",
  "rating": 5,
  "comment": "非常好的產品，推薦購買！"
}
```

---

## Shopping Cart Context Events

### ItemAddedToCartEvent

```java
public record ItemAddedToCartEvent(
    CartId cartId,
    CustomerId customerId,
    ProductId productId,
    int quantity,
    Money unitPrice,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### Example JSON

```json
{
  "eventType": "ItemAddedToCart",
  "eventId": "800e8400-e29b-41d4-a716-446655440000",
  "occurredOn": "2025-10-25T09:30:00Z",
  "cartId": "CART-2025-001",
  "customerId": "CUST-001",
  "productId": "PROD-001",
  "quantity": 1,
  "unitPrice": {
    "amount": 35900,
    "currency": "TWD"
  }
}
```

### CartCheckedOutEvent

```java
public record CartCheckedOutEvent(
    CartId cartId,
    CustomerId customerId,
    OrderId orderId,
    int totalItems,
    Money totalAmount,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

---

## Pricing Context Events

### PriceCalculatedEvent

```java
public record PriceCalculatedEvent(
    OrderId orderId,
    Money subtotal,
    Money tax,
    Money shippingFee,
    Money discount,
    Money totalAmount,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### Example JSON

```json
{
  "eventType": "PriceCalculated",
  "eventId": "900e8400-e29b-41d4-a716-446655440000",
  "occurredOn": "2025-10-25T10:01:00Z",
  "orderId": "ORD-2025-001",
  "subtotal": {
    "amount": 35900,
    "currency": "TWD"
  },
  "tax": {
    "amount": 1795,
    "currency": "TWD"
  },
  "shippingFee": {
    "amount": 0,
    "currency": "TWD"
  },
  "discount": {
    "amount": 0,
    "currency": "TWD"
  },
  "totalAmount": {
    "amount": 37695,
    "currency": "TWD"
  }
}
```

---

## Seller Context Events

### SellerRegisteredEvent

```java
public record SellerRegisteredEvent(
    SellerId sellerId,
    String sellerName,
    String businessRegistrationNumber,
    Email contactEmail,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### SellerProductListedEvent

```java
public record SellerProductListedEvent(
    SellerId sellerId,
    ProductId productId,
    String productName,
    Money price,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

---

## Delivery Context Events

### DeliveryScheduledEvent

```java
public record DeliveryScheduledEvent(
    DeliveryId deliveryId,
    OrderId orderId,
    ShippingAddress address,
    LocalDateTime scheduledTime,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### DeliveryAssignedEvent

```java
public record DeliveryAssignedEvent(
    DeliveryId deliveryId,
    OrderId orderId,
    DriverId driverId,
    String driverName,
    String driverPhone,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### DeliveryCompletedEvent

```java
public record DeliveryCompletedEvent(
    DeliveryId deliveryId,
    OrderId orderId,
    LocalDateTime deliveredAt,
    String recipientSignature,
    Optional<String> deliveryPhoto,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### Example JSON

```json
{
  "eventType": "DeliveryCompleted",
  "eventId": "a00e8400-e29b-41d4-a716-446655440000",
  "occurredOn": "2025-10-27T14:30:00Z",
  "deliveryId": "DEL-2025-001",
  "orderId": "ORD-2025-001",
  "deliveredAt": "2025-10-27T14:30:00Z",
  "recipientSignature": "張小明",
  "deliveryPhoto": "https://s3.amazonaws.com/delivery-photos/DEL-2025-001.jpg"
}
```

---

## Event Summary by Context

| Context | Events | Key Use Cases |
|---------|--------|---------------|
| Promotion | 4 | Discount management, promotion tracking |
| Notification | 3 | Customer communication, alerts |
| Review | 4 | Product feedback, ratings |
| Shopping Cart | 5 | Cart management, checkout |
| Pricing | 3 | Price calculation, tax computation |
| Seller | 5 | Seller onboarding, product listing |
| Delivery | 5 | Last-mile delivery tracking |

---

## Related Documentation

- **Event Catalog**: [event-catalog.md](../event-catalog.md)
- **Customer Events**: [customer-events.md](customer-events.md)
- **Order Events**: [order-events.md](order-events.md)
- **Product Events**: [product-events.md](product-events.md)
- **Payment Events**: [payment-events.md](payment-events.md)

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-25  
**Owner**: Architecture Team
