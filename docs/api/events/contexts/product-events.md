# Product Context Events

## Overview

This document describes all domain events published by the Product bounded context. These events capture product lifecycle, catalog management, and inventory-related changes.

**Last Updated**: 2025-10-25

---

## Event List

| Event Name | Trigger | Frequency | Priority |
|------------|---------|-----------|----------|
| `ProductCreatedEvent` | Product creation | Medium | P1 |
| `ProductUpdatedEvent` | Product edit | High | P1 |
| `ProductPriceChangedEvent` | Price update | Medium | P0 |
| `ProductStockUpdatedEvent` | Stock adjustment | High | P0 |
| `ProductDeactivatedEvent` | Product deactivation | Low | P1 |
| `ProductReactivatedEvent` | Product reactivation | Low | P1 |
| `ProductCategoryChangedEvent` | Category update | Low | P2 |

---

## ProductCreatedEvent

### Event Structure

```java
public record ProductCreatedEvent(
    ProductId productId,
    String productName,
    String description,
    CategoryId categoryId,
    Money price,
    int initialStock,
    SellerId sellerId,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### Example JSON

```json
{
  "eventType": "ProductCreated",
  "eventId": "200e8400-e29b-41d4-a716-446655440000",
  "occurredOn": "2025-10-25T09:00:00Z",
  "productId": "PROD-001",
  "productName": "iPhone 15 Pro 256GB",
  "description": "最新款 iPhone，配備 A17 Pro 晶片",
  "categoryId": "CAT-ELECTRONICS",
  "price": {
    "amount": 35900,
    "currency": "TWD"
  },
  "initialStock": 100,
  "sellerId": "SELLER-001"
}
```

### Event Handlers

- `ProductSearchIndexHandler`: Add to search index
- `ProductCatalogHandler`: Update product catalog
- `SellerProductCountHandler`: Update seller's product count

---

## ProductPriceChangedEvent

### Event Structure

```java
public record ProductPriceChangedEvent(
    ProductId productId,
    Money oldPrice,
    Money newPrice,
    String changeReason,
    LocalDateTime effectiveDate,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### Example JSON

```json
{
  "eventType": "ProductPriceChanged",
  "eventId": "210e8400-e29b-41d4-a716-446655440001",
  "occurredOn": "2025-10-25T10:00:00Z",
  "productId": "PROD-001",
  "oldPrice": {
    "amount": 35900,
    "currency": "TWD"
  },
  "newPrice": {
    "amount": 32900,
    "currency": "TWD"
  },
  "changeReason": "促銷活動",
  "effectiveDate": "2025-10-26T00:00:00Z"
}
```

### Event Handlers

- `PricingCacheHandler`: Update pricing cache
- `PriceHistoryHandler`: Record price history
- `PriceAlertHandler`: Notify customers with price alerts

---

## ProductStockUpdatedEvent

### Event Structure

```java
public record ProductStockUpdatedEvent(
    ProductId productId,
    int oldStock,
    int newStock,
    int changeAmount,
    String updateReason,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### Example JSON

```json
{
  "eventType": "ProductStockUpdated",
  "eventId": "220e8400-e29b-41d4-a716-446655440002",
  "occurredOn": "2025-10-25T11:00:00Z",
  "productId": "PROD-001",
  "oldStock": 100,
  "newStock": 99,
  "changeAmount": -1,
  "updateReason": "ORDER_PLACED"
}
```

### Event Handlers

- `LowStockAlertHandler`: Check for low stock alerts
- `ProductAvailabilityHandler`: Update availability status
- `InventoryReportHandler`: Update inventory reports

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-25  
**Owner**: Product Domain Team
