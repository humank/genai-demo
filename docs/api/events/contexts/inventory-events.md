# Inventory Context Events

## Overview

Domain events for inventory management and stock control.

**Last Updated**: 2025-10-25

---

## Event List

| Event Name | Trigger | Priority |
|------------|---------|----------|
| `InventoryReservedEvent` | Order submission | P0 |
| `InventoryReleasedEvent` | Order cancellation | P0 |
| `InventoryAdjustedEvent` | Stock adjustment | P1 |
| `InventoryLowStockAlertEvent` | Low stock detection | P1 |

---

## InventoryReservedEvent

```java
public record InventoryReservedEvent(
    OrderId orderId,
    List<ReservedItemDto> reservedItems,
    LocalDateTime reservedUntil,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### Example JSON

```json
{
  "eventType": "InventoryReserved",
  "eventId": "400e8400-e29b-41d4-a716-446655440000",
  "occurredOn": "2025-10-25T10:06:30Z",
  "orderId": "ORD-2025-001",
  "reservedItems": [
    {
      "productId": "PROD-001",
      "quantity": 1,
      "warehouseId": "WH-TPE-001"
    }
  ],
  "reservedUntil": "2025-10-25T10:21:30Z"
}
```

---

## InventoryLowStockAlertEvent

```java
public record InventoryLowStockAlertEvent(
    ProductId productId,
    int currentStock,
    int threshold,
    String warehouseId,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### Example JSON

```json
{
  "eventType": "InventoryLowStockAlert",
  "eventId": "410e8400-e29b-41d4-a716-446655440001",
  "occurredOn": "2025-10-25T14:00:00Z",
  "productId": "PROD-001",
  "currentStock": 5,
  "threshold": 10,
  "warehouseId": "WH-TPE-001"
}
```

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-25  
**Owner**: Inventory Domain Team
