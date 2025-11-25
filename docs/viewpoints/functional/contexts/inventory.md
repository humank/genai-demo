---
title: "Inventory Context"
type: "functional-viewpoint"
category: "functional"
status: "active"
last_updated: "2025-10-22"
owner: "Architecture Team"
---

# Inventory Context

> **Responsibility**: Manage product stock levels and inventory operations

## Overview

The Inventory Context tracks the physical stock of products. It manages reservations during the ordering process and handles stock replenishment. It ensures that the platform does not sell more items than are available.

## Domain Model

**Core Aggregate**: `InventoryItem`

**Key Entities**:
- `InventoryItem` (Aggregate Root)
- `StockMovement`
- `Reservation`

**Key Value Objects**:
- `InventoryItemId`
- `ProductId`
- `Quantity`
- `ReservationId`
- `WarehouseLocation`

### Domain Model Diagram

```mermaid
classDiagram
    class InventoryItem {
        +InventoryItemId id
        +ProductId productId
        +Quantity available
        +Quantity reserved
        +WarehouseLocation location
        +reserve()
        +release()
        +replenish()
    }
    class Reservation {
        +ReservationId id
        +OrderId orderId
        +Quantity quantity
        +DateTime expiresAt
    }
    class StockMovement {
        +MovementId id
        +Quantity quantity
        +MovementType type
        +DateTime timestamp
    }

    InventoryItem "1" --> "*" Reservation
    InventoryItem "1" --> "*" StockMovement
```

## Events

### Event Flow

```mermaid
sequenceDiagram
    participant O as Order Context
    participant I as Inventory Context

    O-->>I: OrderSubmittedEvent
    I->>I: Check Availability
    alt Stock Available
        I->>I: Create Reservation
        I-->>O: InventoryReservedEvent
    else Out of Stock
        I-->>O: InventoryDepletedEvent
    end

    O-->>I: OrderConfirmedEvent
    I->>I: Confirm Reservation
    I->>I: Deduct Stock
```

**Domain Events Published**:
- `InventoryReservedEvent`
- `InventoryReleasedEvent`
- `InventoryReplenishedEvent`
- `InventoryDepletedEvent`
- `StockLevelChangedEvent`

**Domain Events Consumed**:
- `OrderSubmittedEvent` (from Order Context) → Reserve inventory
- `OrderCancelledEvent` (from Order Context) → Release reservation
- `OrderDeliveredEvent` (from Order Context) → Commit reservation

## API Interface

**REST API Endpoints**:
- `GET /api/v1/inventory/{productId}` - Get stock level
- `POST /api/v1/inventory/{productId}/replenish` - Add stock (admin)
- `GET /api/v1/inventory/low-stock` - List low stock items (admin)

## Business Rules

- Stock level cannot be negative
- Reservations expire after 15 minutes if order not confirmed
- Low stock alert when quantity < 10
- Automatic reorder when quantity < 5
