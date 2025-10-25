# Shipping Context Events

## Overview

Domain events for shipping and logistics operations.

**Last Updated**: 2025-10-25

---

## Event List

| Event Name | Trigger | Priority |
|------------|---------|----------|
| `ShippingScheduledEvent` | Order confirmation | P0 |
| `ShippingLabelCreatedEvent` | Label generation | P0 |
| `ShippingDispatchedEvent` | Package pickup | P0 |
| `ShippingInTransitEvent` | Transit update | P1 |
| `ShippingDeliveredEvent` | Delivery confirmation | P0 |
| `ShippingFailedEvent` | Delivery failure | P1 |

---

## ShippingScheduledEvent

```java
public record ShippingScheduledEvent(
    ShippingId shippingId,
    OrderId orderId,
    ShippingAddress address,
    LocalDateTime scheduledDate,
    String carrier,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### Example JSON

```json
{
  "eventType": "ShippingScheduled",
  "eventId": "500e8400-e29b-41d4-a716-446655440000",
  "occurredOn": "2025-10-25T10:10:00Z",
  "shippingId": "SHIP-2025-001",
  "orderId": "ORD-2025-001",
  "address": {
    "recipientName": "張小明",
    "street": "台北市信義區信義路五段7號",
    "city": "台北市",
    "postalCode": "110"
  },
  "scheduledDate": "2025-10-26T09:00:00Z",
  "carrier": "黑貓宅急便"
}
```

---

## ShippingDispatchedEvent

```java
public record ShippingDispatchedEvent(
    ShippingId shippingId,
    OrderId orderId,
    String trackingNumber,
    String carrier,
    LocalDateTime dispatchedAt,
    LocalDateTime estimatedDelivery,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### Example JSON

```json
{
  "eventType": "ShippingDispatched",
  "eventId": "510e8400-e29b-41d4-a716-446655440001",
  "occurredOn": "2025-10-26T09:00:00Z",
  "shippingId": "SHIP-2025-001",
  "orderId": "ORD-2025-001",
  "trackingNumber": "TW1234567890",
  "carrier": "黑貓宅急便",
  "dispatchedAt": "2025-10-26T09:00:00Z",
  "estimatedDelivery": "2025-10-27T18:00:00Z"
}
```

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-25  
**Owner**: Shipping Domain Team
