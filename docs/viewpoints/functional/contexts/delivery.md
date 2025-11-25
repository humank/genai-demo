---
title: "Delivery Context"
type: "functional-viewpoint"
category: "functional"
status: "active"
last_updated: "2025-10-22"
owner: "Architecture Team"
---

# Delivery Context

> **Responsibility**: Manage order delivery and logistics

## Overview

The Delivery Context manages the logistics of getting orders to customers. It handles shipping schedules, tracking, and delivery status updates. It integrates with external logistics providers.

## Domain Model

**Core Aggregate**: `Delivery`

**Key Entities**:
- `Delivery` (Aggregate Root)
- `DeliveryRoute`
- `TrackingEvent`

**Key Value Objects**:
- `DeliveryId`
- `OrderId`
- `TrackingNumber`
- `DeliveryStatus` (SCHEDULED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, FAILED)
- `DeliveryAddress`
- `EstimatedDeliveryDate`

### Domain Model Diagram

```mermaid
classDiagram
    class Delivery {
        +DeliveryId id
        +OrderId orderId
        +DeliveryStatus status
        +TrackingNumber trackingNumber
        +DeliveryAddress address
        +schedule()
        +dispatch()
        +deliver()
    }
    class DeliveryRoute {
        +RouteId id
        +List~Checkpoint~ checkpoints
        +EstimatedTime arrival
    }
    class TrackingEvent {
        +DateTime timestamp
        +String location
        +String status
        +String description
    }

    Delivery "1" --> "1" DeliveryRoute
    Delivery "1" --> "*" TrackingEvent
```

## Events

### Event Flow

```mermaid
sequenceDiagram
    participant O as Order Context
    participant D as Delivery Context
    participant L as Logistics Provider

    O-->>D: OrderConfirmedEvent
    D->>L: Schedule Shipment
    L-->>D: Tracking Number Assigned
    D-->>O: DeliveryScheduledEvent

    L-->>D: Package Picked Up
    D-->>O: DeliveryDispatchedEvent

    L-->>D: Package Delivered
    D-->>O: DeliveryDeliveredEvent
```

**Domain Events Published**:
- `DeliveryScheduledEvent`
- `DeliveryDispatchedEvent`
- `DeliveryInTransitEvent`
- `DeliveryDeliveredEvent`
- `DeliveryFailedEvent`

**Domain Events Consumed**:
- `OrderConfirmedEvent` (from Order Context) → Schedule delivery
- `PaymentCompletedEvent` (from Payment Context) → Confirm delivery

## API Interface

**REST API Endpoints**:
- `GET /api/v1/deliveries/{orderId}` - Get delivery status
- `GET /api/v1/deliveries/track/{trackingNumber}` - Track delivery
- `POST /api/v1/deliveries/{id}/update-status` - Update status (logistics)

## Business Rules

- Delivery can only be scheduled for confirmed orders
- Tracking number must be unique
- Estimated delivery: 3-5 business days for standard shipping
- Failed deliveries trigger customer notification
