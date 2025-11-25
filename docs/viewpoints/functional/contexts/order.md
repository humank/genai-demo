---
title: "Order Context"
type: "functional-viewpoint"
category: "functional"
status: "active"
last_updated: "2025-10-22"
owner: "Architecture Team"
---

# Order Context

> **Responsibility**: Manage order lifecycle from creation to completion

## Overview

The Order Context handles the entire lifecycle of an order, from placement to fulfillment and completion. It orchestrates interactions with Inventory, Payment, and Delivery contexts to ensure orders are processed correctly.

## Domain Model

**Core Aggregate**: `Order`

**Key Entities**:
- `Order` (Aggregate Root)
- `OrderItem`
- `OrderHistory`

**Key Value Objects**:
- `OrderId`
- `OrderStatus` (CREATED, PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
- `OrderItem`
- `ShippingAddress`
- `Money`
- `Quantity`

### Domain Model Diagram

```mermaid
classDiagram
    class Order {
        +OrderId id
        +CustomerId customerId
        +OrderStatus status
        +Money totalAmount
        +DateTime createdAt
        +addItem()
        +removeItem()
        +submit()
        +cancel()
    }
    class OrderItem {
        +ProductId productId
        +Quantity quantity
        +Money unitPrice
        +Money subtotal
    }
    class ShippingAddress {
        +String street
        +String city
        +String country
    }

    Order "1" --> "*" OrderItem
    Order "1" --> "1" ShippingAddress
```

## Events

### Event Flow

```mermaid
sequenceDiagram
    participant C as Customer
    participant O as Order Context
    participant I as Inventory Context
    participant P as Payment Context

    C->>O: Create Order
    O-->>O: OrderCreatedEvent

    C->>O: Submit Order
    O-->>I: OrderSubmittedEvent

    I-->>O: InventoryReservedEvent
    O->>P: Initiate Payment

    P-->>O: PaymentCompletedEvent
    O->>O: Confirm Order
    O-->>C: OrderConfirmedEvent
```

**Domain Events Published**:
- `OrderCreatedEvent`
- `OrderSubmittedEvent`
- `OrderConfirmedEvent`
- `OrderCancelledEvent`
- `OrderShippedEvent`
- `OrderDeliveredEvent`
- `OrderCompletedEvent`
- `OrderItemAddedEvent`
- `OrderItemRemovedEvent`

**Domain Events Consumed**:
- `InventoryReservedEvent` (from Inventory Context) → Confirm order
- `PaymentCompletedEvent` (from Payment Context) → Proceed with fulfillment
- `PaymentFailedEvent` (from Payment Context) → Cancel order
- `DeliveryScheduledEvent` (from Delivery Context) → Update order status

## API Interface

**REST API Endpoints**:
- `POST /api/v1/orders` - Create new order
- `GET /api/v1/orders/{id}` - Get order details
- `POST /api/v1/orders/{id}/submit` - Submit order for processing
- `POST /api/v1/orders/{id}/cancel` - Cancel order
- `GET /api/v1/orders?customerId={id}` - List customer orders
- `GET /api/v1/orders/{id}/history` - Get order history

## Business Rules

- Order must have at least one item
- Order can only be cancelled if status is CREATED or PENDING
- Total amount must match sum of item prices plus shipping
- Order confirmation requires successful inventory reservation and payment
